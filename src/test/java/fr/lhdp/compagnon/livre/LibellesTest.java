package fr.lhdp.compagnon.livre;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le livre affichait des noms de variables.
 *
 * <h2>Ce qui s'est passe</h2>
 *
 * <p>La page « Ce qu'on a fait ensemble » listait les compteurs de la fiche tels
 * quels. Tant qu'il n'y en avait que quatre, ca passait a peu pres. Le jour ou
 * les missions en ont ajoute vingt — dont leurs propres compteurs de cuisine —
 * la page s'est mise a afficher <em>m.moule.0 : 14</em> et <em>m.jour : 2</em>.
 *
 * <p>Meme chose pour les moments : un defaut s'affichait <em>defaut bavard</em>,
 * ce qui ressemble a une ligne de code oubliee la.
 *
 * <h2>Pourquoi un test</h2>
 *
 * <p>Parce que ca recommencera. Chaque nouveau compteur, chaque nouvelle manie,
 * chaque nouveau defaut est une occasion d'oublier sa traduction — et personne
 * ne s'en apercoit avant d'ouvrir le livre en jeu, des semaines plus tard.
 *
 * <p>Le test lit les <b>sources</b> plutot que le code compile : c'est ce qui
 * lui permet de tourner sans demarrer Minecraft, et ce qui en fait une seconde
 * source d'information plutot qu'une verification du code par lui-meme.
 */
class LibellesTest {

	private static final Path LANG =
			Path.of("src/main/resources/assets/compagnon/lang/fr_fr.json");
	private static final Path MISSIONS = Path.of("src/main/resources/data/compagnon/missions");
	private static final Path MANIES =
			Path.of("src/main/java/fr/lhdp/compagnon/entite/Manies.java");

	private static JsonObject langue() throws IOException {
		return JsonParser.parseString(Files.readString(LANG, StandardCharsets.UTF_8))
				.getAsJsonObject();
	}

	/** Les identifiants cites entre guillemets par un motif, dans un fichier source. */
	private static Set<String> extraire(Path source, String motif) throws IOException {
		Set<String> trouves = new LinkedHashSet<>();
		Matcher chercheur = Pattern.compile(motif)
				.matcher(Files.readString(source, StandardCharsets.UTF_8));
		while (chercheur.find()) {
			trouves.add(chercheur.group(1));
		}
		return trouves;
	}

	@Test
	@DisplayName("chaque compteur surveille par une mission a son libelle")
	void lesCompteursSeLisent() throws IOException {
		JsonObject langue = langue();
		Set<String> sansLibelle = new LinkedHashSet<>();

		try (Stream<Path> parcours = Files.list(MISSIONS)) {
			for (Path chemin : parcours.filter(c -> c.toString().endsWith(".json")).toList()) {
				JsonObject moule = JsonParser
						.parseString(Files.readString(chemin, StandardCharsets.UTF_8))
						.getAsJsonObject();
				String compteur = moule.get("compteur").getAsString();
				if (!langue.has("compteur.compagnon." + compteur)) {
					sansLibelle.add(compteur);
				}
			}
		}
		assertTrue(sansLibelle.isEmpty(),
				"le livre afficherait ces cles brutes : " + String.join(", ", sansLibelle));
	}

	@Test
	@DisplayName("aucun compteur montre au joueur n'a de point dans son nom")
	void laConventionDesCompteurs() throws IOException {
		// La regle qui separe ce qu'on montre de ce qui est de la cuisine : un
		// compteur interne a toujours un point, un compteur montrable jamais.
		// C'est elle que DonneesLivre applique pour filtrer.
		JsonObject langue = langue();
		List<String> fautifs = new ArrayList<>();
		for (String cle : langue.keySet()) {
			if (!cle.startsWith("compteur.compagnon.")) {
				continue;
			}
			String compteur = cle.substring("compteur.compagnon.".length());
			if (compteur.indexOf('.') >= 0) {
				fautifs.add(compteur + " : un compteur avec un point ne sera jamais montre");
			}
		}
		assertTrue(fautifs.isEmpty(), String.join("\n", fautifs));
	}

	@Test
	@DisplayName("chaque manie a sa phrase")
	void lesManiesSeLisent() throws IOException {
		JsonObject langue = langue();
		Set<String> ids = extraire(MANIES, "new Manie\\(\"([a-z_0-9]+)\"");
		assertFalse(ids.isEmpty(), "aucune manie trouvee dans la source");

		List<String> sansPhrase = new ArrayList<>();
		for (String id : ids) {
			if (!langue.has("moment.compagnon.manie." + id)) {
				sansPhrase.add(id);
			}
		}
		assertTrue(sansPhrase.isEmpty(),
				"le livre afficherait « manie " + String.join(" », « manie ", sansPhrase)
						+ " »");
	}

	@Test
	@DisplayName("chaque defaut a sa phrase")
	void lesDefautsSeLisent() throws IOException {
		JsonObject langue = langue();
		// Les defauts sont declares en constantes : String GOURMAND = "gourmand";
		Set<String> ids = extraire(MANIES,
				"public static final String [A-Z_]+ = \"([a-z_]+)\"");
		assertFalse(ids.isEmpty(), "aucun defaut trouve dans la source");

		List<String> sansPhrase = new ArrayList<>();
		for (String id : ids) {
			if (!langue.has("moment.compagnon.defaut." + id)) {
				sansPhrase.add(id);
			}
		}
		assertTrue(sansPhrase.isEmpty(),
				"le livre afficherait « defaut " + String.join(" », « defaut ", sansPhrase)
						+ " »");
	}

	@Test
	@DisplayName("les familles de moments ont leur en-tete de repli")
	void lesFamillesOntUnRepli() throws IOException {
		// Quand la cle precise manque, le livre retombe sur l'en-tete de la
		// famille. Si celui-la manque aussi, on revoit une cle brute.
		JsonObject langue = langue();
		List<String> manquants = new ArrayList<>();
		for (String famille : List.of("defaut", "manie", "mission", "premiere_fois.biome")) {
			if (!langue.has("moment.compagnon." + famille)) {
				manquants.add(famille);
			}
		}
		assertTrue(manquants.isEmpty(),
				"sans en-tete, ces familles retomberaient sur la cle brute : "
						+ String.join(", ", manquants));
	}
}
