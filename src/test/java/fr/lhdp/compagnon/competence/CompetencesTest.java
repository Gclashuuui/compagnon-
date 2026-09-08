package fr.lhdp.compagnon.competence;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les fichiers de competences tiennent-ils debout ?
 *
 * <h2>Ce qui peut mal tourner sans qu'on le voie</h2>
 *
 * <p>Une competence est un fichier, et un fichier se relit rarement. Trois
 * fautes passeraient inapercues jusqu'a ce qu'un joueur les vive :
 *
 * <ul>
 *   <li>un <b>effet mal orthographie</b> — la competence existe, coute un
 *       point, et ne fait rien du tout ;</li>
 *   <li>un effet <b>dans le mauvais sens</b> — « bonne fourchette » qui donne
 *       <i>plus</i> faim, parce qu'on a ecrit 1,3 au lieu de 0,7 ;</li>
 *   <li>un palier <b>au-dela du niveau maximum</b> — une competence que
 *       personne ne pourra jamais prendre, et qui occupe une ligne du livre
 *       pour toujours.</li>
 * </ul>
 *
 * <p>Aucune de ces trois fautes ne plante quoi que ce soit. Toutes se paient en
 * points de joueur — et un point depense ne se reprend pas.
 */
class CompetencesTest {

	private static final Path DOSSIER =
			Path.of("src/main/resources/data/compagnon/competences");
	private static final Path NIVEAUX =
			Path.of("src/main/resources/data/compagnon/niveaux.json");

	/** Les effets qui ameliorent en DESCENDANT sous 1. */
	private static final Set<String> A_LA_BAISSE =
			Set.of(Competence.FAIM, Competence.ENERGIE, Competence.BOBOS);

	/** Ceux qui ameliorent en MONTANT au-dessus de 1. */
	private static final Set<String> A_LA_HAUSSE = Set.of(Competence.EXPERIENCE);

	@Test
	@DisplayName("chaque effet est une cle que le mod sait lire")
	void aucunEffetFantome() throws IOException {
		List<String> fautes = new ArrayList<>();
		for (Path fichier : fichiers()) {
			JsonObject objet = lire(fichier);
			if (!objet.has("effets")) {
				fautes.add(fichier.getFileName() + " : aucun effet, elle ne ferait rien");
				continue;
			}
			for (String cle : objet.getAsJsonObject("effets").keySet()) {
				if (!Competence.CLES_CONNUES.contains(cle)) {
					fautes.add(fichier.getFileName() + " : l'effet \"" + cle
							+ "\" n'existe pas — elle couterait un point pour rien");
				}
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	/**
	 * Une competence doit ameliorer, jamais empirer.
	 *
	 * <p>Personne ne depenserait un point pour avoir plus faim. Si un jour c'est
	 * voulu — une competence a double tranchant — ce test devra etre relache
	 * <b>exprès</b>, et c'est tres bien : ce sera un choix, pas un oubli.
	 */
	@Test
	@DisplayName("aucune competence ne rend la vie plus dure")
	void toutesAmeliorent() throws IOException {
		List<String> fautes = new ArrayList<>();
		for (Path fichier : fichiers()) {
			JsonObject objet = lire(fichier);
			if (!objet.has("effets")) {
				continue;
			}
			for (Map.Entry<String, JsonElement> effet
					: objet.getAsJsonObject("effets").entrySet()) {
				float valeur = effet.getValue().getAsFloat();
				if (A_LA_BAISSE.contains(effet.getKey()) && valeur >= 1.0F) {
					fautes.add(fichier.getFileName() + " : \"" + effet.getKey() + "\" a "
							+ valeur + " empire les choses au lieu de les ameliorer");
				}
				if (A_LA_HAUSSE.contains(effet.getKey()) && valeur <= 1.0F) {
					fautes.add(fichier.getFileName() + " : \"" + effet.getKey() + "\" a "
							+ valeur + " n'apporte rien");
				}
				if (valeur <= 0.0F) {
					fautes.add(fichier.getFileName() + " : \"" + effet.getKey()
							+ "\" a zero supprimerait la regle entierement");
				}
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	@Test
	@DisplayName("aucune competence n'est hors d'atteinte")
	void toutesAtteignables() throws IOException {
		int maximum = niveauMaximum();
		List<String> fautes = new ArrayList<>();
		for (Path fichier : fichiers()) {
			JsonObject objet = lire(fichier);
			int requis = objet.has("niveau_requis")
					? objet.get("niveau_requis").getAsInt()
					: 1;
			if (requis > maximum) {
				fautes.add(fichier.getFileName() + " : niveau " + requis
						+ " alors que le maximum est " + maximum);
			}
			if (requis < 1) {
				fautes.add(fichier.getFileName() + " : niveau " + requis + " n'existe pas");
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	@Test
	@DisplayName("chaque competence a un nom et une description, et un nom unique")
	void toutesSeLisent() throws IOException {
		List<Path> tous = fichiers();
		assertFalse(tous.isEmpty(), "aucune competence dans le mod");

		Set<String> noms = new HashSet<>();
		List<String> fautes = new ArrayList<>();
		for (Path fichier : tous) {
			JsonObject objet = lire(fichier);
			for (String cle : List.of("nom", "description")) {
				if (!objet.has(cle) || objet.get(cle).getAsString().isBlank()) {
					fautes.add(fichier.getFileName() + " : il manque \"" + cle + "\"");
				}
			}
			// Deux competences du meme nom sont indiscernables dans le livre : on
			// depenserait un point sans savoir laquelle on prend.
			if (objet.has("nom") && !noms.add(objet.get("nom").getAsString())) {
				fautes.add(fichier.getFileName() + " : ce nom est deja pris");
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	// --- Outils ---------------------------------------------------------------------

	private static List<Path> fichiers() throws IOException {
		if (!Files.isDirectory(DOSSIER)) {
			return List.of();
		}
		try (Stream<Path> chemins = Files.list(DOSSIER)) {
			return chemins.filter(p -> p.toString().endsWith(".json")).sorted().toList();
		}
	}

	private static JsonObject lire(Path fichier) throws IOException {
		return JsonParser.parseString(
				Files.readString(fichier, StandardCharsets.UTF_8)).getAsJsonObject();
	}

	private static int niveauMaximum() throws IOException {
		JsonObject racine = JsonParser.parseString(
				Files.readString(NIVEAUX, StandardCharsets.UTF_8)).getAsJsonObject();
		int maximum = 1;
		for (JsonElement palier : racine.getAsJsonArray("niveaux")) {
			maximum = Math.max(maximum, palier.getAsJsonObject().get("niveau").getAsInt());
		}
		return maximum;
	}
}
