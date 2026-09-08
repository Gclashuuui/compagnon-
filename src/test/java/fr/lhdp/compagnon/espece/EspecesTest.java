package fr.lhdp.compagnon.espece;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Chacun sa roue.
 *
 * <h2>Le defaut que ces tests gardent</h2>
 *
 * <p>La table des niveaux ne connait que des noms d'animation, sans espece. Tant
 * qu'il n'y avait qu'un dragonnet, personne ne pouvait s'en apercevoir. Le jour
 * ou la mouette est arrivee, la roue du dragonnet s'est mise a montrer des cases
 * de mouette : elles s'affichaient, elles se debloquaient, et cliquer dessus ne
 * faisait <b>rien du tout</b>.
 *
 * <p>C'est la pire sorte de panne : celle qui ressemble a un jeu qui marche.
 * Elle reviendra au premier ajout d'espece si personne ne la surveille.
 */
class EspecesTest {

	private static final Set<String> CONNUES = Set.of("dragonnet", "mouette");

	@Test
	@DisplayName("un geste de mouette n'apparait pas chez le dragonnet")
	void chacunSaRoue() {
		assertFalse(Especes.concerne("animation.mouette.amb_call", "dragonnet", CONNUES));
		assertFalse(Especes.concerne("animation.dragonnet.yawn", "mouette", CONNUES));
	}

	@Test
	@DisplayName("chacun garde les siens")
	void lesSiensRestent() {
		assertTrue(Especes.concerne("animation.mouette.amb_call", "mouette", CONNUES));
		assertTrue(Especes.concerne("animation.dragonnet.yawn", "dragonnet", CONNUES));
	}

	@Test
	@DisplayName("un nom hors convention reste visible par tout le monde")
	void leDouteProfiteAuNom() {
		// Mieux vaut une case de trop qu'une roue vide : quelqu'un qui nomme ses
		// animations autrement ne doit pas les voir disparaitre sans un mot.
		assertTrue(Especes.concerne("animation.hibou.hulule", "dragonnet", CONNUES));
		assertTrue(Especes.concerne("coucou", "dragonnet", CONNUES));
		assertTrue(Especes.concerne("animation.saut", "dragonnet", CONNUES));
		assertTrue(Especes.concerne(null, "dragonnet", CONNUES));
	}
}
