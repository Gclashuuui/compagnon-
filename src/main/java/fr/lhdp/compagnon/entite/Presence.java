package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.espece.Espece;
import fr.lhdp.compagnon.espece.Especes;
import fr.lhdp.compagnon.espece.Longueurs;
import net.minecraft.world.entity.monster.Monster;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Les petits gestes qui font qu'il est la.
 *
 * <h2>Pourquoi ils ne sont pas des buts</h2>
 *
 * <p>Un but de l'IA prend la main : il coupe la navigation, il occupe une place
 * dans la liste, il empeche les autres de tourner. Tout ce qui est ici est plus
 * petit que ca — un regard rendu, un frisson, un ebrouement. Ce sont des gestes
 * d'une seconde qui n'interrompent rien et qui se jouent <b>par-dessus</b> ce
 * qu'il est en train de faire.
 *
 * <p>Ils passent tous par {@link Attention}, qui decide lequel a le droit de se
 * jouer maintenant. Sans elle, ils se declencheraient tous ensemble et une bete
 * a qui on a ajoute dix habitudes ferait quatre gestes a la fois.
 *
 * <h2>Le cout</h2>
 *
 * <p>Rien ne cherche d'entite : le proprietaire est deja connu de la bete, le
 * biome se lit dans le chunk deja charge, et la pluie est un booleen du monde.
 * L'ensemble tourne un tick sur dix.
 */
public final class Presence {

	private Presence() {
	}

	/** Un tick sur dix suffit largement pour des gestes d'une seconde. */
	public static final int TOUS_LES = 10;

	/** Au-dela, il ne remarque plus qu'on le regarde. */
	private static final double PORTEE_DU_REGARD = 8.0D;

	/**
	 * A quel point il faut le viser pour qu'il se sente regarde.
	 *
	 * <p>C'est un cosinus : 0,985 fait un cone d'environ dix degres. Assez large
	 * pour qu'on n'ait pas a viser au pixel, assez etroit pour qu'on ne le
	 * declenche pas en regardant vaguement dans sa direction.
	 */
	private static final double PRECISION_DU_REGARD = 0.985D;

	/** Combien de temps il faut le fixer avant qu'il ne reponde, en ticks. */
	private static final int AVANT_DE_REPONDRE = 30;

	/** Delais avant de refaire le meme geste, en ticks. */
	private static final int AVANT_DE_REREGARDER = 20 * 25;
	private static final int AVANT_DE_REFRISSONNER = 20 * 40;
	private static final int AVANT_DE_SE_REBROUER = 20 * 30;

	/** Une manie ne revient pas avant une minute et demie. */
	private static final int AVANT_DE_REFAIRE_UNE_MANIE = 20 * 90;

	/** En moyenne une proposition par minute, examinee deux fois par seconde. */
	/** Les roles disponibles, dont chaque espece peut remplir tout ou partie. */
	private static final List<String> GESTES_NATURELS =
			List.of("ambiance", "ambiance_2", "ambiance_3", "ambiance_4",
					"micro_cligne", "micro_cligne_double",
					"micro_oreille_gauche", "micro_oreille_droite",
					"micro_regard_gauche", "micro_regard_droite",
					"micro_tete_gauche", "micro_tete_droite",
					"micro_appui_gauche", "micro_appui_droite",
					"micro_queue_gauche", "micro_queue_droite",
					"micro_queue_repose", "micro_aile_replace",
					"micro_regarde_derriere", "micro_hesite");

	/** Un gourmand remendie au bout d'une minute. */
	private static final int AVANT_DE_REMENDIER = 20 * 60;

	/** En dessous, il peut chercher une attention plutot que rester indifferent. */
	private static final float COMPLICITE_QUI_RECLAME = 35.0F;

	/** Une demande de caresse doit rester un petit evenement, pas devenir un tic. */
	private static final int AVANT_DE_RECLAMER_UNE_CARESSE = 20 * 120;

	/** A quelle distance il faut que son maitre soit pour qu'il le previenne. */
	private static final double PORTEE_DU_MAITRE = 12.0D;

	/** Au-dessus de cette lumiere, il fait jour : rien a signaler. */
	private static final int CLAIR = 8;

	/** Combien de temps une creature revelee reste visible a travers les murs. */
	private static final int TICKS_REVELES = 20 * 6;

	/** Entre deux alertes. Trois secondes : le temps de comprendre. */
	private static final int AVANT_DE_REPREVENIR = 20 * 3;
	private static final int AVANT_DE_REAGIR_ORAGE = 20 * 120;
	private static final int AVANT_DE_REDECOUVRIR_NEIGE = 20 * 180;

	/**
	 * Joue au plus un petit geste. A appeler cote serveur uniquement.
	 *
	 * <p>L'ordre des tests est l'ordre d'importance : ce qui vient du maitre
	 * passe avant ce qui vient du monde. Le premier qui prend la main a gagne,
	 * les autres attendront dix ticks.
	 */
	/**
	 * Les competences de cette bete, ou rien si sa fiche est introuvable.
	 *
	 * <p>La fiche fait autorite, comme partout : l'entite n'est qu'un affichage,
	 * et une competence prise pendant qu'elle etait rangee doit compter des
	 * qu'elle ressort.
	 */
	private static java.util.Set<String> competencesDe(CompagnonEntity compagnon) {
		if (compagnon.ficheId() == null
			|| !(compagnon.level() instanceof net.minecraft.server.level.ServerLevel niveau)) {
			return java.util.Set.of();
		}
		fr.lhdp.compagnon.fiche.FicheCompagnon fiche =
			fr.lhdp.compagnon.fiche.Fiches.de(niveau.getServer()).get(compagnon.ficheId());
		return fiche == null ? java.util.Set.of() : fiche.competences();
	}

	public static void jouer(CompagnonEntity compagnon, Attention attention) {
		LivingEntity maitre = compagnon.getOwner();
		// Une menace est la seule perception autorisée à passer avant un geste
		// d'ambiance ou un besoin. La priorité URGENCE tranche ensuite proprement.
		if (compagnon.memoireCourte().contient(MemoireCourte.Signal.MENACE)
				&& ilTePrevient(compagnon, maitre, attention)) {
			return;
		}
		// peutFaireUnPetitGeste, et surtout pas estLibre : celle-la exige le mode
		// « reste », or un compagnon passe l'essentiel de son temps a suivre.
		if (!compagnon.peutFaireUnPetitGeste()) {
			return;
		}

		if (maitre != null && !maitre.isSpectator()) {
			if (ilTeRegarde(compagnon, maitre, attention)) {
				return;
			}
		}
		if (ilReclameUneCaresse(compagnon, maitre, attention)) {
			return;
		}
		if (ilReclame(compagnon, maitre, attention)) {
			return;
		}
		if (uneManie(compagnon, maitre, attention)) {
			return;
		}
		if (uneEmotionInterieure(compagnon, attention)) {
			return;
		}
		if (unRituelQuotidien(compagnon)) {
			return;
		}
		if (unGesteNaturel(compagnon, attention)) {
			return;
		}
		leMonde(compagnon, attention);
	}

	/** L'ennui et le retour au calme deviennent visibles sans barre supplémentaire. */
	private static boolean uneEmotionInterieure(CompagnonEntity compagnon,
			Attention attention) {
		EtatInterieur interieur = compagnon.etatInterieur();
		if (interieur.ennui() >= 0.68F
				&& attention.permet("emotion.ennui", 20 * 90)) {
			return compagnon.jouerActionPendant("@" + Espece.ENNUI, 30,
					PrioriteAction.AMBIANCE);
		}
		boolean mondeCalme = !compagnon.memoireCourte().contient(MemoireCourte.Signal.MENACE)
				&& !compagnon.memoireCourte().contient(MemoireCourte.Signal.ORAGE)
				&& !compagnon.memoireCourte().contient(MemoireCourte.Signal.MAITRE_EN_DANGER);
		if (mondeCalme && interieur.stress() >= 0.28F && interieur.stress() <= 0.62F
				&& attention.permet("emotion.detente", 20 * 120)) {
			return compagnon.jouerActionPendant("@" + Espece.DETENTE_APRES_ALERTE,
					30, PrioriteAction.AMBIANCE);
		}
		return false;
	}

	/**
	 * Le matin et le soir deviennent des moments reconnaissables, sans horloge
	 * supplémentaire et sans recherche dans le monde. Les rôles spécialisés sont
	 * facultatifs ; les anciens gestes servent de repli jusqu'à leur livraison.
	 */
	private static boolean unRituelQuotidien(CompagnonEntity compagnon) {
		RythmeQuotidien.Moment moment = compagnon.rythmeQuotidien().prochain(
				compagnon.level().getDayTime(), compagnon.getUUID().hashCode());
		String role = switch (moment) {
			case MATIN -> SceneAffectiveGoal.premierRoleDisponible(compagnon,
					Espece.RITUEL_MATIN, "ambiance_3", Espece.JOYEUX);
			case SOIR -> SceneAffectiveGoal.premierRoleDisponible(compagnon,
					Espece.RITUEL_SOIR, "ambiance_2", Espece.ECOUTE);
			case AUCUN -> null;
		};
		return role != null && compagnon.jouerActionPendant("@" + role, 30,
				PrioriteAction.AMBIANCE);
	}

	/**
	 * Un mouvement sans raison utile : renifler, regarder le ciel, s'etirer.
	 *
	 * <p>La liste ne contient que des roles. Une espece qui ne les remplit pas ne
	 * change absolument pas ; celle qui en possede plusieurs varie sans que le
	 * code connaisse le nom d'une seule animation. Le tirage est rare et ne fait
	 * aucune recherche dans le monde.
	 */
	private static boolean unGesteNaturel(CompagnonEntity compagnon, Attention attention) {
		if (compagnon.getRandom().nextInt(
				compagnon.profilCerveau().chanceGesteNaturel()) != 0) {
			return false;
		}
		Espece espece = Especes.get(compagnon.espece());
		if (espece == null) {
			return false;
		}
		java.util.ArrayList<String> possibles = new java.util.ArrayList<>(GESTES_NATURELS.size());
		for (String role : GESTES_NATURELS) {
			if (espece.reaction(role) != null) {
				possibles.add(role);
			}
		}
		if (possibles.isEmpty() || !attention.permet("naturel", 20 * 35)) {
			return false;
		}
		String role = possibles.get(compagnon.getRandom().nextInt(possibles.size()));
		String animation = espece.reaction(role);
		compagnon.jouerActionPendant("@" + role, Longueurs.de(animation));
		return true;
	}

	/**
	 * Son geste a lui, si la situation s'y prete.
	 *
	 * <p>Deux au plus par bete, et chacune avec son declencheur. C'est ce qui
	 * fait qu'on finit par guetter le geste : il revient, mais seulement dans
	 * une situation precise, et pas chez le voisin.
	 *
	 * <p>Le delai avant de la refaire est long — une minute et demie. Une manie
	 * qu'on voit toutes les dix secondes cesse d'etre une manie pour devenir un
	 * tic d'animation.
	 */
	private static boolean uneManie(CompagnonEntity compagnon, LivingEntity maitre,
			Attention attention) {

		if (compagnon.manies().isEmpty()) {
			return false;
		}
		Level niveau = compagnon.level();
		long heure = niveau.getDayTime() % 24000L;
		boolean aube = heure >= 22800L || heure < 1200L;
		boolean pluie = niveau.isRainingAt(compagnon.blockPosition().above());
		boolean maitrePose = maitre != null
				&& compagnon.distanceToSqr(maitre) <= PORTEE_DU_REGARD * PORTEE_DU_REGARD
				&& maitre.getDeltaMovement().horizontalDistanceSqr() < 0.002D;

		for (String id : compagnon.manies()) {
			Manies.Manie manie = Manies.trouver(id);
			if (manie == null) {
				continue;
			}
			boolean cestLeMoment = switch (manie.quand()) {
				case AUBE -> aube;
				case PLUIE -> pluie;
				case MAITRE_POSE -> maitrePose;
				// UN BAVARD SE PARLE TROIS FOIS PLUS SOUVENT. C'est son defaut, et
				// c'est la seule ligne qu'il aura fallu pour qu'on le remarque.
				case SANS_RAISON -> compagnon.getRandom().nextInt(
						Manies.BAVARD.equals(compagnon.defaut()) ? 13 : 40) == 0;
			};
			if (cestLeMoment && attention.permet("manie." + id, AVANT_DE_REFAIRE_UNE_MANIE)) {
				compagnon.jouerActionPendant(manie.geste(), manie.ticks());
				return true;
			}
		}
		return false;
	}

	/**
	 * Le gourmand mendie.
	 *
	 * <p>Des que son maitre s'arrete a portee, il se plante devant lui et
	 * attend. Il n'obtient rien de special s'il est nourri : c'est un defaut,
	 * pas une mecanique — il le fera meme le ventre plein, et c'est tout
	 * l'interet.
	 */
	private static boolean ilReclame(CompagnonEntity compagnon, LivingEntity maitre,
			Attention attention) {

		if (maitre == null || !Manies.GOURMAND.equals(compagnon.defaut())) {
			return false;
		}
		if (compagnon.distanceToSqr(maitre) > PORTEE_DU_REGARD * PORTEE_DU_REGARD
				|| maitre.getDeltaMovement().horizontalDistanceSqr() >= 0.002D) {
			return false;
		}
		if (!attention.permet("mendie", AVANT_DE_REMENDIER)) {
			return false;
		}
		compagnon.getLookControl().setLookAt(maitre, 30.0F, 30.0F);
		compagnon.jouerActionPendant("@ecoute", 25);
		return true;
	}

	/**
	 * Quand le lien est encore fragile, il cherche parfois le regard de son maitre.
	 *
	 * <p>Il ne fait apparaitre ni texte ni ordre : il se tourne, incline la tete et,
	 * s'il est naturellement calin, fait quelques pas. Le joueur apprend ainsi a
	 * lire son besoin avant meme d'ouvrir le carnet. Deux minutes entre deux
	 * demandes, et uniquement quand le maitre est immobile, gardent le geste rare.
	 */
	private static boolean ilReclameUneCaresse(CompagnonEntity compagnon,
			LivingEntity maitre, Attention attention) {

		if (maitre == null || compagnon.complicite() > COMPLICITE_QUI_RECLAME
				|| compagnon.distanceToSqr(maitre) > PORTEE_DU_REGARD * PORTEE_DU_REGARD
				|| maitre.getDeltaMovement().horizontalDistanceSqr() >= 0.002D) {
			return false;
		}
		if (!attention.permet("reclame_caresse", AVANT_DE_RECLAMER_UNE_CARESSE)) {
			return false;
		}

		compagnon.getLookControl().setLookAt(maitre, 30.0F, 30.0F);
		compagnon.jouerActionPendant("@ecoute", 28);
		if (compagnon.caractere().calin() >= 0.50F) {
			compagnon.serrerLeMaitre();
		}
		return true;
	}

	// --- Ce qui vient du maitre ---------------------------------------------

	/**
	 * Il rend le regard.
	 *
	 * <h2>Le geste le plus courant du jeu ne produisait rien</h2>
	 *
	 * <p>Regarder son animal est ce qu'un joueur fait mille fois par partie, et
	 * c'etait jusqu'ici la seule interaction du mod qui n'avait aucune reponse.
	 * Maintenant il le sent : il incline la tete, il s'approche, ou il detourne
	 * les yeux — <b>selon son caractere</b>, jamais au hasard.
	 *
	 * <p>Il faut le fixer une seconde et demie. Un coup d'oeil en passant ne
	 * suffit pas : sinon il repondrait sans arret et le geste ne voudrait plus
	 * rien dire.
	 */
	private static boolean ilTeRegarde(CompagnonEntity compagnon, LivingEntity maitre,
			Attention attention) {

		if (compagnon.distanceToSqr(maitre) > PORTEE_DU_REGARD * PORTEE_DU_REGARD) {
			compagnon.oublierLeRegard();
			return false;
		}

		Vec3 vers = compagnon.getEyePosition().subtract(maitre.getEyePosition()).normalize();
		if (maitre.getLookAngle().normalize().dot(vers) < PRECISION_DU_REGARD) {
			compagnon.oublierLeRegard();
			return false;
		}

		if (compagnon.compterLeRegard(TOUS_LES) < AVANT_DE_REPONDRE) {
			return false;
		}
		if (!attention.permet("regard", AVANT_DE_REREGARDER)) {
			return false;
		}
		compagnon.oublierLeRegard();

		compagnon.getLookControl().setLookAt(maitre, 30.0F, 30.0F);
		// UNE BETE COLLANTE VIENT, UNE BETE RESERVEE DETOURNE LES YEUX.
		//
		// C'est la meme situation et deux reponses opposees : c'est exactement ce
		// qu'on cherche a produire, et ca ne coute qu'un test.
		if (compagnon.caractere().calin() >= 0.55F) {
			compagnon.jouerActionPendant("@joyeux", 25);
			compagnon.getNavigation().moveTo(maitre, 1.0D);
		} else {
			compagnon.jouerActionPendant("@ecoute", 25);
		}
		return true;
	}

	/**
	 * Il a vu quelque chose, et il te le montre.
	 *
	 * <h2>Il ne se bat pas</h2>
	 *
	 * <p>Il fixe la chose, il se raidit, il fait un bruit. C'est tout. Un
	 * compagnon qui tue a ta place devient une arme qu'on emmene ; un compagnon
	 * qui te previent reste un compagnon dont on apprend a lire le regard.
	 *
	 * <p>On apprend vite a suivre la direction de sa tete. C'est une
	 * information reelle, donnee sans un mot et sans rien afficher.
	 *
	 * <h2>Ce qu'on a fait pour que ca ne coute rien</h2>
	 *
	 * <p>C'est le seul endroit de {@link Presence} qui cherche des entites, et
	 * une recherche est ce qui coute vraiment cher a mille compagnons charges.
	 * Trois garde-fous, donc :
	 *
	 * <ul>
	 *   <li><b>Seulement si son maitre est la.</b> Prevenir quelqu'un d'absent
	 *       n'a aucun sens, et ca ecarte d'un coup tous les compagnons qui
	 *       trainent dans une salle commune loin de leur proprietaire.</li>
	 *   <li><b>Seulement dans le noir.</b> En plein jour il n'y a rien a
	 *       signaler, et c'est la moitie du temps de jeu en moins.</li>
	 *   <li><b>Une fois toutes les trois secondes au plus</b>, par le budget
	 *       d'attention, qui refuse avant qu'on ne cherche quoi que ce soit.</li>
	 * </ul>
	 *
	 * <p>Et la recherche elle-meme est mesuree : {@code /compagnon perf} la
	 * compte dans le poste « petits gestes ».
	 */
	private static boolean ilTePrevient(CompagnonEntity compagnon, LivingEntity maitre,
			Attention attention) {

		if (maitre == null
			|| compagnon.distanceToSqr(maitre) > PORTEE_DU_MAITRE * PORTEE_DU_MAITRE) {
			return false;
		}
		// IL FAIT JOUR ET CLAIR : IL N'Y A RIEN A SIGNALER.
		//
		// Sauf s'il est de garde. C'est ce qui empeche une bete ordinaire de
		// commenter chaque zombie qui brule au soleil, et ce qui rend la
		// competence « sentinelle » utile plutot que decorative.
		java.util.Set<String> siennes = competencesDe(compagnon);
		boolean deGarde = fr.lhdp.compagnon.competence.Competences.bonus(
			siennes, fr.lhdp.compagnon.competence.Competence.VEILLE) > 0.0F;
		if (!deGarde
			&& compagnon.level().getMaxLocalRawBrightness(compagnon.blockPosition()) > CLAIR) {
			return false;
		}
		// Le capteur commun a déjà payé cette recherche et en a gardé uniquement
		// l'UUID. Toutes les réactions réutilisent ce même résultat.
		if (!(compagnon.entiteMemorisee(MemoireCourte.Signal.MENACE)
				instanceof Monster laPlusProche) || !laPlusProche.isAlive()) {
			return false;
		}
		if (!attention.permet("alerte", AVANT_DE_REPREVENIR)) {
			return false;
		}

		compagnon.getLookControl().setLookAt(laPlusProche, 30.0F, 30.0F);
		boolean peur = Manies.PEUREUX.equals(compagnon.defaut())
				|| compagnon.caractere().courage() < 0.42F;
		String role = peur
				? SceneAffectiveGoal.premierRoleDisponible(compagnon,
						Espece.PEUR, Espece.TRISTE, Espece.ECOUTE)
				: SceneAffectiveGoal.premierRoleDisponible(compagnon,
						Espece.SURPRIS, Espece.ECOUTE);
		if (role != null) {
			compagnon.jouerActionPendant("@" + role, 30, PrioriteAction.URGENCE);
		}
		Sons.jouer(compagnon, Sons.ALERTE, 0.9F);

		// ET S'IL SAIT REVELER, IL MONTRE.
		//
		// Suivre le regard d'une bete dans un couloir noir ne suffit pas : on
		// sait qu'il y a quelque chose, on ne sait pas ou. Le lisere du jeu se
		// voit a travers les murs, et c'est toute la difference entre « il a
		// aboye » et « il est la ».
		if (fr.lhdp.compagnon.competence.Competences.bonus(siennes,
			fr.lhdp.compagnon.competence.Competence.REVELE) > 0.0F) {
			laPlusProche.setGlowingTag(true);
			Reveles.pendant(laPlusProche, TICKS_REVELES);
		}
		// UN CRAINTIF SE RAPPROCHE, UN BRAVE NON. Le meme signal, deux facons de
		// le donner — et on finit par savoir laquelle est la sienne.
		if (peur) {
			compagnon.serrerLeMaitre();
		}
		return true;
	}

	// --- Ce qui vient du monde ----------------------------------------------

	/**
	 * Le temps qu'il fait et l'endroit ou il est.
	 *
	 * <p>Trois cas seulement, et c'est voulu : le froid, la pluie, et le maitre
	 * en mauvais etat. Un compagnon qui reagirait a tout ne reagirait a rien.
	 */
	private static void leMonde(CompagnonEntity compagnon, Attention attention) {
		Level niveau = compagnon.level();
		BlockPos ou = compagnon.blockPosition();

		// UN ORAGE N'A PAS LE MEME SENS POUR TOUT LE MONDE. Le peureux se
		// rapproche ; le brave lève la tête. Les rôles spécialisés sont optionnels
		// et retombent sur les gestes déjà présents dans les espèces actuelles.
		if (compagnon.memoireCourte().contient(MemoireCourte.Signal.ORAGE)
				&& attention.permet("orage", AVANT_DE_REAGIR_ORAGE)) {
			boolean peur = Manies.PEUREUX.equals(compagnon.defaut())
					|| compagnon.caractere().courage() < 0.42F;
			String role = peur
					? SceneAffectiveGoal.premierRoleDisponible(compagnon,
							Espece.PEUR, Espece.TRISTE, Espece.ECOUTE)
					: SceneAffectiveGoal.premierRoleDisponible(compagnon,
							Espece.SURPRIS, Espece.ECOUTE, Espece.JOYEUX);
			if (role != null) {
				compagnon.jouerActionPendant("@" + role, 30, PrioriteAction.AFFECTIF);
			}
			if (peur) {
				compagnon.serrerLeMaitre();
			}
			return;
		}

		if (compagnon.memoireCourte().contient(MemoireCourte.Signal.NEIGE)
				&& attention.permet("neige", AVANT_DE_REDECOUVRIR_NEIGE)) {
			String role = SceneAffectiveGoal.premierRoleDisponible(compagnon,
					Espece.SURPRIS,
					compagnon.caractere().curiosite() >= 0.5F ? Espece.JOYEUX : Espece.ECOUTE,
					Espece.ECOUTE);
			if (role != null) {
				compagnon.jouerActionPendant("@" + role, 28, PrioriteAction.AFFECTIF);
			}
			return;
		}

		// IL A FROID. Il frissonne, et il se rapproche.
		// UN CASSE-COU NE FRISSONNE PAS. Il a froid comme les autres, il ne le
		// montre simplement pas.
		if (compagnon.profilCerveau().reagitFroid()
				&& compagnon.memoireCourte().contient(MemoireCourte.Signal.FROID)
				&& !Manies.CASSE_COU.equals(compagnon.defaut())
				&& attention.permet("froid", AVANT_DE_REFRISSONNER)) {
			compagnon.jouerActionPendant("@triste", 20);
			compagnon.serrerLeMaitre();
			return;
		}

		// IL EST MOUILLE. Il s'ebroue en sortant, pas pendant.
		if (compagnon.onGround() && !compagnon.isInWater()
				&& compagnon.vientDeSortirDeLEau()
				&& attention.permet("ebrouement", AVANT_DE_SE_REBROUER)) {
			compagnon.jouerActionPendant("@tourne", 18);
			Etincelles.gouttes(compagnon);
			return;
		}

		if (compagnon.profilCerveau().reagitPluie()
				&& compagnon.memoireCourte().contient(MemoireCourte.Signal.PLUIE)
				&& attention.permet("pluie", AVANT_DE_SE_REBROUER)) {
			compagnon.jouerActionPendant("@tourne", 18);
		}
	}

	/**
	 * Le maitre va-t-il assez mal pour qu'il reste colle ?
	 *
	 * <p>Lu par le but de suivi, qui raccourcit sa distance. Il ne soigne rien et
	 * ne dit rien : il est simplement la, ce qui est tout ce qu'un animal peut
	 * faire et tout ce qu'on lui demande.
	 */
	public static boolean maitreEnMauvaisEtat(CompagnonEntity compagnon) {
		return compagnon.memoireCourte().contient(MemoireCourte.Signal.MAITRE_EN_DANGER);
	}
}
