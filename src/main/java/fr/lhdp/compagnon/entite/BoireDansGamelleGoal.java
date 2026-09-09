package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.fiche.Barre;
import fr.lhdp.compagnon.fiche.FicheCompagnon;
import fr.lhdp.compagnon.fiche.Fiches;
import fr.lhdp.compagnon.objet.BlocGamelle;
import fr.lhdp.compagnon.objet.GamellesEau;
import fr.lhdp.compagnon.progression.Niveaux;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Il rejoint une gamelle d'eau et boit quand son énergie commence à baisser. */
public final class BoireDansGamelleGoal extends Goal {
	private static final Set<UUID> ORDRES = ConcurrentHashMap.newKeySet();

	private static final int PORTEE = 14;
	private static final double VITESSE = 1.0D;
	private static final double ARRIVE = 1.45D;
	private static final float ENERGIE_QUI_DECIDE = 78.0F;
	private static final int ENTRE_DEUX_RECHERCHES = 20 * 5;
	private static final int DUREE_GORGEE = 32;
	private static final int SOUVENIR_BOISSON = 20 * 60 * 2;

	private final CompagnonEntity compagnon;
	private BlockPos gamelle;
	private FicheCompagnon fiche;
	private Fiches fiches;
	private int attente;
	private int avantDeBoire;

	public BoireDansGamelleGoal(CompagnonEntity compagnon) {
		this.compagnon = compagnon;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	/** Demande vocale : elle passe avant le seuil d'énergie et le souvenir récent. */
	public static boolean ordonner(CompagnonEntity compagnon) {
		if (GamellesEau.plusProche(compagnon.level(), compagnon.blockPosition(), PORTEE) == null) {
			return false;
		}
		ORDRES.add(compagnon.getUUID());
		return true;
	}

	@Override
	public boolean canUse() {
		boolean demande = ORDRES.remove(this.compagnon.getUUID());
		if (--this.attente > 0 && !demande) {
			return false;
		}
		this.attente = ENTRE_DEUX_RECHERCHES;
		if (!(this.compagnon.level() instanceof ServerLevel niveau)
				|| this.compagnon.ficheId() == null || !peutBoire()
				|| !demande && this.compagnon.memoireCourte().contient(
						MemoireCourte.Signal.A_BU_RECEMMENT)) {
			return false;
		}
		this.fiches = Fiches.de(niveau.getServer());
		this.fiche = this.fiches.get(this.compagnon.ficheId());
		if (this.fiche == null
				|| !demande && this.fiche.barre(Barre.ENERGIE) > ENERGIE_QUI_DECIDE) {
			oublier();
			return false;
		}
		this.gamelle = GamellesEau.plusProche(niveau,
				this.compagnon.blockPosition(), PORTEE);
		return this.gamelle != null;
	}

	private boolean peutBoire() {
		return this.compagnon.estLibre() && this.compagnon.lesMainsVides()
				&& !this.compagnon.estMonte() && !this.compagnon.volDemande();
	}

	@Override
	public void start() {
		this.avantDeBoire = DUREE_GORGEE;
		aller();
	}

	@Override
	public boolean canContinueToUse() {
		return this.gamelle != null && this.fiche != null && peutBoire()
				&& BlocGamelle.contientEau(this.compagnon.level().getBlockState(this.gamelle));
	}

	@Override
	public void tick() {
		this.compagnon.getLookControl().setLookAt(this.gamelle.getX() + 0.5D,
				this.gamelle.getY() + 0.2D, this.gamelle.getZ() + 0.5D);
		if (this.compagnon.distanceToSqr(this.gamelle.getCenter()) > ARRIVE * ARRIVE) {
			if (this.compagnon.getNavigation().isDone()) {
				aller();
			}
			this.avantDeBoire = DUREE_GORGEE;
			return;
		}
		this.compagnon.getNavigation().stop();
		if (this.avantDeBoire == DUREE_GORGEE) {
			if (!this.compagnon.jouerActionPendant("@boit", DUREE_GORGEE,
					PrioriteAction.BESOIN)) {
				this.compagnon.jouerActionPendant("@mange", DUREE_GORGEE,
						PrioriteAction.BESOIN);
			}
		}
		if (--this.avantDeBoire > 0) {
			return;
		}

		this.compagnon.level().setBlock(this.gamelle,
				this.compagnon.level().getBlockState(this.gamelle)
						.setValue(BlocGamelle.EAU, false), Block.UPDATE_ALL);
		fr.lhdp.compagnon.objet.GamellesEau.retirer(this.compagnon.level(), this.gamelle);
		this.fiche.ajouterBarre(Barre.ENERGIE, 9.0F, Niveaux.progression());
		long maintenant = System.currentTimeMillis();
		this.fiche.marquer("premiere_gamelle_eau", maintenant);
		this.fiche.retenirLeLieu("boisson", this.gamelle.getX(), this.gamelle.getY(),
				this.gamelle.getZ(), maintenant);
		this.fiches.setDirty();
		this.compagnon.memoireCourte().retenir(MemoireCourte.Signal.A_BU_RECEMMENT,
				this.gamelle, SOUVENIR_BOISSON);
		Sons.jouerCeSon(this.compagnon, SoundEvents.GENERIC_DRINK, 0.75F);
		Etincelles.gouttes(this.compagnon);
		oublier();
	}

	private void aller() {
		this.compagnon.getNavigation().moveTo(this.gamelle.getX() + 0.5D,
				this.gamelle.getY(), this.gamelle.getZ() + 0.5D, VITESSE);
	}

	@Override
	public void stop() {
		this.compagnon.getNavigation().stop();
		oublier();
	}

	private void oublier() {
		this.gamelle = null;
		this.fiche = null;
		this.fiches = null;
	}
}
