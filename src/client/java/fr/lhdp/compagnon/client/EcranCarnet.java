package fr.lhdp.compagnon.client;

import fr.lhdp.compagnon.livre.EntreeCarnet;
import fr.lhdp.compagnon.reseau.PaquetInvoquer;
import fr.lhdp.compagnon.reseau.PaquetLivre;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Le carnet : tous ses compagnons, et lequel est dehors.
 *
 * <h2>A quoi il sert</h2>
 *
 * <p>Un joueur qui a plusieurs betes n'en promene qu'une. Il lui faut donc un
 * endroit pour voir lesquelles il a, les regarder, et decider laquelle
 * l'accompagne. C'est tout ce que fait cet ecran : <b>choisir</b>. Ce qu'un
 * compagnon mange, ce dont il souffre et ce dont il se souvient est dans le
 * livre, et n'a rien a faire ici.
 *
 * <h2>Ce qu'il ne decide pas</h2>
 *
 * <p>Rien. Le bouton envoie une intention et attend. C'est le serveur qui trouve
 * une place libre, qui range un autre compagnon s'il en faut, et qui renvoie la
 * liste a jour — que cet ecran se contente de reafficher. Un client modifie ne
 * peut donc pas se faire apparaitre dix creatures.
 *
 * <h2>Pourquoi la liste entiere est envoyee d'un coup</h2>
 *
 * <p>Tourner une page ne coute alors aucun aller-retour : c'est instantane, et
 * c'est exactement ce qu'on fait sans arret quand on cherche lequel sortir. Un
 * joueur a quelques compagnons, pas mille — le paquet reste minuscule.
 */
public class EcranCarnet extends EcranCompagnon {

	private ResourceLocation FOND;
	private ResourceLocation SALISSURES;
	private ResourceLocation CADRE_PORTRAIT;
	private ResourceLocation SOULIGNEMENT;
	private ResourceLocation FLECHE_GAUCHE;
	private ResourceLocation FLECHE_GAUCHE_SURVOL;
	private ResourceLocation FLECHE_DROITE;
	private ResourceLocation FLECHE_DROITE_SURVOL;
	private ResourceLocation PAGE_TOURNE_RTL;
	private ResourceLocation PAGE_TOURNE_LTR;

	/** Le livre fait 384 x 256 et non 256 : il faut l'appel long de {@code blit}. */
	private static final int LARGEUR = 384;
	private static final int HAUTEUR = 256;

	private static final int FLECHE_LARGEUR = 29;
	private static final int FLECHE_HAUTEUR = 28;

	private static final int PORTRAIT_SOURCE_L = 142;
	private static final int PORTRAIT_SOURCE_H = 76;
	private static final int SOULIGNEMENT_SOURCE_L = 159;
	private static final int SOULIGNEMENT_SOURCE_H = 11;

	// Les deux pages, en coordonnees relatives au coin du livre. Mêmes valeurs que
	// dans le livre : les deux ecrans doivent se superposer exactement quand on
	// passe de l'un a l'autre, sinon la reliure semble bouger.
	private static final int PAGE_GAUCHE_X = 34;
	private static final int PAGE_DROITE_X = 208;
	private static final int PAGE_LARGEUR = 142;
	private static final int PAGE_Y = 28;

	private int ENCRE;
	private int ENCRE_PALE;
	private int ENCRE_VERTE;
	private int CADRE;
	private int CREUX_HAUT;

	/** Le cadre du modele, en pixels. Assez grand pour voir la bete. */
	private static final int PORTRAIT = 76;
	private static final int THEME_TAILLE = 20;
	private static final long PAGE_IMAGE_MS = 110L;
	private static final long PAGE_ANIMATION_MS = PAGE_IMAGE_MS * 8L;

	/** Hauteur d'une carte de la collection. */
	private static final int LIGNE = 30;

	/** Combien de compagnons par page de liste. */
	private static final int PAR_PAGE = 5;

	private static final int BOUTON_LARGEUR = 104;
	private static final int BOUTON_HAUTEUR = 18;

	private List<EntreeCarnet> entrees;
	private int choisi;

	private int gauche;
	private int haut;

	/** La page de la <b>liste</b>, a droite. Rien a voir avec le compagnon choisi. */
	private int page;
	private int pageCible = -1;
	private int sensPage = 1;
	private long debutTournePage;
	private ThemeLivre theme;
	private boolean themeSurvole;

	/** De zero a un : le bouton est en train de s allumer. */
	private float chaleurDuBouton;

	/** Une animation par carte : celle qu'on quitte refroidit sans sauter. */
	private float[] chaleurDesCartes = new float[0];

	/** Le modele glisse doucement quand on choisit une autre bete. */
	private float arriveePortrait;

	public EcranCarnet(int choisi, List<EntreeCarnet> entrees) {
		super(Component.translatable("carnet.compagnon.titre"));
		this.entrees = entrees;
		this.choisi = borner(choisi, entrees.size());
		this.page = this.choisi / PAR_PAGE;
		appliquerTheme(ThemeLivre.charger());
	}

	private void appliquerTheme(ThemeLivre nouveau) {
		this.theme = nouveau;
		this.FOND = nouveau.texture("book_astra_v2");
		this.SALISSURES = nouveau.texture("book_details_overlay");
		this.CADRE_PORTRAIT = nouveau.texture("portrait_frame");
		this.SOULIGNEMENT = nouveau.texture("underline_astra");
		this.FLECHE_GAUCHE = nouveau.texture("page_turn_left_normal");
		this.FLECHE_GAUCHE_SURVOL = nouveau.texture("page_turn_left_hover");
		this.FLECHE_DROITE = nouveau.texture("page_turn_right_normal");
		this.FLECHE_DROITE_SURVOL = nouveau.texture("page_turn_right_hover");
		this.PAGE_TOURNE_RTL = nouveau.texture("page_turn_rtl_strip");
		this.PAGE_TOURNE_LTR = nouveau.texture("page_turn_ltr_strip");
		this.ENCRE = nouveau.encre;
		this.ENCRE_PALE = nouveau.encrePale;
		this.ENCRE_VERTE = nouveau.encreVerte;
		this.CADRE = nouveau.cadre;
		this.CREUX_HAUT = nouveau.creuxHaut;
	}

	/**
	 * Le serveur a renvoye la liste apres une invocation.
	 *
	 * <p>On remplace les donnees <b>sans rouvrir l'ecran</b> : rouvrir remettrait
	 * la liste a sa premiere page et ferait clignoter le modele. Le joueur vient
	 * de cliquer, il ne doit rien voir bouger d'autre que ce qu'il a change.
	 */
	public void mettreAJour(int choisi, List<EntreeCarnet> entrees) {
		this.entrees = entrees;
		this.choisi = borner(choisi, entrees.size());
		this.pageCible = -1;
		this.page = Math.min(this.page, pagesMaximum() - 1);
		if (this.page < 0) {
			this.page = 0;
		}
	}

	private static int borner(int valeur, int combien) {
		return combien <= 0 ? 0 : Math.floorMod(valeur, combien);
	}

	private int pagesMaximum() {
		return Math.max(1, (this.entrees.size() + PAR_PAGE - 1) / PAR_PAGE);
	}

	private EntreeCarnet courant() {
		return this.entrees.isEmpty() ? null : this.entrees.get(this.choisi);
	}

	@Override
	protected void init() {
		this.gauche = (this.width - LARGEUR) / 2;
		this.haut = (this.height - HAUTEUR) / 2;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// --- Le dessin ------------------------------------------------------------------

	@Override
	public void render(GuiGraphics g, int sourisX, int sourisY, float partiel) {
		super.render(g, sourisX, sourisY, partiel);

		if (this.theme.hauteDefinition()) {
			HabillageLivreHD.dessiner(g, this.theme, this.gauche, this.haut,
					this.width, this.height);
		} else {
			g.blit(FOND, this.gauche, this.haut, 0.0F, 0.0F,
					LARGEUR, HAUTEUR, LARGEUR, HAUTEUR);
			g.blit(SALISSURES, this.gauche, this.haut, 0.0F, 0.0F,
					LARGEUR, HAUTEUR, LARGEUR, HAUTEUR);
		}

		mettreAJourAnimationPage();
		this.themeSurvole = false;
		this.arriveePortrait = Peinture.vers(this.arriveePortrait, 1.0F, 0.25F, partiel);
		pageDuChoisi(g, sourisX, sourisY);
		pageDeLaListe(g, sourisX, sourisY, partiel);
		dessinerAnimationPage(g);
		selecteurTheme(g, sourisX, sourisY);
		clochette(g, sourisX, sourisY, partiel);
		if (this.themeSurvole) {
			g.renderTooltip(this.font, Component.translatable("livre.compagnon.theme",
					Component.translatable(this.theme.traduction())), sourisX, sourisY);
		}
	}

	/** A gauche : celui qu'on regarde, en grand, et le bouton. */
	private void pageDuChoisi(GuiGraphics g, int sourisX, int sourisY) {
		EntreeCarnet entree = courant();
		if (entree == null) {
			return;
		}

		int x = this.gauche + PAGE_GAUCHE_X;
		int y = this.haut + PAGE_Y;

		titre(g, x, y, entree.nom());

		int cadreX = x;
		int cadreY = y + 20;
		portrait(g, entree, cadreX, cadreY, sourisX, sourisY);

		int sousLeCadre = cadreY + PORTRAIT + 8;

		// Le nom d'espece vient du dossier, donc en minuscules. Une majuscule ne
		// change rien au code et beaucoup a la page.
		String espece = majuscule(entree.espece());
		g.drawString(this.font, espece, x + centrer(espece), sousLeCadre, ENCRE_PALE, false);

		// SA PROPRE CLE, et non celle du livre.
		//
		// Celle du livre s'ecrit « Niveau %s / %s » : elle attend le niveau ET le
		// maximum. Le carnet n'envoie que le niveau, et le second trou restait
		// affiche tel quel — « Niveau 50 / %s » en plein milieu de la page.
		String niveau = Component.translatable("carnet.compagnon.niveau",
				entree.niveau()).getString();
		g.drawString(this.font, niveau, x + centrer(niveau), sousLeCadre + 12, ENCRE, false);

		// OU IL EST, et non plus seulement s'il est sorti.
		//
		// « Il est range » ou « il est avec toi », et rien entre les deux — alors
		// qu'entre les deux se trouve le cas le plus frequent : il est dehors,
		// quelque part. Le serveur envoie maintenant la phrase entiere.
		String etat = entree.ou().isEmpty()
				? Component.translatable(entree.sorti()
						? "carnet.compagnon.dehors"
						: "carnet.compagnon.range").getString()
				: entree.ou();
		String coupe = this.font.plainSubstrByWidth(etat, PAGE_LARGEUR);
		g.drawString(this.font, coupe, x + centrer(coupe), sousLeCadre + 26,
				entree.sorti() ? ENCRE_VERTE : ENCRE_PALE, false);

		bouton(g, boutonX(), boutonY(), entree.sorti(), sourisX, sourisY);
	}

	/** A droite : tous les autres, et les fleches. */
	private void pageDeLaListe(GuiGraphics g, int sourisX, int sourisY, float partiel) {
		int x = this.gauche + PAGE_DROITE_X;
		int y = this.haut + PAGE_Y;

		String titre = Component.translatable("carnet.compagnon.tous", this.entrees.size()).getString();
		titre(g, x, y, titre);

		int premier = this.page * PAR_PAGE;
		for (int i = 0; i < PAR_PAGE; i++) {
			int position = premier + i;
			if (position >= this.entrees.size()) {
				break;
			}
			ligne(g, x, y + 22 + i * LIGNE, position, sourisX, sourisY, partiel);
		}

		if (pagesMaximum() > 1) {
			int flecheY = this.haut + 207;
			if (this.page > 0) {
				int flecheX = this.gauche + 22;
				ResourceLocation texture = dansLaBoite(sourisX, sourisY, flecheX, flecheY,
						FLECHE_LARGEUR, FLECHE_HAUTEUR)
						? this.FLECHE_GAUCHE_SURVOL : this.FLECHE_GAUCHE;
				g.blit(texture, flecheX, flecheY, 0.0F, 0.0F,
						FLECHE_LARGEUR, FLECHE_HAUTEUR, FLECHE_LARGEUR, FLECHE_HAUTEUR);
			}
			if (this.page < pagesMaximum() - 1) {
				int flecheX = this.gauche + 333;
				ResourceLocation texture = dansLaBoite(sourisX, sourisY, flecheX, flecheY,
						FLECHE_LARGEUR, FLECHE_HAUTEUR)
						? this.FLECHE_DROITE_SURVOL : this.FLECHE_DROITE;
				g.blit(texture, flecheX, flecheY, 0.0F, 0.0F,
						FLECHE_LARGEUR, FLECHE_HAUTEUR, FLECHE_LARGEUR, FLECHE_HAUTEUR);
			}
			String compte = (this.page + 1) + " / " + pagesMaximum();
			g.drawString(this.font, compte,
					x + (PAGE_LARGEUR - this.font.width(compte)) / 2, flecheY + 10,
					ENCRE_PALE, false);
		}
	}

	/** Une carte de collection : nom, espece, niveau et presence dans le monde. */
	private void ligne(GuiGraphics g, int x, int y, int position, int sourisX, int sourisY,
			float partiel) {
		EntreeCarnet entree = this.entrees.get(position);
		boolean choisie = position == this.choisi;
		boolean survolee = dansLaBoite(sourisX, sourisY, x - 2, y - 2,
				PAGE_LARGEUR + 2, LIGNE - 3);
		if (this.chaleurDesCartes.length != this.entrees.size()) {
			this.chaleurDesCartes = new float[this.entrees.size()];
		}
		this.chaleurDesCartes[position] = Peinture.vers(
				this.chaleurDesCartes[position], survolee ? 1.0F : 0.0F, 0.30F, partiel);
		Peinture.boutonPeint(g, x - 2, y - 2, PAGE_LARGEUR + 2, LIGNE - 3,
				this.chaleurDesCartes[position], false,
					choisie ? 0x223F6B37 : 0x10000000,
					choisie ? 0x334F8246 : 0x26000000,
					choisie ? ENCRE_VERTE : CREUX_HAUT);

		// Pleine s'il est dehors, creuse s'il est range : on lit la colonne d'un
		// coup d'oeil sans avoir a lire les mots.
		pastille(g, x + 4, y + 8, entree.sorti());

		int couleur = choisie ? ENCRE : ENCRE_PALE;
		String nom = this.font.plainSubstrByWidth(entree.nom(), PAGE_LARGEUR - 23);
		g.drawString(this.font, nom, x + 16, y + 3, couleur, false);

		String detail = Component.translatable("carnet.compagnon.carte_detail",
				majuscule(entree.espece()), entree.niveau()).getString();
		detail = this.font.plainSubstrByWidth(detail, PAGE_LARGEUR - 23);
		g.drawString(this.font, detail, x + 16, y + 15, ENCRE_PALE, false);
	}

	private void pastille(GuiGraphics g, int x, int y, boolean pleine) {
		int couleur = pleine ? ENCRE_VERTE : ENCRE_PALE;
		g.fill(x + 1, y, x + 5, y + 1, couleur);
		g.fill(x, y + 1, x + 6, y + 5, pleine ? couleur : 0);
		g.fill(x, y + 1, x + 1, y + 5, couleur);
		g.fill(x + 5, y + 1, x + 6, y + 5, couleur);
		g.fill(x + 1, y + 5, x + 5, y + 6, couleur);
	}

	private void titre(GuiGraphics g, int x, int y, String texte) {
		String coupe = this.font.plainSubstrByWidth(texte, PAGE_LARGEUR);
		g.drawString(this.font, coupe, x + (PAGE_LARGEUR - this.font.width(coupe)) / 2, y,
				ENCRE, false);
		g.blit(SOULIGNEMENT, x, y + 10, PAGE_LARGEUR, 6,
				0.0F, 0.0F, SOULIGNEMENT_SOURCE_L, SOULIGNEMENT_SOURCE_H,
				SOULIGNEMENT_SOURCE_L, SOULIGNEMENT_SOURCE_H);
	}

	/** Premiere lettre en majuscule, sans rien casser d'autre. */
	private static String majuscule(String mot) {
		if (mot == null || mot.isEmpty()) {
			return "";
		}
		return Character.toUpperCase(mot.charAt(0)) + mot.substring(1);
	}

	private int centrer(String texte) {
		return (PAGE_LARGEUR - this.font.width(texte)) / 2;
	}

	/**
	 * Le modele en trois dimensions.
	 *
	 * <p>L'entite n'est jamais ajoutee au monde : elle n'existe que le temps du
	 * dessin. C'est ce qui permet de montrer un compagnon <b>range</b>, qui par
	 * definition n'a pas d'entite nulle part.
	 *
	 * <p>Si le rendu echoue — un modele absent, une texture manquante — on
	 * abandonne l'apercu pour de bon plutot que de relancer l'erreur soixante fois
	 * par seconde. Le reste du carnet continue de marcher.
	 */
	private void portrait(GuiGraphics g, EntreeCarnet entree, int x, int y,
			int sourisX, int sourisY) {

		g.blit(CADRE_PORTRAIT, x, y, PAGE_LARGEUR, PORTRAIT,
				0.0F, 0.0F, PORTRAIT_SOURCE_L, PORTRAIT_SOURCE_H,
				PORTRAIT_SOURCE_L, PORTRAIT_SOURCE_H);

		int marge = 8;
		int glissement = Math.round((1.0F - Peinture.adoucir(this.arriveePortrait)) * 8.0F);
		Apercu.dessiner(g, x + marge, y + marge + glissement,
				PAGE_LARGEUR - marge * 2, PORTRAIT - marge * 2 - glissement,
				entree.espece(), entree.variante(), sourisX, sourisY);
	}

	// --- Le bouton --------------------------------------------------------------------

	private int boutonX() {
		return this.gauche + PAGE_GAUCHE_X + (PAGE_LARGEUR - BOUTON_LARGEUR) / 2;
	}

	private int boutonY() {
		// Assez bas pour laisser respirer la ligne d'etat juste au-dessus, assez
		// haut pour rester dans le parchemin et pas sur la reliure.
		return this.haut + HAUTEUR - 46;
	}

	/**
	 * Dessine a la main plutot qu'avec un bouton du jeu.
	 *
	 * <p>Un bouton gris de Minecraft au milieu d'un parchemin casserait tout. Il
	 * n'y a rien de plus ici qu'un cadre, un fond et un mot centre.
	 */
	private void bouton(GuiGraphics g, int x, int y, boolean sorti, int sourisX, int sourisY) {
		boolean survole = dansLaBoite(sourisX, sourisY, x, y, BOUTON_LARGEUR, BOUTON_HAUTEUR);

		// LE BOUTON S ALLUME, il ne bascule pas.
		//
		// Il changeait de teinte d une image a l autre, ce qui a l air d un
		// interrupteur. Trois images pour s allumer, et il a l air d un bouton.
		// Les coins coupes viennent de la meme boite a outils que le reste du mod.
		this.chaleurDuBouton = Peinture.vers(this.chaleurDuBouton,
			survole ? 1.0F : 0.0F, 0.30F, 1.0F);
		Peinture.boutonPeint(g, x, y, BOUTON_LARGEUR, BOUTON_HAUTEUR,
			this.chaleurDuBouton, false, 0x1A000000, 0x3D000000, CADRE);

		String texte = Component.translatable(sorti
				? "carnet.compagnon.ranger"
				: "carnet.compagnon.invoquer").getString();
		g.drawString(this.font, texte,
				x + (BOUTON_LARGEUR - this.font.width(texte)) / 2,
				y + (BOUTON_HAUTEUR - 8) / 2,
				Peinture.melanger(ENCRE_PALE, ENCRE, Peinture.adoucir(this.chaleurDuBouton)),
			false);
	}

	private static boolean dansLaBoite(double sourisX, double sourisY,
			int x, int y, int largeur, int hauteur) {
		return sourisX >= x && sourisX < x + largeur && sourisY >= y && sourisY < y + hauteur;
	}

	private boolean pageEnMouvement() {
		return this.pageCible >= 0;
	}

	private void allerPageListe(int nouvelle) {
		int bornee = Math.max(0, Math.min(pagesMaximum() - 1, nouvelle));
		if (bornee == this.page || pageEnMouvement()) {
			return;
		}
		this.sensPage = bornee > this.page ? 1 : -1;
		this.pageCible = bornee;
		this.debutTournePage = System.currentTimeMillis();
		Bruits.page();
	}

	private void mettreAJourAnimationPage() {
		if (!pageEnMouvement()) {
			return;
		}
		long ecoule = System.currentTimeMillis() - this.debutTournePage;
		if (ecoule >= PAGE_ANIMATION_MS / 2L) {
			this.page = this.pageCible;
		}
		if (ecoule >= PAGE_ANIMATION_MS) {
			this.page = this.pageCible;
			this.pageCible = -1;
		}
	}

	private void dessinerAnimationPage(GuiGraphics g) {
		if (!pageEnMouvement()) {
			return;
		}
		long ecoule = Math.max(0L, System.currentTimeMillis() - this.debutTournePage);
		int image = Math.min(7, (int) (ecoule / PAGE_IMAGE_MS));
		ResourceLocation bande = this.sensPage > 0 ? this.PAGE_TOURNE_RTL : this.PAGE_TOURNE_LTR;
		g.blit(bande, this.gauche, this.haut, image * LARGEUR, 0.0F,
				LARGEUR, HAUTEUR, LARGEUR * 8, HAUTEUR);
	}

	private void selecteurTheme(GuiGraphics g, int sourisX, int sourisY) {
		int x = themeX();
		int y = this.haut + 43;
		this.themeSurvole = dansLaBoite(sourisX, sourisY, x, y, THEME_TAILLE, THEME_TAILLE);
		Peinture.boutonPeint(g, x, y, THEME_TAILLE, THEME_TAILLE,
				this.themeSurvole ? 1.0F : 0.0F, false,
				this.theme.ongletFond, this.theme.ongletFondActif, this.theme.ongletBord);
		int c = THEME_TAILLE / 2;
		g.fill(x + 5, y + 5, x + c, y + c, this.theme.encre);
		g.fill(x + c, y + 5, x + 15, y + c, this.theme.encreVerte);
		g.fill(x + 5, y + c, x + c, y + 15, this.theme.creuxHaut);
		g.fill(x + c, y + c, x + 15, y + 15, this.theme.ongletBord);
	}

	private boolean themeSous(double sourisX, double sourisY) {
		return dansLaBoite(sourisX, sourisY, themeX(),
				this.haut + 43, THEME_TAILLE, THEME_TAILLE);
	}

	/** Le nuancier reste sur la couverture, jamais sur la zone d'ecriture. */
	private int themeX() {
		return this.theme.hauteDefinition()
				? Math.max(1, this.gauche - HabillageLivreHD.margeGauche(this.gauche) + 4)
				: Math.max(1, this.gauche + 1);
	}

	private void changerTheme(boolean precedent) {
		appliquerTheme(precedent ? this.theme.precedent() : this.theme.suivant());
		this.theme.sauvegarder();
		Bruits.clic();
	}

	// --- Ce qu'on peut faire ------------------------------------------------------------

	@Override
	public boolean mouseClicked(double sourisX, double sourisY, int bouton) {
		if ((bouton == GLFW.GLFW_MOUSE_BUTTON_LEFT || bouton == GLFW.GLFW_MOUSE_BUTTON_RIGHT)
				&& themeSous(sourisX, sourisY)) {
			changerTheme(bouton == GLFW.GLFW_MOUSE_BUTTON_RIGHT);
			return true;
		}
		if (clochetteCliquee(sourisX, sourisY, bouton)) {
			return true;
		}
		if (bouton != 0 || this.entrees.isEmpty()) {
			return super.mouseClicked(sourisX, sourisY, bouton);
		}
		if (pageEnMouvement()) {
			return true;
		}

		if (dansLaBoite(sourisX, sourisY, boutonX(), boutonY(), BOUTON_LARGEUR, BOUTON_HAUTEUR)) {
			basculer();
			return true;
		}

		int listeX = this.gauche + PAGE_DROITE_X;
		int listeY = this.haut + PAGE_Y + 22;
		for (int i = 0; i < PAR_PAGE; i++) {
			int position = this.page * PAR_PAGE + i;
			if (position >= this.entrees.size()) {
				break;
			}
			if (dansLaBoite(sourisX, sourisY, listeX - 2, listeY + i * LIGNE - 2,
					PAGE_LARGEUR + 2, LIGNE)) {
				this.choisi = position;
				this.arriveePortrait = 0.0F;
				Bruits.clic();
				return true;
			}
		}

		if (pagesMaximum() > 1) {
			int flecheY = this.haut + 207;
			if (this.page > 0 && dansLaBoite(sourisX, sourisY,
					this.gauche + 22, flecheY, FLECHE_LARGEUR, FLECHE_HAUTEUR)) {
				allerPageListe(this.page - 1);
				return true;
			}
			if (this.page < pagesMaximum() - 1 && dansLaBoite(sourisX, sourisY,
					this.gauche + 333, flecheY,
					FLECHE_LARGEUR, FLECHE_HAUTEUR)) {
				allerPageListe(this.page + 1);
				return true;
			}
		}
		return super.mouseClicked(sourisX, sourisY, bouton);
	}

	@Override
	public boolean keyPressed(int touche, int codeMateriel, int modificateurs) {
		if (touche == GLFW.GLFW_KEY_C) {
			changerTheme((modificateurs & GLFW.GLFW_MOD_SHIFT) != 0);
			return true;
		}
		if (this.entrees.isEmpty()) {
			return super.keyPressed(touche, codeMateriel, modificateurs);
		}
		switch (touche) {
			case GLFW.GLFW_KEY_TAB, GLFW.GLFW_KEY_DOWN -> {
				choisirAutre(1);
				return true;
			}
			case GLFW.GLFW_KEY_UP -> {
				choisirAutre(-1);
				return true;
			}
			case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
				basculer();
				return true;
			}
			case GLFW.GLFW_KEY_B -> {
				// Le livre de celui qu'on regarde : c'est la suite naturelle du
				// geste, et ca evite de refermer pour rouvrir.
				Bruits.page();
				ClientPlayNetworking.send(new PaquetLivre(this.choisi));
				return true;
			}
			default -> {
				return super.keyPressed(touche, codeMateriel, modificateurs);
			}
		}
	}

	private void choisirAutre(int pas) {
		this.choisi = Math.floorMod(this.choisi + pas, this.entrees.size());
		this.page = this.choisi / PAR_PAGE;
		this.arriveePortrait = 0.0F;
		Bruits.clic();
	}

	/** Envoie l'intention. Le serveur decide, et renverra la liste a jour. */
	private void basculer() {
		EntreeCarnet entree = courant();
		if (entree == null) {
			return;
		}
		Bruits.clic();
		ClientPlayNetworking.send(new PaquetInvoquer(this.choisi, !entree.sorti()));
	}

}
