package fr.lhdp.compagnon.entite;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HabituationTest {

	@Test
	void lesDeuxPremiersEvenementsRestentInteressantsPuisLaSerieEstFiltree() {
		Habituation habituation = new Habituation();
		BlockPos chantier = new BlockPos(10, 64, 10);

		assertTrue(habituation.accepte(chantier, 0));
		assertTrue(habituation.accepte(chantier.offset(1, 0, 0), 10));
		assertFalse(habituation.accepte(chantier.offset(0, 1, 0), 20));
		assertFalse(habituation.accepte(chantier, 30));
		assertTrue(habituation.accepte(chantier, 40));
	}

	@Test
	void unEvenementAilleursEstImmediatementNouveau() {
		Habituation habituation = new Habituation();
		assertTrue(habituation.accepte(BlockPos.ZERO, 0));
		assertTrue(habituation.accepte(new BlockPos(20, 0, 0), 1));
	}

	@Test
	void unePauseSuffitPourDeshabituer() {
		Habituation habituation = new Habituation();
		BlockPos point = new BlockPos(2, 3, 4);
		assertTrue(habituation.accepte(point, 0));
		assertTrue(habituation.accepte(point, 20L * 10L + 1L));
	}

	@Test
	void unePositionAbsenteNestJamaisAcceptee() {
		assertFalse(new Habituation().accepte(null, 0));
	}
}
