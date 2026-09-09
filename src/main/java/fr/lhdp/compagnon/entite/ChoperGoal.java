package fr.lhdp.compagnon.entite;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.EnumSet;

/**
 * « Chope ! » — il va chercher l'objet et le prend dans la gueule.
 *
 * <p>Rien a voir avec {@link RapporterGoal}, qui ramasse de lui-meme et vient
 * l'offrir. Ici c'est un <b>ordre</b> : on lui montre un objet, il va le prendre,
 * et il le <b>garde</b>. Il ne le rend que quand on dit « lache ».
 *
 * <p>Priorite haute, et pour une bonne raison : quand on lui demande quelque
 * chose, il le fait tout de suite. Aller renifler un bloc ou rejoindre un ami
 * peut attendre.
 *
 * <h2>L'objet ne se perd jamais</h2>
 *
 * <p>Comme pour le rapport, l'objet <b>quitte le monde</b> pendant qu'il le
 * porte, et l'entite du compagnon n'est pas sauvegardee. Le filet est le meme, et
 * il est unique : {@code CompagnonEntity.remove} repose au sol tout ce qui etait
 * dans la gueule. Ce but n'a donc rien a defaire quand il s'arrete.
 */
public class ChoperGoal extends Goal {

	/** Il attrape a cette distance. */
	private static final double DISTANCE_PRISE = 1.4D;

	/** Il y va d'un bon pas : on vient de le lui demander. */
	private static final double VITESSE = 1.2D;

	/**
	 * Au-dela, il renonce.
	 *
	 * <p>Un objet derriere une vitre ou sur un rebord le laisserait sinon courir
	 * apres pour toujours.
	 */
	private static final int PATIENCE = 20 * 15;

	/** Combien de temps il baisse la tete, en ticks. */
	private static final int DUREE_GESTE = 30;

	/**
	 * Le role d'animation joue au moment de la prise.
	 *
	 * <p>Le prefixe {@code @} veut dire « c'est un role, la fiche d'espece dira
	 * quelle animation ». Une espece qui ne le decrit pas ne joue rien — elle
	 * ramasse quand meme, simplement sans baisser la tete.
	 */
	private static final String GESTE = "@ramasse";

	private final CompagnonEntity compagnon;
	private int reste;

	public ChoperGoal(CompagnonEntity compagnon) {
		this.compagnon = compagnon;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		return cible() != null && this.compagnon.lesMainsVides();
	}

	@Override
	public void start() {
		this.reste = PATIENCE;
	}

	@Override
	public boolean canContinueToUse() {
		return this.reste > 0 && cible() != null && this.compagnon.lesMainsVides();
	}

	@Override
	public void stop() {
		this.compagnon.oublierLaMission();
		this.compagnon.getNavigation().stop();
	}

	@Override
	public void tick() {
		this.reste--;
		ItemEntity objet = cible();
		if (objet == null) {
			return;
		}
		this.compagnon.getLookControl().setLookAt(objet, 30.0F, 30.0F);

		if (this.compagnon.distanceToSqr(objet) > DISTANCE_PRISE * DISTANCE_PRISE) {
			this.compagnon.getNavigation().moveTo(objet, VITESSE);
			return;
		}

		this.compagnon.jouerActionPendant(GESTE, DUREE_GESTE, PrioriteAction.ORDRE);
		this.compagnon.prendreDansLaGueule(objet.getItem());
		objet.discard();
		this.compagnon.oublierLaMission();
	}

	/** L'objet vise, ou {@code null} s'il a disparu entre-temps. */
	private ItemEntity cible() {
		ItemEntity objet = this.compagnon.mission();
		return objet != null && objet.isAlive() ? objet : null;
	}
}
