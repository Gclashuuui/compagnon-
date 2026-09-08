package fr.lhdp.compagnon.entite;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Il va voir ce qui vient de se passer.
 *
 * <p>Un compagnon qui ne reagit qu'aux gens reste une decoration. Celui-ci
 * remarque ce qu'on <b>fait</b> : on pose un bloc a cote de lui, il vient
 * regarder ; on en casse un, pareil.
 *
 * <p>C'est le trait <b>curiosite</b> qui decide s'il se derange, et son entrain
 * s'il en a l'energie. Un compagnon epuise laisse passer.
 *
 * <p>Le point d'interet est pose de l'exterieur, par ce qui observe les actions
 * des joueurs. Ce but ne fait que s'en servir.
 */
public class AllerVoirGoal extends Goal {

	/** Il ne se derange pas pour quelque chose de trop loin. Valeur inventee. */
	private static final double PORTEE = 10.0D;

	/** Distance a laquelle il s'estime arrive. Valeur inventee. */
	private static final double ARRET = 1.8D;

	/** Au-dela, il se lasse et repart a ses affaires. Valeur inventee. */
	private static final int DUREE = 120;

	private static final double VITESSE = 1.1D;
	private static final int TICKS_ENTRE_CHEMINS = 10;

	private final CompagnonEntity compagnon;
	private Vec3 point;
	private int reste;
	private int attente;

	public AllerVoirGoal(CompagnonEntity compagnon) {
		this.compagnon = compagnon;
		setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		// Un ordre tient : assis ou couche, il ne se leve pas par curiosite.
		if (this.compagnon.mode().pose()) {
			return false;
		}

		Vec3 candidat = this.compagnon.pointDInteret();
		if (candidat == null) {
			return false;
		}
		if (this.compagnon.position().distanceToSqr(candidat) > PORTEE * PORTEE) {
			this.compagnon.oublierLePoint();
			return false;
		}

		// Curieux et en forme : il y va. Sinon il laisse passer, et le point est
		// oublie pour qu'il ne se demande pas cent fois la meme chose.
		float envie = this.compagnon.caractere().curiosite() * this.compagnon.entrain();
		if (this.compagnon.getRandom().nextFloat() > envie) {
			this.compagnon.oublierLePoint();
			return false;
		}

		this.point = candidat;
		this.compagnon.oublierLePoint();
		this.reste = DUREE;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return this.point != null && this.reste > 0 && !this.compagnon.mode().pose();
	}

	@Override
	public void stop() {
		this.point = null;
		this.compagnon.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (this.point == null) {
			return;
		}
		this.reste--;
		this.compagnon.getLookControl().setLookAt(this.point.x, this.point.y, this.point.z);

		if (this.compagnon.position().distanceToSqr(this.point) <= ARRET * ARRET) {
			// Arrive : il regarde, et c'est tout. Il repartira quand il se lassera.
			this.compagnon.getNavigation().stop();
			return;
		}

		if (this.attente-- > 0) {
			return;
		}
		this.attente = TICKS_ENTRE_CHEMINS;
		this.compagnon.getNavigation().moveTo(this.point.x, this.point.y, this.point.z, VITESSE);
	}
}
