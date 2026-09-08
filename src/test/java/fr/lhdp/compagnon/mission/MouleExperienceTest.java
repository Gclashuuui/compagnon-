package fr.lhdp.compagnon.mission;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Le rythme commun des recompenses de mission. */
class MouleExperienceTest {

	private static Moule mission(Integer... xp) {
		return new Moule("essai", "compteur", "texte", List.of(1, 2, 3),
				List.of(xp), "essai", List.of(), false);
	}

	@Test
	@DisplayName("une mission ne paie plus sa valeur brute")
	void recompenseRalentit() {
		assertEquals(29, mission(12, 30, 65).experience(2));
	}

	@Test
	@DisplayName("une petite mission positive rapporte toujours au moins un point")
	void minimumPositif() {
		assertEquals(1, mission(1).experience(0));
		assertEquals(0, mission(0).experience(0));
	}

	@Test
	@DisplayName("le cran reste borne aux valeurs disponibles")
	void cranBorne() {
		Moule moule = mission(10, 20);
		assertEquals(5, moule.experience(-4));
		assertEquals(9, moule.experience(9));
	}
}
