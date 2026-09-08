package fr.lhdp.compagnon.progression;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Les points annonces au moment d'une montee de niveau. */
class MonteeTest {

	@Test
	@DisplayName("un point est annonce tous les cinq niveaux")
	void unPointAuPalier() {
		assertEquals(0, Montee.pointsGagnes(3, 4, 5));
		assertEquals(1, Montee.pointsGagnes(4, 5, 5));
		assertEquals(0, Montee.pointsGagnes(5, 6, 5));
	}

	@Test
	@DisplayName("plusieurs niveaux franchis annoncent tous leurs points")
	void plusieursPaliers() {
		assertEquals(2, Montee.pointsGagnes(4, 12, 5));
		assertEquals(3, Montee.pointsGagnes(9, 20, 5));
	}

	@Test
	@DisplayName("un reglage nul desactive les points")
	void pointsDesactives() {
		assertEquals(0, Montee.pointsGagnes(4, 20, 0));
		assertEquals(0, Montee.pointsGagnes(4, 20, -1));
		assertEquals(0, Montee.pointsGagnes(8, 8, 5));
	}
}
