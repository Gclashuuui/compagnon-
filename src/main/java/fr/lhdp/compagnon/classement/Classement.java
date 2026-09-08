package fr.lhdp.compagnon.classement;

import fr.lhdp.compagnon.fiche.FicheCompagnon;
import fr.lhdp.compagnon.fiche.Fiches;
import fr.lhdp.compagnon.progression.Niveaux;
import fr.lhdp.compagnon.progression.Progression;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Le classement des compagnons du serveur.
 *
 * <h2>Ce qu'on classe</h2>
 *
 * <p>Le meilleur compagnon de chaque joueur, et un seul par joueur. Sans cette
 * regle, quelqu'un qui en collectionne dix occuperait dix lignes du haut du
 * tableau, et le classement dirait « qui en a le plus » au lieu de « qui a le
 * mieux fait grandir le sien ».
 *
 * <h2>Pourquoi c'est mis en cache</h2>
 *
 * <p>Trier mille fiches n'est pas cher, mais le faire a chaque fois qu'un joueur
 * ouvre l'ecran, sur un serveur ou mille personnes peuvent l'ouvrir, l'est. On
 * calcule donc au plus une fois toutes les {@value #TICKS_DE_FRAICHEUR} ticks et
 * tout le monde lit le meme resultat.
 *
 * <p>Le cache ne se compte pas en dates absolues mais en ticks du serveur, qui
 * ne redemarrent qu'avec lui.
 *
 * <h2>Il ne donne aucun avantage</h2>
 *
 * <p>Ni objet, ni bonus, ni experience. On y va pour se voir. C'est une regle de
 * conception, pas une limite technique : le jour ou etre premier rapporterait
 * quelque chose, s'occuper de son animal deviendrait une optimisation.
 */
public final class Classement {

	private Classement() {
	}

	/** Combien de temps un classement calcule reste valable, en ticks. */
	private static final int TICKS_DE_FRAICHEUR = 20 * 30;

	/**
	 * Combien de lignes on envoie au plus.
	 *
	 * <p>Le haut du tableau, plus le voisinage du joueur. Envoyer mille lignes a
	 * mille joueurs toutes les trente secondes ferait un paquet de plusieurs
	 * centaines de kilo-octets pour un ecran ou personne ne fait defiler
	 * jusqu'en bas.
	 */
	public static final int LIGNES_DU_HAUT = 25;

	/** Combien de voisins on montre de chaque cote de sa propre position. */
	public static final int VOISINS = 5;

	private static List<Ligne> enCache = List.of();
	private static int calculeAu = Integer.MIN_VALUE;
	private static boolean ouvert = true;

	/**
	 * Le classement est-il ouvert aux joueurs ?
	 *
	 * <p>Fermable par le staff, d'une commande. Le mod est fait pour un serveur
	 * de jeu de role : si le tableau finit par agacer plus qu'il n'amuse, il faut
	 * pouvoir l'eteindre sans redemarrer ni recompiler.
	 */
	public static boolean ouvert() {
		return ouvert;
	}

	/**
	 * Remet tout a zero. A appeler au demarrage d'un serveur ou d'un monde.
	 *
	 * <h2>Pourquoi il le faut</h2>
	 *
	 * <p>Ces valeurs sont statiques : en solo, elles survivent a la fermeture
	 * d'un monde. Sans cette remise a zero, ouvrir une partie apres une autre
	 * montrait pendant trente secondes <b>le classement de l'autre monde</b>,
	 * et un classement ferme par le staff le restait pour toutes les parties
	 * suivantes.
	 *
	 * <p>Et le compte de ticks repart de zero avec le serveur : la comparaison
	 * de fraicheur aurait compare des ticks de deux mondes differents.
	 */
	public static void oublierTout() {
		enCache = List.of();
		calculeAu = Integer.MIN_VALUE;
		ouvert = true;
	}

	public static void ouvrir(boolean actif) {
		ouvert = actif;
		if (!actif) {
			// Rien ne sert de garder un classement que personne ne verra.
			enCache = List.of();
			calculeAu = Integer.MIN_VALUE;
		}
	}

	/**
	 * Une ligne du tableau.
	 *
	 * @param qui       l'identifiant du proprietaire, pour retrouver son skin
	 * @param joueur    le nom du proprietaire, tel qu'il s'affiche
	 * @param compagnon le nom de la bete
	 * @param espece    son espece, pour la ligne et pour le portrait
	 * @param variante  sa couleur, pour le portrait
	 * @param niveau    ce sur quoi on classe
	 * @param xp        ce qui departage a niveau egal
	 * @param jours     depuis combien de jours ils sont ensemble
	 */
	public record Ligne(UUID qui, String joueur, String compagnon, String espece,
			String variante, int niveau, int xp, int jours) {
	}

	/**
	 * Le classement entier, recalcule si besoin.
	 *
	 * <p>Rendu en entier — c'est ce qui permet de connaitre le rang exact d'un
	 * joueur meme s'il est cinq-centieme. Le decoupage pour le reseau se fait
	 * ailleurs, dans {@link #pour}.
	 */
	public static List<Ligne> tout(MinecraftServer serveur) {
		int maintenant = serveur.getTickCount();
		// Une soustraction, pas une comparaison de dates : c'est le meme piege
		// qui avait fait voler tous les compagnons en permanence.
		if (!enCache.isEmpty() && maintenant - calculeAu < TICKS_DE_FRAICHEUR
				&& maintenant >= calculeAu) {
			return enCache;
		}
		enCache = calculer(serveur);
		calculeAu = maintenant;
		return enCache;
	}

	/**
	 * Ce qu'on envoie a un joueur : le haut du tableau, et son voisinage.
	 *
	 * <p>Les cinq au-dessus et les cinq en dessous de lui comptent autant que le
	 * podium. Voir seulement les dix premiers, quand on est quatre-vingt-dixieme,
	 * donne envie d'abandonner ; voir les cinq qu'on peut rattraper donne envie
	 * de continuer.
	 *
	 * @return le morceau a envoyer, son rang a partir de 1 (ou 0 s'il n'est pas
	 *         classe), le nombre de participants, et l'indice ou commence le
	 *         voisinage dans la liste rendue
	 */
	public static Extrait pour(MinecraftServer serveur, ServerPlayer joueur) {
		List<Ligne> tout = tout(serveur);
		// Par identifiant et non par nom : deux joueurs peuvent porter le meme
		// pseudo sur un serveur hors ligne, et un pseudo peut changer.
		int rang = 0;
		for (int i = 0; i < tout.size(); i++) {
			if (tout.get(i).qui().equals(joueur.getUUID())) {
				rang = i + 1;
				break;
			}
		}

		List<Ligne> envoi = new ArrayList<>(LIGNES_DU_HAUT + 2 * VOISINS + 1);
		int haut = Math.min(LIGNES_DU_HAUT, tout.size());
		envoi.addAll(tout.subList(0, haut));

		// Le voisinage, seulement s'il est en dehors du haut du tableau.
		int debutVoisinage = -1;
		if (rang > haut) {
			int depuis = Math.max(haut, rang - 1 - VOISINS);
			int jusqua = Math.min(tout.size(), rang + VOISINS);
			debutVoisinage = envoi.size();
			envoi.addAll(tout.subList(depuis, jusqua));
		}
		return new Extrait(List.copyOf(envoi), rang, tout.size(), debutVoisinage);
	}

	/**
	 * Le morceau du classement envoye a un joueur.
	 *
	 * @param lignes          le haut du tableau, puis eventuellement son voisinage
	 * @param monRang         son rang a partir de 1, ou 0 s'il n'a pas de compagnon
	 * @param participants    combien de joueurs sont classes en tout
	 * @param debutVoisinage  l'indice ou commence le voisinage, ou -1 s'il n'y en a pas
	 */
	public record Extrait(List<Ligne> lignes, int monRang, int participants,
			int debutVoisinage) {
	}

	// --- Le calcul ------------------------------------------------------------

	/**
	 * Trie les fiches, en gardant la meilleure de chaque proprietaire.
	 *
	 * <p>Le nom du joueur est lu dans le cache des profils du serveur, pas dans
	 * la liste des connectes : un classement ou les gens disparaissent quand ils
	 * se deconnectent n'a aucun interet.
	 */
	private static List<Ligne> calculer(MinecraftServer serveur) {
		Progression table = Niveaux.progression();
		Fiches fiches = Fiches.de(serveur);

		java.util.Map<UUID, FicheCompagnon> meilleures = new java.util.HashMap<>();
		for (FicheCompagnon fiche : fiches.toutes()) {
			FicheCompagnon deja = meilleures.get(fiche.proprietaire());
			if (deja == null || fiche.xp() > deja.xp()) {
				meilleures.put(fiche.proprietaire(), fiche);
			}
		}

		long maintenant = System.currentTimeMillis();
		List<Ligne> lignes = new ArrayList<>(meilleures.size());
		for (FicheCompagnon fiche : meilleures.values()) {
			String joueur = nomDe(serveur, fiche.proprietaire());
			if (joueur == null) {
				// Un profil qu'on ne sait plus nommer : on ne met pas un identifiant
				// brut dans un tableau que tout le monde regarde.
				continue;
			}
			int jours = (int) Math.max(0L,
					(maintenant - fiche.dateObtention()) / 86400000L);
			lignes.add(new Ligne(fiche.proprietaire(), joueur, fiche.nom(), fiche.espece(),
					fiche.variante(), fiche.niveau(table), fiche.xp(), jours));
		}

		lignes.sort(Comparator.comparingInt(Ligne::niveau).reversed()
				.thenComparing(Comparator.comparingInt(Ligne::xp).reversed())
				// A niveau et experience egaux, le plus ancien passe devant : le
				// temps passe ensemble departage, jamais l'ordre de la sauvegarde.
				.thenComparing(Comparator.comparingInt(Ligne::jours).reversed())
				.thenComparing(Ligne::joueur));
		return List.copyOf(lignes);
	}

	/** Le nom affichable d'un joueur, connecte ou non, ou {@code null}. */
	private static String nomDe(MinecraftServer serveur, UUID qui) {
		ServerPlayer present = serveur.getPlayerList().getPlayer(qui);
		if (present != null) {
			return present.getGameProfile().getName();
		}
		return serveur.getProfileCache() == null ? null
				: serveur.getProfileCache().get(qui).map(com.mojang.authlib.GameProfile::getName)
						.orElse(null);
	}
}
