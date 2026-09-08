package fr.lhdp.compagnon.entite;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.keyframe.event.ParticleKeyframeEvent;
import software.bernie.geckolib.animation.keyframe.event.SoundKeyframeEvent;

/**
 * Les reperes que l'animateur pose <b>dans</b> l'animation.
 *
 * <h2>Le probleme</h2>
 *
 * <p>Tous les sons du mod partaient de Java, a des moments grossiers : « il
 * decolle », « on le caresse ». Un battement d'ailes ne tombait donc jamais sur
 * l'image ou l'aile descend — il tombait au debut, une fois, et l'animation
 * continuait en silence.
 *
 * <p>Et chaque changement de timing demandait de recompiler le mod. L'animateur
 * ne pouvait pas regler son propre travail.
 *
 * <h2>Ce que GeckoLib sait faire et qu'on n'utilisait pas</h2>
 *
 * <p>Blockbench permet de poser, sur la barre de temps d'une animation, des
 * <b>images-cles de son</b> et des <b>images-cles de particule</b>. GeckoLib les
 * fait remonter au moment exact ou la tete de lecture les traverse. Il suffisait
 * de repondre.
 *
 * <p>Consequence directe : le bruit du battement se pose sur l'image du
 * battement, la poussiere du decollage sur l'image ou les pattes quittent le
 * sol, et tout ca se regle <b>dans Blockbench</b>, sans toucher au code.
 *
 * <h2>Et ca ne coute rien sur le reseau</h2>
 *
 * <p>C'est le vrai gain, et il est enorme pour un serveur a mille joueurs.
 *
 * <p>Un son joue depuis le serveur, c'est un paquet vers chaque client a portee.
 * Une particule aussi. Un son pose dans une animation, <b>zero</b> : chaque
 * client joue deja l'animation de son cote, il en connait donc l'instant exact
 * tout seul.
 *
 * <p>Autrement dit, ce chemin-la permet des betes bien plus bruyantes et bien
 * plus vivantes <b>pour moins cher</b> que ce qu'on fait aujourd'hui.
 *
 * <h2>La convention du @</h2>
 *
 * <p>Comme partout ailleurs dans le mod, un nom qui commence par {@code @} est
 * un <b>role</b>, pas un son : il est resolu dans la fiche d'espece.
 *
 * <ul>
 *   <li>{@code @envol} dans l'animation de la mouette joue le battement du
 *       perroquet ; la meme animation chez le dragonnet jouerait le sien.</li>
 *   <li>{@code minecraft:block.sand.step} joue exactement ce son-la.</li>
 * </ul>
 *
 * <p>C'est ce qui permet d'ecrire une animation une fois et de la donner a une
 * autre espece sans qu'elle emprunte la voix de la premiere.
 *
 * <p>Et un role qu'une espece ne declare pas ne joue rien du tout : une bete
 * muette reste muette, comme avant, sans une erreur.
 */
public final class Reperes {

	private Reperes() {
	}

	/** Un nom qui commence par ca est un role de la fiche d'espece. */
	private static final String PREFIXE_ROLE = "@";

	/** Le volume d'un son pose dans une animation. Discret : ce sont des betes. */
	private static final float VOLUME = 0.5F;

	/**
	 * Ou sortent les particules, selon le mot ecrit dans l'image-cle.
	 *
	 * <p>Volontairement grossier : trois hauteurs, pas une position d'os. Aller
	 * chercher la position reelle d'un os demande le modele cuit, qui n'existe
	 * qu'au moment du rendu — et pour de la poussiere sous les pattes, la
	 * difference ne se voit pas.
	 */
	private static final String PATTES = "pattes";
	private static final String TETE = "tete";

	/** Branche les deux ecouteurs sur un controleur d'animation. */
	public static <T extends CompagnonEntity> AnimationController<T> ecouter(
			AnimationController<T> controleur) {

		return controleur
			.setSoundKeyframeHandler(Reperes::son)
			.setParticleKeyframeHandler(Reperes::particule);
	}

	// --- Les sons -------------------------------------------------------------

	private static void son(SoundKeyframeEvent<? extends CompagnonEntity> evenement) {
		CompagnonEntity compagnon = evenement.getAnimatable();
		String demande = evenement.getKeyframeData().getSound();
		if (compagnon == null || demande == null || demande.isBlank()) {
			return;
		}

		SoundEvent son = resoudre(compagnon, demande.trim());
		if (son == null) {
			return;
		}

		// playLocalSound et non playSound : on est deja sur le client qui joue
		// l'animation. Passer par le serveur ferait un paquet par spectateur pour
		// un son que chacun sait deja jouer.
		compagnon.level().playLocalSound(
			compagnon.getX(), compagnon.getY(), compagnon.getZ(),
			son, net.minecraft.sounds.SoundSource.NEUTRAL,
			VOLUME, Sons.hauteurDe(compagnon), false);
	}

	/**
	 * Le son du jeu que ce nom designe, ou {@code null}.
	 *
	 * <p>Un role passe par la fiche d'espece, un identifiant complet est pris tel
	 * quel. Dans les deux cas, un nom inconnu rend {@code null} plutot que de
	 * lever une erreur : une faute de frappe dans une animation rend un geste
	 * silencieux, elle ne fait pas tomber le client.
	 */
	private static SoundEvent resoudre(CompagnonEntity compagnon, String demande) {
		if (demande.startsWith(PREFIXE_ROLE)) {
			return Sons.sonDe(compagnon, demande.substring(PREFIXE_ROLE.length()));
		}
		ResourceLocation chemin = ResourceLocation.tryParse(demande);
		return chemin == null ? null : BuiltInRegistries.SOUND_EVENT.get(chemin);
	}

	// --- Les particules -------------------------------------------------------

	private static void particule(ParticleKeyframeEvent<? extends CompagnonEntity> evenement) {
		CompagnonEntity compagnon = evenement.getAnimatable();
		String demande = evenement.getKeyframeData().getEffect();
		if (compagnon == null || demande == null || demande.isBlank()) {
			return;
		}

		ParticleOptions quoi = particuleDe(demande.trim());
		if (quoi == null) {
			return;
		}

		double hauteur = hauteurDe(compagnon, evenement.getKeyframeData().getLocator());
		double etalement = compagnon.getBbWidth() * 0.35D;

		// Une seule par image-cle : c'est l'animateur qui decide combien, en en
		// posant plusieurs. Cinq d'un coup ici lui retirerait ce choix.
		compagnon.level().addParticle(quoi,
			compagnon.getX() + (compagnon.getRandom().nextDouble() - 0.5D) * etalement,
			compagnon.getY() + hauteur,
			compagnon.getZ() + (compagnon.getRandom().nextDouble() - 0.5D) * etalement,
			0.0D, 0.0D, 0.0D);
	}

	/**
	 * Seules les particules « simples » sont acceptees.
	 *
	 * <p>Ce sont celles qui ne demandent aucun argument : coeur, nuage, fumee,
	 * eclaboussure, etincelle. Les autres — un bloc, un objet, une couleur — ont
	 * besoin d'une donnee que l'image-cle ne transporte pas, et on prefere ne
	 * rien afficher plutot que d'inventer.
	 */
	private static ParticleOptions particuleDe(String nom) {
		ResourceLocation chemin = ResourceLocation.tryParse(nom);
		if (chemin == null) {
			return null;
		}
		ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(chemin);
		return type instanceof SimpleParticleType simple ? simple : null;
	}

	/** La hauteur de sortie, d'apres le mot pose dans l'image-cle. */
	private static double hauteurDe(CompagnonEntity compagnon, String ou) {
		double haut = compagnon.getBbHeight();
		if (ou == null) {
			return haut * 0.5D;
		}
		String mot = ou.toLowerCase(java.util.Locale.ROOT);
		if (mot.contains(PATTES)) {
			return 0.1D;
		}
		if (mot.contains(TETE)) {
			return haut * 0.85D;
		}
		return haut * 0.5D;
	}
}
