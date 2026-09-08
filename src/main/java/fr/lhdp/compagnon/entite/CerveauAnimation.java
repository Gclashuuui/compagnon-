package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.espece.Espece;
import fr.lhdp.compagnon.fiche.Humeur;
import fr.lhdp.compagnon.fiche.Mode;

import java.util.function.Predicate;

/**
 * L'arbre de décision visuel commun à toutes les espèces.
 *
 * <p>Les nœuds sont parcourus dans un ordre strict. Le premier qui correspond
 * gagne et aucun autre ne peut jouer une locomotion contradictoire au même
 * instant. Les fichiers d'espèce ne décident toujours que des animations qui
 * incarnent les rôles : le cerveau ne contient aucun nom propre au dragon.
 */
public final class CerveauAnimation {

	private static final float COURSE_ENTREE = 0.68F;
	private static final float COURSE_SORTIE = 0.46F;
	private static final double PLANE_ENTREE = -0.075D;
	private static final double PLANE_SORTIE = -0.015D;

	/** La branche qui a pris la décision, utile au diagnostic et à la future UI. */
	public enum Noeud {
		EAU,
		AIR,
		SOMMEIL,
		ASSIS,
		COUCHE,
		DEPLACEMENT,
		HUMEUR
	}

	/** Tout ce que le cerveau observe sur cette image, sans dépendre d'une espèce. */
	public record Observation(boolean dansLEau, boolean enVol, boolean dort,
			Mode mode, boolean avance, float allure, double vitesseVerticale, Humeur humeur) {
	}

	/** Le résultat unique de l'arbre. */
	public record Decision(Noeud noeud, String role) {
	}

	private boolean course;
	private boolean plane;

	/**
	 * Choisit exactement un rôle. {@code connu} permet de retomber sur une
	 * animation universelle lorsqu'une espèce n'a pas encore un rôle spécialisé.
	 */
	public Decision choisir(Observation vue, Predicate<String> connu) {
		if (vue.dansLEau()) {
			reinitialiserSolEtAir();
			String role = vue.avance() && connu.test(Espece.NAGE)
					? Espece.NAGE
					: !vue.avance() && connu.test(Espece.FLOTTE)
							? Espece.FLOTTE : vue.avance() ? Espece.MARCHE : Espece.IMMOBILE;
			return new Decision(Noeud.EAU, role);
		}

		if (vue.enVol()) {
			this.course = false;
			this.plane = this.plane
					? vue.vitesseVerticale() < PLANE_SORTIE
					: vue.vitesseVerticale() < PLANE_ENTREE;
			String role = this.plane && connu.test(Espece.PLANE)
					? Espece.PLANE : Espece.VOL;
			return new Decision(Noeud.AIR, role);
		}

		this.plane = false;
		if (vue.dort()) {
			this.course = false;
			return new Decision(Noeud.SOMMEIL, Espece.COUCHE);
		}
		if (vue.mode() == Mode.ASSIS) {
			this.course = false;
			return new Decision(Noeud.ASSIS, Espece.ASSIS);
		}
		if (vue.mode() == Mode.COUCHE) {
			this.course = false;
			return new Decision(Noeud.COUCHE, Espece.COUCHE);
		}

		if (vue.avance()) {
			this.course = this.course
					? vue.allure() > COURSE_SORTIE
					: vue.allure() > COURSE_ENTREE;
			String role = this.course && connu.test(Espece.COURSE)
					? Espece.COURSE : Espece.MARCHE;
			return new Decision(Noeud.DEPLACEMENT, role);
		}

		this.course = false;
		String humeur = switch (vue.humeur()) {
			case MISERE, TRISTE -> Espece.TRISTE;
			case RAYONNANT -> Espece.JOYEUX;
			default -> Espece.IMMOBILE;
		};
		return new Decision(Noeud.HUMEUR,
				connu.test(humeur) ? humeur : Espece.IMMOBILE);
	}

	private void reinitialiserSolEtAir() {
		this.course = false;
		this.plane = false;
	}
}
