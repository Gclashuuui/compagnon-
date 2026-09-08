package fr.lhdp.compagnon.entite;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Ce qui a des ailes s'en sert.
 *
 * <h2>Le probleme</h2>
 *
 * <p>Une mouette posee au pied d'une tour ne savait pas monter. Elle cherchait un
 * escalier, n'en trouvait pas, et finissait par se teleporter — ce qui marche,
 * mais qui a l'air de ce que c'est. Un oiseau qui se teleporte au sommet d'un mur
 * pendant qu'on le regarde, c'est le genre de detail qui casse tout.
 *
 * <h2>Ce que ce n'est pas</h2>
 *
 * <p>Ce n'est <b>pas</b> une navigation volante. Minecraft en propose une, elle
 * remplace toute la facon de se deplacer de la bete, et elle transformerait un
 * compagnon en creature qui flotte en permanence — y compris quand elle marche
 * tranquillement dans un couloir, ou elle serait alors bien pire.
 *
 * <p>C'est un <b>coup d'ailes</b>, demande par un but precis, pour franchir ce
 * qui ne se franchit pas a pied. Le reste du temps, la bete marche comme avant.
 *
 * <h2>Pourquoi on ne peut pas rester coince en l'air</h2>
 *
 * <p>Le vol se <b>redemande a chaque tick</b>. Il n'y a aucun etat a eteindre :
 * si le but qui volait s'arrete, change d'avis, ou disparait, plus personne ne
 * demande, et la gravite revient au tick suivant toute seule.
 *
 * <p>C'est volontairement plus bete qu'un drapeau qu'on leve et qu'on baisse. Un
 * drapeau qu'on oublie de baisser, c'est une mouette qui flotte pour toujours au
 * milieu de la cour, et personne ne saurait pourquoi.
 */
public final class Vol {

	/** Vitesse de montee, en blocs par tick. */
	private static final double MONTEE = 0.22D;

	/** Vitesse horizontale pendant le vol, en blocs par tick. */
	private static final double AVANCE = 0.18D;

	/**
	 * Au-dela de cette difference de hauteur, ca vaut la peine de decoller.
	 *
	 * <p>En dessous, il saute ou il monte une marche. Decoller pour un bloc et
	 * demi serait ridicule, et surtout ca ferait sautiller la bete a chaque
	 * bordure de trottoir.
	 */
	public static final double HAUTEUR_QUI_VAUT_LE_COUP = 2.5D;

	/** De combien il regarde devant lui pour voir s'il va se cogner, en blocs. */
	private static final double PORTEE_DU_REGARD = 1.2D;

	/** De combien il monte en plus quand il a un mur devant. */
	private static final double COUP_DE_REIN = 1.6D;

	/** A quelle vitesse son corps pivote vers sa direction, par tick. */
	private static final float PIVOT = 14.0F;

	/**
	 * Quelle part du chemin vers la vitesse voulue il fait par tick.
	 *
	 * <p>Un cinquieme : sa vitesse est atteinte en un quart de seconde, et tout
	 * ce qui tremble entre deux ticks est lisse. C'est aussi ce qui lui donne
	 * l'inertie d'une bete qui pese quelque chose.
	 */
	private static final double SOUPLESSE = 0.2D;

	private Vol() {
	}

	/**
	 * Cette bete peut-elle franchir cet ecart en volant ?
	 *
	 * @param cible ou elle veut aller
	 */
	public static boolean utilePour(CompagnonEntity compagnon, Vec3 cible) {
		if (!compagnon.saitVoler()) {
			return false;
		}
		double monte = cible.y - compagnon.getY();
		if (monte > HAUTEUR_QUI_VAUT_LE_COUP) {
			return true;
		}
		// DEJA EN L'AIR : IL TERMINE.
		//
		// Sans ceci, il s'arretait de voler des qu'il arrivait a la hauteur de sa
		// cible — c'est-a-dire au ras du parapet, encore a dix blocs de distance —
		// et retombait le long de la facade pour tout recommencer. Une mouette qui
		// monte le long d'une tour et redescend en boucle, indefiniment.
		//
		// Il vole donc jusqu'a etre PRES, pas jusqu'a etre HAUT.
		return compagnon.volDemande()
				&& monte > -1.0D
				&& compagnon.position().distanceTo(cible) > 2.0D;
	}

	/** La meme question, quand la cible est une entite. */
	public static boolean utilePour(CompagnonEntity compagnon, Entity cible) {
		return utilePour(compagnon, cible.position());
	}

	/**
	 * Un coup d'ailes vers ce point. A appeler <b>a chaque tick</b> tant qu'on
	 * veut qu'il vole.
	 *
	 * <h2>Il regarde ou il va</h2>
	 *
	 * <p>Le corps pivote vers la direction du vol, il ne se contente pas de
	 * tourner la tete. Un oiseau qui part a droite en regardant droit devant a
	 * l'air de glisser sur un rail ; pire, un oiseau qui recule sans se retourner
	 * n'a l'air de rien du tout.
	 *
	 * <p>Progressivement, et non d'un coup : un demi-tour instantane est aussi
	 * faux qu'une absence de virage.
	 *
	 * <h2>Il contourne</h2>
	 *
	 * <p>S'il a un mur devant lui, il cesse d'avancer et prend de la hauteur. Il
	 * ne cherche pas a comprendre la forme du batiment — il monte jusqu'a ce qu'il
	 * n'y ait plus rien devant, ce qui est exactement ce que fait un oiseau, et ce
	 * qui marche pour n'importe quelle forme de tour.
	 */
	public static void monterVers(CompagnonEntity compagnon, Vec3 cible) {
		compagnon.demanderLeVol();
		compagnon.getNavigation().stop();

		Vec3 vers = cible.subtract(compagnon.position());
		double horizontale = Math.sqrt(vers.x * vers.x + vers.z * vers.z);

		double dx = 0.0D;
		double dz = 0.0D;
		double dy = vers.y > 0.0D ? MONTEE : Math.max(-MONTEE, vers.y * 0.2D);

		// Le seuil est bas : au-dessus, il n'avancait plus du tout des qu'il
		// approchait de sa cible, puis repartait des qu'elle s'eloignait un peu.
		// C'est ce va-et-vient qui donnait l'impression d'une image par seconde.
		if (horizontale > 0.1D) {
			double versX = vers.x / horizontale;
			double versZ = vers.z / horizontale;

			// Il n'avance qu'une fois a peu pres a la bonne hauteur : monter le
			// long d'une facade se termine toujours mal.
			double frein = vers.y > 1.5D ? 0.35D : 1.0D;

			if (mur(compagnon, versX, versZ)) {
				// Quelque chose devant : il grimpe au lieu d'insister. C'est ce qui
				// remplace le fait de « chercher le tour du batiment » — inutile de
				// comprendre la forme d'une tour pour passer par-dessus.
				dy = MONTEE * COUP_DE_REIN;
			} else {
				dx = versX * AVANCE * frein;
				dz = versZ * AVANCE * frein;
			}
			tournerVers(compagnon, versX, versZ);
		}

		// ON GLISSE VERS LA VITESSE VOULUE, ON NE L'IMPOSE PAS.
		//
		// Elle etait ecrite telle quelle a chaque tick. Tant qu'il fonce vers un
		// point lointain, ca ne se voit pas. Mais des qu'il tourne autour de
		// quelque chose, la direction voulue change a chaque tick — et une
		// vitesse qui change vingt fois par seconde, ce n'est pas un vol, c'est
		// un tremblement. C'est exactement ce qu'on voyait quand il planait.
		//
		// Un cinquieme du chemin par tick : il atteint sa vitesse en un quart de
		// seconde, et toutes les secousses sont lissees au passage. C'est aussi ce
		// qui donne l'inertie d'une bete qui pese quelque chose.
		Vec3 avant = compagnon.getDeltaMovement();
		compagnon.setDeltaMovement(
				avant.x + (dx - avant.x) * SOUPLESSE,
				avant.y + (dy - avant.y) * SOUPLESSE,
				avant.z + (dz - avant.z) * SOUPLESSE);

		// La tete suit la cible, le corps suit la route. C'est ce que fait un
		// oiseau qui vise un perchoir tout en le contournant.
		compagnon.getLookControl().setLookAt(cible.x, cible.y, cible.z);
	}

	/**
	 * Fait pivoter le corps vers la direction du vol, sans a-coup.
	 *
	 * <p>On ecrit {@code yBodyRot} et {@code yHeadRot} en plus de la rotation de
	 * l'entite : sans eux le modele resterait de travers, puisque c'est le corps
	 * qui est dessine, pas la rotation abstraite.
	 */
	private static void tournerVers(CompagnonEntity compagnon, double versX, double versZ) {
		float vise = (float) (Mth.atan2(versZ, versX) * (180.0D / Math.PI)) - 90.0F;
		float tourne = Mth.approachDegrees(compagnon.getYRot(), vise, PIVOT);
		compagnon.setYRot(tourne);
		compagnon.yBodyRot = tourne;
		compagnon.yHeadRot = tourne;
	}

	/**
	 * Y a-t-il quelque chose juste devant lui, a hauteur de corps ?
	 *
	 * <p>Deux hauteurs regardees : le milieu et le haut. Une seule laissait passer
	 * les rebords — il donnait du bec dans le dessous d'un balcon en croyant la
	 * voie libre.
	 */
	private static boolean mur(CompagnonEntity compagnon, double versX, double versZ) {
		Level niveau = compagnon.level();
		double x = compagnon.getX() + versX * PORTEE_DU_REGARD;
		double z = compagnon.getZ() + versZ * PORTEE_DU_REGARD;
		double bas = compagnon.getY() + compagnon.getBbHeight() * 0.5D;
		double haut = compagnon.getY() + compagnon.getBbHeight();

		return plein(niveau, x, bas, z) || plein(niveau, x, haut, z);
	}

	private static boolean plein(Level niveau, double x, double y, double z) {
		BlockPos ou = BlockPos.containing(x, y, z);
		return !niveau.getBlockState(ou).getCollisionShape(niveau, ou).isEmpty();
	}
}
