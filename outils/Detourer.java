import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Decoupe une planche de pixel art en icones PNG a fond transparent.
 *
 * <p>Les planches viennent de Gemini, en JPEG, sur fond magenta. Trois choses
 * rendent le travail moins evident qu'il n'y parait, et chacune a sa parade ici.
 *
 * <h2>1. Le JPEG a bave</h2>
 *
 * <p>Le magenta n'est pas #FF00FF partout : il varie, et il deborde de quelques
 * pixels sur le contour des objets. Un test de couleur exact ne detourerait rien.
 * On teste donc « franchement rouge et bleu, franchement pas vert », largement.
 *
 * <h2>2. Un gateau rose n'est pas du fond</h2>
 *
 * <p>Une des icones est un gateau d'anniversaire au glacage rose. Avec un test de
 * couleur tolerant, son glacage passe pour du fond et le gateau se retrouve troue.
 * On ne se contente donc pas de la couleur : on <b>remplit depuis les bords</b>.
 * Seul le magenta qui communique avec l'exterieur devient transparent. Le rose
 * enferme dans un contour reste.
 *
 * <h2>3. La grille de Gemini n'est jamais celle qu'on a demandee</h2>
 *
 * <p>On a demande des cases de 32x32 pixels affichees a 8x. On recoit 1632x656,
 * soit 10,2 pixels par bloc horizontalement et 10,25 verticalement — et la derive
 * s'accumule sur 32 blocs. Plutot que de supposer une grille globale, on mesure
 * la periode <b>a l'interieur de chaque icone</b>, ou la derive n'a pas le temps
 * de s'installer, puis on lit la <b>couleur au centre de chaque bloc</b>. Le
 * centre d'un bloc est la seule zone que le JPEG n'a pas melangee a sa voisine.
 */
public final class Detourer {

	/** Cote de l'image finale. Les icones d'objet du mod sont carrees. */
	private static final int COTE = 32;

	/** En dessous de tant de pixels, une tache est du bruit JPEG, pas un dessin. */
	private static final int TACHE_MINIMALE = 60;

	/** Colonnes et lignes de la grille des planches. */
	private static final int COLONNES = 5;
	private static final int LIGNES = 2;

	/**
	 * Jusquou le fond a bave sur les objets, en pixels de la planche.
	 *
	 * <p>Le JPEG melange chaque pixel avec ses voisins. Au contact du magenta, les
	 * pixels du contour deviennent violets — et cette teinte survivait au
	 * detourage, laissant une frange autour de chaque icone.
	 *
	 * <p>On ecarte donc ces pixels-la du calcul de couleur. Attention : uniquement
	 * du calcul de COULEUR. S'ils decidaient aussi de la transparence, chaque icone
	 * maigrirait de trois pixels et perdrait son contour.
	 */
	private static final int RAYON_DE_BAVE = 3;

	/**
	 * A quelle distance de la couleur du fond un pixel enferme en est encore.
	 *
	 * <p>Somme des ecarts sur les trois canaux. Vingt est tres serre : deux teintes
	 * de magenta du meme aplat JPEG en sont a moins de dix, le glacage rose du
	 * gateau a plus de deux cents.
	 */
	private static final int ECART_AU_FOND = 45;

	public static void main(String[] args) throws Exception {
		String source = null;
		String sortie = null;
		String prefixe = "icone";
		int couperEnBas = 0;
		// Mode libre : chaque forme est une icone a elle seule, et garde ses vraies
		// proportions. Pour les planches qui ne suivent pas la grille 5x2 — la bulle
		// de pensee, ou Gemini a pose six formes de tailles tres differentes, dont
		// une a cheval sur deux cases.
		boolean libre = false;

		for (String arg : args) {
			if (arg.startsWith("--sortie=")) {
				sortie = arg.substring(9);
			} else if (arg.startsWith("--prefixe=")) {
				prefixe = arg.substring(10);
			} else if (arg.startsWith("--bas=")) {
				couperEnBas = Integer.parseInt(arg.substring(6));
			} else if (arg.equals("--libre")) {
				libre = true;
			} else {
				source = arg;
			}
		}
		if (source == null || sortie == null) {
			System.err.println("usage: Detourer <planche.jpg> --sortie=<dossier> "
					+ "[--prefixe=nom] [--bas=N]");
			System.exit(2);
		}

		BufferedImage planche = ImageIO.read(new File(source));
		if (couperEnBas > 0) {
			// La derniere planche porte des legendes ecrites sous les icones. Elles
			// ne sont pas magenta, donc elles survivraient au detourage.
			planche = planche.getSubimage(0, 0, planche.getWidth(),
					planche.getHeight() - couperEnBas);
		}

		new File(sortie).mkdirs();

		boolean[][] fond = fondDepuisLesBords(planche);
		boolean[][] souille = elargir(fond, RAYON_DE_BAVE);
		List<int[]> taches = taches(planche, fond);
		List<int[]> icones = libre
				? deGaucheADroite(taches)
				: regrouperParCase(taches, planche.getWidth(), planche.getHeight());

		System.out.printf("%s : %d tache(s) -> %d icone(s)%n",
				new File(source).getName(), taches.size(), icones.size());

		for (int i = 0; i < icones.size(); i++) {
			int[] boite = icones.get(i);
			BufferedImage icone = extraire(planche, fond, souille, boite, libre);
			String nom = String.format("%s_%02d.png", prefixe, i);
			ImageIO.write(icone, "PNG", new File(sortie, nom));
			System.out.printf("  %s  boite %dx%d en (%d,%d)%n",
					nom, boite[2] - boite[0], boite[3] - boite[1], boite[0], boite[1]);
		}
	}

	// --- 1. Le fond -----------------------------------------------------------------

	/** Un pixel peut appartenir au fond s'il est franchement magenta. Large exprès. */
	private static boolean magenta(int argb) {
		int r = (argb >> 16) & 0xFF;
		int v = (argb >> 8) & 0xFF;
		int b = argb & 0xFF;
		return r > 120 && b > 120 && (r - v) > 45 && (b - v) > 45;
	}

	/**
	 * Le vrai fond : le magenta qui communique avec le bord de l'image.
	 *
	 * <p>C'est ce qui sauve le gateau rose. Un remplissage par diffusion depuis les
	 * quatre bords ne peut pas entrer dans un objet ferme par son contour sombre.
	 */
	private static boolean[][] fondDepuisLesBords(BufferedImage image) {
		int l = image.getWidth();
		int h = image.getHeight();
		boolean[][] fond = new boolean[h][l];
		ArrayDeque<int[]> aVoir = new ArrayDeque<>();

		for (int x = 0; x < l; x++) {
			semer(image, fond, aVoir, x, 0);
			semer(image, fond, aVoir, x, h - 1);
		}
		for (int y = 0; y < h; y++) {
			semer(image, fond, aVoir, 0, y);
			semer(image, fond, aVoir, l - 1, y);
		}

		int[][] voisins = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
		while (!aVoir.isEmpty()) {
			int[] point = aVoir.poll();
			for (int[] pas : voisins) {
				int x = point[0] + pas[0];
				int y = point[1] + pas[1];
				if (x >= 0 && y >= 0 && x < l && y < h) {
					semer(image, fond, aVoir, x, y);
				}
			}
		}

		lesTrous(image, fond);
		return fond;
	}

	/**
	 * Le fond emprisonne a l'interieur des objets.
	 *
	 * <p>La boucle d'un fil a coudre, l'espace entre deux lames de ciseaux, les
	 * anneaux d'un serpent enroule : on voit le fond au travers, mais il ne
	 * communique pas avec le bord de l'image — ou seulement par une diagonale d'un
	 * pixel, que le remplissage ne franchit pas. Ces trous restaient magenta.
	 *
	 * <p>On ne peut pas les traiter comme le reste : c'est justement la souplesse
	 * du premier test qui sauve le gateau rose. Ici on exige donc que la couleur
	 * soit <b>celle du fond lui-meme</b>, mesuree sur l'image, a quelques unites
	 * pres. Le rose du gateau en est a plus de deux cents unites ; un trou de fond,
	 * a moins de vingt.
	 */
	private static void lesTrous(BufferedImage image, boolean[][] fond) {
		int l = image.getWidth();
		int h = image.getHeight();

		List<Integer> rouges = new ArrayList<>();
		List<Integer> verts = new ArrayList<>();
		List<Integer> bleus = new ArrayList<>();
		// Un echantillon suffit largement, et evite de parcourir un million de
		// pixels pour une mediane qui ne bougera pas.
		for (int y = 0; y < h; y += 5) {
			for (int x = 0; x < l; x += 5) {
				if (fond[y][x]) {
					int rgb = image.getRGB(x, y);
					rouges.add((rgb >> 16) & 0xFF);
					verts.add((rgb >> 8) & 0xFF);
					bleus.add(rgb & 0xFF);
				}
			}
		}
		if (rouges.isEmpty()) {
			return;
		}
		int couleurDuFond = 0xFF000000 | (mediane(rouges) << 16)
				| (mediane(verts) << 8) | mediane(bleus);

		int combles = 0;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < l; x++) {
				if (!fond[y][x] && distance(image.getRGB(x, y), couleurDuFond) < ECART_AU_FOND) {
					fond[y][x] = true;
					combles++;
				}
			}
		}
		if (combles > 0) {
			System.out.printf("  fond %06X, %d pixel(s) enfermes rendus transparents%n",
					couleurDuFond & 0xFFFFFF, combles);
		}
	}

	private static void semer(BufferedImage image, boolean[][] fond,
			ArrayDeque<int[]> aVoir, int x, int y) {
		if (!fond[y][x] && magenta(image.getRGB(x, y))) {
			fond[y][x] = true;
			aVoir.add(new int[] { x, y });
		}
	}

	/**
	 * Le fond, epaissi de quelques pixels vers l'interieur des objets.
	 *
	 * <p>Sert uniquement a savoir quels pixels sont trop souilles par le fond pour
	 * qu'on leur demande leur couleur. Fait en deux passes — d'abord horizontale,
	 * puis verticale — parce qu'un carre se dilate exactement comme cela, et que
	 * la version naive en {@code rayon x rayon} par pixel serait cent fois plus
	 * lente sur une planche d'un million de pixels.
	 */
	private static boolean[][] elargir(boolean[][] fond, int rayon) {
		int h = fond.length;
		int l = fond[0].length;

		boolean[][] passe = new boolean[h][l];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < l; x++) {
				if (!fond[y][x]) {
					continue;
				}
				for (int dx = -rayon; dx <= rayon; dx++) {
					int nx = x + dx;
					if (nx >= 0 && nx < l) {
						passe[y][nx] = true;
					}
				}
			}
		}

		boolean[][] large = new boolean[h][l];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < l; x++) {
				if (!passe[y][x]) {
					continue;
				}
				for (int dy = -rayon; dy <= rayon; dy++) {
					int ny = y + dy;
					if (ny >= 0 && ny < h) {
						large[ny][x] = true;
					}
				}
			}
		}
		return large;
	}

	// --- 2. Les taches ---------------------------------------------------------------

	/**
	 * Les groupes de pixels qui ne sont pas du fond.
	 *
	 * @return une boite {x0, y0, x1, y1} par tache, les plus petites ecartees
	 */
	private static List<int[]> taches(BufferedImage image, boolean[][] fond) {
		int l = image.getWidth();
		int h = image.getHeight();
		boolean[][] vu = new boolean[h][l];
		List<int[]> trouvees = new ArrayList<>();

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < l; x++) {
				if (vu[y][x] || fond[y][x]) {
					continue;
				}
				int[] boite = { x, y, x + 1, y + 1 };
				int taille = 0;

				ArrayDeque<int[]> aVoir = new ArrayDeque<>();
				aVoir.add(new int[] { x, y });
				vu[y][x] = true;

				while (!aVoir.isEmpty()) {
					int[] point = aVoir.poll();
					taille++;
					boite[0] = Math.min(boite[0], point[0]);
					boite[1] = Math.min(boite[1], point[1]);
					boite[2] = Math.max(boite[2], point[0] + 1);
					boite[3] = Math.max(boite[3], point[1] + 1);

					// Huit voisins : les diagonales evitent de couper une ligne
					// oblique d'un pixel de large, frequente en pixel art.
					for (int dy = -1; dy <= 1; dy++) {
						for (int dx = -1; dx <= 1; dx++) {
							int nx = point[0] + dx;
							int ny = point[1] + dy;
							if (nx >= 0 && ny >= 0 && nx < l && ny < h
									&& !vu[ny][nx] && !fond[ny][nx]) {
								vu[ny][nx] = true;
								aVoir.add(new int[] { nx, ny });
							}
						}
					}
				}
				if (taille >= TACHE_MINIMALE) {
					trouvees.add(boite);
				}
			}
		}
		return trouvees;
	}

	/**
	 * Rassemble les taches par case de la grille.
	 *
	 * <p>Une icone peut se presenter en plusieurs morceaux : les etincelles autour
	 * du pot de miel, les volutes de vapeur au-dessus de la soupe, les miettes a
	 * cote de la truffe. Elles appartiennent au dessin et doivent voyager avec lui.
	 *
	 * <p>Le rattachement se fait par centre de gravite : meme si la grille de
	 * Gemini derive de quelques pixels, un morceau est toujours tres loin des
	 * bords de sa case.
	 */
	private static List<int[]> regrouperParCase(List<int[]> taches, int l, int h) {
		int[][] boites = new int[COLONNES * LIGNES][];

		for (int[] tache : taches) {
			double cx = (tache[0] + tache[2]) / 2.0;
			double cy = (tache[1] + tache[3]) / 2.0;
			int colonne = Math.min(COLONNES - 1, (int) (cx * COLONNES / l));
			int ligne = Math.min(LIGNES - 1, (int) (cy * LIGNES / h));
			int index = ligne * COLONNES + colonne;

			if (boites[index] == null) {
				boites[index] = tache.clone();
			} else {
				boites[index][0] = Math.min(boites[index][0], tache[0]);
				boites[index][1] = Math.min(boites[index][1], tache[1]);
				boites[index][2] = Math.max(boites[index][2], tache[2]);
				boites[index][3] = Math.max(boites[index][3], tache[3]);
			}
		}

		List<int[]> resultat = new ArrayList<>();
		for (int[] boite : boites) {
			if (boite != null) {
				resultat.add(boite);
			}
		}
		return resultat;
	}

	// --- 3. La reduction -------------------------------------------------------------

	/**
	 * Ramene une icone a sa vraie definition, puis la centre dans un carre.
	 *
	 * <p>On mesure la taille du bloc dans cette icone-la, on lit la couleur au
	 * centre de chaque bloc, et on garde les proportions : un os de poulet plus
	 * large que haut ne doit pas devenir carre.
	 */
	private static BufferedImage extraire(BufferedImage planche, boolean[][] fond,
			boolean[][] souille, int[] boite, boolean libre) {
		int l = boite[2] - boite[0];
		int h = boite[3] - boite[1];

		double periode = mesurerPeriode(planche, fond, boite);
		int blocsX = Math.max(1, (int) Math.round(l / periode));
		int blocsY = Math.max(1, (int) Math.round(h / periode));

		// Le sprite ne doit jamais depasser le carre final, sinon on perdrait des
		// blocs. On reduit les deux cotes du meme facteur pour ne pas deformer.
		if (!libre && (blocsX > COTE || blocsY > COTE)) {
			double facteur = Math.max(blocsX / (double) COTE, blocsY / (double) COTE);
			blocsX = Math.max(1, (int) Math.round(blocsX / facteur));
			blocsY = Math.max(1, (int) Math.round(blocsY / facteur));
		}

		BufferedImage petite = new BufferedImage(blocsX, blocsY, BufferedImage.TYPE_INT_ARGB);

		// Quels blocs n'ont eu que des pixels souilles a se mettre sous la dent.
		// C'est un FAIT MESURE, et non une teinte devinee apres coup : le glacage
		// rose du gateau d'anniversaire passe tous les tests de couleur qu'on
		// pourrait ecrire, et se ferait repeindre par erreur.
		boolean[][] contamine = new boolean[blocsY][blocsX];

		for (int by = 0; by < blocsY; by++) {
			for (int bx = 0; bx < blocsX; bx++) {
				boolean[] sale = new boolean[1];
				petite.setRGB(bx, by, couleurDuBloc(planche, fond, souille, boite,
						bx, by, blocsX, blocsY, sale));
				contamine[by][bx] = sale[0];
			}
		}

		nettoyerLaFrange(petite, contamine);

		if (libre) {
			// Rien a centrer : la forme EST l'image. Un nuage de soixante
			// pixels de large tasse dans un carre de trente-deux perdrait la moitie
			// de ses bosses, et le texte n'y tiendrait plus.
			return petite;
		}

		BufferedImage finale = new BufferedImage(COTE, COTE, BufferedImage.TYPE_INT_ARGB);
		int decalageX = (COTE - blocsX) / 2;
		int decalageY = (COTE - blocsY) / 2;
		for (int y = 0; y < blocsY; y++) {
			for (int x = 0; x < blocsX; x++) {
				finale.setRGB(x + decalageX, y + decalageY, petite.getRGB(x, y));
			}
		}
		return finale;
	}

	/**
	 * La couleur d'un bloc : la mediane du carre central.
	 *
	 * <p>Le centre seulement — les bords d'un bloc sont exactement l'endroit ou le
	 * JPEG a melange deux couleurs voisines. Et la mediane plutot que la moyenne :
	 * sur une frontiere mal placee, la moyenne inventerait une couleur qui n'existe
	 * nulle part dans le dessin, la mediane choisit une couleur reellement presente.
	 */
	private static int couleurDuBloc(BufferedImage planche, boolean[][] fond,
			boolean[][] souille, int[] boite, int bx, int by, int blocsX, int blocsY,
			boolean[] sale) {

		double largeurBloc = (boite[2] - boite[0]) / (double) blocsX;
		double hauteurBloc = (boite[3] - boite[1]) / (double) blocsY;

		// Le bloc entier, et non plus seulement son centre : maintenant que les
		// pixels souilles sont ecartes nommement, se priver du reste ne servirait
		// qu'a calculer la mediane sur trop peu d'echantillons.
		int x0 = (int) Math.round(boite[0] + bx * largeurBloc);
		int x1 = (int) Math.round(boite[0] + (bx + 1) * largeurBloc);
		int y0 = (int) Math.round(boite[1] + by * hauteurBloc);
		int y1 = (int) Math.round(boite[1] + (by + 1) * hauteurBloc);

		x1 = Math.max(x1, x0 + 1);
		y1 = Math.max(y1, y0 + 1);

		List<Integer> rouges = new ArrayList<>();
		List<Integer> verts = new ArrayList<>();
		List<Integer> bleus = new ArrayList<>();
		// Le repli, quand tout le bloc est souille : un contour d'un pixel de large
		// n'a aucun pixel propre, et le laisser vide troue l'icone.
		List<Integer> rougesSales = new ArrayList<>();
		List<Integer> vertsSales = new ArrayList<>();
		List<Integer> bleusSales = new ArrayList<>();

		int transparents = 0;
		int total = 0;

		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				if (x < 0 || y < 0 || x >= planche.getWidth() || y >= planche.getHeight()) {
					continue;
				}
				total++;
				if (fond[y][x]) {
					transparents++;
					continue;
				}
				int rgb = planche.getRGB(x, y);
				if (souille[y][x]) {
					rougesSales.add((rgb >> 16) & 0xFF);
					vertsSales.add((rgb >> 8) & 0xFF);
					bleusSales.add(rgb & 0xFF);
				} else {
					rouges.add((rgb >> 16) & 0xFF);
					verts.add((rgb >> 8) & 0xFF);
					bleus.add(rgb & 0xFF);
				}
			}
		}

		// Un bloc majoritairement fond est du fond. Pas de demi-transparence : le
		// pixel art n'en a pas, et Minecraft afficherait un halo.
		if (total == 0 || transparents * 2 > total) {
			return 0x00000000;
		}
		if (!rouges.isEmpty()) {
			return 0xFF000000 | (mediane(rouges) << 16) | (mediane(verts) << 8) | mediane(bleus);
		}
		if (!rougesSales.isEmpty()) {
			sale[0] = true;
			return 0xFF000000 | (mediane(rougesSales) << 16)
					| (mediane(vertsSales) << 8) | mediane(bleusSales);
		}
		return 0x00000000;
	}

	/** Les formes prises une par une, de haut en bas puis de gauche a droite. */
	private static List<int[]> deGaucheADroite(List<int[]> taches) {
		List<int[]> triees = new ArrayList<>(taches);
		triees.sort((a, b) -> {
			// Deux formes sont sur la meme ligne si elles se chevauchent
			// verticalement : sinon un nuage haut passerait avant un rond pose
			// plus haut mais plus a gauche.
			boolean memeLigne = a[1] < b[3] && b[1] < a[3];
			return memeLigne ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]);
		});
		return triees;
	}

	// --- 4. La frange ----------------------------------------------------------------

	/**
	 * Efface la teinte violette laissee par le fond sur les contours.
	 *
	 * <p>Quand un bloc n'avait que des pixels souilles, sa couleur porte encore du
	 * magenta. En pixel art la reparation est evidente : les aplats sont plats, et
	 * le contour d'a cote est de la meme couleur que celui-ci. On recopie donc le
	 * voisin propre le plus proche.
	 *
	 * <p>S'il n'y a aucun voisin propre — une volute de vapeur d'un bloc de large,
	 * entierement noyee dans le fond — on ne peut rien recopier. On enleve alors la
	 * teinte en ramenant le rouge et le bleu au niveau du vert, ce qui rend a la
	 * vapeur son gris. Ce n'est juste que pour les gris et les blancs, mais c'est
	 * exactement la ou le cas se produit.
	 */
	private static void nettoyerLaFrange(BufferedImage image, boolean[][] aRefaire) {
		int l = image.getWidth();
		int h = image.getHeight();

		BufferedImage avant = new BufferedImage(l, h, BufferedImage.TYPE_INT_ARGB);
		avant.setRGB(0, 0, l, h, lire(image), 0, l);

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < l; x++) {
				if (!aRefaire[y][x]) {
					continue;
				}
				Integer propre = voisinPropre(avant, aRefaire, x, y);
				image.setRGB(x, y, propre != null ? propre : sansTeinte(avant.getRGB(x, y)));
			}
		}
	}

	private static int[] lire(BufferedImage image) {
		int l = image.getWidth();
		int h = image.getHeight();
		int[] pixels = new int[l * h];
		image.getRGB(0, 0, l, h, pixels, 0, l);
		return pixels;
	}

	/** Le voisin opaque et non souille le plus proche, en cercles concentriques. */
	private static Integer voisinPropre(BufferedImage image, boolean[][] aRefaire, int cx, int cy) {
		int l = image.getWidth();
		int h = image.getHeight();
		for (int rayon = 1; rayon <= 3; rayon++) {
			for (int dy = -rayon; dy <= rayon; dy++) {
				for (int dx = -rayon; dx <= rayon; dx++) {
					if (Math.max(Math.abs(dx), Math.abs(dy)) != rayon) {
						continue;
					}
					int x = cx + dx;
					int y = cy + dy;
					if (x < 0 || y < 0 || x >= l || y >= h || aRefaire[y][x]) {
						continue;
					}
					int argb = image.getRGB(x, y);
					if ((argb >>> 24) > 0) {
						return argb;
					}
				}
			}
		}
		return null;
	}

	private static int sansTeinte(int argb) {
		int r = (argb >> 16) & 0xFF;
		int v = (argb >> 8) & 0xFF;
		int b = argb & 0xFF;
		int gris = Math.min(r, Math.min(v, b)) + (v - Math.min(r, Math.min(v, b)));
		int nouveauR = Math.min(r, Math.max(v, gris));
		int nouveauB = Math.min(b, Math.max(v, gris));
		return 0xFF000000 | (nouveauR << 16) | (v << 8) | nouveauB;
	}

	private static int mediane(List<Integer> valeurs) {
		int[] tableau = valeurs.stream().mapToInt(Integer::intValue).sorted().toArray();
		return tableau[tableau.length / 2];
	}

	/**
	 * La taille du bloc a l'interieur de cette icone.
	 *
	 * <p>On compare ce qu'on observe a ce que donnerait un placement au hasard :
	 * sans cette correction, les petites periodes gagnent toujours, puisqu'elles
	 * offrent plus de frontieres a toucher. On ecarte ensuite les diviseurs de la
	 * meilleure periode, qui obtiennent mecaniquement un bon score sans etre la
	 * bonne reponse — la moitie d'une periode juste est juste elle aussi.
	 */
	private static double mesurerPeriode(BufferedImage planche, boolean[][] fond, int[] boite) {
		int l = boite[2] - boite[0];
		double[] poids = new double[l];

		for (int y = boite[1]; y < boite[3]; y++) {
			for (int x = boite[0] + 1; x < boite[2]; x++) {
				if (fond[y][x] || fond[y][x - 1]) {
					continue;
				}
				int d = distance(planche.getRGB(x - 1, y), planche.getRGB(x, y));
				if (d > 60) {
					poids[x - boite[0]] += 1;
				}
			}
		}

		double total = Arrays.stream(poids).sum();
		if (total < 20) {
			// Trop peu de detail pour mesurer quoi que ce soit : on retombe sur la
			// grille nominale de la planche.
			return planche.getHeight() / (double) LIGNES / COTE;
		}

		List<double[]> candidats = new ArrayList<>();
		for (double periode = 7.0; periode <= 14.0; periode += 0.01) {
			double attendu = Math.min(1.0, 1.5 / periode);
			double meilleur = -1;
			for (double decalage = 0; decalage < periode; decalage += 0.1) {
				double sur = 0;
				for (int x = 0; x < l; x++) {
					if (poids[x] == 0) {
						continue;
					}
					double reste = ((x - decalage) % periode + periode) % periode;
					if (Math.min(reste, periode - reste) < 0.75) {
						sur += poids[x];
					}
				}
				meilleur = Math.max(meilleur, sur / total - attendu);
			}
			candidats.add(new double[] { meilleur, periode });
		}

		candidats.sort(Comparator.comparingDouble(c -> -c[0]));
		return candidats.get(0)[1];
	}

	private static int distance(int a, int b) {
		return Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF))
				+ Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF))
				+ Math.abs((a & 0xFF) - (b & 0xFF));
	}
}
