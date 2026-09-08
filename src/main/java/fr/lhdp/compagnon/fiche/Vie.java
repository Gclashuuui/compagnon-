package fr.lhdp.compagnon.fiche;

import fr.lhdp.compagnon.mission.Moule;
import fr.lhdp.compagnon.mission.Compteurs;
import fr.lhdp.compagnon.mission.Carnet;
import fr.lhdp.compagnon.contenu.Bobo;
import fr.lhdp.compagnon.contenu.Contenu;
import fr.lhdp.compagnon.contenu.Donnable;
import fr.lhdp.compagnon.objet.Objets;
import fr.lhdp.compagnon.progression.Niveaux;
import fr.lhdp.compagnon.competence.Competence;
import fr.lhdp.compagnon.competence.Competences;
import fr.lhdp.compagnon.progression.Progression;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import fr.lhdp.compagnon.entite.Manies;
import fr.lhdp.compagnon.entite.CompagnonEntity;
import fr.lhdp.compagnon.entite.Humeurs;
import fr.lhdp.compagnon.entite.Voisinages;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Random;

/**
 * La derive des barres et les petits accidents, minute par minute.
 *
 * <p><b>Rien ne se degrade quand le proprietaire est deconnecte.</b> Un joueur
 * qui revient apres trois semaines de vacances retrouve son compagnon exactement
 * comme il l'a laisse : personne n'est puni d'avoir eu une vie.
 *
 * <p>C'est aussi ce qui rend cette classe bon marche a l'echelle d'un chateau :
 * on part des <b>joueurs connectes</b>, jamais des fiches. Sur un serveur ou la
 * plupart des proprietaires sont absents, leurs fiches ne sont pas meme lues.
 */
public final class Vie {

	/** Une minute de jeu. C'est l'unite des taux du fichier de progression. */
	public static final int PERIODE = 20 * 60;

	/**
	 * Decalage volontaire par rapport a la periode d'apparition.
	 *
	 * <p>Sans lui les deux traitements tomberaient sur le meme tick — la periode
	 * d'apparition divise celle-ci — et additionneraient leurs pics.
	 */
	private static final int DECALAGE = 7;

	private static final float MINUTES_PAR_HEURE = 60.0F;

	/**
	 * Une manie tous les combien, en minutes de jeu, en moyenne.
	 *
	 * <p>Trois heures. Assez long pour qu'une manie arrive <b>apres</b> qu'on a
	 * commence a connaitre la bete, ce qui est tout l'interet : une habitude
	 * qui existe des la premiere minute n'a rien d'une habitude.
	 */
	private static final int MINUTES_AVANT_UNE_MANIE = 180;

	/**
	 * Ce qu'un « jour de jeu » vaut en ticks passes ensemble.
	 *
	 * <p>Vingt minutes, comme un jour dans Minecraft. Trois de ces jours-la
	 * separent deux renouvellements de missions, soit une heure de jeu reelle
	 * en leur compagnie.
	 */
	private static final long TICKS_PAR_JOUR_DE_JEU = 20L * 60L * 20L;

	private static final java.util.Random HASARD_DU_CARACTERE = new java.util.Random();

	/** Chance de rever, a chaque minute passee couche. Valeur inventee. */
	private static final float CHANCE_DE_REVER = 0.03F;

	/** En dessous de cette faim, il commence a reclamer. Valeur inventee. */
	private static final float SEUIL_DE_FAIM = 40.0F;

	/**
	 * Combien de minutes d'affilee il reclame, quand il reclame.
	 *
	 * <p>Il ne reclame <b>pas en continu</b>. L'objet reste affiche trois minutes,
	 * puis disparait le temps d'un silence. C'est tout l'inverse de ce que faisait
	 * la premiere version : des que la barre passait sous le seuil, le biscuit
	 * restait plante au-dessus de sa tete jusqu'au repas, parfois pendant des
	 * heures. On finissait par ne plus le voir, ou par le trouver penible.
	 */
	private static final int MINUTES_DE_DEMANDE = 3;

	/**
	 * Le silence entre deux demandes, quand il commence tout juste a avoir faim.
	 *
	 * <p>Douze minutes : assez rare pour qu'on le remarque quand ca arrive.
	 */
	private static final int SILENCE_LONG = 12;

	/**
	 * Le silence entre deux demandes, quand il est affame.
	 *
	 * <p>Deux minutes : insistant, sans etre colle. C'est la pente entre les deux
	 * qui fait le travail — il se rappelle a vous de plus en plus souvent, et on
	 * comprend sans qu'aucun chiffre ne s'affiche.
	 */
	private static final int SILENCE_COURT = 2;

	private static final Random HASARD = new Random();

	private Vie() {
	}

	public static void enregistrer() {
		ServerTickEvents.END_SERVER_TICK.register(Vie::tick);
	}

	private static void tick(MinecraftServer serveur) {
		if (serveur.getTickCount() % PERIODE != DECALAGE) {
			return;
		}

		Progression table = Niveaux.progression();
		Fiches fiches = Fiches.de(serveur);
		boolean quelqueChoseABouge = false;

		for (ServerPlayer proprietaire : serveur.getPlayerList().getPlayers()) {
			for (FicheCompagnon fiche : fiches.duProprietaireSansCopie(proprietaire.getUUID())) {
				quelqueChoseABouge |= deriver(fiche, table);
				quelqueChoseABouge |= peutEtreUnBobo(fiche, table, proprietaire);
				quelqueChoseABouge |= peutEtreUnReve(serveur, fiche);
				quelqueChoseABouge |= Humeurs.regarderAutour(serveur, fiche);
				quelqueChoseABouge |= Voisinages.seRemarquer(serveur, fiche);
			quelqueChoseABouge |= sonCaractere(fiche);
			quelqueChoseABouge |= compterLaBalade(fiche, proprietaire);
			quelqueChoseABouge |= Compteurs.releverLaMinute(serveur, fiche);
			quelqueChoseABouge |= sesMissions(serveur, fiche, proprietaire, table);
			quelqueChoseABouge |= fr.lhdp.compagnon.progression.Montee.regarder(
					fiche, proprietaire, table);

			// APRES la montee, jamais avant : le titre << il a grandi >> doit
			// arriver sur celui du niveau, pas se faire ecraser par lui.
			quelqueChoseABouge |= fr.lhdp.compagnon.progression.Croissance.regarder(
					fiche, proprietaire, table);
				montrerCeQuIlReclame(serveur, fiche);
			}
		}

		if (quelqueChoseABouge) {
			fiches.setDirty();
		}
	}

	/**
	 * Fait avancer son carnet de missions, et paie ce qui est accompli.
	 *
	 * <h2>Ce que rapporte une mission</h2>
	 *
	 * <p>De l'experience, et rien d'autre : ni objet, ni bonus, ni raccourci.
	 * Une mission doit donner une raison de vivre quelque chose avec sa bete,
	 * jamais un moyen d'aller plus vite.
	 *
	 * <p>Et elle passe dans le journal : une fois accomplie, elle cesse d'etre
	 * une case cochee pour devenir un souvenir date, que le livre relira des
	 * mois plus tard sans qu'on se rappelle que c'etait une mission.
	 *
	 * <p>Le message passe par la barre d'action et jamais par le chat : sur un
	 * serveur a mille joueurs, un mod qui parle est un mod qu'on desinstalle.
	 */
	private static boolean sesMissions(MinecraftServer serveur, FicheCompagnon fiche,
			ServerPlayer proprietaire, Progression table) {

		// Le jour de jeu se compte en temps passe ensemble, jamais en jours de
		// calendrier : quelqu'un qui joue deux heures le samedi ne doit pas rater
		// onze missions sur douze.
		long jour = fiche.ticksEnsemble() / TICKS_PAR_JOUR_DE_JEU;
		Carnet.Avancee avancee = Carnet.avancer(fiche, jour, HASARD_DU_CARACTERE);

		for (Moule moule : avancee.accomplies()) {
			// On lit le cran AVANT de cocher : une fois cochee, la ligne ne dit
			// plus a quelle difficulte elle avait ete proposee.
			int gagne = 0;
			for (Carnet.Ligne ligne : Carnet.lire(fiche)) {
				if (ligne.moule() == moule) {
					gagne = moule.experience(ligne.cran());
					break;
				}
			}
			Carnet.cocher(fiche, moule);
			fiche.setXp(fiche.xp() + gagne);
			fiche.marquer("mission." + moule.id(), System.currentTimeMillis());
			proprietaire.displayClientMessage(Component.translatable(
				"mission.compagnon.accomplie", fiche.nom(), gagne), true);
			// Et ca se voit sur la bete : sans ca, une mission se termine dans un
			// coin de l'ecran et on ne sait meme pas laquelle de ses betes l'a faite.
			faireScintiller(serveur, fiche);
		}
		return avancee.change();
	}

	/** Un soupir de fumee sur la bete, si elle est la. */
	private static void soupirer(MinecraftServer serveur, FicheCompagnon fiche) {
		net.minecraft.server.level.ServerLevel niveau = serveur.getLevel(fiche.dimension());
		if (niveau != null && niveau.getEntity(fiche.id())
				instanceof fr.lhdp.compagnon.entite.CompagnonEntity compagnon) {
			fr.lhdp.compagnon.entite.Etincelles.soupir(compagnon);
		}
	}

	/** Fait scintiller la bete, si elle est la pour qu'on la voie. */
	private static void faireScintiller(MinecraftServer serveur, FicheCompagnon fiche) {
		net.minecraft.server.level.ServerLevel niveau = serveur.getLevel(fiche.dimension());
		if (niveau != null && niveau.getEntity(fiche.id())
				instanceof fr.lhdp.compagnon.entite.CompagnonEntity compagnon) {
			fr.lhdp.compagnon.entite.Etincelles.progres(compagnon, 12);
		}
	}

	/**
	 * Une minute de plus passee a marcher ensemble.
	 *
	 * <p>Le compteur des balades etait declare et personne ne l'alimentait :
	 * une constante morte. Il compte maintenant les minutes ou la bete est
	 * dehors pendant que son maitre se deplace — c'est ce chiffre-la que le
	 * livre pourra un jour raconter autrement qu'en nombre.
	 *
	 * <p>Une minute compte pour une, quelle que soit la distance : compter des
	 * blocs demanderait de suivre la position a chaque tick, pour un chiffre
	 * qui ne dirait rien de plus.
	 */
	private static boolean compterLaBalade(FicheCompagnon fiche, ServerPlayer proprietaire) {
		// IL EST DEHORS AVEC TOI : IL NE T'ATTEND PAS.
		//
		// C'est ce qui fait que la prochaine sortie ne fetera pas des
		// retrouvailles imaginaires. Note a chaque passage, donc a la minute :
		// on mesure des jours d'absence, la minute pres suffit largement.
		if (fiche.sorti()) {
			fiche.noterUneVisite(System.currentTimeMillis());
		}
		if (!fiche.sorti()
			|| proprietaire.getDeltaMovement().horizontalDistanceSqr() < 0.002D) {
			return false;
		}
		fiche.incrementer(FicheCompagnon.BALADES);
		return true;
	}

	/**
	 * Ce qui fait qu'il est lui : son defaut, puis ses manies.
	 *
	 * <p>Le defaut arrive tout de suite — c'est ce qui doit distinguer deux
	 * betes des la premiere heure. Les manies, elles, se meritent : une chance
	 * toutes les trois heures de jeu environ, et jamais plus de deux dans une
	 * vie.
	 *
	 * <p>Rien ne se passe pour un joueur deconnecte : cette boucle ne parcourt
	 * que les compagnons de gens presents. C'est la meme regle que partout
	 * ailleurs dans cette classe, et elle vaut aussi pour ce qui est agreable.
	 */
	private static boolean sonCaractere(FicheCompagnon fiche) {
		boolean bouge = Manies.donnerUnDefaut(fiche, HASARD_DU_CARACTERE);
		if (HASARD_DU_CARACTERE.nextInt(MINUTES_AVANT_UNE_MANIE) == 0) {
			bouge |= Manies.peutEtreUneManie(fiche, HASARD_DU_CARACTERE);
		}
		return bouge;
	}

	/** Fait glisser chaque barre de son taux, selon qu'il agit ou qu'il se repose. */
	private static boolean deriver(FicheCompagnon fiche, Progression table) {
		boolean auRepos = fiche.mode().pose();
		boolean bouge = false;

		for (Barre barre : Barre.values()) {
			Progression.ReglageBarre reglage = table.barres().get(barre);
			if (reglage == null) {
				continue;
			}
			float taux = reglage.taux(auRepos);

			// SES COMPETENCES RALENTISSENT SA DERIVE.
			//
			// Uniquement quand la barre DESCEND : une competence qui reduit la faim
			// ne doit pas reduire aussi la vitesse a laquelle l'energie remonte
			// pendant qu'il dort. Le meme facteur ferait exactement le contraire de
			// ce qu'on lui demande.
			if (taux < 0.0F) {
				taux *= Competences.facteur(fiche.competences(), cleDe(barre));
			}

			if (taux != 0.0F) {
				fiche.ajouterBarre(barre, taux, table);
				bouge = true;
			}
		}
		return bouge;
	}

	/**
	 * La cle d'effet qui correspond a une barre, ou une cle sans effet.
	 *
	 * <p>Seules la faim et l'energie se ralentissent. La sante et la complicite
	 * ne derivent pas d'elles-memes — les toucher ici n'aurait aucun sens.
	 */
	private static String cleDe(Barre barre) {
		return switch (barre) {
			case FAIM -> Competence.FAIM;
			case ENERGIE -> Competence.ENERGIE;
			default -> "";
		};
	}

	/**
	 * Il montre ce dont il a envie quand il a faim.
	 *
	 * <p>Une image vaut mieux qu'un chiffre dans un livre : le joueur voit un
	 * biscuit dans la bulle au-dessus de sa tete et comprend tout de suite, sans
	 * rien ouvrir et sans taper une commande. C'est desormais la SEULE chose que
	 * la bulle sait dire — un compagnon ne parle pas.
	 *
	 * <p>Il ne reclame pas en continu, et pas toujours la meme chose : voir
	 * {@link #cestLeMoment} et {@link #envieDuJour}.
	 */
	private static void montrerCeQuIlReclame(MinecraftServer serveur, FicheCompagnon fiche) {
		ServerLevel niveau = serveur.getLevel(fiche.dimension());
		if (niveau == null || !(niveau.getEntity(fiche.id()) instanceof CompagnonEntity compagnon)) {
			return;
		}

		float faim = fiche.barre(Barre.FAIM);
		if (faim >= SEUIL_DE_FAIM || !cestLeMoment(serveur, fiche, faim)) {
			compagnon.avoirEnvieDe("");
			return;
		}

		Donnable aliment = envieDuJour(fiche);
		compagnon.avoirEnvieDe(aliment == null ? "" : Objets.envie(aliment));
	}

	/**
	 * Est-ce le moment de reclamer ?
	 *
	 * <p>Il alterne : trois minutes ou il montre ce qu'il veut, puis un silence. Plus
	 * il a faim, plus le silence est court — de douze minutes quand la barre vient
	 * de passer sous le seuil, a deux minutes quand elle est a zero.
	 *
	 * <p>Le decalage tire de son identifiant est important a mille compagnons : sans
	 * lui, tout le chateau reclamerait a manger a la meme minute, et le serveur
	 * enverrait mille paquets d'un coup au lieu de les etaler.
	 */
	private static boolean cestLeMoment(MinecraftServer serveur, FicheCompagnon fiche, float faim) {
		float part = Math.max(0.0F, Math.min(1.0F, faim / SEUIL_DE_FAIM));
		int silence = Math.round(SILENCE_COURT + part * (SILENCE_LONG - SILENCE_COURT));

		int cycle = MINUTES_DE_DEMANDE + silence;
		long minute = serveur.getTickCount() / PERIODE;
		long decalage = Math.floorMod(fiche.id().hashCode(), cycle);

		return Math.floorMod(minute + decalage, cycle) < MINUTES_DE_DEMANDE;
	}

	/**
	 * Ce dont il a envie aujourd'hui.
	 *
	 * <p>Tire au sort, mais <b>toujours le meme pour la journee</b> : le hasard est
	 * seme avec son identifiant et le jour en cours. Sans cela, il reclamerait un
	 * biscuit, puis un poisson deux minutes plus tard, puis autre chose encore —
	 * on ne saurait jamais quoi lui apporter.
	 */
	private static Donnable envieDuJour(FicheCompagnon fiche) {
		return Contenu.alimentAuHasard(new Random(fiche.id().hashCode() * 31L + fiche.jour()));
	}

	/**
	 * Il reve, quand il dort.
	 *
	 * <p>Rien de mecanique : cela ecrit une ligne dans son historique, et c'est
	 * tout. C'est ce qui donne au livre l'air d'un carnet tenu a la main plutot
	 * que d'un tableau de chiffres.
	 *
	 * <p>Il ne reve que <b>couche</b>, et jamais deux fois le meme reve : un
	 * souvenir ne se repete pas.
	 */
	private static boolean peutEtreUnReve(MinecraftServer serveur, FicheCompagnon fiche) {
		if (fiche.mode() != Mode.COUCHE) {
			return false;
		}
		if (HASARD.nextFloat() >= CHANCE_DE_REVER) {
			return false;
		}
		String reve = Contenu.reveAuHasard(HASARD);
		if (reve == null) {
			return false;
		}
		if (!fiche.marquer("reve." + reve, System.currentTimeMillis())) {
			// Il a deja fait ce reve : on n'en reparle pas.
			return false;
		}

		// Le reve ne s'affiche plus : il s'ecrit. C'est tout l'interet du livre.
		return true;
	}


	/**
	 * Tire au sort un petit accident.
	 *
	 * <p>La sante ne baisse pas au combat : elle baisse ici, quelques fois par
	 * semaine. Un seul bobo a la fois — tant qu'il n'est pas soigne, il ne peut
	 * pas lui en arriver un deuxieme.
	 */
	private static boolean peutEtreUnBobo(FicheCompagnon fiche, Progression table,
			ServerPlayer proprietaire) {

		if (fiche.aUnBobo() || table.bobosParHeureDeJeu() <= 0.0F) {
			return false;
		}

		// Une bete solide tombe moins souvent malade. Le facteur multiplie le
		// risque : a 0,5 elle attrape deux fois moins de bobos, jamais zero — une
		// competence qui supprimerait entierement les bobos supprimerait aussi
		// tout l'interet des soins.
		float chanceParMinute = table.bobosParHeureDeJeu() / MINUTES_PAR_HEURE
				* Competences.facteur(fiche.competences(), Competence.BOBOS);
		if (HASARD.nextFloat() >= chanceParMinute) {
			return false;
		}

		Bobo bobo = Contenu.boboAuHasard(HASARD);
		if (bobo == null) {
			return false;
		}

		long maintenant = System.currentTimeMillis();
		fiche.setBobo(bobo.id(), maintenant);
		fiche.ajouterBarre(Barre.SANTE, bobo.sante(), table);
		fiche.marquer("premier_bobo", maintenant);
		// Un petit soupir de fumee : il vient de se faire mal, et on le voit
		// avant de lire quoi que ce soit.
		soupirer(proprietaire.server, fiche);

		proprietaire.sendSystemMessage(Component
				.literal(fiche.nom() + " : " + bobo.nom() + ". " + bobo.description())
				.withStyle(ChatFormatting.YELLOW));

		return true;
	}
}
