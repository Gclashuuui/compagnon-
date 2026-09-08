package fr.lhdp.compagnon.client;

import fr.lhdp.compagnon.espece.Espece;
import fr.lhdp.compagnon.espece.Especes;
import fr.lhdp.compagnon.reseau.PaquetRemettre;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * L'ecran par lequel l'equipe offre un compagnon.
 *
 * <h2>Ce qu'il remplace</h2>
 *
 * <p>{@code /compagnon donner Untel atroxiia_adulte cramoisi_femelle}. La
 * commande marche, et elle restera : elle se scripte, elle se repete. Mais pour
 * choisir, elle est mauvaise — il faut connaitre par coeur vingt cles d'espece
 * et leurs couleurs, et on ne voit jamais ce qu'on donne avant de l'avoir donne.
 *
 * <p>Ici on regarde la bete tourner, on essaie ses couleurs, on choisit celui
 * qui la recevra, et on donne.
 *
 * <h2>L'ecran ne decide de rien</h2>
 *
 * <p>Il n'a aucun droit et n'en verifie aucun. Il envoie trois chaines ; c'est
 * le serveur qui exige le niveau 2, qui cherche le joueur parmi ceux qui sont
 * en ligne, et qui refuse une espece qu'il ne connait pas. Un client fabrique
 * n'obtient rien de plus qu'un client honnete.
 */
public class EcranRemise extends EcranCompagnon {

	// --- Le cadre ---------------------------------------------------------------

	private static final int LARGE = 344;
	private static final int HAUT = 206;

	private static final int MARGE = 8;
	private static final int LIGNE = 12;
	private static final int HAUT_LISTE = 120;

	private static final int LARGE_ESPECES = 100;
	private static final int LARGE_APERCU = 116;
	private static final int LARGE_JOUEURS = 94;

	private static final int HAUT_BOUTON = 20;
	private static final int LARGE_BOUTON = 150;

	// --- Les couleurs -----------------------------------------------------------

	private static final int CHOISI = 0x50E8C87A;
	private static final int SURVOL = 0x28FFFFFF;
	private static final int CONFIRME = 0xFF9BD17A;

	/** Combien de temps la phrase de confirmation reste, en images. */
	private static final int DUREE_DU_MERCI = 70;

	private final List<Espece> especes;
	private final List<String> joueurs;

	private int especeChoisie;
	private int joueurChoisi;
	private int varianteChoisie;

	private int hautListe;
	private int hautJoueurs;

	private String merci = "";
	private int resteDuMerci;

	public EcranRemise(List<String> joueurs) {
		super(Component.literal("Offrir un compagnon"));
		this.especes = new ArrayList<>(Especes.toutes());
		this.joueurs = new ArrayList<>(joueurs);
	}

	// --- Ce qui est choisi en ce moment -----------------------------------------

	private Espece espece() {
		return this.especes.isEmpty() ? null
				: this.especes.get(Math.min(this.especeChoisie, this.especes.size() - 1));
	}

	private List<String> variantes() {
		Espece espece = espece();
		return espece == null ? List.of() : new ArrayList<>(espece.variantes().keySet());
	}

	private String variante() {
		List<String> toutes = variantes();
		if (toutes.isEmpty()) {
			return "";
		}
		return toutes.get(Math.floorMod(this.varianteChoisie, toutes.size()));
	}

	private String joueur() {
		return this.joueurs.isEmpty() ? ""
				: this.joueurs.get(Math.min(this.joueurChoisi, this.joueurs.size() - 1));
	}

	// --- Le montage -------------------------------------------------------------

	@Override
	protected void init() {
		int px = (this.width - LARGE) / 2;
		int py = (this.height - HAUT) / 2;
		int basDesListes = py + 26 + HAUT_LISTE;

		// Les fleches de couleur, sous l'apercu.
		int apercuX = px + MARGE + LARGE_ESPECES + MARGE;
		addRenderableWidget(BoutonDuMod.de(apercuX, basDesListes + 2, 16, 16,
				Component.literal("<"), bouton -> tournerLaCouleur(-1)));
		addRenderableWidget(BoutonDuMod.de(apercuX + LARGE_APERCU - 16, basDesListes + 2,
				16, 16, Component.literal(">"), bouton -> tournerLaCouleur(1)));

		addRenderableWidget(BoutonDuMod.de(px + (LARGE - LARGE_BOUTON) / 2,
				py + HAUT - MARGE - HAUT_BOUTON, LARGE_BOUTON, HAUT_BOUTON,
				Component.literal("Offrir"), bouton -> offrir()));
	}

	private void tournerLaCouleur(int sens) {
		List<String> toutes = variantes();
		if (toutes.size() < 2) {
			Bruits.refus();
			return;
		}
		this.varianteChoisie = Math.floorMod(this.varianteChoisie + sens, toutes.size());
		Bruits.page();
	}

	/**
	 * Envoie la demande.
	 *
	 * <p>L'ecran reste ouvert : on distribue rarement un seul compagnon, et
	 * refermer apres chaque don obligerait a tout re-choisir.
	 */
	private void offrir() {
		Espece espece = espece();
		if (espece == null || joueur().isEmpty()) {
			Bruits.refus();
			return;
		}
		ClientPlayNetworking.send(new PaquetRemettre(joueur(), espece.nom(), variante()));
		this.merci = Especes.titre(espece.nom()) + " offert a " + joueur();
		this.resteDuMerci = DUREE_DU_MERCI;
		Bruits.clic();
	}

	// --- Le dessin --------------------------------------------------------------

	@Override
	public void render(GuiGraphics g, int sourisX, int sourisY, float partiel) {
		super.render(g, sourisX, sourisY, partiel);

		int px = (this.width - LARGE) / 2;
		int py = (this.height - HAUT) / 2;
		Peinture.ombre(g, px, py, LARGE, HAUT);
		Peinture.panneau(g, px, py, LARGE, HAUT);

		g.drawCenteredString(this.font, this.title, px + LARGE / 2, py + 8, Peinture.OR);
		Peinture.filet(g, px + MARGE, py + 20, LARGE - 2 * MARGE);

		int hautDesListes = py + 26;
		int especesX = px + MARGE;
		int apercuX = especesX + LARGE_ESPECES + MARGE;
		int joueursX = apercuX + LARGE_APERCU + MARGE;

		this.hautListe = hautDesListes;
		this.hautJoueurs = hautDesListes;

		dessinerLesEspeces(g, especesX, hautDesListes, sourisX, sourisY);
		dessinerLApercu(g, apercuX, hautDesListes, sourisX, sourisY);
		dessinerLesJoueurs(g, joueursX, hautDesListes, sourisX, sourisY);

		// La phrase de confirmation, qui s'efface toute seule.
		if (this.resteDuMerci > 0) {
			this.resteDuMerci--;
			int reste = Math.min(255, this.resteDuMerci * 8);
			g.drawCenteredString(this.font, this.merci, px + LARGE / 2,
					py + HAUT - MARGE - HAUT_BOUTON - 12,
					(reste << 24) | (CONFIRME & 0xFFFFFF));
		}
		clochette(g, sourisX, sourisY, partiel);
	}

	private void dessinerLesEspeces(GuiGraphics g, int x, int y, int sourisX, int sourisY) {
		Peinture.carte(g, x, y, LARGE_ESPECES, HAUT_LISTE, false);
		int combien = Math.min(this.especes.size(), (HAUT_LISTE - 6) / LIGNE);
		int premier = Math.max(0, Math.min(this.especeChoisie - combien / 2,
				this.especes.size() - combien));

		for (int i = 0; i < combien; i++) {
			int index = premier + i;
			int ligneY = y + 4 + i * LIGNE;
			boolean choisi = index == this.especeChoisie;
			if (choisi) {
				g.fill(x + 2, ligneY - 1, x + LARGE_ESPECES - 2, ligneY + LIGNE - 2, CHOISI);
			} else if (sous(sourisX, sourisY, x, ligneY - 1, LARGE_ESPECES, LIGNE)) {
				g.fill(x + 2, ligneY - 1, x + LARGE_ESPECES - 2, ligneY + LIGNE - 2, SURVOL);
			}
			String titre = this.font.plainSubstrByWidth(
					Especes.titre(this.especes.get(index).nom()), LARGE_ESPECES - 10);
			g.drawString(this.font, titre, x + 5, ligneY,
					choisi ? Peinture.OR : Peinture.TEXTE, false);
		}
	}

	private void dessinerLApercu(GuiGraphics g, int x, int y, int sourisX, int sourisY) {
		Peinture.carte(g, x, y, LARGE_APERCU, HAUT_LISTE, true);
		Espece espece = espece();
		if (espece == null) {
			g.drawCenteredString(this.font, "aucune espece", x + LARGE_APERCU / 2,
					y + HAUT_LISTE / 2, Peinture.TEXTE_PALE);
			return;
		}
		// La bete regarde la souris, mais seulement quand elle est sur sa carte :
		// sinon elle se tord vers un curseur parti a l'autre bout de l'ecran.
		boolean dessus = sous(sourisX, sourisY, x, y, LARGE_APERCU, HAUT_LISTE);
		float viseX = dessus ? sourisX : x + LARGE_APERCU / 2.0F;
		float viseY = dessus ? sourisY : y + HAUT_LISTE * 0.35F;

		Apercu.dessiner(g, x + 4, y + 4, LARGE_APERCU - 8, HAUT_LISTE - 8,
				espece.nom(), variante(), viseX, viseY);

		String couleur = variante().replace('_', ' ');
		List<String> toutes = variantes();
		if (toutes.size() > 1) {
			couleur = couleur + "  (" + (Math.floorMod(this.varianteChoisie, toutes.size()) + 1)
					+ "/" + toutes.size() + ")";
		}
		g.drawCenteredString(this.font,
				this.font.plainSubstrByWidth(couleur, LARGE_APERCU - 40),
				x + LARGE_APERCU / 2, y + HAUT_LISTE + 6, Peinture.TEXTE_PALE);
	}

	private void dessinerLesJoueurs(GuiGraphics g, int x, int y, int sourisX, int sourisY) {
		Peinture.carte(g, x, y, LARGE_JOUEURS, HAUT_LISTE, false);
		if (this.joueurs.isEmpty()) {
			g.drawCenteredString(this.font, "personne", x + LARGE_JOUEURS / 2,
					y + HAUT_LISTE / 2, Peinture.TEXTE_PALE);
			return;
		}
		int combien = Math.min(this.joueurs.size(), (HAUT_LISTE - 6) / LIGNE);
		int premier = Math.max(0, Math.min(this.joueurChoisi - combien / 2,
				this.joueurs.size() - combien));

		for (int i = 0; i < combien; i++) {
			int index = premier + i;
			int ligneY = y + 4 + i * LIGNE;
			boolean choisi = index == this.joueurChoisi;
			if (choisi) {
				g.fill(x + 2, ligneY - 1, x + LARGE_JOUEURS - 2, ligneY + LIGNE - 2, CHOISI);
			} else if (sous(sourisX, sourisY, x, ligneY - 1, LARGE_JOUEURS, LIGNE)) {
				g.fill(x + 2, ligneY - 1, x + LARGE_JOUEURS - 2, ligneY + LIGNE - 2, SURVOL);
			}
			g.drawString(this.font,
					this.font.plainSubstrByWidth(this.joueurs.get(index), LARGE_JOUEURS - 10),
					x + 5, ligneY, choisi ? Peinture.OR : Peinture.TEXTE, false);
		}
	}

	// --- Les clics --------------------------------------------------------------

	@Override
	public boolean mouseClicked(double sourisX, double sourisY, int bouton) {
		if (clochetteCliquee(sourisX, sourisY, bouton)) {
			return true;
		}
		int px = (this.width - LARGE) / 2;
		int especesX = px + MARGE;
		int joueursX = especesX + LARGE_ESPECES + MARGE + LARGE_APERCU + MARGE;

		Integer surEspece = ligneSous(sourisX, sourisY, especesX, this.hautListe,
				LARGE_ESPECES, this.especes.size(), this.especeChoisie);
		if (surEspece != null) {
			if (surEspece != this.especeChoisie) {
				this.especeChoisie = surEspece;
				// Une couleur de l'espece precedente n'existe pas forcement ici.
				this.varianteChoisie = 0;
				Bruits.clic();
			}
			return true;
		}
		Integer surJoueur = ligneSous(sourisX, sourisY, joueursX, this.hautJoueurs,
				LARGE_JOUEURS, this.joueurs.size(), this.joueurChoisi);
		if (surJoueur != null) {
			if (surJoueur != this.joueurChoisi) {
				this.joueurChoisi = surJoueur;
				Bruits.clic();
			}
			return true;
		}
		return super.mouseClicked(sourisX, sourisY, bouton);
	}

	/** Quelle ligne de cette liste-la est sous la souris, ou {@code null}. */
	private Integer ligneSous(double sourisX, double sourisY, int x, int y, int large,
			int combienEnTout, int choisi) {
		if (combienEnTout == 0) {
			return null;
		}
		int visibles = Math.min(combienEnTout, (HAUT_LISTE - 6) / LIGNE);
		int premier = Math.max(0, Math.min(choisi - visibles / 2, combienEnTout - visibles));
		for (int i = 0; i < visibles; i++) {
			int ligneY = y + 4 + i * LIGNE;
			if (sous((int) sourisX, (int) sourisY, x, ligneY - 1, large, LIGNE)) {
				return premier + i;
			}
		}
		return null;
	}

	private static boolean sous(int sourisX, int sourisY, int x, int y, int large, int haut) {
		return sourisX >= x && sourisX < x + large && sourisY >= y && sourisY < y + haut;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
