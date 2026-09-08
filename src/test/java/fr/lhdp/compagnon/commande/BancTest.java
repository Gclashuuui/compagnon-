package fr.lhdp.compagnon.commande;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le banc d'essai ne doit rien laisser derriere lui.
 *
 * <h2>Ce qu'on protege</h2>
 *
 * <p>{@code /compagnon banc} fabrique une foule de faux compagnons et fait
 * tourner du vrai code des milliers de fois. C'est exactement ce qu'on veut
 * pour mesurer — et exactement ce qui, mal ecrit, abime une partie :
 *
 * <ul>
 *   <li>dix mille passages de mesure qui rempliraient dix mille fois « pose un
 *       bloc » fausseraient les missions de tout le monde ;</li>
 *   <li>deux cents fausses betes ecrites dans la sauvegarde se reveilleraient au
 *       redemarrage, sans fiche, sans proprietaire, et pour toujours.</li>
 * </ul>
 *
 * <p>Les deux gardes tiennent en une condition chacune, et c'est bien le
 * probleme : une condition d'une ligne se supprime sans y penser. Ce test lit
 * donc les <b>sources</b> — comme {@code SecuriteTest} — pour qu'on s'en
 * apercoive a la compilation et non trois semaines plus tard.
 */
class BancTest {

	private static final Path CURIOSITE =
			Path.of("src/main/java/fr/lhdp/compagnon/entite/Curiosite.java");

	private static final Path ENTITE =
			Path.of("src/main/java/fr/lhdp/compagnon/entite/CompagnonEntity.java");

	private static String lire(Path fichier) throws IOException {
		return Files.readString(fichier, StandardCharsets.UTF_8);
	}

	@Test
	@DisplayName("une mesure ne compte jamais rien")
	void laMesureNeCompteRien() throws IOException {
		String source = lire(CURIOSITE);
		int debut = source.indexOf("public static void mesurerUnPassage");
		assertTrue(debut > 0, "mesurerUnPassage a disparu : le banc ne mesure plus rien");

		int fin = source.indexOf("\n\t}", debut);
		String corps = source.substring(debut, fin < 0 ? source.length() : fin);

		assertTrue(corps.contains("null"),
				"mesurerUnPassage doit passer un compteur nul. Sans ca, chaque mesure "
						+ "remplit les missions de tous les compagnons a portee :\n" + corps);
		assertTrue(!corps.contains("Compteurs."),
				"mesurerUnPassage ne doit citer aucun compteur :\n" + corps);
	}

	@Test
	@DisplayName("une bete du banc n'est jamais ecrite dans la sauvegarde")
	void leBancNeSeSauvegardePas() throws IOException {
		String source = lire(ENTITE);
		int debut = source.indexOf("public boolean shouldBeSaved()");
		assertTrue(debut > 0, "shouldBeSaved a disparu");

		int fin = source.indexOf("\n\t}", debut);
		String corps = source.substring(debut, fin < 0 ? source.length() : fin);

		assertTrue(corps.contains("banc"),
				"shouldBeSaved doit refuser les betes du banc. Elles n'ont pas de fiche, "
						+ "donc sans cette condition elles passent par la porte laissee "
						+ "ouverte aux autres :\n" + corps);
	}

	@Test
	@DisplayName("le banc reste borne")
	void leBancEstBorne() {
		assertTrue(Banc.MAXIMUM > 0 && Banc.MAXIMUM <= 2000,
				"une borne absurde fabrique un probleme au lieu de le mesurer : "
						+ Banc.MAXIMUM);
		assertTrue(Banc.PASSAGES_MAXIMUM > 0 && Banc.PASSAGES_MAXIMUM <= 1_000_000,
				"trop de passages bloquent le serveur le temps de la mesure : "
						+ Banc.PASSAGES_MAXIMUM);
	}
}
