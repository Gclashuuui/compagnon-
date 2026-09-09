package fr.lhdp.compagnon.entite;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;

/**
 * L'ordre unique des nœuds du comportement de tous les compagnons.
 *
 * <p>Le sélecteur de Minecraft est déjà un arbre de priorités, mais des nombres
 * éparpillés dans l'entité rendaient sa logique invisible et permettaient à deux
 * intentions contradictoires d'avoir le même rang. Ici, chaque nœud de mouvement
 * a une place unique : un ordre gagne sur un besoin, un besoin sur une habitude,
 * une habitude sur la curiosité, et la flânerie arrive toujours en dernier.
 */
public final class CerveauComportement {

	public enum Noeud {
		SURVIE(0),
		RAPPEL(1),
		DESTINATION_DEMANDEE(2),
		VOL_DEMANDE(3),
		OBJET_DEMANDE(4),
		JEU_DEMANDE(5),
		SUIVRE(6),
		ABRI(7),
		REPAS(8),
		SOMMEIL(9),
		HABITUDE(10),
		AFFECTION(11),
		CURIOSITE(12),
		CADEAU_SPONTANE(13),
		JOUEUR(14),
		AMI(15),
		SOUVENIR(16),
		SALUT(17),
		FLANERIE(18),
		REGARD_JOUEUR(19),
		REGARD_OBJET(20),
		REGARD_LIBRE(21),
		/** Aucun but ne deplace la bete : elle profite simplement du moment. */
		REPOS(22);

		private final int priorite;

		Noeud(int priorite) {
			this.priorite = priorite;
		}

		public int priorite() {
			return this.priorite;
		}

		public static Noeud depuisPriorite(int priorite) {
			for (Noeud noeud : values()) {
				if (noeud.priorite == priorite) {
					return noeud;
				}
			}
			return REPOS;
		}
	}

	private CerveauComportement() {
	}

	public static void ajouter(GoalSelector selecteur, Noeud noeud, Goal comportement) {
		selecteur.addGoal(noeud.priorite(), comportement);
	}

	/** Ajoute un but derrière le filtre dynamique du profil de l'espèce. */
	public static void ajouter(GoalSelector selecteur, Noeud noeud,
			CompagnonEntity compagnon, Goal comportement) {
		Goal filtre = new Goal() {
			{
				setFlags(comportement.getFlags());
			}

			private boolean autorise() {
				return compagnon.profilCerveau().autorise(noeud);
			}

			@Override public boolean canUse() {
				return autorise() && comportement.canUse();
			}

			@Override public boolean canContinueToUse() {
				return autorise() && comportement.canContinueToUse();
			}

			@Override public void start() { comportement.start(); }
			@Override public void stop() { comportement.stop(); }
			@Override public void tick() { comportement.tick(); }
			@Override public boolean requiresUpdateEveryTick() {
				return comportement.requiresUpdateEveryTick();
			}
			@Override public boolean isInterruptable() {
				return comportement.isInterruptable();
			}
		};
		ajouter(selecteur, noeud, filtre);
	}

	/**
	 * Le nœud qui conduit réellement la bete à cet instant.
	 *
	 * <p>Plusieurs buts de regard peuvent tourner en même temps qu'un but de
	 * déplacement. On retient donc le rang le plus fort, exactement comme le
	 * sélecteur de Minecraft. L'interface ne raconte ainsi jamais « il regarde
	 * autour de lui » pendant qu'il court répondre à un rappel.
	 */
	public static Noeud actif(GoalSelector selecteur) {
		return selecteur.getAvailableGoals().stream()
				.filter(net.minecraft.world.entity.ai.goal.WrappedGoal::isRunning)
				.min(java.util.Comparator.comparingInt(
						net.minecraft.world.entity.ai.goal.WrappedGoal::getPriority))
				.map(but -> Noeud.depuisPriorite(but.getPriority()))
				.orElse(Noeud.REPOS);
	}
}
