package fr.lhdp.compagnon.entite;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Ce que le compagnon a le droit de faire maintenant.
 *
 * <h2>Le probleme qu'elle regle</h2>
 *
 * <p>Chaque petit comportement ajoute au mod est bon pris seul : il baille, il
 * secoue la tete, il regarde ce qu'on pose, il reclame une caresse, il se met
 * dans le passage. Mis ensemble et laisses libres, ils se declenchent tous en
 * meme temps — et une bete qui fait quatre gestes par seconde ne parait pas plus
 * vivante, elle parait cassee.
 *
 * <p>C'est la piece que les mods de familiers oublient, et c'est elle qui decide
 * si on peut en ajouter trente ou seulement cinq.
 *
 * <h2>Deux delais, pas un</h2>
 *
 * <ul>
 *   <li>Un <b>repos general</b> : au plus un petit geste toutes les quelques
 *       secondes, quel qu'il soit. C'est lui qui donne le rythme.</li>
 *   <li>Un <b>delai par geste</b> : le meme geste ne revient pas avant son
 *       propre delai, beaucoup plus long. C'est lui qui empeche qu'une bete
 *       n'ait qu'une seule idee.</li>
 * </ul>
 *
 * <h2>Ce que ca coute</h2>
 *
 * <p>Une addition par tick et par compagnon, et une petite table d'au plus huit
 * entrees. On ne parcourt rien, on ne cherche rien : les delais sont des dates
 * futures comparees a une horloge qui avance, jamais des comptes a rebours qu'il
 * faudrait decrementer un par un.
 *
 * <p>L'horloge est un {@code long} propre a chaque compagnon, et non le
 * {@code tickCount} du monde : celui-la deborde, et un debordement dans une
 * comparaison de dates avait deja fait voler des compagnons en permanence.
 */
public final class Attention {

	/** Entre deux petits gestes, quels qu'ils soient, en ticks. */
	private static final int REPOS = 20 * 3;

	/**
	 * Combien de gestes on retient au plus.
	 *
	 * <p>Au-dela, on oublie les plus anciens. Un compagnon n'a pas trente
	 * habitudes en cours, et une table qui grandit sans fin sur un serveur a
	 * mille betes finit par se voir.
	 */
	private static final int RETENUS = 8;

	private long horloge;
	private long prochainGeste;

	private final Map<String, Long> attentes = new HashMap<>(RETENUS);

	/** A appeler une fois par tick. C'est tout ce que cette classe coute. */
	public void avancer() {
		this.horloge++;
	}

	/**
	 * Le compagnon peut-il faire ce geste maintenant ?
	 *
	 * <p>Repondre {@code true} <b>consomme</b> le droit : l'appelant doit donc
	 * poser la question au moment ou il ferait vraiment le geste, jamais pour
	 * regarder si ce serait possible.
	 *
	 * @param quoi           le nom du geste, libre
	 * @param avantDeRefaire le delai avant de pouvoir le refaire, en ticks
	 */
	public boolean permet(String quoi, int avantDeRefaire) {
		if (this.horloge < this.prochainGeste) {
			return false;
		}
		Long libre = this.attentes.get(quoi);
		if (libre != null && this.horloge < libre) {
			return false;
		}
		this.prochainGeste = this.horloge + REPOS;
		this.attentes.put(quoi, this.horloge + Math.max(1, avantDeRefaire));
		if (this.attentes.size() > RETENUS) {
			oublierLesVieux();
		}
		return true;
	}

	/** Repousse tout : sert quand il vient de recevoir un ordre. */
	public void faireSilence(int ticks) {
		this.prochainGeste = Math.max(this.prochainGeste, this.horloge + ticks);
	}

	/**
	 * Oublie les delais deja passes, et a defaut le plus proche de l'expiration.
	 *
	 * <p>On ne se donne pas la peine de trier : la table fait huit entrees, et
	 * oublier un delai n'a d'autre effet que de laisser un geste revenir un peu
	 * plus tot que prevu.
	 */
	private void oublierLesVieux() {
		Iterator<Map.Entry<String, Long>> parcours = this.attentes.entrySet().iterator();
		String leProchainLibre = null;
		long tot = Long.MAX_VALUE;
		while (parcours.hasNext()) {
			Map.Entry<String, Long> entree = parcours.next();
			if (entree.getValue() <= this.horloge) {
				parcours.remove();
			} else if (entree.getValue() < tot) {
				tot = entree.getValue();
				leProchainLibre = entree.getKey();
			}
		}
		if (this.attentes.size() > RETENUS && leProchainLibre != null) {
			this.attentes.remove(leProchainLibre);
		}
	}
}
