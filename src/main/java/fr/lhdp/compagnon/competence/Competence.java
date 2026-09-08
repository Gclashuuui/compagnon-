package fr.lhdp.compagnon.competence;

import java.util.List;
import java.util.Map;

/**
 * Une chose que le compagnon a apprise a etre, et qu'un autre n'a pas.
 *
 * <h2>Pourquoi ce n'est pas une animation de plus</h2>
 *
 * <p>Le niveau ne debloquait que des <b>gestes</b> : joli, mais ca ne change
 * rien a ce que la bete <i>est</i>. Deux dragons de niveau 50 etaient
 * rigoureusement identiques, et le niveau 30 n'apportait rien du tout s'il n'y
 * avait pas d'animation a ce palier.
 *
 * <p>Une competence, elle, change son comportement — et comme il y a plus de
 * competences que de points a depenser, deux betes du meme niveau ne se
 * ressemblent plus. <b>C'est le choix qui les rend siennes</b>, pas le niveau.
 *
 * <h2>Rien n'est ecrit dans le code</h2>
 *
 * <p>Le nom, la description, le palier, l'espece et les effets viennent tous du
 * fichier. Ajouter une competence, c'est ajouter un fichier et faire
 * {@code /reload} — comme pour un aliment ou un soin.
 *
 * @param id          le nom du fichier, sans l'extension
 * @param nom         ce qu'on lit dans le livre
 * @param description ce qu'elle fait, en une phrase
 * @param niveauRequis le palier a partir duquel on peut la prendre
 * @param especes     les especes concernees ; vide veut dire toutes
 * @param effets      les reglages qu'elle modifie, par leur cle
 */
public record Competence(
		String id,
		String nom,
		String description,
		int niveauRequis,
		List<String> especes,
		Map<String, Float> effets) {

	// --- Les cles d'effet que le code sait lire ---------------------------------
	//
	// Une cle inconnue dans un fichier n'est pas une erreur : elle est simplement
	// sans effet, et le journal le dit. C'est ce qui permet d'ecrire les fichiers
	// avant le code qui les utilisera.

	/** Multiplie la vitesse a laquelle la faim descend. Moins de 1 = il tient plus. */
	public static final String FAIM = "faim";

	/** Multiplie la vitesse a laquelle l'energie descend. */
	public static final String ENERGIE = "energie";

	/** Multiplie le risque d'attraper un bobo. */
	public static final String BOBOS = "bobos";

	/** Multiplie l'experience gagnee. */
	public static final String EXPERIENCE = "experience";

	/** Ajoute des blocs a la portee de sa voix. Un bonus, pas un facteur. */
	public static final String OREILLE = "oreille";

	/** Toutes les cles que le code sait lire, pour prevenir des autres. */
	/**
	 * Il ne se contente pas de prevenir : il <b>montre</b>.
	 *
	 * <p>Toutes les competences du mod etaient des nombres — un peu moins de
	 * faim, un peu plus d'experience. On les prenait sans rien voir changer.
	 * Celle-ci fait quelque chose : la creature qu'il signale s'entoure d'un
	 * lisere visible a travers les murs.
	 *
	 * <p>Au-dessus de zero = active. Ce n'est pas une quantite.
	 */
	public static final String REVELE = "revele";

	/**
	 * Il veille aussi en plein jour.
	 *
	 * <p>Sans elle, il ne previent que dans le noir : c'est ce qui l'empeche de
	 * commenter chaque zombie qui brule au soleil. Avec, il previent partout —
	 * ce qui vaut le coup dans une cave eclairee ou une cour a l'aube.
	 */
	public static final String VEILLE = "veille";

	public static final List<String> CLES_CONNUES =
			List.of(FAIM, ENERGIE, BOBOS, EXPERIENCE, OREILLE, REVELE, VEILLE);

	/** Vrai si cette espece peut prendre cette competence. */
	public boolean pour(String espece) {
		return this.especes.isEmpty() || this.especes.contains(espece);
	}

	/**
	 * La valeur d'un effet, ou celle par defaut si cette competence ne le touche
	 * pas.
	 */
	public float effet(String cle, float parDefaut) {
		Float valeur = this.effets.get(cle);
		return valeur == null ? parDefaut : valeur;
	}
}
