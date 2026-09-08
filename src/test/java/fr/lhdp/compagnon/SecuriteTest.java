package fr.lhdp.compagnon;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ce qu'un joueur ordinaire ne doit pas pouvoir faire.
 *
 * <h2>Pourquoi ces tests lisent le code source</h2>
 *
 * <p>C'est inhabituel, et c'est assume. Les regles verifiees ici ne vivent pas
 * dans une valeur qu'on peut appeler : elles vivent dans la <b>forme</b> du code
 * — un {@code requires} pose au bon endroit, un paquet enregistre dans le bon
 * sens, un import qui ne doit pas exister.
 *
 * <p>Les verifier autrement demanderait de lancer un serveur, un client, et un
 * joueur malveillant. Les lire prend deux millisecondes et attrape exactement
 * les memes fautes — celles qu'on introduit en ajoutant une commande sans
 * reflechir, un mois plus tard, un soir.
 *
 * <h2>Les deux niveaux</h2>
 *
 * <p><b>Niveau 1, les eleves.</b> Aucune commande. Ils ont le livre, la roue, le
 * carnet, la voix et le clic droit — et rien d'autre. Un joueur ne doit jamais
 * avoir besoin de taper quoi que ce soit pour jouer.
 *
 * <p><b>Niveau 2, l'equipe.</b> Tout.
 */
class SecuriteTest {

	private static final Path SOURCES_SERVEUR = Path.of("src/main/java");
	private static final Path RESEAU = Path.of("src/main/java/fr/lhdp/compagnon/reseau");
	private static final Path COMMANDES =
			Path.of("src/main/java/fr/lhdp/compagnon/commande/Commandes.java");
	private static final Path MANIFESTE = Path.of("src/main/resources/fabric.mod.json");

	// --- Niveau 1 : aucune commande -------------------------------------------------

	/**
	 * Toutes les commandes du mod pendent d'une seule racine, et cette racine
	 * exige le niveau 2.
	 *
	 * <p>C'est ainsi que Brigadier ferme un arbre entier : un joueur sans le
	 * niveau ne peut pas executer les branches, et ne les voit meme pas dans
	 * l'autocompletion. Une seconde racine ajoutee un jour sans {@code requires}
	 * ouvrirait tout, en silence.
	 */
	@Test
	@DisplayName("toutes les commandes exigent le niveau 2")
	void aucuneCommandePourLesJoueurs() throws IOException {
		String source = Files.readString(COMMANDES, StandardCharsets.UTF_8);

		List<String> racines = trouver(source, "repartiteur\\.register\\(\\s*Commands\\.literal\\(\"([a-z_]+)\"\\)");
		assertEquals(1, racines.size(),
				"une seule racine de commande, sinon la garde est a poser deux fois : " + racines);

		int racine = source.indexOf("repartiteur.register(");
		int garde = source.indexOf("hasPermission(2)", racine);
		int premiereBranche = source.indexOf(".then(", racine);

		assertTrue(garde > 0, "la racine des commandes n'exige aucun niveau");
		assertTrue(garde < premiereBranche,
				"la garde de niveau doit venir AVANT la premiere branche, sinon elle "
						+ "ne couvre pas tout l'arbre");
	}

	@Test
	@DisplayName("le mod n'enregistre aucune autre commande ailleurs")
	void pasDeCommandeCachee() throws IOException {
		List<String> fautifs = new ArrayList<>();
		for (Path fichier : sources(SOURCES_SERVEUR)) {
			if (fichier.equals(COMMANDES)) {
				continue;
			}
			String source = Files.readString(fichier, StandardCharsets.UTF_8);
			if (source.contains("CommandRegistrationCallback")) {
				fautifs.add(fichier.toString());
			}
		}
		assertTrue(fautifs.isEmpty(),
				"des commandes sont enregistrees hors de Commandes.java, donc hors de "
						+ "sa garde de niveau : " + fautifs);
	}

	// --- Les paquets : la vraie porte d'entree ----------------------------------------

	/**
	 * Un paquet enregistre dans le mauvais sens est une porte ouverte.
	 *
	 * <p>Un paquet de <b>donnees</b> (serveur vers client) enregistre par erreur
	 * en client vers serveur laisserait un client fabrique en envoyer un. Selon
	 * le paquet, cela reviendrait a s'inventer des compagnons ou a se debloquer
	 * des animations.
	 */
	@Test
	@DisplayName("aucun paquet n'est enregistre dans les deux sens")
	void chaquePaquetVaDansUnSeulSens() throws IOException {
		String source = Files.readString(
				RESEAU.resolve("Reseau.java"), StandardCharsets.UTF_8);

		Set<String> versLeServeur = new LinkedHashSet<>(
				trouver(source, "playC2S\\(\\)\\.register\\((Paquet[A-Za-z]+)\\.TYPE"));
		Set<String> versLeClient = new LinkedHashSet<>(
				trouver(source, "playS2C\\(\\)\\.register\\((Paquet[A-Za-z]+)\\.TYPE"));

		assertFalse(versLeServeur.isEmpty(), "aucun paquet client vers serveur");
		assertFalse(versLeClient.isEmpty(), "aucun paquet serveur vers client");

		Set<String> lesDeux = new LinkedHashSet<>(versLeServeur);
		lesDeux.retainAll(versLeClient);
		assertTrue(lesDeux.isEmpty(), "enregistres dans les deux sens : " + lesDeux);

		// Un paquet de donnees ne remonte jamais : c'est une reponse, pas une
		// demande. Le nom le dit, et le nom doit rester vrai.
		for (String paquet : versLeServeur) {
			assertFalse(paquet.startsWith("PaquetDonnees"),
					paquet + " est un paquet de donnees : il ne doit pas pouvoir "
							+ "etre envoye par un client");
		}
	}

	/**
	 * Tout paquet que le client peut envoyer doit etre traite quelque part.
	 *
	 * <p>Un type enregistre sans recepteur n'est pas dangereux en soi, mais c'est
	 * toujours le signe d'un oubli : soit le traitement manque, soit le type ne
	 * devrait plus etre la.
	 */
	@Test
	@DisplayName("chaque paquet montant est traite par le serveur")
	void aucunPaquetMontantSansTraitement() throws IOException {
		String source = Files.readString(
				RESEAU.resolve("Reseau.java"), StandardCharsets.UTF_8);

		for (String paquet : trouver(source, "playC2S\\(\\)\\.register\\((Paquet[A-Za-z]+)\\.TYPE")) {
			assertTrue(source.contains("registerGlobalReceiver(" + paquet + ".TYPE"),
					paquet + " peut etre envoye par un client, mais rien ne le traite");
		}
	}

	/**
	 * Le client ne designe jamais un compagnon par son identifiant.
	 *
	 * <p>Il envoie un <b>numero dans sa propre liste</b>, que le serveur resout
	 * dans les fiches de ce joueur-la et ramene dans les bornes. Un client
	 * fabrique ne peut donc pas nommer la bete de quelqu'un d'autre : le pire
	 * qu'il puisse faire est de designer une des siennes.
	 *
	 * <p>Un identifiant dans un paquet montant casserait cette garantie d'un
	 * coup, et c'est le genre de raccourci qu'on prend sans y penser.
	 */
	@Test
	@DisplayName("aucun paquet montant ne transporte l'identifiant d'un compagnon")
	void leClientNeDesignePasParIdentifiant() throws IOException {
		String reseau = Files.readString(
				RESEAU.resolve("Reseau.java"), StandardCharsets.UTF_8);
		List<String> montants = trouver(reseau,
				"playC2S\\(\\)\\.register\\((Paquet[A-Za-z]+)\\.TYPE");

		List<String> fautifs = new ArrayList<>();
		for (String paquet : montants) {
			Path fichier = RESEAU.resolve(paquet + ".java");
			if (!Files.exists(fichier)) {
				continue;
			}
			if (Files.readString(fichier, StandardCharsets.UTF_8).contains("UUID")) {
				fautifs.add(paquet);
			}
		}
		assertTrue(fautifs.isEmpty(),
				"ces paquets montants portent un identifiant, donc peuvent designer "
						+ "le compagnon d'un autre : " + fautifs);
	}

	// --- Le mod tourne des deux cotes ---------------------------------------------------

	/**
	 * Rien du client ne doit se trouver dans le code commun.
	 *
	 * <p>Gradle l'empeche deja a la compilation, grace aux sources separees. Ce
	 * test tient malgre tout : il coute une milliseconde, il est lisible, et il
	 * dit <b>pourquoi</b> la regle existe — un serveur dedie n'a pas ces classes,
	 * et il tomberait au chargement.
	 */
	@Test
	@DisplayName("le code serveur ne touche a rien du client")
	void leServeurIgnoreLeClient() throws IOException {
		Pattern interdit = Pattern.compile(
				"^import (net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.)", Pattern.MULTILINE);

		List<String> fautifs = new ArrayList<>();
		for (Path fichier : sources(SOURCES_SERVEUR)) {
			if (interdit.matcher(Files.readString(fichier, StandardCharsets.UTF_8)).find()) {
				fautifs.add(fichier.toString());
			}
		}
		assertTrue(fautifs.isEmpty(),
				"un serveur dedie tomberait au chargement a cause de : " + fautifs);
	}

	@Test
	@DisplayName("le manifeste declare les deux cotes correctement")
	void leManifesteEstJuste() throws IOException {
		JsonObject racine = JsonParser.parseString(
				Files.readString(MANIFESTE, StandardCharsets.UTF_8)).getAsJsonObject();

		assertEquals("*", racine.get("environment").getAsString(),
				"le mod tourne des deux cotes : l'environnement doit rester \"*\"");

		JsonObject points = racine.getAsJsonObject("entrypoints");
		assertTrue(points.has("main"), "il faut un point d'entree commun");
		assertTrue(points.has("client"), "il faut un point d'entree client");

		// Le point d'entree commun ne doit surtout pas etre une classe cliente :
		// c'est celui que le serveur dedie charge.
		String commun = points.getAsJsonArray("main").get(0).getAsString();
		assertFalse(commun.contains(".client."),
				"le point d'entree commun est une classe cliente : " + commun);

		// Chaque mixin qui touche au client doit le dire, sinon le serveur essaie
		// de l'appliquer a des classes qu'il n'a pas.
		if (racine.has("mixins")) {
			JsonArray mixins = racine.getAsJsonArray("mixins");
			for (int i = 0; i < mixins.size(); i++) {
				JsonObject mixin = mixins.get(i).getAsJsonObject();
				String config = mixin.get("config").getAsString();
				if (config.contains("client")) {
					assertEquals("client", mixin.get("environment").getAsString(),
							config + " doit etre marque \"client\"");
				}
			}
		}
	}

	// --- Outils -------------------------------------------------------------------------

	private static List<Path> sources(Path racine) throws IOException {
		try (Stream<Path> chemins = Files.walk(racine)) {
			return chemins.filter(p -> p.toString().endsWith(".java")).toList();
		} catch (UncheckedIOException echec) {
			throw new IOException(echec);
		}
	}

	private static List<String> trouver(String source, String motif) {
		Matcher chercheur = Pattern.compile(motif).matcher(source);
		List<String> trouves = new ArrayList<>();
		while (chercheur.find()) {
			trouves.add(chercheur.group(1));
		}
		return trouves;
	}
}
