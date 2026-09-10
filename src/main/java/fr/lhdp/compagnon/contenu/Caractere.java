package fr.lhdp.compagnon.contenu;

/**
 * Le caractere d'un compagnon : son petit cerveau.
 *
 * <p>Cinq penchants, chacun entre zero et un. Ils ne decrivent pas ce que le
 * compagnon <b>fait</b>, mais ce vers quoi il <b>penche</b> — le reste du code
 * s'en sert pour peser ses envies.
 *
 * <p>Le caractere est tire au sort a la naissance et ne change plus. C'est ce
 * qui fait que deux dragonnets de meme espece et de meme couleur ne se
 * comportent pas pareil.
 *
 * <p>Ajouter un caractere, c'est ajouter un fichier dans
 * {@code data/compagnon/caracteres/}.
 *
 * @param id           le nom du fichier, sans extension
 * @param nom          ce qui s'affiche dans le livre
 * @param sociabilite  va vers les inconnus, ou les evite
 * @param attachement  reste pres de son maitre, ou vagabonde
 * @param vivacite     bouge souvent, ou se pose
	 * @param calin        vient reclamer des caresses, reste près du joueur
 * @param curiosite    va voir ce qui bouge
 */
public record Caractere(
		String id,
		String nom,
		float sociabilite,
		float attachement,
		float vivacite,
		float calin,
		float curiosite) {

	/** Le caractere de repli quand aucun fichier n'est lisible : la moyenne. */
	public static final Caractere ORDINAIRE =
			new Caractere("ordinaire", "ordinaire", 0.5F, 0.5F, 0.5F, 0.5F, 0.5F);

	/**
	 * Melange un reglage de base et un penchant.
	 *
	 * <p>Un trait a 0,5 ne change rien ; a 1 il double la valeur, a 0 il la
	 * reduit de moitie. Cela evite d'avoir a ecrire une formule differente pour
	 * chaque comportement.
	 */
	public static float peser(float base, float trait) {
		return base * (0.5F + trait);
	}

	/**
	 * Courage dérivé des mêmes cinq traits : aucune personnalité sauvegardée ne
	 * change de format. Une bête vive et sociable affronte plus facilement ce
	 * qu'elle ne connaît pas ; une bête calme ou timide cherche son maître.
	 */
	public float courage() {
		return Math.max(0.0F,
				Math.min(1.0F, this.vivacite * 0.65F + this.sociabilite * 0.35F));
	}

	/** La patience est l'autre lecture de la vivacité existante. */
	public float patience() {
		return 1.0F - this.vivacite;
	}
}
