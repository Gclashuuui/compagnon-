package fr.lhdp.compagnon.voix;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.lhdp.compagnon.Compagnon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Les mots que les compagnons comprennent.
 *
 * <p>Lus dans {@code data/compagnon/voix.json} : plusieurs facons de dire le
 * meme ordre, parce qu'on ne parle pas tous pareil, et ajouter une tournure ne
 * doit pas demander de recompiler.
 *
 * <h2>La mise en forme, et pourquoi elle compte tant</h2>
 *
 * <p>Un moteur de reconnaissance en mode grammaire <b>retire en silence</b> les
 * mots qu'il ne connait pas. Si la liste se vide, il ne reconnait plus rien du
 * tout, pour personne, et aucune erreur n'apparait nulle part.
 *
 * <p>La mise en forme ci-dessous evite les rejets les plus betes : majuscules,
 * ponctuation, chiffres, apostrophes typographiques, espaces doubles.
 * <b>Les accents, eux, restent</b> — le vocabulaire francais connait
 * « sorciere » accentue et pas l'inverse. Ce n'est pas une incoherence.
 */
public final class Vocabulaire {

	/** Le fichier lu, sous {@code data/compagnon/}. */
	public static final String FICHIER = "voix.json";

	private static volatile Map<CommandeVocale, List<String>> motsParCommande = Map.of();

	private Vocabulaire() {
	}

	public static void charger(ResourceManager gestionnaire) {
		ResourceLocation chemin = Compagnon.id(FICHIER);
		Optional<Resource> fichier = gestionnaire.getResource(chemin);

		if (fichier.isEmpty()) {
			motsParCommande = Map.of();
			Compagnon.LOG.info("Aucun fichier de voix : les ordres a la voix sont inactifs.");
			return;
		}

		try (BufferedReader lecteur = fichier.get().openAsReader()) {
			poser(JsonParser.parseReader(lecteur).getAsJsonObject());
		} catch (Exception echec) {
			Compagnon.LOG.error("Fichier de voix illisible : {}", echec.getMessage());
			motsParCommande = Map.of();
			return;
		}
		Compagnon.LOG.info("Voix : {} ordre(s) compris.", motsParCommande.size());
	}

	/**
	 * Installe les tournures depuis le JSON deja lu.
	 *
	 * <p>Separee de {@link #charger} pour une seule raison, mais une bonne : ceci
	 * ne touche ni au disque, ni aux paquets de ressources, ni a Minecraft. Les
	 * soixante-dix tournures du fichier peuvent donc etre <b>verifiees</b> —
	 * qu'aucune n'en masque une autre, que chacune declenche bien son ordre — sans
	 * lancer le jeu.
	 *
	 * <p>Sans cela, une tournure ajoutee qui en cache une autre ne se decouvre
	 * qu'en jeu, au micro, et par hasard.
	 */
	static void poser(JsonObject racine) {
		Map<CommandeVocale, List<String>> lus = new LinkedHashMap<>();
		JsonObject commandes = racine.getAsJsonObject("commandes");

		for (CommandeVocale commande : CommandeVocale.values()) {
			if (!commandes.has(commande.cle())) {
				continue;
			}
			List<String> mots = new ArrayList<>();
			for (JsonElement element : commandes.getAsJsonArray(commande.cle())) {
				String propre = pourLaGrammaire(element.getAsString());
				if (!propre.isEmpty()) {
					mots.add(propre);
				}
			}
			if (!mots.isEmpty()) {
				lus.put(commande, List.copyOf(mots));
			}
		}
		motsParCommande = Collections.unmodifiableMap(lus);
	}

	/** Les facons de dire un ordre. */
	public static List<String> motsDe(CommandeVocale commande) {
		return motsParCommande.getOrDefault(commande, List.of());
	}

	/**
	 * L'ordre correspondant a une phrase entendue, ou {@code null}.
	 *
	 * <p>On cherche la tournure la plus longue d'abord : « couche toi » doit
	 * l'emporter sur « couche », sinon la seconde masquerait la premiere.
	 */
	public static CommandeVocale reconnaitre(String phrase) {
		String propre = pourLaComparaison(phrase);
		CommandeVocale trouvee = null;
		int meilleureLongueur = 0;

		for (Map.Entry<CommandeVocale, List<String>> entree : motsParCommande.entrySet()) {
			for (String mot : entree.getValue()) {
				String cible = pourLaComparaison(mot);
				if (cible.length() > meilleureLongueur && contientLesMots(propre, cible)) {
					trouvee = entree.getKey();
					meilleureLongueur = cible.length();
				}
			}
		}
		return trouvee;
	}

	/** Tous les mots a donner au moteur, ordres seulement. */
	public static List<String> tousLesMots() {
		List<String> tous = new ArrayList<>();
		for (List<String> mots : motsParCommande.values()) {
			for (String expression : mots) {
				// Une expression de plusieurs mots doit etre donnee mot par mot :
				// le moteur ne connait que des mots isoles.
				Collections.addAll(tous, expression.split(" "));
			}
		}
		return tous.stream().distinct().toList();
	}

	// --- Mise en forme -----------------------------------------------------------

	/**
	 * Prepare un mot pour le moteur : minuscules, apostrophes droites, sans
	 * ponctuation ni chiffres, espaces reduits. <b>Les accents restent.</b>
	 */
	public static String pourLaGrammaire(String brut) {
		if (brut == null) {
			return "";
		}
		String propre = brut.toLowerCase(java.util.Locale.FRENCH)
				.replace('’', '\'')
				.replaceAll("[^\\p{L}' ]", " ")
				.replaceAll("\\s+", " ")
				.trim();
		return propre;
	}

	/**
	 * Prepare un texte pour la comparaison : comme ci-dessus, <b>mais sans les
	 * accents</b>. Celui qui parle peut prononcer « couche » ou « couché » — a la
	 * comparaison, c'est pareil.
	 *
	 * <p>Ne jamais fusionner cette methode avec la precedente : elles font
	 * exactement l'inverse sur les accents, et pour de bonnes raisons.
	 */
	public static String pourLaComparaison(String brut) {
		String propre = pourLaGrammaire(brut);
		return Normalizer.normalize(propre, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");
	}

	/** Vrai si la phrase contient tous les mots de la cible, dans l'ordre. */
	private static boolean contientLesMots(String phrase, String cible) {
		String[] mots = cible.split(" ");
		int depuis = 0;
		for (String mot : mots) {
			int trouve = indexDuMot(phrase, mot, depuis);
			if (trouve < 0) {
				return false;
			}
			depuis = trouve + mot.length();
		}
		return true;
	}

	/** Cherche un mot entier, pas un morceau : « ici » ne doit pas matcher « voici ». */
	private static int indexDuMot(String phrase, String mot, int depuis) {
		String espace = " " + phrase + " ";
		int trouve = espace.indexOf(" " + mot + " ", depuis);
		return trouve < 0 ? -1 : trouve;
	}
}
