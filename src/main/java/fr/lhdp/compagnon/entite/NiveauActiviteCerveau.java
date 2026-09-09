package fr.lhdp.compagnon.entite;

/**
 * Niveau de detail du cerveau selon la distance au joueur le plus proche.
 *
 * <p>Le compagnon ne devient jamais inerte : les urgences, les ordres et un
 * trajet deja commence restent examines normalement. Seules les envies de fond
 * (flanerie, curiosite, petits regards...) sont espacees quand personne ne peut
 * les voir. C'est le meme principe qu'un niveau de detail graphique, applique a
 * la reflexion.
 */
public enum NiveauActiviteCerveau {

	PROCHE(1, 1),
	MOYEN(2, 2),
	LOINTAIN(4, 5);

	private final int perception;
	private final int spontanee;

	NiveauActiviteCerveau(int perception, int spontanee) {
		this.perception = perception;
		this.spontanee = spontanee;
	}

	public int multiplierPerception() {
		return this.perception;
	}

	/** Combien de ticks attendre entre deux examens d'une envie non essentielle. */
	public int cadence(CerveauComportement.Noeud noeud) {
		return switch (noeud) {
			case SURVIE, RAPPEL, DESTINATION_DEMANDEE, VOL_DEMANDE,
					OBJET_DEMANDE, JEU_DEMANDE, SUIVRE, ABRI, REPAS, BOISSON, SOMMEIL -> 1;
			case AFFECTION, JOUEUR, AMI, SALUT, REGARD_JOUEUR ->
					this == LOINTAIN ? 4 : this.spontanee;
			default -> this.spontanee;
		};
	}
}
