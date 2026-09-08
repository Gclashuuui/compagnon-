package fr.lhdp.compagnon;

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
 * Chaque phrase demandee au fichier de langue doit s'y trouver.
 *
 * <h2>Ce qui arrive quand elle n'y est pas</h2>
 *
 * <p>Rien, sur le moment. Minecraft n'a pas d'erreur pour une traduction
 * manquante : il affiche <b>la cle telle quelle</b>. Le joueur lit alors
 * {@code ordre.compagnon.mode} au milieu de son ecran, ce qui a exactement
 * l'air de ce que c'est — un bout de code oublie la.
 *
 * <p>C'est deja arrive deux fois dans ce mod, et les deux fois on l'a decouvert
 * par une capture d'ecran envoyee par un joueur. Le compilateur ne peut rien
 * dire : une cle de traduction est une chaine de caracteres comme une autre.
 *
 * <h2>Ce que le test regarde, et ce qu'il laisse passer</h2>
 *
 * <p>Uniquement les cles ecrites <b>en toutes lettres</b> —
 * {@code Component.translatable("livre.compagnon.titre")}. Celles qu'on
 * fabrique en collant un prefixe a un nom d'animation ou de compteur ne peuvent
 * pas etre verifiees ici : elles dependent des donnees, et c'est
 * {@code LibellesTest} qui s'en charge, fichier de donnees par fichier de
 * donnees.
 *
 * <p>Le test lit les <b>sources</b>, jamais le code compile : c'est ce qui lui
 * permet de tourner sans demarrer Minecraft.
 */
class TraductionsTest {

	private static final Path LANG =
			Path.of("src/main/resources/assets/compagnon/lang/fr_fr.json");

	private static final Path[] SOURCES = {
		Path.of("src/main/java"),
		Path.of("src/client/java"),
	};

	/**
	 * Une cle citee en clair, et rien d'autre.
	 *
	 * <p>Le {@code [,)]} de la fin est tout l'interet du motif : il ne retient que
	 * les cles <b>completes</b>. Une cle suivie d'un {@code +} est un prefixe
	 * qu'on complete a l'execution — la chercher dans le fichier de langue ferait
	 * echouer le test sur du code parfaitement correct.
	 */
	private static final Pattern CITEE =
			Pattern.compile("Component\\.translatable\\(\\s*\"([A-Za-z0-9_.]+)\"\\s*[,)]");

	private static Set<String> citees() throws IOException {
		Set<String> trouvees = new LinkedHashSet<>();
		for (Path racine : SOURCES) {
			if (!Files.isDirectory(racine)) {
				continue;
			}
			try (Stream<Path> fichiers = Files.walk(racine)) {
				for (Path fichier : fichiers
						.filter(f -> f.toString().endsWith(".java"))
						.toList()) {

					Matcher chercheur =
							CITEE.matcher(Files.readString(fichier, StandardCharsets.UTF_8));
					while (chercheur.find()) {
						trouvees.add(chercheur.group(1));
					}
				}
			}
		}
		return trouvees;
	}

	private static JsonObject langue() throws IOException {
		return JsonParser.parseString(Files.readString(LANG, StandardCharsets.UTF_8))
				.getAsJsonObject();
	}

	@Test
	@DisplayName("le code cite bien des phrases traduites")
	void ilYEnA() throws IOException {
		assertFalse(citees().isEmpty(),
				"aucune cle trouvee : le motif de recherche ne marche plus");
	}

	@Test
	@DisplayName("chaque phrase citee par le code existe en francais")
	void aucuneCleNue() throws IOException {
		JsonObject langue = langue();
		List<String> manquantes = new ArrayList<>();
		for (String cle : citees()) {
			if (!langue.has(cle)) {
				manquantes.add(cle);
			}
		}
		assertTrue(manquantes.isEmpty(),
				"ces cles s'afficheraient telles quelles a l'ecran :\n  "
						+ String.join("\n  ", manquantes));
	}

	/**
	 * Les quatre ordres se lisent partout de la meme facon.
	 *
	 * <p>Leur cle est fabriquee a partir du nom de l'ordre, donc invisible pour le
	 * test precedent. Or c'est justement celle-la qui manquait : le message du
	 * clic droit affichait le nom brut de la valeur pendant que le livre, lui,
	 * affichait la vraie phrase.
	 */
	@Test
	@DisplayName("chaque ordre a sa phrase")
	void lesOrdresSeLisent() throws IOException {
		JsonObject langue = langue();
		List<String> manquants = new ArrayList<>();
		for (String ordre : new String[]{"suit", "reste", "assis", "couche"}) {
			String cle = "livre.compagnon.mode." + ordre;
			if (!langue.has(cle)) {
				manquants.add(cle);
			}
		}
		assertTrue(manquants.isEmpty(), String.join(", ", manquants));
	}

	/**
	 * Chaque case de la roue porte un nom francais.
	 *
	 * <p>Sans etiquette, la roue se rabat sur le nom du fichier d'animation :
	 * le joueur lit « amb call », « head shake », « preen ». Ce n'est pas une
	 * panne — tout marche — c'est pire, ca donne l'air inachevee a la seule
	 * interface qu'on ouvre pour s'amuser.
	 *
	 * <p>Les trente premieres cases ont vecu comme ca sans que personne ne le
	 * signale. Une case ajoutee sans son nom repasserait inapercue de la meme
	 * facon, d'ou ce test.
	 */
	@Test
	@DisplayName("chaque case de la roue a un nom francais")
	void lesCasesDeLaRoueSeLisent() throws IOException {
		JsonObject langue = langue();
		List<String> manquants = new ArrayList<>();

		JsonObject table;
		try (java.io.Reader lecteur = java.nio.file.Files.newBufferedReader(
				Path.of("src/main/resources/data/compagnon/niveaux.json"))) {
			table = com.google.gson.JsonParser.parseReader(lecteur).getAsJsonObject();
		}
		for (com.google.gson.JsonElement niveau : table.getAsJsonArray("niveaux")) {
			com.google.gson.JsonArray debloque =
				niveau.getAsJsonObject().getAsJsonArray("debloque");
			if (debloque == null) {
				continue;
			}
			for (com.google.gson.JsonElement animation : debloque) {
				String cle = "roue.compagnon.action." + animation.getAsString();
				if (!langue.has(cle)) {
					manquants.add(cle);
				}
			}
		}
		assertTrue(manquants.isEmpty(),
			manquants.size() + " cases sans nom francais :\n" + String.join("\n", manquants));
	}
}
