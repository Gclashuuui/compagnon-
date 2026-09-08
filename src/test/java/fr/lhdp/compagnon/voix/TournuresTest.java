package fr.lhdp.compagnon.voix;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les soixante-dix facons de parler a son compagnon.
 *
 * <h2>Pourquoi ces tests existent</h2>
 *
 * <p>Le fichier {@code voix.json} a grossi d'un coup : de vingt tournures a
 * soixante-dix, parce qu'on ne dit pas tous « couche » — on dit « allonge-toi »,
 * « va dormir », « repose-toi ».
 *
 * <p>Plus il y en a, plus le risque grandit qu'une tournure en <b>masque</b> une
 * autre. Le moteur retient la plus longue qui colle : le jour ou quelqu'un ecrira
 * « va chercher » dans un ordre et « va » tout court dans un autre, l'un des deux
 * cessera de marcher — <b>en silence</b>, et seulement pour ceux qui emploient
 * cette tournure-la.
 *
 * <p>Ce genre de panne ne se decouvre qu'au micro, par hasard, des semaines plus
 * tard. Ces tests la trouvent en une seconde et sans lancer Minecraft.
 */
class TournuresTest {

	private static final Path FICHIER =
			Path.of("src/main/resources/data/compagnon/voix.json");

	@BeforeAll
	static void lireLeVraiFichier() throws IOException {
		try (Reader lecteur = Files.newBufferedReader(FICHIER, StandardCharsets.UTF_8)) {
			JsonObject racine = JsonParser.parseReader(lecteur).getAsJsonObject();
			Vocabulaire.poser(racine);
		}
	}

	/**
	 * Le vocabulaire est global au mod : ce qu’on charge ici resterait charge
	 * pour les classes de test suivantes, et l’une d’elles verifie justement
	 * qu’un mod sans fichier de voix ne reconnait rien. On rend donc la place
	 * propre.
	 *
	 * <p>Un test qui en fait echouer un autre est pire qu’un test absent : on
	 * cherche le defaut dans le code, alors qu’il est dans l’ordre des tests.
	 */
	@AfterAll
	static void rendreLaPlacePropre() {
		Vocabulaire.poser(JsonParser.parseString("{\"commandes\":{}}").getAsJsonObject());
	}

	@Test
	@DisplayName("chaque ordre a au moins une facon de se dire")
	void personneNEstOublie() {
		for (CommandeVocale commande : CommandeVocale.values()) {
			assertFalse(Vocabulaire.motsDe(commande).isEmpty(),
					"aucune tournure pour " + commande.cle());
		}
	}

	/**
	 * Le test qui compte : dire une tournure declenche <b>son</b> ordre.
	 *
	 * <p>Il attrape le masquage dans les deux sens — une tournure trop courte
	 * qu'une plus longue avale, et une tournure d'un autre ordre qui gagne parce
	 * qu'elle contient les memes mots.
	 */
	@Test
	@DisplayName("chaque tournure declenche son propre ordre")
	void aucuneNEnMasqueUneAutre() {
		List<String> fautes = new ArrayList<>();

		for (CommandeVocale attendu : CommandeVocale.values()) {
			for (String tournure : Vocabulaire.motsDe(attendu)) {
				CommandeVocale trouve = Vocabulaire.reconnaitre(tournure);
				if (trouve != attendu) {
					fautes.add("« " + tournure + " » devrait donner " + attendu.cle()
							+ " mais donne " + (trouve == null ? "rien" : trouve.cle()));
				}
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	@Test
	@DisplayName("la meme tournure n'appartient jamais a deux ordres")
	void pasDeDoublon() {
		Set<String> vues = new HashSet<>();
		for (CommandeVocale commande : CommandeVocale.values()) {
			for (String tournure : Vocabulaire.motsDe(commande)) {
				assertTrue(vues.add(Vocabulaire.pourLaComparaison(tournure)),
						"« " + tournure + " » apparait deux fois");
			}
		}
	}

	/**
	 * Chaque nom dit avant l'ordre doit continuer de marcher.
	 *
	 * <p>C'est la facon normale de parler a sa bete : « Sabre, viens ici ». Si un
	 * nom colle a un mot d'ordre, la phrase entiere doit quand meme se reconnaitre.
	 */
	@Test
	@DisplayName("le nom devant ne gene pas la reconnaissance")
	void leNomDevantNeGenePas() {
		for (String nom : List.of("sabre", "cacahuete", "mouette", "l'ombre")) {
			for (CommandeVocale attendu : CommandeVocale.values()) {
				for (String tournure : Vocabulaire.motsDe(attendu)) {
					assertEquals(attendu, Vocabulaire.reconnaitre(nom + " " + tournure),
							"« " + nom + " " + tournure + " » n'est plus compris");
				}
			}
		}
	}

	@Test
	@DisplayName("la grammaire donnee au moteur ne contient aucun mot vide")
	void aucunMotVide() {
		List<String> tous = Vocabulaire.tousLesMots();
		assertFalse(tous.isEmpty());
		for (String mot : tous) {
			assertFalse(mot.isBlank(), "un mot vide dans la grammaire");
			assertFalse(mot.contains(" "), "« " + mot + " » n'est pas un mot isole");
		}
	}

	/**
	 * Une phrase quelconque ne doit rien declencher.
	 *
	 * <p>Le moteur n'entend que les mots de la grammaire, mais la commande d'essai
	 * accepte n'importe quoi : elle ne doit pas faire s'asseoir un dragon parce
	 * qu'on a tape une phrase au hasard.
	 */
	@Test
	@DisplayName("une phrase sans ordre ne declenche rien")
	void leResteNeDeclencheRien() {
		assertNotNull(Vocabulaire.reconnaitre("viens"));
		for (String phrase : List.of("bonjour tout le monde", "sabre", "il fait beau", "")) {
			assertEquals(null, Vocabulaire.reconnaitre(phrase),
					"« " + phrase + " » ne devrait rien declencher");
		}
	}
}
