package fr.lhdp.compagnon.client;

import com.mojang.authlib.GameProfile;
import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.classement.Classement;
import fr.lhdp.compagnon.entite.CompagnonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Le classement des compagnons du serveur.
 *
 * <h2>Ce qu'il montre, et dans cet ordre</h2>
 *
 * <ol>
 *   <li><b>Ta position et le nombre de participants.</b> Avant tout le reste.
 *       Sans cette ligne, un classement a mille joueurs decourage ; avec elle,
 *       on a toujours un objectif a portee — gagner trois places.</li>
 *   <li><b>Le podium.</b> Trois cartes, la premiere plus haute et bordee d'or.</li>
 *   <li><b>La liste,</b> et surtout <b>les cinq au-dessus et les cinq en dessous
 *       de toi</b> quand tu n'es pas dans le haut du tableau.</li>
 *   <li><b>Le compagnon survole,</b> a droite, en trois dimensions. C'est ce qui
 *       fait passer l'ecran d'une competition a une galerie.</li>
 * </ol>
 *
 * <h2>Ce qu'il ne montre pas</h2>
 *
 * <p>Aucune jauge, aucune competence, aucune barre de complicite. Ce qui se
 * passe entre un joueur et sa bete ne regarde personne d'autre.
 *
 * <h2>Le dessin</h2>
 *
 * <p>Aucune texture : tout est peint en aplats et en degrades. C'est un fichier
 * d'image de moins a livrer, ca se redimensionne tout seul, et surtout ca permet
 * d'avoir des coins arrondis et des bords en degrade — ce qu'une texture fixe ne
 * saurait pas faire a une taille quelconque.
 */
public class EcranClassement extends EcranCompagnon {

	// --- Les mesures ----------------------------------------------------------

	private static final int LARGEUR = 420;
	private static final int HAUTEUR = 236;

	/** La colonne de droite, ou se tient le compagnon survole. */
	private static final int VOLET = 126;
	private static final int MARGE = 12;

	private static final int LIGNE_HAUTEUR = 26;
	private static final int PODIUM_HAUTEUR = 74;

	/** La tete du joueur, en pixels. */
	private static final int TETE = 16;

	/** Combien de lignes tiennent sous le podium. */
	private static final int PAR_PAGE = 4;

	// --- Les couleurs ---------------------------------------------------------
	//
	// Une aubergine profonde, un or chaud, et rien d'autre. La regle qu'on s'est
	// donnee : une seule couleur d'accent, et tout le reste en valeurs du meme
	// violet. C'est ce qui fait qu'un ecran a l'air compose plutot qu'assemble.

	private static final int VOILE = 0xD0100A18;

	private static final int CARTE_BORD = 0xFF3E3360;

	private static final int OR = 0xFFE8C87A;
	private static final int OR_SOMBRE = 0xFF9A7C38;
	private static final int ARGENT = 0xFFC9CEDC;
	private static final int BRONZE = 0xFFC08A5A;

	private static final int TEXTE = 0xFFF2ECF8;
	private static final int TEXTE_PALE = 0xFFA093BE;
	/** Le voile pose sur la ligne survolee. Transparent, pour ne rien effacer. */
	private static final int SURVOL_LIGNE = 0x22FFFFFF;

	// --- L'etat ---------------------------------------------------------------

	private final List<Classement.Ligne> lignes;
	private final int monRang;
	private final int participants;
	private final int debutVoisinage;

	private int page;
	private int survole = -1;

	/** De zero a un : l'ecran est en train de s'ouvrir. */
	private float ouverture;

	private CompagnonEntity apercu;
	private String apercuEspece = "";
	private String apercuVariante = "";
	private boolean apercuAbandonne;

	/**
	 * Les skins deja demandes.
	 *
	 * <p>{@code getInsecureSkin} rend tout de suite — le skin par defaut d'abord,
	 * le vrai des qu'il est charge — mais il passe par un cache a cle composite a
	 * chaque appel. Sur une liste redessinee soixante fois par seconde, autant se
	 * souvenir de ce qu'on a demande.
	 */
	private final Map<UUID, PlayerSkin> skins = new HashMap<>();

	private int gauche;
	private int haut;

	public EcranClassement(List<Classement.Ligne> lignes, int monRang, int participants,
			int debutVoisinage) {

		super(Component.translatable("classement.compagnon.titre"));
		this.lignes = lignes;
		this.monRang = monRang;
		this.participants = participants;
		this.debutVoisinage = debutVoisinage;

		// ON OUVRE DIRECTEMENT SUR SON PROPRE VOISINAGE.
		//
		// Un joueur quarante-septieme n'a aucune envie de faire defiler pour se
		// trouver. Le podium reste a une page de la, s'il veut le voir.
		if (debutVoisinage >= 0) {
			this.page = debutVoisinage / PAR_PAGE;
		}
	}

	@Override
	protected void init() {
		this.gauche = (this.width - LARGEUR) / 2;
		this.haut = (this.height - HAUTEUR) / 2;
	}

	@Override
	public void render(GuiGraphics g, int sourisX, int sourisY, float partiel) {
		// Remis a zero a chaque image : le survol est recalcule par les cadres qui
		// se dessinent en dessous.
		this.survole = -1;

		renderBackground(g, sourisX, sourisY, partiel);
		g.fill(0, 0, this.width, this.height, VOILE);

		// UNE OUVERTURE QUI SE VOIT.
		//
		// L'ecran apparaissait d'un coup. Un panneau qui monte de six pixels en
		// grandissant, sur un quart de seconde, ne coute rien et fait toute la
		// difference entre une interface qui s'affiche et une qui s'ouvre.
		this.ouverture = Peinture.vers(this.ouverture, 1.0F, 0.22F, partiel);
		int monte = Math.round((1.0F - Peinture.adoucir(this.ouverture)) * 6.0F);

		Peinture.panneau(g, this.gauche, this.haut + monte, LARGEUR, HAUTEUR);

		int x = this.gauche + MARGE;
		int y = this.haut + MARGE;
		int large = LARGEUR - VOLET - 3 * MARGE;

		y = enTete(g, x, y, large);
		y = podium(g, x, y, large, sourisX, sourisY);
		liste(g, x, y, large, sourisX, sourisY);

		volet(g, this.gauche + LARGEUR - VOLET - MARGE, this.haut + MARGE, sourisX, sourisY);

		super.render(g, sourisX, sourisY, partiel);
		clochette(g, sourisX, sourisY, partiel);
	}

	// --- Le haut --------------------------------------------------------------

	/** Le titre en grand, puis la ligne d'etat. Rend l'ordonnee suivante. */
	private int enTete(GuiGraphics g, int x, int y, int large) {
		// LE TITRE EN GRAND. La police du jeu n'a qu'une taille : on l'agrandit
		// dans la matrice. C'est le seul endroit de l'ecran ou l'on fait ca, et
		// c'est ce qui donne une hierarchie a la page.
		g.pose().pushPose();
		g.pose().translate(x, y, 0.0F);
		g.pose().scale(1.6F, 1.6F, 1.0F);
		g.drawString(this.font, Component.translatable("classement.compagnon.titre"),
				0, 0, OR, false);
		g.pose().popPose();

		int etatY = y + 22;

		Component position = this.monRang > 0
				? Component.translatable("classement.compagnon.position", this.monRang)
				: Component.translatable("classement.compagnon.non_classe");
		g.drawString(this.font, position, x, etatY, TEXTE, false);

		Component combien = Component.translatable("classement.compagnon.participants",
				this.participants);
		g.drawString(this.font, combien,
				x + large - this.font.width(combien), etatY, TEXTE_PALE, false);

		// Un filet sous l'en-tete, qui s'efface vers la droite. C'est ce qui
		// remplace un cadre : ca separe sans enfermer.
		g.fillGradient(x, etatY + 13, x + large, etatY + 14, OR_SOMBRE, 0x00000000);
		return etatY + 22;
	}

	// --- Le podium ------------------------------------------------------------

	/**
	 * Les trois premiers, en cartes. Rend l'ordonnee ou la liste peut commencer.
	 *
	 * <p>Le podium ne s'affiche qu'a la premiere page : quand on a fait defiler
	 * jusqu'a la centieme place, il n'a plus rien a dire et la place vaut mieux
	 * pour des lignes.
	 */
	private int podium(GuiGraphics g, int x, int y, int large, int sourisX, int sourisY) {
		if (this.page > 0 || this.lignes.isEmpty()) {
			return y;
		}

		int combien = Math.min(3, this.lignes.size());
		int ecart = 6;
		int largeurCarte = (large - 2 * ecart) / 3;

		for (int i = 0; i < combien; i++) {
			int carteX = x + i * (largeurCarte + ecart);
			// Le premier est plus haut : on lit le podium sans lire les chiffres.
			int hauteur = i == 0 ? PODIUM_HAUTEUR : PODIUM_HAUTEUR - 10;
			int carteY = y + (PODIUM_HAUTEUR - hauteur);

			if (dansLeCadre(sourisX, sourisY, carteX, carteY, largeurCarte, hauteur)) {
				this.survole = i;
			}

			int medaille = switch (i) {
				case 0 -> OR;
				case 1 -> ARGENT;
				default -> BRONZE;
			};
			Peinture.carte(g, carteX, carteY, largeurCarte, hauteur, i == 0);

			// Le bandeau de la medaille, en haut de la carte : trois pixels de
			// couleur qui disent le rang avant qu'on ait lu quoi que ce soit.
			g.fill(carteX + 3, carteY + 1, carteX + largeurCarte - 3, carteY + 3, medaille);

			Classement.Ligne ligne = this.lignes.get(i);
			int milieu = carteX + largeurCarte / 2;

			tete(g, ligne, milieu - TETE / 2, carteY + 9);
			centre(g, "#" + (i + 1), milieu, carteY + 28, medaille);
			centre(g, court(ligne.joueur(), largeurCarte - 10), milieu, carteY + 40, TEXTE);
			centre(g, court(ligne.compagnon(), largeurCarte - 10),
					milieu, carteY + 50, TEXTE_PALE);
			centre(g, Component.translatable("classement.compagnon.niveau", ligne.niveau())
					.getString(), milieu, carteY + hauteur - 13, OR);
		}
		return y + PODIUM_HAUTEUR + 8;
	}

	// --- La liste -------------------------------------------------------------

	private void liste(GuiGraphics g, int x, int y, int large, int sourisX, int sourisY) {
		int depuis = this.page == 0 ? 3 : this.page * PAR_PAGE;
		int jusqua = Math.min(this.lignes.size(), depuis + PAR_PAGE);

		if (depuis >= jusqua) {
			g.drawString(this.font,
					Component.translatable("classement.compagnon.personne_dautre"),
					x, y + 6, TEXTE_PALE, false);
			piedDePage(g, x, large);
			return;
		}

		for (int i = depuis; i < jusqua; i++) {
			int ligneY = y + (i - depuis) * LIGNE_HAUTEUR;
			Classement.Ligne ligne = this.lignes.get(i);

			boolean cestMoi = estMoi(i);
			boolean dessus = dansLeCadre(sourisX, sourisY, x, ligneY, large, LIGNE_HAUTEUR - 4);
			if (dessus) {
				this.survole = i;
			}

			Peinture.carte(g, x, ligneY, large, LIGNE_HAUTEUR - 4, cestMoi);
			// LE SURVOL S'ALLUME, il ne bascule pas : un voile clair par-dessus la
			// carte, ce qui permet d'etre a mi-chemin et donc de bouger.
			if (dessus) {
				g.fill(x + 2, ligneY + 1, x + large - 2, ligneY + LIGNE_HAUTEUR - 5,
					SURVOL_LIGNE);
			}

			g.drawString(this.font, "#" + rangAffiche(i), x + 7, ligneY + 7,
					cestMoi ? OR : TEXTE_PALE, false);

			tete(g, ligne, x + 30, ligneY + 3);

			int texteX = x + 30 + TEXTE_APRES_TETE;
			String niveau = Component.translatable("classement.compagnon.niveau", ligne.niveau())
					.getString();
			int placeDuNiveau = this.font.width(niveau) + 12;
			int placeDuTexte = large - (texteX - x) - placeDuNiveau;

			g.drawString(this.font, court(ligne.joueur(), placeDuTexte),
					texteX, ligneY + 4, TEXTE, false);
			g.drawString(this.font,
					court(ligne.compagnon() + " · "
						+ fr.lhdp.compagnon.espece.Especes.titre(ligne.espece()), placeDuTexte),
					texteX, ligneY + 13, TEXTE_PALE, false);

			g.drawString(this.font, niveau, x + large - 7 - this.font.width(niveau),
					ligneY + 8, OR, false);
		}
		piedDePage(g, x, large);
	}

	/** L'ecart entre la tete et le texte qui la suit. */
	private static final int TEXTE_APRES_TETE = TETE + 6;

	private void piedDePage(GuiGraphics g, int x, int large) {
		int y = this.haut + HAUTEUR - MARGE - 8;
		String pages = Component.translatable("classement.compagnon.pages",
				this.page + 1, Math.max(1, nombreDePages())).getString();
		g.drawString(this.font, pages, x, y, TEXTE_PALE, false);

		String aide = Component.translatable("classement.compagnon.fleches").getString();
		g.drawString(this.font, aide, x + large - this.font.width(aide), y, TEXTE_PALE, false);
	}

	/**
	 * Le rang affiche pour cette ligne.
	 *
	 * <p>Les lignes du voisinage ne sont pas a la suite de celles du haut du
	 * tableau : entre les deux il y a un trou, et le numero doit le dire. Sinon
	 * le joueur croit etre vingt-sixieme alors qu'il est quatre-vingt-dixieme.
	 */
	private int rangAffiche(int indice) {
		if (this.debutVoisinage >= 0 && indice >= this.debutVoisinage) {
			int depuis = Math.max(1, this.monRang - Classement.VOISINS);
			return depuis + (indice - this.debutVoisinage);
		}
		return indice + 1;
	}

	private boolean estMoi(int indice) {
		return this.minecraft != null && this.minecraft.player != null
				&& this.lignes.get(indice).qui().equals(this.minecraft.player.getUUID());
	}

	private int nombreDePages() {
		return Math.max(1, (int) Math.ceil(this.lignes.size() / (double) PAR_PAGE));
	}

	// --- Le volet de droite ---------------------------------------------------

	/**
	 * Le compagnon du joueur survole.
	 *
	 * <p>C'est le morceau qui change tout : sans lui, l'ecran est un tableau de
	 * chiffres. Avec lui, on fait defiler pour <b>voir les betes des autres</b>,
	 * ce qui donne envie de soigner la sienne plutot que de la faire monter.
	 */
	private void volet(GuiGraphics g, int x, int y, int sourisX, int sourisY) {
		int hauteur = HAUTEUR - 2 * MARGE;
		Peinture.carte(g, x, y, VOLET, hauteur, false);

		if (this.survole < 0 || this.survole >= this.lignes.size()) {
			centreEnvelope(g, Component.translatable("classement.compagnon.survole").getString(),
					x + VOLET / 2, y + hauteur / 2 - 10, VOLET - 16, TEXTE_PALE);
			return;
		}

		Classement.Ligne ligne = this.lignes.get(this.survole);
		int milieu = x + VOLET / 2;

		tete(g, ligne, milieu - TETE / 2, y + 8);
		centre(g, "#" + rangAffiche(this.survole), milieu, y + 28, OR);
		centre(g, court(ligne.joueur(), VOLET - 14), milieu, y + 39, TEXTE_PALE);

		g.fillGradient(x + 14, y + 51, x + VOLET - 14, y + 52, 0x00000000, CARTE_BORD);

		portrait(g, ligne, milieu - 48, y + 56, sourisX, sourisY);

		centre(g, court(ligne.compagnon(), VOLET - 14), milieu, y + hauteur - 48, TEXTE);
		centre(g, court(majuscule(ligne.espece()), VOLET - 14),
				milieu, y + hauteur - 37, TEXTE_PALE);
		centre(g, Component.translatable("classement.compagnon.niveau", ligne.niveau())
				.getString(), milieu, y + hauteur - 24, OR);
		centre(g, Component.translatable("classement.compagnon.jours", ligne.jours())
				.getString(), milieu, y + hauteur - 13, TEXTE_PALE);
	}

	/**
	 * La bete, en trois dimensions.
	 *
	 * <p>Une seule entite d'apercu, refaite uniquement quand on survole une autre
	 * espece : en creer une par image, sur une liste qu'on parcourt a la souris,
	 * ferait des dizaines de creations par seconde.
	 *
	 * <p>Et si le rendu echoue une fois, on abandonne definitivement plutot que
	 * de reessayer soixante fois par seconde en remplissant le journal.
	 */
	private void portrait(GuiGraphics g, Classement.Ligne ligne, int x, int y,
			int sourisX, int sourisY) {

		if (this.apercuAbandonne || this.minecraft == null || this.minecraft.level == null) {
			return;
		}
		if (this.apercu == null
				|| !this.apercuEspece.equals(ligne.espece())
				|| !this.apercuVariante.equals(ligne.variante())) {

			this.apercu = Compagnon.COMPAGNON.create(this.minecraft.level);
			if (this.apercu == null) {
				this.apercuAbandonne = true;
				return;
			}
			this.apercu.setEspece(ligne.espece());
			this.apercu.setVariante(ligne.variante());
			this.apercuEspece = ligne.espece();
			this.apercuVariante = ligne.variante();
		}

		try {
			InventoryScreen.renderEntityInInventoryFollowsMouse(g,
					x, y, x + 96, y + 96, 30, 0.0625F, sourisX, sourisY, this.apercu);
		} catch (Exception echec) {
			this.apercuAbandonne = true;
			Compagnon.LOG.warn("Apercu impossible dans le classement : {}", echec.toString());
		}
	}

	/**
	 * La tete du joueur, tiree de son skin.
	 *
	 * <p>Un nom seul ne dit rien a personne dans une liste ; un visage, si. C'est
	 * la meme chose que fait la liste des joueurs du jeu, et le meme dessinateur.
	 *
	 * <p>ON NE FABRIQUE PAS DE PROFIL. La premiere version en construisait un
	 * avec un identifiant et un nom, puis le donnait au gestionnaire de skins :
	 * ca rendait <b>toujours le visage par defaut</b>, parce qu'un profil
	 * fabrique n'a aucune texture attachee et que rien dans ce chemin-la n'allait
	 * la chercher.
	 *
	 * <p>Le bon endroit est la liste des connectes que le client tient deja —
	 * celle de la touche Tab. Elle a le profil complet, textures comprises, et
	 * son skin est deja charge : c'est gratuit, et c'est le vrai visage.
	 *
	 * <p>Le vieux commentaire disait : il arrive
	 * tout de suite, d'abord celui par defaut, puis le vrai quand il est charge.
	 * L'alternative — attendre la reponse du serveur de skins — laisserait un trou
	 * dans la liste pendant une seconde a chaque ouverture.
	 */
	private void tete(GuiGraphics g, Classement.Ligne ligne, int x, int y) {
		if (this.minecraft == null) {
			return;
		}
		PlayerSkin skin = this.skins.get(ligne.qui());
		if (skin == null) {
			skin = skinDe(ligne);
			if (skin == null) {
				return;
			}
		}
		// Un cadre d'un pixel autour du visage : c'est ce qui l'empeche de flotter
		// sur le fond de la carte.
		g.fill(x - 1, y - 1, x + TETE + 1, y + TETE + 1, CARTE_BORD);
		PlayerFaceRenderer.draw(g, skin, x, y, TETE);
	}

	/**
	 * Cherche le skin dans la liste des connectes, puis se rabat.
	 *
	 * <p>On ne retient que ce qu'on a vraiment trouve : un visage par defaut mis
	 * en cache ne se corrigerait plus jamais, meme si le joueur se connecte
	 * pendant qu'on regarde l'ecran.
	 */
	private PlayerSkin skinDe(Classement.Ligne ligne) {
		try {
			ClientPacketListener connexion = this.minecraft.getConnection();
			if (connexion != null) {
				PlayerInfo info = connexion.getPlayerInfo(ligne.qui());
				if (info != null) {
					PlayerSkin vrai = info.getSkin();
					this.skins.put(ligne.qui(), vrai);
					return vrai;
				}
			}
			// Un joueur deconnecte : personne sur le client n'a sa texture. Il
			// garde le visage par defaut, et on ne l'enregistre pas.
			return this.minecraft.getSkinManager()
				.getInsecureSkin(new GameProfile(ligne.qui(), ligne.joueur()));
		} catch (Exception echec) {
			// Un skin manquant ne doit jamais empecher le classement de s'afficher.
			return null;
		}
	}

	// --- Les commandes --------------------------------------------------------

	@Override
	public boolean keyPressed(int touche, int balayage, int modificateurs) {
		if (touche == GLFW.GLFW_KEY_RIGHT || touche == GLFW.GLFW_KEY_DOWN) {
			allerA(this.page + 1);
			return true;
		}
		if (touche == GLFW.GLFW_KEY_LEFT || touche == GLFW.GLFW_KEY_UP) {
			allerA(this.page - 1);
			return true;
		}
		return super.keyPressed(touche, balayage, modificateurs);
	}

	@Override
	public boolean mouseScrolled(double sourisX, double sourisY, double horizontal,
			double vertical) {

		if (vertical < 0.0D) {
			allerA(this.page + 1);
		} else if (vertical > 0.0D) {
			allerA(this.page - 1);
		}
		return true;
	}

	/**
	 * Va a cette page, si elle existe.
	 *
	 * <p>Le bruit ne se joue que si la page a <b>vraiment</b> change. Un bruit
	 * qui repond a un geste sans effet est pire que pas de bruit du tout : il
	 * apprend au joueur a ne plus l'ecouter.
	 */
	private void allerA(int page) {
		int voulue = Math.max(0, Math.min(nombreDePages() - 1, page));
		if (voulue != this.page) {
			this.page = voulue;
			Bruits.page();
		}
	}

	@Override
	public boolean mouseClicked(double sourisX, double sourisY, int bouton) {
		if (clochetteCliquee(sourisX, sourisY, bouton)) {
			return true;
		}
		return super.mouseClicked(sourisX, sourisY, bouton);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// --- Le dessin ------------------------------------------------------------


	private void centre(GuiGraphics g, String texte, int milieuX, int y, int couleur) {
		g.drawString(this.font, texte, milieuX - this.font.width(texte) / 2, y, couleur, false);
	}

	/** Un texte centre qui va a la ligne. Pour la seule phrase d'aide de l'ecran. */
	private void centreEnvelope(GuiGraphics g, String texte, int milieuX, int y, int large,
			int couleur) {

		List<net.minecraft.util.FormattedCharSequence> morceaux =
				this.font.split(Component.literal(texte), large);
		int ligneY = y;
		for (net.minecraft.util.FormattedCharSequence morceau : morceaux) {
			g.drawString(this.font, morceau,
					milieuX - this.font.width(morceau) / 2, ligneY, couleur, false);
			ligneY += 10;
		}
	}

	/**
	 * Coupe un texte a la largeur donnee, sans jamais deborder.
	 *
	 * <p>C'est la regle qu'on s'est donnee pour tout le mod : deux textes ne se
	 * chevauchent jamais. Un nom de joueur peut faire seize caracteres, un nom de
	 * compagnon aussi, et une largeur de colonne ne s'etire pas.
	 */
	private String court(String texte, int large) {
		if (large <= 0) {
			return "";
		}
		if (this.font.width(texte) <= large) {
			return texte;
		}
		return this.font.plainSubstrByWidth(texte, large - this.font.width("…")) + "…";
	}

	/** La premiere lettre en majuscule. Les especes sont nommees en minuscules. */
	private static String majuscule(String mot) {
		if (mot == null || mot.isEmpty()) {
			return "";
		}
		return Character.toUpperCase(mot.charAt(0)) + mot.substring(1).replace('_', ' ');
	}

	private static boolean dansLeCadre(int sourisX, int sourisY, int x, int y,
			int large, int haut) {

		return sourisX >= x && sourisX < x + large && sourisY >= y && sourisY < y + haut;
	}
}
