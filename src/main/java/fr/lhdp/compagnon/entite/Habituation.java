package fr.lhdp.compagnon.entite;

import net.minecraft.core.BlockPos;

/**
 * Empêche une activité répétitive de capturer toute l'attention du compagnon.
 *
 * <p>Quand un joueur construit, vingt blocs peuvent être posés au même endroit
 * en quelques secondes. Les deux premiers restent intéressants ; ensuite le
 * compagnon comprend que cela continue et ne relève plus qu'un rappel sur cinq.
 * Un événement plus loin ou après une pause redevient immédiatement nouveau.
 *
 * <p>Une position, une date et un compteur : aucune liste qui grandit.
 */
public final class Habituation {

	private static final int MEME_ZONE = 3;
	private static final long OUBLI_APRES = 20L * 10L;
	private static final int RAPPEL_TOUS_LES = 5;

	private boolean aUnEvenement;
	private long dernierePosition;
	private long dernierTick;
	private int repetitions;

	public boolean accepte(BlockPos position, long maintenant) {
		if (position == null) {
			return false;
		}
		boolean memeSerie = this.aUnEvenement
				&& maintenant - this.dernierTick <= OUBLI_APRES
				&& dansLaMemeZone(BlockPos.of(this.dernierePosition), position);

		this.aUnEvenement = true;
		this.dernierePosition = position.asLong();
		this.dernierTick = maintenant;
		if (!memeSerie) {
			this.repetitions = 1;
			return true;
		}

		if (this.repetitions < Integer.MAX_VALUE) {
			this.repetitions++;
		}
		return this.repetitions <= 2 || this.repetitions % RAPPEL_TOUS_LES == 0;
	}

	private static boolean dansLaMemeZone(BlockPos gauche, BlockPos droite) {
		return Math.abs(gauche.getX() - droite.getX()) <= MEME_ZONE
				&& Math.abs(gauche.getY() - droite.getY()) <= MEME_ZONE
				&& Math.abs(gauche.getZ() - droite.getZ()) <= MEME_ZONE;
	}
}
