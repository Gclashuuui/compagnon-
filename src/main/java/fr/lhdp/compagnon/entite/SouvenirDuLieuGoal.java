package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.fiche.FicheCompagnon;
import fr.lhdp.compagnon.fiche.Fiches;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * « Il se souvient d'ici. »
 *
 * <h2>Ce que c'est</h2>
 *
 * <p>Quand il repasse a l'endroit ou on l'a soigne, ou la ou il mange
 * d'habitude, il s'arrete. Il regarde. Puis il repart.
 *
 * <p>Rien n'est ecrit, rien n'est explique, aucune bulle n'apparait. C'est au
 * joueur de se souvenir de ce qui s'est passe la — et il s'en souvient, parce
 * que c'est lui qui y etait. Une bete qui reconnait un endroit n'a pas besoin
 * de le dire.
 *
 * <h2>Ce que ce n'est pas</h2>
 *
 * <p>Ce n'est <b>pas</b> une memoire des chemins. Celle-la a ete ecartee
 * exprès : elle coute cher, elle est difficile a rendre juste, et surtout elle
 * ne se voit pas. Un chemin memorise ressemble a un chemin ordinaire.
 *
 * <p>Un endroit reconnu, lui, se voit tout de suite.
 *
 * <h2>Ce que ca coute</h2>
 *
 * <p>Quatre comparaisons de distance, une fois toutes les trois secondes, et
 * uniquement quand il ne fait rien d'autre — ce but est le dernier de la liste
 * et n'interrompt jamais rien. Le reste du temps, {@link #canUse()} sort a la
 * premiere ligne.
 */
public class SouvenirDuLieuGoal extends Goal {

	/** A quelle distance d'un souvenir il commence a le reconnaitre, en blocs. */
	private static final double PORTEE = 4.0D;

	/** Il ne verifie pas a chaque tick : trois secondes suffisent largement. */
	private static final int ENTRE_DEUX_REGARDS = 20 * 3;

	/** Combien de temps il s'arrete pour regarder, en ticks. */
	private static final int DUREE = 45;

	/**
	 * Avant de refaire le meme geste au meme endroit.
	 *
	 * <p>Sans ce delai, un compagnon qui vit dans la chambre ou on le nourrit
	 * s'arreterait toutes les trois secondes, pour toujours. Ce qui devait etre
	 * un souvenir deviendrait un tic.
	 */
	private static final int AVANT_DE_REFAIRE = 20 * 90;

	/**
	 * Le role d'animation joue en reconnaissant un endroit.
	 *
	 * <p>Le prefixe {@code @} veut dire « c'est un role, la fiche d'espece dira
	 * quelle animation ». Une espece qui ne le decrit pas s'arrete quand meme et
	 * regarde — simplement sans jouer d'animation particuliere.
	 */
	private static final String GESTE = "@reconnait";

	private final CompagnonEntity compagnon;

	private int avantDeRegarder;
	private int reste;
	private FicheCompagnon.Lieu trouve;

	public SouvenirDuLieuGoal(CompagnonEntity compagnon) {
		this.compagnon = compagnon;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		// Il ne s'arrete pas si on lui a demande quelque chose, ni s'il porte
		// quelque chose : il est occupe, et le souvenir peut attendre.
		if (!this.compagnon.estLibre() || !this.compagnon.lesMainsVides()) {
			return false;
		}
		if (--this.avantDeRegarder > 0) {
			return false;
		}
		this.avantDeRegarder = ENTRE_DEUX_REGARDS;

		this.trouve = lieuSousSesPattes();
		return this.trouve != null;
	}

	@Override
	public void start() {
		this.reste = DUREE;
		this.compagnon.getNavigation().stop();
		this.compagnon.jouerActionPendant(GESTE, DUREE);

		// Le compte a rebours est pose ici et non dans canUse : c'est le fait de
		// s'etre arrete qui compte, pas celui d'etre passe a cote.
		this.avantDeRegarder = AVANT_DE_REFAIRE;
	}

	@Override
	public boolean canContinueToUse() {
		return this.reste > 0 && this.compagnon.estLibre();
	}

	@Override
	public void stop() {
		this.trouve = null;
		this.compagnon.getNavigation().stop();
	}

	@Override
	public void tick() {
		this.reste--;
		if (this.trouve != null) {
			this.compagnon.getLookControl().setLookAt(
					this.trouve.x() + 0.5D, this.trouve.y() + 0.5D, this.trouve.z() + 0.5D);
		}
	}

	/** Le souvenir sur lequel il se trouve, ou {@code null}. */
	private FicheCompagnon.Lieu lieuSousSesPattes() {
		if (!(this.compagnon.level() instanceof ServerLevel niveau)
				|| this.compagnon.ficheId() == null) {
			return null;
		}
		FicheCompagnon fiche = Fiches.de(niveau.getServer()).get(this.compagnon.ficheId());
		if (fiche == null) {
			return null;
		}
		for (FicheCompagnon.Lieu lieu : fiche.lieux()) {
			if (lieu.proche(this.compagnon.getX(), this.compagnon.getY(),
					this.compagnon.getZ(), PORTEE)) {
				return lieu;
			}
		}
		return null;
	}
}
