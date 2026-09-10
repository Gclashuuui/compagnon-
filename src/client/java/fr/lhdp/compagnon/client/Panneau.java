package fr.lhdp.compagnon.client;

import fr.lhdp.compagnon.fiche.Humeur;
import fr.lhdp.compagnon.reseau.PaquetJauges;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Le HUD RP minimal du compagnon, posé dans le coin supérieur gauche.
 *
 * <p>Il montre les quatre besoins sans masquer la scène : 112 × 38 pixels,
 * fond sombre translucide et pictogrammes dessinés à taille native. Les grands
 * décors des carnets restent réservés à la fiche volontairement ouverte avec H.
 * Le panneau s'efface lorsque tout va bien et redevient opaque en cas d'alerte.
 */
public final class Panneau {

	/** Le coin ou il se pose, en pixels depuis le bord. */
	private static final int MARGE = 8;

	static final int LARGEUR = 112;
	static final int HAUTEUR = 38;

	/** L'espace entre deux compagnons empiles. */
	private static final int ENTRE_DEUX = 4;

	/** Hauteur native des petites barres, sans redimensionnement de texture. */
	private static final int JAUGE_HAUTEUR = 4;

	// Les couleurs du livre, aux memes valeurs. Elles ne sont pas recopiees par
	// paresse : c'est la seule facon que les deux ecrans se ressemblent vraiment,
	// et qu'une barre orange veuille dire la meme chose des deux cotes.
	private static final int FAIM = 0xFFEBA337;
	private static final int ENERGIE = 0xFF37C4EB;
	private static final int SANTE = 0xFF8BC458;
	private static final int COMPLICITE = 0xFFE16EAC;
	private static final int ALERTE = 0xFFD94F50;

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

	/** En dessous, la barre passe au rouge et le panneau redevient franc. */
	private static final int SEUIL_ALERTE = 25;

	/** Quand tout va bien, il ne s'impose pas. */
	private static final float OPACITE_AU_REPOS = 0.62F;

	/** Ce que le serveur a envoye en dernier. Vide = rien a dessiner. */
	private static volatile List<PaquetJauges.Jauge> jauges = List.of();

	/**
	 * Ce qui est <b>affiche</b>, par identifiant de compagnon : faim, energie,
	 * sante, complicite, opacite.
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
	 * <p>Range par UUID : les noms peuvent changer et deux compagnons peuvent
	 * porter le meme. L'identifiant de fiche, lui, ne bouge jamais.
	 *
	 * <p>Une table concurrente parce que le menage se fait a l arrivee d un
	 * paquet et le dessin a chaque image. Les deux tombent sur le fil principal
	 * aujourd hui — mais la deconnexion, elle, ne le promet pas, et une table
	 * ordinaire ecrite par deux fils se corrompt en silence.
	 */
	private static final Map<UUID, float[]> affichees = new ConcurrentHashMap<>();

	/** Le style est purement client et ne produit aucun paquet reseau. */
	private static ThemeSante theme = ThemeSante.charger();

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
		affichees.keySet().removeIf(id -> {
			for (PaquetJauges.Jauge jauge : nouvelles) {
				if (jauge.id().equals(id)) {
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

	static boolean replie() {
		return replie;
	}

	static List<PaquetJauges.Jauge> donnees() {
		return jauges;
	}

	static ThemeSante theme() {
		return theme;
	}

	static void changerTheme(boolean precedent) {
		theme = precedent ? theme.precedent() : theme.suivant();
		theme.sauvegarder();
	}

	static void choisirTheme(ThemeSante nouveau) {
		theme = nouveau;
		theme.sauvegarder();
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

		int x = MARGE;
		int y = MARGE;
		for (PaquetJauges.Jauge jauge : montrer) {
			fiche(g, client, x, y, jauge, partiel);
			y += HAUTEUR + ENTRE_DEUX;
		}
	}

	/**
	 * Un compagnon : son nom, sa bouille, son diagnostic et ses quatre barres.
	 *
	 * <p>L'opacite depend de son etat. Un compagnon qui va bien s'efface a deux
	 * tiers ; des qu'une barre descend, le parchemin redevient franc et la barre
	 * passe au rouge. Le panneau ne demande donc l'attention que lorsqu'il a
	 * quelque chose a dire — c'est la promesse d'un affichage discret, tenue par
	 * le dessin plutot que par une phrase dans la documentation.
	 */
	private static void fiche(GuiGraphics g, Minecraft client, int x, int y,
			PaquetJauges.Jauge jauge, float partiel) {

		float[] etat = affichees.computeIfAbsent(jauge.id(),
				id -> new float[]{jauge.faim(), jauge.energie(), jauge.sante(),
						jauge.complicite(), opacite(jauge)});

		// Les barres glissent en une demi-seconde ; l'opacite, elle, met une
		// seconde a changer. Un panneau qui s'allume brusquement fait sursauter,
		// et il s'allume au pire moment : quand une barre vient de tomber bas.
		etat[0] = Peinture.vers(etat[0], jauge.faim(), 0.12F, partiel);
		etat[1] = Peinture.vers(etat[1], jauge.energie(), 0.12F, partiel);
		etat[2] = Peinture.vers(etat[2], jauge.sante(), 0.12F, partiel);
		etat[3] = Peinture.vers(etat[3], jauge.complicite(), 0.12F, partiel);
		etat[4] = Peinture.vers(etat[4], opacite(jauge), 0.06F, partiel);

		float opacite = etat[4];

		dessinerFondMinimal(g, x, y, jauge, opacite);

		String nom = client.font.plainSubstrByWidth(jauge.nom(), LARGEUR - 18);
		g.drawString(client.font, nom, x + 10, y + 3,
				teinte(0xFFF4F1EA, opacite), false);

		petiteBarreMinimaliste(g, x + 5, y + 16, 0, etat[0], FAIM, opacite);
		petiteBarreMinimaliste(g, x + 58, y + 16, 1, etat[1], ENERGIE, opacite);
		petiteBarreMinimaliste(g, x + 5, y + 27, 2, etat[2], SANTE, opacite);
		petiteBarreMinimaliste(g, x + 58, y + 27, 3, etat[3], COMPLICITE, opacite);
	}

	private static void dessinerFondMinimal(GuiGraphics g, int x, int y,
			PaquetJauges.Jauge jauge, float opacite) {
		int fond = teinte(0xCC111318, opacite);
		int bord = teinte(0xAAE8E4DA, opacite * 0.55F);
		g.fill(x + 1, y, x + LARGEUR - 1, y + HAUTEUR, fond);
		g.fill(x, y + 1, x + LARGEUR, y + HAUTEUR - 1, fond);
		g.renderOutline(x, y, LARGEUR, HAUTEUR, bord);
		g.fill(x, y + 2, x + 2, y + HAUTEUR - 2,
				teinte(couleurEtat(jauge), opacite));
	}

	private static void petiteBarreMinimaliste(GuiGraphics g, int x, int y,
			int icone, float valeur, int couleur, float opacite) {
		dessinerIconeNette(g, x, y - 2, icone, teinte(couleur, opacite));
		int bx = x + 10;
		int largeur = 38;
		int rempli = Math.round((largeur - 2) * Math.max(0.0F,
				Math.min(100.0F, valeur)) / 100.0F);
		g.fill(bx, y, bx + largeur, y + JAUGE_HAUTEUR,
				teinte(0xAA000000, opacite));
		g.renderOutline(bx, y, largeur, JAUGE_HAUTEUR,
				teinte(0x668F949E, opacite));
		if (rempli > 0) {
			int teinte = couleur;
			if (valeur <= SEUIL_ALERTE) {
				float part = (float) (Math.sin(battement * BATTEMENT) * 0.5 + 0.5);
				teinte = Peinture.melanger(ALERTE, ALERTE_VIF, part);
			}
			g.fill(bx + 1, y + 1, bx + 1 + rempli, y + JAUGE_HAUTEUR - 1,
					teinte(teinte, opacite));
		}
	}

	/** Quatre pictogrammes dessinés pixel par pixel : aucun redimensionnement flou. */
	private static void dessinerIconeNette(GuiGraphics g, int x, int y,
			int icone, int couleur) {
		switch (icone) {
			case 0 -> { // nourriture : morceau + os
				g.fill(x + 1, y + 2, x + 5, y + 7, couleur);
				g.fill(x + 4, y + 5, x + 7, y + 7, couleur);
				g.fill(x + 6, y + 4, x + 8, y + 8, couleur);
			}
			case 1 -> { // énergie : éclair
				g.fill(x + 4, y, x + 8, y + 3, couleur);
				g.fill(x + 2, y + 3, x + 6, y + 5, couleur);
				g.fill(x + 4, y + 5, x + 6, y + 8, couleur);
			}
			case 2 -> { // santé : croix
				g.fill(x + 3, y + 1, x + 6, y + 8, couleur);
				g.fill(x + 1, y + 3, x + 8, y + 6, couleur);
			}
			default -> { // complicité : cœur
				g.fill(x + 1, y + 2, x + 4, y + 5, couleur);
				g.fill(x + 5, y + 2, x + 8, y + 5, couleur);
				g.fill(x + 2, y + 4, x + 7, y + 7, couleur);
				g.fill(x + 3, y + 7, x + 6, y + 8, couleur);
			}
		}
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
		boolean alerte = jauge.faim() <= SEUIL_ALERTE
				|| jauge.energie() <= SEUIL_ALERTE
				|| jauge.sante() <= SEUIL_ALERTE
				|| jauge.complicite() <= SEUIL_ALERTE || jauge.bobo();
		return alerte ? 1.0F : OPACITE_AU_REPOS;
	}

	/** Un diagnostic court, partage par le HUD et sa fiche detaillee. */
	static Component etat(PaquetJauges.Jauge jauge) {
		if (jauge.bobo() || jauge.sante() <= SEUIL_ALERTE) {
			return Component.translatable("panneau.compagnon.etat.soin");
		}
		if (jauge.faim() <= SEUIL_ALERTE) {
			return Component.translatable("panneau.compagnon.etat.affame");
		}
		if (jauge.energie() <= SEUIL_ALERTE) {
			return Component.translatable("panneau.compagnon.etat.epuise");
		}
		if (jauge.complicite() <= SEUIL_ALERTE) {
			return Component.translatable("panneau.compagnon.etat.distant");
		}
		if (minimum(jauge) < 50) {
			return Component.translatable("panneau.compagnon.etat.surveiller");
		}
		return Component.translatable(jauge.humeur() >= Humeur.CONTENT.ordinal()
				? "panneau.compagnon.etat.heureux"
				: "panneau.compagnon.etat.calme");
	}

	static Component conseil(PaquetJauges.Jauge jauge) {
		if (jauge.bobo() || jauge.sante() <= SEUIL_ALERTE) {
			return Component.translatable("panneau.compagnon.conseil.soin");
		}
		if (jauge.faim() <= SEUIL_ALERTE) {
			return Component.translatable("panneau.compagnon.conseil.faim");
		}
		if (jauge.energie() <= SEUIL_ALERTE) {
			return Component.translatable("panneau.compagnon.conseil.energie");
		}
		if (jauge.complicite() <= SEUIL_ALERTE) {
			return Component.translatable("panneau.compagnon.conseil.complicite");
		}
		return Component.translatable("panneau.compagnon.conseil.bien");
	}

	private static int minimum(PaquetJauges.Jauge jauge) {
		return Math.min(Math.min(jauge.faim(), jauge.energie()),
				Math.min(jauge.sante(), jauge.complicite()));
	}

	static int couleurEtat(PaquetJauges.Jauge jauge) {
		return (jauge.bobo() || minimum(jauge) <= SEUIL_ALERTE) ? ALERTE : theme.accent;
	}

	/** La meme couleur, moins presente. Le canal alpha est multiplie, rien d'autre. */
	private static int teinte(int couleur, float facteur) {
		int alpha = (couleur >>> 24) & 0xFF;
		int nouveau = Math.max(0, Math.min(255, Math.round(alpha * facteur)));
		return (nouveau << 24) | (couleur & 0x00FFFFFF);
	}

	/** Ce qu'on dit au joueur quand il replie ou deplie le panneau. */
	public static Component motDeLEtat() {
		return Component.translatable(replie
				? "panneau.compagnon.replie"
				: "panneau.compagnon.deplie");
	}
}
