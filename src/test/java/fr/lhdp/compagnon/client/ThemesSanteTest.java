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
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verrouille le gabarit commun des dix carnets enchantés. */
class ThemesSanteTest {

	private static final Path RACINE = Path.of(
			"src/main/resources/assets/compagnon/textures/gui/sante/themes");
	private static final List<String> THEMES = List.of(
			"dragon-rubis", "dragon-givre", "dragon-jade", "dragon-amethyste",
			"dragon-rose", "phenix", "renard-lunaire", "glycine", "galaxie",
			"champignons");

	@Test
	@DisplayName("les dix carnets enchantés respectent le même gabarit")
	void gabarit() throws IOException {
		for (String theme : THEMES) {
			Path dossier = RACINE.resolve(theme);
			verifier(dossier.resolve("book_closed.png"), 40, 50);
			verifier(dossier.resolve("health_card.png"), 300, 206);
			BufferedImage fiche = verifier(
					dossier.resolve("health_card_overflow.png"), 540, 406);
			verifier(dossier.resolve("hud_compact.png"), 136, 54);
			BufferedImage hud = verifier(
					dossier.resolve("hud_compact_overflow.png"), 296, 194);
			BufferedImage icones = verifier(dossier.resolve("health_icons.png"), 160, 16);
			verifier(dossier.resolve("health_icons_large.png"), 320, 32);
			for (String etat : List.of("normal", "hover", "pressed")) {
				verifier(dossier.resolve("theme_button_" + etat + ".png"), 32, 32);
				verifier(dossier.resolve("visibility_button_" + etat + ".png"), 24, 24);
			}

			assertEquals(0, alpha(fiche, 0, 0), theme + " : coin de fiche opaque");
			assertEquals(0, alpha(hud, 0, 0), theme + " : coin du HUD opaque");
			for (int cellule = 0; cellule < 4; cellule++) {
				assertTrue(contientUnPixel(icones, cellule * 16, 16),
						theme + " : icône de besoin vide à l'index " + cellule);
			}
		}
	}

	@Test
	@DisplayName("le HUD permanent reste minimal quel que soit le carnet choisi")
	void hudPermanentMinimal() throws IOException {
		String code = Files.readString(Path.of(
				"src/client/java/fr/lhdp/compagnon/client/Panneau.java"),
				StandardCharsets.UTF_8);
		assertFalse(code.contains("theme.texture(\"hud_compact"),
				"un grand décor illustré est encore chargé dans le HUD permanent");
		assertTrue(code.contains("LARGEUR = 112"), "largeur RP modifiée");
		assertTrue(code.contains("HAUTEUR = 38"), "hauteur RP modifiée");
	}

	private static BufferedImage verifier(Path fichier, int largeur, int hauteur)
			throws IOException {
		BufferedImage image = ImageIO.read(fichier.toFile());
		assertNotNull(image, fichier.toString());
		assertEquals(largeur, image.getWidth(), fichier.toString());
		assertEquals(hauteur, image.getHeight(), fichier.toString());
		return image;
	}

	private static boolean contientUnPixel(BufferedImage image, int x, int taille) {
		for (int py = 0; py < taille; py++) {
			for (int px = x; px < x + taille; px++) {
				if (alpha(image, px, py) != 0) {
					return true;
				}
			}
		}
		return false;
	}

	private static int alpha(BufferedImage image, int x, int y) {
		return (image.getRGB(x, y) >>> 24) & 0xFF;
	}
}
