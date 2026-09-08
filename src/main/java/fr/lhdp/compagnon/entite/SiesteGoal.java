package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.mission.Compteurs;
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

	private final CompagnonEntity compagnon;

	private int reste;
	private int avantDeRegarder;

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
		return this.compagnon.attention().permet("sieste", AVANT_DE_REDORMIR);
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
		this.compagnon.getNavigation().stop();
		this.compagnon.setDort(true);
		Compteurs.compter(this.compagnon, Compteurs.SIESTES);
	}

	@Override
	public boolean canContinueToUse() {
		if (this.reste <= 0 || !this.compagnon.estLibre() && !this.compagnon.mode().pose()) {
			return false;
		}
		// On le reveille en le bousculant, en le montant, ou en l'emmenant nager.
		return !this.compagnon.estMonte() && !this.compagnon.isInWater()
				&& this.compagnon.onGround() && !maitreQuiSAgite();
	}

	@Override
	public void stop() {
		this.compagnon.setDort(false);
		this.reste = 0;
	}

	@Override
	public void tick() {
		this.reste--;
		this.compagnon.getNavigation().stop();
	}

	@Override
	public boolean isInterruptable() {
		// Un ordre, une caresse, un danger : tout passe avant la sieste.
		return true;
	}
}
