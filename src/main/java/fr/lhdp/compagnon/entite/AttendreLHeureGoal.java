package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.fiche.FicheCompagnon;
import fr.lhdp.compagnon.fiche.Fiches;
import fr.lhdp.compagnon.fiche.Mode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * « Il t'attend. »
 *
 * <h2>Ce qui se passe</h2>
 *
 * <p>Si on le nourrit toujours a peu pres a la meme heure, il finit par le
 * savoir. Ce jour-la, quand l'heure approche, il va s'asseoir a l'endroit ou on
 * le nourrit d'habitude, et il attend.
 *
 * <p>Le joueur arrive. Il est deja la. Personne ne lui a rien demande.
 *
 * <h2>Une habitude, ou rien</h2>
 *
 * <p>Il faut <b>trois repas sur quatre</b> a deux heures pres pour qu'une heure
 * existe. Voir {@link FicheCompagnon#heureHabituelle()} : une moyenne aurait
 * toujours rendu un chiffre, meme pour quelqu'un qui nourrit sa bete n'importe
 * quand. Elle serait alors allee attendre a une heure qui ne veut rien dire, et
 * toute l'illusion serait tombee du premier coup.
 *
 * <p>Mieux vaut qu'il n'attende jamais que de l'apercevoir attendre au hasard.
 *
 * <h2>Il n'attend pas quand tu es la</h2>
 *
 * <p>Aller poireauter dans la cuisine pendant que son maitre est a cote de lui
 * serait absurde. Ce but ne se declenche que <b>loin</b> de lui — ou en son
 * absence, ce qui est le cas le plus frequent : c'est justement pendant qu'on
 * n'est pas la qu'il faut que quelque chose se passe.
 */
public class AttendreLHeureGoal extends Goal {

	/** Combien d'heures avant l'heure du repas il commence a y aller. */
	private static final int AVANCE = 1;

	/** Et combien de temps apres il renonce. */
	private static final int RETARD = 2;

	/** Il verifie l'heure toutes les dix secondes. Elle ne change pas plus vite. */
	private static final int ENTRE_DEUX_COUPS_D_OEIL = 20 * 10;

	/** Assez pres du coin pour qu'on dise qu'il y est, en blocs. */
	private static final double ARRIVE = 2.5D;

	/** Au-dela, il n'a rien a faire la : son maitre est deja avec lui. */
	private static final double MAITRE_TROP_PRES = 12.0D;

	/** Il y va tranquillement : il n'est pas presse, il est en avance. */
	private static final double VITESSE = 0.9D;

	/** L'etiquette du lieu qu'il rejoint. Voir {@code Interactions}. */
	private static final String LE_COIN_DES_REPAS = "repas";

	private final CompagnonEntity compagnon;

	private int avantDeRegarderLHeure;
	private FicheCompagnon.Lieu coin;

	public AttendreLHeureGoal(CompagnonEntity compagnon) {
		this.compagnon = compagnon;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		// Il ne plante pas un ordre en cours pour aller attendre : ce serait le
		// contraire de ce qu'on veut. Un compagnon qu'on a fait asseoir reste
		// assis, meme a l'heure du diner.
		if (!this.compagnon.estLibre() || this.compagnon.mode() != Mode.RESTE) {
			return false;
		}
		if (--this.avantDeRegarderLHeure > 0) {
			return false;
		}
		this.avantDeRegarderLHeure = ENTRE_DEUX_COUPS_D_OEIL;

		if (maitreDejaLa()) {
			return false;
		}
		this.coin = coinDesRepasSiCEstLHeure();
		return this.coin != null;
	}

	@Override
	public boolean canContinueToUse() {
		if (this.coin == null || !this.compagnon.estLibre()) {
			return false;
		}
		// Arrive : il reste la, mais ce n'est plus a ce but de le tenir. Les buts
		// de flanerie prennent le relais, et il attend simplement sur place.
		return !this.coin.proche(this.compagnon.getX(), this.compagnon.getY(),
				this.compagnon.getZ(), ARRIVE);
	}

	@Override
	public void stop() {
		this.coin = null;
		this.compagnon.getNavigation().stop();
	}

	@Override
	public void tick() {
		if (this.coin == null) {
			return;
		}
		if (this.compagnon.getNavigation().isDone()) {
			this.compagnon.getNavigation().moveTo(
					this.coin.x() + 0.5D, this.coin.y() + 0.5D, this.coin.z() + 0.5D, VITESSE);
		}
	}

	// --- Les deux questions ---------------------------------------------------------

	/** Son maitre est-il deja aupres de lui ? Alors il n'a personne a attendre. */
	private boolean maitreDejaLa() {
		return this.compagnon.getOwner() != null
				&& this.compagnon.getOwner().level() == this.compagnon.level()
				&& this.compagnon.distanceToSqr(this.compagnon.getOwner())
						< MAITRE_TROP_PRES * MAITRE_TROP_PRES;
	}

	/** Le coin des repas, si l'heure y est et s'il en connait un. */
	private FicheCompagnon.Lieu coinDesRepasSiCEstLHeure() {
		if (!(this.compagnon.level() instanceof ServerLevel niveau)
				|| this.compagnon.ficheId() == null) {
			return null;
		}
		FicheCompagnon fiche = Fiches.de(niveau.getServer()).get(this.compagnon.ficheId());
		if (fiche == null) {
			return null;
		}
		int habituelle = fiche.heureHabituelle();
		if (habituelle < 0) {
			return null;
		}

		// Minecraft place minuit a 18 000 sur ses 24 000 ticks : le decalage de six
		// heures est le meme que celui qui a servi a enregistrer l'heure du repas.
		int maintenant = (int) ((niveau.getDayTime() / 1000L + 6L) % 24L);

		// En avance d'une heure, en retard de deux : la fenetre est volontairement
		// large. Trop etroite, il n'y serait presque jamais, et la seule fois ou on
		// l'apercevrait aurait l'air d'un hasard plutot que d'une habitude.
		int ecart = FicheCompagnon.ecartDHeures(maintenant, habituelle);
		boolean avant = ecart <= AVANCE;
		boolean apres = maintenant >= habituelle && maintenant - habituelle <= RETARD;
		if (!avant && !apres) {
			return null;
		}

		for (FicheCompagnon.Lieu lieu : fiche.lieux()) {
			if (lieu.cle().equals(LE_COIN_DES_REPAS)) {
				return lieu;
			}
		}
		return null;
	}
}
