package fr.lhdp.compagnon.espece;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Chaque geste doit durer le temps qu'il dure.
 *
 * <h2>Ce qui n'allait pas</h2>
 *
 * <p>Le mod jouait toutes les animations pendant cinq secondes, la meme duree
 * pour toutes. Les vraies durees vont de un quart de seconde a dix secondes. Une
 * animation courte laissait donc la bete <b>figee dans sa derniere pose</b>
 * pendant le reste du temps, avant de revenir d'un coup ; une longue etait
 * coupee en plein milieu.
 *
 * <p>Dans les deux cas, un geste qu'on vient de debloquer a l'air casse — ce qui
 * est exactement le contraire de l'effet recherche.
 *
 * <h2>Ce que ce test verifie</h2>
 *
 * <p>Que <b>toute animation que la roue peut jouer</b> a bien une duree ecrite
 * dans son fichier. Une animation qui n'en a pas retombe sur une valeur inventee,
 * et le probleme revient pour elle seule — sans que rien ne le signale.
 */
class DureesTest {

	private static final Path NIVEAUX = Path.of("src/main/resources/data/compagnon/niveaux.json");
	private static final Path ANIMATIONS =
			Path.of("src/main/resources/assets/compagnon/animations");

	/** Le plafond que {@code Longueurs} applique, en secondes. */
	private static final double PLAFOND_SECONDES = 12.0D;

	/** Toutes les durees connues, par nom complet d'animation. */
	private static Map<String, Double> durees() throws IOException {
		Map<String, Double> lues = new HashMap<>();
		try (Stream<Path> parcours = Files.list(ANIMATIONS)) {
			for (Path chemin : parcours.filter(c -> c.toString().endsWith(".json")).toList()) {
				JsonObject racine = JsonParser
						.parseString(Files.readString(chemin, StandardCharsets.UTF_8))
						.getAsJsonObject();
				if (!racine.has("animations")) {
					continue;
				}
				for (Map.Entry<String, JsonElement> entree
						: racine.getAsJsonObject("animations").entrySet()) {

					JsonObject animation = entree.getValue().getAsJsonObject();
					if (animation.has("animation_length")) {
						lues.put(entree.getKey(),
								animation.get("animation_length").getAsDouble());
					}
				}
			}
		}
		return lues;
	}

	/** Toutes les animations que la table des niveaux ouvre a la roue. */
	private static List<String> deLaRoue() throws IOException {
		JsonObject racine = JsonParser
				.parseString(Files.readString(NIVEAUX, StandardCharsets.UTF_8))
				.getAsJsonObject();
		List<String> noms = new ArrayList<>();
		for (JsonElement palier : racine.getAsJsonArray("niveaux")) {
			JsonObject bloc = palier.getAsJsonObject();
			if (!bloc.has("debloque")) {
				continue;
			}
			for (JsonElement animation : bloc.getAsJsonArray("debloque")) {
				noms.add(animation.getAsString());
			}
		}
		return noms;
	}

	@Test
	@DisplayName("la table ouvre bien des animations")
	void ilYEnA() throws IOException {
		assertFalse(deLaRoue().isEmpty(), "aucune animation dans la table des niveaux");
		assertFalse(durees().isEmpty(), "aucune duree lue dans les fichiers d'animation");
	}

	@Test
	@DisplayName("chaque geste de la roue a une duree ecrite")
	void toutesOntUneDuree() throws IOException {
		Map<String, Double> durees = durees();
		List<String> sansDuree = new ArrayList<>();
		for (String animation : deLaRoue()) {
			if (!durees.containsKey(animation)) {
				sansDuree.add(animation);
			}
		}
		assertTrue(sansDuree.isEmpty(),
				"ces gestes joueraient une duree inventee, et se figeraient ou "
						+ "seraient coupes :\n  " + String.join("\n  ", sansDuree));
	}

	@Test
	@DisplayName("aucune duree n'est absurde")
	void desDureesRaisonnables() throws IOException {
		List<String> fautives = new ArrayList<>();
		for (Map.Entry<String, Double> entree : durees().entrySet()) {
			double secondes = entree.getValue();
			if (secondes <= 0.0D) {
				fautives.add(entree.getKey() + " dure " + secondes + " s");
			} else if (secondes > PLAFOND_SECONDES) {
				// Au-dela, le plafond de Longueurs coupe : la bete resterait bloquee
				// douze secondes puis le geste serait tronque.
				fautives.add(entree.getKey() + " dure " + secondes
						+ " s, au-dela du plafond de " + PLAFOND_SECONDES + " s");
			}
		}
		assertTrue(fautives.isEmpty(), String.join("\n", fautives));
	}

	@Test
	@DisplayName("les durees ne sont pas toutes les memes")
	void ellesVarient() throws IOException {
		// La garantie de fond : si toutes les animations duraient pareil, la duree
		// fixe d'avant aurait suffi et ce travail n'aurait servi a rien. Ce test
		// dit pourquoi il fallait le faire.
		Map<String, Double> durees = durees();
		double mini = Double.MAX_VALUE;
		double maxi = 0.0D;
		for (double secondes : durees.values()) {
			mini = Math.min(mini, secondes);
			maxi = Math.max(maxi, secondes);
		}
		assertTrue(maxi > mini * 2.0D,
				"les durees vont de " + mini + " s a " + maxi + " s : une duree unique "
						+ "ne peut pas convenir aux deux");
	}
}
