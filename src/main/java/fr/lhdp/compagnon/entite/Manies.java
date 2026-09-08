package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.fiche.FicheCompagnon;
import fr.lhdp.compagnon.fiche.Moment;

import java.util.ArrayList;
import java.util.List;

/**
 * Ses manies, et son defaut.
 *
 * <h2>Le plus court chemin entre un mob et une bete</h2>
 *
 * <p>Une manie est un geste inutile, repete, qui n'appartient qu'a lui : il
 * s'ebroue avant de s'asseoir, il salue le lever du jour, il tourne sur lui-meme
 * quand son maitre s'arrete. Personne ne le lui a appris et ca ne sert a rien.
 * C'est precisement pour ca que ca marche — on ne raconte pas d'un animal qu'il
 * a quatre-vingts d'affection, on raconte qu'il fait toujours ce truc-la.
 *
 * <p>Le defaut est l'autre moitie : une seule chose qui cloche, et qui ne se
 * corrige jamais. On apprend a faire avec. C'est la seule chose du mod que le
 * joueur ne pourra pas ameliorer, donc la seule qu'il devra accepter telle
 * quelle.
 *
 * <h2>Elles ne sont pas tirees au sort</h2>
 *
 * <p>Enfin, pas seulement. Le reservoir est <b>filtre par ce qu'il a vecu</b> :
 * une bete qu'on a beaucoup nourrie peut devenir gourmande, une bete qui a
 * beaucoup patauge peut prendre l'habitude de s'ebrouer. Une manie tiree au sort
 * a la naissance ferait illusion deux heures ; celle-ci raconte quelque chose.
 *
 * <h2>Ou c'est range</h2>
 *
 * <p>Dans les moments de la fiche, avec le prefixe {@code manie.} — la liste
 * existe deja, elle est datee, elle est sauvegardee, et {@code marquer} refuse
 * les doublons tout seul. <b>Aucun changement de format de sauvegarde</b> : une
 * fiche ecrite avant aujourd'hui se relit sans rien perdre, et prendra ses
 * manies a la premiere occasion.
 */
public final class Manies {

	private Manies() {
	}

	/** Le prefixe des manies dans les moments de la fiche. */
	public static final String PREFIXE = "manie.";

	/** Celui du defaut. Il n'y en a qu'un, et il ne change jamais. */
	public static final String PREFIXE_DEFAUT = "defaut.";

	/** Combien de manies il peut prendre au plus. */
	private static final int AU_PLUS = 2;

	/** A partir de combien de repas il peut devenir gourmand. */
	private static final int REPAS_QUI_GATENT = 15;

	/** A partir de combien de caresses il peut devenir collant. */
	private static final int CARESSES_QUI_COLLENT = 25;

	/**
	 * Ce qui declenche une manie.
	 *
	 * <p>Quatre situations seulement, et toutes se lisent sans chercher quoi que
	 * ce soit dans le monde : c'est ce qui permet d'en avoir beaucoup sans que ca
	 * coute quoi que ce soit a mille compagnons charges.
	 */
	public enum Quand {
		/** Le jour vient de se lever. */
		AUBE,
		/** Il pleut sur lui. */
		PLUIE,
		/** Son maitre s'est arrete a cote de lui. */
		MAITRE_POSE,
		/** Rien de particulier. C'est le charme de la chose. */
		SANS_RAISON
	}

	/**
	 * Une manie.
	 *
	 * @param id       son nom, celui de la cle dans la fiche et de la ligne du livre
	 * @param quand    ce qui la declenche
	 * @param geste    le role d'animation joue ; retombe sur rien si l'espece ne
	 *                 le decrit pas, et la manie ne se voit alors pas — c'est
	 *                 volontaire, mieux vaut rien qu'une animation fausse
	 * @param ticks    combien de temps dure le geste
	 * @param condition ce qu'il faut avoir vecu pour pouvoir la prendre
	 */
	public record Manie(String id, Quand quand, String geste, int ticks,
			java.util.function.Predicate<FicheCompagnon> condition) {
	}

	/** Tout ce qu'on peut prendre. Une dizaine suffit : elles se remarquent. */
	private static final List<Manie> RESERVOIR = List.of(
			new Manie("salue_le_jour", Quand.AUBE, "@salut", 25, f -> true),
			new Manie("etire_le_matin", Quand.AUBE, "@tourne", 20, f -> true),
			new Manie("deteste_la_pluie", Quand.PLUIE, "@triste", 22, f -> true),
			new Manie("joue_sous_la_pluie", Quand.PLUIE, "@joie", 25, f -> true),
			new Manie("tourne_quand_tu_t_arretes", Quand.MAITRE_POSE, "@tourne", 20, f -> true),
			new Manie("te_salue_toujours", Quand.MAITRE_POSE, "@salut", 22, f -> true),
			new Manie("reclame_a_table", Quand.MAITRE_POSE, "@ecoute", 25,
					f -> f.compteur(FicheCompagnon.REPAS) >= REPAS_QUI_GATENT),
			new Manie("se_colle", Quand.MAITRE_POSE, "@joyeux", 22,
					f -> f.compteur(FicheCompagnon.CARESSES) >= CARESSES_QUI_COLLENT),
			new Manie("parle_tout_seul", Quand.SANS_RAISON, "@ecoute", 18, f -> true),
			new Manie("regarde_le_ciel", Quand.SANS_RAISON, "@reconnait", 22, f -> true));

	/**
	 * Les defauts.
	 *
	 * <p>Un seul par bete, et il ne change jamais. <b>Chacun a une consequence
	 * qu'on voit</b> — un defaut range dans une fiche et qui ne fait rien ne
	 * serait qu'une ligne de texte de plus :
	 *
	 * <ul>
	 *   <li>{@code gourmand} : il reclame des qu'on s'arrete pres de lui</li>
	 *   <li>{@code peureux} : il refuse de dormir dehors la nuit</li>
	 *   <li>{@code tetu} : il s'endort meme quand son maitre s'agite</li>
	 *   <li>{@code bavard} : ses manies sans raison reviennent trois fois plus</li>
	 *   <li>{@code casse_cou} : il ne frissonne pas dans le froid</li>
	 *   <li>{@code jaloux} : il ne salue jamais les autres compagnons</li>
	 * </ul>
	 */
	public static final String GOURMAND = "gourmand";
	public static final String PEUREUX = "peureux";
	public static final String TETU = "tetu";
	public static final String BAVARD = "bavard";
	public static final String CASSE_COU = "casse_cou";
	public static final String JALOUX = "jaloux";

	private static final List<String> DEFAUTS = List.of(
			GOURMAND, PEUREUX, TETU, BAVARD, CASSE_COU, JALOUX);

	// --- Ce que la fiche connait ---------------------------------------------

	/** Les manies deja prises, dans l'ordre ou elles sont venues. */
	public static List<String> siennes(FicheCompagnon fiche) {
		List<String> prises = new ArrayList<>(AU_PLUS);
		for (Moment moment : fiche.moments()) {
			if (moment.cle().startsWith(PREFIXE)) {
				prises.add(moment.cle().substring(PREFIXE.length()));
			}
		}
		return prises;
	}

	/** Son defaut, ou une chaine vide s'il n'en a pas encore. */
	public static String defaut(FicheCompagnon fiche) {
		for (Moment moment : fiche.moments()) {
			if (moment.cle().startsWith(PREFIXE_DEFAUT)) {
				return moment.cle().substring(PREFIXE_DEFAUT.length());
			}
		}
		return "";
	}

	// --- Ce qu'il prend en grandissant ---------------------------------------

	/**
	 * Lui donne un defaut s'il n'en a pas, et rend vrai si la fiche a change.
	 *
	 * <p>Le defaut arrive tout de suite : c'est ce qui doit rendre deux betes
	 * differentes des la premiere heure. Les manies, elles, se meritent.
	 */
	public static boolean donnerUnDefaut(FicheCompagnon fiche, java.util.Random hasard) {
		if (!defaut(fiche).isEmpty()) {
			return false;
		}
		String choisi = DEFAUTS.get(hasard.nextInt(DEFAUTS.size()));
		return fiche.marquer(PREFIXE_DEFAUT + choisi, System.currentTimeMillis());
	}

	/**
	 * Lui donne peut-etre une manie de plus.
	 *
	 * <p>A appeler rarement — une fois par jour de jeu suffit. Elle n'en prend
	 * jamais plus de deux : trois gestes personnels, ce n'est plus une manie,
	 * c'est un numero de cirque.
	 *
	 * @return vrai si la fiche a change
	 */
	public static boolean peutEtreUneManie(FicheCompagnon fiche, java.util.Random hasard) {
		List<String> deja = siennes(fiche);
		if (deja.size() >= AU_PLUS) {
			return false;
		}

		List<Manie> possibles = new ArrayList<>(RESERVOIR.size());
		for (Manie manie : RESERVOIR) {
			if (!deja.contains(manie.id()) && manie.condition().test(fiche)) {
				possibles.add(manie);
			}
		}
		if (possibles.isEmpty()) {
			return false;
		}
		Manie choisie = possibles.get(hasard.nextInt(possibles.size()));
		return fiche.marquer(PREFIXE + choisie.id(), System.currentTimeMillis());
	}

	// --- Ce qui se joue en jeu ------------------------------------------------

	/** La manie de ce nom, ou {@code null} si le reservoir a change depuis. */
	public static Manie trouver(String id) {
		for (Manie manie : RESERVOIR) {
			if (manie.id().equals(id)) {
				return manie;
			}
		}
		return null;
	}
}
