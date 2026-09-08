package fr.lhdp.compagnon;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le fil qui va du bebe a l'adulte tient-il ?
 *
 * <h2>Ce qui arriverait sans ces tests</h2>
 *
 * <p>Grandir change l'espece d'une fiche <b>en place</b>, sur un compagnon que
 * quelqu'un eleve depuis des semaines. C'est la seule ecriture du mod qui touche
 * a l'identite d'une bete deja vivante, et une faute ici ne se repare pas : la
 * fiche est deja ecrite quand on s'en apercoit.
 *
 * <ul>
 *   <li>un {@code devient} qui nomme une espece absente rendrait la bete
 *       invisible — le code refuse et journalise, mais mieux vaut ne jamais
 *       livrer ce fichier-la ;</li>
 *   <li>deux especes qui se designent l'une l'autre feraient osciller un
 *       compagnon d'une forme a l'autre toutes les minutes, pour toujours ;</li>
 *   <li>un adulte sans robe par defaut valide laisserait un damier a la place
 *       de la bete quand la couleur du petit n'existe pas chez lui.</li>
 * </ul>
 */
class CroissanceTest {

	private static final Path ESPECES = Path.of("src/main/resources/data/compagnon/especes");
	private static final Path NIVEAUX = Path.of("src/main/resources/data/compagnon/niveaux.json");

	@Test
	void chaqueEspeceQuiGranditNommeUneEspeceQuiExiste() throws IOException {
		Map<String, JsonObject> toutes = toutes();
		List<String> fautes = new ArrayList<>();

		for (Map.Entry<String, JsonObject> entree : toutes.entrySet()) {
			String devient = devient(entree.getValue());
			if (devient.isEmpty()) {
				continue;
			}
			if (!toutes.containsKey(devient)) {
				fautes.add(entree.getKey() + " doit devenir \"" + devient
						+ "\", qui n'est pas une espece du mod");
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	/**
	 * La chaine s'arrete toujours.
	 *
	 * <p>On la remonte pas a pas plutot que de chercher un cycle savamment : les
	 * chaines font deux maillons, et un parcours qui compte ses pas dit aussi bien
	 * « ca boucle » que « ca descend trop loin ».
	 */
	@Test
	void aucuneEspeceNeGranditEnRond() throws IOException {
		Map<String, JsonObject> toutes = toutes();
		List<String> fautes = new ArrayList<>();

		for (String depart : toutes.keySet()) {
			Set<String> vues = new HashSet<>();
			String ici = depart;
			while (!ici.isEmpty() && toutes.containsKey(ici)) {
				if (!vues.add(ici)) {
					fautes.add("en partant de " + depart + ", on repasse par " + ici);
					break;
				}
				ici = devient(toutes.get(ici));
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	@Test
	void leNiveauDeCroissanceEstAtteignable() throws IOException {
		int plafond = niveauMaximum();
		List<String> fautes = new ArrayList<>();

		for (Map.Entry<String, JsonObject> entree : toutes().entrySet()) {
			JsonObject fiche = entree.getValue();
			String devient = devient(fiche);
			int niveau = fiche.has("devient_au_niveau")
					? fiche.get("devient_au_niveau").getAsInt() : 0;

			if (!devient.isEmpty() && niveau <= 0) {
				fautes.add(entree.getKey() + " doit devenir \"" + devient
						+ "\" mais ne dit pas a quel niveau : il ne grandira jamais");
			}
			if (niveau > 0 && devient.isEmpty()) {
				fautes.add(entree.getKey() + " a un niveau de croissance (" + niveau
						+ ") mais ne dit pas en quoi : le nombre ne sert a rien");
			}
			if (niveau > plafond) {
				fautes.add(entree.getKey() + " grandit au niveau " + niveau
						+ ", au-dela du dernier niveau de la table (" + plafond + ")");
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	/**
	 * Le repli de couleur mene toujours quelque part.
	 *
	 * <p>Quand l'adulte n'a pas la robe du petit, on prend sa robe par defaut. Il
	 * faut donc qu'elle existe — sinon on echange une couleur manquante contre une
	 * autre, et la bete devient un damier.
	 */
	@Test
	void chaqueEspeceAUneRobeParDefautQuiExiste() throws IOException {
		List<String> fautes = new ArrayList<>();

		for (Map.Entry<String, JsonObject> entree : toutes().entrySet()) {
			JsonObject fiche = entree.getValue();
			JsonObject variantes = fiche.getAsJsonObject("variantes");
			if (variantes == null || variantes.isEmpty()) {
				fautes.add(entree.getKey() + " n'a aucune couleur");
				continue;
			}
			String defaut = fiche.has("variante_par_defaut")
					? fiche.get("variante_par_defaut").getAsString() : "";
			if (!variantes.has(defaut)) {
				fautes.add(entree.getKey() + " a pour robe par defaut \"" + defaut
						+ "\", qui n'est pas dans ses couleurs : "
						+ String.join(", ", variantes.keySet()));
			}
		}
		assertTrue(fautes.isEmpty(), String.join("\n", fautes));
	}

	// --- Outils ---------------------------------------------------------------------

	private static String devient(JsonObject fiche) {
		return fiche.has("devient") ? fiche.get("devient").getAsString() : "";
	}

	private static Map<String, JsonObject> toutes() throws IOException {
		Map<String, JsonObject> lues = new LinkedHashMap<>();
		if (!Files.isDirectory(ESPECES)) {
			return lues;
		}
		try (Stream<Path> flux = Files.list(ESPECES)) {
			for (Path fichier : flux.filter(c -> c.toString().endsWith(".json")).sorted().toList()) {
				String nom = fichier.getFileName().toString();
				lues.put(nom.substring(0, nom.length() - ".json".length()), lire(fichier));
			}
		}
		return lues;
	}

	private static int niveauMaximum() throws IOException {
		int plus = 0;
		for (com.google.gson.JsonElement niveau : lire(NIVEAUX).getAsJsonArray("niveaux")) {
			plus = Math.max(plus, niveau.getAsJsonObject().get("niveau").getAsInt());
		}
		return plus;
	}

	private static JsonObject lire(Path fichier) throws IOException {
		try (Reader lecteur = Files.newBufferedReader(fichier)) {
			return JsonParser.parseReader(lecteur).getAsJsonObject();
		}
	}
}
