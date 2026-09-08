package fr.lhdp.compagnon.mission;

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
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les fichiers de missions, relus un par un.
 *
 * <h2>Pourquoi ce test compte plus que les autres</h2>
 *
 * <p>Une mission est du texte et des nombres dans un fichier. Rien dans le
 * compilateur n'attrape une faute de frappe dans un nom de compteur : la mission
 * se propose normalement, sa barre reste a zero pour toujours, et le joueur
 * croit avoir affaire a un bug. On ne s'en apercevrait qu'en jouant, des
 * semaines plus tard, et sans savoir laquelle est cassee.
 *
 * <p>Ce test lit donc les fichiers comme le jeu les lira, et verifie les quatre
 * choses que le jeu ne peut pas verifier tout seul :
 *
 * <ol>
 *   <li>le compteur surveille est bien un compteur que quelque chose alimente ;</li>
 *   <li>l'enonce a une traduction, sinon le livre affiche une cle brute ;</li>
 *   <li>les difficultes montent, sinon le cran « difficile » est le plus facile ;</li>
 *   <li>il y a autant d'experiences que de quantites.</li>
 * </ol>
 */
class FichiersDeMissionTest {

	private static final Path MISSIONS = Path.of("src/main/resources/data/compagnon/missions");
	private static final Path LANG =
			Path.of("src/main/resources/assets/compagnon/lang/fr_fr.json");

	/**
	 * Tous les compteurs que le mod alimente.
	 *
	 * <p>Ecrit a la main <b>expres</b>. Le lire dans le code reviendrait a
	 * verifier que le code est d'accord avec lui-meme ; cette liste est une
	 * seconde source, et c'est ce qui lui donne sa valeur. Y ajouter une ligne
	 * doit rester un geste conscient.
	 */
	private static final Set<String> COMPTEURS = Set.of(
			// Les anciens, deja alimentes par les interactions.
			"repas", "caresses", "soins", "balades",
			// Les ponctuels, ajoutes avec les missions.
			"mots", "envols", "montes", "siestes", "mine", "pose",
			"rencontres", "salutations", "caresses_amis",
			// Les ambiants, releves chaque minute.
			"nuits", "biomes", "hauteur", "nage", "pluie", "orages");

	// Les compteurs des jouets — jeux, balle, jouets_offerts — ne sont PAS dans
	// cette liste, et c est voulu : les jouets ne sont pas faits. Une mission qui
	// les citerait resterait infinissable a jamais, et ce test la refuse.

	private static List<Path> fichiers() throws IOException {
		try (Stream<Path> parcours = Files.list(MISSIONS)) {
			return parcours.filter(chemin -> chemin.toString().endsWith(".json")).toList();
		}
	}

	private static JsonObject lire(Path chemin) throws IOException {
		return JsonParser.parseString(Files.readString(chemin, StandardCharsets.UTF_8))
				.getAsJsonObject();
	}

	@Test
	@DisplayName("il y a des missions a lire")
	void ilYEnA() throws IOException {
		assertFalse(fichiers().isEmpty(), "aucun fichier de mission trouve");
	}

	@Test
	@DisplayName("chaque mission surveille un compteur que quelque chose alimente")
	void desCompteursVivants() throws IOException {
		List<String> fautifs = new ArrayList<>();
		for (Path chemin : fichiers()) {
			String compteur = lire(chemin).get("compteur").getAsString();
			if (!COMPTEURS.contains(compteur)) {
				fautifs.add(chemin.getFileName() + " surveille \"" + compteur
						+ "\", que personne n'alimente");
			}
		}
		assertTrue(fautifs.isEmpty(), String.join("\n", fautifs));
	}

	@Test
	@DisplayName("chaque enonce a sa traduction")
	void desPhrasesLisibles() throws IOException {
		JsonObject langue = lire(LANG);
		List<String> fautifs = new ArrayList<>();
		for (Path chemin : fichiers()) {
			String texte = lire(chemin).get("texte").getAsString();
			if (!langue.has(texte)) {
				fautifs.add(chemin.getFileName() + " cite \"" + texte
						+ "\", qui n'est pas traduit");
			}
		}
		assertTrue(fautifs.isEmpty(), String.join("\n", fautifs));
	}

	@Test
	@DisplayName("l'enonce attend bien une quantite")
	void chaqueEnonceCiteSaQuantite() throws IOException {
		JsonObject langue = lire(LANG);
		List<String> fautifs = new ArrayList<>();
		for (Path chemin : fichiers()) {
			String cle = lire(chemin).get("texte").getAsString();
			if (!langue.has(cle)) {
				continue;
			}
			String phrase = langue.get(cle).getAsString();
			if (!phrase.contains("%s")) {
				fautifs.add(cle + " : « " + phrase + " » n'affiche jamais la quantite");
			}
		}
		assertTrue(fautifs.isEmpty(), String.join("\n", fautifs));
	}

	@Test
	@DisplayName("les difficultes montent, et l'experience avec")
	void desCransCroissants() throws IOException {
		List<String> fautifs = new ArrayList<>();
		for (Path chemin : fichiers()) {
			JsonObject moule = lire(chemin);
			List<Integer> quantites = entiers(moule.getAsJsonArray("quantites"));
			List<Integer> xp = entiers(moule.getAsJsonArray("xp"));
			String nom = chemin.getFileName().toString();

			if (quantites.size() != xp.size()) {
				fautifs.add(nom + " : " + quantites.size() + " quantites pour "
						+ xp.size() + " experiences");
				continue;
			}
			for (int i = 1; i < quantites.size(); i++) {
				if (quantites.get(i) <= quantites.get(i - 1)) {
					fautifs.add(nom + " : le cran " + i + " n'est pas plus dur que le precedent");
				}
				if (xp.get(i) <= xp.get(i - 1)) {
					fautifs.add(nom + " : le cran " + i + " ne rapporte pas plus que le precedent");
				}
			}
		}
		assertTrue(fautifs.isEmpty(), String.join("\n", fautifs));
	}

	@Test
	@DisplayName("assez de familles differentes pour en proposer trois a la fois")
	void assezDeVariete() throws IOException {
		Set<String> familles = new LinkedHashSet<>();
		int courtes = 0;
		for (Path chemin : fichiers()) {
			JsonObject moule = lire(chemin);
			boolean longue = moule.has("longue") && moule.get("longue").getAsBoolean();
			if (longue) {
				continue;
			}
			courtes++;
			familles.add(moule.has("famille")
					? moule.get("famille").getAsString()
					: chemin.getFileName().toString());
		}
		assertTrue(courtes >= 3, "il faut au moins trois missions courtes");
		// Le tirage refuse deux missions de la meme famille. Avec moins de trois
		// familles, il retomberait sur le repli et proposerait des doublons.
		assertTrue(familles.size() >= 3,
				"il faut au moins trois familles differentes, il y en a " + familles.size());
	}

	@Test
	@DisplayName("il existe au moins une mission de fond")
	void uneMissionLongue() throws IOException {
		boolean trouvee = false;
		for (Path chemin : fichiers()) {
			JsonObject moule = lire(chemin);
			if (moule.has("longue") && moule.get("longue").getAsBoolean()) {
				trouvee = true;
				break;
			}
		}
		assertTrue(trouvee, "aucune mission longue : la page de droite du livre serait vide");
	}

	private static List<Integer> entiers(JsonArray tableau) {
		List<Integer> valeurs = new ArrayList<>();
		for (JsonElement element : tableau) {
			valeurs.add(element.getAsInt());
		}
		return valeurs;
	}
}
