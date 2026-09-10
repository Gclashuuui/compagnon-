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
	private static final List<String> THEMES_HUD = List.of(
			"parchemin", "sylvestre", "nocturne", "dragon-rubis", "dragon-givre",
			"dragon-jade", "dragon-amethyste", "dragon-rose", "phenix",
			"renard-lunaire", "glycine", "galaxie", "champignons");

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
		assertTrue(code.contains("theme.texture(\"hud_frame_hd\")"),
				"le cadre discret du thème n'est pas chargé");
	}

	@Test
	@DisplayName("les treize cadres discrets sont nets et partagent le même gabarit")
	void cadresHud() throws IOException {
		for (String theme : THEMES_HUD) {
			BufferedImage cadre = verifier(
					RACINE.resolve(theme).resolve("hud_frame_hd.png"), 448, 152);
			assertEquals(0, alpha(cadre, 0, 0), theme + " : coin extérieur opaque");
			assertTrue(alpha(cadre, 224, 76) > 0, theme + " : intérieur transparent");
			verifierBlocs(cadre, theme);
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

	private static void verifierBlocs(BufferedImage image, String theme) {
		for (int y = 0; y < image.getHeight(); y += 4) {
			for (int x = 0; x < image.getWidth(); x += 4) {
				int reference = image.getRGB(x, y);
				for (int py = y; py < y + 4; py++) {
					for (int px = x; px < x + 4; px++) {
						assertEquals(reference, image.getRGB(px, py),
								theme + " : bloc HD lissé en " + x + "," + y);
					}
				}
			}
		}
	}

	private static int alpha(BufferedImage image, int x, int y) {
		return (image.getRGB(x, y) >>> 24) & 0xFF;
	}
}
