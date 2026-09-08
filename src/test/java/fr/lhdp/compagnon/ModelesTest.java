package fr.lhdp.compagnon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les modeles et leurs animations se tiennent-ils entre eux ?
 *
 * <h2>Pourquoi ce fichier existe</h2>
 *
 * <p>Un os du kobeko se declarait enfant d'un os nomme {@code root} que son
 * export n'avait pas ecrit. GeckoLib refuse ce modele-la, et il ne le refuse pas
 * poliment : il jette au milieu du chargement des ressources, le jeu reste
 * bloque sur l'ecran Mojang, et le log accuse d'abord trois cents avertissements
 * d'un autre mod avant d'en venir a la vraie ligne.
 *
 * <p>La faute etait dans un fichier livre par quelqu'un d'autre, elle tenait en
 * un mot, et elle a coute un lancement entier. Elle se voit ici en une seconde.
 *
 * <h2>Les quatre pannes silencieuses</h2>
 *
 * <p>Trois des quatre verifications ci-dessous ne font <b>rien</b> lever en jeu :
 *
 * <ul>
 *   <li>un os anime qui n'existe pas dans le modele est ignore sans un mot —
 *       l'animation se joue, mais la moitie de la bete ne bouge pas ;</li>
 *   <li>une espece qui nomme une animation absente de son fichier se joue en
 *       silence : c'est comme ca que les huit bebes dragons sont sortis muets et
 *       immobiles, parce que leurs animations s'appelaient
 *       {@code animation.atroxiia.idle} et non {@code animation.baby_atroxiia.idle} ;</li>
 *   <li>deux os de meme nom : GeckoLib en garde un, au hasard.</li>
 * </ul>
 *
 * <p>Aucun ne demande Minecraft pour etre vu. On lit les fichiers comme le jeu
 * les lira.
 */
class ModelesTest {

	private static final Path ASSETS = Path.of("src/main/resources/assets/compagnon");
	private static final Path ESPECES = Path.of("src/main/resources/data/compagnon/especes");

	/** Les deux tableaux d'une espece ou se nomment des animations. */
	private static final List<String> ROLES = List.of("locomotion", "reactions");

	// --- Ce qui empeche le jeu de demarrer -----------------------------------------

	/**
	 * L'erreur exacte qui a bloque le jeu sur l'ecran Mojang.
	 *
	 * <p>GeckoLib construit l'arbre des os avant de dessiner quoi que ce soit. Un
	 * parent introuvable n'est pas une branche manquante : c'est une exception qui
	 * remonte jusqu'au rechargement des ressources et arrete tout.
	 */
	@Test
	void chaqueOsNommeUnParentQuiExiste() throws IOException {
		List<String> fautes = new ArrayList<>();

		for (Path modele : fichiers(ASSETS.resolve("geo"))) {
			Set<String> presents = osDe(modele);
			for (JsonObject os : lesOs(modele)) {
				if (!os.has("parent")) {
					continue;
				}
				String parent = os.get("parent").getAsString();
				if (!presents.contains(parent)) {
					fautes.add(modele.getFileName() + " : l'os \"" + nom(os)
							+ "\" se dit enfant de \"" + parent + "\", qui n'est pas dans le fichier"
							+ "\n\t\t(GeckoLib refuse ce modele et bloque le chargement du jeu)");
				}
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	// --- Ce qui casse en silence ---------------------------------------------------

	/** Deux os de meme nom : GeckoLib en garde un, et on ne sait pas lequel. */
	@Test
	void deuxOsNePortentPasLeMemeNom() throws IOException {
		List<String> fautes = new ArrayList<>();

		for (Path modele : fichiers(ASSETS.resolve("geo"))) {
			Set<String> vus = new HashSet<>();
			for (JsonObject os : lesOs(modele)) {
				if (!vus.add(nom(os))) {
					fautes.add(modele.getFileName() + " : deux os s'appellent \"" + nom(os) + "\"");
				}
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	/**
	 * Chaque os anime existe-t-il dans le modele de l'espece ?
	 *
	 * <p>Un nom d'os mal orthographie dans une animation ne leve rien du tout. La
	 * bete joue son animation, et le morceau concerne reste raide.
	 */
	@Test
	void chaqueOsAnimeExisteDansLeModele() throws IOException {
		List<String> fautes = new ArrayList<>();

		for (Path espece : fichiers(ESPECES)) {
			JsonObject fiche = lire(espece);
			Path modele = ressource(fiche, "geometrie");
			Path animations = ressource(fiche, "animations");
			if (!Files.exists(modele) || !Files.exists(animations)) {
				continue;   // dit par le test suivant, inutile de le repeter
			}
			Set<String> presents = osDe(modele);

			for (Map.Entry<String, JsonElement> anim : lire(animations)
					.getAsJsonObject("animations").entrySet()) {
				JsonObject os = anim.getValue().getAsJsonObject().getAsJsonObject("bones");
				if (os == null) {
					continue;
				}
				Set<String> absents = new LinkedHashSet<>();
				for (String nom : os.keySet()) {
					if (!presents.contains(nom)) {
						absents.add(nom);
					}
				}
				if (!absents.isEmpty()) {
					fautes.add(sansExtension(espece) + " : \"" + anim.getKey()
							+ "\" anime des os que " + modele.getFileName()
							+ " n'a pas : " + String.join(", ", absents));
				}
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	/**
	 * Chaque animation nommee par une espece existe-t-elle vraiment ?
	 *
	 * <p>C'est la panne des huit bebes dragons : leurs fiches nommaient
	 * {@code animation.baby_atroxiia.idle} alors que le fichier declarait
	 * {@code animation.atroxiia.idle}. Les huit especes sont sorties completement
	 * immobiles, et rien nulle part ne l'a dit.
	 */
	@Test
	void chaqueAnimationNommeeParUneEspeceExiste() throws IOException {
		List<String> fautes = new ArrayList<>();

		for (Path espece : fichiers(ESPECES)) {
			JsonObject fiche = lire(espece);
			Path modele = ressource(fiche, "geometrie");
			Path animations = ressource(fiche, "animations");

			if (!Files.exists(modele)) {
				fautes.add(sansExtension(espece) + " : modele introuvable — " + modele);
			}
			if (!Files.exists(animations)) {
				fautes.add(sansExtension(espece) + " : animations introuvables — " + animations);
				continue;
			}
			Set<String> connues = lire(animations).getAsJsonObject("animations").keySet();

			for (String role : ROLES) {
				JsonObject table = fiche.getAsJsonObject(role);
				if (table == null) {
					continue;
				}
				for (Map.Entry<String, JsonElement> ligne : table.entrySet()) {
					String voulue = ligne.getValue().getAsString();
					// Un role vide est un role qu'on assume : il ne joue rien,
					// ce qui vaut mieux que jouer de travers.
					if (!voulue.isEmpty() && !connues.contains(voulue)) {
						fautes.add(sansExtension(espece) + " : " + role + "." + ligne.getKey()
								+ " demande \"" + voulue + "\", absente de "
								+ animations.getFileName());
					}
				}
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	/**
	 * Chaque ancrage nomme-t-il un os qui existe ?
	 *
	 * <p>Un objet accroche a un os inexistant ne s affiche <b>pas</b>, et rien
	 * nulle part ne le signale : ni erreur, ni ligne de journal. La balle est
	 * simplement invisible dans la gueule, ce qui ressemble exactement a la
	 * panne qu on vient de corriger — sauf que la cause est une lettre.
	 *
	 * <p>Ce test lit le fichier livre avec le mod. Celui que l equipe ecrit en
	 * jeu ne peut pas etre verifie ici, mais il est ecrit par un editeur qui
	 * fait defiler les os reels du modele : on n y tape jamais un nom.
	 */
	@Test
	void chaqueAncrageNommeUnOsQuiExiste() throws IOException {
		Path fichier = Path.of("src/main/resources/data/compagnon/ancrages.json");
		if (!Files.exists(fichier)) {
			return;
		}
		List<String> fautes = new ArrayList<>();
		JsonObject racine = lire(fichier);

		for (String espece : racine.keySet()) {
			if (espece.startsWith("_")) {
				continue;
			}
			Path fiche = ESPECES.resolve(espece + ".json");
			if (!Files.exists(fiche)) {
				fautes.add("ancrages.json parle de \"" + espece + "\", qui n est pas une espece");
				continue;
			}
			Path modele = ressource(lire(fiche), "geometrie");
			if (!Files.exists(modele)) {
				continue;
			}
			Set<String> presents = osDe(modele);
			JsonObject emplacements = racine.getAsJsonObject(espece);
			for (String emplacement : emplacements.keySet()) {
				if (emplacement.startsWith("_")) {
					continue;
				}
				JsonObject un = emplacements.getAsJsonObject(emplacement);
				if (un == null || !un.has("os")) {
					fautes.add(espece + "." + emplacement + " ne dit pas sur quel os");
					continue;
				}
				String os = un.get("os").getAsString();
				if (!presents.contains(os)) {
					fautes.add(espece + "." + emplacement + " est accroche a l os \"" + os
						+ "\", que " + modele.getFileName() + " n a pas"
						+ "\n\t\t(l objet serait invisible, sans une ligne nulle part)");
				}
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	// --- Outils ---------------------------------------------------------------------

	/** Tous les os d'un modele, a plat : GeckoLib les lit deja comme ca. */
	private static List<JsonObject> lesOs(Path modele) throws IOException {
		List<JsonObject> tous = new ArrayList<>();
		JsonArray geometries = lire(modele).getAsJsonArray("minecraft:geometry");
		if (geometries == null) {
			return tous;
		}
		for (JsonElement geometrie : geometries) {
			JsonArray os = geometrie.getAsJsonObject().getAsJsonArray("bones");
			if (os == null) {
				continue;
			}
			for (JsonElement un : os) {
				tous.add(un.getAsJsonObject());
			}
		}
		return tous;
	}

	private static Set<String> osDe(Path modele) throws IOException {
		Set<String> noms = new HashSet<>();
		for (JsonObject os : lesOs(modele)) {
			noms.add(nom(os));
		}
		return noms;
	}

	private static String nom(JsonObject os) {
		return os.has("name") ? os.get("name").getAsString() : "(sans nom)";
	}

	/** {@code compagnon:geo/kobeko.geo.json} devient le chemin du fichier. */
	private static Path ressource(JsonObject fiche, String cle) {
		String brut = fiche.get(cle).getAsString();
		return ASSETS.resolve(brut.substring(brut.indexOf(':') + 1));
	}

	private static List<Path> fichiers(Path dossier) throws IOException {
		if (!Files.isDirectory(dossier)) {
			return List.of();
		}
		try (Stream<Path> flux = Files.list(dossier)) {
			return flux.filter(chemin -> chemin.toString().endsWith(".json")).sorted().toList();
		}
	}

	private static String sansExtension(Path fichier) {
		String nom = fichier.getFileName().toString();
		return nom.substring(0, nom.length() - ".json".length());
	}

	private static JsonObject lire(Path fichier) throws IOException {
		try (Reader lecteur = Files.newBufferedReader(fichier)) {
			return JsonParser.parseReader(lecteur).getAsJsonObject();
		}
	}
}
