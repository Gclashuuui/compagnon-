package fr.lhdp.compagnon.fiche;

/**
 * Un moment marquant de la vie du compagnon, avec sa date.
 *
 * <p>La cle est libre — {@code premier_vol}, {@code premier_soin},
 * {@code niveau_2}... Le code n'en connait pas la liste : c'est ce qui permet
 * d'en ajouter sans toucher a la sauvegarde.
 *
 * @param cle  ce qui s'est passe
 * @param date le moment, en millisecondes depuis 1970
 */
public record Moment(String cle, long date) {

	/**
	 * Ce qu'on accepte d'oublier quand la liste est pleine, dans cet ordre.
	 *
	 * <p>Les niveaux d'abord : ils sont les plus nombreux et les moins
	 * interessants a relire. Puis les reves, puis les objets goutes.
	 *
	 * <p>Les paysages decouverts en dernier, et a contrecoeur : il y a une
	 * soixantaine de biomes dans le jeu, et un grand voyageur les accumulait
	 * jusqu'a remplir la liste a lui seul — apres quoi plus rien n'etait
	 * remplacable et le plafond ne tenait plus. En oublier un fait au pire
	 * recompter ce paysage une seconde fois.
	 */
	public static final String[] REMPLACABLES = {
			"niveau_", "reve.", "objet_prefere.", "premiere_fois.biome."};

	/**
	 * Combien de moments une fiche garde au plus.
	 *
	 * <p>Cent vingt : de quoi tenir toutes les premieres fois, les manies, le
	 * defaut et une longue serie de niveaux, pour quelques kilo-octets par
	 * compagnon.
	 */
	public static final int PLAFOND = 160;

	/**
	 * Fait de la place dans la liste, et dit si elle en a trouve.
	 *
	 * <h2>Pourquoi il en faut</h2>
	 *
	 * <p>La liste n'avait pas de plafond. Tant que les niveaux s'arretaient a
	 * la derniere ligne de la table, elle etait bornee de fait. Depuis qu'ils
	 * ne s'arretent plus, un compagnon de longue date accumulerait une ligne
	 * par niveau, sans fin, dans la sauvegarde de tout le monde.
	 *
	 * <h2>Ce qu'on ne jette jamais</h2>
	 *
	 * <p>Ni une premiere fois, ni une manie, ni un defaut : ce sont eux qui
	 * font que la bete est elle. Perdre un defaut lui en ferait prendre un
	 * nouveau, et il n'aurait alors plus rien de definitif.
	 *
	 * <p>Si tout est precieux, on ne jette rien et la liste depasse son
	 * plafond. C'est le moindre mal, et ca ne va pas loin : les premieres fois,
	 * les manies et le defaut sont en nombre fini.
	 */
	public static boolean faireDeLaPlace(java.util.List<Moment> moments) {
		for (int i = 0; i < moments.size(); i++) {
			String cle = moments.get(i).cle();
			for (String remplacable : REMPLACABLES) {
				if (cle.startsWith(remplacable)) {
					moments.remove(i);
					return true;
				}
			}
		}
		return false;
	}
}
