package fr.lhdp.compagnon.espece;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.DossierDuServeur;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * La table de toutes les positions reglees, espece par espece.
 *
 * <h2>Deux sources, et la bonne gagne</h2>
 *
 * <p>Les positions livrees avec le mod vivent dans {@code data/}, en lecture
 * seule : elles sont dans le jar, et une mise a jour les remplace. Celles que
 * l'equipe regle en jeu vivent dans <b>{@code <serveur>/compagnon/ancrages.json}</b>,
 * et gagnent toujours.
 *
 * <p>C'est ce qui permet de regler une position sur le serveur en direct, sans
 * recompiler et sans qu'une nouvelle version du mod efface le travail. Le
 * fichier reste lisible et modifiable a la main.
 *
 * <h2>Un fichier a plat, pas dans les fiches d'espece</h2>
 *
 * <p>On aurait pu les ecrire dans {@code espece.json}. On ne le fait pas : ces
 * fichiers-la sont dans le jar, l'editeur ne pourrait pas les reecrire, et une
 * espece deposee par l'equipe melangerait alors ses reglages visuels et sa
 * definition. Un fichier separe se sauvegarde, se copie d'un serveur a l'autre,
 * et se relit d'un coup d'oeil.
 */
public final class Ancrages {

	private Ancrages() {
	}

	/** Le fichier livre dans le mod. */
	private static final String LIVRE = "ancrages.json";

	/** Celui que l'equipe ecrit, sous {@code <serveur>/compagnon/}. */
	private static final String DU_SERVEUR = "ancrages.json";

	private static final Gson JOLI = new GsonBuilder().setPrettyPrinting().create();

	/** espece -> emplacement -> position. */
	private static Map<String, Map<String, Ancrage>> table = Map.of();

	// --- Lire ------------------------------------------------------------------------

	public static void charger(ResourceManager gestionnaire) {
		Map<String, Map<String, Ancrage>> lues = new LinkedHashMap<>();

		try {
			// La pile entiere : un pack de donnees peut ajouter ses propres
			// reglages par-dessus ceux du mod, comme pour tout le reste.
			for (Resource ressource : gestionnaire.getResourceStack(Compagnon.id(LIVRE))) {
				try (BufferedReader lecteur = ressource.openAsReader()) {
					avaler(lues, JsonParser.parseReader(lecteur).getAsJsonObject());
				}
			}
		} catch (Exception echec) {
			Compagnon.LOG.error("Ancrages livres illisibles : {}", echec.getMessage());
		}

		// CE QUE L'EQUIPE A REGLE PASSE EN DERNIER, ET GAGNE.
		JsonObject duServeur = DossierDuServeur.lireUn(fichierDuServeur());
		if (duServeur != null) {
			avaler(lues, duServeur);
		}

		table = Collections.unmodifiableMap(lues);
		Compagnon.LOG.info("Ancrages charges : {} espece(s) reglee(s)", table.size());
	}

	/**
	 * Verse un fichier dans la table, emplacement par emplacement.
	 *
	 * <p>Emplacement par emplacement et non espece par espece : regler la bouche
	 * du dragonnet ne doit pas effacer son collier.
	 */
	private static void avaler(Map<String, Map<String, Ancrage>> lues, JsonObject fichier) {
		for (String espece : fichier.keySet()) {
			if (espece.startsWith("_")) {
				continue;   // une note laissee par un humain
			}
			JsonObject emplacements = fichier.getAsJsonObject(espece);
			if (emplacements == null) {
				continue;
			}
			Map<String, Ancrage> pourElle =
					lues.computeIfAbsent(espece, cle -> new LinkedHashMap<>());
			for (String emplacement : emplacements.keySet()) {
				// Une note laissee par un humain, ici aussi. Sans ce garde, un
				// _comment ecrit dans une espece faisait echouer le chargement de
				// TOUTE la table, et plus aucun objet ne se dessinait nulle part.
				if (emplacement.startsWith("_")) {
					continue;
				}
				pourElle.put(emplacement,
						Ancrage.depuis(emplacements.getAsJsonObject(emplacement)));
			}
		}
	}

	/**
	 * Ou se pose cette chose-la, sur cette bete-la.
	 *
	 * <p>Rend {@link Ancrage#ABSENT} quand personne ne l'a reglee : l'appelant
	 * doit s'abstenir de dessiner plutot que d'inventer un endroit.
	 */
	public static Ancrage de(String espece, String objet) {
		Map<String, Ancrage> pourElle = table.get(espece);
		if (pourElle == null) {
			return Ancrage.ABSENT;
		}
		Ancrage sien = pourElle.get(objet);
		if (sien != null && sien.regle()) {
			return sien;
		}
		// Rien pour cet objet-la : on prend le reglage general de l'espece.
		return pourElle.getOrDefault(Ancrage.DEFAUT, Ancrage.ABSENT);
	}

	/** Le reglage propre a cet objet, sans repli : c'est ce que l'editeur montre. */
	public static Ancrage exactement(String espece, String objet) {
		Map<String, Ancrage> pourElle = table.get(espece);
		return pourElle == null
			? Ancrage.ABSENT
			: pourElle.getOrDefault(objet, Ancrage.ABSENT);
	}

	/** Toute la table, pour l'envoyer au client. */
	public static Map<String, Map<String, Ancrage>> toutes() {
		return table;
	}

	/** Le client remplace sa table par celle du serveur. */
	public static void poser(Map<String, Map<String, Ancrage>> recues) {
		table = Collections.unmodifiableMap(new LinkedHashMap<>(recues));
	}

	// --- Ecrire ----------------------------------------------------------------------

	/**
	 * Enregistre une position reglee en jeu.
	 *
	 * <p>On relit le fichier avant d'ecrire plutot que de vider la table en
	 * memoire : quelqu'un a pu l'editer a la main entre-temps, et deux personnes
	 * peuvent regler deux especes en meme temps.
	 *
	 * @return vrai si le fichier a bien ete ecrit
	 */
	public static boolean enregistrer(String espece, String emplacement, Ancrage ancrage) {
		Path fichier = fichierDuServeur();
		JsonObject racine = DossierDuServeur.lireUn(fichier);
		if (racine == null) {
			racine = new JsonObject();
			racine.add("_lisez_moi", note());
		}
		JsonObject pourElle = racine.getAsJsonObject(espece);
		if (pourElle == null) {
			pourElle = new JsonObject();
			racine.add(espece, pourElle);
		}
		pourElle.add(emplacement, ancrage.versJson());

		try {
			Files.createDirectories(fichier.getParent());
			try (Writer plume = Files.newBufferedWriter(fichier, StandardCharsets.UTF_8)) {
				JOLI.toJson(racine, plume);
			}
		} catch (IOException echec) {
			Compagnon.LOG.error("Ancrages : impossible d'ecrire {} : {}",
					fichier, echec.getMessage());
			return false;
		}

		// La table en memoire suit tout de suite : on ne fait pas attendre un
		// /reload a quelqu'un qui vient de bouger une balle d'un demi-pixel.
		enMemoire(espece, emplacement, ancrage);
		return true;
	}

	/**
	 * Change une position <b>sans rien ecrire</b>.
	 *
	 * <p>C'est ce dont l'editeur se sert a chaque pression de bouton : la balle
	 * bouge sous les yeux de celui qui regle, image par image, et rien n'est
	 * enregistre tant qu'il n'a pas dit que c'etait bon. Fermer sans enregistrer
	 * ne coute donc rien — le prochain envoi du serveur remet tout en place.
	 */
	public static void enMemoire(String espece, String emplacement, Ancrage ancrage) {
		Map<String, Map<String, Ancrage>> copie = new LinkedHashMap<>();
		table.forEach((cle, valeurs) -> copie.put(cle, new LinkedHashMap<>(valeurs)));
		copie.computeIfAbsent(espece, cle -> new LinkedHashMap<>()).put(emplacement, ancrage);
		table = Collections.unmodifiableMap(copie);
	}

	private static Path fichierDuServeur() {
		return DossierDuServeur.racine().resolve(DU_SERVEUR);
	}

	private static com.google.gson.JsonArray note() {
		com.google.gson.JsonArray lignes = new com.google.gson.JsonArray();
		lignes.add("Ou se posent les objets sur chaque espece.");
		lignes.add("Ecrit par l'editeur en jeu (/compagnon ancrer), relisible a la main.");
		lignes.add("Le decalage est en unites de modele : seize font un bloc.");
		lignes.add("Les angles sont en degres, l'echelle est un facteur.");
		return lignes;
	}
}
