package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.contenu.Caractere;
import fr.lhdp.compagnon.fiche.Barre;
import fr.lhdp.compagnon.fiche.FicheCompagnon;
import fr.lhdp.compagnon.fiche.Fiches;
import fr.lhdp.compagnon.fiche.Mode;
import fr.lhdp.compagnon.progression.Niveaux;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Random;

/**
 * Ce qu'il pense de l'endroit ou il se trouve.
 *
 * <p>Il a peur de l'orage, il deteste avoir les pattes mouillees, il adore la
 * neige, il s'ennuie quand on le laisse assis trop longtemps.
 *
 * <h2>Rien de tout cela ne s'affiche</h2>
 *
 * <p>Une premiere version l'ecrivait dans une bulle au-dessus de sa tete. C'etait
 * une erreur de fond : <b>un compagnon ne parle pas</b>, et on n'est pas cense
 * savoir ce qu'il a dans la tete. Ce qu'il vit s'inscrit desormais dans son
 * <b>livre</b>, la premiere fois seulement, comme un souvenir. On l'ouvre quand
 * on veut savoir, et on y lit son premier orage, sa premiere neige.
 *
 * <p>Une chance sur six par minute : ce sont des premieres fois, pas un journal
 * de bord minute par minute.
 *
 * <h2>La peur ne coute rien</h2>
 *
 * <p>Ce qu'il aime lui fait gagner un point de complicite. Ce qu'il craint ne lui
 * en fait perdre aucun. Un joueur n'a pas a etre puni parce qu'il a plu, ou parce
 * que le donjon ou l'emmene son cours est sombre.
 */
public final class Humeurs {

	/** Une chance sur tant, chaque minute. */
	private static final int CHANCE = 6;

	/** Ce qu'il gagne en complicite quand quelque chose lui plait. */
	private static final float PLAISIR = 1.0F;

	/** En dessous, il tombe de sommeil. */
	private static final float SEUIL_FATIGUE = 25.0F;

	/** Au-dessus de toutes ces valeurs, tout va bien. */
	private static final float SEUIL_CONTENT = 70.0F;

	/** Le vide sous ses pattes, en blocs, avant qu'il trouve que c'est haut. */
	private static final int VIDE_QUI_INQUIETE = 6;

	/** Jusqu'ou il cherche un autre compagnon qu'il connait. */
	private static final double PORTEE_AMI = 10.0D;

	/** Jusqu'ou il regarde s'il y a quelqu'un, pour savoir s'il s'ennuie. */
	private static final double PORTEE_SOLITUDE = 12.0D;

	private static final Random HASARD = new Random();

	private Humeurs() {
	}

	/**
	 * Quand son proprietaire se reconnecte, il l'attendait.
	 *
	 * <p>C'est la seule pensee qui ne passe pas par le tirage : celle-la doit
	 * arriver a coup sur, parce qu'elle repond a quelque chose que le joueur vient
	 * de faire.
	 */
	public static void enregistrer() {
		ServerPlayConnectionEvents.JOIN.register((reseau, envoi, serveur) -> {
			ServerPlayer joueur = reseau.player;
			// Au tick suivant : a l'instant de la connexion, ses compagnons ne sont
			// pas encore apparus autour de lui.
			serveur.execute(() -> serveur.execute(() -> retrouvailles(serveur, joueur)));
		});
	}

	private static void retrouvailles(MinecraftServer serveur, ServerPlayer joueur) {
		for (FicheCompagnon fiche : Fiches.de(serveur).duProprietaireSansCopie(joueur.getUUID())) {
			CompagnonEntity compagnon = entiteDe(serveur, fiche);
			if (compagnon != null) {
				// Il tourne la tete vers celui qui revient. C'est tout, et c'est assez :
				// un compagnon montre qu'il vous a vu, il ne vous le dit pas.
				compagnon.getLookControl().setLookAt(joueur, 30.0F, 30.0F);
				noterDansLeLivre(fiche, "retrouvailles");
			}
		}
	}

	/**
	 * Il regarde autour de lui et se fait une opinion.
	 *
	 * <p>Appele une fois par minute depuis {@code Vie}, pour les compagnons des
	 * joueurs connectes seulement.
	 *
	 * @return vrai si la fiche a change et merite d'etre sauvegardee
	 */
	public static boolean regarderAutour(MinecraftServer serveur, FicheCompagnon fiche) {
		if (HASARD.nextInt(CHANCE) != 0) {
			return false;
		}
		CompagnonEntity compagnon = entiteDe(serveur, fiche);
		if (compagnon == null) {
			return false;
		}

		String situation = situationDe(compagnon, fiche);
		if (situation == null) {
			return false;
		}
		boolean change = noterDansLeLivre(fiche, situation);

		if (AGREABLES.contains(situation)) {
			fiche.ajouterBarre(Barre.COMPLICITE, PLAISIR, Niveaux.progression());
			change = true;
		}
		return change;
	}

	/** Les situations qui rapprochent. Les autres ne coutent rien : voir la note de classe. */
	private static final List<String> AGREABLES = List.of("neige", "content", "ami");

	/**
	 * Ce qui le marque le plus, ici et maintenant.
	 *
	 * <p>L'ordre compte : c'est une priorite, pas une liste. Un dragonnet trempe
	 * sous l'orage pense a l'orage — le plus fort l'emporte, comme pour nous.
	 */
	private static String situationDe(CompagnonEntity compagnon, FicheCompagnon fiche) {
		ServerLevel niveau = (ServerLevel) compagnon.level();
		BlockPos ou = compagnon.blockPosition();

		if (compagnon.memoireCourte().contient(MemoireCourte.Signal.ORAGE)) {
			return "orage";
		}
		if (compagnon.memoireCourte().contient(MemoireCourte.Signal.EAU)) {
			return "eau";
		}
		if (compagnon.memoireCourte().contient(MemoireCourte.Signal.NEIGE)) {
			return "neige";
		}
		if (compagnon.memoireCourte().contient(MemoireCourte.Signal.PLUIE)) {
			return "pluie";
		}
		if (compagnon.memoireCourte().contient(MemoireCourte.Signal.OBSCURITE)) {
			return "sombre";
		}
		if (leVideEnDessous(niveau, ou)) {
			return "hauteur";
		}
		if (fiche.barre(Barre.ENERGIE) < SEUIL_FATIGUE) {
			return "fatigue";
		}
		if (unAmiEstLa(compagnon)) {
			return "ami";
		}
		if (ilSEnnuie(compagnon, fiche)) {
			return "ennui";
		}
		if (toutVaBien(fiche)) {
			return "content";
		}
		return null;
	}

	/**
	 * Y a-t-il du vide sous ses pattes ?
	 *
	 * <p>Six lectures de bloc, une fois par minute, et seulement quand le tirage
	 * est tombe : c'est negligeable. On ne lance surtout pas de rayon, qui
	 * couterait bien plus cher pour la meme reponse.
	 */
	private static boolean leVideEnDessous(ServerLevel niveau, BlockPos ou) {
		for (int i = 1; i <= VIDE_QUI_INQUIETE; i++) {
			if (!niveau.getBlockState(ou.below(i)).isAir()) {
				return false;
			}
		}
		return true;
	}

	/** Un autre compagnon qu'il connait est-il a portee ? */
	private static boolean unAmiEstLa(CompagnonEntity compagnon) {
		CompagnonEntity ami = compagnon.amiMemorise();
		return ami != null && compagnon.distanceToSqr(ami) <= PORTEE_AMI * PORTEE_AMI;
	}

	/**
	 * Il s'ennuie quand on l'a pose quelque part et que personne ne passe.
	 *
	 * <p>Un compagnon qui suit son maitre ne s'ennuie jamais : il a de quoi faire.
	 */
	private static boolean ilSEnnuie(CompagnonEntity compagnon, FicheCompagnon fiche) {
		if (fiche.mode() == Mode.SUIT) {
			return false;
		}
		Entity quelquUn = compagnon.level().getNearestPlayer(compagnon, PORTEE_SOLITUDE);
		return quelquUn == null;
	}

	private static boolean toutVaBien(FicheCompagnon fiche) {
		for (Barre barre : Barre.values()) {
			if (fiche.barre(barre) < SEUIL_CONTENT) {
				return false;
			}
		}
		return fiche.bobo().isEmpty();
	}

	/**
	 * Ce qu'il a vecu entre dans son livre, la premiere fois seulement.
	 *
	 * <p>Cela s'affichait avant dans une bulle au-dessus de sa tete. C'etait une
	 * erreur : un compagnon ne parle pas, et on n'est pas cense lire dans ses
	 * pensees. Son livre, lui, est fait pour ca — on l'ouvre quand on veut savoir,
	 * et on y trouve le premier orage, la premiere neige, le premier ennui.
	 *
	 * @return vrai si c'etait la premiere fois
	 */
	private static boolean noterDansLeLivre(FicheCompagnon fiche, String situation) {
		return fiche.marquer("premiere_fois." + situation, System.currentTimeMillis());
	}

	private static CompagnonEntity entiteDe(MinecraftServer serveur, FicheCompagnon fiche) {
		ServerLevel niveau = serveur.getLevel(fiche.dimension());
		if (niveau == null) {
			return null;
		}
		return niveau.getEntity(fiche.id()) instanceof CompagnonEntity compagnon ? compagnon : null;
	}

}
