package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.contenu.Caractere;
import fr.lhdp.compagnon.fiche.Barre;
import fr.lhdp.compagnon.fiche.FicheCompagnon;
import fr.lhdp.compagnon.fiche.Mode;

import java.util.Random;

/**
 * Il n'obeit pas toujours.
 *
 * <h2>Pourquoi c'est une bonne chose</h2>
 *
 * <p>La complicite montait, et ne servait a rien. On la voyait dans le livre, on
 * la faisait monter en s'occupant de lui, et le compagnon se comportait
 * exactement pareil a dix qu'a quatre-vingt-dix. Une barre qui ne change rien
 * n'est pas une barre, c'est une decoration.
 *
 * <p>Desormais un compagnon qu'on neglige fait la sourde oreille, et il faut s'y
 * reprendre. Ce n'est pas une punition : c'est ce qui donne un sens a tout le
 * reste du mod.
 *
 * <h2>Trois garde-fous</h2>
 *
 * <ul>
 *   <li><b>Il ne refuse jamais de venir.</b> Un compagnon qu'on ne peut plus
 *       rappeler serait perdu, pas desobeissant. « Viens » marche toujours.</li>
 *   <li><b>Il ne refuse jamais deux fois de suite.</b> Le deuxieme essai passe
 *       toujours : on ne bloque personne, on lui demande d'insister.</li>
 *   <li><b>A complicite pleine, il n'a aucune chance de refuser.</b> Celui qui
 *       s'occupe de sa bete n'est jamais embete.</li>
 * </ul>
 */
public final class Obeissance {

	/**
	 * Le refus le plus probable, quand la complicite est a zero.
	 *
	 * <p>Une fois sur trois : assez pour qu'on le remarque, assez rare pour que ca
	 * reste une bete de caractere et pas un objet casse.
	 */
	private static final float REFUS_MAXIMUM = 0.34F;

	/**
	 * Au-dela de cette complicite, il obeit toujours.
	 *
	 * <p>Volontairement bas : la desobeissance doit disparaitre vite, des qu'on
	 * commence a s'occuper de lui. C'est un signal, pas un mur.
	 */
	private static final float COMPLICITE_SUFFISANTE = 60.0F;

	private static final Random HASARD = new Random();

	private Obeissance() {
	}

	/**
	 * Accepte-t-il de faire ce qu'on lui demande ?
	 *
	 * <p>Quand il refuse, il le <b>montre</b> : une bulle apparait au-dessus de sa
	 * tete. Un ordre qui ne se passe rien sans explication ressemblerait a un bogue,
	 * et le joueur croirait le mod casse plutot que sa bete boudeuse.
	 *
	 * @param vers le mode demande, ou {@code null} pour une action de la roue
	 */
	public static boolean accepte(CompagnonEntity compagnon, FicheCompagnon fiche, Mode vers) {
		// On peut toujours rappeler sa bete. Toujours.
		if (vers == Mode.SUIT) {
			return true;
		}
		if (compagnon.vientDeRefuser()) {
			// Il a deja boude au coup precedent : cette fois il y va.
			compagnon.noterRefus(false);
			return true;
		}

		float complicite = fiche.barre(Barre.COMPLICITE);
		if (complicite >= COMPLICITE_SUFFISANTE) {
			return true;
		}

		// Un compagnon tres attache a son proprietaire boude moins, meme mal
		// aime : c'est ce que veut dire « attachement ».
		Caractere caractere = compagnon.caractere();
		float part = 1.0F - complicite / COMPLICITE_SUFFISANTE;
		float chance = REFUS_MAXIMUM * part * (1.2F - caractere.attachement());

		if (HASARD.nextFloat() >= chance) {
			return true;
		}

		// Le refus se dit dans le chat, par l'appelant. Pas de bulle : un compagnon
		// ne commente pas ce qu'il fait.
		compagnon.noterRefus(true);
		return false;
	}
}
