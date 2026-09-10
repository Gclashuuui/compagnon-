package fr.lhdp.compagnon.client;

import fr.lhdp.compagnon.Icones;
import fr.lhdp.compagnon.reseau.PaquetJauges;
import fr.lhdp.compagnon.reseau.PaquetLivre;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * La fiche de sante lisible du compagnon.
 *
 * <p>Le HUD reste volontairement petit. H ouvre cette fiche sans mettre le jeu
 * en pause : la souris devient disponible, les quatre valeurs sont nommees et
 * le diagnostic donne une action claire. Le serveur ne renvoie rien pour cette
 * vue ; elle relit le dernier paquet deja necessaire au HUD.
 */
public final class EcranSante extends EcranCompagnon {

	private static final int LARGEUR = 300;
	private static final int HAUTEUR = 206;
	private static final int MARGE = 12;
	private static final int BOUTON = 20;
	private static final int LIGNE = 20;
	private static final int JAUGE_X = 98;
	private static final int JAUGE_LARGEUR = 140;

	private static final int FAIM = 0xFFEBA337;
	private static final int ENERGIE = 0xFF37C4EB;
	private static final int SANTE = 0xFF8BC458;
	private static final int COMPLICITE = 0xFFE16EAC;
	private static final int ALERTE = 0xFFD94F50;

	private int gauche;
	private int haut;
	private int choisi;

	public EcranSante() {
		super(Component.translatable("panneau.compagnon.carnet_sante"));
	}

	@Override
	protected void init() {
		this.gauche = (this.width - LARGEUR) / 2;
		this.haut = (this.height - HAUTEUR) / 2;
		bornerChoix();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void render(GuiGraphics g, int sourisX, int sourisY, float partiel) {
		g.fill(0, 0, this.width, this.height, 0x52000000);
		super.render(g, sourisX, sourisY, partiel);

		ThemeSante theme = Panneau.theme();
		fond(g, theme);
		g.drawString(this.font, this.title,
				this.gauche + (theme.illustre() ? 6 : MARGE),
				this.haut + (theme.illustre() ? 6 : 11),
				theme.encre, false);

		boutonVisibilite(g, sourisX, sourisY, theme);
		boutonTheme(g, sourisX, sourisY, theme);

		List<PaquetJauges.Jauge> jauges = Panneau.donnees();
		if (jauges.isEmpty()) {
			g.drawCenteredString(this.font,
					Component.translatable("panneau.compagnon.aucun_dehors"),
					this.gauche + LARGEUR / 2, this.haut + 95, theme.encrePale);
			return;
		}

		bornerChoix();
		dessinerOnglets(g, sourisX, sourisY, jauges, theme);
		PaquetJauges.Jauge jauge = jauges.get(this.choisi);
		if (theme.illustre()) {
			dessinerFicheIllustree(g, sourisX, sourisY, jauge, theme);
			return;
		}

		int y = this.haut + 62;
		g.drawString(this.font, jauge.nom(), this.gauche + MARGE, y - 12,
				theme.encre, false);
		Component etat = Panneau.etat(jauge);
		g.drawString(this.font, etat, this.gauche + LARGEUR - MARGE
				- this.font.width(etat), y - 12, Panneau.couleurEtat(jauge), false);

		ligne(g, theme, y, Icones.FAIM, "livre.compagnon.faim", jauge.faim(), FAIM);
		ligne(g, theme, y + LIGNE, Icones.ENERGIE, "livre.compagnon.energie",
				jauge.energie(), ENERGIE);
		ligne(g, theme, y + 2 * LIGNE, Icones.SOIN, "livre.compagnon.sante",
				jauge.sante(), SANTE);
		ligne(g, theme, y + 3 * LIGNE, Icones.COEUR, "livre.compagnon.complicite",
				jauge.complicite(), COMPLICITE);

		Component conseil = Panneau.conseil(jauge);
		g.drawString(this.font,
				this.font.plainSubstrByWidth(conseil.getString(), LARGEUR - 2 * MARGE),
				this.gauche + MARGE, this.haut + 149, theme.encrePale, false);

		int bx = this.gauche + LARGEUR - MARGE - 112;
		int by = this.haut + HAUTEUR - MARGE - 18;
		boolean survole = dans(sourisX, sourisY, bx, by, 112, 18);
		Peinture.boutonPeint(g, bx, by, 112, 18, survole ? 1.0F : 0.0F,
				false, theme.creuxHaut, theme.creuxBas, theme.cadre);
		Component ouvrir = Component.translatable("panneau.compagnon.ouvrir_journal");
		g.drawCenteredString(this.font, ouvrir, bx + 56, by + 5, theme.encre);

		if (survole) {
			g.renderTooltip(this.font,
					Component.translatable("panneau.compagnon.ouvrir_journal.aide"),
					sourisX, sourisY);
		}
	}

	/** Respecte au pixel pres le gabarit 300 x 206 fourni avec la collection. */
	private void dessinerFicheIllustree(GuiGraphics g, int sourisX, int sourisY,
			PaquetJauges.Jauge jauge, ThemeSante theme) {
		ligneIllustree(g, theme, 0, 53, "livre.compagnon.faim", jauge.faim(), FAIM);
		ligneIllustree(g, theme, 1, 81, "livre.compagnon.energie",
				jauge.energie(), ENERGIE);
		ligneIllustree(g, theme, 2, 109, "livre.compagnon.sante",
				jauge.sante(), SANTE);
		ligneIllustree(g, theme, 3, 137, "livre.compagnon.complicite",
				jauge.complicite(), COMPLICITE);

		Component etat = Panneau.etat(jauge);
		g.drawString(this.font, etat, this.gauche + 6, this.haut + 164,
				Panneau.couleurEtat(jauge), false);
		String conseil = this.font.plainSubstrByWidth(Panneau.conseil(jauge).getString(), 288);
		g.drawString(this.font, conseil, this.gauche + 6, this.haut + 174,
				theme.encrePale, false);

		int bx = this.gauche + 92;
		int by = this.haut + 185;
		boolean survole = dans(sourisX, sourisY, bx, by, 116, 18);
		if (survole) {
			g.renderOutline(bx, by, 116, 18, theme.accent);
			g.renderTooltip(this.font,
					Component.translatable("panneau.compagnon.ouvrir_journal.aide"),
					sourisX, sourisY);
		}
	}

	private void ligneIllustree(GuiGraphics g, ThemeSante theme, int icone,
			int y, String traduction, int valeur, int couleur) {
		int absoluY = this.haut + y;
		g.blit(theme.texture("health_icons"), this.gauche + 6, absoluY,
				16, 16, icone * 16.0F, 0.0F, 16, 16, 160, 16);
		g.drawString(this.font, Component.translatable(traduction),
				this.gauche + 28, absoluY - 3, theme.encre, false);
		barre(g, this.gauche + 28, absoluY + 8, 228, valeur,
				valeur <= 25 ? ALERTE : couleur, theme);
		String nombre = valeur + "%";
		g.drawString(this.font, nombre, this.gauche + 294 - this.font.width(nombre),
				absoluY + 6, theme.encre, false);
	}

	private void dessinerOnglets(GuiGraphics g, int sourisX, int sourisY,
			List<PaquetJauges.Jauge> jauges, ThemeSante theme) {
		int marge = theme.illustre() ? 6 : MARGE;
		int ecart = theme.illustre() ? 4 : 3;
		int hauteur = theme.illustre() ? 18 : 16;
		int disponible = theme.illustre() ? 260 : LARGEUR - 2 * MARGE;
		int largeur = Math.min(theme.illustre() ? 84 : 86,
				(disponible - (jauges.size() - 1) * ecart) / jauges.size());
		int x = this.gauche + marge;
		int y = this.haut + (theme.illustre() ? 26 : 31);
		for (int i = 0; i < jauges.size(); i++) {
			boolean actif = i == this.choisi;
			boolean survole = dans(sourisX, sourisY, x, y, largeur, hauteur);
			int fond = actif ? theme.accent : theme.creuxHaut;
			g.fill(x, y, x + largeur, y + hauteur,
					survole ? melanger(fond, theme.creuxBas) : fond);
			g.fill(x, y + hauteur - 1, x + largeur, y + hauteur,
					actif ? theme.cadre : theme.encrePale);
			String nom = this.font.plainSubstrByWidth(jauges.get(i).nom(), largeur - 8);
			g.drawCenteredString(this.font, nom, x + largeur / 2,
					y + (theme.illustre() ? 5 : 4),
					actif ? 0xFFFFFFFF : theme.encre);
			x += largeur + ecart;
		}
	}

	private void ligne(GuiGraphics g, ThemeSante theme, int y, String icone,
			String traduction, int valeur, int couleur) {
		int x = this.gauche + MARGE;
		g.drawString(this.font, Icones.de(icone), x, y + 2, theme.encrePale, false);
		g.drawString(this.font, Component.translatable(traduction), x + 13, y + 2,
				theme.encre, false);
		barre(g, this.gauche + JAUGE_X, y + 4, JAUGE_LARGEUR, valeur,
				valeur <= 25 ? ALERTE : couleur, theme);
		String nombre = valeur + "%";
		g.drawString(this.font, nombre, this.gauche + LARGEUR - MARGE
				- this.font.width(nombre), y + 2, theme.encre, false);
	}

	private void fond(GuiGraphics g, ThemeSante theme) {
		if (theme.illustre()) {
			boolean place = this.width >= 540 && this.height >= 406;
			if (place) {
				g.blit(theme.texture("health_card_overflow"),
						this.gauche - 120, this.haut - 100, 540, 406,
						0.0F, 0.0F, 540, 406, 540, 406);
			} else {
				g.blit(theme.texture("health_card"), this.gauche, this.haut,
						300, 206, 0.0F, 0.0F, 300, 206, 300, 206);
			}
			return;
		}
		g.fill(this.gauche + 3, this.haut + HAUTEUR, this.gauche + LARGEUR + 3,
				this.haut + HAUTEUR + 3, 0x66000000);
		g.fill(this.gauche + LARGEUR, this.haut + 3, this.gauche + LARGEUR + 3,
				this.haut + HAUTEUR, 0x66000000);
		g.fillGradient(this.gauche + 1, this.haut, this.gauche + LARGEUR - 1,
				this.haut + HAUTEUR, theme.fondHaut, theme.fondBas);
		g.fillGradient(this.gauche, this.haut + 1, this.gauche + LARGEUR,
				this.haut + HAUTEUR - 1, theme.fondHaut, theme.fondBas);
		g.renderOutline(this.gauche, this.haut, LARGEUR, HAUTEUR, theme.cadre);
		g.fill(this.gauche + MARGE, this.haut + 27,
				this.gauche + LARGEUR - MARGE, this.haut + 28, theme.encrePale);
	}

	private void boutonVisibilite(GuiGraphics g, int sourisX, int sourisY, ThemeSante theme) {
		int x = this.gauche + (theme.illustre() ? 271
				: LARGEUR - 2 * MARGE - 2 * BOUTON - 4);
		int y = this.haut + (theme.illustre() ? 2 : 6);
		boolean survole = dans(sourisX, sourisY, x, y, BOUTON, BOUTON);
		if (theme.illustre()) {
			String etat = appuye(survole) ? "visibility_button_pressed"
					: survole ? "visibility_button_hover" : "visibility_button_normal";
			g.blit(theme.texture(etat), x, y, BOUTON, BOUTON,
					0.0F, 0.0F, 24, 24, 24, 24);
		} else {
			petitBouton(g, x, y, survole, theme);
			String symbole = Panneau.replie() ? "×" : "•";
			g.drawCenteredString(this.font, symbole, x + BOUTON / 2, y + 6, theme.encre);
		}
		if (survole) {
			g.renderTooltip(this.font, Component.translatable(Panneau.replie()
					? "panneau.compagnon.afficher" : "panneau.compagnon.masquer"),
					sourisX, sourisY);
		}
	}

	private void boutonTheme(GuiGraphics g, int sourisX, int sourisY, ThemeSante theme) {
		int x = this.gauche + (theme.illustre() ? 242 : LARGEUR - MARGE - BOUTON);
		int y = this.haut + (theme.illustre() ? 2 : 6);
		boolean survole = dans(sourisX, sourisY, x, y, BOUTON, BOUTON);
		if (theme.illustre()) {
			String etat = appuye(survole) ? "theme_button_pressed"
					: survole ? "theme_button_hover" : "theme_button_normal";
			g.blit(theme.texture(etat), x, y, BOUTON, BOUTON,
					0.0F, 0.0F, 32, 32, 32, 32);
		} else {
			petitBouton(g, x, y, survole, theme);
			int c = BOUTON / 2;
			g.fill(x + 4, y + 4, x + c, y + c, theme.encre);
			g.fill(x + c, y + 4, x + 16, y + c, theme.accent);
			g.fill(x + 4, y + c, x + c, y + 16, theme.creuxBas);
			g.fill(x + c, y + c, x + 16, y + 16, theme.cadre);
		}
		if (survole) {
			g.renderTooltip(this.font, Component.translatable("panneau.compagnon.theme",
					Component.translatable(theme.traduction())), sourisX, sourisY);
		}
	}

	private static void petitBouton(GuiGraphics g, int x, int y, boolean survole,
			ThemeSante theme) {
		g.fill(x, y, x + BOUTON, y + BOUTON, theme.cadre);
		g.fill(x + 1, y + 1, x + BOUTON - 1, y + BOUTON - 1,
				survole ? theme.creuxBas : theme.creuxHaut);
	}

	private static void barre(GuiGraphics g, int x, int y, int largeur, int valeur,
			int couleur, ThemeSante theme) {
		g.fill(x, y, x + largeur, y + 7, theme.cadre);
		g.fillGradient(x + 1, y + 1, x + largeur - 1, y + 6,
				theme.creuxHaut, theme.creuxBas);
		int rempli = Math.round((largeur - 2) * Math.max(0, Math.min(100, valeur)) / 100.0F);
		if (rempli > 0) {
			g.fill(x + 1, y + 1, x + 1 + rempli, y + 6, couleur);
			g.fill(x + 1, y + 1, x + 1 + rempli, y + 2, 0x60FFFFFF);
		}
	}

	@Override
	public boolean mouseClicked(double sourisX, double sourisY, int bouton) {
		if (bouton != GLFW.GLFW_MOUSE_BUTTON_LEFT
				&& bouton != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			return super.mouseClicked(sourisX, sourisY, bouton);
		}

		ThemeSante theme = Panneau.theme();
		int visibiliteX = this.gauche + (theme.illustre() ? 271
				: LARGEUR - 2 * MARGE - 2 * BOUTON - 4);
		int boutonsY = this.haut + (theme.illustre() ? 2 : 6);
		if (bouton == GLFW.GLFW_MOUSE_BUTTON_LEFT
				&& dans(sourisX, sourisY, visibiliteX, boutonsY, BOUTON, BOUTON)) {
			Panneau.basculer();
			Bruits.clic();
			return true;
		}

		int themeX = this.gauche + (theme.illustre() ? 242 : LARGEUR - MARGE - BOUTON);
		if (dans(sourisX, sourisY, themeX, boutonsY, BOUTON, BOUTON)) {
			if (bouton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
				Minecraft.getInstance().setScreen(new EcranThemesSante(this, theme));
			} else {
				Panneau.changerTheme(true);
			}
			Bruits.clic();
			return true;
		}

		List<PaquetJauges.Jauge> jauges = Panneau.donnees();
		int disponible = theme.illustre() ? 260 : LARGEUR - 2 * MARGE;
		if (!jauges.isEmpty()) {
			int ecart = theme.illustre() ? 4 : 3;
			int hauteur = theme.illustre() ? 18 : 16;
			int largeur = Math.min(theme.illustre() ? 84 : 86,
					(disponible - (jauges.size() - 1) * ecart) / jauges.size());
			int x = this.gauche + (theme.illustre() ? 6 : MARGE);
			int y = this.haut + (theme.illustre() ? 26 : 31);
			for (int i = 0; i < jauges.size(); i++) {
				if (bouton == GLFW.GLFW_MOUSE_BUTTON_LEFT
						&& dans(sourisX, sourisY, x, y, largeur, hauteur)) {
					this.choisi = i;
					Bruits.clic();
					return true;
				}
				x += largeur + ecart;
			}
		}

		int bx = this.gauche + (theme.illustre() ? 92 : LARGEUR - MARGE - 112);
		int by = this.haut + (theme.illustre() ? 185 : HAUTEUR - MARGE - 18);
		if (bouton == GLFW.GLFW_MOUSE_BUTTON_LEFT && !jauges.isEmpty()
				&& dans(sourisX, sourisY, bx, by, 112, 18)) {
			PaquetJauges.Jauge jauge = jauges.get(Math.min(this.choisi, jauges.size() - 1));
			Touches.retenir(jauge.index());
			Bruits.page();
			ClientPlayNetworking.send(new PaquetLivre(jauge.index()));
			return true;
		}
		return super.mouseClicked(sourisX, sourisY, bouton);
	}

	@Override
	public boolean keyPressed(int touche, int codeMateriel, int modificateurs) {
		if (touche == GLFW.GLFW_KEY_H) {
			onClose();
			return true;
		}
		return super.keyPressed(touche, codeMateriel, modificateurs);
	}

	private void bornerChoix() {
		int combien = Panneau.donnees().size();
		this.choisi = combien == 0 ? 0 : Math.max(0, Math.min(this.choisi, combien - 1));
	}

	private static boolean dans(double sourisX, double sourisY, int x, int y,
			int largeur, int hauteur) {
		return sourisX >= x && sourisX < x + largeur
				&& sourisY >= y && sourisY < y + hauteur;
	}

	private static int melanger(int a, int b) {
		return Peinture.melanger(a, b, 0.45F);
	}

	private static boolean appuye(boolean survole) {
		return survole && GLFW.glfwGetMouseButton(
				Minecraft.getInstance().getWindow().getWindow(),
				GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
	}
}
