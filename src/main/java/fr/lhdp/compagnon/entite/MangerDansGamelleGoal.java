package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.fiche.Barre;
import fr.lhdp.compagnon.fiche.FicheCompagnon;
import fr.lhdp.compagnon.fiche.Fiches;
import fr.lhdp.compagnon.objet.BlocGamelle;
import fr.lhdp.compagnon.objet.Objets;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.EnumSet;

/** Il repere le repas de son coin, s'en approche et mange de lui-meme. */
public final class MangerDansGamelleGoal extends Goal {

	private static final double PORTEE = 14.0D;
	private static final double VITESSE = 1.05D;
	private static final double ARRIVE = 1.35D;
	private static final float FAIM_QUI_DECIDE = 85.0F;
	private static final int ENTRE_DEUX_RECHERCHES = 20;
	private static final int AVANT_LA_PREMIERE_BOUCHEE = 14;

	private final CompagnonEntity compagnon;
	private ItemEntity repas;
	private BlockPos gamelle;
	private FicheCompagnon fiche;
	private Fiches fiches;
	private int avantDeChercher;
	private int avantDeManger;
	private int avantDeRecalculer;

	public MangerDansGamelleGoal(CompagnonEntity compagnon) {
		this.compagnon = compagnon;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (--this.avantDeChercher > 0) {
			return false;
		}
		this.avantDeChercher = ENTRE_DEUX_RECHERCHES;
		if (!peutSeMettreATable() || !(this.compagnon.level() instanceof ServerLevel niveau)
				|| this.compagnon.ficheId() == null) {
			return false;
		}

		this.fiches = Fiches.de(niveau.getServer());
		this.fiche = this.fiches.get(this.compagnon.ficheId());
		if (this.fiche == null || this.fiche.barre(Barre.FAIM) > FAIM_QUI_DECIDE) {
			oublier();
			return false;
		}

		AABB alentours = this.compagnon.getBoundingBox().inflate(PORTEE, 5.0D, PORTEE);
		this.repas = niveau.getEntitiesOfClass(ItemEntity.class, alentours,
				this::estUnRepasDansUneGamelle).stream()
				.min(Comparator.comparingDouble(this.compagnon::distanceToSqr))
				.orElse(null);
		if (this.repas == null) {
			oublier();
			return false;
		}
		this.gamelle = positionDeLaGamelle(this.repas);
		return this.gamelle != null;
	}

	private boolean peutSeMettreATable() {
		return this.compagnon.estLibre() && this.compagnon.lesMainsVides()
				&& !this.compagnon.estMonte() && !this.compagnon.isInWater()
				&& !this.compagnon.volDemande();
	}

	private boolean estUnRepasDansUneGamelle(ItemEntity objet) {
		if (!objet.isAlive() || !BlocGamelle.accepte(objet.getItem())) {
			return false;
		}
		BlockPos position = positionDeLaGamelle(objet);
		return position != null && BlocGamelle.contenu(this.compagnon.level(), position) == objet;
	}

	private BlockPos positionDeLaGamelle(ItemEntity objet) {
		BlockPos position = BlockPos.containing(objet.getX(), objet.getY() - 0.24D, objet.getZ());
		return this.compagnon.level().getBlockState(position).is(Objets.GAMELLE)
				? position : null;
	}

	@Override
	public void start() {
		this.avantDeManger = AVANT_LA_PREMIERE_BOUCHEE;
		this.avantDeRecalculer = 0;
		allerVersLaGamelle();
	}

	@Override
	public boolean canContinueToUse() {
		return this.repas != null && this.repas.isAlive() && this.gamelle != null
				&& this.fiche != null && this.fiche.barre(Barre.FAIM) < Barre.MAXIMUM
				&& peutSeMettreATable() && estUnRepasDansUneGamelle(this.repas);
	}

	@Override
	public void tick() {
		this.compagnon.getLookControl().setLookAt(this.repas, 20.0F, 20.0F);
		if (this.compagnon.distanceToSqr(this.repas) > ARRIVE * ARRIVE) {
			if (--this.avantDeRecalculer <= 0 || this.compagnon.getNavigation().isDone()) {
				this.avantDeRecalculer = 10;
				allerVersLaGamelle();
			}
			this.avantDeManger = AVANT_LA_PREMIERE_BOUCHEE;
			return;
		}

		this.compagnon.getNavigation().stop();
		if (--this.avantDeManger > 0) {
			return;
		}
		ItemStack pile = this.repas.getItem();
		if (Interactions.mangerDepuisGamelle(this.compagnon, this.fiche,
				this.fiches, pile, this.gamelle)) {
			if (pile.isEmpty()) {
				this.repas.discard();
			} else {
				this.repas.setItem(pile);
			}
		}
	}

	private void allerVersLaGamelle() {
		if (this.gamelle != null) {
			this.compagnon.getNavigation().moveTo(this.gamelle.getX() + 0.5D,
					this.gamelle.getY(), this.gamelle.getZ() + 0.5D, VITESSE);
		}
	}

	@Override
	public void stop() {
		this.compagnon.getNavigation().stop();
		oublier();
	}

	private void oublier() {
		this.repas = null;
		this.gamelle = null;
		this.fiche = null;
		this.fiches = null;
	}
}
