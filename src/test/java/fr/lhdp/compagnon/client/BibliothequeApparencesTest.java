package fr.lhdp.compagnon.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Verrouille le gabarit et les ouvertures de la bibliothèque illustrée. */
class BibliothequeApparencesTest {

	private static final Path RACINE = Path.of(
			"src/main/resources/assets/compagnon/textures/gui/livre/bibliotheque");

	@Test
	@DisplayName("la bibliothèque et tous ses atlas gardent leur gabarit")
	void gabarit() throws IOException {
		BufferedImage panneau = verifier("library_panel_hd.png", 1664, 1136);
		BufferedImage cartes = verifier("library_card_states_hd.png", 1504, 272);
		verifier("favorite_button_states_hd.png", 256, 64);
		verifier("filter_tabs_states_hd.png", 1664, 88);
		verifier("pagination_states_hd.png", 432, 72);
		verifier("empty_favorites_hd.png", 256, 256);
		BufferedImage details = verifier("library_details_overlay.png", 1664, 1136);

		assertEquals(0, alpha(panneau, 0, 0), "coin du panneau opaque");
		assertEquals(0, alpha(details, 832, 568), "le décor masque les cartes");
		for (int etat = 0; etat < 4; etat++) {
			assertEquals(0, alpha(cartes, etat * 376 + 188, 118),
					"fenêtre de miniature opaque pour l'état " + etat);
		}
	}

	private static BufferedImage verifier(String nom, int largeur, int hauteur)
			throws IOException {
		Path fichier = RACINE.resolve(nom);
		BufferedImage image = ImageIO.read(fichier.toFile());
		assertNotNull(image, fichier.toString());
		assertEquals(largeur, image.getWidth(), fichier.toString());
		assertEquals(hauteur, image.getHeight(), fichier.toString());
		return image;
	}

	private static int alpha(BufferedImage image, int x, int y) {
		return (image.getRGB(x, y) >>> 24) & 0xFF;
	}
}
