package fr.lhdp.compagnon.fiche;

import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.entite.CompagnonEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Fait apparaitre et disparaitre les entites autour des joueurs.
 *
 * <p>Dans un chateau il y a des compagnons dans toutes les chambres, et personne
 * ne les regarde. Les charger tous serait absurde : une fiche dont personne
 * n'est proche ne coute rien.
 *
 * <h2>Pourquoi cette boucle simple, et pas un index par tronçon</h2>
 *
 * <p>La question s'est posee : a mille compagnons et mille joueurs, ce parcours
 * fait <i>fiches x joueurs</i> comparaisons. Cela ressemble a un million
 * d'operations par seconde, donc a un probleme.
 *
 * <p><b>C'est faux, et cela a ete mesure.</b> Deux raisons :
 *
 * <ul>
 *   <li>la boucle s'arrete au premier joueur trouve — dans un chateau plein,
 *       elle ne fait que quelques comparaisons par fiche ;</li>
 *   <li>une comparaison de distance carree coute quelques nanosecondes. Le pire
 *       cas mesure — huit cents compagnons loin de tout joueur, mille joueurs —
 *       prend <b>0,5 ms</b>, soit un centieme d'un tick.</li>
 * </ul>
 *
 * <p>Un index par tronçon a ete ecrit puis mesure contre celle-ci : il est
 * <b>cinq a deux cents fois plus lent</b>, parce que construire l'index et ses
 * listes coute plus cher que les comparaisons qu'il economise. Ne pas le
 * reintroduire sans mesurer d'abord.
 */
public final class Apparition {

	/** Rayon d'apparition, en blocs. */
	public static final double RAYON = 20.0D;

	/**
	 * Marge avant de le retirer, en blocs. Il apparait a {@value #RAYON} mais ne
	 * disparait qu'au-dela de {@value #RAYON} + cette marge.
	 *
	 * <p>Sans elle, un joueur qui marche pile a la limite le ferait apparaitre et
	 * disparaitre a chaque seconde. Valeur inventee.
	 */
	public static final double MARGE = 4.0D;

	/** Periode de verification, en ticks. Vingt ticks font une seconde. */
	public static final int PERIODE = 20;

	private Apparition() {
	}

	public static void enregistrer() {
		ServerTickEvents.END_SERVER_TICK.register(Apparition::tick);

		// TU MEURS LOIN, IL EST PERDU.
		//
		// La fiche garde la derniere position de la bete, et l apparition ne la
		// remet dans le monde que si un joueur passe pres de CE point-la. Tu
		// mourais dans la foret et tu renaissais au chateau : ton compagnon
		// restait dans la foret, a deux mille blocs, et rien ne le ramenait
		// jamais — il fallait retourner mourir au meme endroit pour le revoir.
		//
		// On deplace donc la fiche avec toi. La bete reapparait a cote, dans la
		// seconde, comme si elle avait toujours ete la.
		ServerPlayerEvents.AFTER_RESPAWN.register((avant, apres, memeMonde) ->
			suivreLeRenaissant(apres));
	}

	/**
	 * Ramene a toi les betes qui etaient dehors quand tu es mort.
	 *
	 * <p>On deplace la fiche et on retire l entite restee la-bas : l apparition
	 * la recreera au bon endroit au prochain passage, avec le meme code que
	 * partout ailleurs. Rien de special a maintenir.
	 *
	 * <p>Celles qui etaient rangees ne bougent pas : elles ne sont nulle part,
	 * donc elles ne sont pas perdues.
	 */
	private static void suivreLeRenaissant(ServerPlayer joueur) {
		Fiches fiches = Fiches.de(joueur.server);
		boolean bouge = false;

		for (FicheCompagnon fiche : fiches.duProprietaireSansCopie(joueur.getUUID())) {
			if (!fiche.sorti()) {
				continue;
			}
			ServerLevel ou = joueur.server.getLevel(fiche.dimension());
			if (ou != null && ou.getEntity(fiche.id()) instanceof CompagnonEntity restee) {
				restee.discard();
			}
			fiche.poserA(joueur.level().dimension(),
				joueur.getX(), joueur.getY(), joueur.getZ(), joueur.getYRot());
			bouge = true;
		}
		if (bouge) {
			fiches.setDirty();
		}
	}

	private static void tick(MinecraftServer serveur) {
		if (serveur.getTickCount() % PERIODE != 0) {
			return;
		}

		Fiches fiches = Fiches.de(serveur);
		if (fiches.nombre() == 0) {
			return;
		}

		boolean quelqueChoseABouge = false;

		for (FicheCompagnon fiche : fiches.toutes()) {
			ServerLevel niveau = serveur.getLevel(fiche.dimension());
			if (niveau == null) {
				// Dimension absente (mod retire, monde supprime). La fiche est
				// conservee telle quelle : on ne perd jamais un compagnon.
				continue;
			}

			CompagnonEntity present = entiteDe(niveau, fiche);

			// RANGE : il n'apparait pour personne, ou qu'on aille.
			//
			// Ce test passe AVANT celui de la distance, et c'est ce qui fait toute
			// la difference avec un compagnon simplement loin : celui-la reviendra
			// des qu'on s'approchera de sa chambre. Un compagnon range, non.
			//
			// On memorise sa position AVANT de le retirer : le livre continue de
			// dire ou il etait, et ce qu'il portait retombe au sol par remove().
			if (!fiche.sorti()) {
				if (present != null) {
					fiche.memoriser(present);
					present.discard();
					quelqueChoseABouge = true;
				}
				continue;
			}

			// Deux distances, pas une : on le fait apparaitre plus tot qu'on ne le
			// retire, sinon il clignote quand on marche pile a la limite.
			boolean quelquUnEstLa = present == null
					? joueurProche(niveau, fiche, null, RAYON)
					: joueurProche(niveau, fiche, present, RAYON + MARGE);

			if (quelquUnEstLa && present == null) {
				apparaitre(niveau, fiche);
				quelqueChoseABouge = true;
			} else if (present != null) {
				// Tant qu'il est la, la fiche suit ce qu'il fait.
				fiche.memoriser(present);
				quelqueChoseABouge = true;
				// ON NE FAIT JAMAIS DISPARAITRE UNE BETE QUI PORTE QUELQU'UN.
				//
				// Une bete montee est par definition a cote d'un joueur : la question
				// ne se pose pas. Sans cette ligne, une monture rapide finissait par
				// s'effacer sous son cavalier, qui tombait, puis elle revenait.
				if (!quelquUnEstLa && !present.isVehicle()) {
					present.discard();
				}
			}
		}

		quelqueChoseABouge |= compterLeTempsEnsemble(serveur, fiches);

		if (quelqueChoseABouge) {
			fiches.setDirty();
		}
	}

	/** L'entite d'une fiche, ou {@code null} si elle n'est pas affichee. */
	private static CompagnonEntity entiteDe(ServerLevel niveau, FicheCompagnon fiche) {
		Entity trouvee = niveau.getEntity(fiche.id());
		return trouvee instanceof CompagnonEntity compagnon && !compagnon.isRemoved() ? compagnon : null;
	}

	/**
	 * Quelqu'un est-il assez pres de lui ?
	 *
	 * <p>De <b>lui</b> quand il est la, et de sa fiche seulement quand il n'y est
	 * pas. La fiche ne se met a jour qu'une fois par seconde : mesurer depuis
	 * elle revenait a mesurer depuis ou la bete etait il y a une seconde. En
	 * volant, une seconde fait une belle distance — assez pour qu'elle se juge
	 * abandonnee alors qu'on est sur son dos.
	 */
	private static boolean joueurProche(ServerLevel niveau, FicheCompagnon fiche,
			CompagnonEntity present, double distance) {

		for (ServerPlayer joueur : niveau.players()) {
			if (joueur.isSpectator()) {
				continue;
			}
			boolean pres = present != null
					? present.distanceToSqr(joueur) <= distance * distance
					: !fiche.loinDe(joueur.getX(), joueur.getY(), joueur.getZ(), distance);
			if (pres) {
				return true;
			}
		}
		return false;
	}

	private static void apparaitre(ServerLevel niveau, FicheCompagnon fiche) {
		CompagnonEntity compagnon = Compagnon.COMPAGNON.create(niveau);
		if (compagnon == null) {
			Compagnon.LOG.error("Impossible de creer l'entite de la fiche {}", fiche.id());
			return;
		}

		// Meme identifiant que la fiche : c'est ce qui permet de la retrouver.
		compagnon.setUUID(fiche.id());
		compagnon.moveTo(fiche.x(), fiche.y(), fiche.z(), fiche.rotation(), 0.0F);
		compagnon.lierA(fiche);

		niveau.addFreshEntity(compagnon);
	}

	/**
	 * Rien ne se degrade quand le proprietaire est deconnecte : le temps ne compte
	 * que quand il joue.
	 *
	 * <p>On part des <b>joueurs connectes</b>, et non des fiches. Sur un serveur ou
	 * la plupart des proprietaires sont absents, leurs fiches ne sont pas meme
	 * lues — et c'est l'index par proprietaire qui rend ce parcours immediat.
	 */
	private static boolean compterLeTempsEnsemble(MinecraftServer serveur, Fiches fiches) {
		boolean quelqueChoseABouge = false;

		for (ServerPlayer joueur : serveur.getPlayerList().getPlayers()) {
			java.util.List<FicheCompagnon> siennes =
					fiches.duProprietaireSansCopie(joueur.getUUID());
			for (FicheCompagnon fiche : siennes) {
				fiche.ajouterTemps(PERIODE);
				quelqueChoseABouge = true;
			}
			// Le petit panneau du coin de l'ecran. Sur ce parcours-ci et pas un
			// autre : les fiches sont deja en main, et les reparcourir ailleurs
			// serait payer deux fois le meme travail. Jauges n'envoie que si
			// quelque chose a change.
			Jauges.rafraichir(joueur, siennes);
		}
		return quelqueChoseABouge;
	}
}
