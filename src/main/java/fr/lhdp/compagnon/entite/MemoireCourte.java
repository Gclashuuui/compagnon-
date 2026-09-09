package fr.lhdp.compagnon.entite;

import net.minecraft.core.BlockPos;

import java.util.UUID;

/**
 * La mémoire de travail du cerveau, volontairement petite et non sauvegardée.
 *
 * <p>Un compagnon n'a pas besoin de conserver chaque bruit et chaque entité.
 * Il lui suffit de savoir, pendant quelques secondes, où était la menace, si
 * son maître attendait et quel ami se trouvait près de lui. La mémoire longue
 * (lieux, habitudes, connaissances et premières fois) reste dans sa fiche.
 *
 * <p>Il n'y a ici ni carte, ni liste qui grandit : trois tableaux de la taille
 * exacte de {@link Signal}. Retenir remplace l'ancien souvenir du même type.
 * Mille compagnons occupent donc strictement la même quantité maximale de
 * mémoire aujourd'hui et dans six mois.
 */
public final class MemoireCourte {

	public enum Signal {
		MAITRE_PROCHE,
		MAITRE_IMMOBILE,
		MAITRE_EN_DANGER,
		EVENEMENT_DU_MONDE,
		MENACE,
		AMI_PROCHE,
		PLUIE,
		ORAGE,
		NEIGE,
		FROID,
		OBSCURITE,
		EAU
	}

	private final long[] expiration = new long[Signal.values().length];
	private final long[] positions = new long[Signal.values().length];
	private final boolean[] aUnePosition = new boolean[Signal.values().length];
	private final UUID[] sources = new UUID[Signal.values().length];
	private long horloge;

	/** Une addition par tick, sans parcourir ni nettoyer les souvenirs. */
	public void avancer() {
		this.horloge++;
	}

	public void retenir(Signal signal, BlockPos position, UUID source, int dureeTicks) {
		int index = signal.ordinal();
		this.expiration[index] = this.horloge + Math.max(1, dureeTicks);
		this.sources[index] = source;
		if (position == null) {
			this.aUnePosition[index] = false;
		} else {
			this.positions[index] = position.asLong();
			this.aUnePosition[index] = true;
		}
	}

	public void retenir(Signal signal, BlockPos position, int dureeTicks) {
		retenir(signal, position, null, dureeTicks);
	}

	public boolean contient(Signal signal) {
		return this.expiration[signal.ordinal()] > this.horloge;
	}

	public BlockPos positionDe(Signal signal) {
		int index = signal.ordinal();
		return contient(signal) && this.aUnePosition[index]
				? BlockPos.of(this.positions[index]) : null;
	}

	public UUID sourceDe(Signal signal) {
		return contient(signal) ? this.sources[signal.ordinal()] : null;
	}

	public void oublier(Signal signal) {
		int index = signal.ordinal();
		this.expiration[index] = 0L;
		this.sources[index] = null;
		this.aUnePosition[index] = false;
	}

	/** Seulement pour le diagnostic : le parcours porte sur dix cases fixes. */
	public int combienDActifs() {
		int actifs = 0;
		for (long jusquA : this.expiration) {
			if (jusquA > this.horloge) {
				actifs++;
			}
		}
		return actifs;
	}
}
