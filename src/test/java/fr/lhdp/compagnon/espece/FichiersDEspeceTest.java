package fr.lhdp.compagnon.espece;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Chaque animation citee existe-t-elle vraiment ?
 *
 * <h2>La faute que ces tests attrapent</h2>
 *
 * <p>Elle vient d'etre commise, et c'est pour cela que ce fichier existe : la
 * fiche de la mouette citait {@code animation.mouette.head_tilt}, alors que
 * l'animation s'appelle {@code amb_head_tilt}. Une lettre de trop.
 *
 * <p>Rien n'aurait plante. Le mod serait parti normalement, la mouette aurait
 * bouge normalement — et le geste appele n'aurait <b>jamais</b> joue. Personne
 * n'aurait su pourquoi, et la premiere hypothese aurait ete que le code du geste
 * ne marche pas.
 *
 * <p>C'est exactement le genre de faute qu'on refait a chaque nouvelle espece,
 * et qui coute une soiree a chercher. Elle se trouve ici en dix millisecondes.
 */
class FichiersDEspeceTest {

	private static final Path ESPECES = Path.of("src/main/resources/data/compagnon/especes");
	private static final Path ASSETS = Path.of("src/main/resources/assets/compagnon");
	private static final Path NIVEAUX = Path.of("src/main/resources/data/compagnon/niveaux.json");

	@Test
	@DisplayName("chaque animation citee par une espece existe dans son fichier")
	void aucuneAnimationFantome() throws IOException {
		List<String> fautes = new ArrayList<>();

		for (Path fichier : fichiersDEspece()) {
			JsonObject espece = lire(fichier);
			Set<String> disponibles = animationsDe(espece);
			if (disponibles.isEmpty()) {
				continue;
			}
			for (Map.Entry<String, String> cite : citees(espece).entrySet()) {
				if (!disponibles.contains(cite.getValue())) {
					fautes.add(fichier.getFileName() + " : le role \"" + cite.getKey()
							+ "\" cite " + cite.getValue() + ", qui n'existe pas");
				}
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	@Test
	@DisplayName("chaque animation de la roue existe chez son espece")
	void aucunDeblocageFantome() throws IOException {
		JsonObject niveaux = lire(NIVEAUX);
		Set<String> toutesLesAnimations = new LinkedHashSet<>();
		for (Path fichier : fichiersDEspece()) {
			toutesLesAnimations.addAll(animationsDe(lire(fichier)));
		}
		if (toutesLesAnimations.isEmpty()) {
			return;
		}

		List<String> fautes = new ArrayList<>();
		for (JsonElement palier : niveaux.getAsJsonArray("niveaux")) {
			JsonObject bloc = palier.getAsJsonObject();
			if (!bloc.has("debloque")) {
				continue;
			}
			for (JsonElement nom : bloc.getAsJsonArray("debloque")) {
				String anime = nom.getAsString();
				if (!toutesLesAnimations.contains(anime)) {
					fautes.add("niveau " + bloc.get("niveau").getAsInt()
							+ " : " + anime + " n'existe dans aucun fichier d'animation");
				}
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	@Test
	@DisplayName("chaque espece a ses fichiers, sa texture et ses roles obligatoires")
	void rienNeManque() throws IOException {
		List<Path> especes = fichiersDEspece();
		assertFalse(especes.isEmpty(), "aucune espece dans le mod");

		List<String> fautes = new ArrayList<>();
		for (Path fichier : especes) {
			JsonObject espece = lire(fichier);

			for (String cle : List.of("geometrie", "animations", "variantes")) {
				if (!espece.has(cle)) {
					fautes.add(fichier.getFileName() + " : il manque \"" + cle + "\"");
				}
			}
			if (espece.has("geometrie")) {
				verifierLeFichier(espece.get("geometrie").getAsString(), fichier, fautes);
			}
			if (espece.has("animations")) {
				verifierLeFichier(espece.get("animations").getAsString(), fichier, fautes);
			}
			if (espece.has("variantes")) {
				for (Map.Entry<String, JsonElement> variante
						: espece.getAsJsonObject("variantes").entrySet()) {
					verifierLeFichier(variante.getValue().getAsString(), fichier, fautes);
				}
			}
			// Sans ces quatre-la, la bete se fige dans sa pose de geometrie : ailes
			// deployees, pattes ecartees. Voir Espece.ROLES.
			if (espece.has("locomotion")) {
				JsonObject locomotion = espece.getAsJsonObject("locomotion");
				for (String role : List.of("immobile", "marche", "course", "vol")) {
					if (!locomotion.has(role)) {
						fautes.add(fichier.getFileName()
								+ " : il manque le role de locomotion \"" + role + "\"");
					}
				}
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	// --- Outils ---------------------------------------------------------------------

	private static void verifierLeFichier(String reference, Path espece, List<String> fautes) {
		Path chemin = versLeFichier(reference);
		if (chemin == null || !Files.exists(chemin)) {
			fautes.add(espece.getFileName() + " : " + reference + " est introuvable");
		}
	}

	/** {@code compagnon:geo/mouette.geo.json} devient un chemin sur le disque. */
	private static Path versLeFichier(String reference) {
		int deuxPoints = reference.indexOf(':');
		if (deuxPoints < 0) {
			return null;
		}
		return ASSETS.resolve(reference.substring(deuxPoints + 1));
	}

	private static List<Path> fichiersDEspece() throws IOException {
		if (!Files.isDirectory(ESPECES)) {
			return List.of();
		}
		try (Stream<Path> chemins = Files.list(ESPECES)) {
			return chemins.filter(p -> p.toString().endsWith(".json")).sorted().toList();
		}
	}

	private static Set<String> animationsDe(JsonObject espece) throws IOException {
		if (!espece.has("animations")) {
			return Set.of();
		}
		Path fichier = versLeFichier(espece.get("animations").getAsString());
		if (fichier == null || !Files.exists(fichier)) {
			return Set.of();
		}
		JsonObject racine = lire(fichier);
		if (!racine.has("animations")) {
			return Set.of();
		}
		return new LinkedHashSet<>(racine.getAsJsonObject("animations").keySet());
	}

	/** Tous les noms d'animation qu'une fiche d'espece cite, par role. */
	private static Map<String, String> citees(JsonObject espece) {
		Map<String, String> citees = new java.util.LinkedHashMap<>();
		for (String bloc : List.of("locomotion", "reactions")) {
			if (!espece.has(bloc)) {
				continue;
			}
			for (Map.Entry<String, JsonElement> role : espece.getAsJsonObject(bloc).entrySet()) {
				String valeur = role.getValue().getAsString();
				// Un role laisse vide est un role qu'on n'a pas encore rempli :
				// c'est permis, et le mod retombe alors sur l'animation immobile.
				if (!valeur.isEmpty()) {
					citees.put(bloc + "." + role.getKey(), valeur);
				}
			}
		}
		return citees;
	}

	private static JsonObject lire(Path fichier) throws IOException {
		JsonElement lu = JsonParser.parseString(
				Files.readString(fichier, StandardCharsets.UTF_8));
		if (lu.isJsonArray()) {
			JsonObject enveloppe = new JsonObject();
			enveloppe.add("niveaux", (JsonArray) lu);
			return enveloppe;
		}
		return lu.getAsJsonObject();
	}
}
