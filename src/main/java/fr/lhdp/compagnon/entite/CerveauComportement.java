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
		CURIOSITE(11),
		CADEAU_SPONTANE(12),
		JOUEUR(13),
		AMI(14),
		SOUVENIR(15),
		SALUT(16),
		FLANERIE(17),
		REGARD_JOUEUR(18),
		REGARD_OBJET(19),
		REGARD_LIBRE(20);

		private final int priorite;

		Noeud(int priorite) {
			this.priorite = priorite;
		}

		public int priorite() {
			return this.priorite;
		}
	}

	private CerveauComportement() {
	}

	public static void ajouter(GoalSelector selecteur, Noeud noeud, Goal comportement) {
		selecteur.addGoal(noeud.priorite(), comportement);
	}
}
