package fr.lhdp.compagnon.client;

import fr.lhdp.compagnon.Compagnon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Consumer;

/** Bibliothèque illustrée des apparences du Journal des Liens. */
final class EcranThemesLivre extends EcranCompagnon {

	private static final ResourceLocation PANNEAU = texture("library_panel_hd");
	private static final ResourceLocation CARTES = texture("library_card_states_hd");
	private static final ResourceLocation FAVORIS = texture("favorite_button_states_hd");
	private static final ResourceLocation FILTRES = texture("filter_tabs_states_hd");
	private static final ResourceLocation PAGINATION = texture("pagination_states_hd");
	private static final ResourceLocation VIDE = texture("empty_favorites_hd");
	private static final ResourceLocation DETAILS = texture("library_details_overlay");
	/** Police Unicode régulière : plus fine et indépendante du pack de ressources. */
	private static final ResourceLocation POLICE =
			ResourceLocation.withDefaultNamespace("uniform");

	private static final int LARGEUR = 416;
	private static final int HAUTEUR = 284;
	private static final int CARTE_L = 94;
	private static final int CARTE_H = 68;
	private static final int PAR_PAGE = 12;
	private static final int[][] POSITIONS = {
		{14, 48}, {112, 48}, {210, 48}, {308, 48},
		{14, 118}, {112, 118}, {210, 118}, {308, 118},
		{14, 188}, {112, 188}, {210, 188}, {308, 188}
	};

	private static final int ENCRE = 0xFF3C271B;
	private static final int ENCRE_PALE = 0xFFF2E1BD;
	private static final int OR = 0xFFFFD36D;

	private final Screen retour;
	private final Consumer<ThemeLivre> appliquer;
	private ThemeLivre selection;
	private boolean seulementFavoris;
	private int page;
	private int gauche;
	private int haut;
	private float echelle = 1.0F;
	private Component infoBulle;

	EcranThemesLivre(Screen retour, ThemeLivre selection,
			Consumer<ThemeLivre> appliquer) {
		super(Component.translatable("livre.compagnon.themes.titre"));
		this.retour = retour;
		this.selection = selection;
		this.appliquer = appliquer;
	}

	@Override
	protected void init() {
		this.echelle = Math.min(1.0F, Math.min(
				(this.width - 8.0F) / LARGEUR, (this.height - 8.0F) / HAUTEUR));
		this.echelle = Math.max(0.5F, this.echelle);
		this.gauche = Math.round((this.width - LARGEUR * this.echelle) / 2.0F);
		this.haut = Math.round((this.height - HAUTEUR * this.echelle) / 2.0F);
		bornerPage();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void render(GuiGraphics g, int sourisX, int sourisY, float partiel) {
		g.fill(0, 0, this.width, this.height, 0x8C000000);
		super.render(g, sourisX, sourisY, partiel);

		int mx = logiqueX(sourisX);
		int my = logiqueY(sourisY);
		this.infoBulle = null;
		List<ThemeLivre> catalogue = ThemeLivre.catalogue(this.seulementFavoris);

		g.pose().pushPose();
		g.pose().translate(this.gauche, this.haut, 0.0F);
		g.pose().scale(this.echelle, this.echelle, 1.0F);

		g.blit(PANNEAU, 0, 0, LARGEUR, HAUTEUR,
				0.0F, 0.0F, 1664, 1136, 1664, 1136);
		dessinerCartes(g, catalogue, mx, my, false);
		dessinerFiltres(g, mx, my, false);
		dessinerPagination(g, catalogue.size(), mx, my);
		g.blit(DETAILS, 0, 0, LARGEUR, HAUTEUR,
				0.0F, 0.0F, 1664, 1136, 1664, 1136);

		dessinerTitre(g);
		dessinerFiltres(g, mx, my, true);
		dessinerCartes(g, catalogue, mx, my, true);
		if (catalogue.isEmpty()) {
			dessinerVide(g);
		}
		g.pose().popPose();

		if (this.infoBulle != null) {
			g.renderTooltip(this.font, this.infoBulle, sourisX, sourisY);
		}
	}

	private void dessinerTitre(GuiGraphics g) {
		dessinerTexteCentre(g, police(Component.translatable(
				"livre.compagnon.themes.titre")), 107, 21, ENCRE, 0.82F);
	}

	private void dessinerFiltres(GuiGraphics g, int sourisX, int sourisY,
			boolean textes) {
		if (textes) {
			dessinerTexteCentre(g, police(Component.translatable(
					"livre.compagnon.themes.tous_court")), 256, 22, ENCRE, 0.85F);
			dessinerTexteCentre(g, police(Component.translatable(
					"livre.compagnon.themes.favoris_court")), 362, 22, ENCRE, 0.85F);
			return;
		}

		boolean tousSurvole = dans(sourisX, sourisY, 192, 15, 104, 22);
		boolean favorisSurvole = dans(sourisX, sourisY, 298, 15, 104, 22);
		int etatTous = (!this.seulementFavoris || tousSurvole) ? 1 : 0;
		int etatFavoris = (this.seulementFavoris || favorisSurvole) ? 3 : 2;
		g.blit(FILTRES, 192, 15, 104, 22,
				etatTous * 416.0F, 0.0F, 416, 88, 1664, 88);
		g.blit(FILTRES, 298, 15, 104, 22,
				etatFavoris * 416.0F, 0.0F, 416, 88, 1664, 88);
	}

	private void dessinerCartes(GuiGraphics g, List<ThemeLivre> catalogue,
			int sourisX, int sourisY, boolean textes) {
		int debut = this.page * PAR_PAGE;
		int fin = Math.min(catalogue.size(), debut + PAR_PAGE);
		for (int position = debut; position < fin; position++) {
			int local = position - debut;
			int x = POSITIONS[local][0];
			int y = POSITIONS[local][1];
			ThemeLivre theme = catalogue.get(position);
			boolean survole = dans(sourisX, sourisY, x, y, CARTE_L, CARTE_H);
			boolean etoileSurvolee = dans(sourisX, sourisY, x + 77, y + 2, 16, 16);

			if (textes) {
				String nomComplet = Component.translatable(theme.traduction()).getString();
				Component nom = abreger(nomComplet, 85);
				dessinerTexteCentre(g, nom, x + 41, y + 55,
						theme == this.selection ? OR : ENCRE, 0.75F);
				if (etoileSurvolee) {
					this.infoBulle = Component.translatable(theme.favori()
							? "livre.compagnon.themes.retirer_favori"
							: "livre.compagnon.themes.ajouter_favori");
				} else if (survole) {
					this.infoBulle = Component.translatable(
							"livre.compagnon.themes.choisir", nomComplet);
				}
				continue;
			}

			// Le livre entier est légèrement reculé dans sa vitrine. Le format
			// 54 x 36 garde exactement son rapport 3:2 et laisse respirer le cadre.
			g.blit(theme.texture("book_astra_v2"), x + 20, y + 12,
					54, 36, 0.0F, 0.0F, 384, 256, 384, 256);
			int etatCarte = theme == this.selection
					? theme.favori() ? 3 : 2
					: survole ? 1 : 0;
			g.blit(CARTES, x, y, CARTE_L, CARTE_H,
					etatCarte * 376.0F, 0.0F, 376, 272, 1504, 272);

			int etatFavori = theme.favori()
					? etoileSurvolee ? 3 : 2
					: etoileSurvolee ? 1 : 0;
			g.blit(FAVORIS, x + 77, y + 2, 16, 16,
					etatFavori * 64.0F, 0.0F, 64, 64, 256, 64);
		}
	}

	private void dessinerPagination(GuiGraphics g, int combien,
			int sourisX, int sourisY) {
		int pages = pages(combien);
		if (this.page > 0) {
			int etat = dans(sourisX, sourisY, 163, 262, 18, 18) ? 1 : 0;
			g.blit(PAGINATION, 163, 262, 18, 18,
					etat * 72.0F, 0.0F, 72, 72, 432, 72);
		}
		if (this.page + 1 < pages) {
			int etat = dans(sourisX, sourisY, 235, 262, 18, 18) ? 3 : 2;
			g.blit(PAGINATION, 235, 262, 18, 18,
					etat * 72.0F, 0.0F, 72, 72, 432, 72);
		}

		int visibles = Math.min(3, pages);
		int debutVisible = premierePageVisible(pages);
		int premier = 199 - (visibles - 1) * 9;
		for (int i = 0; i < visibles; i++) {
			int pageAffichee = debutVisible + i;
			int etat = pageAffichee == this.page ? 5 : 4;
			g.blit(PAGINATION, premier + i * 18, 262, 18, 18,
					etat * 72.0F, 0.0F, 72, 72, 432, 72);
		}
	}

	private void dessinerVide(GuiGraphics g) {
		g.blit(VIDE, 176, 117, 64, 64,
				0.0F, 0.0F, 256, 256, 256, 256);
		List<net.minecraft.util.FormattedCharSequence> lignes = this.font.split(
				Component.translatable("livre.compagnon.themes.aucun_favori"), 276);
		int y = 188;
		for (int i = 0; i < Math.min(2, lignes.size()); i++) {
			g.drawCenteredString(this.font, lignes.get(i), 208, y + i * 10, ENCRE_PALE);
		}
	}

	@Override
	public boolean mouseClicked(double sourisX, double sourisY, int bouton) {
		if (bouton != GLFW.GLFW_MOUSE_BUTTON_LEFT
				&& bouton != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			return super.mouseClicked(sourisX, sourisY, bouton);
		}
		int x = logiqueX(sourisX);
		int y = logiqueY(sourisY);

		if (bouton == GLFW.GLFW_MOUSE_BUTTON_LEFT
				&& dans(x, y, 192, 15, 104, 22)) {
			this.seulementFavoris = false;
			this.page = 0;
			Bruits.clic();
			return true;
		}
		if (bouton == GLFW.GLFW_MOUSE_BUTTON_LEFT
				&& dans(x, y, 298, 15, 104, 22)) {
			this.seulementFavoris = true;
			this.page = 0;
			Bruits.clic();
			return true;
		}

		List<ThemeLivre> catalogue = ThemeLivre.catalogue(this.seulementFavoris);
		int debut = this.page * PAR_PAGE;
		int fin = Math.min(catalogue.size(), debut + PAR_PAGE);
		for (int position = debut; position < fin; position++) {
			int local = position - debut;
			int cx = POSITIONS[local][0];
			int cy = POSITIONS[local][1];
			if (!dans(x, y, cx, cy, CARTE_L, CARTE_H)) {
				continue;
			}
			ThemeLivre theme = catalogue.get(position);
			if (bouton == GLFW.GLFW_MOUSE_BUTTON_RIGHT
					|| dans(x, y, cx + 77, cy + 2, 16, 16)) {
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

		if (bouton == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.page > 0
				&& dans(x, y, 163, 262, 18, 18)) {
			this.page--;
			Bruits.page();
			return true;
		}
		if (bouton == GLFW.GLFW_MOUSE_BUTTON_LEFT
				&& this.page + 1 < pages(catalogue.size())
				&& dans(x, y, 235, 262, 18, 18)) {
			this.page++;
			Bruits.page();
			return true;
		}

		if (bouton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			int pages = pages(catalogue.size());
			int visibles = Math.min(3, pages);
			int debutVisible = premierePageVisible(pages);
			int premier = 199 - (visibles - 1) * 9;
			for (int i = 0; i < visibles; i++) {
				if (dans(x, y, premier + i * 18, 262, 18, 18)) {
					this.page = debutVisible + i;
					Bruits.page();
					return true;
				}
			}
		}
		return super.mouseClicked(sourisX, sourisY, bouton);
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(this.retour);
	}

	private Component abreger(String texte, int largeur) {
		Component complet = police(Component.literal(texte));
		if (this.font.width(complet) <= largeur) {
			return complet;
		}
		String fin = "…";
		int caracteres = texte.length();
		while (caracteres > 0) {
			Component candidat = police(Component.literal(
					texte.substring(0, caracteres).stripTrailing() + fin));
			if (this.font.width(candidat) <= largeur) {
				return candidat;
			}
			caracteres--;
		}
		return police(Component.literal(fin));
	}

	private void dessinerTexteCentre(GuiGraphics g, Component texte, int centreX,
			int y, int couleur, float echelleTexte) {
		g.pose().pushPose();
		g.pose().translate(centreX, y, 0.0F);
		g.pose().scale(echelleTexte, echelleTexte, 1.0F);
		g.drawCenteredString(this.font, texte, 0, 0, couleur);
		g.pose().popPose();
	}

	private static Component police(Component texte) {
		return texte.copy().withStyle(Style.EMPTY.withFont(POLICE));
	}

	private int pages(int combien) {
		return Math.max(1, (combien + PAR_PAGE - 1) / PAR_PAGE);
	}

	private int premierePageVisible(int pages) {
		if (pages <= 3) {
			return 0;
		}
		return Math.max(0, Math.min(this.page - 1, pages - 3));
	}

	private void bornerPage() {
		int maximum = pages(ThemeLivre.catalogue(this.seulementFavoris).size()) - 1;
		this.page = Math.max(0, Math.min(this.page, maximum));
	}

	private int logiqueX(double x) {
		return (int) Math.floor((x - this.gauche) / this.echelle);
	}

	private int logiqueY(double y) {
		return (int) Math.floor((y - this.haut) / this.echelle);
	}

	private static ResourceLocation texture(String nom) {
		return Compagnon.id("textures/gui/livre/bibliotheque/" + nom + ".png");
	}

	private static boolean dans(double sourisX, double sourisY, int x, int y,
			int largeur, int hauteur) {
		return sourisX >= x && sourisX < x + largeur
				&& sourisY >= y && sourisY < y + hauteur;
	}
}
