package fr.lhdp.compagnon.client;

import fr.lhdp.compagnon.fiche.Humeur;
import fr.lhdp.compagnon.reseau.PaquetJauges;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Le petit panneau du coin de l'ecran.
 *
 * <h2>Ce qu'il resout</h2>
 *
 * <p>Savoir si son compagnon a faim demandait d'ouvrir le livre. On ne l'ouvre
 * pas toutes les trente secondes : en pratique, on ne le savait donc jamais, et
 * on s'apercevait de sa faim quand il commencait a reclamer.
 *
 * <h2>Pourquoi il ressemble au livre</h2>
 *
 * <p>La premiere version etait un rectangle sombre a coins droits, avec deux
 * traits pleins dedans. Sur un ciel bleu, ca virait au gris-bleu et ca avait
 * l'air de ce que c'etait : un affichage de mise au point, colle par-dessus le
 * jeu.
 *
 * <p>Celle-ci est un <b>bout de parchemin</b>. Memes couleurs que le livre,
 * meme encre, memes jauges creusees dans le papier, memes teintes pour la faim
 * et l'energie — orange et jaune, exactement comme dans le livre. Le joueur n'a
 * donc rien de neuf a apprendre : il reconnait ses barres au premier coup d'oeil
 * parce qu'il les a deja vues ailleurs.
 *
 * <p>C'est ce qui fait la difference entre un panneau pose sur le jeu et un coin
 * de carnet qui depasse.
 *
 * <h2>Discret, et pour de bon</h2>
 *
 * <ul>
 *   <li>rien du tout quand aucun compagnon n'est dehors ;</li>
 *   <li>rien dans les menus, ni sur une capture sans interface ;</li>
 *   <li><b>il s'efface quand tout va bien</b> et redevient franc des qu'une
 *       barre descend — voir {@link #opacite} ;</li>
 *   <li>une touche le replie, et il reste replie.</li>
 * </ul>
 *
 * <h2>Il ne calcule rien</h2>
 *
 * <p>Le serveur envoie trois nombres deja arrondis, et seulement quand ils ont
 * change. Cette classe ne fait que les dessiner.
 */
public final class Panneau {

	/** Le coin ou il se pose, en pixels depuis le bord. */
	private static final int MARGE = 8;

	private static final int LARGEUR = 96;
	private static final int HAUTEUR = 38;

	/** L'espace entre deux compagnons empiles. */
	private static final int ENTRE_DEUX = 4;

	private static final int PADDING = 6;

	/** La place que prend une icone de la police, espace compris. */
	private static final int ICONE = 10;

	/** La jauge : creusee dans le papier, comme dans le livre. */
	private static final int JAUGE_HAUTEUR = 5;
	private static final int ENTRE_JAUGES = 7;

	// Les couleurs du livre, aux memes valeurs. Elles ne sont pas recopiees par
	// paresse : c'est la seule facon que les deux ecrans se ressemblent vraiment,
	// et qu'une barre orange veuille dire la meme chose des deux cotes.
	private static final int PAPIER_HAUT = 0xFFF3E4C4;
	private static final int PAPIER_BAS = 0xFFE7D3AC;
	private static final int CADRE = 0xFF5A4632;
	private static final int CREUX_HAUT = 0xFFB9A886;
	private static final int CREUX_BAS = 0xFFD9CBAE;
	private static final int ENCRE = 0xFF3A2A18;
	private static final int ENCRE_PALE = 0xFF7A6A55;

	private static final int FAIM = 0xFFD07A2E;
	private static final int ENERGIE = 0xFFD4B02A;
	private static final int ALERTE = 0xFFB03A34;

	/** L'autre bout du battement d'une barre en alerte. */
	private static final int ALERTE_VIF = 0xFFE0574E;

	/** La vitesse du battement. Un aller-retour toutes les deux secondes. */
	private static final float BATTEMENT = 0.08F;

	/**
	 * La longueur d'un aller-retour complet, en images.
	 *
	 * <p>Le compteur du battement est ramene dans cette borne a chaque image.
	 * Sans elle il grandissait d'environ un par image et pour toujours : au
	 * bout de quelques heures de jeu, un nombre flottant de cette taille n'a
	 * plus assez de precision pour decrire un petit pas, et le battement
	 * devient saccade, puis se fige.
	 *
	 * <p>Un tour complet : la borne ne se voit donc pas passer.
	 */
	private static final float TOUR = (float) (2.0 * Math.PI / BATTEMENT);

	/** Le trait de lumiere pose sur le dessus d'une barre pleine. */
	private static final int LUMIERE = 0x40FFFFFF;

	/** L'ombre portee, qui pose le parchemin sur le monde au lieu de le coller. */
	private static final int OMBRE = 0x40000000;

	/** En dessous, la barre passe au rouge et le panneau redevient franc. */
	private static final int SEUIL_ALERTE = 25;

	/** Quand tout va bien, il ne s'impose pas. */
	private static final float OPACITE_AU_REPOS = 0.62F;

	/** Ce que le serveur a envoye en dernier. Vide = rien a dessiner. */
	private static volatile List<PaquetJauges.Jauge> jauges = List.of();

	/**
	 * Ce qui est <b>affiche</b>, par nom de compagnon : faim, energie, opacite.
	 *
	 * <h2>Pourquoi ce n'est pas ce que le serveur a envoye</h2>
	 *
	 * <p>Les barres sautaient. On lui donne a manger, sa faim passe de trente a
	 * cent, et la barre est pleine <b>d'une image a l'autre</b> : on ne voit
	 * rien se passer. Le seul retour qu'on a de son repas, c'est un panneau qui
	 * a change pendant qu'on ne regardait pas.
	 *
	 * <p>La barre glisse maintenant jusqu'a sa nouvelle valeur en une demi-
	 * seconde. Le repas se voit, le sommeil se voit, et l'epuisement d'un vol
	 * trop long se voit descendre. Rien d'autre n'a change : le serveur envoie
	 * toujours aussi peu, et aussi rarement.
	 *
	 * <p>Range par nom : les compagnons entrent et sortent, et une place dans la
	 * liste ne designe pas toujours la meme bete.
	 *
	 * <p>Une table concurrente parce que le menage se fait a l arrivee d un
	 * paquet et le dessin a chaque image. Les deux tombent sur le fil principal
	 * aujourd hui — mais la deconnexion, elle, ne le promet pas, et une table
	 * ordinaire ecrite par deux fils se corrompt en silence.
	 */
	private static final Map<String, float[]> affichees = new ConcurrentHashMap<>();

	/** L'oscillation lente d'une barre en alerte. Avance a chaque image. */
	private static float battement;

	/** Replie par le joueur. Vrai le temps de la session. */
	private static boolean replie;

	private Panneau() {
	}

	public static void poser(List<PaquetJauges.Jauge> nouvelles) {
		jauges = nouvelles;
		// Une bete rentree n'a plus de barre a faire glisser. Sans ce menage, la
		// table grossirait d'une entree par compagnon vu depuis le lancement.
		affichees.keySet().removeIf(nom -> {
			for (PaquetJauges.Jauge jauge : nouvelles) {
				if (jauge.nom().equals(nom)) {
					return false;
				}
			}
			return true;
		});
	}

	/** Le monde a change : on repart de rien plutot que d'afficher du perime. */
	public static void oublier() {
		jauges = List.of();
		affichees.clear();
	}

	public static void basculer() {
		replie = !replie;
	}

	// --- Le dessin ----------------------------------------------------------------

	/**
	 * Dessine le panneau, si tant est qu'il y ait quelque chose a dessiner.
	 *
	 * <p>Les trois premiers tests sont l'essentiel du travail : la plupart du
	 * temps, cette methode sort a la premiere ligne.
	 */
	public static void dessiner(GuiGraphics g, float partiel) {
		List<PaquetJauges.Jauge> montrer = jauges;
		if (replie || montrer.isEmpty()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.options.hideGui || client.screen != null || client.player == null) {
			return;
		}

		// Le battement des barres en alerte. Il tourne meme quand rien n'est en
		// alerte : c'est une addition par image, et ca evite un a-coup au moment
		// ou une barre passe sous le seuil.
		battement = (battement + partiel) % TOUR;

		int y = MARGE;
		for (PaquetJauges.Jauge jauge : montrer) {
			fiche(g, client, MARGE, y, jauge, partiel);
			y += HAUTEUR + ENTRE_DEUX;
		}
	}

	/**
	 * Un compagnon : son nom, sa bouille, ses deux barres.
	 *
	 * <p>L'opacite depend de son etat. Un compagnon qui va bien s'efface a deux
	 * tiers ; des qu'une barre descend, le parchemin redevient franc et la barre
	 * passe au rouge. Le panneau ne demande donc l'attention que lorsqu'il a
	 * quelque chose a dire — c'est la promesse d'un affichage discret, tenue par
	 * le dessin plutot que par une phrase dans la documentation.
	 */
	private static void fiche(GuiGraphics g, Minecraft client, int x, int y,
			PaquetJauges.Jauge jauge, float partiel) {

		float[] etat = affichees.computeIfAbsent(jauge.nom(),
				nom -> new float[]{jauge.faim(), jauge.energie(), opacite(jauge)});

		// Les barres glissent en une demi-seconde ; l'opacite, elle, met une
		// seconde a changer. Un panneau qui s'allume brusquement fait sursauter,
		// et il s'allume au pire moment : quand une barre vient de tomber bas.
		etat[0] = Peinture.vers(etat[0], jauge.faim(), 0.12F, partiel);
		etat[1] = Peinture.vers(etat[1], jauge.energie(), 0.12F, partiel);
		etat[2] = Peinture.vers(etat[2], opacite(jauge), 0.06F, partiel);

		float opacite = etat[2];

		// L'ombre d'abord, decalee d'un pixel : sans elle le parchemin a l'air
		// peint sur l'ecran plutot que pose dessus.
		coinsArrondis(g, x + 1, y + 1, LARGEUR, HAUTEUR, teinte(OMBRE, opacite), 0);

		papier(g, x, y, opacite);

		int texteY = y + PADDING - 1;

		// UNE PATTE DEVANT LE NOM.
		//
		// Dessinee a la meme hauteur et avec la meme methode que le texte : c'est
		// une lettre d'une police a nous, elle s'aligne donc toute seule et prend
		// l'encre du parchemin sans qu'on ait a la teinter a la main.
		g.drawString(client.font, fr.lhdp.compagnon.Icones.de(
				fr.lhdp.compagnon.Icones.PATTE),
			x + PADDING, texteY, teinte(ENCRE_PALE, opacite), false);

		String nom = client.font.plainSubstrByWidth(jauge.nom(),
			LARGEUR - PADDING * 2 - 22 - ICONE);
		g.drawString(client.font, nom, x + PADDING + ICONE, texteY,
			teinte(ENCRE, opacite), false);

		Humeur humeur = humeurDe(jauge.humeur());
		String bouille = humeur.bouille();
		g.drawString(client.font, bouille,
				x + LARGEUR - PADDING - client.font.width(bouille), texteY,
				teinte(ENCRE_PALE, opacite), false);

		int jaugeX = x + PADDING;
		int jaugeL = LARGEUR - PADDING * 2;
		barre(g, jaugeX, y + 20, jaugeL, etat[0], FAIM, opacite);
		barre(g, jaugeX, y + 20 + ENTRE_JAUGES, jaugeL, etat[1], ENERGIE, opacite);
	}

	/** Le parchemin : un degrade chaud, un cadre a l'encre, des coins manquants. */
	private static void papier(GuiGraphics g, int x, int y, float opacite) {
		// Le fond, coins compris — les coins seront ronges juste apres.
		g.fillGradient(x + 1, y, x + LARGEUR - 1, y + HAUTEUR,
				teinte(PAPIER_HAUT, opacite), teinte(PAPIER_BAS, opacite));
		g.fillGradient(x, y + 1, x + LARGEUR, y + HAUTEUR - 1,
				teinte(PAPIER_HAUT, opacite), teinte(PAPIER_BAS, opacite));

		int cadre = teinte(CADRE, opacite);
		g.fill(x + 1, y, x + LARGEUR - 1, y + 1, cadre);
		g.fill(x + 1, y + HAUTEUR - 1, x + LARGEUR - 1, y + HAUTEUR, cadre);
		g.fill(x, y + 1, x + 1, y + HAUTEUR - 1, cadre);
		g.fill(x + LARGEUR - 1, y + 1, x + LARGEUR, y + HAUTEUR - 1, cadre);

		// Un filet clair a l'interieur du cadre, en haut seulement : le papier a
		// l'air legerement bombe, comme une page qui ne touche pas la table.
		g.fill(x + 1, y + 1, x + LARGEUR - 1, y + 2, teinte(0x50FFFFFF, opacite));
	}

	/**
	 * Une jauge creusee dans le papier.
	 *
	 * <p>Le creux va du sombre en haut au clair en bas : c'est ce qui donne
	 * l'illusion d'un sillon. Le remplissage fait l'inverse, avec un trait de
	 * lumiere sur le dessus — il a l'air bombe, donc pose par-dessus.
	 *
	 * <p>Exactement le meme dessin que dans le livre. Deux facons de dessiner la
	 * meme chose, ce serait deux choses.
	 */
	private static void barre(GuiGraphics g, int x, int y, int largeur, float valeur,
			int couleur, float opacite) {

		float borne = Math.max(0.0F, Math.min(100.0F, valeur));
		boolean bas = borne <= SEUIL_ALERTE;

		int cadre = teinte(CADRE, opacite);
		g.fill(x + 1, y, x + largeur - 1, y + 1, cadre);
		g.fill(x + 1, y + JAUGE_HAUTEUR - 1, x + largeur - 1, y + JAUGE_HAUTEUR, cadre);
		g.fill(x, y + 1, x + 1, y + JAUGE_HAUTEUR - 1, cadre);
		g.fill(x + largeur - 1, y + 1, x + largeur, y + JAUGE_HAUTEUR - 1, cadre);

		g.fillGradient(x + 1, y + 1, x + largeur - 1, y + JAUGE_HAUTEUR - 1,
				teinte(CREUX_HAUT, opacite), teinte(CREUX_BAS, opacite));

		int rempli = Math.round((largeur - 2) * borne / 100.0F);
		if (rempli <= 0) {
			return;
		}

		// UNE BARRE EN ALERTE RESPIRE.
		//
		// Tres lentement, et entre deux rouges seulement : c'est assez pour
		// attraper l'oeil du coin de l'ecran, et trop peu pour agacer celui qui
		// a decide d'attendre avant de le nourrir.
		int encre = couleur;
		if (bas) {
			float part = (float) (Math.sin(battement * BATTEMENT) * 0.5 + 0.5);
			encre = Peinture.melanger(ALERTE, ALERTE_VIF, part);
		}
		g.fill(x + 1, y + 1, x + 1 + rempli, y + JAUGE_HAUTEUR - 1,
				teinte(encre, opacite));
		g.fill(x + 1, y + 1, x + 1 + rempli, y + 2, teinte(LUMIERE, opacite));
	}

	/** Un rectangle sans ses quatre pixels de coin. Assez pour arrondir l'oeil. */
	private static void coinsArrondis(GuiGraphics g, int x, int y, int largeur, int hauteur,
			int couleur, int inutilise) {
		g.fill(x + 1, y, x + largeur - 1, y + hauteur, couleur);
		g.fill(x, y + 1, x + largeur, y + hauteur - 1, couleur);
	}

	// --- Les regles d'affichage -------------------------------------------------------

	/**
	 * A quel point il se montre.
	 *
	 * <p>Tout va bien : deux tiers, il se laisse oublier. Une barre sous le quart :
	 * plein, et la barre en question passe au rouge.
	 */
	private static float opacite(PaquetJauges.Jauge jauge) {
		// MONTE, IL RESTE FRANC.
		//
		// Le panneau s'efface quand tout va bien, et c'est ce qui le rend
		// supportable. Mais quand on vole a cent blocs du sol sur son dos, son
		// energie est la seule chose qui compte — c'est le pire moment pour la
		// rendre discrete.
		if (jauge.monte()) {
			return 1.0F;
		}
		boolean alerte = jauge.faim() <= SEUIL_ALERTE || jauge.energie() <= SEUIL_ALERTE;
		return alerte ? 1.0F : OPACITE_AU_REPOS;
	}

	/** La meme couleur, moins presente. Le canal alpha est multiplie, rien d'autre. */
	private static int teinte(int couleur, float facteur) {
		int alpha = (couleur >>> 24) & 0xFF;
		int nouveau = Math.max(0, Math.min(255, Math.round(alpha * facteur)));
		return (nouveau << 24) | (couleur & 0x00FFFFFF);
	}

	/** L'humeur envoyee par son numero. Une valeur inconnue retombe au milieu. */
	private static Humeur humeurDe(int numero) {
		Humeur[] toutes = Humeur.values();
		return numero >= 0 && numero < toutes.length ? toutes[numero] : Humeur.MOYEN;
	}

	/** Ce qu'on dit au joueur quand il replie ou deplie le panneau. */
	public static Component motDeLEtat() {
		return Component.translatable(replie
				? "panneau.compagnon.replie"
				: "panneau.compagnon.deplie");
	}
}
