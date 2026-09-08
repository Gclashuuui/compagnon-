package fr.lhdp.compagnon.client;

import fr.lhdp.compagnon.Icones;
import fr.lhdp.compagnon.entite.CerveauComportement;
import fr.lhdp.compagnon.entite.CompagnonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/**
 * La petite pensée qui rend le cerveau lisible sans ouvrir un menu.
 *
 * <p>Elle apparaît dans le coin uniquement lorsqu'une intention intéressante
 * change, puis s'efface. Le mode diagnostic garde le même carton ouvert et y
 * ajoute la branche d'animation : les créateurs d'espèces peuvent ainsi voir
 * immédiatement si un problème vient du comportement, du rôle ou du fichier
 * Blockbench.
 */
public final class BandeauIntention {

	private static final int MARGE = 8;
	private static final int LARGEUR = 164;
	private static final int HAUTEUR = 30;
	private static final int HAUTEUR_DIAGNOSTIC = 57;
	private static final int DUREE = 20 * 4;
	private static final int APPARITION = 7;
	private static final int DISPARITION = 12;

	private static final int PAPIER_HAUT = 0xFFF3E4C4;
	private static final int PAPIER_BAS = 0xFFE4C997;
	private static final int CADRE = 0xFF6B4A24;
	private static final int ENCRE = 0xFF3A2A18;
	private static final int ENCRE_PALE = 0xFF78634A;
	private static final int OR = 0xFFD89A23;

	private static UUID suivi;
	private static CerveauComportement.Noeud derniere = CerveauComportement.Noeud.REPOS;
	private static long changement;
	private static boolean diagnostic;
	private static String derniereAction = "";

	private BandeauIntention() {
	}

	public static void basculerDiagnostic() {
		diagnostic = !diagnostic;
		changement = temps();
	}

	public static Component motDeLEtat() {
		return Component.translatable(diagnostic
				? "diagnostic.compagnon.ouvert" : "diagnostic.compagnon.ferme");
	}

	public static void oublier() {
		suivi = null;
		derniere = CerveauComportement.Noeud.REPOS;
		changement = 0L;
		diagnostic = false;
		derniereAction = "";
	}

	/** Le compagnon du joueur le plus proche, partagé avec les écrans contextuels. */
	static CompagnonEntity compagnonActif() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null) {
			return null;
		}
		UUID joueur = client.player.getUUID();
		return client.level.getEntitiesOfClass(CompagnonEntity.class,
				client.player.getBoundingBox().inflate(48.0D),
				entite -> joueur.equals(entite.getOwnerUUID()))
				.stream().min(java.util.Comparator.comparingDouble(
						client.player::distanceToSqr)).orElse(null);
	}

	public static void dessiner(GuiGraphics g, float partiel) {
		Minecraft client = Minecraft.getInstance();
		if (client.options.hideGui || client.screen != null || client.player == null) {
			return;
		}
		CompagnonEntity compagnon = compagnonActif();
		if (compagnon == null) {
			return;
		}

		CerveauComportement.Noeud brute = compagnon.intention();
		CerveauComportement.Noeud visible = interessante(brute)
				? brute : CerveauComportement.Noeud.REPOS;
		String action = compagnon.action();
		boolean reaction = !action.isEmpty() && !"!sortie".equals(action);
		if (!compagnon.getUUID().equals(suivi) || visible != derniere
				|| !action.equals(derniereAction)) {
			suivi = compagnon.getUUID();
			derniere = visible;
			derniereAction = action;
			changement = temps();
		}

		long age = temps() - changement;
		if (!diagnostic && ((!reaction && visible == CerveauComportement.Noeud.REPOS)
				|| age >= DUREE)) {
			return;
		}
		float alpha = diagnostic ? 1.0F : Math.min(1.0F, age / (float) APPARITION);
		alpha = diagnostic ? alpha : Math.min(alpha, (DUREE - age) / (float) DISPARITION);
		alpha = Math.max(0.0F, Math.min(1.0F, alpha));

		int largeur = Math.min(LARGEUR, g.guiWidth() - MARGE * 2);
		int hauteur = diagnostic ? HAUTEUR_DIAGNOSTIC : HAUTEUR;
		int x = g.guiWidth() - MARGE - largeur;
		// Sous l'annonce de niveau : les deux messages peuvent ainsi vivre ensemble.
		int y = 56;
		carte(g, x, y, largeur, hauteur, alpha);

		String nom = compagnon.getName().getString();
		String nomCourt = client.font.plainSubstrByWidth(nom, largeur - 30);
		g.drawString(client.font, Icones.de(Icones.PAROLE), x + 7, y + 6,
				teinter(OR, alpha), false);
		g.drawString(client.font, nomCourt, x + 19, y + 5,
				teinter(ENCRE_PALE, alpha), false);

		Component pensee = pensee(compagnon, visible);
		String phrase = client.font.plainSubstrByWidth(pensee.getString(), largeur - 14);
		g.drawString(client.font, phrase, x + 7, y + 17,
				teinter(ENCRE, alpha), false);

		if (diagnostic) {
			String comportement = "IA  " + brute.name().toLowerCase(java.util.Locale.ROOT);
			String animation = "VISUEL  " + compagnon.noeudAnimationJoue().name().toLowerCase(
					java.util.Locale.ROOT) + " / " + compagnon.roleLocomotionJoue();
			g.drawString(client.font, client.font.plainSubstrByWidth(comportement, largeur - 14),
					x + 7, y + 33, teinter(ENCRE_PALE, alpha), false);
			g.drawString(client.font, client.font.plainSubstrByWidth(animation, largeur - 14),
					x + 7, y + 44, teinter(ENCRE_PALE, alpha), false);
		}
	}

	private static Component pensee(CompagnonEntity compagnon,
			CerveauComportement.Noeud noeud) {
		if ("@ecoute".equals(compagnon.action())) {
			return Component.translatable("intention.compagnon.ecoute");
		}
		if (!compagnon.action().isEmpty() && !"!sortie".equals(compagnon.action())) {
			return Component.translatable("intention.compagnon.reagit");
		}
		return Component.translatable("intention.compagnon." +
				noeud.name().toLowerCase(java.util.Locale.ROOT));
	}

	private static boolean interessante(CerveauComportement.Noeud noeud) {
		return switch (noeud) {
			case REGARD_JOUEUR, REGARD_OBJET, REGARD_LIBRE, FLANERIE, REPOS -> false;
			default -> true;
		};
	}

	private static long temps() {
		Minecraft client = Minecraft.getInstance();
		return client.level == null ? 0L : client.level.getGameTime();
	}

	private static void carte(GuiGraphics g, int x, int y, int largeur, int hauteur,
			float alpha) {
		g.fill(x + 2, y + 2, x + largeur + 2, y + hauteur + 2, teinter(0x66000000, alpha));
		g.fillGradient(x + 1, y, x + largeur - 1, y + hauteur,
				teinter(PAPIER_HAUT, alpha), teinter(PAPIER_BAS, alpha));
		g.fillGradient(x, y + 1, x + largeur, y + hauteur - 1,
				teinter(PAPIER_HAUT, alpha), teinter(PAPIER_BAS, alpha));
		int cadre = teinter(CADRE, alpha);
		g.fill(x + 1, y, x + largeur - 1, y + 1, cadre);
		g.fill(x + 1, y + hauteur - 1, x + largeur - 1, y + hauteur, cadre);
		g.fill(x, y + 1, x + 1, y + hauteur - 1, cadre);
		g.fill(x + largeur - 1, y + 1, x + largeur, y + hauteur - 1, cadre);
	}

	private static int teinter(int couleur, float alpha) {
		int a = Math.round(((couleur >>> 24) & 0xFF) * alpha);
		return (a << 24) | (couleur & 0x00FFFFFF);
	}
}
