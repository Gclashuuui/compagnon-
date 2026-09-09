package fr.lhdp.compagnon.entite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NiveauActiviteCerveauTest {

	@Test
	void uneUrgenceResteToujoursImmediate() {
		for (NiveauActiviteCerveau niveau : NiveauActiviteCerveau.values()) {
			assertEquals(1, niveau.cadence(CerveauComportement.Noeud.SURVIE));
			assertEquals(1, niveau.cadence(CerveauComportement.Noeud.RAPPEL));
		}
	}

	@Test
	void lesEnviesDeFondRalentissentAvecLaDistance() {
		int proche = NiveauActiviteCerveau.PROCHE.cadence(
				CerveauComportement.Noeud.FLANERIE);
		int moyen = NiveauActiviteCerveau.MOYEN.cadence(
				CerveauComportement.Noeud.FLANERIE);
		int loin = NiveauActiviteCerveau.LOINTAIN.cadence(
				CerveauComportement.Noeud.FLANERIE);
		assertTrue(proche < moyen);
		assertTrue(moyen < loin);
	}
}
