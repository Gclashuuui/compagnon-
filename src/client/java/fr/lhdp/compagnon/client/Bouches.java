package fr.lhdp.compagnon.client;

import fr.lhdp.compagnon.Compagnon;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ou est la bouche d'un modele ?
 *
 * <h2>Le probleme</h2>
 *
 * <p>Quand un compagnon porte un objet, il faut le dessiner <b>dans sa gueule</b>.
 * La premiere version le posait a un decalage calcule depuis la boite de
 * collision de sa tete : ca donnait un objet qui flotte devant lui, et ca ne
 * bougeait pas quand il baissait la tete.
 *
 * <p>Et chaque creature a une bouche differente. Une mouette n'a pas le bec au
 * meme endroit qu'un dragonnet a la gueule. Un decalage ecrit a la main serait
 * a refaire pour chacune, et faux la moitie du temps.
 *
 * <h2>La solution</h2>
 *
 * <p>On cherche <b>l'os</b> de la bouche dans le modele, et on accroche l'objet
 * dessus. Il suit alors les animations tout seul : quand la machoire s'ouvre,
 * l'objet bouge avec.
 *
 * <p>Les modeles viennent de partout, et personne ne nomme ses os pareil. On
 * accepte donc plusieurs langues et plusieurs mots, et on <b>note dans le log</b>
 * l'os retenu — pour qu'on puisse verifier au lieu de deviner.
 *
 * <p>Si aucun nom ne ressemble a une bouche, on se rabat sur la tete, puis sur
 * la racine du modele. Un objet mal place vaut mieux qu'un objet invisible, et
 * le log dit quoi renommer.
 */
public final class Bouches {

	/**
	 * Les mots qui designent vraiment une bouche, dans les langues qu'on croise.
	 *
	 * <p>Sans accents et en minuscules : les noms d'os sont compares apres la meme
	 * mise en forme, donc « Mâchoire » et « machoire » se valent.
	 */
	private static final List<String> MOTS_BOUCHE = List.of(
			// Francais
			"bouche", "machoire", "machoirebasse", "machoirehaute", "mandibule",
			"gueule", "bec", "levre", "levres", "langue", "dents", "menton",
			// Anglais — de loin le plus frequent dans les modeles partages
			"mouth", "jaw", "jawbone", "lowerjaw", "jawlower", "upperjaw", "jawupper",
			"bottomjaw", "jawbottom", "topjaw", "jawtop", "mandible", "maxilla",
			"beak", "beaklower", "beakupper", "lowerbeak", "upperbeak", "bill",
			"chin", "lip", "lips", "teeth", "tooth", "tongue", "fangs", "fang",
			"maw", "jaws",
			// Autres langues qu'on croise sur les modeles publies
			"boca", "bocca", "mund", "kiefer", "unterkiefer", "schnabel", "maul",
			"mond", "kaak", "pico", "becco", "mascella");

	/**
	 * Le devant du museau : moins juste qu'une machoire, bien mieux qu'une tete.
	 *
	 * <p>Un modele sans os de machoire a souvent un os de museau, et c'est deja
	 * pratiquement le bon endroit.
	 */
	private static final List<String> MOTS_MUSEAU = List.of(
			"museau", "muzzle", "snout", "nose", "naseau", "naseaux", "nostril",
			"nariz", "nase", "schnauze", "grognon", "groin", "truffe");

	/** A defaut, la tete. C'est deja bien plus juste que rien. */
	private static final List<String> MOTS_TETE = List.of(
			"tete", "head", "kopf", "cabeza", "testa", "crane", "skull", "cranium",
			"cabeca", "hoofd", "huvud");

	/** Le nom trouve pour chaque modele, pour ne chercher qu'une fois. */
	private static final Map<String, String> retenus = new ConcurrentHashMap<>();

	private Bouches() {
	}

	/**
	 * Le nom de l'os ou poser ce qu'il porte.
	 *
	 * @param espece pour ne chercher qu'une fois par espece, et pour le log
	 * @return le nom de l'os, ou une chaine vide si le modele n'a aucun os
	 */
	public static String de(String espece, BakedGeoModel modele) {
		String deja = retenus.get(espece);
		if (deja != null) {
			return deja;
		}
		String trouve = chercher(modele);
		retenus.put(espece, trouve);

		if (trouve.isEmpty()) {
			Compagnon.LOG.warn("Espece {} : aucun os trouve dans le modele, "
					+ "ce qu'elle porte ne s'affichera pas.", espece);
		} else {
			Compagnon.LOG.info("Espece {} : ce qu'elle porte ira sur l'os \"{}\". "
					+ "Pour en choisir un autre, renommez-le \"bouche\" dans Blockbench.",
					espece, trouve);
		}
		return trouve;
	}



	private static String chercher(BakedGeoModel modele) {
		Meilleur meilleur = new Meilleur();
		for (GeoBone os : modele.topLevelBones()) {
			parcourir(os, meilleur);
		}
		return meilleur.nom;
	}

	private static void parcourir(GeoBone os, Meilleur meilleur) {
		int note = noter(os.getName());
		if (note > meilleur.note) {
			meilleur.note = note;
			meilleur.nom = os.getName();
		}
		for (GeoBone enfant : os.getChildBones()) {
			parcourir(enfant, meilleur);
		}
	}

	/**
	 * A quel point ce nom d'os ressemble a une bouche.
	 *
	 * <p>Un nom qui EST le mot vaut mieux qu'un nom qui le contient : entre
	 * « bouche » et « bouche_haut », on prend le premier. Et une vraie bouche vaut
	 * toujours mieux qu'une tete.
	 *
	 * <p>Le dernier recours vaut 1 : la racine du modele. Ce n'est pas juste, mais
	 * c'est visible, et le log dit quoi renommer.
	 */
	private static int noter(String nom) {
		String propre = simplifier(nom);
		if (propre.isEmpty()) {
			return 0;
		}
		for (String mot : MOTS_BOUCHE) {
			if (propre.equals(mot)) {
				return 100;
			}
		}
		for (String mot : MOTS_BOUCHE) {
			if (propre.contains(mot)) {
				return 80;
			}
		}
		for (String mot : MOTS_MUSEAU) {
			if (propre.equals(mot)) {
				return 60;
			}
		}
		for (String mot : MOTS_MUSEAU) {
			if (propre.contains(mot)) {
				return 45;
			}
		}
		for (String mot : MOTS_TETE) {
			if (propre.equals(mot)) {
				return 40;
			}
		}
		for (String mot : MOTS_TETE) {
			if (propre.contains(mot)) {
				return 25;
			}
		}
		return 1;
	}

	/**
	 * Minuscules, sans accents, sans separateurs.
	 *
	 * <p>« Mâchoire_Basse », « machoireBasse » et « MACHOIRE-BASSE » deviennent le
	 * meme mot. Personne ne nomme ses os de la meme facon, et ce n'est pas au
	 * modeleur de s'adapter au code.
	 */
	static String simplifier(String nom) {
		if (nom == null) {
			return "";
		}
		String sansAccent = Normalizer.normalize(nom, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");
		return sansAccent.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
	}

	/** Le meilleur candidat trouve pendant le parcours. */
	private static final class Meilleur {
		private int note;
		private String nom = "";
	}
}
