package fr.lhdp.compagnon.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Verrouille les dimensions et les zones transparentes des cinq livres v5. */
class ThemesMerveillesTest {

	private static final Path THEMES = Path.of(
			"src/main/resources/assets/compagnon/textures/gui/livre/themes");
	private static final List<String> IDS = List.of(
			"medieval_hd", "bestiaire_hd", "vitrail_hd", "horlogerie_hd", "porcelaine_hd");

	@Test
	@DisplayName("les cinq livres merveilleux gardent le gabarit continu du journal")
	void gabarit() throws IOException {
		for (String id : IDS) {
			Path dossier = THEMES.resolve(id);
			verifier(dossier.resolve("book_astra_v2.png"), 384, 256);
			BufferedImage cadre = verifier(dossier.resolve("cover_frame_hd.png"), 1920, 1152);
			verifier(dossier.resolve("page_left_hd.png"), 568, 736);
			verifier(dossier.resolve("page_right_hd.png"), 568, 736);

			// Les pages sont dessinees par-dessous. Si ces ouvertures redeviennent
			// opaques, elles sont masquees par un grand rectangle noir ou blanc.
			assertEquals(0, alpha(cadre, 600, 500), id + " : ouverture gauche opaque");
			assertEquals(0, alpha(cadre, 1300, 500), id + " : ouverture droite opaque");
		}
	}

	@Test
	@DisplayName("aucun ecran du livre ne redessine les anciennes fleches")
	void coinsInvisibles() throws IOException {
		for (String fichier : List.of("EcranLivre.java", "EcranCarnet.java")) {
			String code = Files.readString(Path.of(
					"src/client/java/fr/lhdp/compagnon/client", fichier),
					StandardCharsets.UTF_8);
			assertFalse(code.contains("page_turn_left_normal"), fichier);
			assertFalse(code.contains("page_turn_right_normal"), fichier);
		}
	}

	private static BufferedImage verifier(Path fichier, int largeur, int hauteur)
			throws IOException {
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
