package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.mission.Compteurs;
import fr.lhdp.compagnon.fiche.FicheCompagnon;
import fr.lhdp.compagnon.fiche.Fiches;
import fr.lhdp.compagnon.objet.Objets;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Il s'endort pour de bon.
 *
 * <h2>Un besoin qui coupe le service</h2>
 *
 * <p>Jusqu'ici, un compagnon fatigue etait simplement un compagnon plus lent. Il
 * restait disponible en permanence — et une creature toujours disponible est un
 * outil, pas un animal.
 *
 * <p>La sieste est la premiere chose du mod qui rende la bete <b>indisponible</b>
 * sans que ce soit une punition. On peut la reveiller : elle se leve, elle n'a
 * pas l'air ravie, et c'est tout. C'est cette hesitation a la reveiller qu'on
 * cherche a produire.
 *
 * <h2>Les garde-fous</h2>
 *
 * <ul>
 *   <li><b>Un ordre la reveille toujours.</b> Le but s'arrete des que le mode
 *       change ou qu'une action est demandee : on ne perd jamais le controle de
 *       sa bete parce qu'elle avait sommeil.</li>
 *   <li><b>Elle ne dort pas en l'air, ni dans l'eau, ni montee.</b></li>
 *   <li><b>Elle ne dort pas deux fois de suite.</b> Le delai passe par le budget
 *       d'attention, comme tous les autres petits gestes.</li>
 * </ul>
 */
public class SiesteGoal extends Goal {

	/** En dessous de cette energie, il pique du nez. */
	private static final float ENERGIE_QUI_ENDORT = 30.0F;

	/** Il ne s'endort pas si son maitre est en train de bouger tout pres. */
	private static final double PORTEE_DU_MAITRE = 6.0D;

	/** Combien de temps dure une sieste, en ticks. */
	private static final int COURTE = 20 * 8;
	private static final int LONGUE = 20 * 30;

	/** Avant d'en refaire une. Une demi-heure de jeu. */
	private static final int AVANT_DE_REDORMIR = 20 * 60 * 30;

	/** Il ne cherche pas a s'endormir a chaque tick. */
	private static final int ENTRE_DEUX_ENVIES = 20 * 5;
	private static final int PORTEE_COUSSIN = 12;
	private static final int TEMPS_POUR_ATTEINDRE_COUSSIN = 20 * 12;
	private static final double ARRIVE_COUSSIN = 1.45D;
	private static final double VITESSE_VERS_COUSSIN = 0.95D;

	private final CompagnonEntity compagnon;

	private int reste;
	private int avantDeRegarder;
	private int avantDeRecalculer;
	private int tempsPourAtteindre;
	private BlockPos coussin;
	private boolean endormi;

	public SiesteGoal(CompagnonEntity compagnon) {
		this.compagnon = compagnon;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		if (--this.avantDeRegarder > 0) {
			return false;
		}
		this.avantDeRegarder = ENTRE_DEUX_ENVIES;

		if (!this.compagnon.estLibre() || !this.compagnon.lesMainsVides()) {
			return false;
		}
		if (!this.compagnon.onGround() || this.compagnon.isInWater()
				|| this.compagnon.volDemande() || this.compagnon.estMonte()) {
			return false;
		}
		// UN TETU S'ENDORT MEME QUAND SON MAITRE S'AGITE. C'est agacant, c'est
		// exactement ce qu'on veut d'un defaut, et ca tient en un test.
		if (maitreQuiSAgite() && !Manies.TETU.equals(this.compagnon.defaut())) {
			return false;
		}

		// UN PEUREUX NE DORT PAS DEHORS LA NUIT.
		if (Manies.PEUREUX.equals(this.compagnon.defaut())
				&& this.compagnon.level().isNight()
				&& this.compagnon.level().canSeeSky(this.compagnon.blockPosition())) {
			return false;
		}
		if (!ilTombeDeSommeil()) {
			return false;
		}
		if (!this.compagnon.attention().permet("sieste", AVANT_DE_REDORMIR)) {
			return false;
		}
		this.coussin = trouverCoussin();
		return true;
	}

	/** Trouve le coussin pose le plus pres, sans imposer qu'il y en ait un. */
	private BlockPos trouverCoussin() {
		BlockPos centre = this.compagnon.blockPosition();
		BlockPos meilleur = null;
		double distance = Double.MAX_VALUE;
		for (BlockPos position : BlockPos.betweenClosed(
				centre.offset(-PORTEE_COUSSIN, -3, -PORTEE_COUSSIN),
				centre.offset(PORTEE_COUSSIN, 3, PORTEE_COUSSIN))) {
			if (!this.compagnon.level().getBlockState(position).is(Objets.COUSSIN)) {
				continue;
			}
			double candidate = position.distToCenterSqr(this.compagnon.position());
			if (candidate < distance) {
				distance = candidate;
				meilleur = position.immutable();
			}
		}
		return meilleur;
	}

	/**
	 * A-t-il vraiment sommeil ?
	 *
	 * <p>Deux raisons, et deux seulement : il est vide, ou c'est son heure creuse.
	 * Une bete qui s'endormirait au hasard donnerait l'impression d'un bug plutot
	 * que d'un besoin — le joueur doit pouvoir deviner pourquoi.
	 */
	private boolean ilTombeDeSommeil() {
		if (this.compagnon.energie() <= ENERGIE_QUI_ENDORT) {
			return true;
		}
		// La nuit, meme en forme, il pique du nez. Une bete nocturne, elle, non :
		// son espece dira un jour laquelle, en attendant c'est la nuit pour tout
		// le monde.
		return this.compagnon.level().isNight() && this.compagnon.getRandom().nextInt(4) == 0;
	}

	/**
	 * Son maitre est-il tout pres et en mouvement ?
	 *
	 * <p>On ne s'endort pas au milieu d'une partie. S'il te suit, s'il te regarde
	 * marcher, il reste eveille — la sieste est pour les moments creux, et c'est
	 * ce qui fait qu'elle ne gene jamais.
	 */
	private boolean maitreQuiSAgite() {
		LivingEntity maitre = this.compagnon.getOwner();
		if (maitre == null || maitre.isSpectator()) {
			return false;
		}
		if (this.compagnon.distanceToSqr(maitre) > PORTEE_DU_MAITRE * PORTEE_DU_MAITRE) {
			return false;
		}
		return maitre.getDeltaMovement().horizontalDistanceSqr() > 0.004D;
	}

	@Override
	public void start() {
		this.reste = this.compagnon.energie() <= ENERGIE_QUI_ENDORT ? LONGUE : COURTE;
		this.endormi = false;
		this.avantDeRecalculer = 0;
		this.tempsPourAtteindre = TEMPS_POUR_ATTEINDRE_COUSSIN;
		if (coussinAtteint()) {
			sEndormir();
		} else if (this.coussin != null) {
			allerAuCoussin();
		} else {
			sEndormir();
		}
	}

	@Override
	public boolean canContinueToUse() {
		if (this.reste <= 0 || !this.compagnon.estLibre() && !this.compagnon.mode().pose()) {
			return false;
		}
		// On le reveille en le bousculant, en le montant, ou en l'emmenant nager.
		if (this.compagnon.estMonte() || this.compagnon.isInWater()
				|| !this.compagnon.onGround() || maitreQuiSAgite()) {
			return false;
		}
		return this.endormi || this.coussin != null
				&& this.tempsPourAtteindre > 0
				&& this.compagnon.level().getBlockState(this.coussin).is(Objets.COUSSIN);
	}

	@Override
	public void stop() {
		this.compagnon.setDort(false);
		this.compagnon.getNavigation().stop();
		this.reste = 0;
		this.coussin = null;
		this.endormi = false;
		this.tempsPourAtteindre = 0;
	}

	@Override
	public void tick() {
		if (!this.endormi && this.coussin != null) {
			this.tempsPourAtteindre--;
			this.compagnon.getLookControl().setLookAt(this.coussin.getX() + 0.5D,
					this.coussin.getY() + 0.2D, this.coussin.getZ() + 0.5D);
			if (coussinAtteint()) {
				sEndormir();
			} else if (--this.avantDeRecalculer <= 0
					|| this.compagnon.getNavigation().isDone()) {
				this.avantDeRecalculer = 10;
				allerAuCoussin();
			}
			return;
		}
		this.reste--;
		this.compagnon.getNavigation().stop();
	}

	private boolean coussinAtteint() {
		if (this.coussin == null) {
			return false;
		}
		double dx = this.compagnon.getX() - (this.coussin.getX() + 0.5D);
		double dz = this.compagnon.getZ() - (this.coussin.getZ() + 0.5D);
		return dx * dx + dz * dz <= ARRIVE_COUSSIN * ARRIVE_COUSSIN;
	}

	private void allerAuCoussin() {
		this.compagnon.getNavigation().moveTo(this.coussin.getX() + 0.5D,
				this.coussin.getY() + 0.2D, this.coussin.getZ() + 0.5D,
				VITESSE_VERS_COUSSIN);
	}

	private void sEndormir() {
		this.endormi = true;
		this.compagnon.getNavigation().stop();
		this.compagnon.setDort(true);
		Compteurs.compter(this.compagnon, Compteurs.SIESTES);
		if (this.coussin == null || !(this.compagnon.level() instanceof ServerLevel niveau)
				|| this.compagnon.ficheId() == null) {
			return;
		}
		Fiches fiches = Fiches.de(niveau.getServer());
		FicheCompagnon fiche = fiches.get(this.compagnon.ficheId());
		if (fiche == null) {
			return;
		}
		long maintenant = System.currentTimeMillis();
		fiche.marquer("premiere_sieste_coussin", maintenant);
		fiche.retenirLeLieu("sommeil", this.coussin.getX(), this.coussin.getY(),
				this.coussin.getZ(), maintenant);
		fiches.setDirty();
	}

	@Override
	public boolean isInterruptable() {
		// Un ordre, une caresse, un danger : tout passe avant la sieste.
		return true;
	}
}
