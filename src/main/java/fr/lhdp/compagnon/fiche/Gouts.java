package fr.lhdp.compagnon.fiche;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** Les preferences alimentaires propres a une bete, figees pour toute sa vie. */
public record Gouts(List<String> preferes, String boude) {

	public static final Gouts VIDES = new Gouts(List.of(), "");

	public Gouts {
		preferes = List.copyOf(new LinkedHashSet<>(preferes));
		boude = boude == null ? "" : boude;
	}

	public enum Avis {
		PREFERE, BOUDE, ORDINAIRE
	}

	/**
	 * Tire deux favoris et un aliment boude, de facon reproductible.
	 *
	 * <p>La liste est triee avant le tirage : le chargeur de ressources peut rendre
	 * les fichiers dans un ordre different d'une machine a l'autre. L'identifiant
	 * de la bete suffit ensuite a retrouver le meme tirage jusqu'a sa sauvegarde.
	 */
	public static Gouts choisir(UUID compagnon, List<String> disponibles) {
		if (compagnon == null || disponibles == null || disponibles.isEmpty()) {
			return VIDES;
		}
		List<String> melanges = new ArrayList<>(new LinkedHashSet<>(disponibles));
		melanges.removeIf(String::isBlank);
		Collections.sort(melanges);
		long graine = compagnon.getMostSignificantBits()
				^ Long.rotateLeft(compagnon.getLeastSignificantBits(), 23);
		Collections.shuffle(melanges, new Random(graine));

		if (melanges.size() == 1) {
			return new Gouts(List.of(melanges.get(0)), "");
		}
		if (melanges.size() == 2) {
			return new Gouts(List.of(melanges.get(0)), melanges.get(1));
		}
		return new Gouts(List.of(melanges.get(0), melanges.get(1)), melanges.get(2));
	}

	public Avis avis(String aliment) {
		if (this.preferes.contains(aliment)) {
			return Avis.PREFERE;
		}
		return !this.boude.isEmpty() && this.boude.equals(aliment)
				? Avis.BOUDE : Avis.ORDINAIRE;
	}

	public boolean estVide() {
		return this.preferes.isEmpty() && this.boude.isEmpty();
	}
}
