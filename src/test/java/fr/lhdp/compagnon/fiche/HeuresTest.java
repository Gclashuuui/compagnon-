package fr.lhdp.compagnon.fiche;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * L'heure tourne en rond, et c'est tout le piege.
 *
 * <p>Vingt-trois heures et une heure sont voisines : deux heures d'ecart, pas
 * vingt-deux. Une simple soustraction dit le contraire, et un compagnon nourri
 * chaque soir a vingt-trois heures n'aurait jamais eu d'habitude — ses repas
 * auraient paru dispersés sur tout le cadran.
 *
 * <p>C'est une faute qu'on ne voit pas en jouant l'apres-midi.
 */
class HeuresTest {

	@Test
	@DisplayName("deux heures voisines sont proches, meme a cheval sur minuit")
	void leCadranTourne() {
		assertEquals(2, FicheCompagnon.ecartDHeures(23, 1));
		assertEquals(1, FicheCompagnon.ecartDHeures(0, 23));
		assertEquals(2, FicheCompagnon.ecartDHeures(1, 23));
	}

	@Test
	@DisplayName("l'ecart ne depasse jamais la moitie du cadran")
	void jamaisPlusDeDouze() {
		for (int a = 0; a < 24; a++) {
			for (int b = 0; b < 24; b++) {
				int ecart = FicheCompagnon.ecartDHeures(a, b);
				assertEquals(ecart, FicheCompagnon.ecartDHeures(b, a),
						"l'ecart doit etre le meme dans les deux sens");
				if (ecart > 12) {
					throw new AssertionError(a + " et " + b + " : " + ecart);
				}
			}
		}
	}

	@Test
	@DisplayName("la meme heure ne s'ecarte pas d'elle-meme")
	void midiEstMidi() {
		assertEquals(0, FicheCompagnon.ecartDHeures(12, 12));
		assertEquals(12, FicheCompagnon.ecartDHeures(0, 12));
	}
}
