package fr.lhdp.compagnon.client;

import fr.lhdp.compagnon.entite.CompagnonEntity;
import fr.lhdp.compagnon.espece.Ancrage;
import fr.lhdp.compagnon.espece.Ancrages;
import fr.lhdp.compagnon.espece.Especes;
import fr.lhdp.compagnon.reseau.PaquetPoserAncrage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * L'atelier : on y pose un objet sur une bete, au pixel pres.
 *
 * <h2>Ce que c'est</h2>
 *
 * <p>Une vraie vue 3D, dans l'esprit d'un logiciel de modelisation. Le compagnon
 * est <b>fige</b> au milieu, on tourne autour a la souris, et on deplace l'objet
 * avec trois fleches d'axe qu'on attrape directement — ou en le tirant par le
 * corps.
 *
 * <h2>Les trois choses qui rendent le calage possible</h2>
 *
 * <ul>
 *   <li><b>La bete ne bouge pas.</b> Voir {@link Mannequin} : on ne cale pas une
 *       balle sur une machoire qui respire.</li>
 *   <li><b>La gueule s'ouvre.</b> Un curseur fait pivoter la machoire : on place
 *       la balle entre les dents et non derriere elles.</li>
 *   <li><b>Les fleches sont justes.</b> Elles ne sont pas dessinees d'apres un
 *       calcul a nous, mais d'apres la matrice que le jeu vient d'utiliser pour
 *       poser l'objet. Ce qu'on voit est exactement ou il est.</li>
 * </ul>
 *
 * <h2>Un reglage par objet</h2>
 *
 * <p>Une balle et un os n'ont ni la meme forme ni le meme sens : les poser au
 * meme endroit donne un os plante en travers du museau. Chaque objet a donc sa
 * ligne, et {@code defaut} sert a tout ce qu'on n'a pas regle — un compagnon
 * ramasse aussi des steaks et des fleurs.
 */
public class EcranAncrage extends EcranCompagnon {

	// --- Le cadre ---------------------------------------------------------------

	private static final int PANNEAU = 176;
	private static final int MARGE = 8;
	private static final int LIGNE = 17;
	private static final int HAUT_BOUTON = 14;
	private static final int FLECHE = 15;

	/** Les pas de deplacement, en unites de modele. */
	private static final double[] PAS = {0.1D, 0.5D, 1.0D};
	private static final float[] PAS_ANGLE = {5.0F, 15.0F, 45.0F};
	private static final float PAS_ECHELLE = 0.05F;

	/** De combien la molette rapproche ou eloigne. */
	private static final float ZOOM_PAS = 1.12F;
	private static final float ZOOM_MIN = 0.35F;
	private static final float ZOOM_MAX = 4.0F;

	/** Longueur des fleches d'axe, en pixels. */
	private static final int LONGUEUR_FLECHE = 46;

	/** A quelle distance d'une fleche on l'attrape. */
	private static final double PRISE = 6.0D;

	// --- Les couleurs des axes ---------------------------------------------------
	//
	// Rouge, vert, bleu : la convention de tous les logiciels 3D. On ne l'invente
	// pas, justement pour que quelqu'un qui vient de Blockbench s'y retrouve.

	private static final int AXE_X = 0xFFE05A50;
	private static final int AXE_Y = 0xFF7FD46B;
	private static final int AXE_Z = 0xFF5B92E0;
	private static final int AXE_PRIS = 0xFFFFE07A;

	private static final int VERT = 0xFF9BD17A;
	private static final int ROUGE = 0xFFE05A50;

	private final String espece;
	private final String variante;
	private final List<String> osDuModele;

	/** Les objets qu'on peut regler : ceux deja connus, plus les jouets du mod. */
	private final List<String> objets = new ArrayList<>();

	/** Ce qu'il y avait a l'ouverture, pour tout remettre en annulant. */
	private final Map<String, Ancrage> depart = new LinkedHashMap<>();

	private int objetChoisi;
	private int osChoisi;
	private int grosseurDuPas = 1;

	private Ancrage courant;

	// --- L'etat de la vue --------------------------------------------------------

	private float lacetDeLaVue = 155.0F;
	private float tangageDeLaVue = -12.0F;
	private float zoom = 1.0F;

	/** Ce qu'on est en train de tirer : 0 X, 1 Y, 2 Z, 3 le corps, -1 rien. */
	private int enTrainDeTirer = -1;

	/** Vrai quand on fait tourner la vue. */
	private boolean enTrainDeTourner;

	private double dernierX;
	private double dernierY;

	/** Les axes tels qu'ils etaient a l'ecran a la derniere image. */
	private final float[][] axesEcran = new float[3][2];
	private float objetEcranX;
	private float objetEcranY;
	private boolean objetVisible;

	/** De combien la gueule est ouverte, en degres. */
	private float gueule;

	private String message = "";
	private int couleurDuMessage = VERT;
	private int resteDuMessage;

	public EcranAncrage(String espece) {
		super(Component.literal("Atelier — " + Especes.titre(espece)));
		this.espece = espece;
		this.osDuModele = Apercu.osDe(espece);

		fr.lhdp.compagnon.espece.Espece fiche = Especes.get(espece);
		this.variante = fiche != null ? fiche.varianteParDefaut() : "";

		// LES OBJETS QU'ON PROPOSE.
		//
		// Ceux qui ont deja un reglage, les jouets du mod, et « defaut » qui sert
		// a tout le reste. Un ensemble ordonne : pas de doublon, et l'ordre reste
		// celui ou on les a poses.
		LinkedHashSet<String> tous = new LinkedHashSet<>();
		tous.add(Ancrage.DEFAUT);
		tous.add("compagnon:balle");
		tous.add("compagnon:os_a_macher");
		tous.add(Ancrage.COLLIER);
		Map<String, Ancrage> deja = Ancrages.toutes().get(espece);
		if (deja != null) {
			tous.addAll(deja.keySet());
		}
		this.objets.addAll(tous);
		for (String objet : this.objets) {
			this.depart.put(objet, Ancrages.exactement(espece, objet));
		}
		this.courant = this.depart.get(this.objets.get(0));
		this.osChoisi = Math.max(0, this.osDuModele.indexOf(this.courant.os()));
	}

	private String objet() {
		return this.objets.get(Math.min(this.objetChoisi, this.objets.size() - 1));
	}

	// --- Le montage -------------------------------------------------------------

	@Override
	protected void init() {
		int x = MARGE;
		int y = MARGE + 24;

		rangee(x, y, sens -> {
			this.objetChoisi = Math.floorMod(this.objetChoisi + sens, this.objets.size());
			this.courant = Ancrages.exactement(this.espece, objet());
			this.osChoisi = Math.max(0, this.osDuModele.indexOf(this.courant.os()));
			appliquer();
		});
		y += LIGNE;

		rangee(x, y, sens -> {
			if (this.osDuModele.isEmpty()) {
				Bruits.refus();
				return;
			}
			this.osChoisi = Math.floorMod(this.osChoisi + sens, this.osDuModele.size());
			this.courant = avecOs(this.osDuModele.get(this.osChoisi));
			appliquer();
		});
		y += LIGNE + 5;

		rangee(x, y, sens -> pousser(sens, 0));
		y += LIGNE;
		rangee(x, y, sens -> pousser(sens, 1));
		y += LIGNE;
		rangee(x, y, sens -> pousser(sens, 2));
		y += LIGNE + 5;

		rangee(x, y, sens -> tourner(sens, 0));
		y += LIGNE;
		rangee(x, y, sens -> tourner(sens, 1));
		y += LIGNE;
		rangee(x, y, sens -> tourner(sens, 2));
		y += LIGNE + 5;

		rangee(x, y, sens -> redimensionner(sens));
		y += LIGNE + 5;

		// LE CURSEUR DE GUEULE.
		//
		// Sans lui, on cale la balle contre des machoires fermees et elle se
		// retrouve dans le vide des qu'il ouvre la bouche en jeu.
		rangee(x, y, sens -> {
			this.gueule = Math.max(0.0F, Math.min(60.0F, this.gueule + sens * 5.0F));
		});
		y += LIGNE + 8;

		addRenderableWidget(BoutonDuMod.de(x, y, PANNEAU - 2 * MARGE, HAUT_BOUTON,
				Component.literal("Pas : " + PAS[this.grosseurDuPas] + "  /  "
						+ (int) PAS_ANGLE[this.grosseurDuPas] + "°"),
				bouton -> {
					this.grosseurDuPas = (this.grosseurDuPas + 1) % PAS.length;
					Bruits.page();
					rebuildWidgets();
				}));
		y += HAUT_BOUTON + 5;

		addRenderableWidget(BoutonDuMod.de(x, y, PANNEAU - 2 * MARGE, HAUT_BOUTON,
				Component.literal("Recentrer la vue"), bouton -> {
					this.lacetDeLaVue = 155.0F;
					this.tangageDeLaVue = -12.0F;
					this.zoom = 1.0F;
					Bruits.page();
				}));
		y += HAUT_BOUTON + 8;

		int moitie = (PANNEAU - 2 * MARGE - 4) / 2;
		addRenderableWidget(BoutonDuMod.de(x, y, moitie, HAUT_BOUTON,
				Component.literal("Enregistrer"), bouton -> enregistrer()));
		addRenderableWidget(BoutonDuMod.de(x + moitie + 4, y, moitie, HAUT_BOUTON,
				Component.literal("Annuler"), bouton -> onClose()));
	}

	/** Les deux fleches d'une ligne de reglage. Le texte est dessine par-dessus. */
	private void rangee(int x, int y, java.util.function.IntConsumer quoi) {
		addRenderableWidget(BoutonDuMod.de(x, y, FLECHE, HAUT_BOUTON,
				Component.literal("-"), bouton -> {
					quoi.accept(-1);
					Bruits.page();
				}));
		addRenderableWidget(BoutonDuMod.de(x + PANNEAU - 2 * MARGE - FLECHE, y,
				FLECHE, HAUT_BOUTON, Component.literal("+"), bouton -> {
					quoi.accept(1);
					Bruits.page();
				}));
	}

	// --- Les changements ---------------------------------------------------------

	private void pousser(int sens, int axe) {
		deplacer(axe == 0 ? PAS[this.grosseurDuPas] * sens : 0,
				axe == 1 ? PAS[this.grosseurDuPas] * sens : 0,
				axe == 2 ? PAS[this.grosseurDuPas] * sens : 0);
	}

	private void deplacer(double dx, double dy, double dz) {
		this.courant = new Ancrage(this.courant.os(),
				arrondi(this.courant.x() + dx),
				arrondi(this.courant.y() + dy),
				arrondi(this.courant.z() + dz),
				this.courant.tangage(), this.courant.lacet(), this.courant.roulis(),
				this.courant.echelle());
		appliquer();
	}

	private void tourner(int sens, int axe) {
		float pas = PAS_ANGLE[this.grosseurDuPas] * sens;
		this.courant = new Ancrage(this.courant.os(),
				this.courant.x(), this.courant.y(), this.courant.z(),
				borner(this.courant.tangage() + (axe == 0 ? pas : 0)),
				borner(this.courant.lacet() + (axe == 1 ? pas : 0)),
				borner(this.courant.roulis() + (axe == 2 ? pas : 0)),
				this.courant.echelle());
		appliquer();
	}

	private void redimensionner(int sens) {
		float echelle = Math.max(0.05F, Math.round(
				(this.courant.echelle() + sens * PAS_ECHELLE) * 100.0F) / 100.0F);
		this.courant = new Ancrage(this.courant.os(),
				this.courant.x(), this.courant.y(), this.courant.z(),
				this.courant.tangage(), this.courant.lacet(), this.courant.roulis(),
				echelle);
		appliquer();
	}

	private Ancrage avecOs(String os) {
		return new Ancrage(os, this.courant.x(), this.courant.y(), this.courant.z(),
				this.courant.tangage(), this.courant.lacet(), this.courant.roulis(),
				this.courant.echelle());
	}

	/** La bete change sous nos yeux, sans que rien ne soit ecrit. */
	private void appliquer() {
		Ancrages.enMemoire(this.espece, objet(), this.courant);
	}

	private void enregistrer() {
		if (!this.courant.regle()) {
			dire("Choisis d'abord un os.", ROUGE);
			Bruits.refus();
			return;
		}
		ClientPlayNetworking.send(new PaquetPoserAncrage(this.espece, objet(), this.courant));
		this.depart.put(objet(), this.courant);
		dire("Enregistre.", VERT);
		Bruits.clic();
	}

	@Override
	public void onClose() {
		this.depart.forEach((objet, ancrage) ->
				Ancrages.enMemoire(this.espece, objet, ancrage));
		Mannequin.oublier();
		// La bete de demonstration est partagee avec l ecran de remise : sans
		// ca, elle y garderait une balle dans la gueule.
		CompagnonEntity bete = Apercu.bete(this.espece, this.variante);
		if (bete != null) {
			bete.viderLaGueule();
		}
		super.onClose();
	}

	private void dire(String quoi, int couleur) {
		this.message = quoi;
		this.couleurDuMessage = couleur;
		this.resteDuMessage = 70;
	}

	// --- Le dessin ---------------------------------------------------------------

	@Override
	public void render(GuiGraphics g, int sourisX, int sourisY, float partiel) {
		g.fill(0, 0, this.width, this.height, Peinture.VOILE);

		dessinerLAtelier(g);
		dessinerLePanneau(g);
		super.render(g, sourisX, sourisY, partiel);
		dessinerLesValeurs(g);

		if (this.objetVisible) {
			dessinerLesFleches(g, sourisX, sourisY);
		}
		if (this.resteDuMessage > 0) {
			this.resteDuMessage--;
			g.drawCenteredString(this.font, this.message,
					MARGE + (PANNEAU - 2 * MARGE) / 2, this.height - 22,
					this.couleurDuMessage);
		}
	}

	/**
	 * La bete, figee, au milieu de l'atelier.
	 *
	 * <p>C'est ici que {@link Mannequin} est allume : le temps de ce seul appel,
	 * et jamais pendant le rendu du monde.
	 */
	private void dessinerLAtelier(GuiGraphics g) {
		CompagnonEntity bete = Apercu.bete(this.espece, this.variante);
		if (bete == null) {
			this.objetVisible = false;
			return;
		}
		// Ce qu'elle tient : c'est ce qu'on regle.
		bete.prendreDansLaGueule(objetTenu());

		// La bete tourne par son propre cap, comme dans l'inventaire : c'est ce
		// qui fait pivoter aussi ce qu'elle porte.
		bete.setYRot(this.lacetDeLaVue);
		bete.yBodyRot = this.lacetDeLaVue;
		bete.yHeadRot = this.lacetDeLaVue;
		bete.yBodyRotO = this.lacetDeLaVue;
		bete.yHeadRotO = this.lacetDeLaVue;
		bete.setXRot(0.0F);

		int cx = PANNEAU + (this.width - PANNEAU) / 2;
		int cy = this.height / 2 + 40;
		float[] taille = Apercu.encombrement(this.espece);
		float echelle = (this.height * 0.55F / Math.max(taille[0], 0.4F)) * this.zoom;
		echelle = Math.max(4.0F, Math.min(320.0F, echelle));

		Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
		Quaternionf camera = new Quaternionf()
				.rotateX(this.tangageDeLaVue * ((float) Math.PI / 180.0F));
		rotation.mul(camera);

		Mannequin.commencer(this.gueule);
		try {
			InventoryScreen.renderEntityInInventory(g, cx, cy, echelle,
					new Vector3f(0.0F, 0.0F, 0.0F), rotation, camera, bete);
		} finally {
			// Un finally, pas un simple appel : si le rendu jette, le monde ne
			// doit pas continuer a etre dessine en mode mannequin.
			Mannequin.finir();
		}
		lireLaMatrice();
	}

	/** Ce qu'on regle : la balle, l'os, ou un baton pour les cas generaux. */
	private ItemStack objetTenu() {
		String nom = objet();
		if (nom.equals(Ancrage.DEFAUT) || nom.equals(Ancrage.COLLIER)) {
			return new ItemStack(fr.lhdp.compagnon.objet.Objets.BALLE);
		}
		ResourceLocation cle = ResourceLocation.tryParse(nom);
		if (cle == null || !BuiltInRegistries.ITEM.containsKey(cle)) {
			return new ItemStack(fr.lhdp.compagnon.objet.Objets.BALLE);
		}
		return new ItemStack(BuiltInRegistries.ITEM.get(cle));
	}

	/**
	 * Ou est l'objet, et dans quel sens partent ses axes — en pixels d'ecran.
	 *
	 * <p>On ne calcule rien : la matrice vient d'etre utilisee par le jeu pour
	 * poser l'objet. Sa translation est sa position, ses trois premieres colonnes
	 * sont ses axes. Une unite de modele fait un seizieme de bloc, d'ou la
	 * division ; et le signe est inverse parce que l'ancrage se pose en negatif.
	 */
	private void lireLaMatrice() {
		Matrix4f m = Mannequin.matrice();
		if (m == null) {
			this.objetVisible = false;
			return;
		}
		this.objetEcranX = m.m30();
		this.objetEcranY = m.m31();
		this.axesEcran[0][0] = -m.m00() / 16.0F;
		this.axesEcran[0][1] = -m.m01() / 16.0F;
		this.axesEcran[1][0] = -m.m10() / 16.0F;
		this.axesEcran[1][1] = -m.m11() / 16.0F;
		this.axesEcran[2][0] = -m.m20() / 16.0F;
		this.axesEcran[2][1] = -m.m21() / 16.0F;
		this.objetVisible = true;
	}

	/** Les trois fleches, dessinees a la longueur qu'elles ont vraiment. */
	private void dessinerLesFleches(GuiGraphics g, int sourisX, int sourisY) {
		int survole = this.enTrainDeTirer >= 0 && this.enTrainDeTirer < 3
				? this.enTrainDeTirer : axeSous(sourisX, sourisY);
		int[] couleurs = {AXE_X, AXE_Y, AXE_Z};
		String[] noms = {"X", "Y", "Z"};

		for (int axe = 0; axe < 3; axe++) {
			float[] a = this.axesEcran[axe];
			double longueur = Math.hypot(a[0], a[1]);
			if (longueur < 0.0001D) {
				continue;   // l'axe pointe vers nous : rien a montrer
			}
			float ux = (float) (a[0] / longueur);
			float uy = (float) (a[1] / longueur);
			int couleur = axe == survole ? AXE_PRIS : couleurs[axe];

			float boutX = this.objetEcranX + ux * LONGUEUR_FLECHE;
			float boutY = this.objetEcranY + uy * LONGUEUR_FLECHE;
			trait(g, this.objetEcranX, this.objetEcranY, boutX, boutY, couleur);
			pointe(g, boutX, boutY, ux, uy, couleur);
			g.drawString(this.font, noms[axe],
					(int) (boutX + ux * 7) - 2, (int) (boutY + uy * 7) - 4, couleur, false);
		}
		// Le point de prise, au centre : on le tire pour glisser a plat.
		g.fill((int) this.objetEcranX - 3, (int) this.objetEcranY - 3,
				(int) this.objetEcranX + 3, (int) this.objetEcranY + 3,
				this.enTrainDeTirer == 3 ? AXE_PRIS : 0xC0FFFFFF);
	}

	/** Un segment, trace point par point : il n'y a pas de ligne dans une interface. */
	private static void trait(GuiGraphics g, float x1, float y1, float x2, float y2, int couleur) {
		int pas = (int) Math.max(1, Math.hypot(x2 - x1, y2 - y1));
		for (int i = 0; i <= pas; i++) {
			float t = (float) i / pas;
			int x = Math.round(x1 + (x2 - x1) * t);
			int y = Math.round(y1 + (y2 - y1) * t);
			g.fill(x, y, x + 1, y + 1, couleur);
		}
	}

	/** La pointe : un petit triangle plein au bout du trait. */
	private static void pointe(GuiGraphics g, float x, float y, float ux, float uy, int couleur) {
		float px = -uy, py = ux;
		for (int i = 0; i <= 5; i++) {
			float t = i / 5.0F;
			float largeur = 3.0F * (1.0F - t);
			float bx = x + ux * i, by = y + uy * i;
			trait(g, bx - px * largeur, by - py * largeur,
					bx + px * largeur, by + py * largeur, couleur);
		}
	}

	private void dessinerLePanneau(GuiGraphics g) {
		Peinture.ombre(g, 0, 0, PANNEAU, this.height);
		Peinture.panneau(g, 0, 0, PANNEAU, this.height);
		g.drawString(this.font,
				this.font.plainSubstrByWidth(this.title.getString(), PANNEAU - 2 * MARGE),
				MARGE, MARGE, Peinture.OR, false);
		Peinture.filet(g, MARGE, MARGE + 13, PANNEAU - 2 * MARGE);
	}

	private void dessinerLesValeurs(GuiGraphics g) {
		int y = MARGE + 24;
		ligne(g, y, "Objet", courtObjet(objet()), Peinture.OR);
		y += LIGNE;
		ligne(g, y, "Os", this.courant.regle() ? this.courant.os()
				: (this.osDuModele.isEmpty() ? "modele illisible" : "non regle"),
				this.courant.regle() ? Peinture.TEXTE : ROUGE);
		y += LIGNE + 5;

		ligne(g, y, "X  droite", nombre(this.courant.x()), AXE_X);
		y += LIGNE;
		ligne(g, y, "Y  haut", nombre(this.courant.y()), AXE_Y);
		y += LIGNE;
		ligne(g, y, "Z  avant", nombre(this.courant.z()), AXE_Z);
		y += LIGNE + 5;

		ligne(g, y, "Tangage", (int) this.courant.tangage() + "°", Peinture.TEXTE);
		y += LIGNE;
		ligne(g, y, "Lacet", (int) this.courant.lacet() + "°", Peinture.TEXTE);
		y += LIGNE;
		ligne(g, y, "Roulis", (int) this.courant.roulis() + "°", Peinture.TEXTE);
		y += LIGNE + 5;

		ligne(g, y, "Taille",
				String.format(Locale.ROOT, "%.2f", this.courant.echelle()), Peinture.TEXTE);
		y += LIGNE + 5;

		ligne(g, y, "Gueule", (int) this.gueule + "°", Peinture.TEXTE);

		// Le mode d'emploi, en bas, en petit. Personne ne devine qu'on peut
		// tourner la vue en tirant le fond.
		int bas = this.height - 60;
		for (String aide : new String[]{
				"Tirer une fleche : deplacer sur l'axe",
				"Tirer le carre blanc : glisser a plat",
				"Tirer le fond : tourner la vue",
				"Molette : approcher"}) {
			g.drawString(this.font,
					this.font.plainSubstrByWidth(aide, PANNEAU - 2 * MARGE),
					MARGE, bas, Peinture.TEXTE_PALE, false);
			bas += 10;
		}
	}

	/** Le nom a gauche, la valeur a droite, entre les deux fleches. */
	private void ligne(GuiGraphics g, int y, String nom, String valeur, int couleur) {
		int gauche = MARGE + FLECHE + 4;
		int droite = PANNEAU - MARGE - FLECHE - 4;
		g.drawString(this.font, nom, gauche, y + 3, Peinture.TEXTE_PALE, false);
		String court = this.font.plainSubstrByWidth(valeur, droite - gauche - 46);
		g.drawString(this.font, court, droite - this.font.width(court), y + 3, couleur, false);
	}

	/** {@code compagnon:os_a_macher} devient {@code os a macher}. */
	private static String courtObjet(String nom) {
		int deuxPoints = nom.indexOf(':');
		return (deuxPoints >= 0 ? nom.substring(deuxPoints + 1) : nom).replace('_', ' ');
	}

	// --- La souris ---------------------------------------------------------------

	/** Quelle fleche est sous la souris, ou {@code -1}. */
	private int axeSous(double sourisX, double sourisY) {
		if (!this.objetVisible) {
			return -1;
		}
		int meilleur = -1;
		double plusPres = PRISE;
		for (int axe = 0; axe < 3; axe++) {
			float[] a = this.axesEcran[axe];
			double longueur = Math.hypot(a[0], a[1]);
			if (longueur < 0.0001D) {
				continue;
			}
			double ux = a[0] / longueur, uy = a[1] / longueur;
			double px = sourisX - this.objetEcranX, py = sourisY - this.objetEcranY;
			double le = px * ux + py * uy;
			if (le < 0 || le > LONGUEUR_FLECHE + 8) {
				continue;
			}
			double ecart = Math.abs(px * -uy + py * ux);
			if (ecart < plusPres) {
				plusPres = ecart;
				meilleur = axe;
			}
		}
		return meilleur;
	}

	@Override
	public boolean mouseClicked(double sourisX, double sourisY, int bouton) {
		if (super.mouseClicked(sourisX, sourisY, bouton)) {
			return true;
		}
		if (sourisX < PANNEAU) {
			return false;
		}
		this.dernierX = sourisX;
		this.dernierY = sourisY;

		int axe = axeSous(sourisX, sourisY);
		if (axe >= 0) {
			this.enTrainDeTirer = axe;
			return true;
		}
		if (this.objetVisible
				&& Math.hypot(sourisX - this.objetEcranX, sourisY - this.objetEcranY) < 8) {
			this.enTrainDeTirer = 3;
			return true;
		}
		this.enTrainDeTourner = true;
		return true;
	}

	@Override
	public boolean mouseDragged(double sourisX, double sourisY, int bouton,
			double dxIgnore, double dyIgnore) {

		double dx = sourisX - this.dernierX;
		double dy = sourisY - this.dernierY;
		this.dernierX = sourisX;
		this.dernierY = sourisY;

		if (this.enTrainDeTourner) {
			this.lacetDeLaVue += (float) dx * 0.9F;
			this.tangageDeLaVue = Math.max(-89.0F,
					Math.min(89.0F, this.tangageDeLaVue + (float) dy * 0.6F));
			return true;
		}
		if (this.enTrainDeTirer >= 0 && this.enTrainDeTirer < 3) {
			glisserSurUnAxe(this.enTrainDeTirer, dx, dy);
			return true;
		}
		if (this.enTrainDeTirer == 3) {
			glisserAPlat(dx, dy);
			return true;
		}
		return super.mouseDragged(sourisX, sourisY, bouton, dxIgnore, dyIgnore);
	}

	/**
	 * Combien d'unites de modele valent ce mouvement de souris, sur cet axe.
	 *
	 * <p>On projette le deplacement de la souris sur la direction de l'axe a
	 * l'ecran, puis on divise par sa longueur : un axe presque de face bouge
	 * beaucoup pour peu de pixels, un axe vu de profil bouge peu. C'est ce qui
	 * fait qu'on tire une fleche et que l'objet suit le doigt.
	 */
	private void glisserSurUnAxe(int axe, double dx, double dy) {
		float[] a = this.axesEcran[axe];
		double carre = a[0] * a[0] + a[1] * a[1];
		if (carre < 1.0E-6D) {
			return;   // vu exactement de bout : impossible a tirer
		}
		double unites = (dx * a[0] + dy * a[1]) / carre;
		deplacer(axe == 0 ? unites : 0, axe == 1 ? unites : 0, axe == 2 ? unites : 0);
	}

	/**
	 * Glisse dans le plan de l'ecran, sur les deux axes les mieux places.
	 *
	 * <p>Un deplacement a l'ecran a deux dimensions et l'espace en a trois : il
	 * faut choisir lesquelles. On garde les deux axes les plus francs a l'ecran
	 * et on resout a deux inconnues — le troisieme, vu de bout, ne repondrait
	 * qu'a du bruit.
	 */
	private void glisserAPlat(double dx, double dy) {
		int premier = -1, second = -1;
		double meilleure = -1, deuxieme = -1;
		for (int axe = 0; axe < 3; axe++) {
			double longueur = Math.hypot(this.axesEcran[axe][0], this.axesEcran[axe][1]);
			if (longueur > meilleure) {
				deuxieme = meilleure; second = premier;
				meilleure = longueur; premier = axe;
			} else if (longueur > deuxieme) {
				deuxieme = longueur; second = axe;
			}
		}
		if (premier < 0 || second < 0) {
			return;
		}
		float[] u = this.axesEcran[premier];
		float[] v = this.axesEcran[second];
		double det = u[0] * v[1] - u[1] * v[0];
		if (Math.abs(det) < 1.0E-6D) {
			return;   // les deux axes se superposent a l'ecran
		}
		double a = (dx * v[1] - dy * v[0]) / det;
		double b = (u[0] * dy - u[1] * dx) / det;
		double[] bouge = new double[3];
		bouge[premier] = a;
		bouge[second] = b;
		deplacer(bouge[0], bouge[1], bouge[2]);
	}

	@Override
	public boolean mouseReleased(double sourisX, double sourisY, int bouton) {
		this.enTrainDeTirer = -1;
		this.enTrainDeTourner = false;
		return super.mouseReleased(sourisX, sourisY, bouton);
	}

	@Override
	public boolean mouseScrolled(double sourisX, double sourisY,
			double horizontal, double vertical) {
		if (sourisX < PANNEAU) {
			return super.mouseScrolled(sourisX, sourisY, horizontal, vertical);
		}
		this.zoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX,
				this.zoom * (float) Math.pow(ZOOM_PAS, vertical)));
		return true;
	}

	// --- Details -----------------------------------------------------------------

	@Override
	public void renderBackground(GuiGraphics g, int sourisX, int sourisY, float partiel) {
		// Le voile est peint dans render() : ici, rien, sinon il passerait
		// par-dessus la bete.
	}

	private static String nombre(double valeur) {
		return String.format(Locale.ROOT, "%.1f", valeur);
	}

	private static double arrondi(double valeur) {
		return Math.round(valeur * 100.0) / 100.0;
	}

	private static float borner(float angle) {
		float tour = angle % 360.0F;
		return tour > 180.0F ? tour - 360.0F : (tour < -180.0F ? tour + 360.0F : tour);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
