package fr.lhdp.compagnon.entite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EtatInterieurTest {

	@Test
	void uneMenaceFaitMonterLeStressEtTomberLaCuriosite() {
		EtatInterieur etat = new EtatInterieur();
		float explorationAvant = etat.envieDExplorer();
		for (int i = 0; i < 5; i++) {
			etat.actualiser(observation(false, false, true, true,
					false, true, 1.0F, 0.5F, 0.4F, 1.0F, 0.5F, 0.8F, 0.5F));
		}

		assertTrue(etat.stress() > 0.70F);
		assertTrue(etat.envieDExplorer() < explorationAvant);
	}

	@Test
	void linactiviteNourritProgressivementLennui() {
		EtatInterieur etat = new EtatInterieur();
		float avant = etat.ennui();
		for (int i = 0; i < 8; i++) {
			etat.actualiser(observation(false, false, false, false,
					false, true, 1.0F, 0.5F, 0.8F, 0.8F, 0.5F, 1.0F, 0.3F));
		}

		assertTrue(etat.ennui() > avant + 0.25F);
	}

	@Test
	void unCompagnonAttacheChercheLeContactQuandLeMaitreEstAbsent() {
		EtatInterieur etat = new EtatInterieur();
		float avant = etat.besoinDeContact();
		for (int i = 0; i < 8; i++) {
			etat.actualiser(observation(false, false, false, false,
					false, true, 1.0F, 0.4F, 0.7F, 0.4F, 1.0F, 0.5F, 1.0F));
		}

		assertTrue(etat.besoinDeContact() > avant + 0.30F);
	}

	@Test
	void toutesLesTensionsRestentBornees() {
		EtatInterieur etat = new EtatInterieur();
		for (int i = 0; i < 200; i++) {
			etat.actualiser(observation(false, true, true, true,
					false, true, 9.0F, -2.0F, -1.0F, 5.0F, 4.0F, 3.0F, 7.0F));
		}

		assertEntreZeroEtUn(etat.stress());
		assertEntreZeroEtUn(etat.ennui());
		assertEntreZeroEtUn(etat.besoinDeContact());
		assertEntreZeroEtUn(etat.envieDExplorer());
	}

	private static EtatInterieur.Observation observation(boolean maitreProche,
			boolean maitreEnDanger, boolean menace, boolean evenement,
			boolean amiProche, boolean inactif, float energie, float lien,
			float courage, float curiosite, float attachement, float vivacite,
			float calin) {
		return new EtatInterieur.Observation(maitreProche, maitreEnDanger, menace,
				evenement, amiProche, false, false, false, inactif, false,
				energie, lien, courage, curiosite, attachement, vivacite, calin);
	}

	private static void assertEntreZeroEtUn(float valeur) {
		assertTrue(valeur >= 0.0F && valeur <= 1.0F);
	}
}
