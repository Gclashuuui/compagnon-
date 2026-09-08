package fr.lhdp.compagnon.voix;

import fr.lhdp.compagnon.Compagnon;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/**
 * Le moteur qui transforme du son en mots, pour un joueur qui parle.
 *
 * <p>Tout se passe sur le serveur, hors ligne : la voix des joueurs ne part vers
 * aucun service exterieur, et le serveur peut tourner sans connexion.
 *
 * <h2>Liste fermee, et pourquoi c'est le bon choix ici</h2>
 *
 * <p>On ne demande pas au moteur d'ecrire ce qu'il entend. On lui donne la liste
 * exacte des mots possibles — les quelques ordres, plus les noms des compagnons
 * de ce joueur — et il ne peut repondre que par l'un d'eux, ou par « aucun ».
 *
 * <p>Une liste fermee a un defaut connu : elle repond le mot attendu des qu'un
 * son s'en approche. C'est redhibitoire pour un mot de passe. <b>Ici, non</b> :
 * un « assis » entendu a tort fait asseoir un dragon. On prefere largement un
 * compagnon un peu trop obeissant a un compagnon sourd.
 *
 * <h2>Ce qu'il ne faut jamais refaire</h2>
 *
 * <ul>
 *   <li><b>{@code setGrammar}</b> tue la machine virtuelle par acces memoire
 *       invalide. Quand la liste change, on jette l'objet et on en refait un.</li>
 *   <li><b>Attraper {@code Exception}</b> ne suffit pas : une faute du cote natif
 *       arrive en {@code Error}, traverse le filet et abat le serveur.</li>
 *   <li><b>Oublier {@code synchronized} sur {@link #fermer()}</b> laisse un fil
 *       liberer la memoire pendant qu'un autre s'en sert. Le plus souvent rien de
 *       visible ; parfois une corruption qui rend la voix muette <b>pour tout le
 *       serveur</b> jusqu'au redemarrage.</li>
 *   <li><b>Se servir du resultat partiel.</b> Sur une liste courte, le moteur
 *       propose volontiers un mot avant de se raviser : un raclement de gorge
 *       ferait asseoir le compagnon. Seule la decision finale fait foi.</li>
 * </ul>
 */
public final class Reconnaisseur implements AutoCloseable {

	/** Frequence de travail des modeles Vosk. Fixe : c'est une propriete du modele. */
	public static final int FREQUENCE_MODELE = 16_000;

	/**
	 * Frequence supposee quand Plasmo Voice ne veut pas repondre. C'est sa valeur
	 * par defaut.
	 *
	 * <p>Ne jamais la coder en dur a la place de la vraie : {@code sampleRate()}
	 * est un <b>reglage du serveur</b>. A 24 kHz, un son de deux secondes ramene
	 * comme s'il en faisait 48 devient un son d'une seconde joue au double de la
	 * vitesse. Le moteur n'y comprend alors plus rien — pas « moins bien » : plus
	 * rien du tout, pour tout le monde, sans le moindre message d'erreur.
	 */
	public static final int FREQUENCE_DE_SECOURS = 48_000;

	/** Le jeton « aucun des mots attendus ». C'est un silence, pas un essai. */
	public static final String INCONNU = "[unk]";

	private static Model modelePartage;
	private static boolean modeleCherche;

	private final Recognizer moteur;

	/** La liste chargee, pour ne refaire l'objet que si elle a change. */
	private final List<String> liste;

	/** La frequence annoncee par Plasmo pour cette session. */
	private final int frequenceSource;

	/**
	 * Vrai des que la memoire native a ete rendue. Voir la note de classe : c'est
	 * le garde qui evite qu'un fil se serve d'un moteur qu'un autre vient de
	 * liberer.
	 */
	private boolean ferme;

	private Reconnaisseur(Recognizer moteur, List<String> liste, int frequenceSource) {
		this.moteur = moteur;
		this.liste = liste;
		this.frequenceSource = frequenceSource;
	}

	/**
	 * Charge le modele une seule fois pour tout le serveur.
	 *
	 * <p>Vosk permet de partager un modele entre plusieurs moteurs, et c'est
	 * indispensable : chaque copie couterait ~70 Mo. Avec mille joueurs, en faire
	 * une par parleur mettrait le serveur a genoux en quelques secondes.
	 */
	private static synchronized Model modele() {
		if (modeleCherche) {
			return modelePartage;
		}
		modeleCherche = true;

		Path dossier = ModeleVosk.dossier();
		if (dossier == null) {
			return null;
		}
		try {
			// Vosk est tres bavard : on le fait taire pour ne pas noyer le log.
			LibVosk.setLogLevel(LogLevel.WARNINGS);
			modelePartage = new Model(dossier.toString());
			Compagnon.LOG.info("Moteur vocal pret.");
		} catch (Exception | UnsatisfiedLinkError echec) {
			Compagnon.LOG.error("Le moteur vocal n'a pas pu demarrer ({}). Les ordres a la voix sont "
					+ "indisponibles ; tout le reste du mod fonctionne.", echec.toString());
			modelePartage = null;
		}
		return modelePartage;
	}

	/** Vrai si la voix peut fonctionner du tout. */
	public static boolean disponible() {
		return modele() != null;
	}

	/**
	 * Un moteur pour cette liste de mots, ou {@code null} si la voix est indisponible.
	 *
	 * @param frequenceSource la frequence reelle de Plasmo Voice, pas une supposition
	 */
	public static Reconnaisseur creer(List<String> liste, int frequenceSource) {
		Model modele = modele();
		if (modele == null || liste.isEmpty()) {
			return null;
		}
		int frequence = frequenceSource >= FREQUENCE_MODELE ? frequenceSource : FREQUENCE_DE_SECOURS;
		try {
			Recognizer moteur = new Recognizer(modele, FREQUENCE_MODELE, grammaire(liste));
			// Sans ceci, la reponse ne porte que le texte : impossible de distinguer
			// un mot entendu d'un mot devine.
			moteur.setWords(true);
			return new Reconnaisseur(moteur, List.copyOf(liste), frequence);
		} catch (Exception | UnsatisfiedLinkError echec) {
			Compagnon.LOG.error("Moteur vocal non cree : {}", echec.toString());
			return null;
		}
	}

	/** Vrai si ce moteur ecoute deja exactement ces mots-la. */
	public boolean ecouteDeja(List<String> candidate) {
		return this.liste.equals(candidate);
	}

	/**
	 * La liste au format attendu : un tableau JSON de mots.
	 *
	 * <p>Le jeton {@link #INCONNU} est essentiel. Sans lui, le moteur est oblige
	 * de choisir un mot de la liste et le compagnon obeirait a n'importe quelle
	 * conversation. Avec lui, il a le droit de dire « ce n'est aucun de ces
	 * mots-la » — ce qui est le cas le plus frequent, et de loin.
	 */
	static String grammaire(List<String> liste) {
		StringBuilder json = new StringBuilder("[");
		for (String mot : liste) {
			json.append('"').append(mot.replace("\\", "\\\\").replace("\"", "\\\"")).append("\", ");
		}
		return json.append('"').append(INCONNU).append("\"]").toString();
	}

	/**
	 * Avale un fragment de son et rend la phrase quand le moteur juge qu'elle est
	 * finie, sinon {@code null}.
	 */
	public synchronized String avaler(short[] son) {
		if (this.ferme) {
			return null;
		}
		short[] ramene = ramenerA16k(son, this.frequenceSource);
		try {
			if (this.moteur.acceptWaveForm(ramene, ramene.length)) {
				return texte(this.moteur.getResult());
			}
		} catch (Throwable echec) {
			// Throwable et non Exception : une faute native arrive en Error, qui
			// traverserait un filet ordinaire et abattrait le serveur. Aucun defaut
			// du moteur vocal ne doit pouvoir faire tomber le jeu.
			Compagnon.LOG.debug("Fragment audio ignore : {}", echec.toString());
		}
		return null;
	}

	/** Conclut la phrase quand le joueur s'est tu, pour ne pas perdre le dernier mot. */
	public synchronized String conclure() {
		if (this.ferme) {
			return null;
		}
		try {
			String phrase = texte(this.moteur.getFinalResult());
			this.moteur.reset();
			return phrase;
		} catch (Throwable echec) {
			return null;
		}
	}

	/**
	 * Ramene un son a 16 kHz depuis n'importe quelle frequence, en moyennant
	 * chaque tranche de source qui correspond a un echantillon de sortie.
	 *
	 * <p>La moyenne n'est pas une coquetterie : prendre un echantillon sur trois
	 * replierait les hautes frequences sur la voix et la rendrait meconnaissable.
	 * La moyenne fait office de filtre passe-bas, ce que reclame toute reduction
	 * de frequence.
	 *
	 * <p>Le calcul par tranches accepte les rapports non entiers — 44,1 kHz par
	 * exemple — la ou une division fixe n'accepterait que 48 kHz.
	 */
	static short[] ramenerA16k(short[] source, int frequenceSource) {
		if (source.length == 0 || frequenceSource <= FREQUENCE_MODELE) {
			// Deja a la bonne frequence, ou plus basse : rien de bon a tenter en
			// etirant le son.
			return source;
		}
		int longueur = (int) ((long) source.length * FREQUENCE_MODELE / frequenceSource);
		if (longueur == 0) {
			return new short[0];
		}
		short[] sortie = new short[longueur];
		for (int i = 0; i < longueur; i++) {
			int debut = (int) ((long) i * frequenceSource / FREQUENCE_MODELE);
			int fin = (int) ((long) (i + 1) * frequenceSource / FREQUENCE_MODELE);
			if (fin > source.length) {
				fin = source.length;
			}
			if (fin <= debut) {
				fin = debut + 1;
			}
			long somme = 0;
			for (int k = debut; k < fin; k++) {
				somme += source[k];
			}
			sortie[i] = (short) (somme / (fin - debut));
		}
		return sortie;
	}

	/**
	 * Le texte de la reponse, ou {@code null} s'il n'y a rien a en tirer.
	 *
	 * <p>Vosk repond en JSON, toujours de la meme forme
	 * ({@code {"text" : "cacahuete assis"}}) : une bibliotheque JSON complete
	 * serait disproportionnee pour un unique champ.
	 */
	static String texte(String json) {
		if (json == null) {
			return null;
		}
		int cle = json.indexOf("\"text\"");
		if (cle < 0) {
			return null;
		}
		int ouvre = json.indexOf('"', json.indexOf(':', cle) + 1);
		if (ouvre < 0) {
			return null;
		}
		int ferme = json.indexOf('"', ouvre + 1);
		if (ferme < 0) {
			return null;
		}
		String brut = reparerLesAccents(json.substring(ouvre + 1, ferme)).strip();

		// « [unk] » est la reponse « aucun de ces mots ». La rendre comme une
		// phrase ferait chercher un compagnon nomme « unk » cinquante fois par
		// seconde. C'est un silence.
		String sansInconnu = brut.replace(INCONNU, " ").strip().replaceAll("\\s+", " ");
		return sansInconnu.isEmpty() ? null : sansInconnu;
	}

	/**
	 * Repare les accents abimes en chemin depuis le moteur.
	 *
	 * <p><b>Le defaut, vu en jeu :</b> « j'Ã©tais marchÃ© » — le moteur avait bien
	 * compris « j'étais » et « marché ».
	 *
	 * <p>Le moteur ecrit ses reponses en UTF-8. La passerelle entre le code natif
	 * et Java les relit avec l'encodage par defaut de la machine, qui n'est pas
	 * forcement celui-la : chaque accent ressort alors en deux caracteres
	 * parasites. Ce n'est pas cosmetique — un compagnon nomme « Bébé » ne
	 * repondrait plus a son nom.
	 *
	 * <p><b>Pourquoi la reparation est sans risque.</b> On refait le chemin a
	 * l'envers : reprendre les octets tels qu'ils ont ete lus, puis les relire en
	 * UTF-8 <i>strict</i>. Un texte sain ne survit pas a cette operation — le
	 * decodage strict la rejette — et on le rend alors intact. Ne sont repares que
	 * les textes qui sont vraiment de l'UTF-8 mal relu.
	 */
	static String reparerLesAccents(String texte) {
		if (texte == null || texte.isEmpty()) {
			return texte;
		}
		// Signature du defaut : un accent UTF-8 mal relu commence toujours par
		// l'un de ces deux caracteres. Sans eux, il n'y a rien a reparer.
		if (texte.indexOf('Ã') < 0 && texte.indexOf('Â') < 0) {
			return texte;
		}
		try {
			CharsetDecoder decodeur = StandardCharsets.UTF_8.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT);
			byte[] telsQueLus = texte.getBytes(StandardCharsets.ISO_8859_1);
			return decodeur.decode(ByteBuffer.wrap(telsQueLus)).toString();
		} catch (Exception pasDuTout) {
			// Ce n'etait pas de l'UTF-8 mal relu : on ne touche a rien.
			return texte;
		}
	}

	/**
	 * Rend la memoire native.
	 *
	 * <p>{@code synchronized} comme les methodes de travail, et pour la meme
	 * serrure : c'est ce qui garantit qu'aucun autre fil n'est en train de se
	 * servir du moteur au moment ou on le libere.
	 */
	public synchronized void fermer() {
		if (this.ferme) {
			return;
		}
		// Leve AVANT la liberation : meme si celle-ci echoue, plus personne ne doit
		// toucher a ce moteur — on ne sait pas dans quel etat il se trouve.
		this.ferme = true;
		try {
			this.moteur.close();
		} catch (Throwable audepart) {
			// Fermeture au depart d'un joueur : un echec ici ne doit jamais remonter.
		}
	}

	@Override
	public void close() {
		fermer();
	}
}
