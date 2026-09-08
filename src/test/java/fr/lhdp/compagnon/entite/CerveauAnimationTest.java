package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.espece.Espece;
import fr.lhdp.compagnon.fiche.Humeur;
import fr.lhdp.compagnon.fiche.Mode;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CerveauAnimationTest {

	private static final Set<String> TOUS = Set.of(Espece.IMMOBILE, Espece.MARCHE,
			Espece.COURSE, Espece.VOL, Espece.PLANE, Espece.NAGE, Espece.FLOTTE,
			Espece.ASSIS, Espece.COUCHE, Espece.TRISTE, Espece.JOYEUX);

	@Test
	void environnementPasseAvantLesPoses() {
		CerveauAnimation cerveau = new CerveauAnimation();
		assertEquals(Espece.NAGE, cerveau.choisir(
				vue(true, true, true, Mode.COUCHE, true, 1.0F, -0.2D), TOUS::contains).role());
		assertEquals(Espece.VOL, cerveau.choisir(
				vue(false, true, true, Mode.ASSIS, true, 1.0F, 0.1D), TOUS::contains).role());
	}

	@Test
	void sommeilPuisOrdrePuisDeplacement() {
		CerveauAnimation cerveau = new CerveauAnimation();
		assertEquals(CerveauAnimation.Noeud.SOMMEIL, cerveau.choisir(
				vue(false, false, true, Mode.ASSIS, false, 0, 0), TOUS::contains).noeud());
		assertEquals(Espece.ASSIS, cerveau.choisir(
				vue(false, false, false, Mode.ASSIS, true, 1, 0), TOUS::contains).role());
		assertEquals(Espece.MARCHE, cerveau.choisir(
				vue(false, false, false, Mode.RESTE, true, 0.4F, 0), TOUS::contains).role());
	}

	@Test
	void seuilsEvitentDeRelancerMarcheEtCourse() {
		CerveauAnimation cerveau = new CerveauAnimation();
		assertEquals(Espece.COURSE, cerveau.choisir(
				vue(false, false, false, Mode.RESTE, true, 0.8F, 0), TOUS::contains).role());
		assertEquals(Espece.COURSE, cerveau.choisir(
				vue(false, false, false, Mode.RESTE, true, 0.55F, 0), TOUS::contains).role());
		assertEquals(Espece.MARCHE, cerveau.choisir(
				vue(false, false, false, Mode.RESTE, true, 0.4F, 0), TOUS::contains).role());
	}

	@Test
	void rolesAbsentsOntToujoursUnRepli() {
		CerveauAnimation cerveau = new CerveauAnimation();
		Set<String> minimum = Set.of(Espece.IMMOBILE, Espece.MARCHE, Espece.VOL);
		assertEquals(Espece.MARCHE, cerveau.choisir(
				vue(false, false, false, Mode.RESTE, true, 1.0F, 0), minimum::contains).role());
		assertEquals(Espece.IMMOBILE, cerveau.choisir(new CerveauAnimation.Observation(
				false, false, false, Mode.RESTE, false, 0, 0, Humeur.TRISTE),
				minimum::contains).role());
	}

	private static CerveauAnimation.Observation vue(boolean eau, boolean vol, boolean dort,
			Mode mode, boolean avance, float allure, double verticale) {
		return new CerveauAnimation.Observation(eau, vol, dort, mode, avance, allure,
				verticale, Humeur.MOYEN);
	}
}
