package fr.lhdp.compagnon;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Le dossier ou l'equipe depose son contenu, a cote du monde.
 *
 * <pre>
 * &lt;dossier du serveur&gt;/compagnon/especes/*.json
 * &lt;dossier du serveur&gt;/compagnon/aliments/*.json
 * &lt;dossier du serveur&gt;/compagnon/soins/*.json
 * &lt;dossier du serveur&gt;/compagnon/bobos/*.json
 * </pre>
 *
 * <h2>A quoi ca sert</h2>
 *
 * <p>Ajouter une espece ou une friandise demandait jusqu'ici de rouvrir le jar,
 * ou de fabriquer un pack de donnees. On depose maintenant un fichier ici, on
 * fait {@code /reload}, et c'est en jeu. <b>Aucune ligne de code, aucune
 * recompilation, aucun redemarrage.</b>
 *
 * <p>Ces fichiers sont lus <b>apres</b> ceux du mod, et un fichier du meme nom
 * remplace celui d'origine. C'est voulu : on peut corriger la taille d'une
 * espece ou l'effet d'un aliment sans attendre une nouvelle version.
 *
 * <h2>Ce que ce dossier ne peut pas faire</h2>
 *
 * <p>Il porte les <b>chiffres et les noms</b>. Ceux-la sont deja envoyes aux
 * clients par le mod.
 *
 * <p>Il ne porte pas les <b>images</b> : un modele, une texture, un fichier
 * d'animation vivent chez le joueur, pas sur le serveur. Une espece vraiment
 * nouvelle a donc aussi besoin d'un pack de ressources, distribue par
 * {@code resource-pack=} dans {@code server.properties}. Aucun serveur ne peut
 * inventer une image dans la memoire d'un client — c'est une regle de Minecraft,
 * pas une limite du mod.
 *
 * <h2>Rien n'est jamais fatal</h2>
 *
 * <p>Un fichier fautif est signale dans le log et <b>ignore</b> : il n'emporte
 * jamais les autres avec lui, et il n'empeche pas le serveur de demarrer.
 */
public final class DossierDuServeur {

	private static final String SUFFIXE = ".json";

	private DossierDuServeur() {
	}

	/** La racine : {@code <dossier du serveur>/compagnon/}. */
	public static Path racine() {
		return FabricLoader.getInstance().getGameDir().resolve(Compagnon.MOD_ID);
	}

	/**
	 * Les sous-dossiers d'un sous-dossier, tries.
	 *
	 * <p>Une creature complete est un DOSSIER, pas un fichier : elle a son JSON,
	 * son modele, ses animations et plusieurs textures. Voir {@code Especes}.
	 */
	public static List<Path> sousDossiers(String sousDossier) {
		Path dossier = racine().resolve(sousDossier);
		if (!Files.isDirectory(dossier)) {
			return List.of();
		}
		try (Stream<Path> flux = Files.list(dossier)) {
			return flux.filter(Files::isDirectory).sorted().toList();
		} catch (IOException echec) {
			Compagnon.LOG.error("Dossier {} illisible : {}", dossier, echec.toString());
			return List.of();
		}
	}

	/** Un fichier JSON precis, ou {@code null} s'il manque ou s'il est fautif. */
	public static JsonObject lireUn(Path fichier) {
		if (!Files.isRegularFile(fichier)) {
			return null;
		}
		try (BufferedReader lecteur = Files.newBufferedReader(fichier)) {
			return JsonParser.parseReader(lecteur).getAsJsonObject();
		} catch (Exception echec) {
			Compagnon.LOG.error("{} illisible : {}", fichier, echec.getMessage());
			return null;
		}
	}

	/**
	 * Les images d'un dossier, sans leur extension, triees.
	 *
	 * <p>C'est ce qui permet d'ajouter une couleur de compagnon en deposant un
	 * fichier : chaque image devient une variante portant son nom.
	 */
	public static List<String> images(Path dossier) {
		if (!Files.isDirectory(dossier)) {
			return List.of();
		}
		try (Stream<Path> flux = Files.list(dossier)) {
			return flux.map(chemin -> chemin.getFileName().toString())
					.filter(nom -> nom.endsWith(".png"))
					.map(nom -> nom.substring(0, nom.length() - 4))
					.sorted()
					.toList();
		} catch (IOException echec) {
			Compagnon.LOG.error("Dossier {} illisible : {}", dossier, echec.toString());
			return List.of();
		}
	}

	/**
	 * Lit tous les fichiers d'un sous-dossier et les remet un par un.
	 *
	 * <p>Le dossier est <b>cree s'il n'existe pas</b>. Un dossier vide qu'on voit
	 * se remarque ; un dossier a inventer ne se devine pas.
	 *
	 * @param sousDossier  {@code especes}, {@code aliments}...
	 * @param pourChacun   recoit le nom du fichier sans extension, et son contenu
	 * @return combien de fichiers ont ete lus sans erreur
	 */
	public static int lire(String sousDossier, BiConsumer<String, JsonObject> pourChacun) {
		Path dossier = racine().resolve(sousDossier);

		if (!Files.isDirectory(dossier)) {
			try {
				Files.createDirectories(dossier);
			} catch (IOException echec) {
				Compagnon.LOG.warn("Dossier {} impossible a creer : {}", dossier, echec.toString());
			}
			return 0;
		}

		int lus = 0;
		try (Stream<Path> fichiers = Files.list(dossier)) {
			// Trie : deux fichiers qui se marchent dessus doivent le faire dans un
			// ordre previsible, pas dans celui du systeme de fichiers.
			for (Path fichier : fichiers.sorted().toList()) {
				String nomFichier = fichier.getFileName().toString();
				if (!nomFichier.endsWith(SUFFIXE)) {
					continue;
				}
				String nom = nomFichier.substring(0, nomFichier.length() - SUFFIXE.length());
				try (BufferedReader lecteur = Files.newBufferedReader(fichier)) {
					pourChacun.accept(nom, JsonParser.parseReader(lecteur).getAsJsonObject());
					lus++;
				} catch (Exception echec) {
					Compagnon.LOG.error("{}/{} illisible : {}", sousDossier, nomFichier,
							echec.getMessage());
				}
			}
		} catch (IOException echec) {
			Compagnon.LOG.error("Dossier {} illisible : {}", dossier, echec.toString());
		}

		if (lus > 0) {
			Compagnon.LOG.info("{} fichier(s) lus dans {}", lus, dossier);
		}
		return lus;
	}
}
