package fr.lhdp.compagnon.voix;

import fr.lhdp.compagnon.Compagnon;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

/**
 * Les mots que le moteur sait entendre — et surtout, ceux qu'il ne sait pas.
 *
 * <h2>Le piege que cette classe existe pour desamorcer</h2>
 *
 * <p>Le moteur travaille avec une liste fermee : on lui donne les mots a guetter.
 * Quand un de ces mots ne figure pas dans son vocabulaire francais, il ne
 * proteste pas et ne leve rien. Il <b>retire le mot de la liste en silence</b> et
 * continue :
 *
 * <pre>WARNING (VoskAPI:UpdateGrammarFst()) Ignoring word missing in vocabulary: 'cacahuete'</pre>
 *
 * <p>Une liste ainsi videe ne reconnait plus rien du tout — pas seulement le mot
 * fautif. Le compagnon devient sourd, et personne ne voit pourquoi.
 *
 * <p>Or les joueurs baptisent leurs compagnons comme ils veulent. « Pixel »,
 * « Zibou », « Nyx » : rien de tout cela n'est dans le vocabulaire francais. Sans
 * cette classe, le premier joueur a inventer un nom rendrait <b>son</b> compagnon
 * sourd, et personne ne saurait dire pourquoi.
 *
 * <p>On lit donc le vocabulaire nous-memes, pour deux choses :
 *
 * <ul>
 *   <li>{@link Voix} n'inscrit dans la liste que les noms reellement connus — les
 *       autres sont simplement omis, et les ordres continuent de marcher ;</li>
 *   <li>au bapteme, on peut prevenir gentiment que ce nom-la ne s'appellera pas a
 *       la voix, <b>sans jamais le refuser</b> : le nom appartient au joueur, pas
 *       au moteur.</li>
 * </ul>
 *
 * <h2>Ou est ce vocabulaire</h2>
 *
 * <p>Le modele n'a pas de {@code graph/words.txt}, mais {@code graph/Gr.fst}
 * porte sa table de symboles en clair, au format binaire d'OpenFst. Les ~135 000
 * mots sont au tout debut du fichier : on n'en lit que le debut, une seule fois.
 */
public final class Lexique {

	/** Signature d'un automate OpenFst. */
	private static final int SIGNATURE_FST = 0x7EB2FDD6;

	/** Signature d'une table de symboles OpenFst. */
	private static final int SIGNATURE_TABLE = 2_125_658_996;

	/** Le drapeau qui annonce que la table des symboles d'entree suit l'en-tete. */
	private static final int DRAPEAU_SYMBOLES = 1;

	/**
	 * Garde-fou : un fichier abime pourrait annoncer un milliard de symboles et
	 * epuiser la memoire du serveur au demarrage. Le modele reel en declare 135 774.
	 */
	private static final int SYMBOLES_MAXIMUM = 5_000_000;

	/** Idem pour la longueur d'un mot : aucun mot francais ne pese un megaoctet. */
	private static final int MOT_MAXIMUM = 4096;

	/**
	 * Le lexique deja lu, ou {@code null} tant qu'il ne l'est pas.
	 *
	 * <p>{@code volatile} et non protege par la serrure de {@link #lire()} : c'est
	 * ce qui permet a {@link #dejaLu()} de repondre sans jamais attendre. Le fil
	 * principal du serveur ne doit pas se retrouver a patienter derriere la lecture
	 * de 135 000 mots pendant le prechauffage.
	 */
	private static volatile Lexique retenu;

	private static boolean cherche;

	private final Set<String> mots;

	/**
	 * Les mots accentues, ranges sous leur forme sans accent.
	 *
	 * <h2>Pourquoi cette carte existe</h2>
	 *
	 * <p>Le vocabulaire connait « cacahuète » et ignore « cacahuete ». Un joueur
	 * qui baptise son dragon sans mettre l'accent — c'est-a-dire la moitie des
	 * joueurs — se retrouverait avec un compagnon qui ne repond pas a son nom,
	 * alors que le mot est parfaitement francais et parfaitement prononcable.
	 *
	 * <p>On range donc les 36 892 mots accentues du modele sous leur forme nue, et
	 * on rend l'accent au moment de fabriquer la liste. A l'oreille, la difference
	 * n'existe pas de toute facon : {@link Vocabulaire#pourLaComparaison} retire
	 * les accents des deux cotes.
	 *
	 * <p>Seuls les mots reellement accentues y figurent — un tiers du vocabulaire.
	 * Les autres se trouvent deja dans {@link #mots}, et les indexer deux fois
	 * couterait de la memoire pour rien.
	 *
	 * <p>En cas d'homographes ({@code cote}, {@code côte}, {@code coté}), on garde
	 * le premier venu. Il y a 1 446 cas dans le modele, et se tromper d'accent sur
	 * un nom de compagnon ne change rien a l'oreille.
	 */
	private final Map<String, String> parFormeNue;

	private Lexique(Set<String> mots) {
		this.mots = mots;

		Map<String, String> index = new HashMap<>();
		for (String mot : mots) {
			String nu = sansAccents(mot);
			if (!nu.equals(mot)) {
				index.putIfAbsent(nu, mot);
			}
		}
		this.parFormeNue = Collections.unmodifiableMap(index);
	}

	private static String sansAccents(String mot) {
		return Normalizer.normalize(mot, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
	}

	/**
	 * Le vocabulaire du modele installe, ou un lexique vide si on n'a pas su le lire.
	 *
	 * <p>Un lexique vide ne bloque rien : il fait simplement taire les
	 * avertissements. Mieux vaut une verification absente qu'un mod qui refuse de
	 * demarrer parce qu'un modele a change de format.
	 */
	/** Combien de variantes on retient au plus par nom. */
	private static final int VARIANTES_MAXIMUM = 6;

	/** En dessous de cette longueur, un nom ressemble a trop de choses. */
	private static final int LONGUEUR_MINIMALE = 4;

	/** Il faut au moins tant de lettres communes pour que ce soit le meme mot. */
	private static final int RACINE_MINIMALE = 5;

	/** Et pas plus de tant de lettres d'ecart : dragon / dragonne, pas dragonnades. */
	private static final int ECART_MAXIMUM = 2;

	/** L'apostrophe : « d'aragon » n'est pas un nom qu'on crie dans une cour. */
	private static final char APOSTROPHE = (char) 39;

	/** Les variantes deja calculees. Un nom de compagnon ne change presque jamais. */
	private final Map<String, List<String>> variantesRetenues = new ConcurrentHashMap<>();

	public static synchronized Lexique lire() {
		if (cherche) {
			return retenu;
		}
		cherche = true;

		try {
			Path dossier = ModeleVosk.dossier();
			if (dossier == null) {
				retenu = new Lexique(Set.of());
				return retenu;
			}
			Path grammaire = dossier.resolve("graph").resolve("Gr.fst");
			try (InputStream flux = new BufferedInputStream(Files.newInputStream(grammaire), 1 << 16)) {
				retenu = new Lexique(lireLesSymboles(flux));
				Compagnon.LOG.info("Vocabulaire du modele vocal : {} mots connus.", retenu.mots.size());
			}
		} catch (Throwable echec) {
			// Pas une erreur : la reconnaissance marche tres bien sans cette
			// verification, elle previent simplement moins bien.
			Compagnon.LOG.warn("Vocabulaire du modele vocal illisible ({}) : les noms de compagnons "
					+ "ne seront pas verifies a l'avance.", echec.toString());
			retenu = new Lexique(Set.of());
		}
		return retenu;
	}

	/**
	 * Le lexique s'il est deja lu, {@code null} sinon. <b>N'attend jamais.</b>
	 *
	 * <p>A utiliser partout ou l'on est sur le fil principal du serveur : la
	 * lecture du vocabulaire prend un instant au demarrage, et faire patienter le
	 * monde entier pour affiner un filtre de noms serait un mauvais echange.
	 */
	public static Lexique dejaLu() {
		return retenu;
	}

	/** Pour repartir de zero si le modele est reinstalle. */
	public static synchronized void oublier() {
		retenu = null;
		cherche = false;
	}

	/** Vrai si aucun vocabulaire n'a pu etre lu : on ne pretend alors rien savoir. */
	public boolean vide() {
		return this.mots.isEmpty();
	}

	public int taille() {
		return this.mots.size();
	}

	/**
	 * Le moteur saurait-il entendre ce mot ?
	 *
	 * <p>La comparaison est faite telle quelle, sans indulgence : c'est exactement
	 * ce que fait le moteur. « Cacahuete » avec une majuscule est pour lui un mot
	 * different de « cacahuete », d'ou le passage par
	 * {@link Vocabulaire#pourLaGrammaire} en amont.
	 *
	 * <p>Un lexique vide repond {@code true} a tout : sans information, on
	 * n'ecarte rien.
	 */
	public boolean connait(String mot) {
		return this.mots.isEmpty() || this.mots.contains(mot);
	}

	/**
	 * La forme d'un mot que le moteur saura entendre, ou {@code null}.
	 *
	 * <p>Le mot tel quel s'il le connait ; sinon sa version accentuee, s'il en
	 * existe une — voir {@link #parFormeNue}. Sans lexique, on rend le mot tel
	 * quel : sans information, on n'ecarte rien.
	 */
	public String formeEntendable(String mot) {
		if (this.mots.isEmpty() || this.mots.contains(mot)) {
			return mot;
		}
		return this.parFormeNue.get(sansAccents(mot));
	}

	/**
	 * Un nom entier est-il prononcable ? Tous ses mots doivent l'etre.
	 *
	 * <p>« Petit Dragon » demande que « petit » et « dragon » soient connus tous
	 * les deux : dans la liste, un nom en deux mots devient deux mots.
	 */
	public boolean sePrononce(String nom) {
		return !motsEntendables(nom).isEmpty();
	}

	/**
	 * Les mots d'un nom, sous la forme que le moteur sait entendre — ou une liste
	 * vide si un seul d'entre eux lui echappe.
	 *
	 * <p>Tout ou rien : un nom a moitie dans la liste ne serait jamais reconnu en
	 * entier, et ses morceaux isoles se declencheraient a tort.
	 */
	public List<String> motsEntendables(String nom) {
		String propre = Vocabulaire.pourLaGrammaire(nom);
		if (propre.isEmpty()) {
			return List.of();
		}
		List<String> entendables = new ArrayList<>();
		for (String mot : propre.split(" ")) {
			String forme = formeEntendable(mot);
			if (forme == null) {
				return List.of();
			}
			entendables.add(forme);
		}
		return entendables;
	}

	/** Les mots d'un nom que le moteur ignore, dans l'ordre et sans repetition. */
	public List<String> motsInconnus(String nom) {
		String propre = Vocabulaire.pourLaGrammaire(nom);
		if (this.mots.isEmpty() || propre.isEmpty()) {
			return List.of();
		}
		List<String> inconnus = new ArrayList<>();
		for (String mot : propre.split(" ")) {
			if (!mot.isEmpty() && !this.mots.contains(mot) && !inconnus.contains(mot)) {
				inconnus.add(mot);
			}
		}
		return inconnus;
	}

	/**
	 * Les mots du dictionnaire qui <b>sonnent comme</b> ce nom-la.
	 *
	 * <h2>Pourquoi un nom a besoin de variantes</h2>
	 *
	 * <p>Le nom est le mot le plus fragile d'une phrase : un mot rare, souvent
	 * invente, au milieu de mots courants. Le moteur qui hesite entre « dragon »
	 * et « dragons » choisit l'un des deux — et si ce n'est pas celui qu'on
	 * guettait, l'ordre est perdu alors que le joueur a parfaitement articule.
	 *
	 * <p>On met donc les deux dans la liste, et les quelques autres qui s'en
	 * approchent. Le moteur peut alors se tromper de forme sans qu'on perde le
	 * destinataire.
	 *
	 * <h2>Uniquement de vrais mots</h2>
	 *
	 * <p>On ne fabrique rien. Un mot absent du dictionnaire ne peut de toute
	 * facon <b>jamais</b> sortir du moteur : inventer « dragonneau » ne servirait
	 * a rien, puisqu'il ne serait jamais entendu. On ne retient donc que ce que
	 * le modele connait vraiment.
	 *
	 * <h2>Le cout</h2>
	 *
	 * <p>Parcourir 135 000 mots coute trop cher pour le faire a chaque seconde.
	 * D'ou deux precautions : un filtre grossier avant le calcul cher — meme
	 * premiere lettre, longueur voisine — et le resultat garde une fois pour
	 * toutes. Un nom de compagnon ne change presque jamais.
	 */
	public List<String> variantes(String mot) {
		if (this.mots.isEmpty() || mot == null) {
			return List.of();
		}
		String propre = Vocabulaire.pourLaGrammaire(mot);
		if (propre.length() < LONGUEUR_MINIMALE || propre.contains(" ")) {
			// Trop court : tout lui ressemblerait, et on ferait repondre le
			// compagnon a la moitie de la langue francaise.
			return List.of();
		}
		List<String> deja = variantesRetenues.get(propre);
		if (deja != null) {
			return deja;
		}
		List<String> trouvees = chercherLesVariantes(propre);
		variantesRetenues.put(propre, trouvees);
		return trouvees;
	}

	/**
	 * La regle : meme racine, ou deux lettres voisines inversees.
	 *
	 * <h2>Pourquoi pas une simple distance d'edition</h2>
	 *
	 * <p>Elle a ete essayee, et elle est <b>dangereuse</b>. A deux corrections
	 * pres, « Braise » ramenait « baiser » ; « Mouette » ramenait « maquette »
	 * et « mazette ». Ces mots ne se ressemblent pas a l'oreille, et les mettre
	 * dans la liste aurait fait obeir le compagnon a des phrases sans rapport —
	 * tout en genant la reconnaissance de son vrai nom.
	 *
	 * <h2>Ce qu'on garde</h2>
	 *
	 * <p><b>La meme racine.</b> C'est le cas de loin le plus frequent : le moteur
	 * rend le pluriel, le feminin, l'infinitif. « dragon » ramene « dragons » et
	 * « dragonne » ; « sabre » ramene « sabres », « sabrer », « sabreur ».
	 *
	 * <p><b>Deux lettres voisines inversees.</b> C'est la faute de frappe au
	 * bapteme, et elle est courante : un compagnon nomme « Dargon » retrouve
	 * ainsi « dragon », le seul mot que son proprietaire prononce vraiment.
	 *
	 * <p>Les mots a apostrophe sont ecartes : « d'aragon » n'est pas un nom
	 * qu'on crie dans une cour.
	 */
	private List<String> chercherLesVariantes(String propre) {
		String nu = Vocabulaire.pourLaComparaison(propre);
		List<String> trouvees = new ArrayList<>();

		for (String candidat : this.mots) {
			if (trouvees.size() >= VARIANTES_MAXIMUM) {
				break;
			}
			if (candidat.length() < 3 || candidat.indexOf(APOSTROPHE) >= 0
					|| !Character.isLetter(candidat.charAt(0))) {
				continue;
			}
			String candidatNu = Vocabulaire.pourLaComparaison(candidat);
			if (candidatNu.isEmpty() || candidatNu.equals(nu)) {
				continue;
			}
			if (memeRacine(nu, candidatNu) || lettresInversees(nu, candidatNu)) {
				trouvees.add(candidat);
			}
		}
		return List.copyOf(trouvees);
	}

	/** L'un commence par l'autre, et il en reste assez pour que ce soit le meme mot. */
	static boolean memeRacine(String nom, String candidat) {
		int commun = Math.min(nom.length(), candidat.length());
		return commun >= RACINE_MINIMALE
				&& Math.abs(nom.length() - candidat.length()) <= ECART_MAXIMUM
				&& (candidat.startsWith(nom) || nom.startsWith(candidat));
	}

	/** Deux lettres cote a cote echangees, et rien d'autre : dargon / dragon. */
	static boolean lettresInversees(String nom, String candidat) {
		if (nom.length() != candidat.length() || nom.length() < RACINE_MINIMALE) {
			return false;
		}
		int premier = -1;
		int second = -1;
		for (int i = 0; i < nom.length(); i++) {
			if (nom.charAt(i) == candidat.charAt(i)) {
				continue;
			}
			if (premier < 0) {
				premier = i;
			} else if (second < 0) {
				second = i;
			} else {
				// Trois differences : ce n'est plus une inversion.
				return false;
			}
		}
		return second == premier + 1
				&& nom.charAt(premier) == candidat.charAt(second)
				&& nom.charAt(second) == candidat.charAt(premier);
	}

	/**
	 * Quelques noms proches, qui eux se prononcent.
	 *
	 * <p>La ressemblance se mesure sur le debut du mot. C'est grossier, mais cela
	 * suffit a ne pas laisser un joueur devant une impasse : il voit tout de suite
	 * le genre de nom qui marcherait.
	 */
	public List<String> propositions(String mot, int combien) {
		if (this.mots.isEmpty() || mot == null || mot.length() < 3 || combien <= 0) {
			return List.of();
		}
		String debut = mot.substring(0, Math.min(4, mot.length()));
		List<String> trouves = new ArrayList<>();
		for (int longueur = debut.length(); longueur >= 2 && trouves.size() < combien; longueur--) {
			String essai = debut.substring(0, longueur);
			for (String candidat : this.mots) {
				if (trouves.size() >= combien) {
					break;
				}
				// Les symboles techniques du modele (« <eps> », « [unk] », « !SIL »)
				// ne sont pas des mots : les proposer n'aurait aucun sens.
				if (candidat.startsWith(essai) && candidat.length() > 2
						&& !trouves.contains(candidat)
						&& Character.isLetter(candidat.charAt(0))) {
					trouves.add(candidat);
				}
			}
		}
		Collections.sort(trouves);
		return trouves;
	}

	// --- Lecture du format OpenFst ------------------------------------------------

	/**
	 * Extrait la table des symboles d'entree d'un automate OpenFst.
	 *
	 * <p>Disposition, relevee sur le fichier reel : signature, type d'automate,
	 * type d'arc, version, drapeaux, puis quatre entiers longs decrivant
	 * l'automate, puis la table. Tous les entiers sont en petit-boutien.
	 */
	static Set<String> lireLesSymboles(InputStream flux) throws IOException {
		if (entier(flux) != SIGNATURE_FST) {
			throw new IOException("ce fichier n'est pas un automate OpenFst");
		}
		chaine(flux);                 // type d'automate (« ngram »)
		chaine(flux);                 // type d'arc (« standard »)
		entier(flux);                 // version
		int drapeaux = entier(flux);
		sauter(flux, 8 + 8 + 8 + 8);  // proprietes, etat initial, nb d'etats, nb d'arcs

		if ((drapeaux & DRAPEAU_SYMBOLES) == 0) {
			throw new IOException("cet automate ne porte pas sa table de symboles");
		}
		if (entier(flux) != SIGNATURE_TABLE) {
			throw new IOException("table de symboles introuvable a l'endroit attendu");
		}
		chaine(flux);                 // nom de la table
		sauter(flux, 8);              // prochaine cle libre

		long combien = entierLong(flux);
		if (combien < 0 || combien > SYMBOLES_MAXIMUM) {
			throw new IOException("nombre de mots aberrant : " + combien);
		}
		Set<String> mots = new HashSet<>(Math.max(16, (int) (combien / 0.75f) + 1));
		for (long i = 0; i < combien; i++) {
			mots.add(chaine(flux));
			sauter(flux, 8);          // identifiant du mot, dont on n'a pas l'usage
		}
		return Collections.unmodifiableSet(mots);
	}

	private static int entier(InputStream flux) throws IOException {
		return octet(flux) | (octet(flux) << 8) | (octet(flux) << 16) | (octet(flux) << 24);
	}

	private static long entierLong(InputStream flux) throws IOException {
		long bas = entier(flux) & 0xFFFFFFFFL;
		long haut = entier(flux) & 0xFFFFFFFFL;
		return bas | (haut << 32);
	}

	private static String chaine(InputStream flux) throws IOException {
		int longueur = entier(flux);
		if (longueur < 0 || longueur > MOT_MAXIMUM) {
			throw new IOException("longueur de mot aberrante : " + longueur);
		}
		byte[] octets = flux.readNBytes(longueur);
		if (octets.length != longueur) {
			throw new EOFException("fichier tronque");
		}
		// Le modele est ecrit en UTF-8 : le lire autrement transformerait
		// « sorciere » en un mot que le moteur ne reconnaitrait pas, et notre
		// avertissement accuserait a tort un nom parfaitement valide.
		return new String(octets, StandardCharsets.UTF_8);
	}

	private static int octet(InputStream flux) throws IOException {
		int valeur = flux.read();
		if (valeur < 0) {
			throw new EOFException("fichier tronque");
		}
		return valeur;
	}

	private static void sauter(InputStream flux, long combien) throws IOException {
		long reste = combien;
		while (reste > 0) {
			long saute = flux.skip(reste);
			if (saute <= 0) {
				if (flux.read() < 0) {
					throw new EOFException("fichier tronque");
				}
				reste--;
			} else {
				reste -= saute;
			}
		}
	}
}
