package fr.lhdp.compagnon.entite;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CerveauComportementTest {

	@Test
	void chaqueNoeudDeMouvementAUnePrioriteUnique() {
		long uniques = Arrays.stream(CerveauComportement.Noeud.values())
				.map(CerveauComportement.Noeud::priorite).distinct().count();
		assertEquals(CerveauComportement.Noeud.values().length, uniques);
	}

	@Test
	void ordrePuisBesoinPuisVieSpontanee() {
		assertTrue(prioritaire(CerveauComportement.Noeud.RAPPEL,
				CerveauComportement.Noeud.SUIVRE));
		assertTrue(prioritaire(CerveauComportement.Noeud.JEU_DEMANDE,
				CerveauComportement.Noeud.SUIVRE));
		assertTrue(prioritaire(CerveauComportement.Noeud.REPAS,
				CerveauComportement.Noeud.CURIOSITE));
		assertTrue(prioritaire(CerveauComportement.Noeud.SOMMEIL,
				CerveauComportement.Noeud.JOUEUR));
		assertTrue(prioritaire(CerveauComportement.Noeud.HABITUDE,
				CerveauComportement.Noeud.AFFECTION));
		assertTrue(prioritaire(CerveauComportement.Noeud.AFFECTION,
				CerveauComportement.Noeud.CURIOSITE));
	}

	@Test
	void unePrioriteSynchroniseeRetrouveToujoursSonNoeud() {
		for (CerveauComportement.Noeud noeud : CerveauComportement.Noeud.values()) {
			assertEquals(noeud,
					CerveauComportement.Noeud.depuisPriorite(noeud.priorite()));
		}
		assertEquals(CerveauComportement.Noeud.REPOS,
				CerveauComportement.Noeud.depuisPriorite(Integer.MAX_VALUE));
	}

	private static boolean prioritaire(CerveauComportement.Noeud premier,
			CerveauComportement.Noeud second) {
		return premier.priorite() < second.priorite();
	}
}
