package fr.lhdp.compagnon.fiche;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class GoutsTest {

	private static final UUID COMPAGNON = UUID.fromString("ba7738f0-aea6-4d65-b668-e13ca89b14b0");

	@Test
	@DisplayName("un compagnon garde les memes gouts quel que soit l'ordre des fichiers")
	void choixStable() {
		Gouts premier = Gouts.choisir(COMPAGNON,
				List.of("pomme_givree", "larve", "croquettes", "miel_fee"));
		Gouts second = Gouts.choisir(COMPAGNON,
				List.of("miel_fee", "croquettes", "larve", "pomme_givree", "larve"));

		assertEquals(premier, second);
	}

	@Test
	@DisplayName("deux favoris et un aliment boude ne se confondent jamais")
	void troisPreferencesDistinctes() {
		Gouts gouts = Gouts.choisir(COMPAGNON, List.of("a", "b", "c", "d"));

		assertEquals(2, gouts.preferes().size());
		assertFalse(gouts.boude().isEmpty());
		assertFalse(gouts.preferes().contains(gouts.boude()));
		assertEquals(Gouts.Avis.PREFERE, gouts.avis(gouts.preferes().get(0)));
		assertEquals(Gouts.Avis.BOUDE, gouts.avis(gouts.boude()));
		assertEquals(Gouts.Avis.ORDINAIRE, gouts.avis("inconnu"));
	}

	@Test
	@DisplayName("une liste minuscule reste utilisable")
	void petitesListes() {
		assertEquals(Gouts.VIDES, Gouts.choisir(COMPAGNON, List.of()));
		Gouts unique = Gouts.choisir(COMPAGNON, List.of("pomme"));
		assertEquals(List.of("pomme"), unique.preferes());
		assertEquals("", unique.boude());
		assertNotEquals(Gouts.VIDES, unique);
	}
}
