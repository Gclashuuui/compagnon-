package fr.lhdp.compagnon.contenu;

import fr.lhdp.compagnon.fiche.Barre;

import java.util.Map;

/**
 * Un aliment ou un objet de soin : quelque chose qu'on donne au compagnon.
 *
 * <p>Les deux ont la meme forme, seul le dossier change. La base d'un aliment,
 * c'est de remplir la faim ; certains, plus rares, remontent aussi l'energie ou
 * la complicite. C'est le fichier qui le dit, pas le code.
 *
 * @param id     le nom du fichier, sans son extension
 * @param nom    ce qui s'affiche en jeu
 * @param effets ce que l'objet ajoute a chaque barre
 * @param objet  l'objet montre dans la bulle quand il en a envie. N'importe quel
 *               objet du jeu, ou un des notres. Vide : rien a montrer
 */
public record Donnable(String id, String nom, Map<Barre, Float> effets, String objet,
		int modele) {

	public boolean aUnObjet() {
		return this.objet != null && !this.objet.isEmpty();
	}
}
