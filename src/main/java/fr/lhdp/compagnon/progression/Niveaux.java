package fr.lhdp.compagnon.progression;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.fiche.Barre;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lit la table de progression dans {@code data/compagnon/niveaux.json}.
 *
 * <p>Cote serveur : c'est lui qui fait autorite sur les niveaux et les
 * deblocages. Un {@code /reload} relit le fichier ; aucune recompilation.
 */
public final class Niveaux {

	/** Le fichier lu, sous {@code data/compagnon/}. */
	public static final String FICHIER = "niveaux.json";

	private static volatile Progression progression = parDefaut();

	private Niveaux() {
	}

	/** La table en cours. N'est jamais {@code null}. */
	public static Progression progression() {
		return progression;
	}

	public static void charger(ResourceManager gestionnaire) {
		ResourceLocation chemin = Compagnon.id(FICHIER);
		Optional<Resource> fichier = gestionnaire.getResource(chemin);

		if (fichier.isEmpty()) {
			Compagnon.LOG.error("Table des niveaux introuvable ({}). Reglage de secours utilise.", chemin);
			progression = parDefaut();
			return;
		}

		try (BufferedReader lecteur = fichier.get().openAsReader()) {
			progression = lire(JsonParser.parseReader(lecteur).getAsJsonObject());
			Compagnon.LOG.info("Table des niveaux chargee : {} paliers, niveau maximum {}.",
					progression.paliers().size(), progression.niveauMaximum());
		} catch (Exception echec) {
			Compagnon.LOG.error("Table des niveaux illisible ({}) : {}. Reglage de secours utilise.",
					chemin, echec.getMessage());
			progression = parDefaut();
		}
	}

	private static Progression lire(JsonObject racine) {
		JsonObject reglages = racine.getAsJsonObject("progression");

		Map<SourceXp, Progression.Source> sources = new EnumMap<>(SourceXp.class);
		JsonObject sourcesJson = reglages.getAsJsonObject("sources");
		for (SourceXp source : SourceXp.values()) {
			if (!sourcesJson.has(source.cle())) {
				throw new IllegalArgumentException("il manque la source d'experience \"" + source.cle() + "\"");
			}
			JsonObject bloc = sourcesJson.getAsJsonObject(source.cle());
			sources.put(source, new Progression.Source(
					bloc.get("xp").getAsInt(),
					bloc.get("plafond_par_jour").getAsInt()));
		}

		Map<Barre, Progression.ReglageBarre> barres = new EnumMap<>(Barre.class);
		JsonObject barresJson = reglages.getAsJsonObject("barres");
		for (Barre barre : Barre.values()) {
			if (!barresJson.has(barre.cle())) {
				throw new IllegalArgumentException("il manque la barre \"" + barre.cle() + "\"");
			}
			JsonObject bloc = barresJson.getAsJsonObject(barre.cle());
			float parMinute = bloc.get("par_minute").getAsFloat();
			// Absent, le taux de repos vaut celui d'activite : seule l'energie a
			// besoin de se comporter autrement quand il ne fait rien.
			float parMinuteRepos = bloc.has("par_minute_repos")
					? bloc.get("par_minute_repos").getAsFloat()
					: parMinute;
			barres.put(barre, new Progression.ReglageBarre(
					bloc.get("depart").getAsFloat(),
					parMinute,
					parMinuteRepos,
					bloc.get("minimum").getAsFloat(),
					bloc.get("maximum").getAsFloat()));
		}

		// Les bobos : absent du fichier, le taux vaut zero et il n'y en a jamais.
		float bobosParHeure = reglages.has("bobos")
				? reglages.getAsJsonObject("bobos").get("par_heure_de_jeu").getAsFloat()
				: 0.0F;

		// Ce que coute une action de la roue. Absent, elle ne coute rien et ne
		// demande aucun etat particulier.
		Progression.Action action = reglages.has("actions")
				? new Progression.Action(
						reglages.getAsJsonObject("actions").get("energie_depensee").getAsFloat(),
						reglages.getAsJsonObject("actions").get("energie_minimum").getAsFloat())
				: new Progression.Action(0.0F, 0.0F);

		List<Progression.Palier> paliers = new ArrayList<>();
		JsonArray niveauxJson = racine.getAsJsonArray("niveaux");
		for (JsonElement element : niveauxJson) {
			JsonObject bloc = element.getAsJsonObject();
			List<String> debloque = new ArrayList<>();
			if (bloc.has("debloque")) {
				for (JsonElement nom : bloc.getAsJsonArray("debloque")) {
					debloque.add(nom.getAsString());
				}
			}
			paliers.add(new Progression.Palier(
					bloc.get("niveau").getAsInt(),
					bloc.get("xp").getAsInt(),
					List.copyOf(debloque)));
		}

		if (paliers.isEmpty()) {
			throw new IllegalArgumentException("la liste \"niveaux\" est vide");
		}

		// Le fichier peut etre dans n'importe quel ordre : c'est le code qui trie.
		paliers.sort(Comparator.comparingInt(Progression.Palier::xp));

		return new Progression(Map.copyOf(sources), Map.copyOf(barres), bobosParHeure,
				action, List.copyOf(paliers));
	}

	/**
	 * Reglage de secours, utilise seulement si le fichier manque ou ne se lit pas.
	 * Il permet au serveur de tourner au lieu de planter, et le journal dit
	 * pourquoi on en est la.
	 */
	private static Progression parDefaut() {
		Map<SourceXp, Progression.Source> sources = new EnumMap<>(SourceXp.class);
		for (SourceXp source : SourceXp.values()) {
			sources.put(source, new Progression.Source(1, 10));
		}

		Map<Barre, Progression.ReglageBarre> barres = new EnumMap<>(Barre.class);
		for (Barre barre : Barre.values()) {
			barres.put(barre, new Progression.ReglageBarre(
					Barre.MAXIMUM, 0.0F, 0.0F, 0.0F, Barre.MAXIMUM));
		}

		return new Progression(
				Map.copyOf(sources),
				Map.copyOf(barres),
				0.0F,
				new Progression.Action(0.0F, 0.0F),
				List.of(new Progression.Palier(1, 0, List.of())));
	}
}
