package fr.lhdp.compagnon.entite;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le budget d'attention decide si le mod peut porter trente comportements.
 *
 * <p>S'il laisse passer deux gestes dans le meme tick, une bete a qui on a
 * ajoute dix petites habitudes se met a en jouer quatre a la fois — et le
 * resultat n'est pas plus vivant, il est casse.
 *
 * <p>S'il retient un geste trop longtemps, l'inverse : on ajoute du contenu qui
 * ne se voit jamais.
 */
class AttentionTest {

	private static void avancer(Attention attention, int ticks) {
		for (int i = 0; i < ticks; i++) {
			attention.avancer();
		}
	}

	@Test
	@DisplayName("deux gestes ne passent pas dans le meme tick")
	void unSeulALaFois() {
		Attention attention = new Attention();
		assertTrue(attention.permet("bailler", 100));
		assertFalse(attention.permet("secouer", 100), "le repos general doit tenir");
	}

	@Test
	@DisplayName("apres le repos, un autre geste passe")
	void leReposFinitParPasser() {
		Attention attention = new Attention();
		assertTrue(attention.permet("bailler", 10000));
		avancer(attention, 20 * 3);
		assertTrue(attention.permet("secouer", 10000));
	}

	@Test
	@DisplayName("le meme geste attend son propre delai, bien plus long")
	void pasDeuxFoisLaMemeIdee() {
		Attention attention = new Attention();
		assertTrue(attention.permet("bailler", 20 * 60));
		avancer(attention, 20 * 10);
		assertFalse(attention.permet("bailler", 20 * 60), "dix secondes ne suffisent pas");
		avancer(attention, 20 * 60);
		assertTrue(attention.permet("bailler", 20 * 60));
	}

	@Test
	@DisplayName("demander refuse ne consomme rien")
	void unRefusNeCouteRien() {
		Attention attention = new Attention();
		assertTrue(attention.permet("bailler", 100));
		assertFalse(attention.permet("secouer", 100));
		avancer(attention, 20 * 3);
		assertTrue(attention.permet("secouer", 100),
				"le refus precedent ne doit pas avoir pose de delai sur secouer");
	}

	@Test
	@DisplayName("un ordre fait taire les petits gestes")
	void leSilence() {
		Attention attention = new Attention();
		attention.faireSilence(20 * 5);
		assertFalse(attention.permet("bailler", 100));
		avancer(attention, 20 * 5);
		assertTrue(attention.permet("bailler", 100));
	}

	@Test
	@DisplayName("la table ne grandit pas sans fin")
	void elleOublie() {
		Attention attention = new Attention();
		for (int i = 0; i < 200; i++) {
			avancer(attention, 20 * 3);
			assertTrue(attention.permet("geste" + i, 20 * 10),
					"un geste jamais fait doit toujours passer");
		}
		// Rien a verifier de plus qu'un fonctionnement continu : si la table
		// grandissait sans fin, c'est la memoire du serveur qui le dirait, pas ce
		// test. Il protege surtout le fait qu'oublier ne casse pas le reste.
		avancer(attention, 20 * 3);
		assertTrue(attention.permet("encore", 100));
	}
}
