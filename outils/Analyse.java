import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Regarde une planche de pixel art et dit ce qu'elle contient vraiment.
 *
 * <p>On ne devine ni la taille du bloc ni la position de la grille : Gemini ne
 * respecte jamais les dimensions demandees. On les mesure.
 *
 * <p><b>Le piege du premier essai.</b> Compter les changements de couleur qui
 * tombent sur une frontiere de bloc favorise mecaniquement les petites periodes :
 * plus les blocs sont petits, plus il y a de frontieres, donc plus on a de
 * chances d'en toucher une par hasard. Le detecteur repondait « 6 pixels » sur
 * des planches manifestement dessinees en blocs de 10. On compare donc ce qu'on
 * observe a ce que donnerait un placement au hasard, et on garde l'ecart.
 */
public final class Analyse {

	/** Un changement compte comme « sur la frontiere » a moins de ca. */
	private static final double TOLERANCE = 0.75;

	public static void main(String[] args) throws Exception {
		for (String chemin : args) {
			BufferedImage image = ImageIO.read(new File(chemin));
			System.out.println("=== " + new File(chemin).getName()
					+ "  " + image.getWidth() + "x" + image.getHeight());

			couleursDuFond(image);
			System.out.println("  -- horizontal --");
			mesurer(changementsEnX(image), image.getWidth(), 5);
			System.out.println("  -- vertical --");
			mesurer(changementsEnY(image), image.getHeight(), 2);
			System.out.println();
		}
	}

	private static void couleursDuFond(BufferedImage image) {
		Map<Integer, Integer> comptes = new LinkedHashMap<>();
		int l = image.getWidth();
		int h = image.getHeight();
		for (int x = 0; x < l; x++) {
			for (int y : new int[] { 0, 1, h - 2, h - 1 }) {
				comptes.merge(image.getRGB(x, y) & 0xFFFFFF, 1, Integer::sum);
			}
		}
		comptes.entrySet().stream()
				.sorted((a, b) -> b.getValue() - a.getValue())
				.limit(3)
				.forEach(e -> System.out.printf("  fond %06X  x%d%n", e.getKey(), e.getValue()));

		int magenta = 0;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < l; x++) {
				if (estMagenta(image.getRGB(x, y))) {
					magenta++;
				}
			}
		}
		System.out.printf("  magenta : %.1f%% de l'image%n", 100.0 * magenta / (l * h));
	}

	/** Tolerant, parce que le JPEG a bave sur les bords. */
	static boolean estMagenta(int argb) {
		int r = (argb >> 16) & 0xFF;
		int v = (argb >> 8) & 0xFF;
		int b = argb & 0xFF;
		return r > 140 && b > 140 && v < 110 && (r - v) > 60 && (b - v) > 60;
	}

	private static double[] changementsEnX(BufferedImage image) {
		int l = image.getWidth();
		int h = image.getHeight();
		double[] poids = new double[l];
		for (int y = 0; y < h; y++) {
			for (int x = 1; x < l; x++) {
				int d = distance(image.getRGB(x - 1, y), image.getRGB(x, y));
				if (d > 60) {
					poids[x] += 1;
				}
			}
		}
		return poids;
	}

	private static double[] changementsEnY(BufferedImage image) {
		int l = image.getWidth();
		int h = image.getHeight();
		double[] poids = new double[h];
		for (int y = 1; y < h; y++) {
			for (int x = 0; x < l; x++) {
				int d = distance(image.getRGB(x, y - 1), image.getRGB(x, y));
				if (d > 60) {
					poids[y] += 1;
				}
			}
		}
		return poids;
	}

	/**
	 * Cherche la periode qui explique le mieux les changements, en corrigeant le
	 * biais decrit en tete de classe.
	 *
	 * @param cases combien de cases de grille sur cet axe, pour dire la taille du sprite
	 */
	private static void mesurer(double[] poids, int longueur, int cases) {
		double total = 0;
		for (double p : poids) {
			total += p;
		}
		if (total == 0) {
			System.out.println("  aucun changement : image vide ?");
			return;
		}

		List<double[]> resultats = new ArrayList<>();

		for (double periode = 4.0; periode <= 20.0; periode += 0.01) {
			// Ce qu'un placement au hasard donnerait : la part de l'axe couverte
			// par les fenetres de tolerance autour de chaque frontiere.
			double attendu = Math.min(1.0, 2 * TOLERANCE / periode);

			double meilleurPourCettePeriode = -1;
			double meilleurDecalage = 0;
			for (double decalage = 0; decalage < periode; decalage += 0.1) {
				double sur = 0;
				for (int x = 0; x < longueur; x++) {
					if (poids[x] == 0) {
						continue;
					}
					double reste = ((x - decalage) % periode + periode) % periode;
					double ecart = Math.min(reste, periode - reste);
					if (ecart < TOLERANCE) {
						sur += poids[x];
					}
				}
				double observe = sur / total;
				double score = observe - attendu;
				if (score > meilleurPourCettePeriode) {
					meilleurPourCettePeriode = score;
					meilleurDecalage = decalage;
				}
			}
			resultats.add(new double[] { meilleurPourCettePeriode, periode, meilleurDecalage });
		}

		resultats.sort(Comparator.comparingDouble((double[] r) -> -r[0]));

		// On n'affiche que des periodes vraiment differentes entre elles, sinon les
		// cinq premieres lignes ne sont que la meme reponse a 0,01 pres.
		List<double[]> retenus = new ArrayList<>();
		for (double[] r : resultats) {
			boolean tropProche = retenus.stream().anyMatch(v -> Math.abs(v[1] - r[1]) < 0.6);
			if (!tropProche) {
				retenus.add(r);
			}
			if (retenus.size() == 5) {
				break;
			}
		}

		for (double[] r : retenus) {
			System.out.printf("    periode %6.3f  decalage %5.2f  ecart-au-hasard %+.3f"
					+ "   -> sprite %.2f blocs%n",
					r[1], r[2], r[0], longueur / (double) cases / r[1]);
		}
	}

	static int distance(int a, int b) {
		return Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF))
				+ Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF))
				+ Math.abs((a & 0xFF) - (b & 0xFF));
	}
}
