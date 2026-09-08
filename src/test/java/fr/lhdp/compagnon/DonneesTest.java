package fr.lhdp.compagnon;

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
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les donnees se tiennent-elles entre elles ?
 *
 * <h2>Pourquoi ces tests-la valent plus que tous les autres</h2>
 *
 * <p>Les trois pires defauts de ce projet n'etaient pas des defauts de code. Ils
 * etaient dans les fichiers, ils ne faisaient <b>lever aucune erreur</b>, et on
 * ne les voyait qu'en jeu, parfois des jours plus tard :
 *
 * <ul>
 *   <li>trente-six aliments utilisaient des barres qui n'existent pas
 *       ({@code humeur}, {@code proprete}). Le chargeur les ecartait en silence,
 *       avec une ligne dans un log que personne ne lit ;</li>
 *   <li>deux bobos se soignaient avec des remedes supprimes. Ils etaient devenus
 *       <b>incurables</b>, et rien ne le disait ;</li>
 *   <li>des varietes sans texture ou sans modele se seraient affichees en damier
 *       violet dans l'onglet creatif.</li>
 * </ul>
 *
 * <p>Chacun aurait ete arrete ici, en une seconde, avant meme de lancer le jeu.
 * C'est tout l'interet : ces tests ne demandent ni Minecraft, ni serveur, ni
 * client — ils lisent les fichiers comme le mod les lira.
 */
class DonneesTest {

	private static final Path DATA = Path.of("src/main/resources/data/compagnon");
	private static final Path ASSETS = Path.of("src/main/resources/assets/compagnon");

	/** Les seules barres qui existent. Voir {@code fiche/Barre.java}. */
	private static final Set<String> BARRES = Set.of("faim", "energie", "complicite", "sante");

	@Test
	void chaqueEffetNommeUneBarreQuiExiste() throws IOException {
		List<String> fautes = new ArrayList<>();

		for (String dossier : List.of("aliments", "soins")) {
			for (Path fichier : fichiers(DATA.resolve(dossier))) {
				JsonObject contenu = lire(fichier);
				if (!contenu.has("effets")) {
					continue;
				}
				for (String barre : contenu.getAsJsonObject("effets").keySet()) {
					if (!BARRES.contains(barre)) {
						fautes.add(dossier + "/" + fichier.getFileName() + " : barre \"" + barre
								+ "\" inconnue, le fichier serait ECARTE en silence");
					}
				}
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	@Test
	void chaqueBoboSeSoigneAvecUnRemedeQuiExiste() throws IOException {
		Set<String> soins = noms(DATA.resolve("soins"));
		List<String> fautes = new ArrayList<>();

		for (Path fichier : fichiers(DATA.resolve("bobos"))) {
			String remede = lire(fichier).get("soigne_par").getAsString();
			if (!soins.contains(remede)) {
				fautes.add(fichier.getFileName() + " se soigne avec \"" + remede
						+ "\", qui n'existe pas : le bobo serait INCURABLE");
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	@Test
	void chaqueVarieteAUneTextureEtUnModele() throws IOException {
		List<String> fautes = new ArrayList<>();

		for (String dossier : List.of("aliments", "soins")) {
			for (Path fichier : fichiers(DATA.resolve(dossier))) {
				String id = sansExtension(fichier);
				if (!Files.isRegularFile(ASSETS.resolve("textures/item/" + id + ".png"))) {
					fautes.add(id + " : texture manquante, damier violet en jeu");
				}
				if (!Files.isRegularFile(ASSETS.resolve("models/item/" + id + ".json"))) {
					fautes.add(id + " : modele manquant");
				}
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	/**
	 * Deux varietes qui partagent un numero afficheraient la meme image, et on
	 * chercherait longtemps pourquoi.
	 */
	@Test
	void lesNumerosDeModeleSontUniquesEtDeclares() throws IOException {
		List<String> fautes = new ArrayList<>();

		for (String[] paire : new String[][] { { "aliments", "aliment" }, { "soins", "soin" } }) {
			Set<Integer> vus = new HashSet<>();
			JsonObject base = lire(ASSETS.resolve("models/item/" + paire[1] + ".json"));

			Set<String> surcharges = new HashSet<>();
			if (base.has("overrides")) {
				for (JsonElement element : base.getAsJsonArray("overrides")) {
					JsonObject surcharge = element.getAsJsonObject();
					surcharges.add(surcharge.getAsJsonObject("predicate").get("custom_model_data")
							.getAsInt() + "->" + surcharge.get("model").getAsString());
				}
			}

			for (Path fichier : fichiers(DATA.resolve(paire[0]))) {
				String id = sansExtension(fichier);
				JsonObject contenu = lire(fichier);
				if (!contenu.has("modele")) {
					fautes.add(id + " : pas de numero de modele");
					continue;
				}
				int modele = contenu.get("modele").getAsInt();
				if (modele <= 0) {
					// Zero est la valeur par defaut quand la donnee est absente :
					// il appartient a l'objet sans variete.
					fautes.add(id + " : numero de modele " + modele + ", il faut au moins 1");
				}
				if (!vus.add(modele)) {
					fautes.add(id + " : numero de modele " + modele + " deja pris");
				}
				String attendue = modele + "->compagnon:item/" + id;
				if (!surcharges.contains(attendue)) {
					fautes.add(id + " : absent des surcharges de " + paire[1] + ".json");
				}
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	/**
	 * Un reve sans phrase s'afficherait dans le livre sous la forme de sa cle
	 * brute, ce qui a l'air d'un bogue.
	 */
	@Test
	void chaqueReveAUnePhrase() throws IOException {
		JsonObject langue = lire(ASSETS.resolve("lang/fr_fr.json"));
		List<String> fautes = new ArrayList<>();

		for (JsonElement reve : lire(DATA.resolve("reves.json")).getAsJsonArray("reves")) {
			String cle = "moment.compagnon.reve." + reve.getAsString();
			if (!langue.has(cle)) {
				fautes.add(cle + " n'a pas de phrase dans fr_fr.json");
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	/**
	 * Un nom que le moteur vocal ne connait pas serait retire de la grammaire en
	 * silence. La liste proposee au bapteme doit donc etre irreprochable.
	 */
	@Test
	void lesNomsProposesSontEcritsEnUnSeulMotOuDeux() throws IOException {
		List<String> fautes = new ArrayList<>();

		for (JsonElement nom : lire(DATA.resolve("noms.json")).getAsJsonArray("noms")) {
			String valeur = nom.getAsString();
			if (valeur.isBlank() || valeur.split(" ").length > 2) {
				fautes.add("\"" + valeur + "\" : un nom se dit en un mot, deux au plus");
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	// --- Outils -------------------------------------------------------------------

	private static List<Path> fichiers(Path dossier) throws IOException {
		if (!Files.isDirectory(dossier)) {
			return List.of();
		}
		try (Stream<Path> flux = Files.list(dossier)) {
			return flux.filter(chemin -> chemin.toString().endsWith(".json")).sorted().toList();
		}
	}

	private static Set<String> noms(Path dossier) throws IOException {
		Set<String> tous = new HashSet<>();
		for (Path fichier : fichiers(dossier)) {
			tous.add(sansExtension(fichier));
		}
		return tous;
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
