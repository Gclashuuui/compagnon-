package fr.lhdp.compagnon.client;

import fr.lhdp.compagnon.Icones;
import fr.lhdp.compagnon.reseau.PaquetMontee;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

/** Une montee de niveau discrete, posee dans le coin superieur droit. */
public final class AnnonceNiveau {

	private static final int MARGE = 8;
	private static final int LARGEUR = 176;
	private static final int HAUTEUR = 42;
	private static final int DUREE = 20 * 5;
	private static final int APPARITION = 8;
	private static final int DISPARITION = 16;

	private static final int PAPIER_HAUT = 0xFFF3E4C4;
	private static final int PAPIER_BAS = 0xFFE4C997;
	private static final int CADRE = 0xFF6B4A24;
	private static final int OR = 0xFFD89A23;
	private static final int ENCRE = 0xFF3A2A18;
	private static final int ENCRE_PALE = 0xFF78634A;

	private static volatile PaquetMontee annonce;
	private static int restant;

	private AnnonceNiveau() {
	}

	public static void poser(PaquetMontee nouvelle) {
		annonce = nouvelle;
		restant = DUREE;
	}

	public static void tick() {
		if (restant > 0 && --restant == 0) {
			annonce = null;
		}
	}

	public static void oublier() {
		annonce = null;
		restant = 0;
	}

	public static void dessiner(GuiGraphics g, float partiel) {
		Minecraft client = Minecraft.getInstance();
		PaquetMontee a = annonce;
		if (a == null || client.player == null || client.options.hideGui || client.screen != null) {
			return;
		}

		float vecu = DUREE - restant + partiel;
		float alpha = Math.min(1.0F, vecu / APPARITION);
		alpha = Math.min(alpha, Math.min(1.0F, restant / (float) DISPARITION));
		int largeur = Math.min(LARGEUR, g.guiWidth() - MARGE * 2);
		int x = g.guiWidth() - MARGE - largeur;
		int y = MARGE;

		g.fill(x + 2, y + 2, x + largeur + 2, y + HAUTEUR + 2, teinter(0x66000000, alpha));
		g.fillGradient(x + 1, y, x + largeur - 1, y + HAUTEUR,
				teinter(PAPIER_HAUT, alpha), teinter(PAPIER_BAS, alpha));
		g.fillGradient(x, y + 1, x + largeur, y + HAUTEUR - 1,
				teinter(PAPIER_HAUT, alpha), teinter(PAPIER_BAS, alpha));
		g.fill(x + 1, y, x + largeur - 1, y + 1, teinter(CADRE, alpha));
		g.fill(x + 1, y + HAUTEUR - 1, x + largeur - 1, y + HAUTEUR, teinter(CADRE, alpha));
		g.fill(x, y + 1, x + 1, y + HAUTEUR - 1, teinter(CADRE, alpha));
		g.fill(x + largeur - 1, y + 1, x + largeur, y + HAUTEUR - 1, teinter(CADRE, alpha));

		g.drawString(client.font, Icones.de(Icones.ETOILE), x + 7, y + 7,
				teinter(OR, alpha), false);
		String titre = Component.translatable(
				"montee.compagnon.titre", a.nom(), a.niveau()).getString();
		String titreCourt = client.font.plainSubstrByWidth(titre, largeur - 30);
		g.drawString(client.font, titreCourt, x + 19, y + 7,
				teinter(ENCRE, alpha), false);

		Component detail = detail(a);
		String detailCourt = client.font.plainSubstrByWidth(detail.getString(), largeur - 14);
		g.drawString(client.font, detailCourt, x + 7, y + 21,
				teinter(ENCRE_PALE, alpha), false);

		int progression = Math.round((largeur - 2) * restant / (float) DUREE);
		g.fill(x + 1, y + HAUTEUR - 3, x + 1 + progression, y + HAUTEUR - 1,
				teinter(OR, alpha));
	}

	private static Component detail(PaquetMontee a) {
		if (!a.gestes().isEmpty()) {
			if (a.gestes().size() > 1) {
				return Component.translatable("montee.compagnon.nouveaux_gestes", a.gestes().size());
			}
			return Component.translatable("montee.compagnon.nouveau_geste", nomLisible(a.gestes().get(0)));
		}
		if (a.points() == 1) {
			return Component.translatable("montee.compagnon.point_competence");
		}
		if (a.points() > 1) {
			return Component.translatable("montee.compagnon.points_competence", a.points());
		}
		if (a.monte()) {
			return Component.translatable("montee.compagnon.monte");
		}
		return Component.translatable("montee.compagnon.nouvelle_etape");
	}

	private static Component nomLisible(String animation) {
		String cle = "roue.compagnon.action." + animation;
		if (Language.getInstance().has(cle)) {
			return Component.translatable(cle);
		}
		int point = animation.lastIndexOf('.');
		String court = point >= 0 ? animation.substring(point + 1) : animation;
		return Component.literal(court.replace('_', ' '));
	}

	private static int teinter(int couleur, float alpha) {
		int a = Math.round(((couleur >>> 24) & 0xFF) * Math.max(0.0F, Math.min(1.0F, alpha)));
		return (a << 24) | (couleur & 0x00FFFFFF);
	}
}
