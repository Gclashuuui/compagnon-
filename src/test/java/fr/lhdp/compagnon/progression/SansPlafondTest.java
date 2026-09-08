package fr.lhdp.compagnon.progression;

import fr.lhdp.compagnon.fiche.Barre;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La table des niveaux s'arretait a sa derniere ligne.
 *
 * <p>Sur un serveur qui dure des annees, un joueur arrive au bout n'a plus
 * aucune raison de continuer a jouer avec sa bete. Les niveaux se prolongent
 * donc au-dela de la table, chacun un peu plus cher que le precedent.
 *
 * <h2>Ce que ce test protege</h2>
 *
 * <p>Deux formules se repondent : une qui donne le niveau depuis l'experience,
 * l'autre l'experience depuis le niveau. Si elles se desaccordent d'un seul
 * niveau, la barre du livre se remplit a l'envers ou reste bloquee — et
 * personne ne s'en apercoit avant qu'un joueur ne depasse le dernier palier,
 * c'est-a-dire des mois apres la faute.
 */
class SansPlafondTest {

	/** Une table courte : trois paliers, dernier ecart de 100. */
	private static Progression table() {
		return new Progression(
				Map.of(),
				Map.of(),
				0.0F,
				new Progression.Action(0.0F, 0.0F),
				List.of(
						new Progression.Palier(1, 0, List.of()),
						new Progression.Palier(2, 100, List.of()),
						new Progression.Palier(3, 200, List.of())));
	}

	@Test
	@DisplayName("dans la table, rien ne change")
	void laTableFaitToujoursAutorite() {
		Progression t = table();
		assertEquals(1, t.niveauPour(0));
		assertEquals(1, t.niveauPour(99));
		assertEquals(2, t.niveauPour(100));
		assertEquals(3, t.niveauPour(200));
		assertEquals(100, t.xpDu(2));
		assertEquals(200, t.xpDu(3));
	}

	@Test
	@DisplayName("au-dela du dernier palier, ca continue de monter")
	void plusDePlafond() {
		Progression t = table();
		assertEquals(3, t.niveauMaximum());
		// Le quatrieme niveau coute le dernier ecart majore : 100 x 1,08 = 108.
		assertEquals(3, t.niveauPour(300));
		assertEquals(4, t.niveauPour(308));
		assertTrue(t.niveauPour(100000) > 30, "cent mille points doivent aller loin");
	}

	@Test
	@DisplayName("les deux formules se repondent, sur cent niveaux")
	void allerEtRetour() {
		Progression t = table();
		for (int niveau = 4; niveau <= 100; niveau++) {
			int xp = t.xpDu(niveau);
			assertEquals(niveau, t.niveauPour(xp),
					"l'experience du niveau " + niveau + " doit rendre le niveau " + niveau);
			assertEquals(niveau - 1, t.niveauPour(xp - 1),
					"un point de moins doit rendre le niveau precedent");
		}
	}

	@Test
	@DisplayName("l'experience du suivant ne se bloque plus jamais")
	void laBarreContinueDeSeRemplir() {
		Progression t = table();
		for (int niveau = 1; niveau <= 60; niveau++) {
			int suivant = t.xpDuSuivant(niveau);
			assertTrue(suivant > t.xpDu(niveau) || niveau < 3,
					"le palier suivant du niveau " + niveau + " doit etre au-dessus");
		}
	}

	@Test
	@DisplayName("chaque niveau coute plus cher que le precedent")
	void laMonteeRalentit() {
		Progression t = table();
		int avant = t.xpDu(5) - t.xpDu(4);
		for (int niveau = 6; niveau <= 60; niveau++) {
			int ecart = t.xpDu(niveau) - t.xpDu(niveau - 1);
			assertTrue(ecart >= avant,
					"le niveau " + niveau + " doit couter au moins autant que le precedent");
			avant = ecart;
		}
	}

	@Test
	@DisplayName("une table d'une seule ligne plafonne, comme avant")
	void rienAProlonger() {
		Progression courte = new Progression(Map.of(), Map.of(), 0.0F,
				new Progression.Action(0.0F, 0.0F),
				List.of(new Progression.Palier(1, 0, List.of())));
		assertEquals(1, courte.niveauPour(999999));
		assertEquals(-1, courte.xpDuSuivant(1));
	}

	@Test
	@DisplayName("aucune experience ne fait deborder ni boucler")
	void rienNeDeborde() {
		Progression t = table();
		assertTrue(t.niveauPour(Integer.MAX_VALUE) > 0);
		assertTrue(t.xpDu(10000) > 0, "un niveau absurde doit rester un nombre positif");
		// Les barres du livre se lisent aussi tout en haut.
		assertTrue(t.xpDuSuivant(10000) > 0);
		assertEquals(Barre.values().length, Barre.values().length);
	}
}
