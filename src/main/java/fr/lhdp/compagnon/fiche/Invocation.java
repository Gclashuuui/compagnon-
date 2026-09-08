package fr.lhdp.compagnon.fiche;

import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.entite.CompagnonEntity;
import fr.lhdp.compagnon.espece.Espece;
import fr.lhdp.compagnon.espece.Especes;
import fr.lhdp.compagnon.reglage.Reglages;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Faire venir un compagnon, ou le ranger.
 *
 * <h2>Ce que « ranger » veut dire ici</h2>
 *
 * <p>Rien ne disparait. La fiche est intacte, ses barres continuent de vivre,
 * son livre s'ouvre, son niveau ne bouge pas. Il n'est simplement <b>pas la</b>
 * — et il ne reapparaitra pour personne tant qu'on ne l'aura pas rappele.
 *
 * <p>C'est different d'un compagnon reste dans une chambre : celui-la est encore
 * dehors, il attend, et il reapparaitra des qu'un joueur passera devant sa
 * porte. Voir {@link Apparition}.
 *
 * <h2>Pourquoi une limite</h2>
 *
 * <p>Un joueur peut avoir dix betes. Les sortir toutes, c'est dix creatures a
 * animer, a faire marcher et a envoyer sur le reseau — multiplie par le nombre
 * de joueurs presents. Le reglage {@code sortis_en_meme_temps} borne cela.
 *
 * <p>Mais on ne <b>refuse</b> jamais : en sortir un de plus range le precedent.
 * Un refus obligerait a ranger d'abord, puis a invoquer — deux gestes pour une
 * seule intention, et un message d'erreur pour quelque chose qui n'en est pas
 * une.
 */
public final class Invocation {

	/** A quelle distance devant le joueur on essaie de le poser, en blocs. */
	private static final double DEVANT = 2.0D;

	/** Les autres distances essayees si la place manque devant. */
	private static final double[] RAYONS = {2.0D, 3.0D, 1.5D, 4.0D};

	/** Combien de directions on essaie a chaque distance. */
	private static final int DIRECTIONS = 8;

	/** Jusqu'ou on descend pour trouver un sol, en blocs. */
	private static final int DESCENTE = 4;

	/** Et jusqu'ou on monte, pour ne pas le poser dans le plafond. */
	private static final int MONTEE = 2;

	private Invocation() {
	}

	/**
	 * Le resultat, pour que l'appelant puisse le raconter au joueur.
	 *
	 * @param fait    vrai si quelque chose a change
	 * @param message ce qu'on dit au joueur
	 */
	public record Resultat(boolean fait, String message) {
	}

	/**
	 * Le fait venir aupres de son proprietaire.
	 *
	 * <p>On deplace la <b>fiche</b>, qui fait autorite sur la position, puis on
	 * fait naitre l'entite tout de suite au lieu d'attendre le prochain passage
	 * d'{@link Apparition} : une seconde d'attente apres un clic se sent.
	 */
	public static Resultat invoquer(ServerPlayer joueur, Fiches fiches, FicheCompagnon fiche) {
		if (!fiche.proprietaire().equals(joueur.getUUID())) {
			// Ne devrait jamais arriver : le paquet ne propose que ses fiches a lui.
			// Mais c'est le serveur qui decide, pas le paquet.
			return new Resultat(false, "Ce n'est pas ton compagnon.");
		}

		String rentres = rangerLesAutres(joueur, fiches, fiche);

		ServerLevel niveau = joueur.serverLevel();
		Vec3 place = placeLibre(niveau, joueur, fiche);

		fiche.poserA(niveau.dimension(), place.x, place.y, place.z, joueur.getYRot());
		fiche.setSorti(true);
		fiche.setMode(Mode.SUIT);
		fiches.setDirty();

		// Tout de suite, et pas au prochain tick d'Apparition : le joueur vient de
		// cliquer, il regarde. Si la creation echoue, Apparition reessaiera dans la
		// seconde — la fiche est deja a la bonne place, rien n'est perdu.
		faireNaitre(niveau, fiche);

		// ET S'IL T'ATTENDAIT DEPUIS LONGTEMPS, IL LE MONTRE.
		//
		// Ici et non a la connexion : quelqu'un qui a cinq compagnons et qui
		// revient apres un mois recevrait cinq fetes d'un coup, ce qui n'en fait
		// aucune. Et sortir sa bete est le moment ou on la regarde.
		Retrouvailles.auRetour(joueur, fiche);
		fiches.setDirty();

		return new Resultat(true, fiche.nom() + " arrive." + rentres);
	}

	/**
	 * Le range.
	 *
	 * <p>Ce qu'il portait retombe au sol : c'est {@code CompagnonEntity.remove}
	 * qui s'en charge, et c'est le meme filet que pour tous les autres cas de
	 * disparition. Rien ne part dans le neant.
	 */
	public static Resultat ranger(ServerPlayer joueur, Fiches fiches, FicheCompagnon fiche) {
		if (!fiche.proprietaire().equals(joueur.getUUID())) {
			return new Resultat(false, "Ce n'est pas ton compagnon.");
		}
		if (!fiche.sorti()) {
			return new Resultat(false, fiche.nom() + " est deja range.");
		}
		rangerVraiment(joueur.server.getLevel(fiche.dimension()), fiche);
		fiches.setDirty();
		return new Resultat(true, fiche.nom() + " est range.");
	}

	// --- Le travail ---------------------------------------------------------------

	/**
	 * Range ce qu'il faut pour que celui-ci puisse sortir.
	 *
	 * @return un bout de phrase a ajouter au message, ou une chaine vide
	 */
	private static String rangerLesAutres(ServerPlayer joueur, Fiches fiches,
			FicheCompagnon celuiQuiSort) {

		if (Reglages.sansLimite()) {
			return "";
		}

		List<FicheCompagnon> dehors = new ArrayList<>();
		for (FicheCompagnon autre : fiches.duProprietaireSansCopie(joueur.getUUID())) {
			if (autre.sorti() && !autre.id().equals(celuiQuiSort.id())) {
				dehors.add(autre);
			}
		}

		// Combien de trop, en comptant celui qui va sortir.
		int aRanger = dehors.size() + 1 - Reglages.sortisEnMemeTemps();
		if (aRanger <= 0) {
			return "";
		}

		List<String> noms = new ArrayList<>();
		for (int i = 0; i < aRanger && i < dehors.size(); i++) {
			FicheCompagnon range = dehors.get(i);
			rangerVraiment(joueur.server.getLevel(range.dimension()), range);
			noms.add(range.nom());
		}
		if (noms.isEmpty()) {
			return "";
		}
		return " " + String.join(", ", noms) + (noms.size() > 1 ? " rentrent." : " rentre.");
	}

	/** Marque la fiche et retire l'entite si elle est la. */
	private static void rangerVraiment(ServerLevel niveau, FicheCompagnon fiche) {
		if (niveau != null) {
			CompagnonEntity present = entiteDe(niveau, fiche);
			if (present != null) {
				// La position d'abord : le livre continue de dire ou il etait.
				fiche.memoriser(present);
				// La bouffee AVANT de le retirer : une bete deja partie n'a plus de
				// position d'ou emettre.
				fr.lhdp.compagnon.entite.Etincelles.bouffee(present);
				present.discard();
			}
		}
		fiche.setSorti(false);
	}

	private static void faireNaitre(ServerLevel niveau, FicheCompagnon fiche) {
		CompagnonEntity present = entiteDe(niveau, fiche);
		if (present != null) {
			// Deja la, mais ailleurs : on le deplace au lieu d'en creer un second.
			present.teleportTo(fiche.x(), fiche.y(), fiche.z());
			present.lierA(fiche);
			arriver(present);
			return;
		}

		CompagnonEntity compagnon = Compagnon.COMPAGNON.create(niveau);
		if (compagnon == null) {
			Compagnon.LOG.error("Invocation : impossible de creer l'entite de la fiche {}",
					fiche.id());
			return;
		}
		compagnon.setUUID(fiche.id());
		compagnon.moveTo(fiche.x(), fiche.y(), fiche.z(), fiche.rotation(), 0.0F);
		compagnon.lierA(fiche);
		niveau.addFreshEntity(compagnon);
		arriver(compagnon);
	}

	/**
	 * Il vient de sortir : on le voit et on l'entend arriver.
	 *
	 * <p>Apres {@code addFreshEntity} seulement — une entite qui n'est pas
	 * encore dans le monde n'a personne a qui envoyer ses particules.
	 */
	private static void arriver(CompagnonEntity compagnon) {
		fr.lhdp.compagnon.entite.Etincelles.bouffee(compagnon);
		fr.lhdp.compagnon.entite.Sons.jouer(compagnon,
			fr.lhdp.compagnon.entite.Sons.CONTENT);
	}

	private static CompagnonEntity entiteDe(ServerLevel niveau, FicheCompagnon fiche) {
		Entity trouvee = niveau.getEntity(fiche.id());
		return trouvee instanceof CompagnonEntity compagnon && !compagnon.isRemoved()
				? compagnon
				: null;
	}

	// --- Ou le poser ---------------------------------------------------------------

	/**
	 * Une place ou il tient, aussi pres que possible de devant le joueur.
	 *
	 * <p>On essaie devant lui d'abord — c'est la qu'il regarde — puis tout autour,
	 * de plus en plus loin. A chaque endroit on cherche un sol en descendant puis
	 * en montant : un couloir etroit, un escalier, un rebord de fenetre.
	 *
	 * <p>Si vraiment rien ne convient — dans un placard, sur un radeau — on le
	 * pose <b>sur le joueur</b>. Ce n'est pas elegant, mais un compagnon qu'on
	 * n'arrive pas a invoquer serait bien pire, et le jeu le repoussera tout seul.
	 */
	private static Vec3 placeLibre(ServerLevel niveau, ServerPlayer joueur, FicheCompagnon fiche) {
		Espece espece = Especes.get(fiche.espece());
		double largeur = espece == null ? Espece.LARGEUR_PAR_DEFAUT : espece.largeur();
		double hauteur = espece == null ? Espece.HAUTEUR_PAR_DEFAUT : espece.hauteur();

		Vec3 regard = joueur.getLookAngle();
		double angleDuRegard = Math.atan2(regard.z, regard.x);

		Vec3 devant = essayer(niveau, joueur,
				joueur.getX() + Math.cos(angleDuRegard) * DEVANT,
				joueur.getZ() + Math.sin(angleDuRegard) * DEVANT,
				largeur, hauteur);
		if (devant != null) {
			return devant;
		}

		for (double rayon : RAYONS) {
			for (int i = 0; i < DIRECTIONS; i++) {
				double angle = angleDuRegard + (Math.PI * 2.0D * i) / DIRECTIONS;
				Vec3 trouve = essayer(niveau, joueur,
						joueur.getX() + Math.cos(angle) * rayon,
						joueur.getZ() + Math.sin(angle) * rayon,
						largeur, hauteur);
				if (trouve != null) {
					return trouve;
				}
			}
		}
		return joueur.position();
	}

	/** Un sol libre a cette colonne, ou {@code null}. */
	private static Vec3 essayer(ServerLevel niveau, ServerPlayer joueur, double x, double z,
			double largeur, double hauteur) {

		for (int dy = MONTEE; dy >= -DESCENTE; dy--) {
			double y = Math.floor(joueur.getY()) + dy;
			BlockPos sous = BlockPos.containing(x, y - 0.5D, z);
			if (niveau.getBlockState(sous).getCollisionShape(niveau, sous).isEmpty()) {
				// Rien pour le porter : on continue de descendre.
				continue;
			}
			AABB boite = new AABB(
					x - largeur / 2.0D, y, z - largeur / 2.0D,
					x + largeur / 2.0D, y + hauteur, z + largeur / 2.0D);
			if (niveau.noCollision(boite)) {
				return new Vec3(x, y, z);
			}
		}
		return null;
	}
}
