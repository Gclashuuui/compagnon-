package fr.lhdp.compagnon.client;

import fr.lhdp.compagnon.Icones;
import fr.lhdp.compagnon.reseau.PaquetJauges;
import fr.lhdp.compagnon.reseau.PaquetLivre;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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

	private static final int FAIM = 0xFFD07A2E;
	private static final int ENERGIE = 0xFFD4B02A;
	private static final int SANTE = 0xFFB94A52;
	private static final int COMPLICITE = 0xFFB95285;
	private static final int ALERTE = 0xFFE0574E;

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
		g.drawString(this.font, this.title, this.gauche + MARGE, this.haut + 11,
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

	private void dessinerOnglets(GuiGraphics g, int sourisX, int sourisY,
			List<PaquetJauges.Jauge> jauges, ThemeSante theme) {
		int disponible = LARGEUR - 2 * MARGE;
		int largeur = Math.min(86, (disponible - (jauges.size() - 1) * 3) / jauges.size());
		int x = this.gauche + MARGE;
		int y = this.haut + 31;
		for (int i = 0; i < jauges.size(); i++) {
			boolean actif = i == this.choisi;
			boolean survole = dans(sourisX, sourisY, x, y, largeur, 16);
			int fond = actif ? theme.accent : theme.creuxHaut;
			g.fill(x, y, x + largeur, y + 16, survole ? melanger(fond, theme.creuxBas) : fond);
			g.fill(x, y + 15, x + largeur, y + 16, actif ? theme.cadre : theme.encrePale);
			String nom = this.font.plainSubstrByWidth(jauges.get(i).nom(), largeur - 8);
			g.drawCenteredString(this.font, nom, x + largeur / 2, y + 4,
					actif ? 0xFFFFFFFF : theme.encre);
			x += largeur + 3;
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
		int x = this.gauche + LARGEUR - 2 * MARGE - 2 * BOUTON - 4;
		int y = this.haut + 6;
		boolean survole = dans(sourisX, sourisY, x, y, BOUTON, BOUTON);
		petitBouton(g, x, y, survole, theme);
		String symbole = Panneau.replie() ? "×" : "•";
		g.drawCenteredString(this.font, symbole, x + BOUTON / 2, y + 6, theme.encre);
		if (survole) {
			g.renderTooltip(this.font, Component.translatable(Panneau.replie()
					? "panneau.compagnon.afficher" : "panneau.compagnon.masquer"),
					sourisX, sourisY);
		}
	}

	private void boutonTheme(GuiGraphics g, int sourisX, int sourisY, ThemeSante theme) {
		int x = this.gauche + LARGEUR - MARGE - BOUTON;
		int y = this.haut + 6;
		boolean survole = dans(sourisX, sourisY, x, y, BOUTON, BOUTON);
		petitBouton(g, x, y, survole, theme);
		int c = BOUTON / 2;
		g.fill(x + 4, y + 4, x + c, y + c, theme.encre);
		g.fill(x + c, y + 4, x + 16, y + c, theme.accent);
		g.fill(x + 4, y + c, x + c, y + 16, theme.creuxBas);
		g.fill(x + c, y + c, x + 16, y + 16, theme.cadre);
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

		int visibiliteX = this.gauche + LARGEUR - 2 * MARGE - 2 * BOUTON - 4;
		int boutonsY = this.haut + 6;
		if (bouton == GLFW.GLFW_MOUSE_BUTTON_LEFT
				&& dans(sourisX, sourisY, visibiliteX, boutonsY, BOUTON, BOUTON)) {
			Panneau.basculer();
			Bruits.clic();
			return true;
		}

		int themeX = this.gauche + LARGEUR - MARGE - BOUTON;
		if (dans(sourisX, sourisY, themeX, boutonsY, BOUTON, BOUTON)) {
			Panneau.changerTheme(bouton == GLFW.GLFW_MOUSE_BUTTON_RIGHT);
			Bruits.clic();
			return true;
		}

		List<PaquetJauges.Jauge> jauges = Panneau.donnees();
		int disponible = LARGEUR - 2 * MARGE;
		if (!jauges.isEmpty()) {
			int largeur = Math.min(86, (disponible - (jauges.size() - 1) * 3) / jauges.size());
			int x = this.gauche + MARGE;
			for (int i = 0; i < jauges.size(); i++) {
				if (bouton == GLFW.GLFW_MOUSE_BUTTON_LEFT
						&& dans(sourisX, sourisY, x, this.haut + 31, largeur, 16)) {
					this.choisi = i;
					Bruits.clic();
					return true;
				}
				x += largeur + 3;
			}
		}

		int bx = this.gauche + LARGEUR - MARGE - 112;
		int by = this.haut + HAUTEUR - MARGE - 18;
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
}
