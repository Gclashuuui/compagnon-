package fr.lhdp.compagnon.livre;

/**
 * Une ligne du carnet : un compagnon, vu de loin.
 *
 * <p>Juste assez pour le reconnaitre et le dessiner. Tout le reste — ses barres,
 * ses souvenirs, ses bobos — est dans le livre, et n'a rien a faire ici : le
 * carnet sert a choisir, pas a s'informer.
 *
 * @param nom      son nom
 * @param espece   pour retrouver son modele et ses animations
 * @param variante pour retrouver sa texture
 * @param niveau   son niveau, seul chiffre affiche
 * @param sorti    vrai s'il est dehors, faux s'il est range
 * @param ou       une phrase courte disant ou il se trouve
 */
public record EntreeCarnet(String nom, String espece, String variante, int niveau,
		boolean sorti, String ou) {
}
