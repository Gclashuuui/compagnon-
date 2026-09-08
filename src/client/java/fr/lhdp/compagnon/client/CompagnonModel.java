package fr.lhdp.compagnon.client;

import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.entite.CompagnonEntity;
import fr.lhdp.compagnon.espece.Espece;
import fr.lhdp.compagnon.espece.Especes;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

/**
 * Le modele lit tout dans la fiche d'espece : geometrie, animations, texture de
 * la variante. Aucun chemin d'asset n'est ecrit en dur ici.
 *
 * <p>GeckoLib 4.9.2 a deprecie les variantes a un seul argument tout en les
 * laissant abstraites : la logique vit donc dans les variantes a deux arguments,
 * celles que GeckoLib appelle et vers lesquelles il migre, et les anciennes s'y
 * ramenent.
 */
public class CompagnonModel extends GeoModel<CompagnonEntity> {

	/**
	 * Les noms d'os ou peut vivre la tete, du plus precis au plus general.
	 *
	 * <p>Le dragonnet a un os <b>d'actionneur</b> pose sur le meme pivot que sa
	 * tete : c'est celui-la qu'on tourne, ce qui laisse l'os {@code head} entier
	 * a l'animateur. Les especes plus simples n'en ont pas, et on tourne leur
	 * tete directement — l'ajout se fait par-dessus l'animation, jamais a la
	 * place, donc les deux cohabitent.
	 */
	private static final String[] OS_DU_REGARD = {"head_act", "head", "tete"};

	/**
	 * Jusqu'ou la tete tourne toute seule, en degres.
	 *
	 * <p>Au-dela, une tete depasse ce que son cou permet et la bete a l'air
	 * cassee. Le reste du chemin est fait par le corps, qui pivote deja.
	 */
	private static final float LIMITE_LACET = 55.0F;
	private static final float LIMITE_TANGAGE = 35.0F;

	/**
	 * La tete suit le joueur qu'elle regarde.
	 *
	 * <h2>Ce qui ne marchait pas</h2>
	 *
	 * <p>Le mod demandait deja « regarde-le » a dix endroits — quand tu ouvres
	 * sa roue, quand il te previent, quand il te retrouve. Mais rien n'appliquait
	 * ce regard au modele : c'etait tout le corps qui pivotait lentement, et la
	 * tete restait droite. Le geste le plus simple d'un animal ne se voyait pas.
	 *
	 * <h2>Il lache la tete pendant un geste</h2>
	 *
	 * <p>C'est la moitie qui compte. Quatre animations sont <b>entierement</b> des
	 * poses de tete — il t'ecoute, il te juge, il menace, il court apres sa queue.
	 * Si le suivi continuait par-dessus, l'inclinaison que l'animateur a posee
	 * serait ecrasee et son travail ne se verrait jamais.
	 *
	 * <p>Tant qu'un geste joue, le code ne touche donc plus a la tete. L'animateur
	 * a la main pleine et entiere pendant son animation, et le suivi reprend
	 * quand elle est finie.
	 *
	 * <p>On lit l'action dans les donnees <b>synchronisees</b> et non dans
	 * {@code occupe()} : ce compteur-la vit sur le serveur et vaut toujours zero
	 * ici. Le nom de l'animation en cours, lui, arrive bien jusqu'au client.
	 */
	@Override
	public void setCustomAnimations(CompagnonEntity compagnon, long identifiant,
			software.bernie.geckolib.animation.AnimationState<CompagnonEntity> etat) {

		super.setCustomAnimations(compagnon, identifiant, etat);
		if (etat == null || compagnon == null) {
			return;
		}

		// L'EDITEUR REGARDE UNE BETE IMMOBILE.
		//
		// On repose chaque os la ou il etait a la premiere image. Le suivi du
		// regard n'a alors plus lieu d'etre : on cale un objet sur une pose,
		// pas sur une bete qui suit la souris.
		if (Mannequin.actif()) {
			for (software.bernie.geckolib.cache.object.GeoBone os
					: getAnimationProcessor().getRegisteredBones()) {
				Mannequin.figer(os);
			}
			return;
		}

		// UN GESTE A LA MAIN PLEINE SUR LA TETE.
		if (!compagnon.action().isEmpty()) {
			return;
		}

		software.bernie.geckolib.model.data.EntityModelData donnees =
			etat.getData(software.bernie.geckolib.constant.DataTickets.ENTITY_MODEL_DATA);
		if (donnees == null) {
			return;
		}

		software.bernie.geckolib.cache.object.GeoBone tete = osDuRegard();
		if (tete == null) {
			return;
		}

		// ON AJOUTE, ON NE REMPLACE PAS.
		//
		// La rotation de l'animation est deja posee sur l'os a ce moment-la. En
		// l'ecrasant, une bete qui marche cesserait de bouger la tete des qu'elle
		// regarde quelqu'un. En l'ajoutant, elle regarde EN marchant.
		//
		// Les os de GeckoLib sont en radians, les angles du jeu en degres.
		tete.setRotY(tete.getRotY() + net.minecraft.util.Mth.clamp(
			donnees.netHeadYaw(), -LIMITE_LACET, LIMITE_LACET)
			* net.minecraft.util.Mth.DEG_TO_RAD);
		tete.setRotX(tete.getRotX() + net.minecraft.util.Mth.clamp(
			donnees.headPitch(), -LIMITE_TANGAGE, LIMITE_TANGAGE)
			* net.minecraft.util.Mth.DEG_TO_RAD);

		// ON EFFACE LA MARQUE QU'ON VIENT DE POSER.
		//
		// C'est la ligne qui repare la tete folle, et elle merite son paragraphe.
		//
		// Ecrire dans un os le marque comme << anime >>. GeckoLib efface ces
		// marques a la fin de tickAnimation, donc AVANT nous : la notre est la
		// seule qui survit jusqu'a l'image suivante. Et a l'image suivante,
		// GeckoLib ne remet a sa pose de repos que les os NON marques.
		//
		// La tete restait donc a la valeur d'hier, on rajoutait le regard
		// par-dessus, et ainsi de suite : soixante fois par seconde, l'angle
		// s'empilait. La bete ne regardait pas, elle devissait.
		//
		// Le defaut ne se voyait pas sur l'exemple de GeckoLib parce que leur os
		// << head >> est pilote par l'animation, qui le reecrit en entier chaque
		// image. Les notres non : chez les huit bebes dragons, << head >> est un
		// pivot vide dont l'enfant << headController >> porte le crane, et aucune
		// animation ne le touche. Rien ne l'ecrasait, tout s'accumulait.
		//
		// En rendant la marque, on rend a GeckoLib la charge de la remise a zero.
		// Le rendu, lui, ne lit jamais ces marques : effacer ne cache rien.
		tete.resetStateChanges();
	}

	/** Le premier os de tete que ce modele possede, ou {@code null}. */
	private software.bernie.geckolib.cache.object.GeoBone osDuRegard() {
		for (String nom : OS_DU_REGARD) {
			java.util.Optional<software.bernie.geckolib.cache.object.GeoBone> trouve =
				getBone(nom);
			if (trouve.isPresent()) {
				return trouve.get();
			}
		}
		return null;
	}

	@Override
	public ResourceLocation getModelResource(CompagnonEntity compagnon, GeoRenderer<CompagnonEntity> rendu) {
		Espece espece = Especes.get(compagnon.espece());
		return espece != null ? espece.geometrie() : parDefaut(compagnon, "geo/", ".geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(CompagnonEntity compagnon, GeoRenderer<CompagnonEntity> rendu) {
		Espece espece = Especes.get(compagnon.espece());
		return espece != null
				? espece.texture(compagnon.variante())
				: parDefaut(compagnon, "textures/entity/", ".png");
	}

	@Override
	public ResourceLocation getAnimationResource(CompagnonEntity compagnon) {
		Espece espece = Especes.get(compagnon.espece());
		return espece != null
				? espece.animations()
				: parDefaut(compagnon, "animations/", ".animation.json");
	}

	@Deprecated
	@Override
	public ResourceLocation getModelResource(CompagnonEntity compagnon) {
		return getModelResource(compagnon, null);
	}

	@Deprecated
	@Override
	public ResourceLocation getTextureResource(CompagnonEntity compagnon) {
		return getTextureResource(compagnon, null);
	}

	/**
	 * Repli quand aucune fiche d'espece n'a pu etre lue : on devine le chemin
	 * plutot que de planter le rendu. GeckoLib signalera le fichier manquant.
	 */
	private static ResourceLocation parDefaut(CompagnonEntity compagnon, String prefixe, String suffixe) {
		return Compagnon.id(prefixe + compagnon.espece() + suffixe);
	}
}
