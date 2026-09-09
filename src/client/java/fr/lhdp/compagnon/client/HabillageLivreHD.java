package fr.lhdp.compagnon.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Habillage detaille autour du livre logique de 384 x 256 pixels.
 *
 * <p>Les deux zones d'ecriture de 142 x 184 pixels ne bougent jamais : seules
 * les marges illustrees s'adaptent a la place disponible a l'ecran. Le texte
 * garde donc exactement le meme calibrage, meme sur une petite fenetre.
 */
final class HabillageLivreHD {
	static final int SIGNET_LARGEUR = 28;
	static final int SIGNET_HAUTEUR = 36;
	static final int SIGNET_ECART = 4;

	private HabillageLivreHD() {
	}

	static int margeGauche(int gauche) {
		return Math.min(48, Math.max(0, gauche - 4));
	}

	static int margeDroite(int gauche, int largeurEcran) {
		// Le signet chevauche la couverture de quatre pixels.
		return Math.min(48, Math.max(0,
				largeurEcran - 4 - (gauche + 384) - (SIGNET_LARGEUR - 4)));
	}

	static int signetX(int gauche, int largeurEcran) {
		int ideal = gauche + 384 + margeDroite(gauche, largeurEcran) - 4;
		return Math.min(ideal, Math.max(0, largeurEcran - SIGNET_LARGEUR - 4));
	}

	static void dessiner(GuiGraphics g, ThemeLivre theme, int gauche, int haut,
			int largeurEcran, int hauteurEcran) {
		ResourceLocation cadre = theme.texture("cover_frame_hd");
		int hautExterne = Math.min(16, Math.max(0, haut - 4));
		int basExterne = Math.min(16, Math.max(0, hauteurEcran - haut - 256 - 4));
		int[] sourceX = {0, 328, 896, 1024, 1592, 1920};
		int[] sourceY = {0, 176, 912, 1152};
		int[] x = {gauche - margeGauche(gauche), gauche + 34, gauche + 176,
				gauche + 208, gauche + 350,
				gauche + 384 + margeDroite(gauche, largeurEcran)};
		int[] y = {haut - hautExterne, haut + 28, haut + 212, haut + 256 + basExterne};
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 5; col++) {
				// Les deux cases centrales sont remplacees par les pages haute definition.
				if (row == 1 && (col == 1 || col == 3)) {
					continue;
				}
				g.blit(cadre, x[col], y[row], x[col + 1] - x[col], y[row + 1] - y[row],
						(float)sourceX[col], (float)sourceY[row],
						sourceX[col + 1] - sourceX[col], sourceY[row + 1] - sourceY[row],
						1920, 1152);
			}
		}
		g.blit(theme.texture("page_left_hd"), gauche + 34, haut + 28,
				142, 184, 0.0F, 0.0F, 568, 736, 568, 736);
		g.blit(theme.texture("page_right_hd"), gauche + 208, haut + 28,
				142, 184, 0.0F, 0.0F, 568, 736, 568, 736);
	}
}
