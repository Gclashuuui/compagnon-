package fr.lhdp.compagnon.competence;

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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Les competences lues dans {@code data/compagnon/competences/}.
 *
 * <p>Un fichier par competence, comme pour les aliments et les soins. Le serveur
 * fait autorite : c'est lui qui dit ce qui existe, ce que ca coute et ce que ca
 * fait, et un {@code /reload} suffit a tout relire.
 *
 * <p>Les competences sont triees par palier puis par nom : le livre les affiche
 * dans cet ordre, et un joueur qui parcourt sa page voit donc son avenir dans le
 * bon sens.
 */
public final class Competences {

	/** Le dossier lu, sous {@code data/compagnon/}. */
	public static final String DOSSIER = "competences";

	private static volatile Map<String, Competence> competences = Map.of();

	private Competences() {
	}

	public static void charger(ResourceManager gestionnaire) {
		// Le chemin est relatif a la racine des donnees, et on filtre sur notre
		// espace de noms : sans ce filtre, un autre mod qui aurait un dossier du
		// meme nom verrait ses fichiers lus comme les notres. Meme facon de faire
		// que pour les aliments et les soins.
		Map<ResourceLocation, Resource> fichiers = new LinkedHashMap<>();
		gestionnaire.listResources(DOSSIER, chemin -> chemin.getPath().endsWith(".json"))
				.forEach((chemin, ressource) -> {
					if (chemin.getNamespace().equals(Compagnon.MOD_ID)) {
						fichiers.put(chemin, ressource);
					}
				});

		List<Competence> lues = new ArrayList<>();
		for (Map.Entry<ResourceLocation, Resource> fichier : fichiers.entrySet()) {
			String id = nomDuFichier(fichier.getKey());
			try (BufferedReader lecteur = fichier.getValue().openAsReader()) {
				lues.add(lire(id, JsonParser.parseReader(lecteur).getAsJsonObject()));
			} catch (Exception echec) {
				// Une competence illisible n'empeche pas les autres d'exister : on
				// nomme la fautive et on continue.
				Compagnon.LOG.error("Competence \"{}\" illisible : {}", id, echec.getMessage());
			}
		}

		// Par palier, puis par nom. C'est l'ordre dans lequel le livre les montre,
		// et donc l'ordre dans lequel un joueur decouvre ce qui l'attend.
		lues.sort((a, b) -> a.niveauRequis() != b.niveauRequis()
				? Integer.compare(a.niveauRequis(), b.niveauRequis())
				: a.nom().compareToIgnoreCase(b.nom()));

		Map<String, Competence> rangees = new LinkedHashMap<>();
		for (Competence competence : lues) {
			rangees.put(competence.id(), competence);
		}
		competences = Collections.unmodifiableMap(rangees);

		Compagnon.LOG.info("Competences chargees : {}",
				competences.isEmpty() ? "aucune" : String.join(", ", competences.keySet()));
	}

	private static Competence lire(String id, JsonObject objet) {
		String nom = objet.has("nom") ? objet.get("nom").getAsString() : id;
		String description = objet.has("description")
				? objet.get("description").getAsString()
				: "";
		int niveauRequis = objet.has("niveau_requis")
				? objet.get("niveau_requis").getAsInt()
				: 1;

		List<String> especes = new ArrayList<>();
		if (objet.has("especes")) {
			for (JsonElement element : objet.getAsJsonArray("especes")) {
				especes.add(element.getAsString());
			}
		}

		Map<String, Float> effets = new LinkedHashMap<>();
		if (objet.has("effets")) {
			JsonObject bloc = objet.getAsJsonObject("effets");
			for (Map.Entry<String, JsonElement> effet : bloc.entrySet()) {
				if (!Competence.CLES_CONNUES.contains(effet.getKey())) {
					// Pas une erreur : on peut ecrire un fichier avant le code qui
					// le lira. Mais il faut le dire, sinon une faute de frappe
					// donne une competence qui ne fait rien, en silence.
					Compagnon.LOG.warn("Competence \"{}\" : l'effet \"{}\" n'est pas "
							+ "connu du mod, il ne fera rien.", id, effet.getKey());
					continue;
				}
				effets.put(effet.getKey(), effet.getValue().getAsFloat());
			}
		}

		return new Competence(id, nom, description, niveauRequis,
				List.copyOf(especes), Map.copyOf(effets));
	}

	private static String nomDuFichier(ResourceLocation chemin) {
		String complet = chemin.getPath();
		int barre = complet.lastIndexOf('/');
		String fichier = barre >= 0 ? complet.substring(barre + 1) : complet;
		return fichier.endsWith(".json")
				? fichier.substring(0, fichier.length() - ".json".length())
				: fichier;
	}

	/** Toutes les competences, dans l'ordre d'affichage. */
	public static Collection<Competence> toutes() {
		return competences.values();
	}

	/** Celles qu'une espece donnee peut prendre. */
	public static List<Competence> pour(String espece) {
		List<Competence> siennes = new ArrayList<>();
		for (Competence competence : competences.values()) {
			if (competence.pour(espece)) {
				siennes.add(competence);
			}
		}
		return siennes;
	}

	public static Competence get(String id) {
		return competences.get(id);
	}

	/**
	 * Le produit des effets d'une liste de competences, pour une cle donnee.
	 *
	 * <p>Un <b>produit</b> et non une somme : deux competences qui reduisent la
	 * faim de 20 % chacune la reduisent de 36 %, pas de 40 %. C'est ce qui evite
	 * qu'une pile de bonus finisse par ramener une barre a zero — et donc qu'un
	 * compagnon de haut niveau n'ait plus jamais faim.
	 *
	 * @param cle l'effet cherche, parmi {@link Competence#CLES_CONNUES}
	 */
	public static float facteur(Set<String> prises, String cle) {
		float facteur = 1.0F;
		for (String id : prises) {
			Competence competence = competences.get(id);
			if (competence != null) {
				facteur *= competence.effet(cle, 1.0F);
			}
		}
		return facteur;
	}

	/**
	 * La somme des effets d'une liste, pour une cle qui s'ajoute.
	 *
	 * <p>Certains effets ne se multiplient pas : deux blocs de portee de voix en
	 * plus, puis deux autres, font quatre blocs. Multiplier n'aurait aucun sens.
	 */
	public static float bonus(Set<String> prises, String cle) {
		float total = 0.0F;
		for (String id : prises) {
			Competence competence = competences.get(id);
			if (competence != null) {
				total += competence.effet(cle, 0.0F);
			}
		}
		return total;
	}

	/** Les identifiants valides parmi ceux d'une fiche, pour ne rien garder de mort. */
	public static Set<String> nettoyer(Collection<String> ids) {
		Set<String> valides = new LinkedHashSet<>();
		for (String id : ids) {
			if (competences.containsKey(id)) {
				valides.add(id);
			}
		}
		return valides;
	}
}
