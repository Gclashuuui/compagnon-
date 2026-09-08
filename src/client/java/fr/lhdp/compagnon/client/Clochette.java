package fr.lhdp.compagnon.client;

import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * La clochette : de quoi faire taire les interfaces, en la faisant tinter.
 *
 * <h2>Pourquoi pas un curseur</h2>
 *
 * <p>Un curseur de volume, il faut le viser, le tirer, et regarder un nombre
 * pour savoir ou on en est. Pour trois crans, c'est trois fois trop de travail —
 * et ca ressemble a un panneau de reglages, ce qu'aucun de ces ecrans n'est.
 *
 * <p>Une clochette se clique. Elle se balance, elle sonne, et elle sonne
 * <b>au volume qu'elle vient de choisir</b> : le reglage se demontre tout seul.
 * On n'a rien a lire, on entend ce qu'on a pris.
 *
 * <p>Sur « muet », elle se balance quand meme — et il n'en sort rien. C'est la
 * facon la plus courte de dire « muet » qu'on ait trouvee, et c'est la seule qui
 * fasse sourire.
 *
 * <h2>Ce qui est dessine</h2>
 *
 * <p>Une cloche de dix pixels, au pixel pres, sans aucune texture a livrer. A
 * cote d'elle, ce qui en sort : <b>deux ondes</b> quand tout est fort, une seule
 * en doux, un trait barre quand c'est muet. On lit le niveau sans compter et
 * sans savoir lire.
 *
 * <h2>Elle est au meme endroit partout</h2>
 *
 * <p>En haut a droite de l'ecran, sur les six ecrans du mod. Un reglage qui
 * change de place d'un ecran a l'autre est un reglage qu'on ne trouve jamais.
 */
public final class Clochette {

	private Clochette() {
	}

	/** Sa boite cliquable, genereuse : une cloche de dix pixels se rate. */
	public static final int LARGEUR = 26;
	public static final int HAUTEUR = 18;

	/** Sa distance au coin de l'ecran. */
	private static final int MARGE = 5;

	private static final int LAITON = 0xFFE2B75C;
	private static final int LAITON_CLAIR = 0xFFF6DFA0;
	private static final int LAITON_SOMBRE = 0xFF8A6A26;
	private static final int ETEINTE = 0xFF6E6558;
	private static final int ETEINTE_CLAIRE = 0xFF938878;
	private static final int ONDE = 0xFFF6DFA0;
	private static final int BARRE = 0xFFE05A50;

	/** L'angle maximum du balancement, en degres. */
	private static final float ANGLE = 24.0F;

	/** La vitesse du balancement. Plus haut, elle sonne comme un reveil. */
	private static final float CADENCE = 0.55F;

	/** De un a zero : l'elan qui reste dans la cloche. */
	private static float elan;

	/** Ou en est le balancement. Avance tant qu'il reste de l'elan. */
	private static float phase;

	/** De zero a un : la souris est dessus. */
	private static float chaleur;

	// --- Ou elle est ----------------------------------------------------------

	public static int x(int largeurEcran) {
		return largeurEcran - LARGEUR - MARGE;
	}

	public static int y() {
		return MARGE;
	}

	public static boolean sous(double sourisX, double sourisY, int largeurEcran) {
		int x = x(largeurEcran);
		return sourisX >= x && sourisX < x + LARGEUR
				&& sourisY >= y() && sourisY < y() + HAUTEUR;
	}

	// --- Ce qu'elle fait ------------------------------------------------------

	/**
	 * On l'a cliquee : elle tourne d'un cran, se met a se balancer, et sonne.
	 *
	 * <p>Le son part <b>apres</b> le changement de niveau, jamais avant : c'est
	 * ce qui fait qu'on entend le volume choisi et non celui qu'on quitte.
	 */
	public static void tinter() {
		Bruits.tourner();
		elan = 1.0F;
		phase = 0.0F;
		Bruits.clochette();
	}

	// --- Le dessin ------------------------------------------------------------

	/**
	 * Dessine la clochette dans le coin, avec son balancement.
	 *
	 * <p>A appeler <b>en dernier</b> dans le rendu d'un ecran : elle doit passer
	 * par-dessus les voiles et les panneaux, sans quoi elle se retrouve dans le
	 * noir sur la roue.
	 */
	public static void dessiner(GuiGraphics g, int largeurEcran, int sourisX, int sourisY,
			float partiel) {

		int x = x(largeurEcran);
		int y = y();
		boolean survolee = sous(sourisX, sourisY, largeurEcran);

		chaleur = Peinture.vers(chaleur, survolee ? 1.0F : 0.0F, 0.25F, partiel);

		// Le balancement s'eteint tout seul. Tant qu'il reste de l'elan, la phase
		// avance — une fois arretee, la cloche est droite et ne coute plus rien.
		if (elan > 0.01F) {
			phase += Math.max(0.2F, partiel * 3.0F);
			elan = Peinture.vers(elan, 0.0F, 0.055F, partiel);
		} else {
			elan = 0.0F;
		}

		int niveau = Bruits.niveau();
		boolean muette = niveau == Bruits.MUET;

		int corps = muette ? ETEINTE : LAITON;
		int clair = muette ? ETEINTE_CLAIRE : LAITON_CLAIR;
		if (chaleur > 0.0F) {
			corps = Peinture.melanger(corps, clair, Peinture.adoucir(chaleur) * 0.7F);
		}

		// Le pivot est en haut de l'anse : une cloche tourne autour de son
		// accroche, pas autour de son milieu. Pivoter au centre donnerait un
		// balancier, pas une cloche.
		float angle = (float) Math.sin(phase * CADENCE) * ANGLE * elan;
		int pivotX = x + 9;
		int pivotY = y + 2;

		g.pose().pushPose();
		g.pose().translate(pivotX, pivotY, 0.0F);
		g.pose().mulPose(Axis.ZP.rotationDegrees(angle));
		g.pose().translate(-pivotX, -pivotY, 0.0F);
		cloche(g, x + 4, y + 2, corps, clair, muette);
		g.pose().popPose();

		// Ce qui en sort ne se balance pas : le son ne suit pas la cloche.
		if (niveau >= Bruits.FORT) {
			onde(g, x + 18, y + 8, 3);
		}
		if (niveau >= Bruits.DOUX) {
			onde(g, x + 16, y + 8, 2);
		} else {
			barre(g, x + 3, y + 2);
		}
	}

	/** La cloche elle-meme, dessinee au pixel. */
	private static void cloche(GuiGraphics g, int x, int y, int corps, int clair,
			boolean muette) {

		// L'anse.
		g.fill(x + 4, y - 2, x + 6, y - 1, corps);
		g.fill(x + 3, y - 1, x + 7, y, corps);

		// La coupole, de plus en plus large.
		g.fill(x + 2, y, x + 8, y + 2, corps);
		g.fill(x + 1, y + 2, x + 9, y + 6, corps);
		g.fill(x, y + 6, x + 10, y + 8, corps);

		// La lumiere sur la gauche : deux traits, et la cloche cesse d'etre une
		// silhouette pour devenir un volume.
		g.fill(x + 2, y + 1, x + 3, y + 6, clair);
		g.fill(x + 1, y + 6, x + 2, y + 8, clair);

		// Le bord, plus sombre, qui la pose.
		g.fill(x, y + 8, x + 10, y + 9, muette ? ETEINTE : LAITON_SOMBRE);

		// Le battant. Rentre quand elle est muette : rien ne va frapper.
		if (!muette) {
			g.fill(x + 4, y + 9, x + 6, y + 11, LAITON_SOMBRE);
		}
	}

	/**
	 * Une onde qui sort de la cloche.
	 *
	 * <p>Un arc de trois pixels, ouvert vers la droite. Deux arcs valent « fort »,
	 * un seul vaut « doux » — ce sont les memes traits que sur une icone de
	 * volume, en plus petit, donc personne n'a rien a apprendre.
	 */
	private static void onde(GuiGraphics g, int x, int y, int hauteur) {
		g.fill(x, y - hauteur, x + 1, y - hauteur + 1, ONDE);
		g.fill(x + 1, y - hauteur + 1, x + 2, y + hauteur, ONDE);
		g.fill(x, y + hauteur, x + 1, y + hauteur + 1, ONDE);
	}

	/** Le trait barre du silence, en diagonale sur la cloche. */
	private static void barre(GuiGraphics g, int x, int y) {
		for (int i = 0; i < 14; i++) {
			g.fill(x + i, y + i - 1, x + i + 2, y + i, BARRE);
		}
	}

	/** Ce qu'elle raconte quand on s'arrete dessus. */
	public static Component motDeLEtat() {
		String cle = switch (Bruits.niveau()) {
			case Bruits.MUET -> "clochette.compagnon.muet";
			case Bruits.DOUX -> "clochette.compagnon.doux";
			default -> "clochette.compagnon.fort";
		};
		return Component.translatable(cle);
	}
}
