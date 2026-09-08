package fr.lhdp.compagnon.entite;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * « Monte. »
 *
 * <h2>Le seul ordre qui ne mene nulle part</h2>
 *
 * <p>Tous les autres ont un but : venir, s'asseoir, aller chercher. Celui-ci
 * n'en a aucun. On lui dit de monter, il monte, il tourne au-dessus de vous, et
 * il redescend quand il en a assez.
 *
 * <p>C'est exactement pour ca qu'il vaut la peine. Un dragon qui tourne dans le
 * ciel pendant qu'on fait autre chose, c'est ce qu'on a envie de regarder — et
 * ce n'est possible que depuis que le vol existe pour de bon.
 *
 * <h2>Il redescend toujours</h2>
 *
 * <p>Le compte a rebours descend a chaque tick, et rien ne le remonte a part un
 * nouvel ordre. Une bete qu'on aurait laissee en l'air et oubliee serait une
 * bete perdue : au bout de quinze secondes, elle se pose.
 *
 * <p>Et {@code Vol} n'ayant pas d'etat a eteindre, la gravite revient toute
 * seule des que ce but s'arrete — quelle qu'en soit la raison.
 */
public class PlanerGoal extends Goal {

	/** A quelle hauteur au-dessus de son maitre il aime tourner, en blocs. */
	private static final double HAUTEUR = 7.0D;

	/** Le rayon du cercle qu'il decrit. */
	private static final double RAYON = 4.0D;

	/**
	 * De combien la cible reste en avance sur lui, en radians.
	 *
	 * <p>Un quart de tour. C'est un cap, pas un rendez-vous : il ne l'atteindra
	 * jamais, puisqu'elle se recalcule depuis sa propre position a chaque tick.
	 * Sa vitesse de tour est donc celle de son vol, et non un chiffre impose —
	 * ce qui donne un cercle regulier quelle que soit l'allure de la bete.
	 */
	private static final double AVANCE_SUR_LE_CERCLE = Math.PI / 2.0D;

	/** Faute de maitre a portee, il tourne au-dessus de l'endroit ou il etait. */
	private final CompagnonEntity compagnon;

	private Vec3 centre;

	public PlanerGoal(CompagnonEntity compagnon) {
		this.compagnon = compagnon;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		return this.compagnon.veutLeCiel() && this.compagnon.saitVoler();
	}

	@Override
	public void start() {
		LivingEntity maitre = this.compagnon.getOwner();
		// Autour de son maitre s'il est la, sinon autour d'ici. Dans les deux cas
		// on fige le centre au depart : le suivre en continu ferait un vol nerveux
		// qui se recale sans arret sur les pas du joueur.
		this.centre = maitre != null && maitre.level() == this.compagnon.level()
				? maitre.position()
				: this.compagnon.position();
		this.compagnon.getNavigation().stop();
	}

	@Override
	public boolean canContinueToUse() {
		return this.compagnon.veutLeCiel() && this.compagnon.saitVoler();
	}

	@Override
	public void stop() {
		this.centre = null;
		// On ne coupe rien : Vol s'eteint tout seul des qu'on cesse de le demander.
		this.compagnon.getNavigation().stop();
	}

	@Override
	public void tick() {
		this.compagnon.consommerDuCiel();
		if (this.centre == null) {
			return;
		}

		// LA CIBLE EST TOUJOURS UN PEU DEVANT LUI.
		//
		// La premiere version faisait tourner un point a vitesse fixe et lui
		// demandait de le suivre. Il volait plus vite que le point : il le
		// rattrapait, s'arretait, attendait qu'il reprenne de l'avance, repartait.
		// Vingt fois par seconde. C'est ce qui donnait l'impression d'une image
		// par seconde alors que le jeu tournait normalement.
		//
		// On part maintenant de l'angle ou il se trouve <b>vraiment</b>, et on vise
		// un peu plus loin sur le cercle. La cible ne peut plus etre rattrapee :
		// elle se deplace exactement au rythme ou il avance.
		double versLui = Math.atan2(
			this.compagnon.getZ() - this.centre.z,
			this.compagnon.getX() - this.centre.x);
		double vise = versLui + AVANCE_SUR_LE_CERCLE;

		Vec3 point = new Vec3(
				this.centre.x + Math.cos(vise) * RAYON,
				this.centre.y + HAUTEUR,
				this.centre.z + Math.sin(vise) * RAYON);

		Vol.monterVers(this.compagnon, point);
	}
}
