package fr.lhdp.compagnon.fiche;

import fr.lhdp.compagnon.entite.CompagnonEntity;
import fr.lhdp.compagnon.entite.Etincelles;
import fr.lhdp.compagnon.entite.Sons;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.TimeUnit;

/**
 * Ce qui se passe quand tu reviens apres une longue absence.
 *
 * <h2>La moitie qui manquait</h2>
 *
 * <p>Le mod avait deja tout le mecanisme qui fait qu'on s'attache : les barres
 * descendent quand on n'est pas la, la bete a faim, son humeur baisse. C'est le
 * ressort le plus puissant qu'on connaisse pour faire revenir quelqu'un — il ne
 * marche pas parce qu'on perd des points, il marche parce qu'on se sent
 * <b>coupable</b>.
 *
 * <p>Mais il lui manquait la seconde moitie. On revenait apres une semaine, on
 * sortait sa bete, et il ne se passait <b>rien du tout</b>. La punition existait,
 * les retrouvailles non.
 *
 * <h2>Au moment ou tu le sors, pas a la connexion</h2>
 *
 * <p>Deux raisons, et les deux comptent.
 *
 * <p>D'abord ca ne peut pas se declencher en rafale : quelqu'un qui possede cinq
 * compagnons et qui revient apres un mois recevrait cinq messages d'un coup a la
 * connexion, et cinq fetes valent zero fete.
 *
 * <p>Ensuite c'est le bon instant. Sortir sa bete est un geste volontaire ; c'est
 * la qu'on la regarde. Une fete jouee pendant l'ecran de chargement n'est vue par
 * personne.
 *
 * <h2>Il est content, il ne fait pas la tete</h2>
 *
 * <p>On a ecarte la version ou la bete boude tant qu'on ne l'a pas caressee.
 * C'est un joli mecanisme sur le papier, et une punition en jeu : quelqu'un qui
 * revient apres ses examens n'a pas besoin qu'un mod lui fasse la morale.
 *
 * <p>Il est donc toujours heureux. Ce qu'on dit en plus, c'est son etat — « il a
 * faim » — et c'est le joueur qui en tire la conclusion. La culpabilite vient de
 * ce qu'on voit, jamais de ce qu'on nous reproche.
 */
public final class Retrouvailles {

	private Retrouvailles() {
	}

	/**
	 * A partir de combien de temps on considere qu'il y a eu une absence.
	 *
	 * <p>Deux jours reels. En dessous, ce n'est pas une absence, c'est une nuit —
	 * et faire la fete tous les matins la banaliserait en trois jours.
	 */
	private static final long ABSENCE = TimeUnit.DAYS.toMillis(2L);

	/** Au-dela, on ne dit plus le nombre de jours mais « tres longtemps ». */
	private static final long TRES_LONGTEMPS = TimeUnit.DAYS.toMillis(60L);

	/** En dessous de ca, on signale que sa barre est basse. */
	private static final float BASSE = 30.0F;

	/** Combien de temps il fete, en ticks. */
	private static final int DUREE_DE_LA_JOIE = 40;

	/**
	 * Il vient de sortir : est-ce qu'il t'attendait depuis longtemps ?
	 *
	 * <p>A appeler <b>apres</b> que l'entite soit dans le monde, et avant que la
	 * visite ne soit notee — sans quoi l'absence vaut toujours zero.
	 *
	 * @return vrai s'il y a eu des retrouvailles, donc si la fiche a change
	 */
	public static boolean auRetour(ServerPlayer proprietaire, FicheCompagnon fiche) {
		long vuLe = fiche.vuLe();
		long maintenant = System.currentTimeMillis();

		// Jamais vu : c'est sa premiere sortie, ou une fiche d'avant cette
		// version. On ne fete pas des retrouvailles qu'on n'a pas vecues.
		if (vuLe <= 0L) {
			fiche.noterUneVisite(maintenant);
			return true;
		}

		long absence = maintenant - vuLe;
		fiche.noterUneVisite(maintenant);
		if (absence < ABSENCE) {
			return true;
		}

		montrerLaJoie(proprietaire, fiche);
		proprietaire.displayClientMessage(fr.lhdp.compagnon.Icones.devant(
			fr.lhdp.compagnon.Icones.COEUR, phrase(fiche, absence)), false);
		return true;
	}

	/** Ce qu'on voit sur la bete, si elle est bien la pour qu'on la voie. */
	private static void montrerLaJoie(ServerPlayer proprietaire, FicheCompagnon fiche) {
		ServerLevel niveau = proprietaire.server.getLevel(fiche.dimension());
		if (niveau == null
				|| !(niveau.getEntity(fiche.id()) instanceof CompagnonEntity compagnon)) {
			return;
		}
		// Il te regarde d'abord. Une fete jouee de dos ne s'adresse a personne.
		compagnon.getLookControl().setLookAt(proprietaire, 40.0F, 40.0F);
		compagnon.jouerActionPendant("@joie", DUREE_DE_LA_JOIE);
		Sons.jouer(compagnon, Sons.CONTENT, 1.2F);
		Etincelles.coeurs(compagnon, 10);
	}

	/**
	 * La phrase, qui dit le temps ecoule et, s'il y a lieu, ce qui ne va pas.
	 *
	 * <p>C'est le seul endroit ou l'on parle de son etat sans qu'on l'ait
	 * demande. On s'y autorise parce que le joueur vient justement de revenir, et
	 * que c'est le moment ou l'information sert a quelque chose.
	 */
	private static Component phrase(FicheCompagnon fiche, long absence) {
		String duree = absence >= TRES_LONGTEMPS
			? Component.translatable("retrouvailles.compagnon.longtemps").getString()
			: Component.translatable("retrouvailles.compagnon.jours",
				Math.max(2L, TimeUnit.MILLISECONDS.toDays(absence))).getString();

		if (fiche.barre(Barre.FAIM) <= BASSE) {
			return Component.translatable("retrouvailles.compagnon.faim", fiche.nom(), duree);
		}
		if (fiche.barre(Barre.ENERGIE) <= BASSE) {
			return Component.translatable("retrouvailles.compagnon.fatigue", fiche.nom(), duree);
		}
		return Component.translatable("retrouvailles.compagnon.simple", fiche.nom(), duree);
	}
}
