package fr.lhdp.compagnon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
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
 * Chaque bete a-t-elle une voix qui existe vraiment ?
 *
 * <h2>La panne que ce test rattrape</h2>
 *
 * <p>Une espece emprunte sa voix au jeu : quatre identifiants de son, un par
 * role. Un identifiant mal ecrit ne leve <b>rien</b>. Le jeu cherche le son, ne
 * le trouve pas, et ne joue pas. La bete est simplement muette, pour toujours,
 * et personne ne s'en apercoit avant des semaines — un compagnon silencieux ne
 * ressemble pas a un bug, il ressemble a un compagnon discret.
 *
 * <p>Le bebe stegonaut a vecu comme ca : son cri d'alerte etait
 * {@code entity.goat.screaming_ambient} alors que le jeu ecrit
 * {@code entity.goat.screaming.ambient}. Un tiret bas au lieu d'un point.
 *
 * <h2>D'ou vient la liste de reference</h2>
 *
 * <p>{@code sons_vanilla.txt} est la liste des identifiants declares par
 * {@code net.minecraft.sounds.SoundEvents}, extraite de la table des constantes
 * de sa classe compilee pour la 1.21.1. Ce ne sont que des noms — aucun contenu
 * du jeu n'est copie — et ils ne bougent pas d'une version corrective a l'autre.
 *
 * <p>A refaire le jour d'un changement de version de Minecraft, sinon un son
 * ajoute depuis passerait pour une faute.
 */
class VoixTest {

	private static final Path ESPECES = Path.of("src/main/resources/data/compagnon/especes");
	private static final String REFERENCE = "/sons_vanilla.txt";

	@Test
	void chaqueVoixExisteDansLeJeu() throws IOException {
		Set<String> connus = sonsDuJeu();
		List<String> fautes = new ArrayList<>();

		for (Path espece : fichiers(ESPECES)) {
			JsonObject sons = lire(espece).getAsJsonObject("sons");
			if (sons == null) {
				continue;   // muette, et c'est un choix qui se defend
			}
			for (Map.Entry<String, JsonElement> role : sons.entrySet()) {
				String identifiant = role.getValue().getAsString();
				if (identifiant.isEmpty()) {
					continue;
				}
				// Sans espace de noms, le jeu comprend « minecraft ».
				String court = identifiant.startsWith("minecraft:")
						? identifiant.substring("minecraft:".length())
						: identifiant;
				if (!connus.contains(court)) {
					fautes.add(sansExtension(espece) + " : sons." + role.getKey()
							+ " vaut \"" + identifiant + "\", qui n'existe pas dans le jeu"
							+ "\n\t\t(la bete serait muette pour ce role, sans une ligne nulle part)");
				}
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	/**
	 * Deux especes ne parlent pas de la meme voix.
	 *
	 * <p>Ce n'est pas une regle du jeu, c'est une regle a nous : deux betes qui
	 * font le meme bruit sont deux betes qu'on confond, et tout le travail de
	 * leur donner un caractere tombe. Le petit et l'adulte d'une meme espece ont
	 * le droit de se ressembler, mais pas d'etre identiques.
	 */
	@Test
	void deuxEspecesNeParlentPasExactementPareil() throws IOException {
		Set<String> vues = new HashSet<>();
		List<String> fautes = new ArrayList<>();

		for (Path espece : fichiers(ESPECES)) {
			JsonObject sons = lire(espece).getAsJsonObject("sons");
			if (sons == null || sons.isEmpty()) {
				continue;
			}
			if (!vues.add(sons.toString())) {
				fautes.add(sansExtension(espece) + " a exactement la voix d'une autre espece");
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	/** La liste de reference doit etre la, sinon le test ci-dessus ne prouve rien. */
	@Test
	void laListeDeReferenceEstPresente() throws IOException {
		assertFalse(sonsDuJeu().isEmpty(),
				"sons_vanilla.txt est vide ou absent de src/test/resources");
	}

	// --- Outils ---------------------------------------------------------------------

	private static Set<String> sonsDuJeu() throws IOException {
		try (InputStream flux = VoixTest.class.getResourceAsStream(REFERENCE)) {
			if (flux == null) {
				return Set.of();
			}
			return new HashSet<>(List.of(
					new String(flux.readAllBytes(), StandardCharsets.UTF_8).split("\\R")));
		}
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
