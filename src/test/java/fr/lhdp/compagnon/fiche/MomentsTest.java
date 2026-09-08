package fr.lhdp.compagnon.fiche;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La liste des moments n'avait pas de plafond.
 *
 * <p>Ca ne se voyait pas tant que les niveaux s'arretaient a la derniere ligne
 * de la table. Depuis qu'ils ne s'arretent plus, un compagnon de longue date
 * accumulerait une ligne par niveau, sans fin, dans la sauvegarde de tout le
 * monde — et personne ne s'en apercevrait avant des mois.
 *
 * <h2>Ce que ce test protege surtout</h2>
 *
 * <p>Pas le plafond : la regle de tri. Un defaut ou une manie oublies feraient
 * changer le caractere de la bete du jour au lendemain, ce qui est bien pire que
 * quelques kilo-octets de trop. Ce sont les deux seules choses du mod qui sont
 * censees etre definitives.
 */
class MomentsTest {

	private static List<Moment> liste(String... cles) {
		List<Moment> moments = new ArrayList<>();
		for (int i = 0; i < cles.length; i++) {
			moments.add(new Moment(cles[i], i));
		}
		return moments;
	}

	@Test
	@DisplayName("un niveau part avant tout le reste")
	void lesNiveauxDAbord() {
		List<Moment> moments = liste("premiere_fois.mer", "niveau_4", "manie.salue_le_jour");
		assertTrue(Moment.faireDeLaPlace(moments));
		assertEquals(2, moments.size());
		assertFalse(moments.stream().anyMatch(m -> m.cle().equals("niveau_4")));
	}

	@Test
	@DisplayName("le plus vieux niveau part en premier")
	void lePlusVieuxDAbord() {
		List<Moment> moments = liste("niveau_2", "niveau_3", "niveau_4");
		Moment.faireDeLaPlace(moments);
		assertEquals("niveau_3", moments.get(0).cle());
	}

	@Test
	@DisplayName("on ne jette jamais un defaut ni une manie ni une premiere fois")
	void ceQuiFaitQuIlEstLui() {
		List<Moment> moments = liste("defaut.gourmand", "manie.salue_le_jour",
				"premiere_fois.mer", "premier_repas", "premier_ami");
		assertFalse(Moment.faireDeLaPlace(moments),
				"aucun de ces moments n'est remplacable");
		assertEquals(5, moments.size(), "la liste doit etre intacte");
	}

	@Test
	@DisplayName("les reves et les objets goutes sont remplacables")
	void ceQuOnPeutOublier() {
		for (String cle : List.of("reve.vol", "objet_prefere.saumon")) {
			List<Moment> moments = liste("defaut.tetu", cle);
			assertTrue(Moment.faireDeLaPlace(moments), cle + " doit pouvoir partir");
			assertEquals(1, moments.size());
			assertEquals("defaut.tetu", moments.get(0).cle());
		}
	}

	@Test
	@DisplayName("une liste vide ne casse rien")
	void listeVide() {
		List<Moment> moments = new ArrayList<>();
		assertFalse(Moment.faireDeLaPlace(moments));
		assertTrue(moments.isEmpty());
	}

	@Test
	@DisplayName("cent niveaux de suite tiennent dans le plafond")
	void centNiveaux() {
		List<Moment> moments = liste("defaut.jaloux", "manie.regarde_le_ciel",
				"premiere_fois.neige");
		for (int niveau = 1; niveau <= 500; niveau++) {
			if (moments.size() >= Moment.PLAFOND) {
				Moment.faireDeLaPlace(moments);
			}
			moments.add(new Moment("niveau_" + niveau, niveau));
		}
		assertTrue(moments.size() <= Moment.PLAFOND,
				"la liste doit rester sous le plafond, elle fait " + moments.size());
		// Et le caractere de la bete a survecu aux cinq cents niveaux.
		assertTrue(moments.stream().anyMatch(m -> m.cle().equals("defaut.jaloux")));
		assertTrue(moments.stream().anyMatch(m -> m.cle().equals("manie.regarde_le_ciel")));
		assertTrue(moments.stream().anyMatch(m -> m.cle().equals("premiere_fois.neige")));
	}
}
