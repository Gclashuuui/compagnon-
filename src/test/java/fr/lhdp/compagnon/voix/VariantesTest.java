package fr.lhdp.compagnon.voix;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les variantes d'un nom, et surtout celles qu'il ne faut pas.
 *
 * <h2>Ce que ces tests empechent de revenir</h2>
 *
 * <p>La premiere version cherchait les mots a deux corrections pres. Essayee sur
 * le vrai dictionnaire, elle ramenait pour « Braise » : <b>baiser</b>. Et pour
 * « Mouette » : maquette, mazette, malette.
 *
 * <p>Ces mots ne se ressemblent pas a l'oreille. Les mettre dans la liste du
 * moteur aurait fait deux degats a la fois : le compagnon aurait obei a des
 * phrases sans aucun rapport, et son vrai nom aurait ete moins bien reconnu,
 * puisqu'il aurait eu six concurrents de plus.
 *
 * <p>La regle retenue est plus etroite et plus sure : meme racine, ou deux
 * lettres voisines inversees. Ces tests la tiennent.
 */
class VariantesTest {

	@Test
	@DisplayName("le pluriel et les formes voisines comptent")
	void memeRacineAcceptee() {
		assertTrue(Lexique.memeRacine("dragon", "dragons"));
		assertTrue(Lexique.memeRacine("dragon", "dragonne"));
		assertTrue(Lexique.memeRacine("mouette", "mouettes"));
		assertTrue(Lexique.memeRacine("sabre", "sabrer"));
		assertTrue(Lexique.memeRacine("braise", "braises"));
	}

	@Test
	@DisplayName("un mot qui ne sonne pas pareil est refuse")
	void leResteEstRefuse() {
		// C'est la raison d'etre de tout ce fichier.
		assertFalse(Lexique.memeRacine("braise", "baiser"));
		assertFalse(Lexique.memeRacine("mouette", "maquette"));
		assertFalse(Lexique.memeRacine("mouette", "mazette"));
		assertFalse(Lexique.memeRacine("dragon", "daron"));
	}

	@Test
	@DisplayName("une racine trop courte ne suffit pas")
	void tropCourtPourEtreSur() {
		// « noix » et « noire » partagent trois lettres : ce n'est pas assez pour
		// affirmer que c'est le meme mot.
		assertFalse(Lexique.memeRacine("noix", "noire"));
		assertFalse(Lexique.memeRacine("bou", "boulanger"));
	}

	@Test
	@DisplayName("une trop grande difference de longueur ne suffit pas non plus")
	void tropLoinEnLongueur() {
		assertFalse(Lexique.memeRacine("dragon", "dragonnades"));
		assertTrue(Lexique.memeRacine("dragon", "dragonne"));
	}

	@Test
	@DisplayName("deux lettres voisines inversees : la faute de frappe au bapteme")
	void inversionRattrapee() {
		assertTrue(Lexique.lettresInversees("dargon", "dragon"));
		assertTrue(Lexique.lettresInversees("dragon", "dargon"));
		assertTrue(Lexique.lettresInversees("sabre", "saber"));
	}

	@Test
	@DisplayName("une inversion, pas deux, et pas a distance")
	void inversionSeulement() {
		// Deux lettres changees mais pas voisines : ce n'est pas une inversion.
		assertFalse(Lexique.lettresInversees("dragon", "drogan"));
		// Longueurs differentes : jamais.
		assertFalse(Lexique.lettresInversees("dragon", "dragons"));
		// Trop court pour qu'une inversion veuille dire quelque chose.
		assertFalse(Lexique.lettresInversees("chat", "chta"));
	}
}
