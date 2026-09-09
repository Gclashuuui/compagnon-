package fr.lhdp.compagnon.entite;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Les capteurs communs de tous les compagnons.
 *
 * <h2>Trois vitesses</h2>
 *
 * <p>Le maître et la météo sont des lectures sans recherche, dix fois moins
 * fréquentes qu'un tick. Les menaces sont cherchées toutes les trois secondes
 * si le maître est là, dix secondes sinon. Un ami est cherché toutes les cinq
 * secondes. Chaque compagnon reçoit un décalage stable tiré de son UUID : mille
 * cerveaux ne se réveillent jamais tous pendant le même tick.
 *
 * <p>Les résultats entrent dans {@link MemoireCourte}. Les buts, les réactions
 * et les souvenirs réutilisent ensuite le même résultat au lieu de refaire leur
 * propre recherche d'entités.
 */
public final class PerceptionCerveau {

	private static final int CAPTEURS_RAPIDES = 10;
	private static final int MENACES_ACTIF = 20 * 3;
	private static final int MENACES_CALME = 20 * 10;
	private static final int COMPAGNONS = 20 * 5;
	private static final double PORTEE_MAITRE = 16.0D;
	private static final double PORTEE_MENACE = 10.0D;
	private static final double PORTEE_AMI = 12.0D;
	private static final float VIE_INQUIETANTE = 0.4F;
	private static final int SOMBRE = 6;
	private static final int RECALCUL_DISTANCE = 20 * 2;
	private static final double DISTANCE_PROCHE = 24.0D;
	private static final double DISTANCE_MOYEN = 64.0D;

	private PerceptionCerveau() {
	}

	public static void actualiser(CompagnonEntity compagnon) {
		if (!(compagnon.level() instanceof ServerLevel)) {
			return;
		}
		MemoireCourte memoire = compagnon.memoireCourte();
		memoire.avancer();
		if (leTourDe(compagnon, RECALCUL_DISTANCE, 91)) {
			actualiserNiveauActivite(compagnon);
		}
		int multiplicateur = compagnon.niveauActiviteCerveau().multiplierPerception();
		if (leTourDe(compagnon, CAPTEURS_RAPIDES * multiplicateur, 0)) {
			capteursRapides(compagnon, memoire);
		}

		// Le capteur rapide a déjà fait la mesure : ne pas redemander le maître à
		// chaque tick uniquement pour choisir une fréquence.
		boolean maitrePresent = memoire.contient(MemoireCourte.Signal.MAITRE_PROCHE);
		int intervalleMenaces = maitrePresent
				&& memoire.contient(MemoireCourte.Signal.OBSCURITE)
				? MENACES_ACTIF : MENACES_CALME;
		if (leTourDe(compagnon, intervalleMenaces * multiplicateur, 17)) {
			chercherMenace(compagnon, memoire, maitrePresent);
		}
		if (compagnon.niveauActiviteCerveau() != NiveauActiviteCerveau.LOINTAIN
				&& leTourDe(compagnon, COMPAGNONS * multiplicateur, 43)) {
			chercherAmi(compagnon, memoire);
		}
	}

	private static void actualiserNiveauActivite(CompagnonEntity compagnon) {
		Player proche = compagnon.level().getNearestPlayer(compagnon,
				DISTANCE_MOYEN);
		if (proche == null) {
			compagnon.setNiveauActiviteCerveau(NiveauActiviteCerveau.LOINTAIN);
			return;
		}
		double distance = compagnon.distanceToSqr(proche);
		compagnon.setNiveauActiviteCerveau(distance <= DISTANCE_PROCHE * DISTANCE_PROCHE
				? NiveauActiviteCerveau.PROCHE : NiveauActiviteCerveau.MOYEN);
	}

	private static void capteursRapides(CompagnonEntity compagnon, MemoireCourte memoire) {
		LivingEntity maitre = compagnon.getOwner();
		if (maitre != null && maitre.isAlive()
				&& compagnon.distanceToSqr(maitre) <= PORTEE_MAITRE * PORTEE_MAITRE) {
			memoire.retenir(MemoireCourte.Signal.MAITRE_PROCHE,
					maitre.blockPosition(), maitre.getUUID(), CAPTEURS_RAPIDES * 3);
			if (maitre.getDeltaMovement().horizontalDistanceSqr() < 0.002D) {
				memoire.retenir(MemoireCourte.Signal.MAITRE_IMMOBILE,
						maitre.blockPosition(), maitre.getUUID(), CAPTEURS_RAPIDES * 3);
			}
			if (maitre instanceof Player joueur && !joueur.isCreative()
					&& !joueur.isSpectator()
					&& joueur.getHealth() <= joueur.getMaxHealth() * VIE_INQUIETANTE) {
				memoire.retenir(MemoireCourte.Signal.MAITRE_EN_DANGER,
						maitre.blockPosition(), maitre.getUUID(), CAPTEURS_RAPIDES * 3);
			}
		}

		ServerLevel niveau = (ServerLevel) compagnon.level();
		BlockPos ou = compagnon.blockPosition();
		if (niveau.isRainingAt(ou.above())) {
			memoire.retenir(MemoireCourte.Signal.PLUIE, ou, CAPTEURS_RAPIDES * 3);
		}
		if (niveau.isThundering() && niveau.canSeeSky(ou)) {
			memoire.retenir(MemoireCourte.Signal.ORAGE, ou, CAPTEURS_RAPIDES * 3);
		}
		if (niveau.getBlockState(ou).is(Blocks.SNOW)
				|| niveau.getBlockState(ou.below()).is(Blocks.SNOW_BLOCK)
				|| niveau.getBlockState(ou.below()).is(Blocks.POWDER_SNOW)) {
			memoire.retenir(MemoireCourte.Signal.NEIGE, ou, CAPTEURS_RAPIDES * 6);
		}
		if (niveau.getBiome(ou).value().coldEnoughToSnow(ou)) {
			memoire.retenir(MemoireCourte.Signal.FROID, ou, CAPTEURS_RAPIDES * 6);
		}
		if (niveau.getMaxLocalRawBrightness(ou) < SOMBRE) {
			memoire.retenir(MemoireCourte.Signal.OBSCURITE, ou, CAPTEURS_RAPIDES * 4);
		}
		if (compagnon.isInWater()) {
			memoire.retenir(MemoireCourte.Signal.EAU, ou, CAPTEURS_RAPIDES * 3);
		}
	}

	private static void chercherMenace(CompagnonEntity compagnon, MemoireCourte memoire,
			boolean maitrePresent) {
		// Prévenir un maître absent ne produit aucun comportement visible. Dans ce
		// cas on oublie l'ancien résultat sans lancer de recherche.
		if (!maitrePresent) {
			memoire.oublier(MemoireCourte.Signal.MENACE);
			return;
		}
		List<Monster> menaces = compagnon.level().getEntitiesOfClass(Monster.class,
				compagnon.getBoundingBox().inflate(PORTEE_MENACE),
				monstre -> monstre.isAlive() && !monstre.isInvisible());
		Monster proche = null;
		double distance = Double.MAX_VALUE;
		for (Monster menace : menaces) {
			double candidate = compagnon.distanceToSqr(menace);
			if (candidate < distance) {
				distance = candidate;
				proche = menace;
			}
		}
		if (proche == null) {
			memoire.oublier(MemoireCourte.Signal.MENACE);
		} else {
			memoire.retenir(MemoireCourte.Signal.MENACE, proche.blockPosition(),
					proche.getUUID(), MENACES_ACTIF + 20);
		}
	}

	private static void chercherAmi(CompagnonEntity compagnon, MemoireCourte memoire) {
		if (!compagnon.estLibre() || !compagnon.lesMainsVides()
				|| Manies.JALOUX.equals(compagnon.defaut())
				|| compagnon.caractere().sociabilite() < 0.2F) {
			memoire.oublier(MemoireCourte.Signal.AMI_PROCHE);
			return;
		}
		List<CompagnonEntity> autour = compagnon.level().getEntitiesOfClass(
				CompagnonEntity.class, compagnon.getBoundingBox().inflate(PORTEE_AMI),
				autre -> autre != compagnon && autre.isAlive()
						&& autre.ficheId() != null
						&& (compagnon.connait(autre.getUUID())
								|| compagnon.connait(autre.ficheId())));
		CompagnonEntity proche = null;
		double distance = Double.MAX_VALUE;
		for (CompagnonEntity autre : autour) {
			double candidate = compagnon.distanceToSqr(autre);
			if (candidate < distance) {
				distance = candidate;
				proche = autre;
			}
		}
		if (proche == null) {
			memoire.oublier(MemoireCourte.Signal.AMI_PROCHE);
		} else {
			memoire.retenir(MemoireCourte.Signal.AMI_PROCHE, proche.blockPosition(),
					proche.getUUID(), COMPAGNONS + 40);
		}
	}

	static boolean leTourDe(CompagnonEntity compagnon, int intervalle, int sel) {
		int decalage = compagnon.getUUID().hashCode() * 31 + sel;
		return Math.floorMod(compagnon.tickCount + decalage, intervalle) == 0;
	}
}
