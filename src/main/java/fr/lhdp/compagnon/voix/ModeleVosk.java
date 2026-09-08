package fr.lhdp.compagnon.voix;

import fr.lhdp.compagnon.Compagnon;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Ou trouver le modele vocal francais, sans rien demander a personne.
 *
 * <p>Vosk a besoin d'un vrai dossier sur le disque, d'environ 68 Mo. Le modele ne
 * voyage pas dans notre jar : il pese 42 Mo compresse, et <b>tous les joueurs
 * telechargent le jar</b> alors que la reconnaissance ne tourne que sur le
 * serveur. Faire porter 42 Mo a mille joueurs pour une fonction qui ne les
 * concerne pas serait un mauvais marche.
 *
 * <h2>Ou on regarde, et dans cet ordre</h2>
 *
 * <ol>
 *   <li>{@code <jeu>/compagnon/vosk-model-fr} — le notre, deja extrait ;</li>
 *   <li>{@code <jeu>/lhdpsecret/vosk-model-fr} — <b>celui du mod des passages
 *       secrets</b>. Les deux mods tournent sur le meme serveur : reextraire les
 *       memes 68 Mo a cote serait du gaspillage pur ;</li>
 *   <li>{@code <jeu>/vosk-model-fr} — un modele pose a la main par l'equipe ;</li>
 *   <li>{@code <jeu>/compagnon/model-fr.zip} — une archive posee a la main, qu'on
 *       extrait une fois pour toutes ;</li>
 *   <li>{@code /vosk/model-fr.zip} dans notre jar, si quelqu'un a choisi de l'y
 *       mettre. Le code le gere, le projet ne le fait pas.</li>
 * </ol>
 *
 * <p>Aucune de ces etapes n'est obligatoire. Sans modele, les ordres a la voix
 * s'eteignent et <b>tout le reste du mod fonctionne</b> : le livre, la roue, les
 * caresses, la nourriture. On le dit une fois dans le log, calmement, avec le
 * chemin exact ou poser le fichier.
 */
public final class ModeleVosk {

	/** Nom du dossier extrait, partout ou on le cherche. */
	private static final String DOSSIER = "vosk-model-fr";

	/** Nom de l'archive, si quelqu'un en pose une. */
	private static final String ARCHIVE = "model-fr.zip";

	/** L'archive dans le jar, si elle y est. */
	private static final String ARCHIVE_EMBARQUEE = "/vosk/" + ARCHIVE;

	/** Longueur maximale d'un chemin dans l'archive. Garde-fou. */
	private static final int CHEMIN_MAXIMUM = 512;

	private static Path trouve;
	private static boolean cherche;
	private static String raisonDeLAbsence = "";

	private ModeleVosk() {
	}

	/**
	 * Le dossier du modele, pret a l'emploi, ou {@code null}.
	 *
	 * <p>Ne leve jamais rien : un modele absent doit eteindre la voix, jamais
	 * empecher le serveur de tourner. Le resultat est retenu — on ne cherche
	 * qu'une fois par demarrage.
	 */
	public static synchronized Path dossier() {
		if (cherche) {
			return trouve;
		}
		cherche = true;

		Path jeu = FabricLoader.getInstance().getGameDir();
		Path chezNous = jeu.resolve(Compagnon.MOD_ID).resolve(DOSSIER);

		// 1, 2, 3 : un dossier deja extrait quelque part.
		for (Path candidat : List.of(chezNous,
				jeu.resolve("lhdpsecret").resolve(DOSSIER),
				jeu.resolve(DOSSIER))) {
			if (utilisable(candidat)) {
				trouve = candidat;
				Compagnon.LOG.info("Modele vocal trouve : {}", candidat);
				return trouve;
			}
		}

		// 4, 5 : une archive a extraire, posee a cote ou embarquee.
		try {
			Path archive = jeu.resolve(Compagnon.MOD_ID).resolve(ARCHIVE);
			if (Files.isRegularFile(archive)) {
				try (InputStream flux = Files.newInputStream(archive)) {
					extraire(flux, chezNous);
				}
				trouve = chezNous;
				Compagnon.LOG.info("Modele vocal installe depuis {} vers {}", archive, chezNous);
				return trouve;
			}
			try (InputStream flux = ModeleVosk.class.getResourceAsStream(ARCHIVE_EMBARQUEE)) {
				if (flux != null) {
					extraire(flux, chezNous);
					trouve = chezNous;
					Compagnon.LOG.info("Modele vocal installe dans {}", chezNous);
					return trouve;
				}
			}
		} catch (IOException | RuntimeException echec) {
			raisonDeLAbsence = "installation impossible : " + echec;
			Compagnon.LOG.error("Modele vocal : {}", raisonDeLAbsence);
			trouve = null;
			return null;
		}

		raisonDeLAbsence = "aucun modele trouve";
		Compagnon.LOG.info("""
				Pas de modele vocal : les ordres a la voix sont indisponibles. \
				Tout le reste du mod fonctionne normalement.
				Pour les activer, posez le dossier d'un modele Vosk francais ici : {}
				ou l'archive {} ici : {}""",
				chezNous, ARCHIVE, jeu.resolve(Compagnon.MOD_ID));
		return null;
	}

	/** Pourquoi il n'y a pas de modele, pour la commande de diagnostic. */
	public static synchronized String raisonDeLAbsence() {
		return raisonDeLAbsence;
	}

	/**
	 * Un dossier est utilisable s'il porte ce qu'un modele Vosk doit porter.
	 *
	 * <p>On ne se contente pas de son existence : une extraction interrompue — une
	 * coupure de courant, un disque plein — laisse un dossier incomplet, que Vosk
	 * accepte d'ouvrir avant de mourir en memoire native, loin d'ici. Autant le
	 * refuser tout de suite.
	 *
	 * <p>Le dossier {@code am/} porte le modele acoustique et {@code graph/} la
	 * grammaire : sans l'un des deux, il n'y a pas de modele.
	 *
	 * <p><b>Pas de numero de version.</b> On a d'abord ecrit un temoin
	 * {@code .version} pour reextraire tout seul le jour ou l'on changerait de
	 * modele — mais on ne le relisait jamais, et le commentaire promettait donc
	 * quelque chose de faux. La verification par structure est de toute facon plus
	 * solide : elle attrape aussi les dossiers a moitie ecrits, ce qu'un numero ne
	 * ferait pas. Pour changer de modele : supprimer le dossier, il se refera.
	 */
	private static boolean utilisable(Path candidat) {
		return Files.isDirectory(candidat.resolve("am"))
				&& Files.isDirectory(candidat.resolve("graph"));
	}

	private static void extraire(InputStream archive, Path cible) throws IOException {
		Compagnon.LOG.info("Installation du modele vocal francais : quelques secondes, une seule fois.");

		// On repart d'un dossier propre : un reliquat d'extraction interrompue
		// rendrait Vosk instable, et l'instabilite serait native, donc muette.
		if (Files.exists(cible)) {
			effacer(cible);
		}
		Files.createDirectories(cible);

		try (ZipInputStream zip = new ZipInputStream(archive)) {
			ZipEntry entree;
			while ((entree = zip.getNextEntry()) != null) {
				copier(zip, entree, cible);
				zip.closeEntry();
			}
		}
		if (!utilisable(cible)) {
			throw new IOException("archive extraite mais incomplete : ni am/ ni graph/ dans " + cible);
		}
	}

	/**
	 * Ecrit une entree de l'archive.
	 *
	 * <p>Les archives officielles rangent tout sous un dossier racine
	 * ({@code vosk-model-small-fr-0.22/}) : on retire ce niveau pour que le chemin
	 * final reste le meme le jour ou l'on change de modele.
	 */
	private static void copier(ZipInputStream zip, ZipEntry entree, Path cible) throws IOException {
		String nom = sansLaRacine(entree.getName());
		if (nom.isEmpty() || nom.length() > CHEMIN_MAXIMUM) {
			return;
		}

		Path destination = cible.resolve(nom).normalize();
		// « Zip slip » : une archive mal intentionnee peut contenir « ../.. » et
		// ecrire n'importe ou sur le disque du serveur.
		if (!destination.startsWith(cible)) {
			throw new IOException("entree d'archive hors du dossier cible : " + entree.getName());
		}

		if (entree.isDirectory()) {
			Files.createDirectories(destination);
			return;
		}
		Files.createDirectories(destination.getParent());
		Files.copy(zip, destination, StandardCopyOption.REPLACE_EXISTING);
	}

	private static String sansLaRacine(String nom) {
		int barre = nom.indexOf('/');
		return barre < 0 ? "" : nom.substring(barre + 1);
	}

	private static void effacer(Path chemin) throws IOException {
		try (var parcours = Files.walk(chemin)) {
			// Les fichiers d'abord, les dossiers ensuite : d'ou l'ordre inverse.
			for (Path element : parcours.sorted(Comparator.reverseOrder()).toList()) {
				Files.deleteIfExists(element);
			}
		}
	}
}
