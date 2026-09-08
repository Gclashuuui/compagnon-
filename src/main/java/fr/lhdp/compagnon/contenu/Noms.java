package fr.lhdp.compagnon.contenu;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.lhdp.compagnon.Compagnon;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Des noms que le moteur vocal sait entendre.
 *
 * <h2>Pourquoi cette liste existe</h2>
 *
 * <p>Les joueurs inventent des noms. « Zibou », « Pixnou », « Krakos » : rien de
 * tout cela n'est dans le vocabulaire francais du moteur, et un mot absent est
 * retire de la grammaire <b>en silence</b>. Le compagnon ne repond alors jamais a
 * son nom, et son proprietaire n'a aucun moyen de comprendre pourquoi.
 *
 * <p>Plutot que de refuser les noms — ils appartiennent aux joueurs, pas au
 * moteur — on en <b>propose</b>. Un bouton, un nom qui marche, et celui qui n'en
 * a rien a faire tape ce qu'il veut.
 *
 * <p>Les 103 noms de la liste ont ete verifies un par un dans le vocabulaire reel
 * du modele. Voir {@code fr.lhdp.compagnon.voix.Lexique}.
 *
 * <h2>Pourquoi on lit le jar directement</h2>
 *
 * <p>C'est l'ecran de bapteme qui s'en sert, donc le <b>client</b> — et sur un
 * serveur dedie, le client n'a jamais vu les donnees du serveur. Le fichier, lui,
 * voyage dans le jar, que les deux cotes savent ouvrir. Meme raison que pour
 * l'onglet creatif.
 */
public final class Noms {

	private static final String FICHIER = "noms.json";

	private static volatile List<String> noms;

	private Noms() {
	}

	/** La liste, lue une seule fois. Vide si le fichier manque : rien ne casse. */
	public static List<String> tous() {
		List<String> deja = noms;
		if (deja != null) {
			return deja;
		}
		synchronized (Noms.class) {
			if (noms == null) {
				noms = lire();
			}
			return noms;
		}
	}

	/** Un nom au hasard, ou une chaine vide si la liste n'a pas pu etre lue. */
	public static String auHasard(Random hasard) {
		List<String> tous = tous();
		return tous.isEmpty() ? "" : tous.get(hasard.nextInt(tous.size()));
	}

	private static List<String> lire() {
		Optional<Path> chemin = FabricLoader.getInstance()
				.getModContainer(Compagnon.MOD_ID)
				.flatMap(mod -> mod.findPath("data/" + Compagnon.MOD_ID + "/" + FICHIER));

		if (chemin.isEmpty()) {
			Compagnon.LOG.warn("Liste de noms introuvable : aucune suggestion au bapteme.");
			return List.of();
		}
		try (BufferedReader lecteur = Files.newBufferedReader(chemin.get())) {
			JsonObject racine = JsonParser.parseReader(lecteur).getAsJsonObject();
			List<String> lus = new ArrayList<>();
			for (JsonElement element : racine.getAsJsonArray("noms")) {
				lus.add(element.getAsString());
			}
			return List.copyOf(lus);
		} catch (Exception echec) {
			Compagnon.LOG.error("Liste de noms illisible : {}", echec.toString());
			return List.of();
		}
	}
}
