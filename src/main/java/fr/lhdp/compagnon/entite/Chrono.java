package fr.lhdp.compagnon.entite;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Le chronometre, pour repondre aux questions de performance qu'on ne peut pas
 * trancher hors du jeu.
 *
 * <h2>Pourquoi il existe</h2>
 *
 * <p>Certains couts se calculent sur un coin de table, et {@code outils/} est
 * plein de petits bancs d'essai qui le font. D'autres demandent un serveur en
 * marche, avec de vrais joueurs et de vraies entites autour : tout ce qu'on peut
 * en dire de serieux avant, c'est qu'on ne sait pas.
 *
 * <p>Plutot que d'en discuter, {@code /compagnon perf} les mesure.
 *
 * <h2>Un poste par question</h2>
 *
	 * <p>Quatre compteurs separes, et ce n'est pas un detail : additionner deux
 * choses differentes dans le meme total donne un chiffre qui ne repond a aucune
 * question. Quand on cherche ou passe le temps, on veut savoir <b>lequel</b>
 * coute.
 *
 * <ul>
 *   <li>{@link #COLLISION} — repousser ce qui entre dans un morceau de la bete.
 *       Tourne a chaque tick, des deux cotes, pour chaque compagnon charge.</li>
 *   <li>{@link #PRESENCE} — les petits gestes : il te regarde, il te previent,
 *       il reclame. Toutes les dix ticks, et seulement cote serveur.</li>
	 *   <li>{@link #CURIOSITE} — <b>le chemin le plus frequent du mod</b>, et le
 *       seul qui ne depende pas du nombre de compagnons mais du nombre de
 *       <b>joueurs</b> : une recherche d'entites a chaque bloc casse et a chaque
 *       clic droit sur un bloc. Un joueur qui creuse vite en declenche cinq par
	 *       seconde ; mille joueurs qui creusent, cinq mille.</li>
	 *   <li>{@link #CERVEAU} — capteurs espacés, mémoire courte et recherches
	 *       mutualisées de menaces et d'amis.</li>
 * </ul>
 *
 * <p><b>Eteint, tout ca coute la lecture d'un booleen.</b> C'est pour ca que le
 * chronometre peut rester dans le code au lieu d'etre un bricolage jetable.
 */
public final class Chrono {

	/** Repousser ce qui entre dans un morceau de la bete. */
	public static final Poste COLLISION = new Poste("Collision en plusieurs morceaux");

	/** Les petits gestes : il te regarde, il te previent, il reclame. */
	public static final Poste PRESENCE = new Poste("Petits gestes (presence)");

	/** Un bloc casse ou pose : qui l'a vu ? */
	public static final Poste CURIOSITE = new Poste("Curiosite (un bloc casse ou pose)");

	/** Capteurs espacés du cerveau : environnement, menaces et compagnons. */
	public static final Poste CERVEAU = new Poste("Cerveau (perception et memoire)");

	private static final List<Poste> POSTES = List.of(COLLISION, PRESENCE, CURIOSITE, CERVEAU);

	private static volatile boolean enMarche;

	private Chrono() {
	}

	public static boolean enMarche() {
		return enMarche;
	}

	/** Tous les postes, dans l'ordre ou {@code /compagnon perf} les affiche. */
	public static List<Poste> postes() {
		return POSTES;
	}

	/** Met en marche et repart de zero. */
	public static void demarrer() {
		for (Poste poste : POSTES) {
			poste.remettreAZero();
		}
		enMarche = true;
	}

	public static void arreter() {
		enMarche = false;
	}

	/** Le total de tous les postes, en millisecondes par tick. */
	public static double totalMsParTick(long ticks) {
		double total = 0.0D;
		for (Poste poste : POSTES) {
			total += poste.msParTick(ticks);
		}
		return total;
	}

	/**
	 * Un poste de depense.
	 *
	 * <p>Des compteurs atomiques : la collision est mesuree des deux cotes, et
	 * la curiosite depuis le fil de n'importe quel joueur.
	 */
	public static final class Poste {

		private final String nom;
		private final AtomicLong nanos = new AtomicLong();
		private final AtomicLong passages = new AtomicLong();

		private Poste(String nom) {
			this.nom = nom;
		}

		public String nom() {
			return this.nom;
		}

		/**
		 * Le temps passe dans un passage.
		 *
		 * <p>A n'appeler que si {@link Chrono#enMarche()} : sinon on paie deux
		 * appels a l'horloge pour rien, a chaque tick et pour chaque bete.
		 */
		public void ajouter(long duree) {
			this.nanos.addAndGet(duree);
			this.passages.incrementAndGet();
		}

		public long passages() {
			return this.passages.get();
		}

		/** Le temps moyen d'un passage, en nanosecondes. */
		public double moyenneNanos() {
			long combien = this.passages.get();
			return combien == 0L ? 0.0D : this.nanos.get() / (double) combien;
		}

		/**
		 * Le total, ramene a ce que ca represente sur un tick.
		 *
		 * @param ticks combien de ticks se sont ecoules depuis le depart
		 */
		public double msParTick(long ticks) {
			return ticks <= 0L ? 0.0D : this.nanos.get() / 1_000_000.0D / ticks;
		}

		private void remettreAZero() {
			this.nanos.set(0L);
			this.passages.set(0L);
		}
	}
}
