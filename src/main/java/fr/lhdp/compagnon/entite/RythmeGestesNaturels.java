package fr.lhdp.compagnon.entite;

/**
 * Une horloge déterministe pour les gestes de fond.
 *
 * <p>Elle remplace le tirage « une chance sur N » : un compagnon ne lance plus
 * soudainement n'importe quel geste. L'ennui et la vivacité raccourcissent
 * l'attente, une petite variation issue de son identité évite la synchronisation,
 * puis un curseur stable choisit le prochain geste adapté à sa personnalité.
 */
public final class RythmeGestesNaturels {

	private final int graine;
	private int ticksRestants = -1;
	private int curseur;

	public RythmeGestesNaturels(int graine) {
		this.graine = graine;
		this.curseur = Math.floorMod(graine, 97);
	}

	/** Vrai uniquement lorsque le prochain rendez-vous vient d'être atteint. */
	public boolean avancer(int pas, int intervalleBase, float ennui, float vivacite) {
		if (this.ticksRestants < 0) {
			reprogrammer(intervalleBase, ennui, vivacite);
			return false;
		}
		this.ticksRestants -= Math.max(1, pas);
		if (this.ticksRestants > 0) {
			return false;
		}
		this.curseur++;
		reprogrammer(intervalleBase, ennui, vivacite);
		return true;
	}

	public int index(int taille) {
		return taille <= 0 ? 0 : Math.floorMod(this.curseur, taille);
	}

	int ticksRestants() {
		return this.ticksRestants;
	}

	private void reprogrammer(int intervalleBase, float ennui, float vivacite) {
		float ennuiBorne = borner(ennui);
		float vivaciteBornee = borner(vivacite);
		float humeur = 1.18F - ennuiBorne * 0.52F - vivaciteBornee * 0.18F;
		int variation = Math.floorMod(this.graine + this.curseur * 37, 31) - 15;
		float facteurPersonnel = 1.0F + variation / 100.0F;
		this.ticksRestants = Math.max(20 * 18,
				Math.round(Math.max(1, intervalleBase) * humeur * facteurPersonnel));
	}

	private static float borner(float valeur) {
		return Math.max(0.0F, Math.min(1.0F, valeur));
	}
}
