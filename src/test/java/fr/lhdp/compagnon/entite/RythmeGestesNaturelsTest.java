package fr.lhdp.compagnon.entite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RythmeGestesNaturelsTest {

	@Test
	void deuxCerveauxIdentiquesGardentLeMemeRythme() {
		RythmeGestesNaturels a = new RythmeGestesNaturels(1234);
		RythmeGestesNaturels b = new RythmeGestesNaturels(1234);
		for (int i = 0; i < 300; i++) {
			assertEquals(a.avancer(10, 1_000, 0.4F, 0.6F),
					b.avancer(10, 1_000, 0.4F, 0.6F));
		}
	}

	@Test
	void lennuiRapprocheLeProchainGeste() {
		RythmeGestesNaturels calme = new RythmeGestesNaturels(7);
		RythmeGestesNaturels ennuye = new RythmeGestesNaturels(7);
		assertFalse(calme.avancer(10, 1_000, 0.0F, 0.5F));
		assertFalse(ennuye.avancer(10, 1_000, 1.0F, 0.5F));
		assertTrue(ennuye.ticksRestants() < calme.ticksRestants());
	}

	@Test
	void aucunGesteNePartImmediatementALapparition() {
		RythmeGestesNaturels rythme = new RythmeGestesNaturels(42);
		assertFalse(rythme.avancer(10, 1_000, 1.0F, 1.0F));
		assertTrue(rythme.ticksRestants() >= 20 * 18);
	}

	@Test
	void leCurseurResteDansLaListe() {
		RythmeGestesNaturels rythme = new RythmeGestesNaturels(-999);
		for (int i = 0; i < 20; i++) {
			int index = rythme.index(5);
			assertTrue(index >= 0 && index < 5);
			rythme.avancer(10_000, 360, 1.0F, 1.0F);
		}
	}
}
