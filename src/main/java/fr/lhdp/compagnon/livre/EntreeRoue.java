package fr.lhdp.compagnon.livre;

/**
 * Une case de la roue.
 *
 * <p>Ce qui n'est pas encore ouvert est envoye quand meme, avec
 * {@code debloque} a faux : la roue montre le cadenas plutot que de cacher la
 * case. Montrer ce qui existe encore fait plus pour l'envie que de le cacher.
 *
 * <p>Le nom est celui que la table des niveaux donne. Le code ne le connait pas
 * et ne le juge pas : il le transporte.
 *
 * @param nom           le nom d'animation, tel qu'ecrit dans la table
 * @param niveauRequis  le niveau qui l'ouvre
 * @param debloque      vrai si le compagnon l'a atteint
 * @param mot           le mot appris pour ce geste, ou une chaine vide
 */
public record EntreeRoue(String nom, int niveauRequis, boolean debloque, String mot) {

	/** Vrai si le joueur lui a appris un mot pour ce geste. */
	public boolean aUnMot() {
		return this.mot != null && !this.mot.isEmpty();
	}
}
