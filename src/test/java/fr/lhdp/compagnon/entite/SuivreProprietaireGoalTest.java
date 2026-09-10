package fr.lhdp.compagnon.entite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuivreProprietaireGoalTest {

	@Test
	void memeTresAttacheIlNeViseJamaisLeCorpsDuJoueur() {
		assertEquals(SuivreProprietaireGoal.ESPACE_MINIMUM,
				SuivreProprietaireGoal.distancePersonnelle(1.0F), 0.0001D);
		assertTrue(SuivreProprietaireGoal.distancePersonnelle(1.0F) > 1.0D);
	}

	@Test
	void unIndependantGardeDavantageDePlace() {
		assertTrue(SuivreProprietaireGoal.distancePersonnelle(0.0F)
				> SuivreProprietaireGoal.distancePersonnelle(1.0F));
	}

	@Test
	void lesValeursDeCaractereInvalidesRestentBornees() {
		assertEquals(SuivreProprietaireGoal.distancePersonnelle(1.0F),
				SuivreProprietaireGoal.distancePersonnelle(8.0F), 0.0001D);
		assertEquals(SuivreProprietaireGoal.distancePersonnelle(0.0F),
				SuivreProprietaireGoal.distancePersonnelle(-8.0F), 0.0001D);
	}
}
