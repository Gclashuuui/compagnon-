package fr.lhdp.compagnon.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Sélection directe des habillages du carnet de santé. */
final class EcranThemesSante extends EcranCompagnon {

	private static final int CARTE_L = 64;
	private static final int CARTE_H = 50;
	private static final int ECART = 4;
	private static final int MARGE = 6;
	private static final int ENTETE = 28;
	private static final int PIED = 18;

	private final Screen retour;
	private ThemeSante selection;
	private int page;
	private int gauche;
	private int haut;
	private int colonnes;
	private int lignes;

	EcranThemesSante(Screen retour, ThemeSante selection) {
		super(Component.translatable("panneau.compagnon.themes.titre"));
		this.retour = retour;
		this.selection = selection;
	}

	@Override
	protected void init() {
		this.colonnes = Math.max(1, Math.min(4,
				(this.width - 2 * MARGE + ECART) / (CARTE_L + ECART)));
		this.lignes = Math.max(1, Math.min(2,
				(this.height - ENTETE - PIED - 2 * MARGE + ECART) / (CARTE_H + ECART)));
		this.gauche = (this.width - largeurPanneau()) / 2;
		this.haut = (this.height - hauteurPanneau()) / 2;
		this.page = Math.max(0, Math.min(this.page, pages() - 1));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void render(GuiGraphics g, int sourisX, int sourisY, float partiel) {
		g.fill(0, 0, this.width, this.height, 0x78000000);
		super.render(g, sourisX, sourisY, partiel);
		Peinture.panneau(g, this.gauche, this.haut, largeurPanneau(), hauteurPanneau());
		g.drawString(this.font, this.title, this.gauche + MARGE, this.haut + 7,
				Peinture.TEXTE, false);
		g.fill(this.gauche + MARGE, this.haut + 21,
				this.gauche + largeurPanneau() - MARGE, this.haut + 22, Peinture.OR_SOMBRE);

		ThemeSante[] themes = ThemeSante.values();
		int debut = this.page * parPage();
		int fin = Math.min(themes.length, debut + parPage());
		for (int i = debut; i < fin; i++) {
			int local = i - debut;
			dessinerCarte(g, sourisX, sourisY, carteX(local), carteY(local), themes[i]);
		}
		dessinerPagination(g, sourisX, sourisY);
	}

	private void dessinerCarte(GuiGraphics g, int sourisX, int sourisY,
			int x, int y, ThemeSante theme) {
		boolean survole = dans(sourisX, sourisY, x, y, CARTE_L, CARTE_H);
		boolean choisi = theme == this.selection;
		Peinture.carte(g, x, y, CARTE_L, CARTE_H, choisi || survole);

		if (theme.illustre()) {
			// Réduction exacte à 50 % : deux pixels source deviennent toujours
			// un pixel d'interface, sans alternance de colonnes ni flou.
			g.blit(theme.texture("book_closed"), x + 22, y + 3,
					20, 25, 0.0F, 0.0F, 40, 50, 40, 50);
		} else {
			dessinerLivreSimple(g, x + 22, y + 3, theme);
		}
		if (choisi) {
			g.renderOutline(x + 2, y + 2, CARTE_L - 4, 28, theme.accent);
		}

		String nom = Component.translatable(theme.traduction()).getString();
		nom = this.font.plainSubstrByWidth(nom, CARTE_L - 8);
		g.drawCenteredString(this.font, nom, x + CARTE_L / 2, y + 36,
				choisi ? Peinture.OR : Peinture.TEXTE);
		if (survole) {
			g.renderTooltip(this.font,
					Component.translatable("panneau.compagnon.themes.choisir",
							Component.translatable(theme.traduction())), sourisX, sourisY);
		}
	}

	private static void dessinerLivreSimple(GuiGraphics g, int x, int y,
			ThemeSante theme) {
		g.fill(x + 1, y, x + 19, y + 25, theme.cadre);
		g.fillGradient(x + 3, y + 2, x + 17, y + 23, theme.fondHaut, theme.fondBas);
		g.fill(x + 3, y + 3, x + 5, y + 22, theme.accent);
		g.fill(x + 7, y + 6, x + 15, y + 8, theme.encrePale);
		g.fill(x + 7, y + 11, x + 13, y + 13, theme.encrePale);
	}

	private void dessinerPagination(GuiGraphics g, int sourisX, int sourisY) {
		int y = this.haut + hauteurPanneau() - 13;
		if (this.page > 0) {
			int x = this.gauche + MARGE;
			g.drawCenteredString(this.font, "<", x + 10, y,
					dans(sourisX, sourisY, x, y - 3, 20, 18) ? Peinture.OR : Peinture.TEXTE);
		}
		if (this.page + 1 < pages()) {
			int x = this.gauche + largeurPanneau() - MARGE - 20;
			g.drawCenteredString(this.font, ">", x + 10, y,
					dans(sourisX, sourisY, x, y - 3, 20, 18) ? Peinture.OR : Peinture.TEXTE);
		}
		g.drawCenteredString(this.font,
				Component.translatable("panneau.compagnon.themes.page", this.page + 1, pages()),
				this.gauche + largeurPanneau() / 2, y, Peinture.TEXTE_PALE);
	}

	@Override
	public boolean mouseClicked(double sourisX, double sourisY, int bouton) {
		if (bouton != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			return super.mouseClicked(sourisX, sourisY, bouton);
		}
		ThemeSante[] themes = ThemeSante.values();
		int debut = this.page * parPage();
		int fin = Math.min(themes.length, debut + parPage());
		for (int i = debut; i < fin; i++) {
			int local = i - debut;
			if (dans(sourisX, sourisY, carteX(local), carteY(local), CARTE_L, CARTE_H)) {
				this.selection = themes[i];
				Panneau.choisirTheme(this.selection);
				Bruits.page();
				Minecraft.getInstance().setScreen(this.retour);
				return true;
			}
		}

		int y = this.haut + hauteurPanneau() - 16;
		if (this.page > 0 && dans(sourisX, sourisY, this.gauche + MARGE, y, 20, 18)) {
			this.page--;
			Bruits.page();
			return true;
		}
		if (this.page + 1 < pages() && dans(sourisX, sourisY,
				this.gauche + largeurPanneau() - MARGE - 20, y, 20, 18)) {
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

	private int pages() {
		return Math.max(1, (ThemeSante.values().length + parPage() - 1) / parPage());
	}

	private int carteX(int local) {
		return this.gauche + MARGE + (local % this.colonnes) * (CARTE_L + ECART);
	}

	private int carteY(int local) {
		return this.haut + ENTETE + (local / this.colonnes) * (CARTE_H + ECART);
	}

	private static boolean dans(double sourisX, double sourisY, int x, int y,
			int largeur, int hauteur) {
		return sourisX >= x && sourisX < x + largeur
				&& sourisY >= y && sourisY < y + hauteur;
	}
}
