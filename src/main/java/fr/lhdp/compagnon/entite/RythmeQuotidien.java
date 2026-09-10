package fr.lhdp.compagnon.entite;

/**
 * Deux petites habitudes liées à la journée Minecraft.
 *
 * <p>Chaque compagnon les joue au plus une fois par journée. Le décalage tiré
 * de son UUID évite le réveil parfaitement synchronisé de toute une ménagerie.
 * Deux nombres seulement sont conservés et rien n'est sauvegardé : manquer un
 * rituel pendant un déchargement de chunk n'a aucune conséquence durable.
 */
public final class RythmeQuotidien {

	public enum Moment {
		AUCUN,
		MATIN,
		SOIR
	}

	private static final long TICKS_PAR_JOUR = 24_000L;
	private static final long FIN_MATIN = 2_000L;
	private static final long DEBUT_SOIR = 12_000L;
	private static final long FIN_SOIR = 14_000L;
	private static final int DECALAGE_MAXIMUM = 1_200;

	private long dernierMatin = Long.MIN_VALUE;
	private long dernierSoir = Long.MIN_VALUE;

	/** Rend le prochain rituel dû, et le marque immédiatement comme joué. */
	public Moment prochain(long tempsDuMonde, int graine) {
		long jour = Math.floorDiv(tempsDuMonde, TICKS_PAR_JOUR);
		long heure = Math.floorMod(tempsDuMonde, TICKS_PAR_JOUR);
		long decalage = Math.floorMod(graine, DECALAGE_MAXIMUM);

		if (heure >= decalage && heure < FIN_MATIN && this.dernierMatin != jour) {
			this.dernierMatin = jour;
			return Moment.MATIN;
		}
		if (heure >= DEBUT_SOIR + decalage && heure < FIN_SOIR
				&& this.dernierSoir != jour) {
			this.dernierSoir = jour;
			return Moment.SOIR;
		}
		return Moment.AUCUN;
	}
}
