import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

/**
 * Assemble les icones detourees en une planche de controle, agrandie et posee sur
 * un damier.
 *
 * <p>Le damier n'est pas decoratif : sur fond uni, un halo de fond mal detoure ou
 * un pixel reste opaque ne se voit pas. Sur un damier, si.
 */
public final class Contact {

	private static final int ZOOM = 6;
	private static final int MARGE = 4;
	private static final int CARREAU = 8;
	private static final int CLAIR = 0xFFB0B0B0;
	private static final int SOMBRE = 0xFF707070;

	public static void main(String[] args) throws Exception {
		File dossier = new File(args[0]);
		File sortie = new File(args[1]);
		int colonnes = args.length > 2 ? Integer.parseInt(args[2]) : 5;

		File[] fichiers = dossier.listFiles((d, n) -> n.endsWith(".png"));
		if (fichiers == null || fichiers.length == 0) {
			System.err.println("aucune icone dans " + dossier);
			System.exit(1);
		}
		Arrays.sort(fichiers);

		// La case est taillee sur la plus grande image, pas sur la premiere : une
		// planche melangeant une bulle de 9 pixels et un nuage de 78 debordait.
		int largeMax = 1;
		int hautMax = 1;
		for (File f : fichiers) {
			BufferedImage im = ImageIO.read(f);
			largeMax = Math.max(largeMax, im.getWidth());
			hautMax = Math.max(hautMax, im.getHeight());
		}
		int cote = largeMax * ZOOM;
		int hauteurCase = hautMax * ZOOM;
		int lignes = (fichiers.length + colonnes - 1) / colonnes;

		BufferedImage planche = new BufferedImage(
				colonnes * (cote + MARGE) + MARGE,
				lignes * (hauteurCase + MARGE) + MARGE,
				BufferedImage.TYPE_INT_RGB);

		for (int y = 0; y < planche.getHeight(); y++) {
			for (int x = 0; x < planche.getWidth(); x++) {
				boolean pair = ((x / CARREAU) + (y / CARREAU)) % 2 == 0;
				planche.setRGB(x, y, pair ? CLAIR : SOMBRE);
			}
		}

		for (int i = 0; i < fichiers.length; i++) {
			BufferedImage icone = ImageIO.read(fichiers[i]);
			int ox = MARGE + (i % colonnes) * (cote + MARGE);
			int oy = MARGE + (i / colonnes) * (hauteurCase + MARGE);

			for (int y = 0; y < icone.getHeight() * ZOOM; y++) {
				for (int x = 0; x < icone.getWidth() * ZOOM; x++) {
					int argb = icone.getRGB(x / ZOOM, y / ZOOM);
					// On ne dessine que l'opaque : le damier doit rester visible
					// partout ou l'icone est censee etre transparente.
					if ((argb >>> 24) > 127) {
						planche.setRGB(ox + x, oy + y, argb);
					}
				}
			}
		}

		ImageIO.write(planche, "PNG", sortie);
		System.out.println(sortie + "  " + planche.getWidth() + "x" + planche.getHeight()
				+ "  (" + fichiers.length + " icones)");
	}
}
