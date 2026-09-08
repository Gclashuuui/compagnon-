package fr.lhdp.compagnon.livre;

/**
 * Une competence, vue depuis le livre.
 *
 * <p>Tout ce qu'il faut pour la dessiner et savoir si on peut la prendre — et
 * rien de plus. Les effets restent cote serveur : le client n'a pas a savoir ce
 * qu'une competence fait pour l'afficher, et lui envoyer les regles ne servirait
 * qu'a lui permettre de mentir dessus.
 *
 * @param id           l'identifiant, pour la demander au serveur
 * @param nom          ce qu'on lit
 * @param description  ce qu'elle fait, en une phrase
 * @param niveauRequis le palier a partir duquel elle s'ouvre
 * @param prise        vrai s'il l'a deja
 */
public record EntreeCompetence(String id, String nom, String description,
		int niveauRequis, boolean prise) {
}
