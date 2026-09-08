package fr.lhdp.compagnon.fiche;

import fr.lhdp.compagnon.Compagnon;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * La copie de secours des fiches.
 *
 * <h2>Pourquoi ce fichier existe</h2>
 *
 * <p>Quand Minecraft n'arrive pas a lire un fichier de donnees sauvegardees —
 * coupure de courant pendant l'ecriture, disque plein, fichier tronque — il
 * <b>ecrit une ligne dans le journal et repart d'un magasin vide</b>. Rien ne
 * plante, rien n'alerte : au prochain demarrage, tous les compagnons du serveur
 * ont simplement disparu.
 *
 * <p>C'est la seule facon connue de violer la regle « on ne perd jamais un
 * compagnon », et elle est silencieuse. D'ou cette copie.
 *
 * <h2>Comment elle se comporte</h2>
 *
 * <ul>
 *   <li>A chaque demarrage reussi <b>avec des fiches</b>, le fichier est recopie
 *       en {@code .secours}. La copie est donc toujours un etat connu bon.</li>
 *   <li>Si le demarrage donne <b>zero fiche alors qu'une copie de secours
 *       existe</b>, on crie dans le journal : c'est exactement la signature d'un
 *       fichier perdu. On ne restaure pas tout seul — un monde neuf a aussi zero
 *       fiche, et ecraser serait pire que de prevenir.</li>
 * </ul>
 */
public final class Secours {

	/** Le fichier ecrit par Minecraft pour nos fiches. */
	private static final String FICHIER = Fiches.NOM_FICHIER + ".dat";

	private static final String SUFFIXE_SECOURS = ".secours";

	private Secours() {
	}

	public static Path fichier(MinecraftServer serveur) {
		return serveur.getWorldPath(LevelResource.ROOT).resolve("data").resolve(FICHIER);
	}

	public static Path copie(MinecraftServer serveur) {
		return serveur.getWorldPath(LevelResource.ROOT).resolve("data")
				.resolve(FICHIER + SUFFIXE_SECOURS);
	}

	/**
	 * A appeler une fois le serveur demarre, quand les fiches viennent d'etre
	 * lues.
	 */
	public static void verifierAuDemarrage(MinecraftServer serveur) {
		Fiches fiches = Fiches.de(serveur);
		Path source = fichier(serveur);
		Path copie = copie(serveur);

		if (fiches.nombre() > 0) {
			ecrireLaCopie(source, copie, fiches.nombre());
			return;
		}

		if (!Files.exists(copie)) {
			// Monde neuf, ou serveur sans compagnon. Rien a signaler.
			return;
		}

		long taille = tailleDe(copie);
		if (taille <= 0L) {
			return;
		}

		Compagnon.LOG.error("======================================================");
		Compagnon.LOG.error("AUCUNE FICHE CHARGEE alors qu'une copie de secours existe.");
		Compagnon.LOG.error("  fichier attendu : {}", source);
		Compagnon.LOG.error("  copie de secours : {} ({} octets)", copie, taille);
		Compagnon.LOG.error("Si le serveur avait des compagnons hier, le fichier est perdu.");
		Compagnon.LOG.error("Pour restaurer : arreter le serveur, remplacer le fichier par");
		Compagnon.LOG.error("la copie de secours en retirant le suffixe, puis redemarrer.");
		Compagnon.LOG.error("La copie n'a PAS ete ecrasee.");
		Compagnon.LOG.error("======================================================");
	}

	private static void ecrireLaCopie(Path source, Path copie, int combien) {
		if (!Files.exists(source)) {
			// Premier demarrage : le fichier n'est ecrit qu'a la sauvegarde.
			return;
		}
		try {
			Files.copy(source, copie, StandardCopyOption.REPLACE_EXISTING);
			Compagnon.LOG.info("Copie de secours des fiches ecrite ({} compagnon(s)).", combien);
		} catch (IOException echec) {
			// Ne pas empecher le serveur de demarrer pour autant.
			Compagnon.LOG.warn("Copie de secours impossible : {}", echec.toString());
		}
	}

	private static long tailleDe(Path chemin) {
		try {
			return Files.size(chemin);
		} catch (IOException echec) {
			return -1L;
		}
	}
}
