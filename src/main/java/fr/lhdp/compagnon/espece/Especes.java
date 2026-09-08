package fr.lhdp.compagnon.espece;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.DossierDuServeur;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Le catalogue des especes.
 *
 * <p>Il est lu <b>cote serveur</b>, dans {@code data/compagnon/especes/}, puis
 * envoye aux clients qui se connectent. Deux raisons a ce sens-la :
 *
 * <ul>
 *   <li>le serveur doit connaitre la liste pour proposer les especes et les
 *       variantes dans les commandes, et pour verifier ce qu'un client demande ;</li>
 *   <li>le livre aura besoin des memes donnees cote client, et il ne peut pas
 *       les inventer.</li>
 * </ul>
 *
 * <p>La carte est la meme classe des deux cotes : le serveur la remplit en lisant
 * les fichiers, le client la remplit en recevant le paquet.
 */
public final class Especes {

	/** Le dossier lu sous {@code data/compagnon/}. */
	public static final String DOSSIER = "especes";

	private static final String SUFFIXE = ".json";

	/** Le fichier de chiffres, dans un dossier de creature. */
	public static final String FICHIER_D_ESPECE = "espece.json";

	/** Ou vivent les images d'une creature, dans son dossier. */
	public static final String DOSSIER_TEXTURES = "textures";

	private static volatile Map<String, Espece> especes = Map.of();

	private Especes() {
	}

	/**
	 * Ce que l'equipe a depose a la main, par-dessus ce que le mod livre.
	 *
	 * <p>Voir {@link fr.lhdp.compagnon.DossierDuServeur} : un fichier du meme nom
	 * remplace celui d'origine, ce qui permet de corriger une taille ou une boite
	 * de collision sans attendre une nouvelle version du jar.
	 */
	private static void lireLeDossierDuServeur(Map<String, Espece> lues) {
		// Un fichier pose a plat : <serveur>/compagnon/especes/mouette.json
		DossierDuServeur.lire(DOSSIER, (nom, objet) -> ajouter(lues, nom, objet, List.of()));

		// Ou un dossier complet, ce qui est la bonne facon :
		//
		//   <serveur>/compagnon/especes/mouette/
		//       espece.json
		//       mouette.geo.json
		//       mouette.animation.json
		//       textures/blanche.png, grise.png, ...
		//
		// Le fichier n'a alors que les chiffres a porter : le reste se deduit de ce
		// qu'il y a a cote. Ajouter une couleur, c'est deposer une image.
		for (Path dossier : DossierDuServeur.sousDossiers(DOSSIER)) {
			String nom = dossier.getFileName().toString();
			JsonObject objet = DossierDuServeur.lireUn(dossier.resolve(FICHIER_D_ESPECE));
			if (objet == null) {
				Compagnon.LOG.warn("Le dossier d'espece \"{}\" n'a pas de {} : ignore.",
						nom, FICHIER_D_ESPECE);
				continue;
			}
			ajouter(lues, nom, objet, DossierDuServeur.images(dossier.resolve(DOSSIER_TEXTURES)));
		}
	}

	private static void ajouter(Map<String, Espece> lues, String nom, JsonObject objet,
			List<String> textures) {
		try {
			boolean remplace = lues.containsKey(nom);
			lues.put(nom, lire(nom, objet, textures));
			Compagnon.LOG.info("Espece {} depuis le dossier du serveur : {}{}",
					remplace ? "REMPLACEE" : "ajoutee", nom,
					textures.isEmpty() ? "" : " (" + textures.size() + " texture(s) : "
							+ String.join(", ", textures) + ")");
		} catch (Exception echec) {
			// Une espece fautive ne doit pas emporter les autres.
			Compagnon.LOG.error("Espece \"{}\" du dossier du serveur : {}", nom, echec.getMessage());
		}
	}

	public static void charger(ResourceManager gestionnaire) {
		Map<String, Espece> lues = new LinkedHashMap<>();

		Map<ResourceLocation, Resource> fichiers =
				gestionnaire.listResources(DOSSIER, chemin -> chemin.getPath().endsWith(SUFFIXE));

		for (Map.Entry<ResourceLocation, Resource> fichier : fichiers.entrySet()) {
			ResourceLocation chemin = fichier.getKey();
			if (!chemin.getNamespace().equals(Compagnon.MOD_ID)) {
				continue;
			}
			String nom = nomDepuisChemin(chemin);
			try (BufferedReader lecteur = fichier.getValue().openAsReader()) {
				lues.put(nom, lire(nom, JsonParser.parseReader(lecteur).getAsJsonObject()));
			} catch (Exception echec) {
				Compagnon.LOG.error("Espece \"{}\" illisible ({}) : {}", nom, chemin, echec.getMessage());
			}
		}

		// Ce que l'equipe a depose a la main passe EN DERNIER, et gagne donc sur ce
		// qui est livre dans le mod. C'est ce qui permet de corriger la taille ou la
		// boite d'une espece sans attendre une nouvelle version du jar.
		lireLeDossierDuServeur(lues);

		// Collections.unmodifiableMap et non Map.copyOf : ce dernier ne conserve
		// pas l'ordre, et premierNom() renverrait alors une espece au hasard.
		especes = Collections.unmodifiableMap(lues);
		Compagnon.LOG.info("Especes chargees : {}",
				especes.isEmpty() ? "aucune" : String.join(", ", especes.keySet()));
		// Chaque geste doit durer le temps qu'il dure : voir Longueurs.
		Longueurs.charger(especes.values());
	}

	private static Espece lire(String nom, JsonObject objet) {
		return lire(nom, objet, List.of());
	}

	/**
	 * Lit une espece, en comblant ce que le fichier ne dit pas.
	 *
	 * <h2>Pourquoi des valeurs par defaut</h2>
	 *
	 * <p>Une espece deposee sous forme de <b>dossier</b> range deja ses fichiers a
	 * un endroit previsible. Reecrire ces chemins dans le JSON serait les redire
	 * deux fois, et se tromper une fois sur deux. Un fichier d'espece de dossier
	 * peut donc se contenter des chiffres : la geometrie, les animations et la
	 * liste des textures se deduisent du dossier lui-meme.
	 *
	 * <p>Ce qui est ecrit dans le fichier l'emporte toujours : on garde la main
	 * quand le rangement automatique ne convient pas.
	 *
	 * @param texturesTrouvees les images vues a cote, sans extension. Vide pour une
	 *                         espece livree dans le mod, qui declare tout.
	 */
	private static Espece lire(String nom, JsonObject objet, List<String> texturesTrouvees) {
		ResourceLocation geometrie = objet.has("geometrie")
				? ResourceLocation.parse(champ(objet, "geometrie"))
				: Compagnon.id("geo/" + nom + ".geo.json");
		ResourceLocation animations = objet.has("animations")
				? ResourceLocation.parse(champ(objet, "animations"))
				: Compagnon.id("animations/" + nom + ".animation.json");

		Map<String, ResourceLocation> variantes = new LinkedHashMap<>();
		if (objet.has("variantes")) {
			for (Map.Entry<String, com.google.gson.JsonElement> variante
					: objet.getAsJsonObject("variantes").entrySet()) {
				variantes.put(variante.getKey(),
						ResourceLocation.parse(variante.getValue().getAsString()));
			}
		} else {
			// Chaque image du dossier devient une variante portant son nom. C'est
			// ce qui permet d'ajouter une couleur en deposant un fichier, sans
			// toucher au JSON.
			for (String texture : texturesTrouvees) {
				variantes.put(texture, Compagnon.id("textures/entity/" + nom + "/" + texture + ".png"));
			}
		}
		if (variantes.isEmpty()) {
			throw new IllegalArgumentException("aucune variante : ni dans le fichier, ni d'image a cote");
		}

		// A defaut, la premiere de la liste. Une espece a une variante n'a alors
		// rien a declarer du tout.
		String varianteParDefaut = objet.has("variante_par_defaut")
				? champ(objet, "variante_par_defaut")
				: variantes.keySet().iterator().next();

		if (!variantes.containsKey(varianteParDefaut)) {
			throw new IllegalArgumentException(
					"la variante par defaut \"" + varianteParDefaut + "\" n'est pas dans la liste des variantes");
		}

		JsonObject locomotionJson = objet.getAsJsonObject("locomotion");
		Map<String, String> locomotion = new LinkedHashMap<>();
		for (String role : Espece.ROLES) {
			if (locomotionJson == null || !locomotionJson.has(role)) {
				throw new IllegalArgumentException("il manque le role de locomotion \"" + role + "\"");
			}
			locomotion.put(role, locomotionJson.get(role).getAsString());
		}
		// Les poses et les allures plus fines sont facultatives. L'entite choisit
		// un repli qui a du sens quand une espece ne sait ni nager ni planer.
		// Une case laissee vide compte comme absente — on peut donc preparer les
		// cases dans le fichier et les remplir plus tard.
		for (String role : Espece.ROLES_OPTIONNELS) {
			String trouvee = texteOuNull(locomotionJson, role);
			if (trouvee != null) {
				locomotion.put(role, trouvee);
			}
		}

		// Les reactions aussi. Tant que la liste est vide, la mecanique marche
		// mais rien ne s'anime — c'est a l'auteur des animations de la remplir.
		Map<String, String> reactions = new LinkedHashMap<>();
		if (objet.has("reactions")) {
			JsonObject reactionsJson = objet.getAsJsonObject("reactions");
			for (String role : reactionsJson.keySet()) {
				String trouvee = texteOuNull(reactionsJson, role);
				if (trouvee != null) {
					reactions.put(role, trouvee);
				}
			}
		}

		// Sa voix. Absente, la bete est muette : c'est le comportement d'avant.
		Map<String, String> sons = new LinkedHashMap<>();
		if (objet.has("sons")) {
			JsonObject sonsJson = objet.getAsJsonObject("sons");
			for (String role : sonsJson.keySet()) {
				String trouve = texteOuNull(sonsJson, role);
				if (trouve != null) {
					sons.put(role, trouve);
				}
			}
		}

		// unmodifiableMap et non copyOf pour les variantes : elles sont proposees
		// telles quelles dans les commandes, et copyOf perdrait l'ordre du fichier.
		// La taille de la boite de collision. Absente, on garde la valeur de base :
		// une espece livree sans cette section continue de marcher.
		float largeur = Espece.LARGEUR_PAR_DEFAUT;
		float hauteur = Espece.HAUTEUR_PAR_DEFAUT;
		if (objet.has("taille")) {
			JsonObject taille = objet.getAsJsonObject("taille");
			if (taille.has("largeur")) {
				largeur = taille.get("largeur").getAsFloat();
			}
			if (taille.has("hauteur")) {
				hauteur = taille.get("hauteur").getAsFloat();
			}
		}

		// Les morceaux qu'on ne peut pas traverser. Absents, le compagnon n'a que
		// sa boite principale — c'est le comportement d'avant, rien ne casse.
		List<Partie> parties = new ArrayList<>();
		if (objet.has("parties")) {
			for (com.google.gson.JsonElement element : objet.getAsJsonArray("parties")) {
				JsonObject p = element.getAsJsonObject();
				parties.add(new Partie(
						p.has("nom") ? p.get("nom").getAsString() : "",
						decalage(p, 0), decalage(p, 1), decalage(p, 2),
						p.get("largeur").getAsDouble(),
						p.get("hauteur").getAsDouble()));
			}
		}

		// UNE BETE QUI VOLE.
		//
		// Absent = elle ne vole pas. C'est le bon defaut : une espece ecrite
		// avant que ce champ existe ne doit pas se mettre a decoller.
		boolean vole = objet.has("vole") && objet.get("vole").getAsBoolean();

		// COMMENT ON L'APPELLE A VOIX HAUTE.
		//
		// Le nom du dossier n'est pas toujours un mot que le micro sait dire :
		// « dragonnet » n'existe pas dans le vocabulaire francais, « dragon » si.
		// Cette cle permet de donner le mot qu'on crie vraiment dans une cour.
		//
		// Absente, on prend le nom de l'espece — ce qui suffit pour « mouette ».
		String nomVocal = objet.has("nom_a_la_voix")
				? objet.get("nom_a_la_voix").getAsString()
				: nom;

		// LA SELLE ET LA MONTE.
		//
		// Absentes, la bete ne se monte pas. C'est le bon defaut : on n'ajoute
		// pas une facon de se deplacer a tout un serveur par omission.
		Partie selle = objet.has("selle")
				? new Partie("selle", decalageDe(objet.getAsJsonArray("selle"), 0),
						decalageDe(objet.getAsJsonArray("selle"), 1),
						decalageDe(objet.getAsJsonArray("selle"), 2), 0.0D, 0.0D)
				: null;
		int monterAuNiveau = objet.has("monter_au_niveau")
				? objet.get("monter_au_niveau").getAsInt()
				: 0;

		// Le nom qu'on montre. Absent, on le fabrique a partir de la cle.
		String titre = objet.has("titre") ? champ(objet, "titre") : joli(nom);

		// Ce qu'il deviendra. Absent, il ne grandit pas : c'est le defaut, et
		// c'est le bon — on n'inflige pas une metamorphose par omission.
		String devient = objet.has("devient") ? champ(objet, "devient") : "";
		int devientAuNiveau = objet.has("devient_au_niveau")
			? objet.get("devient_au_niveau").getAsInt()
			: 0;

		return new Espece(nom, titre, geometrie, animations, varianteParDefaut,
				Collections.unmodifiableMap(variantes),
				Map.copyOf(locomotion), Map.copyOf(reactions), Map.copyOf(sons),
		largeur, hauteur,
				List.copyOf(parties), vole, nomVocal, selle, monterAuNiveau,
			devient, devientAuNiveau);
	}

	/** Une composante d'un tableau [droite, haut, avant]. Absente, elle vaut zero. */
	private static double decalageDe(com.google.gson.JsonArray liste, int index) {
		return index < liste.size() ? liste.get(index).getAsDouble() : 0.0D;
	}

	/** Une composante du decalage : droite, haut, avant. Absente, elle vaut zero. */
	private static double decalage(JsonObject partie, int index) {
		if (!partie.has("decalage")) {
			return 0.0D;
		}
		com.google.gson.JsonArray liste = partie.getAsJsonArray("decalage");
		return index < liste.size() ? liste.get(index).getAsDouble() : 0.0D;
	}

	/**
	 * La valeur d'une cle, ou {@code null} si elle est absente <b>ou vide</b>. Une
	 * case laissee vide dans le fichier doit se comporter comme une case qui n'y
	 * est pas : sinon on demanderait a GeckoLib une animation nommee "".
	 */
	private static String texteOuNull(JsonObject objet, String cle) {
		if (!objet.has(cle)) {
			return null;
		}
		String valeur = objet.get(cle).getAsString().trim();
		return valeur.isEmpty() ? null : valeur;
	}

	private static String champ(JsonObject objet, String cle) {
		if (!objet.has(cle)) {
			throw new IllegalArgumentException("il manque le champ \"" + cle + "\"");
		}
		return objet.get(cle).getAsString();
	}

	private static String nomDepuisChemin(ResourceLocation chemin) {
		String brut = chemin.getPath();
		return brut.substring(DOSSIER.length() + 1, brut.length() - SUFFIXE.length());
	}

	/**
	 * Installe une liste recue du serveur. C'est ce que fait le client : il ne lit
	 * aucun fichier d'espece, il croit le serveur.
	 */
	public static void poser(List<Espece> recues) {
		Map<String, Espece> nouvelles = new LinkedHashMap<>();
		for (Espece espece : recues) {
			nouvelles.put(espece.nom(), espece);
		}
		especes = Collections.unmodifiableMap(nouvelles);
		// Le client aussi : c'est lui qui verrouille la roue pendant le geste.
		Longueurs.charger(especes.values());
		Compagnon.LOG.info("Especes recues du serveur : {}",
				especes.isEmpty() ? "aucune" : String.join(", ", especes.keySet()));
	}

	/** Toutes les especes, pour les envoyer. */
	public static List<Espece> toutes() {
		return List.copyOf(especes.values());
	}

	/** L'espece demandee, ou {@code null} si elle n'est pas chargee. */
	public static Espece get(String nom) {
		return especes.get(nom);
	}

	/**
	 * Le nom de la premiere espece chargee, ou une chaine vide s'il n'y en a
	 * aucune. Sert de repli : le code n'ecrit ainsi jamais un nom d'espece.
	 */
	public static String premierNom() {
		return especes.isEmpty() ? "" : especes.keySet().iterator().next();
	}

	public static Set<String> noms() {
		return especes.keySet();
	}

	/**
	 * Cette animation concerne-t-elle cette espece ?
	 *
	 * <h2>Le probleme qu'elle resout</h2>
	 *
	 * <p>La table des niveaux ne connait que des noms d'animation. Tant qu'il
	 * n'y avait qu'une espece, cela suffisait. Des la deuxieme, la roue d'un
	 * dragonnet montrait les cases d'une mouette : des animations qu'il ne
	 * possede pas, et qui ne font donc <b>rien</b> quand on clique dessus.
	 *
	 * <h2>Comment on tranche</h2>
	 *
	 * <p>Blockbench nomme ses animations {@code animation.<espece>.<geste>}.
	 * Le deuxieme morceau donne donc l'espece, gratuitement et sans rien
	 * demander a personne.
	 *
	 * <p>Et si ce morceau n'est <b>pas</b> le nom d'une espece connue, on rend
	 * vrai : le nom appartient a quelqu'un qui ne suit pas la convention, et
	 * il vaut mieux montrer une case de trop que d'en cacher une bonne. C'est
	 * aussi ce qui evite qu'une faute de frappe fasse disparaitre la roue.
	 */
	public static boolean concerne(String animation, String espece) {
		return concerne(animation, espece, especes.keySet());
	}

	/**
	 * La meme decision, sans rien lire de global.
	 *
	 * <p>C'est cette version qui est verifiee : elle ne touche ni au monde, ni
	 * aux registres, ni au journal, et se teste donc sans lancer Minecraft.
	 * L'autre n'est plus qu'une ligne au-dessus.
	 */
	/**
	 * Le nom affichable de cette espece-la.
	 *
	 * <p>Les ecrans ne connaissent souvent qu'une chaine — celle qui a voyage
	 * dans un paquet, dans un carnet, sur un oeuf. Ils passent par ici plutot
	 * que de montrer l'identifiant brut.
	 *
	 * <p>Une espece inconnue rend quand meme quelque chose de lisible : mieux
	 * vaut un titre approche qu'un trou dans l'interface.
	 */
	public static String titre(String espece) {
		if (espece == null || espece.isEmpty()) {
			return "";
		}
		Espece trouvee = especes.get(espece);
		return trouvee != null ? trouvee.titre() : joli(espece);
	}

	/** {@code oiseau_bleu} devient « Oiseau bleu ». */
	static String joli(String cle) {
		if (cle == null || cle.isEmpty()) {
			return "";
		}
		String espace = cle.replace('_', ' ');
		return Character.toUpperCase(espace.charAt(0)) + espace.substring(1);
	}

	static boolean concerne(String animation, String espece, Set<String> connues) {
		if (animation == null || espece == null) {
			return true;
		}
		String[] morceaux = animation.split(java.util.regex.Pattern.quote("."));
		if (morceaux.length < 3 || !"animation".equals(morceaux[0])) {
			return true;
		}
		String annoncee = morceaux[1];
		return !connues.contains(annoncee) || annoncee.equals(espece);
	}
}
