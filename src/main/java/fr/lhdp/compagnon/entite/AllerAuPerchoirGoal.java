package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.fiche.FicheCompagnon;
import fr.lhdp.compagnon.fiche.Fiches;
import fr.lhdp.compagnon.fiche.Mode;
import fr.lhdp.compagnon.objet.Objets;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** L'ordre explicite « va à ce perchoir », donné après avoir montré le meuble. */
public final class AllerAuPerchoirGoal extends Goal {

	private static final Map<UUID, BlockPos> ORDRES = new ConcurrentHashMap<>();
	private static final double VITESSE = 1.05D;
	private static final double ARRIVE = 1.65D;
	private static final int TEMPS_MAXIMUM = 20 * 15;

	private final CompagnonEntity compagnon;
	private BlockPos perchoir;
	private int reste;
	private int avantDeRecalculer;
	private boolean installe;

	public AllerAuPerchoirGoal(CompagnonEntity compagnon) {
		this.compagnon = compagnon;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	public static void ordonner(CompagnonEntity compagnon, BlockPos perchoir) {
		ORDRES.put(compagnon.getUUID(), perchoir.immutable());
	}

	@Override
	public boolean canUse() {
		this.perchoir = ORDRES.remove(this.compagnon.getUUID());
		return this.perchoir != null && !this.compagnon.estMonte()
				&& this.compagnon.level().getBlockState(this.perchoir).is(Objets.PERCHOIR);
	}

	@Override
	public void start() {
		this.reste = TEMPS_MAXIMUM;
		this.avantDeRecalculer = 0;
		this.installe = false;
		this.compagnon.setDort(false);
		this.compagnon.appliquerMode(Mode.RESTE);
		aller();
	}

	@Override
	public boolean canContinueToUse() {
		return !this.installe && this.reste > 0 && this.perchoir != null
				&& !this.compagnon.estMonte()
				&& this.compagnon.level().getBlockState(this.perchoir).is(Objets.PERCHOIR);
	}

	@Override
	public void tick() {
		this.reste--;
		this.compagnon.getLookControl().setLookAt(this.perchoir.getX() + 0.5D,
				this.perchoir.getY() + 0.75D, this.perchoir.getZ() + 0.5D);
		if (estArrive()) {
			installer();
			return;
		}
		if (--this.avantDeRecalculer <= 0 || this.compagnon.getNavigation().isDone()) {
			this.avantDeRecalculer = 10;
			aller();
		}
	}

	private boolean estArrive() {
		double dx = this.compagnon.getX() - (this.perchoir.getX() + 0.5D);
		double dz = this.compagnon.getZ() - (this.perchoir.getZ() + 0.5D);
		return dx * dx + dz * dz <= ARRIVE * ARRIVE;
	}

	private void aller() {
		this.compagnon.getNavigation().moveTo(this.perchoir.getX() + 0.5D,
				this.perchoir.getY(), this.perchoir.getZ() + 0.5D, VITESSE);
	}

	private void installer() {
		this.installe = true;
		this.compagnon.getNavigation().stop();
		this.compagnon.appliquerMode(Mode.ASSIS);
		if (!(this.compagnon.level() instanceof ServerLevel niveau)
				|| this.compagnon.ficheId() == null) {
			return;
		}
		Fiches fiches = Fiches.de(niveau.getServer());
		FicheCompagnon fiche = fiches.get(this.compagnon.ficheId());
		if (fiche == null) {
			return;
		}
		long maintenant = System.currentTimeMillis();
		fiche.setMode(Mode.ASSIS);
		fiche.retenirLeLieu("perchoir", this.perchoir.getX(), this.perchoir.getY(),
				this.perchoir.getZ(), maintenant);
		fiche.marquer("premier_perchoir", maintenant);
		fiches.setDirty();
	}

	@Override
	public void stop() {
		this.compagnon.getNavigation().stop();
		this.perchoir = null;
	}
}
