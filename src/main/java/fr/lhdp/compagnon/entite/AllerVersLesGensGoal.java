package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.fiche.Mode;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Il va voir les gens, et se colle a eux.
 *
 * <p>C'est le comportement qui fait vivre les couloirs : un compagnon pose dans
 * une salle ne reste pas plante, il vient renifler ceux qui passent.
 *
 * <p>Deux traits le pilotent, et ils ne font pas la meme chose :
 *
 * <ul>
 *   <li>la <b>sociabilite</b> decide s'il <i>ose</i> approcher un inconnu. Un
 *       timide ne bougera que pour son maitre ;</li>
 *   <li>le <b>calin</b> decide a quelle distance il s'arrete. Un calin vient
 *       carrement dans les jambes, un independant garde deux pas d'ecart.</li>
 * </ul>
 *
 * <p>Il ne s'impose pas : au bout d'un moment il repart, meme si la personne est
 * toujours la. Sinon il resterait colle indefiniment, ce qui est penible.
 */
public class AllerVersLesGensGoal extends Goal {

	/** A quelle distance il remarque quelqu'un. Valeur inventee. */
	private static final double PORTEE = 8.0D;

	/** Distance a laquelle il s'arrete, avant l'effet du trait calin. */
	private static final double ARRET = 2.0D;

	/** Chance, a chaque examen, qu'il ait envie d'y aller. Valeur inventee. */
	private static final float ENVIE = 0.02F;

	/**
	 * En dessous de cette sociabilite, il n'approche pas un inconnu. Il ira quand
	 * meme vers quelqu'un qu'il connait. Valeur inventee.
	 */
	private static final float SEUIL_INCONNU = 0.35F;

	/** Combien de temps il reste avant de se lasser, en ticks. Valeur inventee. */
	private static final int DUREE = 200;

	private static final double VITESSE = 1.0D;

	/** On ne recalcule pas un chemin a chaque tick. */
	private static final int TICKS_ENTRE_CHEMINS = 10;

	private final CompagnonEntity compagnon;
	private Player cible;
	private int reste;
	private int attente;

	public AllerVersLesGensGoal(CompagnonEntity compagnon) {
		this.compagnon = compagnon;
		setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		// Assis ou couche, un ordre tient : il ne se leve pas pour aller voir
		// quelqu'un. Et quand il suit son maitre, il a deja mieux a faire.
		if (this.compagnon.mode().pose() || this.compagnon.mode() == Mode.SUIT) {
			return false;
		}
		// Ce tirage vient en premier : c'est la recherche de joueur qui coute.
		if (this.compagnon.getRandom().nextFloat() > envie()) {
			return false;
		}

		Player candidat = this.compagnon.level().getNearestPlayer(this.compagnon, PORTEE);
		if (candidat == null || candidat.isSpectator()) {
			return false;
		}

		// Un timide n'ira pas vers un inconnu, mais il ira vers quelqu'un qui
		// s'occupe de lui depuis un moment. C'est la que sa memoire compte.
		if (!this.compagnon.connait(candidat.getUUID())
				&& this.compagnon.caractere().sociabilite() < SEUIL_INCONNU) {
			return false;
		}

		this.cible = candidat;
		this.reste = DUREE;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return this.cible != null
				&& this.cible.isAlive()
				&& this.reste > 0
				&& !this.compagnon.mode().pose()
				&& this.compagnon.distanceToSqr(this.cible) < PORTEE * PORTEE * 2.0D;
	}

	@Override
	public void stop() {
		this.cible = null;
		this.compagnon.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (this.cible == null) {
			return;
		}
		this.reste--;
		this.compagnon.getLookControl().setLookAt(this.cible, 10.0F, 10.0F);

		double arret = arret();
		if (this.compagnon.distanceToSqr(this.cible) <= arret * arret) {
			// Arrive. Il reste la et le regarde, plutot que de tourner autour.
			this.compagnon.getNavigation().stop();
			return;
		}

		if (this.attente-- > 0) {
			return;
		}
		this.attente = TICKS_ENTRE_CHEMINS;
		this.compagnon.getNavigation().moveTo(this.cible, VITESSE);
	}

	/**
	 * L'envie d'y aller : sa sociabilite, tempere par son entrain.
	 *
	 * <p>Un compagnon affame ou epuise ne vient pas vous dire bonjour, meme s'il
	 * est du genre sociable. C'est ce qui relie les cinq barres a ce qu'on voit.
	 */
	private float envie() {
		return ENVIE * this.compagnon.caractere().sociabilite() * 2.0F
				* this.compagnon.entrain() * this.compagnon.profilCerveau().sociabilite();
	}

	/**
	 * Plus il est calin, plus il vient pres. Et il s'approche encore davantage de
	 * quelqu'un qu'il connait : on ne garde pas ses distances avec un habitue.
	 */
	private double arret() {
		double base = ARRET * (1.5D - this.compagnon.caractere().calin());
		boolean connu = this.cible != null && this.compagnon.connait(this.cible.getUUID());
		return connu ? base * 0.6D : base;
	}
}
