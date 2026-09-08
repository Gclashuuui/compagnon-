package fr.lhdp.compagnon.entite;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;


import java.util.EnumSet;

/**
 * « Viens la ! » — et il vient vraiment.
 *
 * <h2>La difference avec le suivi ordinaire</h2>
 *
 * <p>{@code SuivreProprietaireGoal} marche a cote de son maitre : il vise un
 * point a sa droite et se contente de le suivre. Tant qu'on avance ensemble sur
 * un sol plat, c'est parfait — et des qu'il y a un mur, un escalier ou une
 * cloture, il reste plante de l'autre cote.
 *
 * <p>Ici, c'est un ordre : il doit <b>arriver</b>. Alors il cherche un chemin, il
 * le refait quand on bouge, il grimpe les marches, et s'il n'y arrive vraiment
 * pas, il finit par se debrouiller autrement.
 *
 * <h2>Ce qu'il essaie, dans l'ordre</h2>
 *
 * <ol>
 *   <li><b>Un chemin.</b> Minecraft sait deja calculer un itineraire : escaliers,
 *       portes, detours. On lui demande donc un vrai chemin, pas une direction.</li>
 *   <li><b>Un autre chemin.</b> On recalcule regulierement — celui qui appelle
 *       bouge, et un chemin d'il y a cinq secondes ne mene plus nulle part.</li>
 *   <li><b>Un point d'approche.</b> Quand aucun chemin ne va jusqu'au joueur, on
 *       vise le point atteignable le plus proche de lui. Il s'approche au maximum
 *       au lieu de renoncer sur place.</li>
 *   <li><b>Le saut.</b> En dernier recours, apres plusieurs echecs et seulement
 *       s'il est loin, il se retrouve pres de vous. C'est ce que font les loups
 *       de Minecraft depuis toujours, pour la meme raison : un animal qu'on ne
 *       peut plus recuperer est un animal perdu.</li>
 * </ol>
 */
public class VenirIciGoal extends Goal {

	/** Il a fini quand il est aussi pres, en blocs. */
	private static final double ARRIVE = 2.0D;

	/** Il vient d'un bon pas : on vient de l'appeler. */
	private static final double VITESSE = 1.25D;

	/** On refait le chemin tous les tant de ticks, parce que le joueur bouge. */
	private static final int REPENSER = 20;

	/**
	 * Combien de temps il s'obstine, en ticks.
	 *
	 * <p>Trente secondes : de quoi traverser un chateau. Au-dela, ce n'est plus
	 * qu'il cherche, c'est qu'il ne peut pas.
	 */
	private static final int PATIENCE = 20 * 30;

	/** Apres tant de chemins impossibles d'affilee, il se rapproche autrement. */
	private static final int ECHECS_AVANT_LE_SAUT = 4;

	/**
	 * En dessous de cette distance, jamais de saut.
	 *
	 * <p>Voir quelqu'un se teleporter a trois blocs de soi est ridicule, et
	 * casserait completement l'illusion. De loin, on ne le voit pas arriver.
	 */
	private static final double DISTANCE_MINIMALE_DU_SAUT = 12.0D;

	/** Autour du joueur, ou l'on cherche un endroit ou le poser. */
	private static final int RAYON_DU_SAUT = 3;

	private final CompagnonEntity compagnon;
	private LivingEntity appelant;
	private int reste;
	private int avantDeRepenser;
	private int echecs;

	public VenirIciGoal(CompagnonEntity compagnon) {
		this.compagnon = compagnon;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity qui = this.compagnon.quiLAppelle();
		return qui != null && qui.isAlive() && qui.level() == this.compagnon.level();
	}

	@Override
	public void start() {
		this.appelant = this.compagnon.quiLAppelle();
		this.reste = PATIENCE;
		this.avantDeRepenser = 0;
		this.echecs = 0;
	}

	@Override
	public boolean canContinueToUse() {
		if (this.appelant == null || !this.appelant.isAlive() || this.reste <= 0) {
			return false;
		}
		if (this.appelant.level() != this.compagnon.level()) {
			return false;
		}
		// Arrive : on rend la main au suivi ordinaire, qui prendra le relais.
		return this.compagnon.distanceToSqr(this.appelant) > ARRIVE * ARRIVE;
	}

	@Override
	public void stop() {
		this.compagnon.plusPersonneNeLAppelle();
		this.compagnon.getNavigation().stop();
		this.appelant = null;
	}

	@Override
	public void tick() {
		this.reste--;
		this.compagnon.getLookControl().setLookAt(this.appelant, 30.0F, 30.0F);

		// IL A DES AILES, IL S'EN SERT.
		//
		// Des qu'un chemin a pied a echoue et que l'appelant est nettement plus
		// haut, il decolle au lieu de chercher un escalier qui n'existe pas. Une
		// mouette au pied d'une tour ne fait pas le tour du batiment.
		//
		// Ce test passe AVANT le compte a rebours : le vol se redemande a chaque
		// tick, sinon la gravite reviendrait entre deux reflexions et il
		// monterait en escalier.
		if (this.echecs > 0 && Vol.utilePour(this.compagnon, this.appelant)) {
			Vol.monterVers(this.compagnon, this.appelant.position());
			return;
		}

		if (--this.avantDeRepenser > 0) {
			return;
		}
		this.avantDeRepenser = REPENSER;

		// Un vrai chemin jusqu'a lui, escaliers et detours compris.
		Path chemin = this.compagnon.getNavigation().createPath(this.appelant, 0);
		if (chemin != null && chemin.canReach()) {
			// On ne remet PAS le compteur a zero tant qu'il est en l'air.
			//
			// Depuis les airs, le chemin vers le sol se trouve souvent : le
			// compteur retombait a zero, le vol s'arretait, la bete tombait, le
			// chemin echouait de nouveau, elle redecollait. C'est precisement le
			// va-et-vient qu'on voyait, et il pouvait durer indefiniment.
			if (!this.compagnon.volDemande()) {
				this.echecs = 0;
			}
			this.compagnon.getNavigation().moveTo(chemin, VITESSE);
			return;
		}

		// Pas de chemin complet : on vise le point atteignable le plus proche de
		// lui. Mieux vaut s'approcher que rester plante.
		this.echecs++;
		if (chemin != null) {
			this.compagnon.getNavigation().moveTo(chemin, VITESSE);
		}

		// IL ATTEND A LA PORTE.
		//
		// Arrive au plus pres et toujours sans chemin : il s'assoit et regarde dans
		// ta direction, au lieu de pietiner contre le mur. Un animal qui attend
		// devant une porte a l'air d'attendre ; le meme qui tourne en rond a l'air
		// casse.
		if (this.compagnon.getNavigation().isDone()
				&& this.compagnon.distanceToSqr(this.appelant) > ARRIVE * ARRIVE) {
			this.compagnon.getNavigation().stop();
		}

		// Le saut reste le dernier recours de ceux qui marchent. Une bete qui
		// vole n'en a pas besoin : elle finira par arriver, et on la voit venir.
		if (this.echecs >= ECHECS_AVANT_LE_SAUT
				&& !this.compagnon.saitVoler()
				&& this.compagnon.distanceToSqr(this.appelant)
						> DISTANCE_MINIMALE_DU_SAUT * DISTANCE_MINIMALE_DU_SAUT) {
			seRapprocher();
		}
	}

	/**
	 * Il se retrouve pres de vous.
	 *
	 * <p>On cherche un endroit ou il tient debout, et on n'y va que si on en
	 * trouve un : le poser dans un mur ou au-dessus du vide serait pire que de le
	 * laisser chercher.
	 */
	private void seRapprocher() {
		BlockPos autour = this.appelant.blockPosition();

		for (int essai = 0; essai < 12; essai++) {
			int dx = this.compagnon.getRandom().nextInt(RAYON_DU_SAUT * 2 + 1) - RAYON_DU_SAUT;
			int dy = this.compagnon.getRandom().nextInt(3) - 1;
			int dz = this.compagnon.getRandom().nextInt(RAYON_DU_SAUT * 2 + 1) - RAYON_DU_SAUT;
			BlockPos ou = autour.offset(dx, dy, dz);

			if (!convient(ou)) {
				continue;
			}
			this.compagnon.moveTo(ou.getX() + 0.5D, ou.getY(), ou.getZ() + 0.5D,
					this.compagnon.getYRot(), this.compagnon.getXRot());
			this.compagnon.getNavigation().stop();
			this.echecs = 0;
			return;
		}
	}

	/**
	 * De la place pour lui, et un sol sous ses pattes.
	 *
	 * <p>Verifie a la main plutot qu'avec l'evaluateur de chemin de Minecraft :
	 * celui-ci a change de nom entre deux versions, et deux blocs plus une boite
	 * de collision repondent a la seule question qui compte — tient-il debout ici
	 * sans etre dans un mur ?
	 */
	private boolean convient(BlockPos ou) {
		// Du sol dessous, sinon il tomberait.
		if (this.compagnon.level().getBlockState(ou.below()).isAir()) {
			return false;
		}
		return this.compagnon.level().noCollision(this.compagnon,
				this.compagnon.getBoundingBox().move(
						ou.getX() + 0.5D - this.compagnon.getX(),
						ou.getY() - this.compagnon.getY(),
						ou.getZ() + 0.5D - this.compagnon.getZ()));
	}
}
