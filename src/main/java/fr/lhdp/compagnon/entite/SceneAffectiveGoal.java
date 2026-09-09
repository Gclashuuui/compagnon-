package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.espece.Espece;
import fr.lhdp.compagnon.espece.Especes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Une initiative affective envers le maître, commune à toutes les espèces.
 *
 * <p>Aucune animation nouvelle n'est obligatoire : le rôle le plus proche déjà
 * fourni par l'espèce est choisi. Une future animation spécialisée pourra être
 * raccordée dans le JSON sans réécrire ce comportement.
 */
public final class SceneAffectiveGoal extends Goal {

	private static final double DISTANCE = 2.2D;
	private static final int ESSAI_TOUTES_LES = 20 * 15;
	private static final int APPROCHE_MAXIMUM = 20 * 8;
	private static final int REPOS_APRES_SCENE = 20 * 90;

	private final CompagnonEntity compagnon;
	private LivingEntity maitre;
	private int reste;
	private long prochaineEvaluation;
	private boolean gesteLance;

	public SceneAffectiveGoal(CompagnonEntity compagnon) {
		this.compagnon = compagnon;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		long maintenant = this.compagnon.level().getGameTime();
		if (this.prochaineEvaluation == 0L) {
			// Une première date propre à cette créature empêche toutes les scènes
			// affectives d'un serveur de commencer ensemble après un redémarrage.
			this.prochaineEvaluation = maintenant + Math.floorMod(
					this.compagnon.getUUID().hashCode(), ESSAI_TOUTES_LES);
			return false;
		}
		if (maintenant < this.prochaineEvaluation) {
			return false;
		}
		this.prochaineEvaluation = maintenant + ESSAI_TOUTES_LES;
		if (!this.compagnon.estLibre() || !this.compagnon.lesMainsVides()
				|| !this.compagnon.memoireCourte().contient(MemoireCourte.Signal.MAITRE_IMMOBILE)) {
			return false;
		}
		this.maitre = this.compagnon.getOwner();
		if (this.maitre == null || !this.maitre.isAlive()) {
			return false;
		}

		float lien = this.compagnon.complicite() / 100.0F;
		float envie = 0.15F + this.compagnon.caractere().calin() * 0.35F
				+ this.compagnon.caractere().attachement() * 0.25F + lien * 0.25F;
		return this.compagnon.getRandom().nextFloat() < envie * this.compagnon.entrain();
	}

	@Override
	public void start() {
		this.reste = APPROCHE_MAXIMUM;
		this.gesteLance = false;
		this.prochaineEvaluation = Math.max(this.prochaineEvaluation,
				this.compagnon.level().getGameTime() + REPOS_APRES_SCENE);
	}

	@Override
	public boolean canContinueToUse() {
		if (this.maitre == null || !this.maitre.isAlive() || this.reste <= 0) {
			return false;
		}
		return this.gesteLance ? this.compagnon.occupe() : this.compagnon.estLibre();
	}

	@Override
	public void tick() {
		this.reste--;
		this.compagnon.getLookControl().setLookAt(this.maitre, 30.0F, 30.0F);
		if (this.gesteLance) {
			return;
		}
		if (this.compagnon.distanceToSqr(this.maitre) > DISTANCE * DISTANCE) {
			this.compagnon.getNavigation().moveTo(this.maitre, 1.0D);
			return;
		}

		this.compagnon.getNavigation().stop();
		String role = roleDisponible(this.compagnon);
		if (role == null) {
			this.reste = 0;
			return;
		}
		this.gesteLance = this.compagnon.jouerActionPendant("@" + role, 30,
				PrioriteAction.AFFECTIF);
		if (this.gesteLance && this.compagnon.caractere().calin() >= 0.75F
				&& this.compagnon.complicite() >= 60.0F) {
			Etincelles.coeurs(this.compagnon, 1);
		}
	}

	@Override
	public void stop() {
		this.compagnon.getNavigation().stop();
		this.maitre = null;
		this.gesteLance = false;
	}

	/** Le caractère change le geste, les rôles restent communs aux espèces. */
	static String roleDisponible(CompagnonEntity compagnon) {
		String[] souhaites;
		if (compagnon.caractere().vivacite() >= 0.75F) {
			souhaites = new String[] {Espece.AFFECTION, Espece.JOIE,
					Espece.JOYEUX, Espece.SALUT, Espece.ECOUTE};
		} else if (compagnon.caractere().sociabilite() < 0.3F) {
			souhaites = new String[] {Espece.AFFECTION, Espece.ECOUTE,
					Espece.SALUT, Espece.JOYEUX};
		} else {
			souhaites = new String[] {Espece.AFFECTION, Espece.JOYEUX,
					Espece.SALUT, Espece.ECOUTE};
		}
		return premierRoleDisponible(compagnon, souhaites);
	}

	static String roleAmiDisponible(CompagnonEntity compagnon) {
		return premierRoleDisponible(compagnon, Espece.SCENE_AMI, Espece.JOIE,
				Espece.SALUT, Espece.JOYEUX, Espece.ECOUTE);
	}

	static String premierRoleDisponible(CompagnonEntity compagnon, String... souhaites) {
		Espece espece = Especes.get(compagnon.espece());
		if (espece != null) {
			for (String role : souhaites) {
				if (espece.reaction(role) != null) {
					return role;
				}
			}
		}
		return null;
	}
}
