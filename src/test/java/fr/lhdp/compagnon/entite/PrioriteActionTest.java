package fr.lhdp.compagnon.entite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrioriteActionTest {

	@Test
	void uneActionEgaleNeCoupeJamaisCelleQuiEstEngagee() {
		assertFalse(PrioriteAction.AFFECTIF.interrompt(PrioriteAction.AFFECTIF));
		assertFalse(PrioriteAction.AMBIANCE.interrompt(PrioriteAction.AFFECTIF));
	}

	@Test
	void seulUnEvenementPlusImportantInterrompt() {
		assertTrue(PrioriteAction.ORDRE.interrompt(PrioriteAction.AFFECTIF));
		assertTrue(PrioriteAction.URGENCE.interrompt(PrioriteAction.ORDRE));
	}
}
