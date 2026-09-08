package fr.lhdp.compagnon;

import com.google.gson.JsonArray;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La police d'icones du mod doit rester coherente avec le code.
 *
 * <h2>Ce qui se casse tout seul</h2>
 *
 * <p>Une icone est un caractere de la zone privee d'Unicode. Rien, absolument
 * rien, ne relie la constante {@code Icones.COEUR} au fichier de police : si le
 * caractere n'y est pas, le jeu affiche un <b>carre blanc</b> a la place et
 * continue sans se plaindre.
 *
 * <p>C'est donc exactement le genre de chose qu'on ne decouvre qu'en jeu, sur la
 * capture d'ecran d'un joueur — et le mod en est deja a deux fois.
 *
 * <h2>Ce que le test verifie</h2>
 *
 * <ol>
 *   <li>le fichier de police et son image existent ;</li>
 *   <li>chaque caractere declare dans {@link Icones} s'y trouve ;</li>
 *   <li>l'image est assez grande pour contenir la grille annoncee.</li>
 * </ol>
 */
class PoliceTest {

	private static final Path POLICE =
			Path.of("src/main/resources/assets/compagnon/font/icones.json");

	private static final Path IMAGE =
			Path.of("src/main/resources/assets/compagnon/textures/font/icones.png");

	private static final Path SOURCE =
			Path.of("src/main/java/fr/lhdp/compagnon/Icones.java");

	/** La taille d'un glyphe, celle qu'annonce le fichier de police. */
	private static final int COTE = 8;

	private static JsonObject fournisseur() throws IOException {
		JsonObject racine = JsonParser
				.parseString(Files.readString(POLICE, StandardCharsets.UTF_8))
				.getAsJsonObject();
		JsonArray fournisseurs = racine.getAsJsonArray("providers");
		assertTrue(fournisseurs != null && !fournisseurs.isEmpty(),
				"la police ne declare aucun fournisseur");
		return fournisseurs.get(0).getAsJsonObject();
	}

	/** Tous les caracteres que la police sait dessiner. */
	private static Set<Character> dessines() throws IOException {
		Set<Character> trouves = new LinkedHashSet<>();
		for (var rangee : fournisseur().getAsJsonArray("chars")) {
			for (char c : rangee.getAsString().toCharArray()) {
				if (c != ' ') {
					trouves.add(c);
				}
			}
		}
		return trouves;
	}

	/** Les caracteres cites par les constantes de {@link Icones}. */
	private static Set<Character> cites() throws IOException {
		Set<Character> trouves = new LinkedHashSet<>();
		Matcher chercheur = Pattern
				.compile("public static final String [A-Z_]+ = \"\\\\u([0-9a-fA-F]{4})\";")
				.matcher(Files.readString(SOURCE, StandardCharsets.UTF_8));
		while (chercheur.find()) {
			trouves.add((char) Integer.parseInt(chercheur.group(1), 16));
		}
		return trouves;
	}

	@Test
	@DisplayName("la police et son image sont livrees")
	void lesFichiersSontLa() {
		assertTrue(Files.exists(POLICE), "fichier de police manquant : " + POLICE);
		assertTrue(Files.exists(IMAGE), "image de police manquante : " + IMAGE);
	}

	@Test
	@DisplayName("chaque icone du code existe dans la police")
	void aucunCarreBlanc() throws IOException {
		Set<Character> dessines = dessines();
		List<String> manquants = new ArrayList<>();
		for (char c : cites()) {
			if (!dessines.contains(c)) {
				manquants.add(String.format("U+%04X", (int) c));
			}
		}
		assertTrue(manquants.isEmpty(),
				"ces icones s'afficheraient en carre blanc : " + String.join(", ", manquants));
	}

	@Test
	@DisplayName("le code et la police comptent le meme nombre d'icones")
	void memeCompte() throws IOException {
		assertEquals(Icones.COMBIEN, cites().size(),
				"Icones.COMBIEN ne correspond plus au nombre de constantes");
		assertEquals(Icones.COMBIEN, dessines().size(),
				"la police ne dessine pas le meme nombre d'icones que le code en declare");
	}

	@Test
	@DisplayName("l'image est assez grande pour la grille annoncee")
	void limageSuffit() throws IOException {
		assertEquals(COTE, fournisseur().get("height").getAsInt(),
				"la hauteur declaree ne correspond plus a la taille d'un glyphe");

		// L'en-tete d'un PNG donne ses dimensions aux octets 16 a 23. Pas besoin
		// de decoder l'image pour verifier qu'elle a la bonne taille.
		byte[] octets = Files.readAllBytes(IMAGE);
		assertTrue(octets.length > 24, "image de police tronquee");

		int largeur = ((octets[16] & 0xFF) << 24) | ((octets[17] & 0xFF) << 16)
				| ((octets[18] & 0xFF) << 8) | (octets[19] & 0xFF);
		int hauteur = ((octets[20] & 0xFF) << 24) | ((octets[21] & 0xFF) << 16)
				| ((octets[22] & 0xFF) << 8) | (octets[23] & 0xFF);

		JsonArray rangees = fournisseur().getAsJsonArray("chars");
		int colonnes = rangees.get(0).getAsString().length();

		assertEquals(colonnes * COTE, largeur,
				"l'image ne fait pas la largeur de la grille declaree");
		assertEquals(rangees.size() * COTE, hauteur,
				"l'image ne fait pas la hauteur de la grille declaree");
	}
}
