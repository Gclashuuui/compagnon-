package fr.lhdp.compagnon.client;

import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.entite.CompagnonEntity;
import fr.lhdp.compagnon.fiche.Barre;
import fr.lhdp.compagnon.livre.DonneesLivre;
import fr.lhdp.compagnon.livre.EntreeCompetence;
import fr.lhdp.compagnon.reseau.PaquetLivre;
import fr.lhdp.compagnon.reseau.PaquetRoue;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.locale.Language;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Le carnet de suivi du compagnon.
 *
 * <p>Il lit la fiche, pas l'entite : il s'ouvre donc toujours, meme quand le
 * compagnon est reste dans une chambre a l'autre bout du chateau.
 *
 * <p>Ce n'est pas un tableau de statistiques. L'humeur s'ecrit en toutes lettres
 * avec sa bouille, et le bobo dit tout de suite ce qu'il faut pour le regler :
 * le joueur ne doit jamais avoir a taper une commande pour savoir ce qui ne va
 * pas.
 *
 * <p>Tous les textes viennent du fichier de langue. On peut les reformuler sans
 * recompiler, et les accents y sont sans risque.
 */
public class EcranLivre extends EcranCompagnon {

	private static final ResourceLocation FOND = Compagnon.id("textures/gui/livre/book.png");
	private static final ResourceLocation SALISSURES = Compagnon.id("textures/gui/livre/smudges.png");
	private static final ResourceLocation CADRE_PORTRAIT = Compagnon.id("textures/gui/livre/iconbacking.png");
	private static final ResourceLocation SOULIGNEMENT = Compagnon.id("textures/gui/livre/underline.png");
	private static final ResourceLocation FLECHE_GAUCHE = Compagnon.id("textures/gui/livre/pageturnlargeleft.png");
	private static final ResourceLocation FLECHE_DROITE = Compagnon.id("textures/gui/livre/pageturnlargeright.png");

	/**
	 * La texture du livre fait 384 x 256, pas 256 x 256. Il faut donc l'appel long
	 * de {@code blit} — le court supposerait 256 et etirerait l'image.
	 */
	private static final int LARGEUR = 384;
	private static final int HAUTEUR = 256;

	private static final int FLECHE_LARGEUR = 29;
	private static final int FLECHE_HAUTEUR = 28;

	private static final int PORTRAIT = 52;
	private static final int PORTRAIT_SOURCE = 88;
	private static final int SOULIGNEMENT_SOURCE_L = 159;
	private static final int SOULIGNEMENT_SOURCE_H = 11;

	// Les deux pages, en coordonnees relatives au coin du livre.
	private static final int PAGE_GAUCHE_X = 34;
	private static final int PAGE_DROITE_X = 208;
	private static final int PAGE_LARGEUR = 142;
	private static final int PAGE_Y = 28;

	private static final int ENCRE = 0xFF3A2A18;
	private static final int ENCRE_PALE = 0xFF7A6A55;
	private static final int ENCRE_ROUGE = 0xFF8A2F2F;
	private static final int FILET = 0x557A6A55;

	private static final int ONGLET_HAUTEUR = 18;
	private static final int ONGLET_MARGE = 10;
	private static final int ONGLET_ECART = 4;
	private static final int ONGLET_AU_DESSUS = 6;
	private static final int BORD = 4;

	private static final int ONGLET_FOND = 0x99000000;
	private static final int ONGLET_FOND_ACTIF = 0xCC3F6B37;
	private static final int ONGLET_BORD = 0xFFC8BCA4;
	private static final int ONGLET_TEXTE = 0xFFE8E0D0;

	private static final int CADRE = 0xFF5A4632;
	private static final int CREUX_HAUT = 0xFFB9A886;
	private static final int CREUX_BAS = 0xFFD9CBAE;

	/**
	 * En dessous, le livre dit quoi faire au lieu de se contenter du chiffre.
	 *
	 * <p>Quarante sur cent : assez bas pour qu'un conseil soit utile, assez haut
	 * pour prevenir avant que ca aille mal.
	 */
	private static final float SEUIL_DU_CONSEIL = 40.0F;

	/** L'espace minimal entre un libelle et sa valeur, en pixels. */
	/** La derniere page du livre. Trois au total : sante, histoire, competences. */
	/**
	 * Les pages du livre, nommees.
	 *
	 * <h2>Pourquoi elles ont un nom</h2>
	 *
	 * <p>Le test « suis-je sur la page des competences ? » s'ecrivait
	 * {@code page != DERNIERE_PAGE}, ce qui etait vrai tant que les competences
	 * <b>etaient</b> la derniere page. Le jour ou les missions sont arrivees
	 * derriere elles, ce test s'est mis a designer la mauvaise page et
	 * <b>on ne pouvait plus prendre aucune competence</b> — sans rien casser
	 * de visible, sans erreur, sans que rien ne le dise.
	 *
	 * <p>D'ou ces constantes : une page se designe par ce qu'elle est, jamais
	 * par sa position dans la pile.
	 */
	private static final int PAGE_IDENTITE = 0;
	private static final int PAGE_HISTOIRE = 1;
	private static final int PAGE_COMPETENCES = 2;
	private static final int PAGE_MISSIONS = 3;
	private static final int DERNIERE_PAGE = PAGE_MISSIONS;

	/** Hauteur d'une ligne de competence, description comprise. */
	private static final int LIGNE_COMPETENCE = 21;

	private static final int ENCRE_VERTE = 0xFF3F6B37;

	private static final int ECART_COLONNES = 4;

	/** Ce qu'on met a la place de ce qu'on a coupe. */
	private static final String SUITE = "…";

	private static final int HAUTEUR_JAUGE = 8;
	private static final int LIGNE = 11;

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final DonneesLivre donnees;
	private final int combien;
	private final int index;

	/**
	 * Les noms de toutes ses betes, pour les onglets du haut.
	 *
	 * <p>Tab marchait deja, et le bas de page annoncait « compagnon 1 sur 2 » —
	 * mais rien ne disait comment passer au second. Une information qu'il faut
	 * deviner n'est pas une information.
	 */
	private final List<String> compagnons;

	private int gauche;
	private int haut;
	private int page;

	/** L'apercu en trois dimensions ; {@code null} tant qu'on ne l'a pas cree. */
	private CompagnonEntity apercu;

	/** Passe a vrai si le rendu de l'entite echoue : on n'insiste pas. */
	private boolean apercuAbandonne;

	public EcranLivre(DonneesLivre donnees, int combien, int index,
			List<String> compagnons) {

		super(Component.literal(donnees.nom()));
		this.donnees = donnees;
		this.combien = combien;
		this.index = index;
		this.compagnons = compagnons;
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

	@Override
	public void render(GuiGraphics g, int sourisX, int sourisY, float partiel) {
		super.render(g, sourisX, sourisY, partiel);

		g.blit(FOND, this.gauche, this.haut, 0.0F, 0.0F, LARGEUR, HAUTEUR, LARGEUR, HAUTEUR);
		g.blit(SALISSURES, this.gauche, this.haut, 0.0F, 0.0F, LARGEUR, HAUTEUR, LARGEUR, HAUTEUR);

		this.missionSurvolee = -1;
		onglets(g, sourisX, sourisY, partiel);

		if (this.page == PAGE_IDENTITE) {
			pageIdentite(g, sourisX, sourisY);
			pageSante(g);
		} else if (this.page == PAGE_HISTOIRE) {
			pageHistoire(g);
			pageMoments(g);
		} else if (this.page == PAGE_COMPETENCES) {
			pageCompetences(g, sourisX, sourisY);
		} else {
			pageMissions(g, sourisX, sourisY);
		}

		bordDePage(g);
		clochette(g, sourisX, sourisY, partiel);
	}

	// --- Page 1, a gauche : qui il est, et comment il va ------------------------

	private void pageIdentite(GuiGraphics g, int sourisX, int sourisY) {
		int x = this.gauche + PAGE_GAUCHE_X;
		int y = this.haut + PAGE_Y;

		portrait(g, x, y, sourisX, sourisY);

		int texteX = x + PORTRAIT + 8;
		int largeurTexte = PAGE_LARGEUR - PORTRAIT - 8;

		g.drawString(this.font, this.donnees.nom(), texteX, y + 6, ENCRE, false);
		g.blit(SOULIGNEMENT, texteX, y + 16, largeurTexte, 4,
				0.0F, 0.0F, SOULIGNEMENT_SOURCE_L, SOULIGNEMENT_SOURCE_H,
				SOULIGNEMENT_SOURCE_L, SOULIGNEMENT_SOURCE_H);

		g.drawString(this.font,
				fr.lhdp.compagnon.espece.Especes.titre(this.donnees.espece())
					+ " · " + this.donnees.variante(),
				texteX, y + 23, ENCRE_PALE, false);
		g.drawString(this.font, Component.translatable(this.donnees.mode()),
				texteX, y + 35, ENCRE_PALE, false);

		// PEUT-ON LE MONTER ?
		//
		// C'est la seule chose qu'un joueur d'oiseau bleu veut savoir, et le livre
		// ne la disait nulle part : il fallait essayer pour decouvrir qu'il
		// refusait, sans jamais apprendre a partir de quand il accepterait.
		//
		// Une espece qui ne se monte pas n'affiche rien du tout : mieux vaut le
		// silence qu'une ligne qui dit non.
		if (this.donnees.monterAuNiveau() > 0) {
			boolean maintenant = this.donnees.niveau() >= this.donnees.monterAuNiveau();
			g.drawString(this.font, maintenant
					? Component.translatable("livre.compagnon.se_monte")
					: Component.translatable("livre.compagnon.se_monte_au",
						this.donnees.monterAuNiveau()),
				texteX, y + 47, maintenant ? ENCRE_VERTE : ENCRE_PALE, false);
		}

		y += PORTRAIT + 10;
		filet(g, x, y);
		y += 7;

		// Niveau et experience.
		String titreNiveau = Component.translatable("livre.compagnon.niveau",
				this.donnees.niveau(), this.donnees.niveauMax()).getString();
		String xp = this.donnees.xpDuSuivant() < 0
				? Component.translatable("livre.compagnon.xp_max", this.donnees.xp()).getString()
						: Component.translatable("livre.compagnon.xp",
					this.donnees.xp(), this.donnees.xpDuSuivant()).getString();
		deuxColonnes(g, x, y, titreNiveau, xp, ENCRE, ENCRE_PALE);

		y += LIGNE + 1;
		jauge(g, x, y, this.donnees.avancementDuNiveau(), 0xFF3E7BB5);

		y += HAUTEUR_JAUGE + 8;

		y += barre(g, x, y, "livre.compagnon.faim", Barre.FAIM, 0xFFD07A2E);
		y += barre(g, x, y, "livre.compagnon.energie", Barre.ENERGIE, 0xFFD4B02A);
		y += barre(g, x, y, "livre.compagnon.complicite", Barre.COMPLICITE, 0xFFC9569A);
		y += barre(g, x, y, "livre.compagnon.sante", Barre.SANTE, 0xFFC04040);
		y += 3;

		filet(g, x, y);
		y += 6;

		// L'humeur, en toutes lettres avec sa bouille. Jamais en jauge.
		g.drawString(this.font, this.donnees.humeurBouille(), x, y, ENCRE_PALE, false);
		g.drawString(this.font, this.donnees.humeurLibelle(),
				x + this.font.width(this.donnees.humeurBouille()) + 6, y, ENCRE, false);
	}

	/**
	 * Une barre, et — si elle est basse — ce qu'il faut faire.
	 *
	 * <h2>Constater ne suffit pas</h2>
	 *
	 * <p>« Complicite 42 » ne dit rien a personne. Le joueur voit bien que le
	 * chiffre est bas, et n'a aucun moyen de savoir ce qui le ferait monter — il
	 * faut l'avoir lu quelque part, ou l'avoir devine.
	 *
	 * <p>Le mod, lui, le sait. Il ne le disait simplement a personne.
	 *
	 * <p>Le conseil n'apparait que <b>sous le seuil</b> : une page couverte de
	 * conseils quand tout va bien ne serait plus une fiche de sante, ce serait
	 * un mode d'emploi.
	 *
	 * @return la hauteur prise, conseil compris
	 */
	private int barre(GuiGraphics g, int x, int y, String cle, Barre barre, int couleur) {
		float valeur = this.donnees.barres().getOrDefault(barre, 0.0F);
		deuxColonnes(g, x, y, Component.translatable(cle).getString(),
				String.valueOf(Math.round(valeur)), ENCRE_PALE, ENCRE_PALE);
		jauge(g, x, y + LIGNE, valeur / Barre.MAXIMUM, couleur);

		int pris = LIGNE + HAUTEUR_JAUGE + 3;
		if (valeur > SEUIL_DU_CONSEIL) {
			return pris;
		}
		Component conseil = Component.translatable(cle + ".conseil");
		g.drawString(this.font, conseil, x + 2, y + LIGNE + HAUTEUR_JAUGE + 2,
				ENCRE_ROUGE, false);
		return pris + 9;
	}

	/**
	 * Une jauge creusee dans le papier.
	 *
	 * <p>Le creux va du sombre en haut au clair en bas — c'est ce qui donne
	 * l'illusion d'un sillon. Le remplissage fait l'inverse, avec un trait de
	 * lumiere sur le dessus : il a l'air bombe, donc pose par-dessus.
	 *
	 * <p>Les quatre coins sont laisses vides, ce qui suffit a arrondir l'oeil a
	 * cette taille.
	 */
	/**
	 * Un libelle a gauche, une valeur a droite, et jamais l'un sur l'autre.
	 *
	 * <h2>Le defaut que ceci supprime</h2>
	 *
	 * <p>Chaque ligne a deux colonnes de ce livre etait ecrite deux fois : le
	 * libelle depuis la marge gauche, la valeur calee sur la marge droite. Tant
	 * que le libelle etait court, tout allait bien.
	 *
	 * <p>« Il a passe un moment avec un autre compagnon » ne l'est pas, et la
	 * date se dessinait <b>par-dessus la phrase</b>. Illisible, et sur une page
	 * de parchemin ca a simplement l'air casse.
	 *
	 * <p>La valeur est prioritaire — une date tronquee ne veut plus rien dire —
	 * donc c'est le libelle qui cede, avec des points de suspension pour qu'on
	 * voie qu'il continue. Quatre pixels restent toujours entre les deux : deux
	 * textes qui se touchent se lisent presque aussi mal que deux textes qui se
	 * chevauchent.
	 *
	 * <p><b>Les quatre lignes a deux colonnes du livre passent par ici</b> — le
	 * niveau, les barres, les compteurs et les moments. C'est ce qui fait que le
	 * defaut ne peut pas revenir a un endroit qu'on aurait oublie.
	 */
	private void deuxColonnes(GuiGraphics g, int x, int y, String gauche, String droite,
			int couleurGauche, int couleurDroite) {

		int placeDeLaValeur = this.font.width(droite);
		int placeDuLibelle = PAGE_LARGEUR - placeDeLaValeur - ECART_COLONNES;

		String coupe = gauche;
		if (placeDuLibelle > 0 && this.font.width(gauche) > placeDuLibelle) {
			// On reserve la place des points AVANT de couper, sinon ils depassent a
			// leur tour et on n'a rien regle.
			coupe = this.font.plainSubstrByWidth(gauche,
				Math.max(0, placeDuLibelle - this.font.width(SUITE))) + SUITE;
		}
		g.drawString(this.font, coupe, x, y, couleurGauche, false);
		g.drawString(this.font, droite, x + PAGE_LARGEUR - placeDeLaValeur, y,
				couleurDroite, false);
	}

	private void jauge(GuiGraphics g, int x, int y, float part, int couleur) {
		int l = PAGE_LARGEUR;
		int h = HAUTEUR_JAUGE;

		g.fill(x + 1, y, x + l - 1, y + 1, CADRE);
		g.fill(x + 1, y + h - 1, x + l - 1, y + h, CADRE);
		g.fill(x, y + 1, x + 1, y + h - 1, CADRE);
		g.fill(x + l - 1, y + 1, x + l, y + h - 1, CADRE);

		g.fillGradient(x + 1, y + 1, x + l - 1, y + h - 1, CREUX_HAUT, CREUX_BAS);

		int rempli = Math.round((l - 2) * Math.max(0.0F, Math.min(1.0F, part)));
		if (rempli <= 0) {
			return;
		}

		int x2 = x + 1 + rempli;
		int milieu = y + 1 + (h - 2) / 2;
		g.fillGradient(x + 1, y + 1, x2, milieu, eclaircir(couleur, 0.40F), couleur);
		g.fillGradient(x + 1, milieu, x2, y + h - 1, couleur, assombrir(couleur, 0.35F));
		g.fill(x + 1, y + 1, x2, y + 2, eclaircir(couleur, 0.70F));
	}

	/**
	 * L'apercu du compagnon. Le rendu d'une entite dans une interface est un
	 * travail a part : s'il echoue, on garde le cadre et on continue, plutot que
	 * de faire tomber tout le livre.
	 */
	private void portrait(GuiGraphics g, int x, int y, int sourisX, int sourisY) {
		g.blit(CADRE_PORTRAIT, x, y, PORTRAIT, PORTRAIT,
				0.0F, 0.0F, PORTRAIT_SOURCE, PORTRAIT_SOURCE, PORTRAIT_SOURCE, PORTRAIT_SOURCE);

		if (this.apercuAbandonne || this.minecraft == null || this.minecraft.level == null) {
			return;
		}

		if (this.apercu == null) {
			this.apercu = Compagnon.COMPAGNON.create(this.minecraft.level);
			if (this.apercu == null) {
				this.apercuAbandonne = true;
				return;
			}
			this.apercu.setEspece(this.donnees.espece());
			this.apercu.setVariante(this.donnees.variante());
		}

		int marge = 5;
		try {
			InventoryScreen.renderEntityInInventoryFollowsMouse(g,
					x + marge, y + marge, x + PORTRAIT - marge, y + PORTRAIT - marge,
					16, 0.0625F, sourisX, sourisY, this.apercu);
		} catch (Exception echec) {
			this.apercuAbandonne = true;
			Compagnon.LOG.warn("Apercu du compagnon impossible dans le livre : {}", echec.toString());
		}
	}

	// --- Page 1, a droite : ce qui ne va pas, et ce qu'on a fait ----------------

	private void pageSante(GuiGraphics g) {
		int x = this.gauche + PAGE_DROITE_X;
		int y = this.haut + PAGE_Y;

		if (!this.donnees.aUnBobo()) {
			g.drawString(this.font, Component.translatable("livre.compagnon.va_bien"),
					x, y, ENCRE, false);
			y += LIGNE;
			g.drawString(this.font, Component.translatable("livre.compagnon.rien_a_soigner"),
					x, y, ENCRE_PALE, false);
			y += LIGNE + 8;
		} else {
			g.drawString(this.font, this.donnees.boboNom(), x, y, ENCRE_ROUGE, false);
			y += LIGNE + 2;

			for (String ligne : decouper(this.donnees.boboDescription())) {
				g.drawString(this.font, ligne, x, y, ENCRE_PALE, false);
				y += LIGNE;
			}

			y += 6;
			g.drawString(this.font, Component.translatable("livre.compagnon.il_lui_faut"),
					x, y, ENCRE, false);
			y += LIGNE;
			g.drawString(this.font, this.donnees.boboRemede(), x, y, ENCRE_ROUGE, false);
			y += LIGNE + 8;
		}

		filet(g, x, y);
		y += 7;

		g.drawString(this.font, Component.translatable("livre.compagnon.compteurs"), x, y, ENCRE, false);
		y += LIGNE + 2;
		compteurs(g, x, y);
	}

	private void compteurs(GuiGraphics g, int x, int y) {
		if (this.donnees.compteurs().isEmpty()) {
			g.drawString(this.font, Component.translatable("livre.compagnon.rien_encore"),
					x, y, ENCRE_PALE, false);
			return;
		}
		for (Map.Entry<String, Integer> compteur : this.donnees.compteurs().entrySet()) {
			if (y > this.haut + HAUTEUR - 48) {
				break;
			}
			deuxColonnes(g, x, y, libelleCompteur(compteur.getKey()),
					String.valueOf(compteur.getValue()), ENCRE_PALE, ENCRE);
			y += LIGNE;
		}
	}

	// --- Page 2 : l'histoire ----------------------------------------------------

	private void pageHistoire(GuiGraphics g) {
		int x = this.gauche + PAGE_GAUCHE_X;
		int y = this.haut + PAGE_Y;

		g.drawString(this.font, Component.translatable("livre.compagnon.ensemble_depuis"),
				x, y, ENCRE, false);
		y += LIGNE;
		// La date, ET depuis combien de jours. « 23 jours » se ressent, une date
		// seule demande un calcul mental que personne ne fait.
		String quand = Instant.ofEpochMilli(this.donnees.dateObtention())
				.atZone(ZoneId.systemDefault()).format(DATE);
		g.drawString(this.font, quand, x, y, ENCRE_PALE, false);
		y += LIGNE;
		long jours = joursDepuis(this.donnees.dateObtention());
		g.drawString(this.font, Component.translatable("livre.compagnon.depuis_jours", jours),
				x, y, ENCRE_PALE, false);
		y += LIGNE;

		// LE TEMPS SE DIT, IL NE SE COMPTE PAS.
		//
		// « 47 jours » est un chiffre ; « vous vous connaissez bien maintenant »
		// est une phrase, et c'est elle qu'on relit. La bete ne grandira jamais —
		// c'est voulu — mais le temps doit se voir quelque part.
		g.drawString(this.font, Component.translatable(ageEnMots(jours)),
				x, y, ENCRE, false);
		y += LIGNE + 8;

		filet(g, x, y);
		y += 7;

		g.drawString(this.font, Component.translatable("livre.compagnon.temps_ensemble"),
				x, y, ENCRE, false);
		y += LIGNE;
		g.drawString(this.font, duree(this.donnees.ticksEnsemble()), x, y, ENCRE_PALE, false);
		y += LIGNE + 4;

		objetPrefere(g, x, y);
		y += 20;

		filet(g, x, y);
		y += 7;

		quiIlConnait(g, x, y);
	}

	/**
	 * Les gens et les betes qu'il connait.
	 *
	 * <p>Ces noms existaient depuis longtemps dans la fiche, et rien ne les
	 * montrait. C'est pourtant ce qui donne une existence aux amities : sans cette
	 * page, un compagnon qui va jouer avec un autre le fait sans qu'on sache
	 * pourquoi.
	 */
	private void quiIlConnait(GuiGraphics g, int x, int y) {
		g.drawString(this.font, Component.translatable("livre.compagnon.connait"), x, y, ENCRE, false);
		y += LIGNE;

		if (this.donnees.connait().isEmpty()) {
			g.drawString(this.font, Component.translatable("livre.compagnon.personne"),
					x, y, ENCRE_PALE, false);
			return;
		}

		// Sur plusieurs lignes : a vingt connaissances, une seule debordait la page.
		for (var ligne : this.font.split(
				Component.literal(String.join(", ", this.donnees.connait())), PAGE_LARGEUR)) {
			if (y > this.haut + HAUTEUR - 48) {
				break;
			}
			g.drawString(this.font, ligne, x, y, ENCRE_PALE, false);
			y += LIGNE;
		}
	}

	/**
	 * Les mots qu'on lui a appris.
	 *
	 * <p>Rien du tout quand il n'en connait aucun : une rubrique vide occuperait
	 * la place des moments pour ne rien dire. Le jour ou il apprend son premier
	 * mot, la rubrique apparait — et c'est une bonne facon de decouvrir que
	 * c'est possible.
	 *
	 * @return la hauteur ou continuer
	 */
	private int cequIlSaitFaire(GuiGraphics g, int x, int y) {
		if (this.donnees.motsAppris().isEmpty()) {
			return y;
		}
		g.drawString(this.font, Component.translatable("livre.compagnon.sait_faire"),
				x, y, ENCRE, false);
		y += LIGNE + 1;

		for (Map.Entry<String, String> appris : this.donnees.motsAppris().entrySet()) {
			if (y > this.haut + HAUTEUR - 96) {
				break;
			}
			// Le mot a gauche, entre guillemets — c'est lui qu'on prononce. Le geste
			// a droite, en pale : il ne sert qu'a se rappeler ce que le mot declenche.
			deuxColonnes(g, x, y, "« " + appris.getKey() + " »",
					etiquette(appris.getValue()), ENCRE, ENCRE_PALE);
			y += LIGNE;
		}
		y += 4;
		filet(g, x, y);
		return y + 7;
	}

	/**
	 * « Ce qu'il est. »
	 *
	 * <p>Sur les deux pages a la fois : les competences sont longues a lire, et
	 * les serrer dans une colonne de cent quarante pixels les rendrait
	 * illisibles. C'est la seule page du livre qui s'etale ainsi, et c'est
	 * justifie — on y prend une decision qu'on ne pourra pas reprendre.
	 *
	 * <p>Tout est montre, y compris ce qu'il ne peut pas encore prendre. Montrer
	 * ce qui existe fait plus pour l'envie que de le cacher : c'est le meme
	 * raisonnement que les cadenas de la roue.
	 */
	private void pageCompetences(GuiGraphics g, int sourisX, int sourisY) {
		int x = this.gauche + PAGE_GAUCHE_X;
		int y = this.haut + PAGE_Y;
		int large = PAGE_DROITE_X - PAGE_GAUCHE_X + PAGE_LARGEUR;

		g.drawString(this.font, Component.translatable("livre.compagnon.competences"),
				x, y, ENCRE, false);

		int points = this.donnees.pointsDeCompetence();
		Component compte = points > 0
				? Component.translatable("livre.compagnon.competence.points", points)
				: Component.translatable("livre.compagnon.competence.aucun_point");
		g.drawString(this.font, compte, x + large - this.font.width(compte), y,
				points > 0 ? ENCRE_VERTE : ENCRE_PALE, false);
		y += LIGNE + 4;

		if (this.donnees.competences().isEmpty()) {
			g.drawString(this.font, Component.translatable("livre.compagnon.competence.vide"),
				x, y, ENCRE_PALE, false);
			return;
		}

		for (EntreeCompetence competence : this.donnees.competences()) {
			if (y > this.haut + HAUTEUR - 56) {
				break;
			}
			ligneDeCompetence(g, x, y, large, competence, points, sourisX, sourisY);
			y += LIGNE_COMPETENCE;
		}
	}

	/**
	 * Une competence : son nom, ce qu'elle fait, et son etat.
	 *
	 * <p>Trois etats, trois couleurs, et aucun texte pour les expliquer : prise
	 * en encre verte, a portee en encre franche, hors de portee en pale avec son
	 * palier. On voit l'etat d'une page entiere sans lire un mot.
	 */
	private void ligneDeCompetence(GuiGraphics g, int x, int y, int large,
			EntreeCompetence competence, int points, int sourisX, int sourisY) {

		boolean ouverte = this.donnees.niveau() >= competence.niveauRequis();
		boolean prenable = !competence.prise() && ouverte && points > 0;
		boolean survolee = prenable && sourisX >= x && sourisX < x + large
				&& sourisY >= y - 2 && sourisY < y + LIGNE_COMPETENCE - 4;

		if (survolee) {
			g.fill(x - 2, y - 2, x + large, y + LIGNE_COMPETENCE - 4, 0x18000000);
		}

		int couleur = competence.prise() ? ENCRE_VERTE : ouverte ? ENCRE : ENCRE_PALE;
		g.drawString(this.font, competence.nom(), x, y, couleur, false);

		if (!competence.prise() && !ouverte) {
			Component requis = Component.translatable(
				"livre.compagnon.competence.requis", competence.niveauRequis());
			g.drawString(this.font, requis, x + large - this.font.width(requis), y,
				ENCRE_PALE, false);
		}
		g.drawString(this.font,
				this.font.plainSubstrByWidth(competence.description(), large),
				x + 4, y + 10, ENCRE_PALE, false);
	}

	/** La competence sous la souris, si on peut la prendre. */
	private EntreeCompetence competenceSous(double sourisX, double sourisY) {
		if (this.page != PAGE_COMPETENCES || this.donnees.pointsDeCompetence() <= 0) {
			return null;
		}
		int x = this.gauche + PAGE_GAUCHE_X;
		int large = PAGE_DROITE_X - PAGE_GAUCHE_X + PAGE_LARGEUR;
		int y = this.haut + PAGE_Y + LIGNE + 4;

		for (EntreeCompetence competence : this.donnees.competences()) {
			if (y > this.haut + HAUTEUR - 56) {
				break;
			}
			boolean dessus = sourisX >= x && sourisX < x + large
					&& sourisY >= y - 2 && sourisY < y + LIGNE_COMPETENCE - 4;
			if (dessus && !competence.prise()
					&& this.donnees.niveau() >= competence.niveauRequis()) {
				return competence;
			}
			y += LIGNE_COMPETENCE;
		}
		return null;
	}

	private void pageMoments(GuiGraphics g) {
		int x = this.gauche + PAGE_DROITE_X;
		int y = this.haut + PAGE_Y;

		// CE QU'IL SAIT FAIRE, d'abord.
		//
		// La roue montre les mots un par un, sous leur geste. Il manquait la vue
		// d'ensemble : ce que CETTE bete-la sait, en une fois. C'est aussi ce qui
		// donne envie de lui en apprendre d'autres.
		y = cequIlSaitFaire(g, x, y);

		g.drawString(this.font, Component.translatable("livre.compagnon.moments"), x, y, ENCRE, false);
		y += LIGNE + 2;

		if (this.donnees.moments().isEmpty()) {
			g.drawString(this.font, Component.translatable("livre.compagnon.rien_encore"),
					x, y, ENCRE_PALE, false);
			return;
		}

		// DU PLUS RECENT AU PLUS ANCIEN, et c'est un correctif.
		//
		// Les moments sont ranges dans l'ordre ou ils arrivent, et la page s'arrete
		// des qu'elle est pleine. On affichait donc les PREMIERS souvenirs, et les
		// nouveaux tombaient hors de la page. Au bout de quelques semaines, un
		// compagnon n'aurait plus montre que ses tout debuts, et on aurait cru que
		// le livre avait cesse de le suivre.
		List<fr.lhdp.compagnon.fiche.Moment> recents =
				new java.util.ArrayList<>(this.donnees.moments());
		java.util.Collections.reverse(recents);

		for (var moment : recents) {
			if (y > this.haut + HAUTEUR - 48) {
				break;
			}
			String quand = Instant.ofEpochMilli(moment.date())
					.atZone(ZoneId.systemDefault()).format(DATE);
			deuxColonnes(g, x, y, libelleMoment(moment.cle()), quand, ENCRE, ENCRE_PALE);
			y += LIGNE;
		}
	}

	// --- Page 4 : ses missions ------------------------------------------------

	/**
	 * Ce qu'il y a a vivre avec lui en ce moment.
	 *
	 * <h2>Elles ne s'acceptent pas</h2>
	 *
	 * <p>Aucun bouton pour les prendre : elles sont la, et elles se cochent
	 * toutes seules. La difference n'est pas cosmetique — une mission qu'on
	 * accepte devient une tache, une mission qu'on decouvre avoir accomplie
	 * reste un souvenir.
	 *
	 * <h2>Rien ne se chevauche</h2>
	 *
	 * <p>L'enonce est decoupe a la largeur de la page, la barre a sa ligne, et
	 * la page s'arrete quand elle est pleine. C'est la regle qu'on s'est donnee
	 * pour tout le livre : deux textes ne se superposent jamais.
	 */
	private void pageMissions(GuiGraphics g, int sourisX, int sourisY) {
		int gaucheX = this.gauche + PAGE_GAUCHE_X;
		int droiteX = this.gauche + PAGE_DROITE_X;
		int y = this.haut + PAGE_Y;

		g.drawString(this.font, Component.translatable("livre.compagnon.missions"),
			gaucheX, y, ENCRE, false);
		int courtesY = y + LIGNE + 2;

		boolean aucune = true;
		int place = 0;
		for (var mission : this.donnees.missions()) {
			if (mission.longue()) {
				continue;
			}
			if (courtesY > this.haut + HAUTEUR - 56 || place >= fr.lhdp.compagnon.mission.Carnet.COMBIEN) {
				break;
			}
			int debut = courtesY;
			boolean survolee = !mission.finie() && this.donnees.peutEcarter()
				&& sourisX >= gaucheX && sourisX < gaucheX + PAGE_LARGEUR;
			courtesY = uneMission(g, gaucheX, courtesY, PAGE_LARGEUR, mission);
			// LE SURVOL, dessine APRES le texte et en transparent : un aplat pose
			// avant serait recouvert, un aplat opaque effacerait l'enonce.
			if (survolee && sourisY >= debut - 2 && sourisY < courtesY - 4) {
				g.fill(gaucheX - 3, debut - 2, gaucheX + PAGE_LARGEUR, courtesY - 4,
					SURVOL_MISSION);
				this.missionSurvolee = place;
			}
			place++;
			aucune = false;
		}
		if (aucune) {
			g.drawString(this.font,
				Component.translatable("livre.compagnon.aucune_mission"),
				gaucheX, courtesY, ENCRE_PALE, false);
		}

		// LA MISSION DE FOND, sur l'autre page. Elle court sur des semaines et
		// n'a rien a faire au milieu de celles de trois jours : la separer est ce
		// qui fait comprendre, sans un mot, qu'elle n'est pas du meme ordre.
		int longueY = this.haut + PAGE_Y;
		g.drawString(this.font,
			Component.translatable("livre.compagnon.mission_longue"),
			droiteX, longueY, ENCRE, false);
		longueY += LIGNE + 2;

		boolean aucuneLongue = true;
		for (var mission : this.donnees.missions()) {
			if (!mission.longue()) {
				continue;
			}
			longueY = uneMission(g, droiteX, longueY, PAGE_LARGEUR, mission);
			aucuneLongue = false;
		}
		if (aucuneLongue) {
			g.drawString(this.font,
				Component.translatable("livre.compagnon.aucune_mission"),
				droiteX, longueY, ENCRE_PALE, false);
		}

		// L'INVITE, en bas a gauche. Elle dit ce qu'on peut faire, ou pourquoi on
		// ne peut plus : une invite qui disparait sans explication passe pour un
		// bug, une invite qui se grise s'explique toute seule.
		g.drawString(this.font, Component.translatable(this.donnees.peutEcarter()
				? "livre.compagnon.ecarter" : "livre.compagnon.plus_decarter"),
			gaucheX, this.haut + HAUTEUR - 52, ENCRE_PALE, false);
	}

	/**
	 * La mission courte sous la souris, ou {@code -1}.
	 *
	 * <p>Renseignee pendant le dessin : les enonces vont a la ligne, leur
	 * hauteur depend du texte, et la recalculer ici reviendrait a redessiner la
	 * page en double pour trouver la meme chose.
	 */
	private int missionSurvolee = -1;

	/** Le voile pose sur la mission survolee. Transparent, pour ne rien effacer. */
	private static final int SURVOL_MISSION = 0x22000000;

	/**
	 * Une mission : son enonce, puis sa barre. Rend l'ordonnee suivante.
	 *
	 * <p>Une mission finie n'affiche pas de barre pleine mais une coche : une
	 * barre pleine se confond avec une barre presque pleine, et on se demande
	 * si c'est fait ou pas.
	 */
	private int uneMission(GuiGraphics g, int x, int y, int large,
			fr.lhdp.compagnon.livre.EntreeMission mission) {

		Component enonce = Component.translatable(mission.texte(), mission.quantite());
		for (var morceau : this.font.split(enonce, large)) {
			g.drawString(this.font, morceau, x, y,
				mission.finie() ? ENCRE_PALE : ENCRE, false);
			y += LIGNE - 4;
		}

		if (mission.finie()) {
			g.drawString(this.font,
				Component.translatable("livre.compagnon.mission_faite"),
				x, y, ENCRE_VERTE, false);
			return y + LIGNE + 2;
		}

		// La barre, puis le compte a droite. Les deux sur la meme ligne mais
		// jamais l'un sur l'autre : la barre s'arrete la ou le texte commence.
		String compte = mission.faits() + " / " + mission.quantite();
		int placeDuCompte = this.font.width(compte) + 6;
		int largeBarre = Math.max(10, large - placeDuCompte);

		g.fill(x, y + 2, x + largeBarre, y + 7, CREUX_HAUT);
		int rempli = Math.round(largeBarre * mission.part());
		if (rempli > 0) {
			g.fill(x, y + 2, x + rempli, y + 7, ENCRE_VERTE);
		}
		g.drawString(this.font, compte, x + large - this.font.width(compte), y,
			ENCRE_PALE, false);
		return y + LIGNE + 2;
	}

	// --- Navigation -------------------------------------------------------------

	private void bordDePage(GuiGraphics g) {
		int y = this.haut + HAUTEUR - 44;

		if (this.page > 0) {
			g.blit(FLECHE_GAUCHE, this.gauche + 44, y, 0.0F, 0.0F,
					FLECHE_LARGEUR, FLECHE_HAUTEUR, FLECHE_LARGEUR, FLECHE_HAUTEUR);
		}
		if (this.page < DERNIERE_PAGE) {
			g.blit(FLECHE_DROITE, this.gauche + LARGEUR - 73, y, 0.0F, 0.0F,
					FLECHE_LARGEUR, FLECHE_HAUTEUR, FLECHE_LARGEUR, FLECHE_HAUTEUR);
		}

		if (this.combien > 1) {
			Component compte = Component.translatable("livre.compagnon.compte",
					this.index + 1, this.combien);
			g.drawString(this.font, compte,
					this.gauche + LARGEUR / 2 - this.font.width(compte) / 2,
					this.haut + HAUTEUR - 30, ENCRE_PALE, false);
		}
	}

	@Override
	public boolean mouseClicked(double sourisX, double sourisY, int bouton) {
		if (clochetteCliquee(sourisX, sourisY, bouton)) {
			return true;
		}
		int onglet = ongletSous(sourisX, sourisY);
		if (onglet >= 0) {
			if (onglet != this.index) {
				Bruits.page();
				ClientPlayNetworking.send(new PaquetLivre(onglet));
			}
			return true;
		}

		// ECARTER UNE MISSION. Le serveur decide s'il reste un droit de rejet :
		// on lui demande, il repond en renvoyant le livre.
		if (this.page == PAGE_MISSIONS && this.missionSurvolee >= 0) {
			Bruits.clic();
			ClientPlayNetworking.send(new fr.lhdp.compagnon.reseau.PaquetEcarter(
				this.index, this.missionSurvolee));
			return true;
		}

		EntreeCompetence choisie = competenceSous(sourisX, sourisY);
		if (choisie != null) {
			// Le serveur decide. Il renverra la page a jour, prise ou refusee.
			Bruits.clic();
			ClientPlayNetworking.send(
				new fr.lhdp.compagnon.reseau.PaquetCompetence(this.index, choisie.id()));
			return true;
		}

		int y = this.haut + HAUTEUR - 44;
		if (sourisY >= y && sourisY <= y + FLECHE_HAUTEUR) {
			if (this.page > 0 && dansX(sourisX, this.gauche + 44)) {
				this.page--;
				Bruits.page();
				return true;
			}
			if (this.page < DERNIERE_PAGE && dansX(sourisX, this.gauche + LARGEUR - 73)) {
				this.page++;
				Bruits.page();
				return true;
			}
		}
		return super.mouseClicked(sourisX, sourisY, bouton);
	}

	/**
	 * Un onglet par compagnon, au-dessus du livre.
	 *
	 * <p>Memes mesures et memes couleurs que dans la roue : ce sont les memes
	 * betes, et passer d'un ecran a l'autre ne doit rien deplacer.
	 *
	 * <p>Cliquer redemande le livre au serveur plutot que de changer
	 * l'affichage : lui seul sait ce qu'il y a dans la fiche de cette bete-la.
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

	private void onglets(GuiGraphics g, int sourisX, int sourisY, float partiel) {
		if (this.compagnons.size() < 2) {
			return;
		}
		if (this.chaleurDesOnglets.length != this.compagnons.size()) {
			this.chaleurDesOnglets = new float[this.compagnons.size()];
		}
		int y = ongletY();
		int x = premierOngletX();

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
			g.fill(x, y, x + largeur, y + 1, ONGLET_BORD);
			g.fill(x, y + ONGLET_HAUTEUR - 1, x + largeur, y + ONGLET_HAUTEUR, ONGLET_BORD);
			g.fill(x, y, x + 1, y + ONGLET_HAUTEUR, ONGLET_BORD);
			g.fill(x + largeur - 1, y, x + largeur, y + ONGLET_HAUTEUR, ONGLET_BORD);

			String nom = this.compagnons.get(i);
			g.drawString(this.font, nom, x + (largeur - this.font.width(nom)) / 2,
					y + (ONGLET_HAUTEUR - 8) / 2,
					actif || survole ? 0xFFFFFFFF : ONGLET_TEXTE, false);

			x += largeur + ONGLET_ECART;
		}
	}

	private int largeurOnglet(int i) {
		return this.font.width(this.compagnons.get(i)) + ONGLET_MARGE * 2;
	}

	/** Au-dessus du livre, mais jamais hors de l'ecran. */
	private int ongletY() {
		return Math.max(BORD, this.haut - ONGLET_HAUTEUR - ONGLET_AU_DESSUS);
	}

	/** La rangee est centree sur le livre. */
	private int premierOngletX() {
		int total = 0;
		for (int i = 0; i < this.compagnons.size(); i++) {
			total += largeurOnglet(i) + ONGLET_ECART;
		}
		return this.gauche + LARGEUR / 2 - (total - ONGLET_ECART) / 2;
	}

	/** L'onglet sous la souris, ou -1. */
	private int ongletSous(double sourisX, double sourisY) {
		if (this.compagnons.size() < 2) {
			return -1;
		}
		int y = ongletY();
		if (sourisY < y || sourisY >= y + ONGLET_HAUTEUR) {
			return -1;
		}
		int x = premierOngletX();
		for (int i = 0; i < this.compagnons.size(); i++) {
			int largeur = largeurOnglet(i);
			if (sourisX >= x && sourisX < x + largeur) {
				return i;
			}
			x += largeur + ONGLET_ECART;
		}
		return -1;
	}

	private boolean dansX(double sourisX, int x) {
		return sourisX >= x && sourisX <= x + FLECHE_LARGEUR;
	}

	@Override
	public boolean keyPressed(int touche, int codeMateriel, int modificateurs) {
		// Passer d'un compagnon a l'autre : on redemande au serveur, qui reste
		// seul juge de ce que ce joueur a le droit de voir.
		if (this.combien > 1 && touche == GLFW.GLFW_KEY_TAB) {
			Bruits.page();
			ClientPlayNetworking.send(new PaquetLivre(this.index + 1));
			return true;
		}

		// La roue s'ouvre aussi depuis le livre.
		if (touche == GLFW.GLFW_KEY_R) {
			Bruits.clic();
			ClientPlayNetworking.send(new PaquetRoue(this.index));
			return true;
		}
		return super.keyPressed(touche, codeMateriel, modificateurs);
	}

	/**
	 * Le texte d'un moment.
	 *
	 * <p>Si le fichier de langue le traduit, on prend la traduction — c'est ainsi
	 * qu'un reve s'ecrit en toutes lettres. Sinon on nettoie l'identifiant, ce qui
	 * reste lisible pour un moment qu'on vient d'ajouter.
	 */
	/** Prefixe de la cle qui range son objet prefere. Voir RapporterGoal. */
	private static final String PREFIXE_OBJET_PREFERE = "objet_prefere.";

	/**
	 * Le nom lisible d'un compteur.
	 *
	 * <p>La page affichait la cle brute — « caresses », « nage », et pire
	 * encore les compteurs de cuisine des missions. Une page de livre doit se
	 * lire, pas se decoder.
	 */
	private String libelleCompteur(String cle) {
		String traduction = "compteur.compagnon." + cle;
		if (Language.getInstance().has(traduction)) {
			return Component.translatable(traduction).getString();
		}
		return cle.replace('_', ' ');
	}

	private String libelleMoment(String cle) {
		String traduction = "moment.compagnon." + cle;
		if (Language.getInstance().has(traduction)) {
			return Component.translatable(traduction).getString();
		}

		// Son objet prefere porte le nom de l'objet dans sa propre cle, faute de
		// quoi il aurait fallu un champ de plus dans la sauvegarde. On rend donc
		// ici le vrai nom de l'objet, traduit : sans cela le livre affichait
		// « objet prefere minecraft:feather », ce qui ne se lit pas.
		if (cle.startsWith(PREFIXE_OBJET_PREFERE)) {
			String nom = nomDObjet(cle.substring(PREFIXE_OBJET_PREFERE.length()));
			return Component.translatable("moment.compagnon.objet_prefere").getString()
					+ " " + nom;
		}
		// LES FAMILLES DE MOMENTS, dont la fin du nom est libre.
		//
		// Un defaut, une manie, une mission accomplie ou un paysage decouvert
		// portent leur identite dans leur cle. On traduit donc la famille, et on
		// lui accroche le detail — sinon le livre affichait « defaut bavard »,
		// qui ressemble a une ligne de code oubliee la.
		for (String famille : FAMILLES_DE_MOMENTS) {
			if (!cle.startsWith(famille)) {
				continue;
			}
			String detail = cle.substring(famille.length());
			String precis = "moment.compagnon." + famille + detail;
			if (Language.getInstance().has(precis)) {
				return Component.translatable(precis).getString();
			}
			String entete = "moment.compagnon." + famille.substring(0, famille.length() - 1);
			if (Language.getInstance().has(entete)) {
				return Component.translatable(entete, detail.replace('_', ' ')).getString();
			}
		}
		return cle.replace('_', ' ').replace('.', ' ');
	}

	/**
	 * Les prefixes de moments dont la fin est libre.
	 *
	 * <p>L'ordre compte : {@code premiere_fois.biome.} avant
	 * {@code premiere_fois.}, sinon le plus court gagnerait toujours.
	 */
	private static final String[] FAMILLES_DE_MOMENTS = {
			"premiere_fois.biome.", "premiere_fois.", "defaut.", "manie.",
			"mission.", "reve."};

	/**
	 * Depuis combien de jours il est avec toi.
	 *
	 * <p>Au moins un : le jour ou on le recoit, on ne veut pas lire « 0 jour ».
	 */
	/**
	 * Ce que ce nombre de jours veut dire, en toutes lettres.
	 *
	 * <p>Les paliers ne sont pas ronds exprès : a sept, trente, quatre-vingt-dix
	 * et trois cent soixante-cinq jours, on reconnait la semaine, le mois, la
	 * saison et l'annee sans qu'on ait besoin de les nommer.
	 */
	private static String ageEnMots(long jours) {
		if (jours < 2) {
			return "livre.compagnon.age.arrive";
		}
		if (jours < 7) {
			return "livre.compagnon.age.decouvre";
		}
		if (jours < 30) {
			return "livre.compagnon.age.habitude";
		}
		if (jours < 90) {
			return "livre.compagnon.age.bien";
		}
		if (jours < 365) {
			return "livre.compagnon.age.longtemps";
		}
		return "livre.compagnon.age.une_vie";
	}

	private static long joursDepuis(long date) {
		long jours = (System.currentTimeMillis() - date) / 86_400_000L;
		return Math.max(1L, jours);
	}

	/**
	 * Son objet prefere, en vignette a cote de son portrait.
	 *
	 * <p>C'est le premier objet qu'il t'a rapporte, et il le gardera toute sa vie.
	 * Perdu au milieu de la liste des souvenirs, personne ne le remarquait.
	 */
	private void objetPrefere(GuiGraphics g, int x, int y) {
		ItemStack prefere = ItemStack.EMPTY;
		for (var moment : this.donnees.moments()) {
			if (moment.cle().startsWith(PREFIXE_OBJET_PREFERE)) {
				prefere = pileDe(moment.cle().substring(PREFIXE_OBJET_PREFERE.length()));
				break;
			}
		}
		if (prefere.isEmpty()) {
			return;
		}
		g.renderItem(prefere, x, y);
		g.drawString(this.font, Component.translatable("livre.compagnon.prefere"),
				x + 20, y + 4, ENCRE_PALE, false);
	}

	private static ItemStack pileDe(String identifiant) {
		ResourceLocation quoi = ResourceLocation.tryParse(identifiant);
		if (quoi == null) {
			return ItemStack.EMPTY;
		}
		return BuiltInRegistries.ITEM.getOptional(quoi)
				.map(ItemStack::new)
				.orElse(ItemStack.EMPTY);
	}

	/** Le nom traduit d'un objet, ou son identifiant si ce client ne le connait pas. */
	private static String nomDObjet(String identifiant) {
		ResourceLocation quoi = ResourceLocation.tryParse(identifiant);
		if (quoi == null) {
			return identifiant;
		}
		return BuiltInRegistries.ITEM.getOptional(quoi)
				.map(objet -> new ItemStack(objet).getHoverName().getString())
				.orElse(identifiant);
	}

	// --- Outils -----------------------------------------------------------------

	/** Un filet fin, pour separer les blocs sans faire de trait dur. */
	private void filet(GuiGraphics g, int x, int y) {
		g.fill(x, y, x + PAGE_LARGEUR, y + 1, FILET);
	}

	private List<String> decouper(String texte) {
		return this.font.getSplitter().splitLines(texte, PAGE_LARGEUR, Style.EMPTY)
				.stream().map(ligne -> ligne.getString()).toList();
	}

	private static String duree(long ticks) {
		long minutes = ticks / (20L * 60L);
		long heures = minutes / 60L;
		return heures > 0 ? heures + " h " + (minutes % 60L) + " min" : minutes + " min";
	}

	private static int eclaircir(int couleur, float part) {
		return melanger(couleur, 0xFFFFFF, part);
	}

	private static int assombrir(int couleur, float part) {
		return melanger(couleur, 0x000000, part);
	}

	private static int melanger(int couleur, int vers, float part) {
		int alpha = (couleur >>> 24) & 255;
		int r = melangerCanal((couleur >> 16) & 255, (vers >> 16) & 255, part);
		int v = melangerCanal((couleur >> 8) & 255, (vers >> 8) & 255, part);
		int b = melangerCanal(couleur & 255, vers & 255, part);
		return (alpha << 24) | (r << 16) | (v << 8) | b;
	}

	private static int melangerCanal(int de, int vers, float part) {
		return Math.round(de + (vers - de) * part);
	}
}
