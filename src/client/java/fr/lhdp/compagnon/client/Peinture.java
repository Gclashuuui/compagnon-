package fr.lhdp.compagnon.client;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.lhdp.compagnon.Compagnon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * De quoi peindre les ecrans du mod.
 *
 * <h2>Pourquoi une boite a outils</h2>
 *
 * <p>Chaque ecran redessinait ses propres cadres a coups de rectangles. Le
 * resultat etait honnete mais plat, et surtout <b>different d'un ecran a
 * l'autre</b> : le classement n'avait pas les memes coins que le carnet, qui
 * n'avait pas les memes que la roue. Un mod se reconnait a ca.
 *
 * <h2>Neuf morceaux</h2>
 *
 * <p>Depuis 1.20.5 le jeu sait etirer une texture en gardant ses coins intacts :
 * les quatre coins ne bougent pas, les bords se repetent, le centre se remplit.
 * Une seule petite image donne donc un cadre de n'importe quelle taille avec des
 * angles nets — ce qu'aucun rectangle dessine a la main ne sait faire, et c'est
 * exactement ce qui separe une interface bricolee d'une interface dessinee.
 *
 * <h2>Le mouvement</h2>
 *
 * <p>Un bouton qui change d'etat d'une image a l'autre a l'air d'un interrupteur.
 * Un bouton qui met trois images a s'allumer a l'air d'un bouton. D'ou les
 * fonctions d'adoucissement ici : elles ne servent qu'a ca, et elles changent
 * tout.
 */
public final class Peinture {

	private Peinture() {
	}

	// --- Les sprites ----------------------------------------------------------

	/** Le grand cadre : un ecran entier. */
	public static final ResourceLocation PANNEAU = Compagnon.id("compagnon/panneau");

	/** Une carte : une ligne, une case, un volet. */
	public static final ResourceLocation CARTE = Compagnon.id("compagnon/carte");

	/** La meme, bordee d'or : le premier du classement, ou la tienne. */
	public static final ResourceLocation CARTE_OR = Compagnon.id("compagnon/carte_or");

	public static final ResourceLocation BOUTON = Compagnon.id("compagnon/bouton");
	public static final ResourceLocation BOUTON_SURVOL = Compagnon.id("compagnon/bouton_survol");
	public static final ResourceLocation BOUTON_PRESSE = Compagnon.id("compagnon/bouton_presse");

	// --- Les couleurs communes ------------------------------------------------
	//
	// Une seule couleur d'accent pour tout le mod, et le reste en valeurs du meme
	// violet. C'est ce qui fait qu'un ecran a l'air compose plutot qu'assemble.

	public static final int OR = 0xFFE8C87A;
	public static final int OR_SOMBRE = 0xFF9A7C38;
	public static final int TEXTE = 0xFFF2ECF8;
	public static final int TEXTE_PALE = 0xFFA093BE;
	public static final int VOILE = 0xD0100A18;
	public static final int OMBRE = 0x60000000;

	// --- Les cadres -----------------------------------------------------------

	/** Le grand cadre, avec son ombre portee. */
	public static void panneau(GuiGraphics g, int x, int y, int large, int haut) {
		ombre(g, x, y, large, haut);
		g.blitSprite(PANNEAU, x, y, large, haut);
	}

	/** Une carte. Doree quand elle doit attirer l'oeil. */
	public static void carte(GuiGraphics g, int x, int y, int large, int haut, boolean or) {
		g.blitSprite(or ? CARTE_OR : CARTE, x, y, large, haut);
	}

	/**
	 * Une ombre portee, decalee de deux pixels.
	 *
	 * <p>Sans elle, un panneau a l'air peint sur l'ecran ; avec elle, il a l'air
	 * pose dessus. C'est deux rectangles et ca change la lecture de tout l'ecran.
	 */
	public static void ombre(GuiGraphics g, int x, int y, int large, int haut) {
		g.fill(x + 2, y + haut, x + large + 2, y + haut + 2, OMBRE);
		g.fill(x + large, y + 2, x + large + 2, y + haut, OMBRE);
	}

	/**
	 * Un bouton, avec son etat.
	 *
	 * @param chaleur de zero a un : zero au repos, un survole. Entre les deux, on
	 *                est en train d'y arriver — c'est ce qui fait le mouvement.
	 */
	public static void bouton(GuiGraphics g, int x, int y, int large, int haut,
			float chaleur, boolean presse) {

		if (presse) {
			g.blitSprite(BOUTON_PRESSE, x, y, large, haut);
			return;
		}
		g.blitSprite(BOUTON, x, y, large, haut);
		if (chaleur <= 0.0F) {
			return;
		}
		// L'etat survole se peint PAR-DESSUS, en transparence : c'est ce qui
		// permet d'etre a mi-chemin entre les deux, ce qu'un simple echange de
		// texture ne saurait pas faire.
		RenderSystem.enableBlend();
		g.setColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, chaleur));
		g.blitSprite(BOUTON_SURVOL, x, y, large, haut);
		g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
	}

	/**
	 * Un bouton peint a la main, dans les couleurs qu on lui donne.
	 *
	 * <p>Le bouton en neuf morceaux est violet et dore : parfait sur les ecrans
	 * sombres, deplace sur un parchemin. Coller le meme partout ferait une
	 * interface plus laide, pas plus soignee.
	 *
	 * <p>Les deux partagent donc le <b>mouvement</b> — l allumage progressif, le
	 * leger enfoncement — sans partager la palette. C est le geste qui fait
	 * l unite d une interface, pas la couleur.
	 *
	 * @param chaleur de zero a un : le chemin parcouru vers l etat survole
	 */
	public static void boutonPeint(GuiGraphics g, int x, int y, int large, int haut,
			float chaleur, boolean presse, int fond, int fondChaud, int bord) {

		// Enfonce, il descend d un pixel. C est le seul retour tactile dont on
		// dispose, et il suffit.
		int decale = presse ? 1 : 0;
		int y2 = y + decale;
		int couleur = melanger(fond, fondChaud, adoucir(chaleur));

		// Les coins coupes de deux pixels, comme partout ailleurs dans le mod.
		g.fill(x + 2, y2, x + large - 2, y2 + haut, couleur);
		g.fill(x, y2 + 2, x + large, y2 + haut - 2, couleur);

		g.fill(x + 2, y2, x + large - 2, y2 + 1, bord);
		g.fill(x + 2, y2 + haut - 1, x + large - 2, y2 + haut, bord);
		g.fill(x, y2 + 2, x + 1, y2 + haut - 2, bord);
		g.fill(x + large - 1, y2 + 2, x + large, y2 + haut - 2, bord);
		g.fill(x + 1, y2 + 1, x + 2, y2 + 2, bord);
		g.fill(x + large - 2, y2 + 1, x + large - 1, y2 + 2, bord);
		g.fill(x + 1, y2 + haut - 2, x + 2, y2 + haut - 1, bord);
		g.fill(x + large - 2, y2 + haut - 2, x + large - 1, y2 + haut - 1, bord);

		// Une ligne claire en haut quand il s allume : le bouton prend du relief
		// au moment ou la souris arrive, et le perd quand elle repart.
		if (!presse && chaleur > 0.05F) {
			g.fill(x + 2, y2 + 1, x + large - 2, y2 + 2,
				melanger(0x00FFFFFF, 0x40FFFFFF, adoucir(chaleur)));
		}
	}

	// --- Le mouvement ---------------------------------------------------------

	/**
	 * Fait avancer une valeur vers une cible, image par image.
	 *
	 * <p>{@code vitesse} est la part du chemin parcourue par tick. Un dixieme
	 * donne une demi-seconde pour arriver : assez lent pour qu'on voie le
	 * mouvement, assez rapide pour qu'on ne l'attende pas.
	 *
	 * <p>On multiplie par le temps ecoule depuis la derniere image, faute de quoi
	 * l'animation irait deux fois plus vite a cent-vingt images par seconde qu'a
	 * soixante.
	 */
	public static float vers(float valeur, float cible, float vitesse, float partiel) {
		float pas = Math.min(1.0F, vitesse * Math.max(0.2F, partiel * 3.0F));
		return valeur + (cible - valeur) * pas;
	}

	/**
	 * Adoucit une progression : lente au depart, lente a l'arrivee.
	 *
	 * <p>C'est la difference entre un objet qu'on tire et un objet qui bouge de
	 * lui-meme. Toutes les interfaces qui paraissent soignees font ca quelque
	 * part, et presque aucune ne le dit.
	 */
	public static float adoucir(float part) {
		float t = Math.max(0.0F, Math.min(1.0F, part));
		return t * t * (3.0F - 2.0F * t);
	}

	/** Melange deux couleurs, opacite comprise. */
	public static int melanger(int depuis, int vers, float part) {
		float t = Math.max(0.0F, Math.min(1.0F, part));
		int a = melangerCanal(depuis >>> 24, vers >>> 24, t);
		int r = melangerCanal((depuis >> 16) & 0xFF, (vers >> 16) & 0xFF, t);
		int v = melangerCanal((depuis >> 8) & 0xFF, (vers >> 8) & 0xFF, t);
		int b = melangerCanal(depuis & 0xFF, vers & 0xFF, t);
		return (a << 24) | (r << 16) | (v << 8) | b;
	}

	private static int melangerCanal(int depuis, int vers, float part) {
		return Math.round(depuis + (vers - depuis) * part);
	}

	// --- Les traits -----------------------------------------------------------

	/**
	 * Un filet qui s'efface vers la droite.
	 *
	 * <p>Il separe sans enfermer : c'est ce qu'on utilise a la place d'un cadre
	 * quand deux blocs de texte se suivent.
	 */
	public static void filet(GuiGraphics g, int x, int y, int large) {
		g.fillGradient(x, y, x + large, y + 1, OR_SOMBRE, 0x00000000);
	}

	/**
	 * Une barre de progression, avec son creux.
	 *
	 * @param part de zero a un
	 */
	public static void jauge(GuiGraphics g, int x, int y, int large, int haut, float part,
			int couleur) {

		g.fill(x, y, x + large, y + haut, 0x50000000);
		int rempli = Math.round(large * Math.max(0.0F, Math.min(1.0F, part)));
		if (rempli > 0) {
			g.fill(x, y, x + rempli, y + haut, couleur);
			// Une ligne plus claire en haut du remplissage : la barre cesse d'etre
			// un rectangle et devient un volume.
			g.fill(x, y, x + rempli, y + 1, melanger(couleur, 0xFFFFFFFF, 0.35F));
		}
	}
}
