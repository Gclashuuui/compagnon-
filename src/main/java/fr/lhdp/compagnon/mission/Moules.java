package fr.lhdp.compagnon.mission;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.lhdp.compagnon.Compagnon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lit les moules a missions dans {@code data/compagnon/missions/}.
 *
 * <p>Meme facon de faire que les competences, les aliments et les soins : un
 * fichier illisible ne fait pas tomber les autres, on nomme le fautif dans le
 * journal et on continue.
 *
 * <p><b>Le code ne connait aucune mission.</b> Il n'en connait que les
 * compteurs, et encore : il les traite comme des chaines. Ajouter une mission,
 * c'est ajouter un fichier — et si son compteur n'existe pas, elle ne progresse
 * simplement jamais, ce qu'un test attrape.
 */
public final class Moules {

	/** Le dossier lu, sous {@code data/compagnon/}. */
	public static final String DOSSIER = "missions";

	private static volatile List<Moule> moules = List.of();

	private Moules() {
	}

	public static List<Moule> tous() {
		return moules;
	}

	/** Un moule par son nom, ou {@code null} si le fichier a disparu depuis. */
	public static Moule get(String id) {
		for (Moule moule : moules) {
			if (moule.id().equals(id)) {
				return moule;
			}
		}
		return null;
	}

	public static void charger(ResourceManager gestionnaire) {
		Map<ResourceLocation, Resource> fichiers = new LinkedHashMap<>();
		gestionnaire.listResources(DOSSIER, chemin -> chemin.getPath().endsWith(".json"))
				.forEach((chemin, ressource) -> {
					if (chemin.getNamespace().equals(Compagnon.MOD_ID)) {
						fichiers.put(chemin, ressource);
					}
				});

		List<Moule> lus = new ArrayList<>();
		for (Map.Entry<ResourceLocation, Resource> fichier : fichiers.entrySet()) {
			String id = nomDuFichier(fichier.getKey());
			try (BufferedReader lecteur = fichier.getValue().openAsReader()) {
				lus.add(lire(id, JsonParser.parseReader(lecteur).getAsJsonObject()));
			} catch (Exception echec) {
				Compagnon.LOG.error("Moule a missions \"{}\" illisible : {}", id,
						echec.getMessage());
			}
		}

		// Un ordre stable, pour que deux serveurs tirent la meme chose avec la
		// meme graine : sans tri, l'ordre du systeme de fichiers deciderait.
		lus.sort((a, b) -> a.id().compareTo(b.id()));
		moules = List.copyOf(lus);

		long combien = 0L;
		for (Moule moule : moules) {
			combien += moule.crans();
		}
		Compagnon.LOG.info("Missions chargees : {} moules, soit {} enonces distincts.",
				moules.size(), combien);
	}

	private static Moule lire(String id, JsonObject racine) {
		String compteur = racine.get("compteur").getAsString();
		String texte = racine.get("texte").getAsString();

		List<Integer> quantites = entiers(racine, "quantites");
		if (quantites.isEmpty()) {
			throw new IllegalArgumentException("la liste \"quantites\" est vide");
		}
		List<Integer> xp = entiers(racine, "xp");
		if (xp.isEmpty()) {
			throw new IllegalArgumentException("la liste \"xp\" est vide");
		}

		String famille = racine.has("famille") ? racine.get("famille").getAsString() : id;
		boolean longue = racine.has("longue") && racine.get("longue").getAsBoolean();

		List<String> especes = new ArrayList<>();
		if (racine.has("especes")) {
			for (JsonElement element : racine.getAsJsonArray("especes")) {
				especes.add(element.getAsString());
			}
		}

		return new Moule(id, compteur, texte, List.copyOf(quantites), List.copyOf(xp),
				famille, List.copyOf(especes), longue);
	}

	private static List<Integer> entiers(JsonObject racine, String cle) {
		List<Integer> valeurs = new ArrayList<>();
		if (!racine.has(cle)) {
			return valeurs;
		}
		JsonArray tableau = racine.getAsJsonArray(cle);
		for (JsonElement element : tableau) {
			valeurs.add(element.getAsInt());
		}
		return valeurs;
	}

	private static String nomDuFichier(ResourceLocation chemin) {
		String texte = chemin.getPath();
		int barre = texte.lastIndexOf('/');
		int point = texte.lastIndexOf('.');
		return texte.substring(barre + 1, point < barre ? texte.length() : point);
	}
}
