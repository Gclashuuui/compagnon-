package fr.lhdp.compagnon.espece;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Ou se pose une chose sur une bete.
 *
 * <h2>Le probleme que ca resout</h2>
 *
 * <p>Une balle dans la gueule d'un dragonnet et la meme balle dans la gueule
 * d'une mouette ne sont pas au meme endroit. Ni au meme angle, ni a la meme
 * taille. Vingt especes, vingt modeles, vingt squelettes : il n'existe aucune
 * position qui marche partout, et une animation unique donne un resultat
 * bizarre sur dix-neuf betes sur vingt.
 *
 * <p>On ne cherche donc pas la position universelle. On en garde <b>une par
 * espece et par emplacement</b>, reglee a l'oeil en jeu, et ecrite une fois pour
 * toutes.
 *
 * <h2>Les unites sont celles de l'animateur</h2>
 *
 * <p>Le decalage est en <b>unites de modele</b> — seize font un bloc, c'est la
 * convention Bedrock et celle de Blockbench. Quelqu'un qui a fait le modele lit
 * ces chiffres sans les convertir.
 *
 * <p>Les angles sont en degres, et l'echelle est un facteur : {@code 1} laisse
 * l'objet a sa taille d'origine, {@code 0.5} le reduit de moitie.
 *
 * @param os        le nom de l'os auquel la chose est accrochee. Elle suivra
 *                  toutes ses animations sans qu'on ait rien a calculer.
 * @param x         vers la droite de la bete, en unites de modele
 * @param y         vers le haut
 * @param z         vers l'avant
 * @param tangage   rotation autour de X, en degres
 * @param lacet     rotation autour de Y
 * @param roulis    rotation autour de Z
 * @param echelle   facteur de taille
 */
public record Ancrage(String os, double x, double y, double z,
		float tangage, float lacet, float roulis, float echelle) {

	/**
	 * Le reglage qui sert a tout ce qui n'a pas le sien.
	 *
	 * <p>Les positions sont rangees par <b>objet</b> : une balle et un os n'ont
	 * ni la meme taille ni le meme sens, et les poser au meme endroit donne un
	 * os plante en travers du museau. Chacun a donc sa ligne.
	 *
	 * <p>Mais un compagnon ramasse aussi des steaks, des fleurs et tout ce qui
	 * traine : on ne va pas regler trois cents objets. Celui-ci les recoit
	 * tous, et c'est le seul qu'il faut vraiment poser pour qu'une espece
	 * fonctionne.
	 */
	public static final String DEFAUT = "defaut";

	/** Un accessoire porte, pas un objet tenu. */
	public static final String COLLIER = "collier";

	/**
	 * Ce qu'on prend quand une espece n'a rien dit.
	 *
	 * <p>Pas de position inventee : l'os vide veut dire « pas encore regle », et
	 * le rendu s'abstient plutot que de poser une balle au milieu du ventre. Un
	 * trou visible se corrige ; une approximation se garde des mois.
	 */
	public static final Ancrage ABSENT = new Ancrage("", 0, 0, 0, 0, 0, 0, 1.0F);

	/** Vrai si quelqu'un a vraiment regle cet emplacement-la. */
	public boolean regle() {
		return !this.os.isEmpty();
	}

	// --- Aller et venir en JSON -----------------------------------------------------

	public static Ancrage depuis(JsonObject objet) {
		if (objet == null || !objet.has("os")) {
			return ABSENT;
		}
		double[] d = trois(objet, "decalage", 0.0);
		double[] r = trois(objet, "rotation", 0.0);
		float echelle = objet.has("echelle") ? objet.get("echelle").getAsFloat() : 1.0F;
		return new Ancrage(objet.get("os").getAsString(),
				d[0], d[1], d[2], (float) r[0], (float) r[1], (float) r[2], echelle);
	}

	public JsonObject versJson() {
		JsonObject objet = new JsonObject();
		objet.addProperty("os", this.os);
		objet.add("decalage", tableau(this.x, this.y, this.z));
		objet.add("rotation", tableau(this.tangage, this.lacet, this.roulis));
		objet.addProperty("echelle", this.echelle);
		return objet;
	}

	private static double[] trois(JsonObject objet, String cle, double defaut) {
		double[] valeurs = {defaut, defaut, defaut};
		if (!objet.has(cle) || !objet.get(cle).isJsonArray()) {
			return valeurs;
		}
		JsonArray liste = objet.getAsJsonArray(cle);
		for (int i = 0; i < 3 && i < liste.size(); i++) {
			valeurs[i] = liste.get(i).getAsDouble();
		}
		return valeurs;
	}

	private static JsonArray tableau(double a, double b, double c) {
		JsonArray liste = new JsonArray(3);
		liste.add(arrondi(a));
		liste.add(arrondi(b));
		liste.add(arrondi(c));
		return liste;
	}

	/**
	 * Deux decimales suffisent, et le fichier reste lisible.
	 *
	 * <p>Un editeur qui avance par pas de 0,1 produit sinon des
	 * {@code 0.30000000000000004} partout, et plus personne n'ose ouvrir le
	 * fichier a la main.
	 */
	private static double arrondi(double valeur) {
		return Math.round(valeur * 100.0) / 100.0;
	}
}
