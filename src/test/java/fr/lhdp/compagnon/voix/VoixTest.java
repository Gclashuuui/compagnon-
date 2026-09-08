package fr.lhdp.compagnon.voix;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La logique de la voix, sans micro, sans serveur et sans Minecraft.
 *
 * <p>Tout ce qui est teste ici est du texte et des nombres. Le morceau qui a
 * besoin d'un vrai moteur — capter, dechiffrer, transcrire — est ailleurs et ne
 * se teste qu'en jeu.
 */
class VoixTest {

	/**
	 * Les deux mises en forme font <b>l'inverse</b> l'une de l'autre sur les
	 * accents, et c'est le piege le plus facile a « corriger » par erreur.
	 *
	 * <p>Le moteur connait « cacahuète » accentue et pas l'autre : la grammaire
	 * garde donc les accents. La comparaison, elle, doit accepter qu'on prononce
	 * « couche » ou « couché » : elle les retire. Fusionner les deux casserait
	 * l'un des deux cotes, en silence.
	 */
	@Test
	void laGrammaireGardeLesAccentsEtLaComparaisonLesRetire() {
		assertEquals("cacahuète", Vocabulaire.pourLaGrammaire("Cacahuète"));
		assertEquals("cacahuete", Vocabulaire.pourLaComparaison("Cacahuète"));
		assertNotEquals(Vocabulaire.pourLaGrammaire("couché"),
				Vocabulaire.pourLaComparaison("couché"));
	}

	@Test
	void laMiseEnFormeEnleveCeQueLeMoteurRefuse() {
		// Majuscules, ponctuation, chiffres et espaces doubles font rejeter un mot.
		assertEquals("viens ici", Vocabulaire.pourLaGrammaire("  VIENS,   ici ! 42 "));
		// L'apostrophe typographique devient droite : ce sont deux mots differents
		// pour le moteur, et il ne connait que la seconde.
		assertEquals("l'ombre", Vocabulaire.pourLaGrammaire("L’Ombre"));
	}

	@Test
	void unNomVideNeCasseRien() {
		assertEquals("", Vocabulaire.pourLaGrammaire(null));
		assertEquals("", Vocabulaire.pourLaGrammaire("   "));
		assertEquals("", Vocabulaire.pourLaComparaison("!!!"));
	}

	/**
	 * Sans fichier charge, rien n'est reconnu — et surtout, rien n'explose. C'est
	 * l'etat d'un serveur ou {@code voix.json} manque.
	 */
	@Test
	void sansFichierCharge_rienNEstReconnu() {
		assertNull(Vocabulaire.reconnaitre("cacahuete assis"));
		assertTrue(Vocabulaire.tousLesMots().isEmpty());
		assertTrue(Vocabulaire.motsDe(CommandeVocale.ASSIS).isEmpty());
	}

	@Test
	void chaqueOrdreConnaitSaCleEtSonMode() {
		for (CommandeVocale ordre : CommandeVocale.values()) {
			assertEquals(ordre, CommandeVocale.depuis(ordre.cle()));
		}
		assertNull(CommandeVocale.depuis("nimportequoi"));
	}

	// --- Le son ------------------------------------------------------------------

	/**
	 * Un son deja a la bonne frequence, ou plus bas, doit ressortir <b>intact</b>.
	 * L'etirer n'apporterait rien et abimerait la voix.
	 */
	@Test
	void unSonDejaEn16kHzNEstPasTouche() {
		short[] son = { 1, 2, 3, 4, 5 };
		assertEquals(son, Reconnaisseur.ramenerA16k(son, Reconnaisseur.FREQUENCE_MODELE));
		assertEquals(son, Reconnaisseur.ramenerA16k(son, 8_000));
	}

	/**
	 * On MOYENNE, on ne decime pas.
	 *
	 * <p>Prendre un echantillon sur trois replierait les hautes frequences sur la
	 * voix et la rendrait meconnaissable. Avec une source constante, la moyenne
	 * rend exactement la meme valeur — ce qu'une decimation ferait aussi ; on
	 * verifie donc surtout la LONGUEUR, qui est le tiers.
	 */
	@Test
	void leSonEstRameneAuTiersDepuis48kHz() {
		short[] source = new short[300];
		java.util.Arrays.fill(source, (short) 1000);

		short[] sortie = Reconnaisseur.ramenerA16k(source, 48_000);

		assertEquals(100, sortie.length);
		for (short valeur : sortie) {
			assertEquals(1000, valeur);
		}
	}

	/** Les rapports non entiers doivent passer : 44,1 kHz existe. */
	@Test
	void unRapportNonEntierPasseQuandMeme() {
		short[] source = new short[441];
		assertEquals(160, Reconnaisseur.ramenerA16k(source, 44_100).length);
	}

	@Test
	void unSonVideNeCassePas() {
		assertEquals(0, Reconnaisseur.ramenerA16k(new short[0], 48_000).length);
	}

	/**
	 * Le jeton « aucun des mots attendus » est un SILENCE, pas une phrase.
	 *
	 * <p>Le rendre tel quel faisait chercher un compagnon nomme « unk » cinquante
	 * fois par seconde.
	 */
	@Test
	void leJetonInconnuEstTraiteCommeUnSilence() {
		assertNull(Reconnaisseur.texte("{\"text\" : \"[unk]\"}"));
		assertNull(Reconnaisseur.texte("{\"text\" : \"\"}"));
		assertEquals("assis", Reconnaisseur.texte("{\"text\" : \"[unk] assis\"}"));
	}

	@Test
	void unePhraseOrdinaireRessortIntacte() {
		assertEquals("cacahuete assis", Reconnaisseur.texte("{\"text\" : \"cacahuete assis\"}"));
		assertNull(Reconnaisseur.texte("pas du json"));
		assertNull(Reconnaisseur.texte(null));
	}

	/**
	 * La reparation d'accents ne doit <b>jamais</b> abimer un texte sain.
	 *
	 * <p>Elle refait le chemin a l'envers et relit en UTF-8 strict : un texte
	 * correct ne survit pas a l'operation, et on le rend alors tel quel.
	 */
	@Test
	void laReparationDAccentsNAbimePasUnTexteSain() {
		assertEquals("cacahuète", Reconnaisseur.reparerLesAccents("cacahuète"));
		assertEquals("assis", Reconnaisseur.reparerLesAccents("assis"));
		assertEquals("", Reconnaisseur.reparerLesAccents(""));
		assertNull(Reconnaisseur.reparerLesAccents(null));
	}

	/** Et elle repare bien le vrai defaut, vu en jeu : « j'Ã©tais » pour « j'étais ». */
	@Test
	void laReparationDAccentsRepareLeVraiDefaut() {
		assertEquals("j'étais", Reconnaisseur.reparerLesAccents("j'Ã©tais"));
		assertEquals("marché", Reconnaisseur.reparerLesAccents("marchÃ©"));
	}

	/**
	 * La grammaire porte toujours le jeton « inconnu ». Sans lui, le moteur est
	 * oblige de choisir un mot de la liste, et le compagnon obeirait a n'importe
	 * quelle conversation.
	 */
	@Test
	void laGrammairePorteToujoursLeJetonInconnu() {
		String grammaire = Reconnaisseur.grammaire(java.util.List.of("assis", "viens"));
		assertTrue(grammaire.contains("\"assis\""), grammaire);
		assertTrue(grammaire.contains("\"viens\""), grammaire);
		assertTrue(grammaire.contains(Reconnaisseur.INCONNU), grammaire);
		assertTrue(grammaire.startsWith("[") && grammaire.endsWith("]"), grammaire);
	}
}
