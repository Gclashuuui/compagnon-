import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Le plus grand rectangle plein a l'interieur d'un nuage.
 *
 * <p>C'est la que le texte ira. On le mesure au lieu de l'estimer : les bosses du
 * pourtour n'ont pas la meme epaisseur partout, et une marge devinee ferait
 * deborder une phrase sur le contour.
 */
public final class Interieur {

	public static void main(String[] args) throws Exception {
		for (String chemin : args) {
			BufferedImage image = ImageIO.read(new File(chemin));
			int l = image.getWidth();
			int h = image.getHeight();

			// Plein = opaque ET clair : le contour sombre ne compte pas comme
			// interieur, sinon le texte se poserait dessus.
			boolean[][] plein = new boolean[h][l];
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < l; x++) {
					int argb = image.getRGB(x, y);
					int clarte = (((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF)) / 3;
					plein[y][x] = (argb >>> 24) > 127 && clarte > 150;
				}
			}

			int[] meilleur = { 0, 0, 0, 0, 0 }; // aire, x, y, largeur, hauteur
			int[] hauteurs = new int[l];
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < l; x++) {
					hauteurs[x] = plein[y][x] ? hauteurs[x] + 1 : 0;
				}
				plusGrandRectangle(hauteurs, y, meilleur);
			}
			System.out.printf("%-14s %2dx%-2d  interieur %2dx%-2d en (%d,%d)"
					+ "   marges  g%d d%d h%d b%d%n",
					new File(chemin).getName(), l, h,
					meilleur[3], meilleur[4], meilleur[1], meilleur[2],
					meilleur[1], l - meilleur[1] - meilleur[3],
					meilleur[2], h - meilleur[2] - meilleur[4]);
		}
	}

	/** Le plus grand rectangle sous un histogramme, par pile croissante. */
	private static void plusGrandRectangle(int[] hauteurs, int ligne, int[] meilleur) {
		int l = hauteurs.length;
		java.util.Deque<Integer> pile = new java.util.ArrayDeque<>();
		for (int x = 0; x <= l; x++) {
			int courante = x == l ? 0 : hauteurs[x];
			while (!pile.isEmpty() && hauteurs[pile.peek()] >= courante) {
				int haut = hauteurs[pile.pop()];
				int gauche = pile.isEmpty() ? 0 : pile.peek() + 1;
				int large = x - gauche;
				if (haut * large > meilleur[0]) {
					meilleur[0] = haut * large;
					meilleur[1] = gauche;
					meilleur[2] = ligne - haut + 1;
					meilleur[3] = large;
					meilleur[4] = haut;
				}
			}
			pile.push(x);
		}
	}
}
