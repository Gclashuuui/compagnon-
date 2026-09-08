package fr.lhdp.compagnon.voix;

import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.entite.CompagnonEntity;
import fr.lhdp.compagnon.espece.Espece;
import fr.lhdp.compagnon.espece.Especes;
import fr.lhdp.compagnon.fiche.FicheCompagnon;
import fr.lhdp.compagnon.fiche.Fiches;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Le branchement de la voix, et la seule porte entre le mod et Plasmo Voice.
 *
 * <p><b>Aucune classe du chemin principal ne nomme Plasmo Voice.</b> C'est ce qui
 * garantit que le mod tourne partout : sur un serveur sans chat vocal, il demarre
 * normalement et se contente de le dire dans le log. La seule classe qui la
 * reference — {@link OreilleCompagnon} — n'est chargee qu'apres avoir verifie que
 * l'API repond vraiment.
 *
 * <h2>La frontiere entre les deux fils</h2>
 *
 * <p>Le son arrive sur le fil reseau de Plasmo Voice. Le monde — les fiches, les
 * entites, les positions — appartient au fil principal du serveur. <b>Les deux ne
 * doivent jamais se croiser</b> : lire une entite depuis le fil audio, c'est lire
 * une structure pendant qu'un autre fil la modifie.
 *
 * <p>D'ou le <b>profil vocal</b> : le fil principal prepare, une fois par seconde
 * et pour les seuls joueurs qui parlent, la liste des mots a guetter. Le fil audio
 * ne fait que la lire. Il ne touche jamais au monde.
 */
public final class Voix {

	/** Vrai si l'oreille tourne pour de bon. */
	private static volatile boolean active;

	/** Pourquoi elle ne tourne pas, si elle ne tourne pas. */
	private static volatile String silence = "pas encore demarree";

	/** Age maximal d'un profil, en ticks. Au-dela, on le refait. */
	private static final int FRAICHEUR = 20;

	/**
	 * Combien de temps un profil survit au silence de son joueur, en ticks.
	 *
	 * <h2>Pourquoi c'etait quinze secondes, et pourquoi c'etait faux</h2>
	 *
	 * <p>Un profil oublie coute le <b>debut</b> de la phrase suivante. Le fil audio
	 * demande la liste des mots, ne la trouve pas, et le fil principal ne la
	 * prepare qu'au tick d'apres. Entre les deux, les premiers fragments etaient
	 * jetes — soit les cinquante premieres millisecondes de parole.
	 *
	 * <p>Cinquante millisecondes, c'est la duree du <b>d</b> de « dragon ». Le
	 * moteur recevait « ragon » et ne reconnaissait rien. Quiconque se taisait
	 * quinze secondes perdait donc son premier ordre, a chaque fois.
	 *
	 * <p>Deux corrections, et il faut les deux : le son est desormais <b>mis de
	 * cote</b> au lieu d'etre jete (voir {@code OreilleCompagnon.Attente}), et le
	 * profil vit assez longtemps pour que le cas ne se presente presque jamais.
	 * Deux minutes couvrent une conversation ; au-dela le joueur est parti faire
	 * autre chose, et un profil ne pese que quelques mots.
	 */
	private static final int OUBLI = 20 * 120;

	/** Un mot appris tient en tant de lettres. */
	private static final int MOT_APPRIS_LONGUEUR_MAX = 24;

	/** En dessous, le mot s'entend dans tout et n'importe quoi. */
	private static final int MOT_APPRIS_LONGUEUR_MIN = 3;

	// --- Le diagnostic ------------------------------------------------------------
	// Sans ces compteurs, une voix muette est un mystere complet : on ne sait meme
	// pas si le son arrive jusqu'a nous. Ils ont coute assez cher a l'autre mod
	// pour qu'on les mette ici des le premier jour.

	public static final AtomicLong fragmentsRecus = new AtomicLong();
	public static final AtomicLong fragmentsParUdp = new AtomicLong();
	public static final AtomicLong sonIllisible = new AtomicLong();
	public static final AtomicLong phrasesEntendues = new AtomicLong();
	public static final AtomicLong ordresCompris = new AtomicLong();
	public static final AtomicInteger ecoutes = new AtomicInteger();

	/** La derniere phrase entendue, et par qui : de loin le plus utile en jeu. */
	private static volatile String derniereEntendue = "";

	/** La frequence annoncee par Plasmo, ou 0 si elle n'a pas voulu repondre. */
	private static volatile int frequence;

	/** Le dernier incident du cote du son, pour ne pas chercher dans le log. */
	private static volatile String dernierIncident = "";

	/** Les profils prets, lus par le fil audio, ecrits par le fil principal. */
	private static final Map<UUID, Profil> profils = new ConcurrentHashMap<>();

	/** Les joueurs dont le fil audio reclame un profil. */
	private static final Set<UUID> aPreparer = ConcurrentHashMap.newKeySet();

	/** La voix ordinaire de chaque joueur, pour reconnaitre quand il la hausse. */
	private static final Map<UUID, Habitude> tons = new ConcurrentHashMap<>();

	/**
	 * La voix ordinaire d'un joueur.
	 *
	 * @param moyenne la force moyenne de ses phrases
	 * @param phrases combien ont servi a l'etablir, plafonne
	 */
	private record Habitude(double moyenne, int phrases) {
	}

	/** En dessous, ce n'est pas une voix : du souffle, une chaise qui bouge. */
	private static final double PLANCHER_AUDIBLE = 300.0D;

	/** Combien de fois plus fort que d'habitude fait un cri. */
	private static final double CE_QUI_FAIT_UN_CRI = 1.8D;

	/** En dessous de cette part de sa voix ordinaire, il chuchote. */
	private static final double CE_QUI_FAIT_UN_CHUCHOTEMENT = 0.55D;

	/** Avant d'avoir entendu tant de phrases, on ne juge pas. */
	private static final int PHRASES_POUR_JUGER = 4;

	/** A quel point la moyenne suit la derniere phrase. Doucement. */
	private static final double SOUPLESSE = 0.25D;

	private Voix() {
	}

	/**
	 * La liste des mots a guetter pour un joueur, a un instant donne.
	 *
	 * @param mots      les mots, deja mis en forme pour le moteur
	 * @param prepareA  le tick ou il a ete fait
	 */
	private record Profil(List<String> mots, int prepareA) {
	}

	/**
	 * Branche la reconnaissance <b>si et seulement si</b> Plasmo Voice est la.
	 *
	 * <p>Ne leve jamais rien. Une incompatibilite de version se manifeste par une
	 * {@code LinkageError}, pas par une exception : elle ne doit pas empecher le
	 * serveur de demarrer, elle doit juste eteindre la voix.
	 */
	public static void enregistrer() {
		ServerTickEvents.END_SERVER_TICK.register(Voix::tick);

		// LE DEPART D'UN JOUEUR EFFACE TOUT CE QU'ON GARDAIT DE LUI.
		//
		// Il y avait deja un nettoyage, mais accroche a l'evenement de deconnexion
		// de Plasmo Voice — qui ne se declenche pas toujours : un joueur qui plante
		// ou qui perd sa connexion ne le declenche pas, et un joueur qui n'a jamais
		// parle n'a jamais eu de session vocale du tout.
		//
		// Celui-ci se declenche toujours. Une entree oubliee ne pese que quelques
		// dizaines d'octets, mais sur un serveur qui tourne des mois avec mille
		// joueurs, ce qui ne s'efface jamais finit par peser.
		ServerPlayConnectionEvents.DISCONNECT.register((gestionnaire, serveur) ->
				oublier(gestionnaire.getPlayer().getUUID()));

		if (!plasmoPresent()) {
			silence = "Plasmo Voice absent";
			Compagnon.LOG.info("Plasmo Voice absent : les compagnons n'obeissent pas a la voix. "
					+ "Tout le reste du mod fonctionne normalement.");
			return;
		}
		try {
			// Chargement tardif volontaire : cette ligne est la seule qui nomme la
			// classe de l'oreille, et cette classe nomme Plasmo. La citer plus tot
			// ferait echouer le chargement sur un serveur qui n'a pas le chat vocal.
			su.plo.voice.api.server.PlasmoVoiceServer.getAddonsLoader()
					.load(new OreilleCompagnon());
			active = true;
			silence = "";
			prechauffer();
		} catch (Throwable echec) {
			silence = "Plasmo Voice incompatible : " + echec;
			Compagnon.LOG.error("Plasmo Voice detecte mais incompatible ({}). Les ordres a la voix "
					+ "sont desactives ; tout le reste du mod fonctionne.", echec.toString());
		}
	}

	/**
	 * Charge le modele et son vocabulaire pendant que personne ne parle encore.
	 *
	 * <p>Le modele pese 68 Mo, et la premiere fois il faut peut-etre l'extraire
	 * d'une archive. Le laisser arriver tout seul le ferait tomber sur le premier
	 * joueur qui parle — donc sur le fil reseau de Plasmo, qu'on bloquerait
	 * plusieurs secondes pour tout le chat vocal du serveur.
	 *
	 * <p>Sur un fil a part, et demon : si le serveur s'arrete pendant, il s'arrete.
	 */
	private static void prechauffer() {
		Thread fil = new Thread(() -> {
			if (!Reconnaisseur.disponible()) {
				silence = "modele vocal indisponible (" + ModeleVosk.raisonDeLAbsence() + ")";
				active = false;
				return;
			}
			verifierLesOrdres(Lexique.lire());
		}, "compagnon-voix-prechauffage");
		fil.setDaemon(true);
		fil.start();
	}

	/**
	 * Verifie que chaque mot d'ordre existe vraiment dans le vocabulaire.
	 *
	 * <p>C'est la seule chose qui rende visible le piege decrit dans
	 * {@link Lexique} : un mot inconnu du francais est retire de la liste
	 * <b>en silence</b>. Quelqu'un qui ajoute « couché-toi » ou « aporte » dans le
	 * fichier verrait simplement l'ordre ne jamais marcher, sans savoir pourquoi.
	 * Ici, il trouve une ligne dans le log qui nomme le mot fautif.
	 */
	private static void verifierLesOrdres(Lexique lexique) {
		if (lexique == null || lexique.vide()) {
			return;
		}
		List<String> introuvables = new ArrayList<>();
		for (String mot : Vocabulaire.tousLesMots()) {
			if (!lexique.connait(mot)) {
				introuvables.add(mot);
			}
		}
		if (!introuvables.isEmpty()) {
			Compagnon.LOG.warn("Mots d'ordre absents du vocabulaire francais, donc jamais entendus : "
					+ "{}. Corrigez-les dans data/compagnon/{}.", introuvables, Vocabulaire.FICHIER);
		}
	}

	public static boolean plasmoPresent() {
		try {
			Class.forName("su.plo.voice.api.server.PlasmoVoiceServer");
			return true;
		} catch (ClassNotFoundException | LinkageError absent) {
			return false;
		}
	}

	// --- Le profil vocal ----------------------------------------------------------

	/**
	 * Les mots a guetter pour ce joueur, ou {@code null} si le profil n'est pas
	 * encore pret.
	 *
	 * <p>Appelable depuis le fil audio : cette methode ne touche a rien d'autre
	 * qu'une carte concurrente.
	 *
	 * <p>Un {@code null} coute un fragment de son — vingt millisecondes, que
	 * personne n'entend passer, et le moteur en demande de toute facon plusieurs
	 * avant de conclure quoi que ce soit.
	 */
	public static List<String> motsPour(UUID joueur) {
		aPreparer.add(joueur);
		Profil profil = profils.get(joueur);
		return profil == null ? null : profil.mots();
	}

	/** Le fil principal prepare ce que le fil audio a reclame. */
	private static void tick(MinecraftServer serveur) {
		if (aPreparer.isEmpty() && profils.isEmpty()) {
			return;
		}
		int maintenant = serveur.getTickCount();

		for (UUID id : aPreparer) {
			Profil ancien = profils.get(id);
			if (ancien != null && maintenant - ancien.prepareA() < FRAICHEUR) {
				continue;
			}
			ServerPlayer joueur = serveur.getPlayerList().getPlayer(id);
			profils.put(id, new Profil(
					joueur == null ? List.of() : preparer(joueur), maintenant));
		}
		aPreparer.clear();

		// Les profils que plus personne ne reclame finissent par disparaitre : sinon
		// la carte grossirait a chaque joueur ayant parle depuis le demarrage.
		profils.entrySet().removeIf(entree -> maintenant - entree.getValue().prepareA() > OUBLI);
	}

	/**
	 * Les mots a guetter pour ce joueur — ou une liste vide, qui veut dire
	 * « n'allume pas le moteur pour lui ».
	 *
	 * <h2>Pourquoi la liste vide compte autant</h2>
	 *
	 * <p>Dans un chateau a mille joueurs, l'immense majorite de ceux qui parlent
	 * bavardent simplement entre eux. Allumer un moteur de reconnaissance pour
	 * chacun serait ruineux — et parfaitement inutile, puisqu'ils n'ont aucun
	 * compagnon a portee de voix. On rend donc une liste vide des qu'il n'y a
	 * personne pour entendre, et le cout retombe a zero dans le cas de loin le
	 * plus frequent.
	 *
	 * <h2>Et les noms que le moteur ne sait pas dire</h2>
	 *
	 * <p>Ils sont <b>omis</b> de la liste. Un nom absent du vocabulaire francais
	 * en serait retire en silence de toute facon, et son proprietaire ne saurait
	 * jamais pourquoi — d'ou l'avertissement au bapteme, voir
	 * {@link #prevenirSiInaudible} et {@link Lexique}.
	 *
	 * <p>Et si <b>aucun</b> nom ne survit, on rend une liste vide plutot que les
	 * seuls ordres : on s'adresse toujours a un compagnon par son nom, un ordre
	 * sans nom ne declenchera donc jamais rien. Allumer un moteur pour ecouter des
	 * mots qui ne peuvent rien faire serait payer pour du silence.
	 */
	private static List<String> preparer(ServerPlayer joueur) {
		Fiches fiches = Fiches.de(joueur.server);

		// TOUS les compagnons a portee qui connaissent ce joueur, et pas seulement
		// les siens. Sans cela, le moteur ne saurait meme pas ENTENDRE le nom de la
		// bete d'un ami : le mot ne serait pas dans sa liste, et l'ordre serait
		// perdu bien avant d'arriver jusqu'a nous.
		List<CompagnonEntity> autour = joueur.level().getEntitiesOfClass(
				CompagnonEntity.class,
				joueur.getBoundingBox().inflate(EcouteVocale.PORTEE),
				bete -> bete.ficheId() != null && bete.connait(joueur.getUUID()));

		if (autour.isEmpty()) {
			return List.of();
		}

		// dejaLu et non lire : on est sur le fil principal, et pendant le
		// prechauffage lire() ferait attendre le serveur entier. Tant qu'il n'est
		// pas la, on ne filtre pas — les mots des ordres sont dans le vocabulaire
		// francais de toute facon, la liste ne peut donc pas se vider.
		Lexique lexique = Lexique.dejaLu();
		List<String> mots = new ArrayList<>();

		// TRIE, ET CE TRI N'EST PAS COSMETIQUE.
		//
		// getEntitiesOfClass ne promet aucun ordre : deux appels a une seconde
		// d'intervalle peuvent rendre les memes betes rangees autrement. Or le fil
		// audio compare la nouvelle liste a l'ancienne avec equals, qui tient
		// compte de l'ordre — il croyait donc que la liste avait change, jetait le
		// moteur et en refaisait un. EN PLEINE PHRASE, avec tout le son deja avale.
		//
		// C'est la panne que le joueur vit comme « des fois il m'entend, des fois
		// non » : elle ne depend que de l'ordre dans lequel le monde a range ses
		// entites, donc de rien de reproductible.
		autour.sort(java.util.Comparator.comparing(bete -> String.valueOf(bete.ficheId())));

		for (CompagnonEntity bete : autour) {
			FicheCompagnon fiche = fiches.get(bete.ficheId());
			if (fiche == null) {
				continue;
			}
			// Le lexique rend au passage les accents que le joueur n'a pas tapes :
			// « Cacahuete » entre dans la liste comme « cacahuète », que le moteur
			// connait. A l'oreille c'est le meme mot.
			List<String> nom = lexique == null
					? List.of(Vocabulaire.pourLaGrammaire(fiche.nom()).split(" "))
					: lexique.motsEntendables(fiche.nom());
			for (String morceau : nom) {
				if (!morceau.isEmpty() && !mots.contains(morceau)) {
					mots.add(morceau);
				}
			}

			// PLUSIEURS FACONS D'ENTENDRE LE MEME NOM.
			//
			// Le moteur qui hesite entre « dragon » et « dragons » en choisit un. Si
			// ce n'est pas celui qu'on guettait, l'ordre est perdu — alors que le
			// joueur a parfaitement articule. On met donc les deux dans la liste, et
			// les quelques autres qui s'en approchent.
			//
			// Ce ne sont jamais des mots inventes : uniquement ce que le dictionnaire
			// contient deja. Un mot absent ne pourrait de toute facon jamais sortir
			// du moteur.
			if (lexique != null) {
				for (String variante : lexique.variantes(fiche.nom())) {
					if (!mots.contains(variante)) {
						mots.add(variante);
					}
				}
			}

			// SON ESPECE COMPTE AUSSI COMME UN NOM.
			//
			// « dragon », « mouette » : de vrais mots francais, que le moteur connait
			// toujours. Un compagnon baptise d'un nom invente reste donc appelable —
			// par ce qu'il est, a defaut de par son nom.
			// « dragonnet » n'existe pas dans le vocabulaire francais ; « dragon » si.
			// C'est la fiche d'espece qui dit par quel mot on l'appelle vraiment.
			Espece sonEspece = Especes.get(fiche.espece());
			String appel = Vocabulaire.pourLaGrammaire(
				sonEspece == null ? fiche.espece() : sonEspece.nomVocal());
			if (!appel.isEmpty() && !appel.contains(" ") && !mots.contains(appel)
					&& (lexique == null || lexique.connait(appel))) {
				mots.add(appel);
			}

			// LES MOTS QU'IL A APPRIS.
			//
			// Ceux-la sont deja verifies dans le dictionnaire au moment ou on les lui
			// apprend : inutile de recommencer, et de toute facon un mot refuse n'a
			// jamais pu entrer dans sa fiche.
			for (String mot : fiche.motsAppris().keySet()) {
				String propre = Vocabulaire.pourLaGrammaire(mot);
				if (!propre.isEmpty() && !mots.contains(propre)) {
					mots.add(propre);
				}
			}
		}

		// LES ORDRES PARTENT TOUJOURS, MEME SANS UN SEUL NOM PRONONCABLE.
		//
		// On rendait ici une liste vide quand aucun nom ne survivait au lexique,
		// avec ce raisonnement : « un ordre sans nom ne declenchera jamais rien ».
		//
		// CE N'EST PLUS VRAI. Depuis que le nom est facultatif quand il n'y a aucun
		// doute, un ordre seul suffit. Et l'ancien raisonnement avait une
		// consequence terrible : un compagnon baptise « Dargon » — un mot qui
		// n'existe pas en francais — rendait la voix ENTIEREMENT MUETTE pour son
		// proprietaire. Pas seulement son nom : tout. « assis », « viens ici »,
		// « chope » — plus rien n'etait entendu, parce qu'aucun moteur n'etait
		// meme allume.
		//
		// Le diagnostic le disait pourtant noir sur blanc, en une ligne :
		// « Mots guettes pour vous — rien ».
		for (String mot : Vocabulaire.tousLesMots()) {
			if (!mots.contains(mot)) {
				mots.add(mot);
			}
		}
		return List.copyOf(mots);
	}

	/**
	 * Previent gentiment quand un nom ne pourra pas etre appele a la voix.
	 *
	 * <p><b>Cela ne refuse jamais le nom.</b> Le nom appartient au joueur : s'il
	 * veut appeler son dragon « Zibou », c'est son droit, et tout le reste du mod
	 * marchera parfaitement. Il perd seulement la possibilite de l'appeler a voix
	 * haute — et il vaut mieux qu'il l'apprenne maintenant, en une phrase, que dans
	 * trois semaines apres avoir crie le nom de son dragon dans le vide.
	 *
	 * <p>Silencieux tant que rien ne permet d'affirmer quoi que ce soit : pas de
	 * chat vocal, pas de modele, vocabulaire pas encore lu. On ne previent que
	 * quand on sait.
	 */
	public static void prevenirSiInaudible(ServerPlayer joueur, String nom) {
		if (!active) {
			return;
		}
		Lexique lexique = Lexique.dejaLu();
		if (lexique == null || lexique.vide() || lexique.sePrononce(nom)) {
			return;
		}
		joueur.sendSystemMessage(Component.literal(
				"« " + nom + " » ne peut pas etre appele a la voix — ce nom n'existe pas dans le "
				+ "dictionnaire du micro. Tout le reste fonctionne normalement.")
				.withStyle(ChatFormatting.GRAY));

		List<String> proches = lexique.propositions(
				Vocabulaire.pourLaGrammaire(nom).split(" ")[0], 4);
		if (!proches.isEmpty()) {
			joueur.sendSystemMessage(Component.literal(
					"Des noms qui marcheraient : " + String.join(", ", proches))
					.withStyle(ChatFormatting.DARK_GRAY));
		}
	}

	/** Un compagnon de cette fiche est-il la, dans ce monde, a portee de voix ? */
	private static boolean aPortee(ServerPlayer joueur, FicheCompagnon fiche) {
		ServerLevel niveau = joueur.server.getLevel(fiche.dimension());
		if (niveau == null || niveau != joueur.level()) {
			return false;
		}
		Entity entite = niveau.getEntity(fiche.id());
		if (!(entite instanceof CompagnonEntity)) {
			return false;
		}
		double portee = EcouteVocale.PORTEE;
		return joueur.distanceToSqr(entite) <= portee * portee;
	}

	// --- Lecture ------------------------------------------------------------------

	public static boolean active() {
		return active;
	}

	public static String silence() {
		return silence;
	}

	/**
	 * Les mots que le moteur guette pour ce joueur, en clair.
	 *
	 * <p>C'est le renseignement qui manquait le plus. Quand un ordre n'est pas
	 * compris, la premiere question est : ce mot etait-il seulement dans la
	 * liste ? Sans reponse, on soupconne le micro, le reseau, le modele — et
	 * c'etait parfois simplement un compagnon hors de portee, donc un nom
	 * absent de la grammaire, donc une phrase sans destinataire possible.
	 */
	public static String motsGuettes(UUID joueur) {
		Profil profil = profils.get(joueur);
		if (profil == null) {
			return "aucun profil (il n'a pas encore parle)";
		}
		if (profil.mots().isEmpty()) {
			return "rien : aucun compagnon a portee ne le connait";
		}
		return String.join(" ", profil.mots());
	}

	public static String derniereEntendue() {
		return derniereEntendue;
	}

	public static void noterEntendue(String qui, String phrase) {
		derniereEntendue = qui + " : \"" + phrase + "\"";
	}

	public static int frequence() {
		return frequence;
	}

	public static void noterFrequence(int valeur) {
		frequence = valeur;
	}

	public static String dernierIncident() {
		return dernierIncident;
	}

	public static void noterIncident(String quoi) {
		dernierIncident = quoi;
	}

	/** Combien de profils sont en memoire — pour verifier qu'ils s'oublient bien. */
	public static int profilsEnMemoire() {
		return profils.size();
	}

	/** Un joueur s'en va : son profil n'a plus lieu d'etre. */
	/**
	 * Un mot, un seul, mis en forme pour le moteur.
	 *
	 * <p>On ne garde que le premier : une expression de plusieurs mots serait
	 * donnee au moteur mot par mot, et chacun d'eux declencherait le tour tout
	 * seul. « fais le beau » ferait donc obeir a « le ».
	 */
	public static String motSimple(String brut) {
		String propre = Vocabulaire.pourLaGrammaire(brut);
		if (propre.isEmpty()) {
			return "";
		}
		String premier = propre.split(" ")[0];
		return premier.length() > MOT_APPRIS_LONGUEUR_MAX
				? premier.substring(0, MOT_APPRIS_LONGUEUR_MAX)
				: premier;
	}

	/**
	 * Pourquoi ce mot ne peut pas etre appris, ou {@code null} s'il convient.
	 *
	 * <p>On rend une <b>phrase</b> et non un booleen : « ce mot ne va pas » ne
	 * dit pas quoi changer, et le joueur reessaierait au hasard.
	 */
	public static String pourquoiCeMotNeVaPas(ServerPlayer joueur, FicheCompagnon fiche,
			String mot) {

		if (mot.length() < MOT_APPRIS_LONGUEUR_MIN) {
			return "Trop court : un mot de deux lettres s'entend dans tout et n'importe quoi.";
		}

		// Un mot d'ordre du mod ne peut pas vouloir dire deux choses.
		for (String ordre : Vocabulaire.tousLesMots()) {
			if (ordre.equals(mot)) {
				return "« " + mot + " » est deja un ordre. Choisis un autre mot.";
			}
		}

		// Ni le nom d'une bete a portee : on ne veut pas qu'un tour reponde a un appel.
		if (mot.equalsIgnoreCase(Vocabulaire.pourLaGrammaire(fiche.nom()))) {
			return "C'est son nom. Il lui faut un autre mot pour ce tour.";
		}

		// LE TEST QUI COMPTE : le micro sait-il dire ce mot ?
		//
		// Sans lui, le mot serait retire de la liste du moteur EN SILENCE, et le
		// joueur crierait pendant des semaines un mot qui ne peut pas etre entendu.
		Lexique lexique = Lexique.dejaLu();
		if (lexique == null || lexique.vide()) {
			// Pas de dictionnaire lu : on ne peut rien affirmer, donc on accepte.
			return null;
		}
		if (lexique.sePrononce(mot)) {
			return null;
		}
		List<String> proches = lexique.propositions(mot, 4);
		return "Le micro ne connait pas « " + mot + " ». "
				+ (proches.isEmpty() ? "Essaie un mot plus courant."
						: "Des mots qui marcheraient : " + String.join(", ", proches) + ".");
	}

	/**
	 * A-t-il hausse le ton ?
	 *
	 * <h2>Relativement a lui, jamais dans l'absolu</h2>
	 *
	 * <p>C'est le point qui decide de tout. Un seuil absolu recompenserait ceux
	 * qui ont un bon micro, un preampli genereux, une voix qui porte — et
	 * priverait les autres d'une fonction du jeu pour une raison qui n'a rien a
	 * voir avec le jeu.
	 *
	 * <p>On compare donc chaque phrase a la <b>moyenne de ce joueur-la</b>.
	 * Quelqu'un qui parle bas crie a un volume ou quelqu'un d'autre murmure, et
	 * les deux sont entendus de la meme facon. C'est la meme regle que le seuil
	 * de silence a 900 ms : le mod s'adapte a la personne, pas l'inverse.
	 *
	 * <h2>Prudent au demarrage</h2>
	 *
	 * <p>Tant qu'on n'a pas quelques phrases pour asseoir la moyenne, on repond
	 * non. Mieux vaut ne pas voir un cri que d'en inventer un — un cri fait
	 * obeir une bete qui boude, ce n'est pas un detail.
	 *
	 * @param force la moyenne quadratique la plus forte de la phrase
	 */
	/**
	 * Comment il a parle.
	 *
	 * <p>Trois tons, et chacun sert a quelque chose de different. Crier fait
	 * porter la voix ; chuchoter la <b>retient</b>, ce qui est exactement ce
	 * qu'on veut dans un couloir ou trois personnes commandent leurs betes en
	 * meme temps.
	 */
	public enum Ton {
		CHUCHOTE,
		ORDINAIRE,
		CRIE
	}

	/**
	 * Sur quel ton il vient de parler.
	 *
	 * <p>Meme principe que pour le cri, et pour la meme raison : tout se mesure
	 * <b>par rapport a sa propre voix</b>. Un chuchotement n'est pas un volume,
	 * c'est un ecart — celui d'une personne qui parle plus bas qu'elle n'a
	 * l'habitude.
	 */
	public static Ton tonDe(UUID joueur, double force) {
		Ton ton = Ton.ORDINAIRE;
		Ton avant = tons.containsKey(joueur) && tons.get(joueur).phrases() >= PHRASES_POUR_JUGER
				&& force >= PLANCHER_AUDIBLE
				&& force < tons.get(joueur).moyenne() * CE_QUI_FAIT_UN_CHUCHOTEMENT
				? Ton.CHUCHOTE : Ton.ORDINAIRE;
		// aCrie met la moyenne a jour : on l'appelle toujours, meme si on a deja
		// reconnu un chuchotement. Sans cela, quelqu'un qui chuchote souvent
		// verrait sa moyenne se figer.
		if (aCrie(joueur, force)) {
			ton = Ton.CRIE;
		} else if (avant == Ton.CHUCHOTE) {
			ton = Ton.CHUCHOTE;
		}
		return ton;
	}

	public static boolean aCrie(UUID joueur, double force) {
		if (force < PLANCHER_AUDIBLE) {
			// Du souffle, un raclement de chaise : on ne s'en sert meme pas pour
			// la moyenne, qui en serait tiree vers le bas sans raison.
			return false;
		}
		Habitude ton = tons.get(joueur);
		if (ton == null) {
			tons.put(joueur, new Habitude(force, 1));
			return false;
		}

		boolean crie = ton.phrases() >= PHRASES_POUR_JUGER
				&& force > ton.moyenne() * CE_QUI_FAIT_UN_CRI;

		// La moyenne suit lentement : elle doit representer la voix ordinaire du
		// joueur, pas la derniere phrase. Et un cri ne la tire pas vers le haut,
		// sinon crier trois fois de suite rendrait le quatrieme cri impossible.
		if (!crie) {
			double suivie = ton.moyenne() * (1.0D - SOUPLESSE) + force * SOUPLESSE;
			tons.put(joueur, new Habitude(suivie, Math.min(ton.phrases() + 1, PHRASES_POUR_JUGER)));
		}
		return crie;
	}

	public static void oublier(UUID joueur) {
		profils.remove(joueur);
		aPreparer.remove(joueur);
		tons.remove(joueur);
		// Les souvenirs de conversation partent avec lui : sans cela, chaque
		// joueur ayant parle depuis le demarrage laisserait une entree derriere
		// lui. Une seule est minuscule ; mille qui ne partent jamais, non.
		EcouteVocale.oublier(joueur);
	}
}
