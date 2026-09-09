package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.fiche.FicheCompagnon;
import fr.lhdp.compagnon.fiche.Fiches;
import fr.lhdp.compagnon.fiche.Mode;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quand tu dors, il dort.
 *
 * <h2>Pourquoi celui-la et pas un autre</h2>
 *
 * <p>Sur un serveur de Poudlard, le coucher est le seul moment de la journee ou
 * tout un dortoir fait la meme chose en meme temps. C'etait aussi le seul moment
 * ou le compagnon ne faisait <b>rien</b> : son maitre disparaissait dans un lit
 * et il continuait a tourner en rond a cote.
 *
 * <p>Maintenant il se couche pres du lit, et il se reveille avec toi. C'est deux
 * evenements du jeu auxquels personne ne repondait, et le resultat est une image
 * qu'on n'oublie pas : dix eleves endormis, dix betes couchees au pied des lits.
 *
 * <h2>Il y gagne vraiment quelque chose</h2>
 *
 * <p>Couche, il est au repos : son energie remonte au lieu de descendre, comme
 * pour n'importe quel ordre de repos. Une nuit passee ensemble le repose donc
 * pour de vrai, et ce n'est pas une decoration.
 *
 * <h2>Ce qu'on ne touche pas</h2>
 *
 * <p>Seules les betes qui te <b>suivent</b> sont couchees. Celle a qui tu as dit
 * de rester assise garde son ordre : ce qui vient du joueur passe avant ce que
 * le mod trouve joli, toujours.
 *
 * <p>Et l'ordre d'avant est retenu pour etre rendu au reveil — pas devine. Une
 * bete qu'on retrouve dans un autre etat que celui ou on l'avait laissee, c'est
 * un bug, meme quand c'est joli.
 */
public final class Sommeil {

	private Sommeil() {
	}

	/** Le role joue au reveil. Une espece qui ne le declare pas ne s'anime pas. */
	private static final String ROLE_REVEIL = "@reveil";

	/** Combien de temps dure le reveil, en ticks. */
	private static final int DUREE_DU_REVEIL = 45;

	/**
	 * A quelle distance du lit il se couche aussi.
	 *
	 * <p>Seize blocs : la piece, pas le chateau. Une bete restee dehors pendant
	 * que son maitre monte se coucher n'a aucune raison de s'endormir.
	 */
	private static final double PORTEE = 16.0D;

	/**
	 * Ce que chaque bete faisait avant qu'on la couche.
	 *
	 * <p>Le temps de la session seulement : si le serveur tombe pendant la nuit,
	 * la bete se reveille couchee, ce qui est exactement ce qu'on attend d'une
	 * bete qu'on a laissee endormie. Rien a sauvegarder.
	 */
	private static final Map<UUID, Mode> avantLeCoucher = new ConcurrentHashMap<>();

	public static void enregistrer() {
		EntitySleepEvents.START_SLEEPING.register((entite, ou) -> basculer(entite, true));
		EntitySleepEvents.STOP_SLEEPING.register((entite, ou) -> basculer(entite, false));
	}

	private static void basculer(LivingEntity entite, boolean dort) {
		if (!(entite instanceof ServerPlayer joueur)) {
			return;
		}
		Fiches fiches = Fiches.de(joueur.server);
		boolean change = false;

		for (FicheCompagnon fiche : fiches.duProprietaireSansCopie(joueur.getUUID())) {
			if (!fiche.sorti()) {
				continue;
			}
			ServerLevel niveau = joueur.server.getLevel(fiche.dimension());
			if (niveau == null || niveau != joueur.serverLevel()
					|| !(niveau.getEntity(fiche.id()) instanceof CompagnonEntity compagnon)) {
				continue;
			}
			change |= dort
				? endormir(joueur, fiche, compagnon)
				: reveiller(fiche, compagnon);
		}
		if (change) {
			fiches.setDirty();
		}
	}

	private static boolean endormir(ServerPlayer joueur, FicheCompagnon fiche,
			CompagnonEntity compagnon) {

		if (compagnon.distanceToSqr(joueur) > PORTEE * PORTEE) {
			return false;
		}
		// Un ordre du joueur ne se defait pas. Celle qu'on a assise reste assise.
		if (fiche.mode() != Mode.SUIT) {
			return false;
		}
		avantLeCoucher.put(fiche.id(), fiche.mode());
		fiche.setMode(Mode.COUCHE);
		compagnon.appliquerMode(Mode.COUCHE);
		compagnon.setDort(true);
		return true;
	}

	private static boolean reveiller(FicheCompagnon fiche, CompagnonEntity compagnon) {
		Mode avant = avantLeCoucher.remove(fiche.id());
		if (avant == null) {
			// Ce n'est pas nous qui l'avons couchee : on n'y touche pas.
			return false;
		}
		compagnon.setDort(false);
		fiche.setMode(avant);
		compagnon.appliquerMode(avant);
		// Il s'etire avant de se relever, si son espece sait le faire.
		compagnon.jouerActionPendant(ROLE_REVEIL, DUREE_DU_REVEIL,
				PrioriteAction.BESOIN);
		return true;
	}
}
