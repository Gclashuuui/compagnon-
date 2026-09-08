package fr.lhdp.compagnon.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.entite.CompagnonEntity;
import fr.lhdp.compagnon.espece.Espece;
import fr.lhdp.compagnon.espece.Especes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedReader;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Montre une bete en vrai, dans une interface.
 *
 * <h2>Pourquoi ca vaut mieux qu'une liste de noms</h2>
 *
 * <p>« atroxiia_adulte / cramoisi_femelle » ne dit rien a personne. L'image, si.
 * Quand on choisit ce qu'on offre a quelqu'un, on veut <b>voir</b> ce qu'on
 * offre — c'est toute la difference entre une commande et un cadeau.
 *
 * <h2>La bete montree n'existe pas</h2>
 *
 * <p>C'est une entite fabriquee ici, jamais ajoutee au monde : elle ne bouge
 * pas, ne mange pas, ne se sauvegarde pas, et le serveur ignore jusqu'a son
 * existence. Une par espece, gardee de cote — GeckoLib range ses animations par
 * numero d'entite, et deux especes qui partageraient le meme numero
 * melangeraient leurs squelettes.
 *
 * <h2>L'echelle se calcule, elle ne se devine pas</h2>
 *
 * <p>Un ignivorus adulte fait vingt-quatre blocs du museau a la queue, une
 * mouette en fait un demi. A echelle fixe, l'un deborde de l'ecran et l'autre
 * est un point. On lit donc le <b>vrai</b> encombrement dans le fichier de
 * geometrie, une fois par espece, et on cadre dessus. Une espece deposee demain
 * sera cadree sans qu'on y touche.
 */
public final class Apercu {

	private Apercu() {
	}

	/** Une bete de demonstration par espece. */
	private static final Map<String, CompagnonEntity> betes = new ConcurrentHashMap<>();

	/** L'encombrement lu dans chaque geometrie : {@code {hauteur, largeur}} en blocs. */
	private static final Map<String, float[]> tailles = new ConcurrentHashMap<>();

	/** Les noms d'os de chaque modele, dans l'ordre du fichier. */
	private static final Map<String, java.util.List<String>> squelettes =
			new ConcurrentHashMap<>();

	/** Seize unites de modele font un bloc. C'est la convention Bedrock. */
	private static final float UNITES_PAR_BLOC = 16.0F;

	/** Ce qu'on garde de vide autour de la bete, en part de la boite. */
	private static final float MARGE = 0.82F;

	/** En dessous, la bete serait un point ; au-dessus, un mur de pixels. */
	private static final int ECHELLE_MINIMUM = 3;
	private static final int ECHELLE_MAXIMUM = 220;

	/**
	 * Dessine l'espece demandee dans la boite donnee, tournee vers la souris.
	 *
	 * @return vrai si quelque chose a ete dessine
	 */
	public static boolean dessiner(GuiGraphics g, int x, int y, int large, int haut,
			String espece, String variante, float sourisX, float sourisY) {

		CompagnonEntity bete = beteDe(espece, variante);
		if (bete == null) {
			return false;
		}
		float[] taille = tailleDe(espece);
		int echelle = echellePour(taille, haut, large);

		// La bete est posee sur le bas de la boite, pas centree dessus : un animal
		// flotte moins bien qu'un joueur.
		InventoryScreen.renderEntityInInventoryFollowsMouse(g,
				x, y, x + large, y + haut,
				echelle, MARGE, sourisX, sourisY, bete);
		return true;
	}

	/**
	 * L'echelle qui fait tenir la bete dans la boite.
	 *
	 * <p>On regarde la hauteur ET la largeur : un varasuchus est bas et tres long,
	 * cadrer sur sa seule hauteur le ferait sortir par les cotes.
	 */
	private static int echellePour(float[] taille, int haut, int large) {
		float parLaHauteur = haut * MARGE / Math.max(taille[0], 0.1F);
		float parLaLargeur = large * MARGE / Math.max(taille[1], 0.1F);
		int echelle = (int) Math.min(parLaHauteur, parLaLargeur);
		return Math.max(ECHELLE_MINIMUM, Math.min(ECHELLE_MAXIMUM, echelle));
	}

	/**
	 * La bete de demonstration, pour qui veut la dessiner lui-meme.
	 *
	 * <p>L'editeur de position en a besoin : il la tourne, la cadre et lui met
	 * un objet dans la gueule, ce que l'apercu simple ne sait pas faire.
	 */
	public static CompagnonEntity bete(String espece, String variante) {
		return beteDe(espece, variante);
	}

	/** Combien de blocs elle occupe : {@code {hauteur, largeur}}. */
	public static float[] encombrement(String espece) {
		return tailleDe(espece);
	}

	/** La bete de demonstration de cette espece, habillee de cette variante. */
	private static CompagnonEntity beteDe(String espece, String variante) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.level == null || espece == null || espece.isEmpty()) {
			return null;
		}
		CompagnonEntity bete = betes.computeIfAbsent(espece, cle -> {
			CompagnonEntity neuve = new CompagnonEntity(Compagnon.COMPAGNON, client.level);
			neuve.setEspece(cle);
			return neuve;
		});
		// La variante change sans rien recreer : c'est la meme bete qui se change.
		if (variante != null && !variante.equals(bete.variante())) {
			bete.setVariante(variante);
		}
		return bete;
	}

	/**
	 * Combien de blocs la bete occupe vraiment, d'apres son modele.
	 *
	 * <p>Et non d'apres sa boite de collision : celle-la est choisie pour qu'elle
	 * puisse circuler, elle n'a rien a voir avec ce qu'on voit. Un ignivorus tient
	 * dans cinq blocs et en couvre vingt-quatre.
	 */
	private static float[] tailleDe(String espece) {
		return tailles.computeIfAbsent(espece, cle -> {
			Espece fiche = Especes.get(cle);
			float[] repli = {1.0F, 1.0F};
			if (fiche == null) {
				return repli;
			}
			try {
				return mesurer(fiche.geometrie());
			} catch (Exception echec) {
				// Un modele illisible ne doit pas emporter l'ecran : GeckoLib le
				// signalera de son cote, et un cadrage approximatif vaut mieux
				// qu'une interface qui ne s'ouvre pas.
				Compagnon.LOG.warn("Apercu : geometrie de \"{}\" illisible ({})",
						cle, echec.getMessage());
				return repli;
			}
		});
	}

	/** Parcourt les cubes du modele et rend {@code {hauteur, largeur}} en blocs. */
	private static float[] mesurer(ResourceLocation geometrie) throws Exception {
		Optional<Resource> trouve = Minecraft.getInstance()
				.getResourceManager().getResource(geometrie);
		if (trouve.isEmpty()) {
			return new float[]{1.0F, 1.0F};
		}
		JsonObject racine;
		try (BufferedReader lecteur = trouve.get().openAsReader()) {
			racine = JsonParser.parseReader(lecteur).getAsJsonObject();
		}

		float basY = Float.MAX_VALUE, hautY = -Float.MAX_VALUE;
		float etendue = 0.0F;
		boolean vu = false;

		JsonArray geometries = racine.getAsJsonArray("minecraft:geometry");
		if (geometries == null) {
			return new float[]{1.0F, 1.0F};
		}
		for (JsonElement geo : geometries) {
			JsonArray os = geo.getAsJsonObject().getAsJsonArray("bones");
			if (os == null) {
				continue;
			}
			for (JsonElement unOs : os) {
				JsonArray cubes = unOs.getAsJsonObject().getAsJsonArray("cubes");
				if (cubes == null) {
					continue;
				}
				for (JsonElement cube : cubes) {
					JsonObject c = cube.getAsJsonObject();
					JsonArray origine = c.getAsJsonArray("origin");
					JsonArray taille = c.getAsJsonArray("size");
					if (origine == null || taille == null) {
						continue;
					}
					float x = origine.get(0).getAsFloat(), y = origine.get(1).getAsFloat();
					float z = origine.get(2).getAsFloat();
					float lx = taille.get(0).getAsFloat(), ly = taille.get(1).getAsFloat();
					float lz = taille.get(2).getAsFloat();

					basY = Math.min(basY, y);
					hautY = Math.max(hautY, y + ly);
					// La bete tourne devant nous : c'est son plus grand cote
					// horizontal qui decide, pas seulement sa largeur.
					etendue = Math.max(etendue, Math.max(
							Math.max(Math.abs(x), Math.abs(x + lx)) * 2.0F,
							Math.max(Math.abs(z), Math.abs(z + lz)) * 2.0F));
					vu = true;
				}
			}
		}
		if (!vu) {
			return new float[]{1.0F, 1.0F};
		}
		return new float[]{
				Math.max((hautY - basY) / UNITES_PAR_BLOC, 0.1F),
				Math.max(etendue / UNITES_PAR_BLOC, 0.1F)};
	}

	/**
	 * Tous les os de ce modele-la, dans l'ordre ou le fichier les declare.
	 *
	 * <p>L'editeur de position s'en sert pour proposer l'accrochage : on ne tape
	 * pas un nom d'os de memoire, on fait defiler ceux qui existent. Un nom mal
	 * orthographie ne donne rien a l'ecran et rien dans le journal — c'est le
	 * genre de faute qu'on cherche une heure.
	 *
	 * <p>L'ordre du fichier vaut mieux que l'ordre alphabetique : Blockbench
	 * ecrit les os dans l'ordre de la hierarchie, donc le cou suit la tete.
	 */
	public static java.util.List<String> osDe(String espece) {
		return squelettes.computeIfAbsent(espece, cle -> {
			Espece fiche = Especes.get(cle);
			if (fiche == null) {
				return java.util.List.of();
			}
			try {
				return nommerLesOs(fiche.geometrie());
			} catch (Exception echec) {
				Compagnon.LOG.warn("Os de \"{}\" illisibles ({})", cle, echec.getMessage());
				return java.util.List.of();
			}
		});
	}

	private static java.util.List<String> nommerLesOs(ResourceLocation geometrie)
			throws Exception {

		Optional<Resource> trouve = Minecraft.getInstance()
			.getResourceManager().getResource(geometrie);
		if (trouve.isEmpty()) {
			return java.util.List.of();
		}
		JsonObject racine;
		try (BufferedReader lecteur = trouve.get().openAsReader()) {
			racine = JsonParser.parseReader(lecteur).getAsJsonObject();
		}
		java.util.List<String> noms = new java.util.ArrayList<>();
		JsonArray geometries = racine.getAsJsonArray("minecraft:geometry");
		if (geometries == null) {
			return java.util.List.of();
		}
		for (JsonElement geo : geometries) {
			JsonArray os = geo.getAsJsonObject().getAsJsonArray("bones");
			if (os == null) {
				continue;
			}
			for (JsonElement unOs : os) {
				JsonObject o = unOs.getAsJsonObject();
				if (o.has("name")) {
					noms.add(o.get("name").getAsString());
				}
			}
		}
		return java.util.List.copyOf(noms);
	}

	/**
	 * On quitte le monde : les betes de demonstration le tenaient par la main.
	 *
	 * <p>Une entite garde une reference vers son niveau. Sans cet oubli, chaque
	 * monde visite resterait en memoire tant que le jeu tourne.
	 */
	public static void oublier() {
		betes.clear();
		tailles.clear();
		squelettes.clear();
	}
}
