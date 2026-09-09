package fr.lhdp.compagnon.entite;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoireCourteTest {

	@Test
	void unSouvenirExpireSansNettoyageDeListe() {
		MemoireCourte memoire = new MemoireCourte();
		memoire.retenir(MemoireCourte.Signal.PLUIE, new BlockPos(1, 2, 3), 2);
		assertTrue(memoire.contient(MemoireCourte.Signal.PLUIE));

		memoire.avancer();
		assertTrue(memoire.contient(MemoireCourte.Signal.PLUIE));
		memoire.avancer();
		assertFalse(memoire.contient(MemoireCourte.Signal.PLUIE));
		assertNull(memoire.positionDe(MemoireCourte.Signal.PLUIE));
	}

	@Test
	void retenirLeMemeTypeRemplaceSansFaireGrandirLaMemoire() {
		MemoireCourte memoire = new MemoireCourte();
		UUID premier = UUID.randomUUID();
		UUID second = UUID.randomUUID();
		memoire.retenir(MemoireCourte.Signal.AMI_PROCHE, BlockPos.ZERO, premier, 20);
		BlockPos nouvellePosition = new BlockPos(1, 1, 1);
		memoire.retenir(MemoireCourte.Signal.AMI_PROCHE, nouvellePosition, second, 20);

		assertEquals(1, memoire.combienDActifs());
		assertEquals(second, memoire.sourceDe(MemoireCourte.Signal.AMI_PROCHE));
		assertEquals(nouvellePosition, memoire.positionDe(MemoireCourte.Signal.AMI_PROCHE));
	}
}
