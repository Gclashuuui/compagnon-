package fr.lhdp.compagnon.client;

import fr.lhdp.compagnon.Icones;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Consumer;

/**
 * La bibliotheque des apparences du Journal des Liens.
 *
 * <p>Avec plus de vingt themes, un bouton « suivant » n'est plus un choix :
 * c'est une loterie. Cette grille montre le livre avant de l'appliquer, place
 * les favoris en premier et permet de n'afficher qu'eux. Tout reste local au
 * joueur ; aucun paquet ne part au serveur.
 */
final class EcranThemesLivre extends EcranCompagnon {

	private static final int CARTE_L = 94;
	private static final int CARTE_H = 68;
	private static final int ECART = 6;
	private static final int MARGE = 8;
	private static final int ENTETE = 42;
	private static final int PIED = 25;
	private static final int APERCU_H = 48;

	private final Screen retour;
	private final Consumer<ThemeLivre> appliquer;
	private ThemeLivre selection;
	private boolean seulementFavoris;
	private int page;
	private int gauche;
	private int haut;
	private int colonnes;
	private int lignes;

	EcranThemesLivre(Screen retour, ThemeLivre selection,
			Consumer<ThemeLivre> appliquer) {
		super(Component.translatable("livre.compagnon.themes.titre"));
		this.retour = retour;
		this.selection = selection;
		this.appliquer = appliquer;
	}

	@Override
	protected void init() {
		this.colonnes = Math.max(1, Math.min(4, (this.width - 2 * MARGE + ECART)
				/ (CARTE_L + ECART)));
		this.lignes = Math.max(1, Math.min(3, (this.height - ENTETE - PIED
				- 2 * MARGE + ECART) / (CARTE_H + ECART)));
		int largeur = largeurPanneau();
		int hauteur = hauteurPanneau();
		this.gauche = (this.width - largeur) / 2;
		this.haut = (this.height - hauteur) / 2;
		bornerPage();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void render(GuiGraphics g, int sourisX, int sourisY, float partiel) {
		g.fill(0, 0, this.width, this.height, 0x78000000);
		super.render(g, sourisX, sourisY, partiel);

		int largeur = largeurPanneau();
		int hauteur = hauteurPanneau();
		Peinture.panneau(g, this.gauche, this.haut, largeur, hauteur);
		g.drawString(this.font, this.title, this.gauche + MARGE, this.haut + 10,
				Peinture.TEXTE, false);

		dessinerFiltre(g, sourisX, sourisY);
		g.fill(this.gauche + MARGE, this.haut + 31,
				this.gauche + largeur - MARGE, this.haut + 32, Peinture.OR_SOMBRE);

		List<ThemeLivre> catalogue = ThemeLivre.catalogue(this.seulementFavoris);
		int debut = this.page * parPage();
		int fin = Math.min(catalogue.size(), debut + parPage());
		if (catalogue.isEmpty()) {
			g.drawCenteredString(this.font,
					Component.translatable("livre.compagnon.themes.aucun_favori"),
					this.gauche + largeur / 2, this.haut + ENTETE + 28,
					Peinture.TEXTE_PALE);
		}

		for (int position = debut; position < fin; position++) {
			int locale = position - debut;
			int x = carteX(locale);
			int y = carteY(locale);
			dessinerCarte(g, sourisX, sourisY, x, y, catalogue.get(position));
		}

		dessinerPagination(g, catalogue.size(), sourisX, sourisY);
	}

	private void dessinerCarte(GuiGraphics g, int sourisX, int sourisY, int x, int y,
			ThemeLivre theme) {
		boolean survole = dans(sourisX, sourisY, x, y, CARTE_L, CARTE_H);
		boolean choisi = theme == this.selection;
		Peinture.carte(g, x, y, CARTE_L, CARTE_H, choisi || survole);

		g.blit(theme.texture("book_astra_v2"), x + 3, y + 3,
				CARTE_L - 6, APERCU_H, 0.0F, 0.0F, 384, 256, 384, 256);
		if (choisi) {
			g.renderOutline(x + 1, y + 1, CARTE_L - 2, APERCU_H + 4, Peinture.OR);
		}

		String nom = Component.translatable(theme.traduction()).getString();
		nom = this.font.plainSubstrByWidth(nom, CARTE_L - 20);
		g.drawString(this.font, nom, x + 4, y + CARTE_H - 12,
				choisi ? Peinture.OR : Peinture.TEXTE, false);

		int etoileX = x + CARTE_L - 15;
		int etoileY = y + CARTE_H - 14;
		g.drawString(this.font, Icones.de(Icones.ETOILE), etoileX, etoileY,
				theme.favori() ? Peinture.OR : 0xFF5C536C, false);

		if (dans(sourisX, sourisY, etoileX - 2, etoileY - 2, 14, 14)) {
			g.renderTooltip(this.font, Component.translatable(theme.favori()
					? "livre.compagnon.themes.retirer_favori"
					: "livre.compagnon.themes.ajouter_favori"), sourisX, sourisY);
		} else if (survole) {
			g.renderTooltip(this.font,
					Component.translatable("livre.compagnon.themes.choisir", nom),
					sourisX, sourisY);
		}
	}

	private void dessinerFiltre(GuiGraphics g, int sourisX, int sourisY) {
		int x = this.gauche + largeurPanneau() - MARGE - 96;
		int y = this.haut + 7;
		boolean survole = dans(sourisX, sourisY, x, y, 96, 20);
		Peinture.bouton(g, x, y, 96, 20, survole ? 1.0F : 0.0F,
				this.seulementFavoris);
		Component texte = Icones.devant(Icones.ETOILE, Component.translatable(
				this.seulementFavoris ? "livre.compagnon.themes.tous"
						: "livre.compagnon.themes.favoris"));
		g.drawCenteredString(this.font, texte, x + 48, y + 6,
				this.seulementFavoris ? Peinture.OR : Peinture.TEXTE);
	}

	private void dessinerPagination(GuiGraphics g, int combien, int sourisX, int sourisY) {
		int pages = pages(combien);
		int y = this.haut + hauteurPanneau() - 18;
		if (this.page > 0) {
			int x = this.gauche + MARGE;
			boolean survole = dans(sourisX, sourisY, x, y - 3, 24, 18);
			g.drawCenteredString(this.font, "<", x + 12, y,
					survole ? Peinture.OR : Peinture.TEXTE);
		}
		if (this.page + 1 < pages) {
			int x = this.gauche + largeurPanneau() - MARGE - 24;
			boolean survole = dans(sourisX, sourisY, x, y - 3, 24, 18);
			g.drawCenteredString(this.font, ">", x + 12, y,
					survole ? Peinture.OR : Peinture.TEXTE);
		}
		Component compte = Component.translatable("livre.compagnon.themes.page",
				Math.min(this.page + 1, pages), pages);
		g.drawCenteredString(this.font, compte,
				this.gauche + largeurPanneau() / 2, y, Peinture.TEXTE_PALE);
	}

	@Override
	public boolean mouseClicked(double sourisX, double sourisY, int bouton) {
		if (bouton != GLFW.GLFW_MOUSE_BUTTON_LEFT
				&& bouton != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			return super.mouseClicked(sourisX, sourisY, bouton);
		}

		int filtreX = this.gauche + largeurPanneau() - MARGE - 96;
		if (bouton == GLFW.GLFW_MOUSE_BUTTON_LEFT
				&& dans(sourisX, sourisY, filtreX, this.haut + 7, 96, 20)) {
			this.seulementFavoris = !this.seulementFavoris;
			this.page = 0;
			Bruits.clic();
			return true;
		}

		List<ThemeLivre> catalogue = ThemeLivre.catalogue(this.seulementFavoris);
		int debut = this.page * parPage();
		int fin = Math.min(catalogue.size(), debut + parPage());
		for (int position = debut; position < fin; position++) {
			int locale = position - debut;
			int x = carteX(locale);
			int y = carteY(locale);
			ThemeLivre theme = catalogue.get(position);
			int etoileX = x + CARTE_L - 15;
			int etoileY = y + CARTE_H - 14;
			boolean etoile = dans(sourisX, sourisY, etoileX - 2, etoileY - 2, 14, 14);
			if (dans(sourisX, sourisY, x, y, CARTE_L, CARTE_H)) {
				if (bouton == GLFW.GLFW_MOUSE_BUTTON_RIGHT || etoile) {
					theme.basculerFavori();
					bornerPage();
					Bruits.clic();
					return true;
				}
				this.selection = theme;
				this.appliquer.accept(theme);
				theme.sauvegarder();
				Bruits.page();
				Minecraft.getInstance().setScreen(this.retour);
				return true;
			}
		}

		int y = this.haut + hauteurPanneau() - 21;
		if (bouton == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.page > 0
				&& dans(sourisX, sourisY, this.gauche + MARGE, y, 24, 18)) {
			this.page--;
			Bruits.page();
			return true;
		}
		if (bouton == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.page + 1 < pages(catalogue.size())
				&& dans(sourisX, sourisY,
						this.gauche + largeurPanneau() - MARGE - 24, y, 24, 18)) {
			this.page++;
			Bruits.page();
			return true;
		}
		return super.mouseClicked(sourisX, sourisY, bouton);
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(this.retour);
	}

	private int largeurPanneau() {
		return 2 * MARGE + this.colonnes * CARTE_L + (this.colonnes - 1) * ECART;
	}

	private int hauteurPanneau() {
		return ENTETE + this.lignes * CARTE_H + (this.lignes - 1) * ECART + PIED;
	}

	private int parPage() {
		return this.colonnes * this.lignes;
	}

	private int pages(int combien) {
		return Math.max(1, (combien + parPage() - 1) / parPage());
	}

	private void bornerPage() {
		int maximum = pages(ThemeLivre.catalogue(this.seulementFavoris).size()) - 1;
		this.page = Math.max(0, Math.min(this.page, maximum));
	}

	private int carteX(int locale) {
		return this.gauche + MARGE + (locale % this.colonnes) * (CARTE_L + ECART);
	}

	private int carteY(int locale) {
		return this.haut + ENTETE + (locale / this.colonnes) * (CARTE_H + ECART);
	}

	private static boolean dans(double sourisX, double sourisY, int x, int y,
			int largeur, int hauteur) {
		return sourisX >= x && sourisX < x + largeur
				&& sourisY >= y && sourisY < y + hauteur;
	}
}
