package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.contenu.Caractere;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Il va voir un compagnon qu'il connait.
 *
 * <p>Deux betes qui se sont assez souvent croisees finissent par se chercher.
 * C'est petit, mais c'est ce qui donne l'impression que le chateau est habite :
 * dans la salle commune, les compagnons ne sont plus alignes chacun devant son
 * proprietaire, ils se regroupent.
 *
 * <p>Il ne le fait que s'il est <b>libre</b> : un compagnon a qui on a dit de
 * rester assis reste assis, et un compagnon qui suit son maitre ne le lache pas
 * pour aller jouer. L'ordre du joueur passe toujours avant l'envie de la bete.
 */
public class RetrouverGoal extends Goal {

	/** Jusqu'ou il repere un ami, en blocs. Valeur inventee. */
	private static final double PORTEE = 12.0D;

	/** Il s'arrete a cette distance : assez pres pour jouer, sans se marcher dessus. */
	private static final double DISTANCE_ARRET = 2.5D;

	/** Vitesse d'approche. Il n'y va pas en courant : ce n'est pas urgent. */
	private static final double VITESSE = 1.0D;

	/**
	 * Combien de temps il reste avec lui, en ticks.
	 *
	 * <p>Vingt secondes : le temps qu'on remarque qu'ils sont ensemble, sans qu'ils
	 * se collent pour la soiree.
	 */
	private static final int DUREE = 20 * 20;

	/**
	 * Une chance sur tant a chaque evaluation. Sans cela, deux amis se colleraient
	 * l'un a l'autre en permanence des qu'ils se voient.
	 */
	private static final int RARETE = 200;

	private final CompagnonEntity compagnon;
	private CompagnonEntity ami;
	private int reste;
	private boolean sceneJouee;

	public RetrouverGoal(CompagnonEntity compagnon) {
		this.compagnon = compagnon;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (!this.compagnon.estLibre()) {
			return false;
		}
		// Un compagnon tres sociable cherche ses amis plus souvent. C'est le seul
		// endroit ou le caractere pousse VERS quelque chose plutot que de freiner.
		Caractere caractere = this.compagnon.caractere();
		int rarete = Math.max(20, (int) (RARETE * (1.5F - caractere.sociabilite())));
		if (this.compagnon.getRandom().nextInt(rarete) != 0) {
			return false;
		}

		// Le capteur social a déjà trouvé le plus proche. Aucun second parcours
		// d'entités n'est lancé par ce but.
		this.ami = this.compagnon.amiMemorise();
		return this.ami != null;
	}

	@Override
	public void start() {
		this.reste = DUREE;
		this.sceneJouee = false;
	}

	@Override
	public boolean canContinueToUse() {
		return this.ami != null
				&& this.ami.isAlive()
				&& this.reste > 0
				&& this.compagnon.estLibre()
				&& this.compagnon.distanceToSqr(this.ami) < PORTEE * PORTEE * 4;
	}

	@Override
	public void stop() {
		this.ami = null;
		this.sceneJouee = false;
		this.compagnon.getNavigation().stop();
	}

	@Override
	public void tick() {
		this.reste--;
		this.compagnon.getLookControl().setLookAt(this.ami, 30.0F, 30.0F);

		double distance = this.compagnon.distanceToSqr(this.ami);
		if (distance > DISTANCE_ARRET * DISTANCE_ARRET) {
			this.compagnon.getNavigation().moveTo(this.ami, VITESSE);
		} else {
			// Arrive : il ne pousse pas plus loin, il reste la et le regarde.
			this.compagnon.getNavigation().stop();
			jouerPetiteScene();
		}
	}

	/** Les deux amis réutilisent leurs gestes existants et les jouent ensemble. */
	private void jouerPetiteScene() {
		if (this.sceneJouee || this.ami == null) {
			return;
		}
		this.sceneJouee = true;
		String mien = SceneAffectiveGoal.roleAmiDisponible(this.compagnon);
		String sien = SceneAffectiveGoal.roleAmiDisponible(this.ami);
		if (mien != null) {
			this.compagnon.jouerActionPendant("@" + mien, 35, PrioriteAction.AFFECTIF);
		}
		if (sien != null && this.ami.peutFaireUnPetitGeste()) {
			this.ami.getLookControl().setLookAt(this.compagnon, 30.0F, 30.0F);
			this.ami.jouerActionPendant("@" + sien, 35, PrioriteAction.AFFECTIF);
		}
	}
}
