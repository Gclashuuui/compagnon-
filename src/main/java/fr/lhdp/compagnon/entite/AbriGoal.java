package fr.lhdp.compagnon.entite;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * Sous l'orage, il va se mettre a l'abri.
 *
 * <p>Il cherche le premier endroit couvert autour de lui et s'y rend. L'effet de
 * groupe vient tout seul et sans rien coordonner : quand il pleut sur la cour,
 * tous les compagnons libres visent le meme porche, parce que c'est le seul.
 *
 * <p>Il ne le fait que s'il est <b>libre</b>. Un compagnon a qui on a dit de
 * rester assis reste assis, meme trempe : c'est son maitre qui l'a voulu.
 */
public class AbriGoal extends Goal {

	/** Jusqu'ou il cherche un toit, en blocs. Valeur inventee. */
	private static final int RAYON = 10;

	/** Il n'y va pas en flanant : la pluie, ca motive. */
	private static final double VITESSE = 1.2D;

	/** Une chance sur tant, a chaque examen. */
	private static final int RARETE = 40;

	/** Il reste sous l'abri au moins ce temps, en ticks. */
	private static final int DUREE = 20 * 30;

	private final CompagnonEntity compagnon;
	private BlockPos abri;
	private int reste;

	public AbriGoal(CompagnonEntity compagnon) {
		this.compagnon = compagnon;
		setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (!this.compagnon.estLibre() || !ilPleutSurLui()) {
			return false;
		}
		if (this.compagnon.getRandom().nextInt(RARETE) != 0) {
			return false;
		}
		this.abri = chercherUnToit();
		return this.abri != null;
	}

	private boolean ilPleutSurLui() {
		Level niveau = this.compagnon.level();
		return niveau.isRaining() && niveau.canSeeSky(this.compagnon.blockPosition());
	}

	/**
	 * Le point couvert le plus proche.
	 *
	 * <p>On balaie en carres concentriques pour tomber sur le plus proche d'abord,
	 * et on s'arrete des qu'on en tient un. Un carre de dix blocs de rayon fait
	 * quatre cents tests de « voit-on le ciel », mais on n'y vient qu'une fois sur
	 * quarante et seulement quand il pleut vraiment sur lui.
	 */
	private BlockPos chercherUnToit() {
		Level niveau = this.compagnon.level();
		BlockPos depuis = this.compagnon.blockPosition();

		for (int rayon = 2; rayon <= RAYON; rayon++) {
			for (int dx = -rayon; dx <= rayon; dx++) {
				for (int dz = -rayon; dz <= rayon; dz++) {
					// Seulement le bord du carre : l'interieur a deja ete vu au
					// tour precedent.
					if (Math.max(Math.abs(dx), Math.abs(dz)) != rayon) {
						continue;
					}
					BlockPos candidat = depuis.offset(dx, 0, dz);
					if (!niveau.canSeeSky(candidat)
							&& niveau.getBlockState(candidat).isAir()
							&& !niveau.getBlockState(candidat.below()).isAir()) {
						return candidat;
					}
				}
			}
		}
		return null;
	}

	@Override
	public void start() {
		this.reste = DUREE;
		this.compagnon.getNavigation().moveTo(
				this.abri.getX() + 0.5D, this.abri.getY(), this.abri.getZ() + 0.5D, VITESSE);
	}

	@Override
	public boolean canContinueToUse() {
		return this.abri != null
				&& this.reste > 0
				&& this.compagnon.estLibre()
				&& this.compagnon.level().isRaining();
	}

	@Override
	public void stop() {
		this.abri = null;
		this.compagnon.getNavigation().stop();
	}

	@Override
	public void tick() {
		this.reste--;
		if (this.compagnon.getNavigation().isDone() && ilPleutSurLui()) {
			// Arrive quelque part, mais toujours sous la pluie : l'abri visé ne
			// valait rien, on en cherche un autre au prochain examen.
			this.reste = 0;
		}
	}
}
