package fr.lhdp.compagnon.entite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RythmeQuotidienTest {

	@Test
	void leMatinNeSeJoueQuUneFoisParJour() {
		RythmeQuotidien rythme = new RythmeQuotidien();
		assertEquals(RythmeQuotidien.Moment.MATIN, rythme.prochain(500, 100));
		assertEquals(RythmeQuotidien.Moment.AUCUN, rythme.prochain(700, 100));
		assertEquals(RythmeQuotidien.Moment.MATIN, rythme.prochain(24_500, 100));
	}

	@Test
	void leSoirNeSeJoueQuUneFoisParJour() {
		RythmeQuotidien rythme = new RythmeQuotidien();
		assertEquals(RythmeQuotidien.Moment.SOIR, rythme.prochain(12_800, 200));
		assertEquals(RythmeQuotidien.Moment.AUCUN, rythme.prochain(13_000, 200));
	}

	@Test
	void leDecalageEviteUnDepartSimultane() {
		RythmeQuotidien premier = new RythmeQuotidien();
		RythmeQuotidien second = new RythmeQuotidien();
		assertEquals(RythmeQuotidien.Moment.MATIN, premier.prochain(100, 50));
		assertEquals(RythmeQuotidien.Moment.AUCUN, second.prochain(100, 900));
		assertEquals(RythmeQuotidien.Moment.MATIN, second.prochain(950, 900));
	}

	@Test
	void horsDesFenetresIlNeForceAucunGeste() {
		RythmeQuotidien rythme = new RythmeQuotidien();
		assertEquals(RythmeQuotidien.Moment.AUCUN, rythme.prochain(8_000, 0));
		assertEquals(RythmeQuotidien.Moment.AUCUN, rythme.prochain(18_000, 0));
	}
}
