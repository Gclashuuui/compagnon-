package fr.lhdp.compagnon.livre;

/**
 * Une mission, telle que le livre l'affiche.
 *
 * <p>Le client ne connait ni les moules ni les compteurs : le serveur lui envoie
 * la phrase deja choisie et les deux nombres. C'est la meme regle que pour tout
 * le reste du livre — il dessine, il ne calcule pas.
 *
 * @param texte    la cle de traduction de l'enonce ; elle recoit la quantite
 * @param quantite ce qu'il faut faire
 * @param faits    ce qui est fait
 * @param finie    vrai quand elle est accomplie
 * @param longue   vrai pour la mission de fond, qui court sur des semaines
 */
public record EntreeMission(String texte, int quantite, int faits, boolean finie,
		boolean longue) {

	/** De zero a un, pour la petite barre. */
	public float part() {
		return this.quantite <= 0 ? 1.0F : Math.min(1.0F, this.faits / (float) this.quantite);
	}
}
