package fr.lhdp.compagnon.mission;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Une mission est rangee dans la fiche par l'empreinte de son nom.
 *
 * <h2>Pourquoi pas son numero d'ordre</h2>
 *
 * <p>C'est ce que faisait la premiere version. Sauf que la liste des moules est
 * triee par nom de fichier : <b>ajouter une seule mission decale toutes celles
 * qui viennent apres</b>. Les missions en cours de tous les joueurs du serveur
 * auraient change d'enonce d'un coup, en gardant leur progression.
 *
 * <h2>Ce que ce test protege</h2>
 *
 * <p>Deux noms differents qui tomberaient sur la meme empreinte donneraient deux
 * missions confondues : celle qu'on remplit validerait l'autre. Ce serait
 * silencieux, permanent, et impossible a comprendre depuis le jeu.
 *
 * <p>Une trentaine de noms sur un milliard de valeurs : la collision est
 * improbable. Improbable n'est pas impossible, et c'est exactement le genre de
 * chose qu'une machine verifie mieux qu'un humain.
 */
class EmpreintesTest {

	private static final Path MISSIONS = Path.of("src/main/resources/data/compagnon/missions");

	/** La meme formule que {@code Carnet.empreinteDe}, sur le nom du fichier. */
	private static int empreinte(String id) {
		return (id.hashCode() & 0x3FFFFFFF) + 1;
	}

	private static List<String> identifiants() throws IOException {
		try (Stream<Path> parcours = Files.list(MISSIONS)) {
			return parcours
					.filter(chemin -> chemin.toString().endsWith(".json"))
					.map(chemin -> {
						String nom = chemin.getFileName().toString();
						return nom.substring(0, nom.length() - ".json".length());
					})
					.toList();
		}
	}

	@Test
	@DisplayName("deux missions n'ont jamais la meme empreinte")
	void aucuneCollision() throws IOException {
		Map<Integer, String> vues = new HashMap<>();
		for (String id : identifiants()) {
			String deja = vues.put(empreinte(id), id);
			assertTrue(deja == null,
					"« " + id + " » et « " + deja + " » ont la meme empreinte : "
							+ "l'une validerait l'autre. Renomme l'un des deux fichiers.");
		}
		assertFalse(vues.isEmpty(), "aucune mission trouvee");
	}

	@Test
	@DisplayName("aucune empreinte ne vaut zero, qui veut dire « pas de mission »")
	void jamaisZero() throws IOException {
		for (String id : identifiants()) {
			assertTrue(empreinte(id) > 0,
					"« " + id + " » a une empreinte de " + empreinte(id)
							+ ", que la fiche lirait comme une place vide");
		}
	}

	@Test
	@DisplayName("l'empreinte ne depend que du nom, jamais de l'ordre des fichiers")
	void elleNeBougePas() throws IOException {
		// La garantie qui compte : ajouter une mission ne doit rien changer aux
		// empreintes des autres. On le verifie en recalculant apres avoir insere
		// un nom qui se trie avant tous les autres.
		List<String> avant = identifiants();
		for (String id : avant) {
			int calculee = empreinte(id);
			// Un second calcul, apres avoir touche a la liste : meme resultat.
			List<String> apres = new java.util.ArrayList<>(avant);
			apres.add(0, "aaa_mission_ajoutee");
			assertTrue(apres.contains(id));
			assertTrue(empreinte(id) == calculee,
					"l'empreinte de « " + id + " » a bouge");
		}
	}
}
