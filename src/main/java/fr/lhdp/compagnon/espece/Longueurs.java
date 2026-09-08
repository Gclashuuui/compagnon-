package fr.lhdp.compagnon.espece;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.lhdp.compagnon.Compagnon;
import net.minecraft.resources.ResourceLocation;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Combien de temps dure chaque animation.
 *
 * <h2>Le probleme</h2>
 *
 * <p>Le mod jouait <b>toutes</b> les animations pendant cinq secondes, la meme
 * duree pour toutes. C'etait une valeur inventee, et elle avait deux facons
 * d'etre fausse :
 *
 * <ul>
 *   <li>Une animation d'une seconde et demie se terminait, puis la bete restait
 *       <b>figee dans sa derniere pose</b> pendant trois secondes et demie,
 *       immobile, avant de revenir d'un coup. C'est ce qu'on voyait sur
 *       l'animation « allonge » : il se couchait, et il restait couche.</li>
 *   <li>Une animation de sept secondes etait <b>coupee en plein milieu</b>.</li>
 * </ul>
 *
 * <p>Dans les deux cas le retour etait sec, et un geste qu'on vient de debloquer
 * a l'air casse. Ce qui est le contraire de ce qu'on cherche : une animation
 * qu'on debloque doit donner envie d'en debloquer une autre.
 *
 * <h2>La solution</h2>
 *
 * <p>La duree est ecrite dans les fichiers d'animation, en secondes. On les lit
 * une fois au demarrage et on la retient. Chaque geste dure exactement le temps
 * qu'il dure.
 *
 * <h2>Pourquoi on lit depuis le jar et non depuis le gestionnaire de ressources</h2>
 *
 * <p>Les animations vivent dans {@code assets/}, qui est le domaine du client :
 * un serveur dedie n'a pas de gestionnaire de ressources pour ce dossier-la. Le
 * fichier, lui, est bien dans le jar des deux cotes — on le lit donc directement,
 * ce qui marche partout et ne demande rien a personne.
 *
 * <p>Consequence assumee : un pack de ressources qui remplacerait une animation
 * par une plus longue ne serait pas suivi. C'est un cas qui n'existe pas ici, et
 * le prix a payer serait de dupliquer les durees dans {@code data/} a la main.
 */
public final class Longueurs {

	private Longueurs() {
	}

	/**
	 * La duree utilisee quand on ne sait pas.
	 *
	 * <p>Deux secondes et demie, et non cinq : quand on se trompe, mieux vaut se
	 * tromper court. Une animation coupee trop tot se voit moins qu'une bete qui
	 * reste figee.
	 */
	public static final int PAR_DEFAUT = 50;

	/** Un geste ne bloque jamais la bete plus longtemps que ca. */
	private static final int PLAFOND = 20 * 12;

	/** Ni moins longtemps que ca : sous ce seuil, on ne verrait rien. */
	private static final int PLANCHER = 10;

	private static volatile Map<String, Integer> ticks = Map.of();

	/**
	 * La duree d'une animation en ticks, ou {@link #PAR_DEFAUT} si on l'ignore.
	 *
	 * <p>Prend le nom complet, celui qui est ecrit dans la table des niveaux :
	 * {@code animation.oiseau_bleu.joie}.
	 */
	public static int de(String animation) {
		return ticks.getOrDefault(animation, PAR_DEFAUT);
	}

	public static int combien() {
		return ticks.size();
	}

	/**
	 * Relit tous les fichiers d'animation des especes chargees.
	 *
	 * <p>A appeler <b>apres</b> {@code Especes.charger} : c'est elle qui dit ou
	 * sont les fichiers.
	 */
	public static void charger(Iterable<Espece> especes) {
		Map<String, Integer> lues = new HashMap<>();
		for (Espece espece : especes) {
			ResourceLocation chemin = espece.animations();
			if (chemin == null) {
				continue;
			}
			lire(chemin, lues);
		}
		ticks = Map.copyOf(lues);
		Compagnon.LOG.info("Durees d'animation lues : {}.", lues.size());
	}

	private static void lire(ResourceLocation chemin, Map<String, Integer> dans) {
		String dansLeJar = "/assets/" + chemin.getNamespace() + "/" + chemin.getPath();
		try (InputStream flux = Longueurs.class.getResourceAsStream(dansLeJar)) {
			if (flux == null) {
				Compagnon.LOG.warn("Animations introuvables pour les durees : {}", dansLeJar);
				return;
			}
			try (BufferedReader lecteur = new BufferedReader(
					new InputStreamReader(flux, StandardCharsets.UTF_8))) {

				JsonObject racine = JsonParser.parseReader(lecteur).getAsJsonObject();
				if (!racine.has("animations")) {
					return;
				}
				for (Map.Entry<String, JsonElement> entree
						: racine.getAsJsonObject("animations").entrySet()) {

					if (!entree.getValue().isJsonObject()) {
						continue;
					}
					JsonObject animation = entree.getValue().getAsJsonObject();
					if (!animation.has("animation_length")) {
						// Une animation sans duree tourne en boucle sans fin : on ne
						// lui en invente pas une, elle prendra celle par defaut.
						continue;
					}
					double secondes = animation.get("animation_length").getAsDouble();
					int enTicks = (int) Math.round(secondes * 20.0D);
					dans.put(entree.getKey(),
							Math.max(PLANCHER, Math.min(PLAFOND, enTicks)));
				}
			}
		} catch (Exception echec) {
			// Une lecture ratee n'empeche rien : tout retombe sur la duree par
			// defaut, et le mod se comporte comme avant ce fichier.
			Compagnon.LOG.warn("Durees d'animation illisibles ({}) : {}",
					dansLeJar, echec.toString());
		}
	}
}
