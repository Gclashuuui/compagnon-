package fr.lhdp.compagnon.entite;

/**
 * Ce qui a le droit d'interrompre une animation déjà engagée.
 *
 * <p>À rang égal, le geste en cours gagne : une animation commencée va au bout.
 * Seul quelque chose de réellement plus important peut la remplacer.
 */
public enum PrioriteAction {
	AMBIANCE,
	AFFECTIF,
	BESOIN,
	ORDRE,
	EVENEMENT,
	URGENCE;

	public boolean interrompt(PrioriteAction enCours) {
		return this.ordinal() > enCours.ordinal();
	}
}
