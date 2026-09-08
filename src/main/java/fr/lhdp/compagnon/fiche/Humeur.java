package fr.lhdp.compagnon.fiche;

/**
 * L'humeur n'est pas une barre : c'est un <b>resultat</b>.
 *
 * <p>On ne la stocke pas et on ne la remplit pas. Elle se deduit des cinq barres,
 * ce qui garantit qu'elle a toujours une cause, et que la cause est visible juste
 * a cote dans le livre.
 *
 * <p>Consequence voulue : <b>il ne peut jamais etre rayonnant alors qu'il creve
 * de faim.</b> Une barre au plus bas plafonne l'humeur, quoi que disent les
 * autres.
 *
 * <p>Tous les chiffres de cette classe sont inventes et a regler en jeu.
 */
public enum Humeur {

	MISERE("au plus mal", "T_T"),
	TRISTE("triste", ":,("),
	MOYEN("comme ci comme ca", ":|"),
	CONTENT("content", ":)"),
	RAYONNANT("rayonnant", "^_^");

	/** Poids de chaque barre dans le calcul. Leur somme fait 1. */
	private static final float POIDS_FAIM = 0.30F;
	private static final float POIDS_ENERGIE = 0.20F;
	private static final float POIDS_SANTE = 0.20F;
	private static final float POIDS_COMPLICITE = 0.30F;

	/** En dessous de ce niveau, une barre est consideree comme critique. */
	private static final float SEUIL_CRITIQUE = 25.0F;

	/** Avec une barre critique, l'humeur ne peut pas depasser ce score. */
	private static final float PLAFOND_CRITIQUE = 35.0F;

	private static final float SEUIL_TRISTE = 20.0F;
	private static final float SEUIL_MOYEN = 40.0F;
	private static final float SEUIL_CONTENT = 60.0F;
	private static final float SEUIL_RAYONNANT = 80.0F;

	private final String libelle;
	private final String bouille;

	Humeur(String libelle, String bouille) {
		this.libelle = libelle;
		this.bouille = bouille;
	}

	/** En toutes lettres, pas en jauge. */
	public String libelle() {
		return this.libelle;
	}

	public String bouille() {
		return this.bouille;
	}

	/**
	 * Calcule l'humeur a partir des barres d'une fiche.
	 *
	 * <p>La complicite tire vers le haut, mais elle ne rachete pas un ventre vide :
	 * la faim, l'energie et la sante peuvent plafonner le resultat a elles seules.
	 */
	public static Humeur calculer(FicheCompagnon fiche) {
		float faim = fiche.barre(Barre.FAIM);
		float energie = fiche.barre(Barre.ENERGIE);
		float sante = fiche.barre(Barre.SANTE);
		float complicite = fiche.barre(Barre.COMPLICITE);

		float score = faim * POIDS_FAIM
				+ energie * POIDS_ENERGIE
				+ sante * POIDS_SANTE
				+ complicite * POIDS_COMPLICITE;

		// Un besoin non satisfait plafonne l'humeur.
		float pire = Math.min(faim, Math.min(energie, sante));
		if (pire < SEUIL_CRITIQUE) {
			score = Math.min(score, PLAFOND_CRITIQUE);
		}

		if (score < SEUIL_TRISTE) {
			return MISERE;
		}
		if (score < SEUIL_MOYEN) {
			return TRISTE;
		}
		if (score < SEUIL_CONTENT) {
			return MOYEN;
		}
		if (score < SEUIL_RAYONNANT) {
			return CONTENT;
		}
		return RAYONNANT;
	}
}
