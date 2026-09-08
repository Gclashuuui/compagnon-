package fr.lhdp.compagnon.voix;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Crier, c'est parler plus fort <b>que soi</b>.
 *
 * <h2>Ce que ces tests garantissent</h2>
 *
 * <p>Un seuil absolu aurait ete plus simple a ecrire, et profondement injuste :
 * il aurait recompense ceux qui ont un bon micro, un preampli genereux, une voix
 * qui porte — et prive les autres d'une fonction du jeu pour une raison qui n'a
 * rien a voir avec le jeu.
 *
 * <p>C'est la meme regle que le seuil de silence a 900 ms : le mod s'adapte a la
 * personne, pas l'inverse. Ces tests la tiennent, et ils sont ecrits pour
 * echouer si quelqu'un revient un jour a un seuil fixe.
 */
class TonTest {

	/** Quelqu'un dont le micro rend des valeurs faibles. */
	private static final double VOIX_BASSE = 900.0D;

	/** Quelqu'un dont le micro rend des valeurs elevees, pour les memes mots. */
	private static final double VOIX_FORTE = 9000.0D;

	@Test
	@DisplayName("parler bas ne compte jamais pour un cri")
	void leCalmeNestPasUnCri() {
		UUID joueur = UUID.randomUUID();
		for (int i = 0; i < 10; i++) {
			assertFalse(Voix.aCrie(joueur, VOIX_BASSE),
					"une phrase ordinaire ne doit jamais passer pour un cri");
		}
	}

	/**
	 * Le test qui compte : deux micros tres differents, le meme resultat.
	 *
	 * <p>Le joueur a voix basse crie a un volume ou l'autre murmure encore. Les
	 * deux doivent etre entendus de la meme facon.
	 */
	@Test
	@DisplayName("le cri se mesure par rapport a soi, pas dans l'absolu")
	void chacunSonEchelle() {
		UUID discret = UUID.randomUUID();
		UUID sonore = UUID.randomUUID();

		for (int i = 0; i < 6; i++) {
			Voix.aCrie(discret, VOIX_BASSE);
			Voix.aCrie(sonore, VOIX_FORTE);
		}

		assertTrue(Voix.aCrie(discret, VOIX_BASSE * 3.0D),
				"celui qui parle bas doit pouvoir crier");
		assertTrue(Voix.aCrie(sonore, VOIX_FORTE * 3.0D),
				"celui qui parle fort aussi");

		// Et le volume qui est un cri pour l'un reste une voix ordinaire pour
		// l'autre. C'est tout le principe.
		assertFalse(Voix.aCrie(sonore, VOIX_BASSE * 3.0D),
				"le cri du discret ne doit pas etre un cri pour le sonore");
	}

	@Test
	@DisplayName("on ne juge pas avant d'avoir entendu la voix ordinaire")
	void prudentAuDemarrage() {
		UUID inconnu = UUID.randomUUID();
		// Premiere phrase, et forte : on ne sait pas encore si c'est sa voix
		// normale. Mieux vaut ne pas voir un cri que d'en inventer un.
		assertFalse(Voix.aCrie(inconnu, VOIX_FORTE));
		assertFalse(Voix.aCrie(inconnu, VOIX_FORTE));
	}

	@Test
	@DisplayName("un souffle n'est pas une voix")
	void leSouffleNeComptePas() {
		UUID joueur = UUID.randomUUID();
		for (int i = 0; i < 6; i++) {
			Voix.aCrie(joueur, VOIX_BASSE);
		}
		// Sous le plancher : ni un cri, ni de quoi tirer la moyenne vers le bas.
		assertFalse(Voix.aCrie(joueur, 10.0D));
		assertTrue(Voix.aCrie(joueur, VOIX_BASSE * 3.0D),
				"la moyenne ne doit pas avoir bouge a cause du souffle");
	}
}
