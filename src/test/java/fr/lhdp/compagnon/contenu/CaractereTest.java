package fr.lhdp.compagnon.contenu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaractereTest {

	@Test
	void lesNouveauxPenchantsSontDerivesSansChangerLeFormat() {
		Caractere timide = new Caractere("timide", "Timide",
				0.15F, 0.95F, 0.4F, 0.4F, 0.3F);
		Caractere joueur = new Caractere("joueur", "Joueur",
				0.75F, 0.5F, 0.95F, 0.6F, 0.9F);

		assertTrue(joueur.courage() > timide.courage());
		assertTrue(timide.patience() > joueur.patience());
		assertEquals(0.6F, timide.patience(), 0.0001F);
	}
}
