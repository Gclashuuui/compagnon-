package fr.lhdp.compagnon.entite;

import com.google.gson.JsonParser;
import fr.lhdp.compagnon.Compagnon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Charge les cerveaux JSON avant les espèces qui les choisissent. */
public final class Cerveaux {

	public static final String DOSSIER = "cerveaux";
	private static volatile Map<String, ProfilCerveau> profils = Map.of();

	private Cerveaux() {
	}

	public static void charger(ResourceManager ressources) {
		Map<String, ProfilCerveau> lus = new LinkedHashMap<>();
		Map<ResourceLocation, Resource> fichiers = ressources.listResources(DOSSIER,
				chemin -> chemin.getPath().endsWith(".json"));
		for (Map.Entry<ResourceLocation, Resource> fichier : fichiers.entrySet()) {
			if (!fichier.getKey().getNamespace().equals(Compagnon.MOD_ID)) {
				continue;
			}
			String chemin = fichier.getKey().getPath();
			String id = chemin.substring(DOSSIER.length() + 1, chemin.length() - 5);
			try (BufferedReader lecteur = fichier.getValue().openAsReader()) {
				lus.put(id, ProfilCerveau.lire(id,
						JsonParser.parseReader(lecteur).getAsJsonObject()));
			} catch (Exception echec) {
				Compagnon.LOG.error("Cerveau \"{}\" illisible ({}) : {}",
						id, fichier.getKey(), echec.getMessage());
			}
		}
		profils = Collections.unmodifiableMap(lus);
		Compagnon.LOG.info("Cerveaux chargés : {}",
				profils.isEmpty() ? "aucun" : String.join(", ", profils.keySet()));
	}

	public static boolean existe(String id) {
		return profils.containsKey(id);
	}

	public static ProfilCerveau get(String id, boolean vole) {
		return profils.getOrDefault(id, ProfilCerveau.parDefaut(vole));
	}
}
