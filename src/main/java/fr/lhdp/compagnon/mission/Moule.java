package fr.lhdp.compagnon.mission;

import java.util.List;

/**
 * Un moule a missions.
 *
 * <h2>Pourquoi des moules et pas des missions</h2>
 *
 * <p>On veut des milliers de missions. Personne n'en ecrira trois mille a la
 * main, et si quelqu'un le faisait, elles se ressembleraient toutes et il
 * faudrait recompiler pour en corriger une.
 *
 * <p>Un moule est un enonce a trous : « Fais-lui gouter <em>n</em> ×
 * <em>quelque chose</em> ». Quarante moules, une dizaine de valeurs par trou et
 * trois difficultes donnent deja plus de trois mille enonces distincts, tous
 * ecrits une fois.
 *
 * <h2>Comment une mission sait qu'elle est finie</h2>
 *
 * <p>Elle ne surveille rien. Chaque moule nomme un <b>compteur</b> de la fiche,
 * et la mission retient la valeur de ce compteur au moment ou elle est proposee.
 * Sa progression est la difference. C'est tout.
 *
 * <p>Cela veut dire qu'ajouter un type de mission ne demande aucun code de
 * suivi : il suffit qu'un compteur existe et que quelque chose l'incremente.
 * Et cela veut dire aussi qu'une mission ne peut pas etre validee par ce qu'on
 * avait deja fait avant qu'elle n'arrive.
 *
 * @param id        le nom du fichier, sans extension
 * @param compteur  le compteur de la fiche que la mission regarde grandir
 * @param texte     la cle de traduction de l'enonce ; elle recoit la quantite
 *                  puis, s'il y en a un, le parametre
 * @param quantites les quantites possibles, de la plus facile a la plus dure
 * @param xp        l'experience gagnee, par cran de difficulte
 * @param famille   a quoi elle touche, pour eviter d'en proposer trois pareilles
 * @param especes   les especes concernees ; vide = toutes
 * @param longue    vrai si c'est une mission de fond, qui ne se renouvelle pas
 */
public record Moule(String id, String compteur, String texte, List<Integer> quantites,
		List<Integer> xp, String famille, List<String> especes, boolean longue) {

	/** Le moule vaut-il pour cette espece ? */
	public boolean concerne(String espece) {
		return this.especes.isEmpty() || this.especes.contains(espece);
	}

	/**
	 * La quantite demandee au cran de difficulte donne.
	 *
	 * <p>Le cran est ramene dans les bornes plutot que de refuser : un fichier
	 * qui ne declare que deux quantites doit fonctionner, pas planter.
	 */
	public int quantite(int cran) {
		if (this.quantites.isEmpty()) {
			return 1;
		}
		return this.quantites.get(Math.max(0, Math.min(this.quantites.size() - 1, cran)));
	}

	/** L'experience gagnee a ce cran. */
	public int experience(int cran) {
		if (this.xp.isEmpty()) {
			return 0;
		}
		return this.xp.get(Math.max(0, Math.min(this.xp.size() - 1, cran)));
	}

	/** Combien de crans de difficulte ce moule propose. */
	public int crans() {
		return Math.max(1, this.quantites.size());
	}
}
