package fr.lhdp.compagnon.contenu;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.DossierDuServeur;
import fr.lhdp.compagnon.fiche.Barre;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Les aliments, les objets de soin et les bobos, lus dans {@code data/compagnon/}.
 *
 * <p>Ajouter un aliment, c'est ajouter un fichier. Jamais une classe, jamais une
 * compilation. Un {@code /reload} suffit a le voir arriver en jeu.
 *
 * <p>Cote serveur : c'est lui qui decide de ce qu'un aliment fait, pas le client.
 */
public final class Contenu {

	public static final String DOSSIER_ALIMENTS = "aliments";
	public static final String DOSSIER_SOINS = "soins";
	public static final String DOSSIER_BOBOS = "bobos";
	public static final String DOSSIER_CARACTERES = "caracteres";

	/** Le fichier des reves, lu sous {@code data/compagnon/}. */
	public static final String FICHIER_REVES = "reves.json";

	private static final String SUFFIXE = ".json";

	private static volatile Map<String, Donnable> aliments = Map.of();
	private static volatile Map<String, Donnable> soins = Map.of();
	private static volatile Map<String, Bobo> bobos = Map.of();
	private static volatile Map<String, Caractere> caracteres = Map.of();
	private static volatile List<String> reves = List.of();

	private Contenu() {
	}

	public static void charger(ResourceManager gestionnaire) {
		aliments = lireDonnables(gestionnaire, DOSSIER_ALIMENTS);
		soins = lireDonnables(gestionnaire, DOSSIER_SOINS);
		bobos = lireBobos(gestionnaire);
		caracteres = lireCaracteres(gestionnaire);
		reves = lireReves(gestionnaire);

		Compagnon.LOG.info("Contenu charge : {} aliment(s), {} soin(s), {} bobo(s), {} caractere(s).",
				aliments.size(), soins.size(), bobos.size(), caracteres.size());
		Compagnon.LOG.info("Reves charges : {}.", reves.size());

		// Un bobo qui nomme un remede inexistant serait incurable. Mieux vaut le
		// dire au demarrage qu'au moment ou un joueur essaie de le soigner.
		for (Bobo bobo : bobos.values()) {
			if (!soins.containsKey(bobo.soignePar())) {
				Compagnon.LOG.error(
						"Le bobo \"{}\" se soigne avec \"{}\", qui n'existe pas dans {}/ : il serait incurable.",
						bobo.id(), bobo.soignePar(), DOSSIER_SOINS);
			}
		}
	}

	private static Map<String, Donnable> lireDonnables(ResourceManager gestionnaire, String dossier) {
		Map<String, Donnable> lus = new LinkedHashMap<>();

		for (Map.Entry<ResourceLocation, Resource> fichier : fichiersDe(gestionnaire, dossier).entrySet()) {
			String id = idDepuis(fichier.getKey(), dossier);
			try (BufferedReader lecteur = fichier.getValue().openAsReader()) {
				JsonObject objet = JsonParser.parseReader(lecteur).getAsJsonObject();
				lus.put(id, new Donnable(
						id,
						objet.get("nom").getAsString(),
						lireEffets(objet),
						objet.has("objet") ? objet.get("objet").getAsString() : "",
						objet.has("modele") ? objet.get("modele").getAsInt() : 0));
			} catch (Exception echec) {
				Compagnon.LOG.error("{} \"{}\" illisible : {}", dossier, id, echec.getMessage());
			}
		}
		// Ce que l'equipe a depose passe en dernier, et gagne. Voir DossierDuServeur.
		DossierDuServeur.lire(dossier, (id, objet) -> {
			try {
				lus.put(id, new Donnable(id,
						objet.get("nom").getAsString(),
						lireEffets(objet),
						objet.has("objet") ? objet.get("objet").getAsString() : "",
						objet.has("modele") ? objet.get("modele").getAsInt() : 0));
			} catch (Exception echec) {
				Compagnon.LOG.error("{} \"{}\" du dossier du serveur : {}", dossier, id,
						echec.getMessage());
			}
		});

		// Meme raison que pour les especes : l'ordre du fichier doit tenir.
		return Collections.unmodifiableMap(lus);
	}

	private static Map<Barre, Float> lireEffets(JsonObject objet) {
		Map<Barre, Float> effets = new EnumMap<>(Barre.class);
		if (!objet.has("effets")) {
			return Map.copyOf(effets);
		}
		for (Map.Entry<String, JsonElement> effet : objet.getAsJsonObject("effets").entrySet()) {
			Barre barre = Barre.depuis(effet.getKey());
			if (barre == null) {
				throw new IllegalArgumentException("barre inconnue : \"" + effet.getKey() + "\"");
			}
			effets.put(barre, effet.getValue().getAsFloat());
		}
		return Map.copyOf(effets);
	}

	private static Map<String, Bobo> lireBobos(ResourceManager gestionnaire) {
		Map<String, Bobo> lus = new LinkedHashMap<>();

		for (Map.Entry<ResourceLocation, Resource> fichier
				: fichiersDe(gestionnaire, DOSSIER_BOBOS).entrySet()) {
			String id = idDepuis(fichier.getKey(), DOSSIER_BOBOS);
			try (BufferedReader lecteur = fichier.getValue().openAsReader()) {
				JsonObject objet = JsonParser.parseReader(lecteur).getAsJsonObject();
				lus.put(id, new Bobo(
						id,
						objet.get("nom").getAsString(),
						objet.get("description").getAsString(),
						objet.get("soigne_par").getAsString(),
						objet.get("sante").getAsFloat()));
			} catch (Exception echec) {
				Compagnon.LOG.error("bobo \"{}\" illisible : {}", id, echec.getMessage());
			}
		}
		DossierDuServeur.lire(DOSSIER_BOBOS, (id, objet) -> {
			try {
				lus.put(id, new Bobo(id,
						objet.get("nom").getAsString(),
						objet.get("description").getAsString(),
						objet.get("soigne_par").getAsString(),
						objet.get("sante").getAsFloat()));
			} catch (Exception echec) {
				Compagnon.LOG.error("bobo \"{}\" du dossier du serveur : {}", id, echec.getMessage());
			}
		});

		// Meme raison que pour les especes : l'ordre du fichier doit tenir.
		return Collections.unmodifiableMap(lus);
	}

	private static Map<String, Caractere> lireCaracteres(ResourceManager gestionnaire) {
		Map<String, Caractere> lus = new LinkedHashMap<>();

		for (Map.Entry<ResourceLocation, Resource> fichier
				: fichiersDe(gestionnaire, DOSSIER_CARACTERES).entrySet()) {
			String id = idDepuis(fichier.getKey(), DOSSIER_CARACTERES);
			try (BufferedReader lecteur = fichier.getValue().openAsReader()) {
				JsonObject objet = JsonParser.parseReader(lecteur).getAsJsonObject();
				JsonObject traits = objet.getAsJsonObject("traits");
				lus.put(id, new Caractere(
						id,
						objet.get("nom").getAsString(),
						trait(traits, "sociabilite"),
						trait(traits, "attachement"),
						trait(traits, "vivacite"),
						trait(traits, "calin"),
						trait(traits, "curiosite")));
			} catch (Exception echec) {
				Compagnon.LOG.error("caractere {} illisible : {}", id, echec.getMessage());
			}
		}
		return Collections.unmodifiableMap(lus);
	}

	/**
	 * Un trait absent vaut la moyenne, et tout est borne entre zero et un : un
	 * fichier incomplet ou mal ecrit reste utilisable au lieu de tout casser.
	 */
	private static float trait(JsonObject traits, String nom) {
		if (traits == null || !traits.has(nom)) {
			return 0.5F;
		}
		return Math.max(0.0F, Math.min(1.0F, traits.get(nom).getAsFloat()));
	}

	/**
	 * La liste des reves.
	 *
	 * <p>Le fichier ne porte que des identifiants ; les phrases vivent dans le
	 * fichier de langue. On peut donc les reecrire, ou les traduire, sans toucher
	 * ni au code ni aux donnees.
	 */
	private static List<String> lireReves(ResourceManager gestionnaire) {
		ResourceLocation chemin = Compagnon.id(FICHIER_REVES);
		var fichier = gestionnaire.getResource(chemin);
		if (fichier.isEmpty()) {
			return List.of();
		}
		try (BufferedReader lecteur = fichier.get().openAsReader()) {
			JsonObject objet = JsonParser.parseReader(lecteur).getAsJsonObject();
			List<String> lus = new java.util.ArrayList<>();
			for (JsonElement element : objet.getAsJsonArray("reves")) {
				lus.add(element.getAsString());
			}
			return List.copyOf(lus);
		} catch (Exception echec) {
			Compagnon.LOG.error("Liste des reves illisible : {}", echec.getMessage());
			return List.of();
		}
	}

	public static List<String> reves() {
		return reves;
	}




	/** Un aliment au hasard, pour ce dont il a envie. */
	public static Donnable alimentAuHasard(Random hasard) {
		if (aliments.isEmpty()) {
			return null;
		}
		List<String> noms = List.copyOf(aliments.keySet());
		return aliments.get(noms.get(hasard.nextInt(noms.size())));
	}

	/** Un reve au hasard, ou {@code null} si le pack n'en decrit aucun. */
	public static String reveAuHasard(Random hasard) {
		return reves.isEmpty() ? null : reves.get(hasard.nextInt(reves.size()));
	}

	private static Map<ResourceLocation, Resource> fichiersDe(ResourceManager gestionnaire, String dossier) {
		Map<ResourceLocation, Resource> retenus = new LinkedHashMap<>();
		gestionnaire.listResources(dossier, chemin -> chemin.getPath().endsWith(SUFFIXE))
				.forEach((chemin, ressource) -> {
					if (chemin.getNamespace().equals(Compagnon.MOD_ID)) {
						retenus.put(chemin, ressource);
					}
				});
		return retenus;
	}

	private static String idDepuis(ResourceLocation chemin, String dossier) {
		String brut = chemin.getPath();
		return brut.substring(dossier.length() + 1, brut.length() - SUFFIXE.length());
	}

	// --- Lecture ----------------------------------------------------------------

	public static Donnable aliment(String id) {
		return aliments.get(id);
	}

	public static Donnable soin(String id) {
		return soins.get(id);
	}

	public static Bobo bobo(String id) {
		return bobos.get(id);
	}

	public static List<String> nomsAliments() {
		return List.copyOf(aliments.keySet());
	}

	public static List<String> nomsSoins() {
		return List.copyOf(soins.keySet());
	}

	public static List<String> nomsBobos() {
		return List.copyOf(bobos.keySet());
	}

	public static Caractere caractere(String id) {
		return caracteres.get(id);
	}

	public static List<String> nomsCaracteres() {
		return List.copyOf(caracteres.keySet());
	}

	/**
	 * Un caractere au hasard, pour la naissance d'un compagnon.
	 *
	 * <p>C'est ici que se joue le fait que deux compagnons identiques sur le
	 * papier ne se ressemblent pas en jeu.
	 */
	public static Caractere caractereAuHasard(Random hasard) {
		if (caracteres.isEmpty()) {
			return Caractere.ORDINAIRE;
		}
		List<String> noms = List.copyOf(caracteres.keySet());
		return caracteres.get(noms.get(hasard.nextInt(noms.size())));
	}

	/** Un bobo au hasard, ou {@code null} si le pack n'en decrit aucun. */
	public static Bobo boboAuHasard(Random hasard) {
		if (bobos.isEmpty()) {
			return null;
		}
		List<String> noms = List.copyOf(bobos.keySet());
		return bobos.get(noms.get(hasard.nextInt(noms.size())));
	}
}
