package fr.lhdp.compagnon.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.livre.EntreeRoue;
import fr.lhdp.compagnon.reseau.PaquetAction;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * La roue des actions : un anneau decoupe en parts.
 *
 * <p>Ce qui n'est pas encore ouvert s'affiche <b>avec un cadenas et le niveau
 * qu'il faut</b>, jamais cache : montrer ce qui existe encore fait plus pour
 * l'envie que de le cacher.
 *
 * <p>La roue ne decide rien. Elle affiche ce que le serveur lui a envoye, et
 * quand on clique elle ne fait qu'envoyer un nom. Le serveur reverifie tout.
 *
 * <p>Aucun nom d'animation n'est ecrit ici : ils viennent tous de la table des
 * niveaux.
 *
 * <p>Une seule texture, celle d'une part du haut. Elle est dessinee huit fois,
 * tournee d'un huitieme de tour a chaque fois, et teintee selon l'etat de la
 * case. C'est plus fiable que de coudre des triangles a la main : le rendu
 * d'interface de Minecraft groupe ses dessins, et de la geometrie posee au
 * milieu se fait recouvrir.
 */
public class EcranRoue extends EcranCompagnon {

	private static final ResourceLocation PART = Compagnon.id("textures/gui/roue/part.png");

	/** Taille de la texture, et taille de la roue a l'ecran. */
	private static final int TEXTURE = 512;
	private static final int DIAMETRE = 232;

	/** Nombre de parts, donc d'actions visibles a la fois. */
	private static final int PARTS = 8;

	/** Rayons a l'ecran, deduits de la texture pour que le survol tombe juste. */
	private static final float RAYON_EXTERIEUR = DIAMETRE * 244.0F / TEXTURE;
	private static final float RAYON_INTERIEUR = DIAMETRE * 96.0F / TEXTURE;

	private static final int VOILE = 0x90101014;

	/** De combien la part survolee grandit, en pixels de diametre. */
	private static final int POUSSEE = 11;

	/** La taille de la roue au premier instant de l'ouverture. */
	private static final float DEPART = 0.86F;

	private static final int ONGLET_HAUTEUR = 18;
	private static final int ONGLET_MARGE = 10;
	private static final int ONGLET_ECART = 4;
	private static final int ONGLET_AU_DESSUS = 24;

	/** Marge minimale gardee contre le bord de l'ecran. */
	private static final int BORD = 4;

	private static final int ONGLET_FOND = 0x66000000;
	private static final int ONGLET_FOND_ACTIF = 0xAA3F6B37;

	private static final int BOUTON_LARGEUR = 150;
	private static final int BOUTON_HAUTEUR = 20;
	private static final int BOUTON_SOUS_LA_ROUE = 24;

	private static final int BOUTON_FOND = 0x66000000;
	private static final int BOUTON_FOND_ARME = 0xAA3F6B37;
	private static final int BOUTON_BORD = 0xFFC8BCA4;

	/** Le fond du bouton quand la souris est dessus, sans etre arme. */
	private static final int BOUTON_FOND_CHAUD = 0x99000000;

	private static final int TEXTE = 0xFFF6EEDC;
	private static final int TEXTE_SURVOL = 0xFF2A2118;
	private static final int TEXTE_VERROU = 0xFF9C9080;
	private static final int TEXTE_TITRE = 0xFFFFFFFF;

	/** La place que prend l'etoile devant un geste tout neuf. */
	private static final int ETOILE = 10;

	/** L'or des cases qui viennent de s'ouvrir. */
	private static final int TEXTE_NOUVEAU = 0xFFE8C87A;

	/** Le rouge d'un refus : celui des degats du jeu, deja connu du joueur. */
	private static final int TEXTE_REFUS = 0xFFE05A50;

	/**
	 * Jusqu'a quand la roue est verrouillee, en temps de jeu.
	 *
	 * <h2>Pourquoi c'est ici et pas dans l'ecran</h2>
	 *
	 * <p>La roue se ferme des qu'on clique. Le probleme n'etait donc pas de
	 * cliquer deux fois dans le meme ecran, mais de <b>la rouvrir aussitot</b>
	 * et de relancer un geste par-dessus celui qui jouait encore : on obtenait
	 * deux animations tronquees collees l'une a l'autre.
	 *
	 * <p>La valeur survit donc a la fermeture de l'ecran. Le serveur refuse de
	 * toute facon — mais un bouton qu'on peut cliquer et qui repond « non » est
	 * moins bon qu'un bouton qui montre qu'il n'est pas encore l'heure.
	 */
	private static long occupeJusqua;

	/** Est-il encore en train de jouer le geste qu'on vient de lui demander ? */
	private boolean occupe() {
		return this.minecraft != null && this.minecraft.level != null
			&& this.minecraft.level.getGameTime() < occupeJusqua;
	}

	private final int index;
	private final String nomCompagnon;
	private final int niveau;
	private final List<EntreeRoue> entrees;

	/**
	 * Les noms de toutes ses betes, dans l'ordre du serveur.
	 *
	 * <p>Un onglet par compagnon, en haut de la roue. Tab marchait deja, mais
	 * il fallait le savoir : rien a l'ecran ne disait qu'on pouvait changer de
	 * bete, ni combien on en avait.
	 */
	private final List<String> compagnons;

	private int page;
	private int survolee = -1;

	/**
	 * La derniere case pour laquelle on a demande un apercu.
	 *
	 * <p>Sans ce souvenir, on enverrait un paquet a CHAQUE image tant que la
	 * souris ne bouge pas — soixante par seconde et par joueur. On n'envoie qu'au
	 * changement.
	 */
	/**
	 * Vrai quand le prochain clic sur une part sert a lui <b>apprendre</b> un
	 * mot, au lieu de jouer le geste.
	 *
	 * <p>Un mode et non un bouton par part : la roue marche au survol, et un
	 * bouton pose sur une part serait perdu des qu'on bouge la souris pour aller
	 * le chercher. On arme d'abord, on choisit ensuite.
	 */
	private boolean modeApprentissage;

	/** De zero a un : le bouton est en train de s'allumer. */
	private float chaleurDuBouton;

	/**
	 * La case sur laquelle le dernier clic a ete refuse.
	 *
	 * <p>Cliquer sur un cadenas ne faisait <b>rien du tout</b> : ni bruit, ni
	 * mouvement. Le joueur ne pouvait pas faire la difference entre « c'est
	 * verrouille » et « mon clic n'est pas passe », alors il recliquait.
	 *
	 * <p>Desormais le cadenas rougit une demi-seconde et un bruit grave repond.
	 * Un refus est une reponse, pas une absence de reponse.
	 */
	private int refusee = -1;

	/** De un a zero : le rouge du refus en train de s'effacer. */
	private float chaleurDuRefus;

	/**
	 * De zero a un : la roue est en train de s'ouvrir.
	 *
	 * <p>Elle apparaissait d'un seul coup, en pleine taille. C'est ce qui fait
	 * la difference entre un menu qui s'ouvre et une image qu'on colle sur
	 * l'ecran — et sur un menu qu'on ouvre vingt fois par soiree, ce dixieme
	 * de seconde est tout ce qu'on retient.
	 */
	private float ouverture;

	/**
	 * De zero a un pour chaque part : elle est en train de venir vers toi.
	 *
	 * <p>La part survolee changeait seulement de couleur. Elle avance
	 * maintenant de quelques pixels, comme une touche qu'on souleve : c'est ce
	 * qui fait qu'une roue se pilote du coin de l'oeil.
	 *
	 * <p>Une valeur par part, et non un seul survol : celle qu'on quitte doit
	 * redescendre pendant que la nouvelle monte. Sans ca, l'une saute pendant
	 * que l'autre glisse.
	 */
	private final float[] avancee = new float[PARTS];

	private int apercuEnvoye = -1;
	private boolean survolAvant;
	private boolean survolApres;

	public EcranRoue(int index, String nomCompagnon, int niveau, List<EntreeRoue> entrees,
			List<String> compagnons) {

		super(Component.literal(nomCompagnon));
		this.index = index;
		this.nomCompagnon = nomCompagnon;
		this.niveau = niveau;
		this.entrees = entrees;
		this.compagnons = compagnons;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private int nombreDePages() {
		return Math.max(1, (this.entrees.size() + PARTS - 1) / PARTS);
	}

	/** L'entree posee sur la part {@code i} de la page courante, ou {@code null}. */
	private EntreeRoue entreeDe(int i) {
		// Le moins un de « rien sous la souris » arrivait jusqu ici et sortait par
		// une erreur d indice. Tous les appels le filtraient — jusqu au jour ou
		// l un d eux l oublierait.
		if (i < 0) {
			return null;
		}
		int position = this.page * PARTS + i;
		return position < this.entrees.size() ? this.entrees.get(position) : null;
	}

	@Override
	public void render(GuiGraphics g, int sourisX, int sourisY, float partiel) {
		super.render(g, sourisX, sourisY, partiel);
		g.fill(0, 0, this.width, this.height, VOILE);

		int cx = this.width / 2;
		int cy = this.height / 2;

		this.ouverture = Peinture.vers(this.ouverture, 1.0F, 0.28F, partiel);
		this.chaleurDuRefus = Peinture.vers(this.chaleurDuRefus, 0.0F, 0.10F, partiel);
		if (this.chaleurDuRefus < 0.02F) {
			this.refusee = -1;
		}
		calculerSurvol(cx, cy, sourisX, sourisY);
		demanderLApercu();

		// CHAQUE PART AVANCE OU RECULE A SON RYTHME.
		for (int i = 0; i < PARTS; i++) {
			EntreeRoue entree = entreeDe(i);
			boolean vivante = entree != null && entree.debloque() && !occupe();
			this.avancee[i] = Peinture.vers(this.avancee[i],
				i == this.survolee && vivante ? 1.0F : 0.0F, 0.34F, partiel);
		}

		// LA ROUE S'OUVRE EN GRANDISSANT, autour de son centre. Tout ce qui est
		// dessine ici passe par la meme mise a l'echelle : l'anneau, les mots et
		// le trou du milieu arrivent ensemble, sans quoi le texte flotterait
		// au-dessus d'un anneau plus petit que lui.
		float echelle = DEPART + (1.0F - DEPART) * Peinture.adoucir(this.ouverture);
		g.pose().pushPose();
		g.pose().translate(cx, cy, 0.0F);
		g.pose().scale(echelle, echelle, 1.0F);
		g.pose().translate(-cx, -cy, 0.0F);

		anneau(g, cx, cy);
		etiquettes(g, cx, cy);
		centre(g, cx, cy);

		g.pose().popPose();
		boutonApprendre(g, cx, cy, sourisX, sourisY);
		onglets(g, cx, cy, sourisX, sourisY, partiel);
		clochette(g, sourisX, sourisY, partiel);
	}

	// --- Ou pointe la souris ----------------------------------------------------

	private void calculerSurvol(int cx, int cy, int sourisX, int sourisY) {
		this.survolee = -1;
		this.survolAvant = false;
		this.survolApres = false;

		float dx = sourisX - cx;
		float dy = sourisY - cy;
		float distance = (float) Math.sqrt(dx * dx + dy * dy);

		if (distance < RAYON_INTERIEUR) {
			// Le trou du milieu : c'est la que se changent les pages.
			if (nombreDePages() > 1) {
				this.survolAvant = dx < -10.0F;
				this.survolApres = dx > 10.0F;
			}
			return;
		}
		if (distance > RAYON_EXTERIEUR) {
			return;
		}

		// L'angle zero est en haut, et on tourne dans le sens des aiguilles. On
		// decale d'une demi-part pour que la premiere soit centree en haut.
		double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0 + (180.0 / PARTS);
		if (angle < 0) {
			angle += 360.0;
		}
		this.survolee = (int) (angle % 360.0 / (360.0 / PARTS));

		// PAS DE BRUIT AU SURVOL.
		//
		// Il y en a eu un, tres court et tres aigu, cense faire sentir la
		// selection sans qu'on ait a la lire. En jeu il etait desagreable : la
		// souris traverse jusqu'a huit parts d'un seul geste, et huit tics aigus
		// a la suite ne font pas un retour, ils font une alarme.
		//
		// La part survolee grandit deja, et c'est bien assez.
	}

	// --- L'anneau ---------------------------------------------------------------

	private void anneau(GuiGraphics g, int cx, int cy) {
		for (int i = 0; i < PARTS; i++) {
			EntreeRoue entree = entreeDe(i);
			float[] teinte = teinteDe(entree, i == this.survolee);
			int taille = DIAMETRE
				+ Math.round(POUSSEE * Peinture.adoucir(this.avancee[i]));
			partTournee(g, cx, cy, i * (360.0F / PARTS), teinte, taille);
		}
	}

	/** Rouge, vert, bleu, opacite, entre zero et un. */
	private float[] teinteDe(EntreeRoue entree, boolean survol) {
		if (entree == null) {
			// Une part vide reste dessinee : la roue garde sa forme meme quand la
			// table ne decrit que deux actions.
			return new float[]{1.00F, 1.00F, 1.00F, 0.16F};
		}
		if (!entree.debloque()) {
			return new float[]{0.26F, 0.23F, 0.20F, 0.62F};
		}
		// Pendant qu il joue, toute la roue s eteint : on voit d un coup d oeil
		// que ce n est pas le moment, sans avoir a cliquer pour se le faire dire.
		if (occupe()) {
			return new float[]{0.40F, 0.38F, 0.34F, 0.40F};
		}
		return survol
				? new float[]{0.96F, 0.88F, 0.66F, 0.92F}
				: new float[]{0.91F, 0.85F, 0.69F, 0.44F};
	}

	private void partTournee(GuiGraphics g, int cx, int cy, float degres, float[] teinte,
			int taille) {

		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(teinte[0], teinte[1], teinte[2], teinte[3]);

		g.pose().pushPose();
		g.pose().translate(cx, cy, 0.0F);
		g.pose().mulPose(Axis.ZP.rotationDegrees(degres));
		g.pose().translate(-cx, -cy, 0.0F);

		g.blit(PART, cx - taille / 2, cy - taille / 2, taille, taille,
				0.0F, 0.0F, TEXTURE, TEXTURE, TEXTURE, TEXTURE);

		g.pose().popPose();

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
	}

	// --- Ce qui est ecrit sur les parts -----------------------------------------

	private void etiquettes(GuiGraphics g, int cx, int cy) {
		float parPart = 360.0F / PARTS;
		float rayonTexte = (RAYON_INTERIEUR + RAYON_EXTERIEUR) / 2.0F;
		int largeurMax = 74;

		for (int i = 0; i < PARTS; i++) {
			EntreeRoue entree = entreeDe(i);
			if (entree == null) {
				continue;
			}

			double angle = Math.toRadians(i * parPart - 90.0);
			int x = cx + Math.round((float) Math.cos(angle) * rayonTexte);
			int y = cy + Math.round((float) Math.sin(angle) * rayonTexte);

			boolean ouverte = entree.debloque();
			boolean survol = i == this.survolee && ouverte;

			// CE QUI VIENT DE S'OUVRIR SE VOIT.
			//
			// Un geste debloque apparaissait dans la roue exactement comme les
			// autres : il fallait se souvenir de ce qu'il y avait avant pour le
			// remarquer. Une case ouverte au niveau ou l'on est passe reste donc
			// doree, avec une etoile, jusqu'au niveau suivant.
			boolean neuve = ouverte && entree.niveauRequis() == this.niveau
				&& entree.niveauRequis() > 0;
			int couleur = !ouverte ? TEXTE_VERROU
				: survol ? TEXTE_SURVOL : (neuve ? TEXTE_NOUVEAU : TEXTE);

			// L'etoile d'un geste qui vient de s'ouvrir vient de notre police : le
			// caractere d'avant n'existait pas dans toutes les polices du jeu et
			// s'affichait en carre chez une partie des joueurs.
			String etiquette = this.font.plainSubstrByWidth(
				etiquette(entree.nom()), largeurMax);

			if (ouverte) {
				// Le mot appris se lit sous le geste : c'est le seul endroit ou on voit
				// d'un coup d'oeil ce que sa bete sait deja, et donc ce qu'il reste a
				// lui apprendre.
				int largeurTotale = this.font.width(etiquette) + (neuve ? ETOILE : 0);
				int debut = x - largeurTotale / 2;
				int hautDuNom = entree.aUnMot() ? y - 9 : y - 4;
				if (neuve) {
					g.drawString(this.font, fr.lhdp.compagnon.Icones.de(
							fr.lhdp.compagnon.Icones.ETOILE),
						debut, hautDuNom, TEXTE_NOUVEAU, false);
					debut += ETOILE;
				}
				g.drawString(this.font, etiquette, debut, hautDuNom, couleur, false);
				if (entree.aUnMot()) {
					String mot = this.font.plainSubstrByWidth(
							"« " + entree.mot() + " »", largeurMax);
					g.drawString(this.font, mot, x - this.font.width(mot) / 2, y + 2,
							survol ? TEXTE_SURVOL : TEXTE_VERROU, false);
				}
			} else {
				// Le cadenas rougit quand on vient de cliquer dessus, puis se calme.
				int encre = i == this.refusee
					? Peinture.melanger(TEXTE_VERROU, TEXTE_REFUS,
						Peinture.adoucir(this.chaleurDuRefus))
					: TEXTE_VERROU;
				cadenas(g, x - 3, y - 13, encre);
				g.drawString(this.font, etiquette,
						x - this.font.width(etiquette) / 2, y - 1, couleur, false);
				String requis = Component.translatable("roue.compagnon.requis",
						entree.niveauRequis()).getString();
				g.drawString(this.font, requis,
						x - this.font.width(requis) / 2, y + 10, encre, false);
			}
		}
	}

	/**
	 * Une fleche de page, dessinee au pixel.
	 *
	 * <p>Le point {@code x, y} est la pointe. On empile des colonnes de plus en
	 * plus hautes en s'en eloignant, ce qui donne un triangle net a n'importe
	 * quelle echelle d'interface.
	 */
	private void fleche(GuiGraphics g, int x, int y, boolean versLaDroite, int couleur) {
		int hauteur = 6;
		for (int i = 0; i <= hauteur; i++) {
			int colonne = versLaDroite ? x - i : x + i;
			g.fill(colonne, y - i, colonne + 1, y + i + 1, couleur);
		}
	}

	/** Un petit cadenas dessine au pixel : aucune texture a fournir. */
	private void cadenas(GuiGraphics g, int x, int y, int couleur) {
		g.fill(x + 2, y, x + 5, y + 1, couleur);
		g.fill(x + 1, y + 1, x + 2, y + 3, couleur);
		g.fill(x + 5, y + 1, x + 6, y + 3, couleur);
		g.fill(x, y + 3, x + 7, y + 8, couleur);
	}

	// --- Le trou du milieu ------------------------------------------------------

	private void centre(GuiGraphics g, int cx, int cy) {
		EntreeRoue regardee = this.survolee < 0 ? null : entreeDe(this.survolee);
		if (regardee == null) {
			texteCentre(g, Component.literal(this.nomCompagnon), cx, cy - 24, TEXTE_TITRE);
			texteCentre(g, Component.translatable("roue.compagnon.niveau", this.niveau),
					cx, cy - 13, TEXTE_VERROU);
		} else {
			// Le centre devient la legende de ce qu'on regarde. Le compagnon joue deja
			// l'apercu dans le monde ; ces deux lignes disent ce que c'est et comment
			// le redemander a la voix.
			texteCentre(g, Component.literal(etiquette(regardee.nom())),
					cx, cy - 24, regardee.debloque() ? TEXTE_TITRE : TEXTE_VERROU);
			if (!regardee.debloque()) {
				texteCentre(g, Component.translatable("roue.compagnon.verrouillee",
						regardee.niveauRequis()), cx, cy - 11, TEXTE_VERROU);
			} else if (regardee.aUnMot()) {
				texteCentre(g, Component.translatable("roue.compagnon.dire", regardee.mot()),
						cx, cy - 11, TEXTE_NOUVEAU);
			} else {
				texteCentre(g, Component.translatable("roue.compagnon.apercu"),
						cx, cy - 11, TEXTE_VERROU);
			}
		}

		if (this.entrees.isEmpty()) {
			texteCentre(g, Component.translatable("roue.compagnon.vide"), cx, cy + 4, TEXTE_VERROU);
			return;
		}

		if (nombreDePages() > 1) {
			fleche(g, cx - 30, cy + 8, false, this.survolAvant ? TEXTE_TITRE : TEXTE_VERROU);
			fleche(g, cx + 24, cy + 8, true, this.survolApres ? TEXTE_TITRE : TEXTE_VERROU);
		}
		texteCentre(g, Component.literal((this.page + 1) + " / " + nombreDePages()),
				cx, cy + 7, TEXTE);
	}

	/**
	 * Le bouton sous la roue, et ce qu'il dit une fois arme.
	 *
	 * <p>Arme, il change de couleur et la consigne apparait : sans elle, on ne
	 * saurait pas que le clic suivant ne joue plus le geste.
	 */
	/**
	 * Un onglet par compagnon, au-dessus de la roue.
	 *
	 * <p>Rien du tout quand il n'y en a qu'un : un onglet unique n'apprend rien
	 * a personne et mange de la place au-dessus de la roue.
	 *
	 * <p>Cliquer sur un onglet <b>redemande la roue au serveur</b> plutot que de
	 * changer l'affichage : lui seul sait le niveau de cette bete-la, ce qu'elle
	 * a debloque, et les mots qu'on lui a appris.
	 */
	/**
	 * De zero a un pour chaque onglet : il est en train de s'allumer.
	 *
	 * <p>Les onglets basculaient d'une image a l'autre. Un element qui change
	 * d'etat instantanement a l'air d'un interrupteur ; le meme, en trois
	 * images, a l'air d'un bouton. C'est la seule difference, et c'est toute la
	 * difference.
	 *
	 * <p>Un tableau et non une seule valeur : celui qu'on quitte doit
	 * s'eteindre pendant que le suivant s'allume.
	 */
	private float[] chaleurDesOnglets = new float[0];

	/** Le fond d'un onglet survole, sans etre celui qu'on regarde. */
	private static final int ONGLET_FOND_CHAUD = 0xCC1A1A1A;

	private void onglets(GuiGraphics g, int cx, int cy, int sourisX, int sourisY,
			float partiel) {

		if (this.compagnons.size() < 2) {
			return;
		}
		if (this.chaleurDesOnglets.length != this.compagnons.size()) {
			this.chaleurDesOnglets = new float[this.compagnons.size()];
		}
		int y = ongletY(cy);
		int x = premierOngletX(cx);

		for (int i = 0; i < this.compagnons.size(); i++) {
			int largeur = largeurOnglet(i);
			boolean actif = i == this.index;
			boolean survole = sourisX >= x && sourisX < x + largeur
					&& sourisY >= y && sourisY < y + ONGLET_HAUTEUR;

			this.chaleurDesOnglets[i] = Peinture.vers(this.chaleurDesOnglets[i],
					survole ? 1.0F : 0.0F, 0.30F, partiel);
			g.fill(x, y, x + largeur, y + ONGLET_HAUTEUR,
					actif ? ONGLET_FOND_ACTIF
						: Peinture.melanger(ONGLET_FOND, ONGLET_FOND_CHAUD,
							Peinture.adoucir(this.chaleurDesOnglets[i])));
			g.fill(x, y, x + largeur, y + 1, BOUTON_BORD);
			g.fill(x, y + ONGLET_HAUTEUR - 1, x + largeur, y + ONGLET_HAUTEUR, BOUTON_BORD);
			g.fill(x, y, x + 1, y + ONGLET_HAUTEUR, BOUTON_BORD);
			g.fill(x + largeur - 1, y, x + largeur, y + ONGLET_HAUTEUR, BOUTON_BORD);

			String nom = this.compagnons.get(i);
			g.drawString(this.font, nom, x + (largeur - this.font.width(nom)) / 2,
					y + (ONGLET_HAUTEUR - 8) / 2,
					actif || survole ? TEXTE_TITRE : TEXTE, false);

			x += largeur + ONGLET_ECART;
		}
	}

	private int largeurOnglet(int i) {
		return this.font.width(this.compagnons.get(i)) + ONGLET_MARGE * 2;
	}

	/**
	 * Au-dessus de la roue, mais jamais hors de l'ecran.
	 *
	 * <p>La roue fait 232 pixels de haut. A l'echelle d'interface 4 sur un
	 * ecran ordinaire, la hauteur utile tombe a 270 : l'onglet se serait dessine
	 * <b>au-dessus du bord</b>, invisible et incliquable. La borne coute une
	 * comparaison et evite une interface cassee pour tous ceux qui jouent en
	 * grand.
	 */
	private int ongletY(int cy) {
		return Math.max(BORD, cy - DIAMETRE / 2 - ONGLET_AU_DESSUS);
	}

	/** La rangee est centree : on part de la moitie de sa largeur totale. */
	private int premierOngletX(int cx) {
		int total = 0;
		for (int i = 0; i < this.compagnons.size(); i++) {
			total += largeurOnglet(i) + ONGLET_ECART;
		}
		return cx - (total - ONGLET_ECART) / 2;
	}

	private void boutonApprendre(GuiGraphics g, int cx, int cy, int sourisX, int sourisY) {
		if (this.entrees.isEmpty()) {
			return;
		}
		int x = boutonX(cx);
		int y = boutonY(cy);
		boolean survole = sourisX >= x && sourisX < x + BOUTON_LARGEUR
				&& sourisY >= y && sourisY < y + BOUTON_HAUTEUR;

		// LE BOUTON S'ALLUME PROGRESSIVEMENT, comme partout ailleurs dans le mod.
		//
		// Arme, il reste vert et chaud sans clignoter : le mode d'apprentissage
		// est un etat, pas un survol, et les deux ne doivent pas se confondre.
		this.chaleurDuBouton = Peinture.vers(this.chaleurDuBouton,
			survole || this.modeApprentissage ? 1.0F : 0.0F, 0.30F, 1.0F);
		Peinture.boutonPeint(g, x, y, BOUTON_LARGEUR, BOUTON_HAUTEUR,
			this.chaleurDuBouton, false,
			BOUTON_FOND, this.modeApprentissage ? BOUTON_FOND_ARME : BOUTON_FOND_CHAUD,
			BOUTON_BORD);

		Component texte = Component.translatable(this.modeApprentissage
				? "roue.compagnon.choisis_le_geste"
				: "roue.compagnon.apprendre");
		g.drawString(this.font, texte,
				x + (BOUTON_LARGEUR - this.font.width(texte)) / 2,
				y + (BOUTON_HAUTEUR - 8) / 2,
				survole || this.modeApprentissage ? TEXTE_TITRE : TEXTE, false);
	}

	private static int boutonX(int cx) {
		return cx - BOUTON_LARGEUR / 2;
	}

	/** Sous la roue, et jamais sous le bord. Meme raison que pour les onglets. */
	private int boutonY(int cy) {
		return Math.min(this.height - BORD - BOUTON_HAUTEUR,
				cy + DIAMETRE / 2 + BOUTON_SOUS_LA_ROUE);
	}

	private void texteCentre(GuiGraphics g, Component texte, int cx, int y, int couleur) {
		g.drawString(this.font, texte, cx - this.font.width(texte) / 2, y, couleur, false);
	}

	/**
	 * Demande au compagnon de montrer la case survolee.
	 *
	 * <p>On choisit ce qu'on a <b>vu</b>, pas un nom. C'est aussi le seul moyen de
	 * savoir a quoi ressemble une animation encore verrouillee — et c'est
	 * exactement ce qui donne envie de l'avoir.
	 *
	 * <p>Envoye <b>seulement quand la case change</b>. Sans ce garde, on enverrait
	 * un paquet a chaque image tant que la souris ne bouge pas : soixante par
	 * seconde et par joueur, pour rien.
	 */
	private void demanderLApercu() {
		if (this.survolee == this.apercuEnvoye) {
			return;
		}
		this.apercuEnvoye = this.survolee;

		EntreeRoue entree = this.survolee < 0 ? null : entreeDe(this.survolee);
		if (entree != null) {
			ClientPlayNetworking.send(PaquetAction.survolee(this.index, entree.nom()));
		}
	}

	// --- Ce qu'on peut faire ----------------------------------------------------

	/**
	 * Tab passe d'un compagnon a l'autre, comme dans le livre.
	 *
	 * <p>On redemande au serveur au lieu de deviner : lui seul sait combien de
	 * betes ce joueur possede, et laquelle vient apres. Il ramene l'index dans
	 * ses bornes, donc avec un seul compagnon la roue se contente de se
	 * rouvrir sur lui.
	 */
	@Override
	public boolean keyPressed(int touche, int codeMateriel, int modificateurs) {
		if (touche == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB) {
			ClientPlayNetworking.send(new fr.lhdp.compagnon.reseau.PaquetRoue(this.index + 1));
			return true;
		}
		return super.keyPressed(touche, codeMateriel, modificateurs);
	}

	@Override
	public boolean mouseClicked(double sourisX, double sourisY, int bouton) {
		if (clochetteCliquee(sourisX, sourisY, bouton)) {
			return true;
		}
		int cx = this.width / 2;
		int cy = this.height / 2;

		int choisi = ongletSous(cx, cy, sourisX, sourisY);
		if (choisi >= 0) {
			if (choisi != this.index) {
				Bruits.clic();
				ClientPlayNetworking.send(new fr.lhdp.compagnon.reseau.PaquetRoue(choisi));
			}
			return true;
		}

		if (!this.entrees.isEmpty()
				&& sourisX >= boutonX(cx) && sourisX < boutonX(cx) + BOUTON_LARGEUR
				&& sourisY >= boutonY(cy) && sourisY < boutonY(cy) + BOUTON_HAUTEUR) {
			this.modeApprentissage = !this.modeApprentissage;
			Bruits.clic();
			return true;
		}

		if (this.survolAvant) {
			this.page = Math.floorMod(this.page - 1, nombreDePages());
			Bruits.page();
			return true;
		}
		if (this.survolApres) {
			this.page = Math.floorMod(this.page + 1, nombreDePages());
			Bruits.page();
			return true;
		}

		EntreeRoue entree = this.survolee < 0 ? null : entreeDe(this.survolee);
		// IL FINIT CE QU'IL A COMMENCE. Sauf pour aller lui apprendre un mot,
		// qui n'est pas un ordre et ne coupe rien.
		if (entree != null && entree.debloque() && occupe() && !this.modeApprentissage) {
			refuser(this.survolee);
			return true;
		}
		if (entree != null && entree.debloque()) {
			if (this.modeApprentissage) {
				// On ne joue pas le geste : on va lui apprendre le mot qui le
				// declenchera. Le serveur verifiera tout, y compris que le micro
				// sait dire ce mot.
				Bruits.clic();
				this.minecraft.setScreen(new EcranApprendre(
						this.index, this.nomCompagnon, entree.nom(), entree.mot()));
				return true;
			}
			// On retient combien de temps le geste va durer : c'est la meme duree
			// que celle que le serveur va employer, lue dans le meme fichier.
			if (this.minecraft.level != null) {
				occupeJusqua = this.minecraft.level.getGameTime()
					+ fr.lhdp.compagnon.espece.Longueurs.de(entree.nom());
			}
			Bruits.clic();
			ClientPlayNetworking.send(PaquetAction.choisie(this.index, entree.nom()));
			onClose();
			return true;
		}
		// Sur un cadenas, on ne demande rien au serveur — il refuserait de toute
		// facon — mais on repond au joueur. Un clic sans reponse passe pour un bug.
		if (entree != null) {
			refuser(this.survolee);
			return true;
		}
		return super.mouseClicked(sourisX, sourisY, bouton);
	}

	/** Dit non : un bruit grave, et la case rougit une demi-seconde. */
	private void refuser(int laquelle) {
		this.refusee = laquelle;
		this.chaleurDuRefus = 1.0F;
		Bruits.refus();
	}

	/** L'onglet sous la souris, ou -1. */
	private int ongletSous(int cx, int cy, double sourisX, double sourisY) {
		if (this.compagnons.size() < 2) {
			return -1;
		}
		int y = ongletY(cy);
		if (sourisY < y || sourisY >= y + ONGLET_HAUTEUR) {
			return -1;
		}
		int x = premierOngletX(cx);
		for (int i = 0; i < this.compagnons.size(); i++) {
			int largeur = largeurOnglet(i);
			if (sourisX >= x && sourisX < x + largeur) {
				return i;
			}
			x += largeur + ONGLET_ECART;
		}
		return -1;
	}

	@Override
	public boolean mouseScrolled(double sourisX, double sourisY, double horizontal, double vertical) {
		if (nombreDePages() > 1 && vertical != 0.0) {
			this.page = Math.floorMod(this.page + (vertical > 0 ? -1 : 1), nombreDePages());
			Bruits.page();
			return true;
		}
		return super.mouseScrolled(sourisX, sourisY, horizontal, vertical);
	}
}
