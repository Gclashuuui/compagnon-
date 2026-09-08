package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.contenu.Caractere;
import fr.lhdp.compagnon.fiche.Mode;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Il marche a cote de son maitre, pas derriere lui.
 *
 * <p>Le suivi de Minecraft — celui des loups — court derriere le joueur, s'arrete
 * quand il est assez pres, repart quand il est trop loin. Cela donne un pet qui
 * zigzague et se cogne dans les murs.
 *
 * <p>Ici il vise un point precis : <b>a la droite du maitre, a un demi-bloc</b>.
 * Il se cale donc a son allure au lieu de le poursuivre, tourne avec lui, et
 * s'arrete quand il s'arrete.
 *
 * <p>Le caractere module la distance : un compagnon tres attache se colle,
 * un independant garde ses distances.
 */
public class SuivreProprietaireGoal extends Goal {

	/** Distance a la droite du maitre, en blocs. Valeur inventee. */
	private static final double COTE = 0.6D;

	/** Un peu en arriere de son epaule, pour ne pas lui couper la route. */
	private static final double RECUL = 0.2D;

	/** En deca, il est deja bien place : on ne le fait pas gigoter. */
	private static final double TOLERANCE = 0.55D;

	/**
	 * Tolerance quand le maitre est arrete. Beaucoup plus large : une fois pose a
	 * cote de lui, il n'a aucune raison de bouger encore.
	 */
	private static final double TOLERANCE_ARRET = 1.6D;

	/** En dessous de cette vitesse, on considere que le maitre est arrete. */
	private static final double IMMOBILE = 0.02D;

	/** Au-dela, il a vraiment decroche : il court. */
	private static final double DISTANCE_COURSE = 5.0D;

	/** Au-dela, il abandonne la marche et se teleporte. */
	private static final double DISTANCE_RATTRAPAGE = 20.0D;

	private static final double VITESSE_MARCHE = 1.0D;
	private static final double VITESSE_COURSE = 1.35D;

	/** On ne recalcule pas un chemin a chaque tick : c'est ce qui coute. */
	private static final int TICKS_ENTRE_CHEMINS = 8;

	private final CompagnonEntity compagnon;
	private LivingEntity proprietaire;
	private int attente;

	public SuivreProprietaireGoal(CompagnonEntity compagnon) {
		this.compagnon = compagnon;
		setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (this.compagnon.mode() != Mode.SUIT || this.compagnon.estPerche()) {
			return false;
		}
		LivingEntity maitre = this.compagnon.getOwner();
		if (maitre == null || maitre.isSpectator()) {
			return false;
		}
		this.proprietaire = maitre;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return this.compagnon.mode() == Mode.SUIT
				&& !this.compagnon.estPerche()
				&& this.proprietaire != null
				&& !this.proprietaire.isSpectator();
	}

	@Override
	public void stop() {
		this.proprietaire = null;
		this.compagnon.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (this.proprietaire == null) {
			return;
		}

		Vec3 cible = placeACote();
		double distance = this.compagnon.position().distanceTo(cible);

		// Le maitre est monte quelque part : il le rejoint en volant plutot que
		// de reapparaitre a cote de lui. On voit l'oiseau monter, et c'est tout
		// ce qu'on demandait.
		//
		// Avant la teleportation de rattrapage, exprès : celle-ci reste pour ceux
		// qui marchent, quand ils sont vraiment distances.
		// Sans condition sur la navigation. Elle en portait une, et c'etait une
		// des sources du vol en dents de scie : au premier tick d'un envol la
		// navigation pouvait ne pas encore etre arretee, le vol etait saute pour
		// ce tick-la, et la bete retombait. monterVers arrete la navigation
		// lui-meme : la condition ne servait qu'a se faire du mal.
		if (Vol.utilePour(this.compagnon, cible)) {
			Vol.monterVers(this.compagnon, cible);
			return;
		}

		// ON NE TELEPORTE PAS UNE BETE EN VOL.
		//
		// Elle est en train d'arriver, et on la voit arriver : la faire
		// reapparaitre au dernier moment gache tout le trajet, et donne
		// exactement l'impression de saccade qu'on cherche a enlever.
		if (distance > DISTANCE_RATTRAPAGE && !this.compagnon.volDemande()) {
			rattraper(cible);
			return;
		}

		// Quand le maitre est immobile, on est bien plus tolerant : sinon le
			// moindre pivotement de camera deplacerait le point vise, et le compagnon
			// passerait sa vie a se recaler autour de lui.
		boolean maitreImmobile = this.proprietaire.getDeltaMovement()
				.horizontalDistanceSqr() < IMMOBILE * IMMOBILE;
		double tolerance = maitreImmobile ? TOLERANCE_ARRET : TOLERANCE;

		// IL SE RAPPROCHE QUAND SON MAITRE VA MAL.
		//
		// Il ne soigne rien et ne dit rien : il cesse simplement de garder ses
		// distances. C est tout ce qu un animal peut faire, et c est exactement ce
		// qu on remarque en sortant d un mauvais combat.
		//
		// Meme un independant se rapproche : la ou les cinq penchants decident de
		// tout le reste, celui-ci passe avant eux.
		if (Presence.maitreEnMauvaisEtat(this.compagnon)) {
			tolerance = Math.min(tolerance, TOLERANCE);
		}

		if (distance < tolerance) {
			// Il est bien place. On le laisse tranquille plutot que de lui faire
			// corriger sa position sans arret.
			this.compagnon.getNavigation().stop();

			// ET C'EST SEULEMENT MAINTENANT QU'IL TE REGARDE.
			//
			// Avant, ce regard etait pose a chaque tick, y compris pendant qu'il
			// marchait. Le corps suit la tete : il gardait donc le nez sur son
			// maitre et se deplacait <b>de cote ou a reculons</b> pour se recaler.
			// Une bete qui recule sans se retourner n'a l'air de rien de vivant.
			//
			// Pose une fois arrete, le regard fait ce qu'on lui demandait : il
			// marche en regardant devant, et il te regarde quand il s'arrete.
			this.compagnon.getLookControl().setLookAt(this.proprietaire, 10.0F, 10.0F);
			return;
		}

		if (this.attente-- > 0) {
			return;
		}
		this.attente = TICKS_ENTRE_CHEMINS;

		double vitesse = distance > DISTANCE_COURSE ? VITESSE_COURSE : VITESSE_MARCHE;
		this.compagnon.getNavigation().moveTo(cible.x, cible.y, cible.z, vitesse);
	}

	/**
	 * Le point vise : a la droite du maitre.
	 *
	 * <p>Le vecteur « droite » s'obtient en croisant le regard avec la verticale.
	 * Il tourne donc avec le maitre, et le compagnon reste du bon cote meme quand
	 * celui-ci fait demi-tour.
	 */
	private Vec3 placeACote() {
		Vec3 regard = this.proprietaire.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize();
		if (regard.lengthSqr() < 1.0E-4D) {
			return this.proprietaire.position();
		}
		Vec3 droite = regard.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();

		double ecart = Caractere.peser((float) COTE, 1.0F - this.compagnon.caractere().attachement());

		// Le produit vectoriel regard x verticale donne deja la droite du maitre :
		// on ajoute, on ne soustrait pas, sinon il marcherait a gauche.
		return this.proprietaire.position()
				.add(droite.scale(ecart))
				.subtract(regard.scale(RECUL));
	}

	/** Trop loin pour marcher : on le repose a cote, sans bruit. */
	private void rattraper(Vec3 cible) {
		this.compagnon.getNavigation().stop();
		this.compagnon.moveTo(cible.x, cible.y, cible.z,
				this.proprietaire.getYRot(), this.compagnon.getXRot());
	}
}
