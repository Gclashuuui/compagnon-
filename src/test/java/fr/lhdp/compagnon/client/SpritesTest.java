package fr.lhdp.compagnon.client;

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les textures d'interface citees par le code doivent exister.
 *
 * <h2>Pourquoi ca merite un test</h2>
 *
 * <p>Une texture manquante ne fait pas d'erreur : le jeu affiche le damier noir
 * et rose a la place, et continue. Le compilateur ne peut rien dire — un nom de
 * texture est une chaine de caracteres. On ne s'en apercoit donc qu'en ouvrant
 * l'ecran en jeu, et si c'est un ecran rare, jamais.
 *
 * <p>Le meme raisonnement vaut pour le fichier d'accompagnement : une texture
 * livree sans son {@code .mcmeta} perd son decoupage en neuf morceaux. Elle
 * s'affiche alors etiree — les coins deformes, le cadre baveux — et on cherche
 * longtemps pourquoi l'ecran est laid alors que « la texture est bien la ».
 */
class SpritesTest {

	private static final Path PEINTURE =
			Path.of("src/client/java/fr/lhdp/compagnon/client/Peinture.java");

	private static final Path SPRITES =
			Path.of("src/main/resources/assets/compagnon/textures/gui/sprites");

	/** Les chemins cites par {@code Compagnon.id("...")} dans la boite a outils. */
	private static Set<String> cites() throws IOException {
		Set<String> trouves = new LinkedHashSet<>();
		Matcher chercheur = Pattern.compile("Compagnon\\.id\\(\"([^\"]+)\"\\)")
				.matcher(Files.readString(PEINTURE, StandardCharsets.UTF_8));
		while (chercheur.find()) {
			trouves.add(chercheur.group(1));
		}
		return trouves;
	}

	@Test
	@DisplayName("la boite a outils cite bien des sprites")
	void ilYEnA() throws IOException {
		assertFalse(cites().isEmpty(), "aucun sprite cite dans Peinture");
	}

	@Test
	@DisplayName("chaque sprite cite existe sur le disque")
	void aucunDamierRose() throws IOException {
		List<String> manquants = new ArrayList<>();
		for (String chemin : cites()) {
			if (!Files.exists(SPRITES.resolve(chemin + ".png"))) {
				manquants.add(chemin + ".png");
			}
		}
		assertTrue(manquants.isEmpty(),
				"ces textures s'afficheraient en damier rose :\n  "
						+ String.join("\n  ", manquants));
	}

	@Test
	@DisplayName("chaque sprite a son decoupage en neuf morceaux")
	void chacunSaDecoupe() throws IOException {
		List<String> fautifs = new ArrayList<>();
		for (String chemin : cites()) {
			Path mcmeta = SPRITES.resolve(chemin + ".png.mcmeta");
			if (!Files.exists(mcmeta)) {
				fautifs.add(chemin + " : pas de .mcmeta, la texture sera etiree");
				continue;
			}
			JsonObject racine = JsonParser
					.parseString(Files.readString(mcmeta, StandardCharsets.UTF_8))
					.getAsJsonObject();
			JsonObject echelle = racine.getAsJsonObject("gui").getAsJsonObject("scaling");
			String type = echelle.get("type").getAsString();
			if (!"nine_slice".equals(type)) {
				fautifs.add(chemin + " : type \"" + type + "\" au lieu de nine_slice");
				continue;
			}
			int largeur = echelle.get("width").getAsInt();
			int bordure = echelle.get("border").getAsInt();
			// Deux bordures doivent tenir dans la largeur, sinon les coins se
			// chevauchent et le centre disparait.
			if (bordure * 2 >= largeur) {
				fautifs.add(chemin + " : bordure de " + bordure + " pour une largeur de "
						+ largeur + " — les coins se chevaucheraient");
			}
		}
		assertTrue(fautifs.isEmpty(), String.join("\n", fautifs));
	}
}
