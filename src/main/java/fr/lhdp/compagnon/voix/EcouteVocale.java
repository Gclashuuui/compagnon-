package fr.lhdp.compagnon.voix;

import fr.lhdp.compagnon.entite.CompagnonEntity;
import fr.lhdp.compagnon.entite.MangerDansGamelleGoal;
import fr.lhdp.compagnon.entite.AllerAuPerchoirGoal;
import fr.lhdp.compagnon.entite.Obeissance;
import fr.lhdp.compagnon.entite.RapporterGoal;
import fr.lhdp.compagnon.espece.Espece;
import fr.lhdp.compagnon.espece.Especes;
import fr.lhdp.compagnon.competence.Competence;
import fr.lhdp.compagnon.competence.Competences;
import fr.lhdp.compagnon.fiche.Barre;
import fr.lhdp.compagnon.fiche.FicheCompagnon;
import fr.lhdp.compagnon.progression.Niveaux;
import fr.lhdp.compagnon.fiche.Fiches;
import fr.lhdp.compagnon.fiche.Mode;
import fr.lhdp.compagnon.objet.Objets;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ce qui arrive quand un joueur appelle son compagnon par son nom.
 *
 * <p>« Cacahuete » — il vient. « Cacahuete assis » — il s'assoit.
 *
 * <p>Cette classe ne sait pas d'ou vient la phrase. Elle peut venir d'un micro,
 * d'une commande tapee, d'un essai automatique : c'est voulu. Le morceau delicat
 * — capter la voix, la dechiffrer, la transcrire — est ailleurs, et tout ce qui
 * est ici se teste sans micro.
 *
 * <h2>Ce qu'on verifie avant d'obeir</h2>
 *
 * <ol>
 *   <li>le nom prononce est bien celui d'un compagnon <b>de ce joueur</b> — on
 *       ne commande pas la bete du voisin ;</li>
 *   <li>le compagnon est <b>a portee de voix</b> — on ne se fait pas entendre a
 *       l'autre bout du chateau ;</li>
 *   <li>son entite est la : un compagnon range dans une chambre n'entend rien.</li>
 * </ol>
 */
public final class EcouteVocale {

	/** Jusqu'ou porte la voix, en blocs. Valeur inventee. */
	public static final double PORTEE = 16.0D;

	/**
	 * Jusqu'ou il cherche un objet a ramasser quand on lui dit « chope ».
	 *
	 * <p>Autour de LUI, pas du joueur : c'est lui qui va le chercher.
	 */
	/**
	 * Jusqu'ou porte la voix quand on hausse le ton.
	 *
	 * <p>Une vingtaine de metres de plus. Assez pour rappeler une bete qui
	 * s'eloigne, pas assez pour commander a travers un chateau.
	 */
	public static final double PORTEE_DU_CRI = 26.0D;

	/**
	 * Le bonus d'oreille le plus genereux qu'une competence puisse donner.
	 *
	 * <p>Il ne sert qu'a borner la recherche dans le monde : on cherche large,
	 * puis chaque bete decide si elle etait vraiment a portee. Sans cette borne
	 * il faudrait chercher a l'infini pour ne rien manquer.
	 */
	private static final double OREILLE_MAXIMUM = 16.0D;

	/**
	 * Jusqu'ou porte la voix quand on chuchote.
	 *
	 * <p>Quelques pas. Assez pour la bete devant soi, pas assez pour celle du
	 * voisin — et c'est tout l'interet : on peut donner un ordre discret au
	 * milieu d'une salle pleine sans que trois autres compagnons repondent.
	 */
	public static final double PORTEE_DU_CHUCHOTEMENT = 6.0D;

	private static final double PORTEE_RAMASSAGE = 12.0D;

	/**
	 * Combien de temps on se souvient du compagnon que ce joueur vient de nommer.
	 *
	 * <p>Quinze secondes : le temps de reprendre son souffle, de chercher un mot,
	 * de recommencer une phrase.
	 */
	private static final long MEMOIRE_DU_NOM = 30_000L;

	/**
	 * Combien de temps un ordre attend qu'on lui donne un nom.
	 *
	 * <p>Beaucoup de gens disent le verbe d'abord : « chope !... Dragon ! ».
	 * C'est meme la tournure la plus naturelle quand on est presse. Sans cette
	 * memoire, l'ordre tombait dans le vide et le nom qui suivait ne faisait
	 * que le faire venir.
	 *
	 * <p>Plus court que la memoire du nom : un ordre sans destinataire est plus
	 * facheux qu'un nom sans ordre s'il traine trop longtemps.
	 */
	private static final long MEMOIRE_DE_L_ORDRE = 8_000L;

	/** Combien de temps dure un tour appris, en ticks. Comme depuis la roue. */
	private static final int DUREE_DU_TOUR = 100;

	/** Le petit geste qui dit « j'ai entendu », en ticks. Court : c'est un accuse. */
	private static final int DUREE_DE_L_ECOUTE = 16;

	/**
	 * Combien de temps il plane quand on lui dit de monter, en ticks.
	 *
	 * <p>Quinze secondes. Assez pour qu'on le regarde tourner au-dessus de soi,
	 * assez peu pour qu'il redescende tout seul si on l'oublie — un compagnon
	 * laisse en l'air pour toujours serait une bete perdue.
	 */
	private static final int DUREE_DU_VOL_LIBRE = 20 * 15;

	/** Combien la complicite monte quand on le felicite. */
	private static final float CE_QUE_VAUT_UN_BRAVO = 1.5F;

	/**
	 * Avant qu'un bravo compte de nouveau, en millisecondes.
	 *
	 * <p>Sans ce delai, repeter « bravo » vingt fois de suite remplacerait tout
	 * le reste du jeu. Une minute : on felicite un geste, pas une syllabe.
	 */
	private static final long ENTRE_DEUX_BRAVOS = 60_000L;

	/** Le dernier bravo de chaque joueur, pour ne pas qu'il se repete. */
	private static final Map<UUID, Long> derniersBravos = new ConcurrentHashMap<>();

	/** Le dernier compagnon nomme par chaque joueur, et quand. */
	private static final Map<UUID, Rappel> derniersAppeles = new ConcurrentHashMap<>();

	/** Le dernier ordre dit sans nom par chaque joueur, et quand. */
	private static final Map<UUID, Attente> ordresOrphelins = new ConcurrentHashMap<>();

	/**
	 * Combien de fois d'affilee ce joueur n'a pas ete compris.
	 *
	 * <h2>Ce que ca coute</h2>
	 *
	 * <p>Un entier par joueur qui parle, en memoire, efface a sa deconnexion.
	 * <b>Rien n'est ecrit sur le disque</b>, aucune phrase n'est conservee, et il
	 * n'y a aucun travail par tick : on incremente au moment ou l'echec arrive,
	 * et c'est tout. A mille joueurs, c'est quatre kilo-octets.
	 *
	 * <h2>Ce que ca apporte</h2>
	 *
	 * <p>Quelqu'un dont la voix passe mal reessaie la meme phrase, encore et
	 * encore, parce que rien ne lui dit qu'il existe une autre facon de le dire.
	 * Au troisieme echec, on lui en propose une — courte, et donc plus facile a
	 * entendre.
	 *
	 * <p>Le compteur repart a zero des qu'il est compris : c'est ce qui evite
	 * d'expliquer la voix a quelqu'un qui la maitrise deja.
	 */
	private static final Map<UUID, Integer> echecsDAffilee = new ConcurrentHashMap<>();

	/** Au bout de tant d'echecs de suite, on propose autre chose. */
	private static final int ECHECS_AVANT_UN_CONSEIL = 3;

	private record Rappel(UUID compagnon, long quand) {
	}

	private record Attente(CommandeVocale ordre, long quand) {
	}

	private EcouteVocale() {
	}

	/**
	 * Le resultat d'une ecoute, pour que la commande d'essai puisse le raconter.
	 *
	 * @param compris  vrai si un compagnon a obei
	 * @param message  ce qu'on peut dire au joueur
	 */
	public record Resultat(boolean compris, String message) {
	}

	/**
	 * Un joueur vient de dire quelque chose.
	 *
	 * @param phrase ce qui a ete entendu, tel quel
	 */
	public static Resultat entendu(ServerPlayer joueur, String phrase) {
		return entendu(joueur, phrase, Voix.Ton.ORDINAIRE);
	}

	/**
	 * La meme chose, en sachant s'il a hausse le ton.
	 *
	 * <h2>Ce que change un cri</h2>
	 *
	 * <p>Deux choses, et deux seulement : <b>la voix porte plus loin</b>, et
	 * <b>la bete ne peut pas faire semblant de ne pas entendre</b>.
	 *
	 * <p>Rien de plus. Un cri ne fait pas obeir un compagnon qui n'est pas a
	 * vous, ne remplace pas son nom, et ne donne aucun droit. Crier plus fort ne
	 * doit jamais devenir une facon de jouer — c'est une facon de se faire
	 * entendre de loin, et d'insister quand la bete boude.
	 *
	 * <p>Voir {@code Voix.aCrie} : le cri se mesure par rapport a la voix
	 * ordinaire de CE joueur, jamais dans l'absolu. Un seuil absolu
	 * recompenserait le bon materiel.
	 */
	public static Resultat entendu(ServerPlayer joueur, String phrase, Voix.Ton ton) {
		Resultat resultat = analyser(joueur, phrase, ton);
		repondre(joueur, phrase, resultat);
		return resultat;
	}

	/**
	 * Dit au joueur ce qui vient de se passer, meme quand rien n'a marche.
	 *
	 * <h2>Pourquoi c'est indispensable</h2>
	 *
	 * <p>Une voix qui echoue en silence est indebogable. Le joueur crie, rien
	 * ne bouge, et il ne peut rien en conclure : micro coupe ? mot mal
	 * entendu ? compagnon qui boude ? refus parce qu'il tient deja quelque
	 * chose ? Ces quatre pannes se ressemblaient toutes — un dragon immobile.
	 *
	 * <p>Maintenant il lit ce qui a ete compris. S'il voit « chaud » alors
	 * qu'il a dit « chope », il sait que c'est l'oreille. S'il voit « il a
	 * deja quelque chose dans la gueule », il sait que c'est lui.
	 *
	 * <p>Dans la barre d'action, pas dans le chat : ca s'efface tout seul et ca
	 * n'encombre l'historique de personne.
	 */
	private static void repondre(ServerPlayer joueur, String phrase, Resultat resultat) {
		if (resultat.compris()) {
			echecsDAffilee.remove(joueur.getUUID());
			joueur.displayClientMessage(Component.literal(resultat.message())
				.withStyle(ChatFormatting.GREEN), true);
			return;
		}
		String dit = Vocabulaire.pourLaGrammaire(phrase);
		if (dit.isEmpty()) {
			// Le moteur n'a rien tire du son : inutile d'afficher des guillemets vides.
			return;
		}
		joueur.displayClientMessage(Component.literal("« " + dit + " » — " + resultat.message())
			.withStyle(ChatFormatting.GRAY), true);

		conseiller(joueur);
	}

	/**
	 * Propose une autre facon de le dire, apres quelques echecs de suite.
	 *
	 * <p>Des mots <b>courts</b> : plus un mot est court et courant, mieux le
	 * moteur l'entend. Et on rappelle que le nom est facultatif — c'est le
	 * conseil qui debloque le plus de monde, parce que le nom est justement le
	 * mot le plus dur a faire passer.
	 *
	 * <p>Une seule fois : le compteur repart a zero apres le conseil. Repeter la
	 * meme phrase a chaque echec serait pire que de se taire.
	 */
	private static void conseiller(ServerPlayer joueur) {
		int echecs = echecsDAffilee.merge(joueur.getUUID(), 1, Integer::sum);
		if (echecs < ECHECS_AVANT_UN_CONSEIL) {
			return;
		}
		echecsDAffilee.remove(joueur.getUUID());
		joueur.sendSystemMessage(Component.translatable("voix.compagnon.conseil")
				.withStyle(ChatFormatting.GRAY));
	}

	private static Resultat analyser(ServerPlayer joueur, String phrase, Voix.Ton ton) {
		boolean crie = ton == Voix.Ton.CRIE;
		String propre = Vocabulaire.pourLaComparaison(phrase);
		if (propre.isEmpty()) {
			return new Resultat(false, "Rien entendu.");
		}

		// L'ordre se lit AVANT de chercher a qui il s'adresse : il n'en depend pas,
		// et il faut pouvoir le garder de cote si le nom n'arrive qu'apres.
		CommandeVocale ordre = Vocabulaire.reconnaitre(propre);

		Fiches fiches = Fiches.de(joueur.server);

		// UNE SEULE RECHERCHE DANS LE MONDE, et tout se decide dessus.
		//
		// Il y en avait jusqu'a trois par phrase entendue — une pour le nom, une
		// pour le souvenir, une pour le reste. A mille joueurs qui bavardent,
		// c'est trois fois trop.
		//
		// TOUS ceux qui sont a portee, pas seulement les siens : un compagnon qui
		// vous connait bien repond aussi a vous.
		List<CompagnonEntity> autour = ceuxQuiEcoutent(joueur, ton);

		// L'ORDRE DE CES QUATRE QUESTIONS EST LE COEUR DE TOUT.
		//
		// Ce qui vient d'etre DIT l'emporte toujours sur ce dont on se SOUVIENT.
		// C'etait l'inverse, et voici ce que ca donnait avec deux betes :
		//
		//   « Mouette, viens »  -> elle vient, et on retient son nom 30 secondes.
		//   « Prince, viens »   -> le moteur rend « princes » au lieu de « prince ».
		//                          Le nom exact ne colle pas, on passait donc au
		//                          souvenir — et c'est la MOUETTE qui repondait.
		//
		// Le joueur voit alors une bete qui obeit et une autre qui semble sourde,
		// alors qu'il a parfaitement prononce les deux noms. La variante du nom
		// qu'il vient de dire passe donc AVANT le souvenir, et l'espece aussi.
		CompagnonEntity compagnon = celuiDontOnDitLeNom(autour, fiches, propre);

		// Son nom, entendu de travers : « princes » pour « Prince ».
		if (compagnon == null) {
			compagnon = celuiDontLeNomRessemble(autour, fiches, propre, joueur);
		}

		// Son espece : « Dragon, assis » quand la bete s'appelle Dargon.
		if (compagnon == null) {
			compagnon = celuiDontOnDitLEspece(autour, fiches, propre, joueur);
		}

		// PERSONNE N'EST OBLIGE DE DIRE SA PHRASE D'UN SEUL SOUFFLE.
		//
		// « Cacahuete... » puis, quelques secondes plus tard, « assis » : les deux
		// morceaux se rejoignent. Ca sert a tout le monde — on se reprend, on
		// hesite, on est coupe — et surtout a ceux qui begaient ou qui parlent
		// lentement, pour qui une phrase d'un seul tenant n'a rien d'evident.
		//
		// Mais seulement si AUCUN nom n'a ete prononce : voir plus haut.
		if (compagnon == null) {
			compagnon = celuiQuOnVientDeNommer(joueur, autour);
		}

		// LE NOM EST FACULTATIF QUAND IL N'Y A AUCUN DOUTE.
		//
		// Quand une seule bete peut vous entendre, a qui d'autre parleriez-vous ?
		// Le nom ne sert qu'a CHOISIR, et on ne choisit que quand il y a le choix.
		if (compagnon == null && ordre != null) {
			compagnon = leSeulQuiEcoute(autour, fiches, joueur);
		}
		if (compagnon == null) {
			return sansDestinataire(joueur, fiches, autour, ordre);
		}
		derniersAppeles.put(joueur.getUUID(),
				new Rappel(compagnon.getUUID(), System.currentTimeMillis()));
		FicheCompagnon appele = fiches.get(compagnon.ficheId());
		if (appele == null) {
			return new Resultat(false, "Ce compagnon n'a pas de fiche.");
		}
		boolean sonMaitre = appele.proprietaire().equals(joueur.getUUID());

		// IL MONTRE QU'IL A ENTENDU, AVANT MEME D'AVOIR COMPRIS.
		//
		// Il tournait deja la tete. Mais entre le moment ou l'on parle et celui ou
		// la bete fait quelque chose, il se passe une bonne seconde — le temps que
		// le silence soit constate, la phrase conclue, l'ordre reconnu. Pendant
		// cette seconde, rien ne se passait, et on ne savait pas si on avait ete
		// entendu ou si on parlait dans le vide.
		//
		// Un geste bref suffit a combler ce trou : il dresse la tete, il attend. Le
		// role est court exprès — l'animation de l'ordre lui-meme le remplacera
		// aussitot, et c'est tres bien ainsi.
		compagnon.getLookControl().setLookAt(joueur, 30.0F, 30.0F);
		compagnon.jouerActionPendant("@ecoute", DUREE_DE_L_ECOUTE);

		// UN MOT QU'IL A APPRIS, RIEN QU'A LUI.
		//
		// Avant les ordres du mod : c'est le geste le plus precis qu'on puisse lui
		// demander, et c'est son proprietaire qui le lui a appris. Il l'emporte
		// donc sur « assis » si les deux se trouvent dans la meme phrase.
		String geste = gesteApprisDans(appele, propre);
		if (geste != null) {
			return faireSonTour(compagnon, appele, geste);
		}

		// Le nom seul, apres un ordre reste sans destinataire : on les rassemble.
		// « Chope ! » puis « Sabre ! » — beaucoup de gens disent le verbe d'abord.
		if (ordre == null) {
			ordre = ordreQuiAttendait(joueur);
		}

		// CE QU'UN AUTRE QUE SON MAITRE PEUT LUI DEMANDER.
		//
		// Les gestes touchent a ce qu'il porte, donc aux affaires de quelqu'un.
		// Un camarade de classe ne fait pas lacher a votre dragon ce que vous lui
		// avez confie, et ne lui fait pas ramasser vos choses. Le reste — venir,
		// s'asseoir, se coucher — n'appartient a personne.
		if (!sonMaitre && ordre != null && ordre.geste() != null) {
			return new Resultat(false, appele.nom() + " n'ecoute que "
					+ nomDuMaitre(joueur, appele) + " pour ca.");
		}

		// Les gestes ne changent pas son mode : il fait la chose, et c'est fini.
		if (ordre != null && ordre.geste() != null) {
			// « J'écoute » ne doit pas rester au-dessus du geste demandé ni bloquer
			// sa navigation. L'ordre compris prend la main immédiatement.
			compagnon.annulerActionPourOrdre();
			return geste(joueur, compagnon, appele, fiches, ordre.geste());
		}

		Mode mode = ordre == null ? Mode.SUIT : ordre.mode();

		// Il a entendu, mais il n'est pas oblige d'etre d'accord. Voir Obeissance :
		// il ne refuse jamais de venir, et jamais deux fois de suite.
		// UN CRI PASSE OUTRE LA BOUDERIE.
		//
		// Une bete a le droit de faire la sourde oreille — c'est ce qui lui donne
		// un caractere. Mais elle ne peut pas faire semblant de ne pas entendre
		// quelqu'un qui hausse le ton : personne n'y croirait, et le joueur qui
		// insiste merite d'etre entendu.
		if (!crie && !Obeissance.accepte(compagnon, appele, mode)) {
			return new Resultat(false, appele.nom() + " fait la sourde oreille.");
		}

		compagnon.annulerActionPourOrdre();
		appele.setMode(mode);
		compagnon.appliquerMode(mode);
		fiches.setDirty();

		// « Viens » n'est pas qu'un mode : c'est un ordre d'arriver. Il va donc
		// chercher un chemin jusqu'a celui qui l'appelle, au lieu de simplement se
		// mettre a le suivre — ce qui ne sert a rien s'il y a un mur entre eux.
		if (mode == Mode.SUIT) {
			compagnon.venirVers(joueur);
		}

		// Un seul message, et c'est repondre() qui l'affiche : le joueur doit
		// lire quelque chose meme quand ca ne marche pas, sinon il n'a aucun
		// moyen de savoir si c'est son micro, l'oreille, ou la bete qui boude.
		return new Resultat(true, appele.nom() + " " + libelle(mode));
	}

	/**
	 * « Chope ! » et « lache ! ».
	 *
	 * <p>Ce sont les deux seuls ordres qui touchent au monde plutot qu'a son
	 * humeur. Ils ne passent pas par l'obeissance : si on lui demande de lacher, il
	 * lache. Un compagnon qui boude en gardant votre pioche dans la gueule serait
	 * insupportable.
	 */
	private static Resultat geste(ServerPlayer joueur, CompagnonEntity compagnon,
			FicheCompagnon fiche, Fiches fiches, CommandeVocale.Geste geste) {

		// LES QUATRE ORDRES QUI NE TOUCHENT A RIEN.
		//
		// Monter, descendre, tourner, feliciter : aucun ne deplace un objet, donc
		// aucun n'a de raison d'etre reserve au proprietaire. N'importe qui de
		// connu peut dire bravo a une bete, et c'est tres bien ainsi.
		switch (geste) {
			case MONTER -> {
				if (!compagnon.saitVoler()) {
					return new Resultat(false, fiche.nom() + " n'a pas d'ailes.");
				}
				compagnon.monterDansLeCiel(DUREE_DU_VOL_LIBRE);
				return new Resultat(true, fiche.nom() + " prend de la hauteur.");
			}
			case DESCENDRE -> {
				if (!compagnon.saitVoler()) {
					return new Resultat(false, fiche.nom() + " est deja au sol.");
				}
				compagnon.redescendre();
				return new Resultat(true, fiche.nom() + " redescend.");
			}
			case TOURNER -> {
				compagnon.jouerActionPendant("@tourne", DUREE_DU_TOUR);
				return new Resultat(true, fiche.nom() + " fait son tour.");
			}
			case MANGER -> {
				if (fiche.barre(Barre.FAIM) >= Barre.MAXIMUM) {
					return new Resultat(false, fiche.nom() + " n'a plus faim.");
				}
				if (!MangerDansGamelleGoal.ordonner(compagnon)) {
					return new Resultat(false, "aucun repas ne l'attend dans une gamelle proche.");
				}
				fiche.setMode(Mode.RESTE);
				compagnon.appliquerMode(Mode.RESTE);
				fiches.setDirty();
				return new Resultat(true, fiche.nom() + " va manger dans sa gamelle.");
			}
			case PERCHOIR -> {
				BlockPos perchoir = null;
				for (FicheCompagnon.Lieu lieu : fiche.lieux()) {
					BlockPos position = new BlockPos(lieu.x(), lieu.y(), lieu.z());
					if (lieu.cle().equals("perchoir")
							&& compagnon.level().getBlockState(position).is(Objets.PERCHOIR)) {
						perchoir = position;
						break;
					}
				}
				if (perchoir == null) {
					return new Resultat(false,
							"montre-lui d'abord un perchoir en cliquant dessus, puis sur lui.");
				}
				fiche.setMode(Mode.RESTE);
				compagnon.appliquerMode(Mode.RESTE);
				AllerAuPerchoirGoal.ordonner(compagnon, perchoir);
				fiches.setDirty();
				return new Resultat(true, fiche.nom() + " retourne à son perchoir.");
			}
			case FELICITER -> {
				return feliciter(joueur, compagnon, fiche, fiches);
			}
			default -> {
				// PRENDRE et LACHER : ils touchent aux affaires de quelqu'un, et
				// sont donc traites plus bas, apres le controle du proprietaire.
			}
		}

		if (geste == CommandeVocale.Geste.LACHER) {
			if (compagnon.lesMainsVides()) {
				return new Resultat(false, "il n'a rien dans la gueule.");
			}
			compagnon.poserCeQuIlPorte();
			return new Resultat(true, fiche.nom() + " pose ce qu'il portait.");
		}

		ItemEntity objet = leplusProche(compagnon);
		if (objet == null) {
			return new Resultat(false, "il n'a rien a ramasser autour de lui.");
		}

		// IL LACHE CE QU'IL A POUR PRENDRE CE QU'ON LUI DEMANDE.
		//
		// Avant, il refusait — et il refusait EN SILENCE. On avait alors
		// exactement l'impression qu'il rejetait certains objets : la terre
		// marchait, la buche de chene non. En verite il tenait deja la terre,
		// et personne ne le lui avait dit.
		//
		// Un chien a qui on montre autre chose lache son baton. Rien ne se perd :
		// ce qu'il portait retombe a ses pieds, dans le monde.
		if (!compagnon.lesMainsVides()) {
			compagnon.poserCeQuIlPorte();
		}
		compagnon.allerChercher(objet);
		return new Resultat(true, fiche.nom() + " va chercher "
				+ objet.getItem().getHoverName().getString() + ".");
	}

	/**
	 * L'objet pose a terre le plus proche du compagnon.
	 *
	 * <p>Le plus proche de LUI et non du joueur : c'est lui qui va le chercher, et
	 * c'est ce qu'on montre du doigt quand on le lui demande.
	 *
	 * <p>On respecte le delai de ramassage de Minecraft : un objet que quelqu'un
	 * vient de lacher lui appartient encore.
	 */
	private static ItemEntity leplusProche(CompagnonEntity compagnon) {
		// N'IMPORTE QUEL OBJET. Une buche, un minerai, une epee, un objet d'un
		// autre mod : on ne filtre rien. Ce n'est pas un repas, c'est un ordre.
		//
		// On ne respecte pas non plus le delai de ramassage, contrairement au
		// ramassage spontane : ce delai existe pour empecher qu'on vous prenne ce
		// que vous venez de lacher. Ici c'est vous qui le demandez, et attendre
		// deux secondes apres avoir jete l'objet serait absurde.
		return RapporterGoal.leplusProche(compagnon, PORTEE_RAMASSAGE,
				objet -> objet.isAlive() && !objet.isInLava());
	}

	/**
	 * Le compagnon dont le nom apparait dans la phrase, a portee de voix.
	 *
	 * <h2>Qui repond a qui</h2>
	 *
	 * <p>Son <b>maitre</b>, toujours. Et les gens qu'il <b>connait</b> — ceux qui
	 * s'occupent de lui assez souvent pour ne plus etre des inconnus. Un passant,
	 * lui, peut crier son nom autant qu'il veut : la bete ne le regarde meme pas.
	 *
	 * <p>C'est ce qui rend un compagnon SIEN. Sans cette regle, n'importe qui dans
	 * un couloir de Poudlard ferait asseoir la bete des autres en criant un nom.
	 *
	 * <p>Le nom le plus long gagne : entre « Bou » et « Boule », dire « Boule »
	 * ne doit pas appeler le premier.
	 */
	private static CompagnonEntity celuiDontOnDitLeNom(List<CompagnonEntity> autour,
			Fiches fiches, String phrase) {

		CompagnonEntity trouve = null;
		int meilleureLongueur = 0;

		for (CompagnonEntity bete : autour) {
			FicheCompagnon fiche = fiches.get(bete.ficheId());
			if (fiche == null) {
				continue;
			}
			String nom = Vocabulaire.pourLaComparaison(fiche.nom());
			if (nom.isEmpty() || nom.length() <= meilleureLongueur) {
				continue;
			}
			if ((" " + phrase + " ").contains(" " + nom + " ")) {
				trouve = bete;
				meilleureLongueur = nom.length();
			}
		}
		return trouve;
	}

	/**
	 * Le compagnon que ce joueur vient de nommer, s'il est encore la.
	 *
	 * <p>On reverifie tout : il doit toujours exister, etre a portee, et connaitre
	 * le joueur. Le souvenir sert a rattacher un ordre a un nom, jamais a donner
	 * un droit qu'on n'aurait plus.
	 */
	private static CompagnonEntity celuiQuOnVientDeNommer(ServerPlayer joueur,
			List<CompagnonEntity> autour) {

		Rappel rappel = derniersAppeles.get(joueur.getUUID());
		if (rappel == null || System.currentTimeMillis() - rappel.quand() > MEMOIRE_DU_NOM) {
			derniersAppeles.remove(joueur.getUUID());
			return null;
		}
		// On reverifie sur la liste du moment : il doit toujours etre la, et
		// toujours nous connaitre. Le souvenir rattache un ordre a un nom, il ne
		// donne jamais un droit qu'on n'aurait plus.
		for (CompagnonEntity bete : autour) {
			if (bete.getUUID().equals(rappel.compagnon())) {
				return bete;
			}
		}
		return null;
	}

	/**
	 * Ceux qui peuvent entendre ce joueur, maintenant.
	 *
	 * <p>A portee de voix, et qui le <b>connaissent</b> : son maitre toujours, et
	 * les gens qui s'occupent de lui assez souvent pour ne plus etre des
	 * inconnus. Un passant peut crier ce qu'il veut, la bete ne le regarde meme
	 * pas — c'est ce qui fait qu'un compagnon est SIEN.
	 */
	private static List<CompagnonEntity> ceuxQuiEcoutent(ServerPlayer joueur, Voix.Ton ton) {
		// LE TON CHANGE LA PORTEE, ET RIEN D'AUTRE.
		//
		// Crier porte loin : on rappelle une bete qui s'eloigne. Chuchoter
		// <b>retient</b> la voix a quelques pas — c'est exactement ce qu'on veut
		// dans un couloir ou trois personnes commandent leurs betes en meme
		// temps, et ou parler normalement les ferait toutes reagir.
		// LA PORTEE EST CELLE DE LA BETE, PAS CELLE DU JOUEUR.
		//
		// Une bete qui a l'oreille fine entend de plus loin — c'est sa competence
		// a elle, pas une propriete de celui qui parle. On cherche donc large, et
		// chacune decide ensuite si elle etait a portee.
		double base = switch (ton) {
			case CRIE -> PORTEE_DU_CRI;
			case CHUCHOTE -> PORTEE_DU_CHUCHOTEMENT;
			default -> PORTEE;
		};
		Fiches fiches = Fiches.de(joueur.server);
		return joueur.level().getEntitiesOfClass(CompagnonEntity.class,
				joueur.getBoundingBox().inflate(base + OREILLE_MAXIMUM),
				bete -> bete.ficheId() != null
						&& bete.connait(joueur.getUUID())
						&& aPortee(joueur, bete, fiches, base));
	}

	/** Cette bete-ci entend-elle d'ici, avec l'oreille qu'elle a ? */
	private static boolean aPortee(ServerPlayer joueur, CompagnonEntity bete,
			Fiches fiches, double base) {

		FicheCompagnon fiche = fiches.get(bete.ficheId());
		double portee = base + (fiche == null ? 0.0F
				: Competences.bonus(fiche.competences(), Competence.OREILLE));
		return bete.distanceToSqr(joueur) <= portee * portee;
	}

	/**
	 * Celui dont le nom a ete entendu <b>de travers</b>.
	 *
	 * <p>Le moteur rend « dragons » quand on a dit « dragon » : c'est le meme
	 * mot a l'oreille, et le compagnon doit repondre. Les variantes viennent du
	 * dictionnaire lui-meme, pas d'une invention — voir {@code Lexique.variantes}.
	 *
	 * <p>Meme prudence que partout : si deux betes se disputent la variante, on
	 * ne devine pas.
	 */
	private static CompagnonEntity celuiDontLeNomRessemble(List<CompagnonEntity> autour,
			Fiches fiches, String phrase, ServerPlayer joueur) {

		Lexique lexique = Lexique.dejaLu();
		if (lexique == null || lexique.vide()) {
			return null;
		}
		List<CompagnonEntity> nommes = new ArrayList<>();
		String entoure = " " + phrase + " ";
		for (CompagnonEntity bete : autour) {
			FicheCompagnon fiche = fiches.get(bete.ficheId());
			if (fiche == null) {
				continue;
			}
			for (String variante : lexique.variantes(fiche.nom())) {
				String propre = Vocabulaire.pourLaComparaison(variante);
				if (!propre.isEmpty() && entoure.contains(" " + propre + " ")) {
					nommes.add(bete);
					break;
				}
			}
		}
		return nommes.isEmpty() ? null : leSeulQuiEcoute(nommes, fiches, joueur);
	}

	/**
	 * Celui dont on a dit l'<b>espece</b> plutot que le nom.
	 *
	 * <p>« Dragon, assis » marche meme si la bete s'appelle Dargon — un nom que
	 * le micro ne sait pas dire, et qui la rendait donc inappelable a vie.
	 *
	 * <p>Meme prudence que partout ailleurs : s'il y a plusieurs dragons a
	 * portee, on prend celui du joueur, et s'il en a deux on ne devine pas.
	 */
	private static CompagnonEntity celuiDontOnDitLEspece(List<CompagnonEntity> autour,
			Fiches fiches, String phrase, ServerPlayer joueur) {

		List<CompagnonEntity> nommes = new ArrayList<>();
		for (CompagnonEntity bete : autour) {
			FicheCompagnon fiche = fiches.get(bete.ficheId());
			if (fiche == null) {
				continue;
			}
			// Le mot par lequel on l'appelle vraiment, qui n'est pas toujours le nom
			// du dossier : on crie « dragon » a un dragonnet.
			Espece sonEspece = Especes.get(fiche.espece());
			String appel = Vocabulaire.pourLaComparaison(
				sonEspece == null ? fiche.espece() : sonEspece.nomVocal());
			if (!appel.isEmpty() && (" " + phrase + " ").contains(" " + appel + " ")) {
				nommes.add(bete);
			}
		}
		return nommes.isEmpty() ? null : leSeulQuiEcoute(nommes, fiches, joueur);
	}

	/**
	 * Le seul a qui cet ordre puisse s'adresser, ou {@code null} s'il y a un doute.
	 *
	 * <h2>Deux facons de ne pas avoir de doute</h2>
	 *
	 * <p>Une seule bete vous entend : c'est elle, evidemment.
	 *
	 * <p>Plusieurs vous entendent mais une seule est a vous : c'est la votre. On
	 * ne fait pas asseoir la bete d'un camarade en oubliant un nom — et dans un
	 * couloir plein de monde, c'est le cas de loin le plus frequent.
	 *
	 * <p>Deux betes a vous, toutes les deux la : il faut un nom. On ne devine pas.
	 */
	private static CompagnonEntity leSeulQuiEcoute(List<CompagnonEntity> autour,
			Fiches fiches, ServerPlayer joueur) {

		if (autour.size() == 1) {
			return autour.get(0);
		}
		CompagnonEntity sien = null;
		for (CompagnonEntity bete : autour) {
			FicheCompagnon fiche = fiches.get(bete.ficheId());
			if (fiche == null || !fiche.proprietaire().equals(joueur.getUUID())) {
				continue;
			}
			if (sien != null) {
				// Deux betes a lui : il faut choisir, donc il faut un nom.
				return null;
			}
			sien = bete;
		}
		return sien;
	}

	/**
	 * Ce qu'on repond quand on n'a trouve personne a qui parler.
	 *
	 * <p>Trois situations, trois phrases differentes — et c'est important : un
	 * message unique laissait le joueur sans la moindre idee de ce qu'il fallait
	 * changer. Est-il trop loin ? A-t-on mal prononce ? Sont-ils deux ?
	 */
	private static Resultat sansDestinataire(ServerPlayer joueur, Fiches fiches,
			List<CompagnonEntity> autour, CommandeVocale ordre) {

		if (autour.isEmpty()) {
			return new Resultat(false, "aucun compagnon assez pres pour t'entendre.");
		}
		if (ordre == null) {
			// On a entendu quelque chose, mais aucun ordre connu dedans.
			return new Resultat(false, "je n'ai pas compris l'ordre.");
		}

		// Il y a de quoi obeir et de quoi choisir : il ne manque que le nom. On le
		// garde de cote quelques secondes, et on dit LESQUELS attendent — sans les
		// nommer, le joueur ne sait meme pas quoi dire.
		ordresOrphelins.put(joueur.getUUID(), new Attente(ordre, System.currentTimeMillis()));

		List<String> noms = new ArrayList<>();
		for (CompagnonEntity bete : autour) {
			FicheCompagnon fiche = fiches.get(bete.ficheId());
			if (fiche != null && fiche.proprietaire().equals(joueur.getUUID())) {
				noms.add(fiche.nom());
			}
		}
		if (noms.isEmpty()) {
			return new Resultat(false, "aucun compagnon a toi n'est assez pres.");
		}
		return new Resultat(false, "lequel ? " + String.join(" ou ", noms) + " ?");
	}

	/** Un joueur s'en va : ses souvenirs de conversation avec lui. */
	public static void oublier(UUID joueur) {
		derniersAppeles.remove(joueur);
		ordresOrphelins.remove(joueur);
		echecsDAffilee.remove(joueur);
		derniersBravos.remove(joueur);
	}

	/**
	 * Le geste appris que cette phrase contient, ou {@code null}.
	 *
	 * <p>Le mot le plus long gagne, comme partout ailleurs : si on lui a appris
	 * « salut » et « saluts », dire le second ne doit pas declencher le premier.
	 */
	private static String gesteApprisDans(FicheCompagnon fiche, String phrase) {
		String entoure = " " + phrase + " ";
		String meilleur = null;
		int longueur = 0;
		for (Map.Entry<String, String> appris : fiche.motsAppris().entrySet()) {
			String mot = Vocabulaire.pourLaComparaison(appris.getKey());
			if (mot.length() > longueur && entoure.contains(" " + mot + " ")) {
				meilleur = appris.getValue();
				longueur = mot.length();
			}
		}
		return meilleur;
	}

	/**
	 * Il fait le tour qu'on lui a appris.
	 *
	 * <p>Fatigue, il refuse — comme pour la roue, et pour la meme raison : une
	 * bete epuisee qui execute quand meme n'est plus une bete.
	 */
	private static Resultat faireSonTour(CompagnonEntity compagnon, FicheCompagnon fiche,
			String geste) {

		if (fiche.barre(Barre.ENERGIE)
				< Niveaux.progression().action().energieMinimum()) {
			return new Resultat(false, fiche.nom() + " est trop fatigue.");
		}
		compagnon.jouerActionPendant(geste, DUREE_DU_TOUR);
		return new Resultat(true, fiche.nom() + " fait son tour.");
	}

	/**
	 * « Bravo. »
	 *
	 * <p>Le seul ordre qui ne demande rien, et le premier moyen de lui dire
	 * qu'on est content de lui <b>sans le toucher</b>. Jusqu'ici il fallait
	 * s'approcher et caresser ; on peut maintenant le feliciter de loin, en
	 * pleine action, au moment ou ca compte.
	 *
	 * <p>Repete, il ne vaut plus rien : on felicite un geste, pas une syllabe.
	 */
	private static Resultat feliciter(ServerPlayer joueur, CompagnonEntity compagnon,
			FicheCompagnon fiche, Fiches fiches) {

		compagnon.getLookControl().setLookAt(joueur, 30.0F, 30.0F);
		compagnon.jouerActionPendant("@joie", DUREE_DU_TOUR);

		Long dernier = derniersBravos.get(joueur.getUUID());
		long maintenant = System.currentTimeMillis();
		if (dernier != null && maintenant - dernier < ENTRE_DEUX_BRAVOS) {
			// Il est content quand meme — il se retourne et il fait sa mine — mais
			// la complicite ne bouge pas. Le geste garde son sens, pas sa valeur.
			return new Resultat(true, fiche.nom() + " est content.");
		}
		derniersBravos.put(joueur.getUUID(), maintenant);

		fiche.ajouterBarre(Barre.COMPLICITE, CE_QUE_VAUT_UN_BRAVO, Niveaux.progression());
		fiches.setDirty();
		return new Resultat(true, fiche.nom() + " est tres content.");
	}

	/** Ce qu'on affiche au joueur : du francais, pas un nom de constante. */
	private static String libelle(Mode mode) {
		return switch (mode) {
			case SUIT -> "arrive.";
			case ASSIS -> "s'assoit.";
			case COUCHE -> "se couche.";
			case RESTE -> "attend ici.";
		};
	}

	/**
	 * L'ordre que ce joueur a lance sans dire a qui, s'il est encore frais.
	 *
	 * <p>Consomme : un ordre ne s'execute qu'une fois. Sans cela, dire le nom
	 * du compagnon trois fois de suite lui ferait tout relacher a chaque fois.
	 */
	private static CommandeVocale ordreQuiAttendait(ServerPlayer joueur) {
		Attente attente = ordresOrphelins.remove(joueur.getUUID());
		if (attente == null
				|| System.currentTimeMillis() - attente.quand() > MEMOIRE_DE_L_ORDRE) {
			return null;
		}
		return attente.ordre();
	}

	/** Le nom du proprietaire, pour dire a qui la bete obeit vraiment. */
	private static String nomDuMaitre(ServerPlayer qui, FicheCompagnon fiche) {
		ServerPlayer maitre = qui.server.getPlayerList().getPlayer(fiche.proprietaire());
		return maitre == null ? "son maitre" : maitre.getGameProfile().getName();
	}
}
