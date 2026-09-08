package fr.lhdp.compagnon.reglage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.lhdp.compagnon.Compagnon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.util.Optional;

/**
 * Les reglages du serveur, dans {@code data/compagnon/reglages.json}.
 *
 * <p>Ce qui ne tient ni dans une espece, ni dans un aliment, ni dans la table des
 * niveaux : les quelques nombres qui decrivent comment le mod se comporte sur
 * <b>ce</b> serveur. Un {@code /reload} les relit ; aucune recompilation.
 *
 * <p>Le fichier peut manquer et chaque cle peut manquer : dans ce cas on prend
 * la valeur par defaut, sans un mot. Un serveur qui n'a jamais entendu parler de
 * ce fichier doit marcher exactement comme avant.
 */
public final class Reglages {

	/** Le fichier lu, sous {@code data/compagnon/}. */
	public static final String FICHIER = "reglages.json";

	/**
	 * Combien de compagnons un joueur peut avoir dehors en meme temps.
	 *
	 * <p>Trois par defaut. Ce n'est pas une limite technique, c'est un choix :
	 * assez pour se promener avec sa petite bande, assez peu pour qu'une salle
	 * commune a cent personnes ne devienne pas une menagerie de mille creatures
	 * a animer et a envoyer sur le reseau.
	 *
	 * <p>Mettre {@code 0} enleve la limite.
	 */
	private static final int SORTIS_PAR_DEFAUT = 3;

	private static volatile int sortisEnMemeTemps = SORTIS_PAR_DEFAUT;

	/**
	 * Tous les combien de niveaux il gagne un point de competence.
	 *
	 * <p>Cinq. Au niveau maximum cela fait dix points pour neuf competences —
	 * presque toutes, mais pas tout de suite, et pas dans le meme ordre que le
	 * voisin. Un point par niveau en donnerait cinquante : chacun finirait par
	 * tout prendre, et deux betes du meme niveau seraient de nouveau identiques.
	 *
	 * <p>C'est la rarete des points qui fait le choix, et le choix qui fait la
	 * difference entre deux compagnons. Mettre {@code 0} enleve les points, donc
	 * les competences.
	 */
	private static final int POINTS_PAR_DEFAUT = 5;

	private static volatile int pointsTousLes = POINTS_PAR_DEFAUT;

	private Reglages() {
	}

	/**
	 * Combien de compagnons peuvent etre dehors a la fois, ou {@code 0} pour
	 * « autant qu'il veut ».
	 */
	public static int sortisEnMemeTemps() {
		return sortisEnMemeTemps;
	}

	/** Vrai s'il n'y a pas de limite du tout. */
	public static boolean sansLimite() {
		return sortisEnMemeTemps <= 0;
	}

	/** Tous les combien de niveaux il gagne un point de competence. */
	public static int pointsTousLesNiveaux() {
		return pointsTousLes;
	}

	public static void charger(ResourceManager gestionnaire) {
		int lu = SORTIS_PAR_DEFAUT;
		int points = POINTS_PAR_DEFAUT;
		ResourceLocation chemin = Compagnon.id(FICHIER);
		Optional<Resource> fichier = gestionnaire.getResource(chemin);

		if (fichier.isPresent()) {
			try (BufferedReader lecteur = fichier.get().openAsReader()) {
				JsonObject racine = JsonParser.parseReader(lecteur).getAsJsonObject();
				if (racine.has("invocation")) {
					JsonObject invocation = racine.getAsJsonObject("invocation");
					if (invocation.has("sortis_en_meme_temps")) {
						lu = invocation.get("sortis_en_meme_temps").getAsInt();
					}
				}
				if (racine.has("competences")) {
					JsonObject bloc = racine.getAsJsonObject("competences");
					if (bloc.has("un_point_tous_les_niveaux")) {
						points = bloc.get("un_point_tous_les_niveaux").getAsInt();
					}
				}
			} catch (Exception echec) {
				// Un fichier abime ne doit pas empecher le serveur de tourner : on
				// dit ce qui ne va pas et on garde les valeurs par defaut.
				Compagnon.LOG.error("Reglages illisibles ({}) : {}. Valeurs par defaut.",
						chemin, echec.getMessage());
				lu = SORTIS_PAR_DEFAUT;
				points = POINTS_PAR_DEFAUT;
			}
		}

		sortisEnMemeTemps = lu;
		pointsTousLes = points;
		Compagnon.LOG.info("Reglages : {} compagnon(s) dehors en meme temps, "
				+ "un point de competence tous les " + pointsTousLes + " niveaux.",
				sansLimite() ? "sans limite de" : String.valueOf(sortisEnMemeTemps));
	}
}
