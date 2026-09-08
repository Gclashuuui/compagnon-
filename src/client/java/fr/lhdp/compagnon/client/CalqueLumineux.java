package fr.lhdp.compagnon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.lhdp.compagnon.entite.CompagnonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.texture.AutoGlowingTexture;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ce qui brille sur une bete, dans le noir.
 *
 * <h2>Ce que c'est</h2>
 *
 * <p>A cote de la texture d'une variante, on peut poser une seconde image : le
 * <b>calque lumineux</b>. Tout ce qui y est peint s'affiche a pleine lumiere,
 * quelle que soit celle de l'endroit. Des yeux qui luisent dans un couloir noir,
 * des marques qui s'allument sur le dos, une gueule qui rougeoie.
 *
 * <p>Le reste de la bete continue d'etre eclairee normalement. Ce n'est pas la
 * bete qui brille, ce sont <b>quelques pixels</b> — et c'est ce qui fait la
 * difference entre une creature magique et une lampe.
 *
 * <h2>Ca ne coute rien a celles qui n'en ont pas</h2>
 *
 * <p>Le calque est facultatif, variante par variante. Une espece entiere peut ne
 * jamais en avoir, et une espece de dix couleurs peut n'en donner qu'a trois —
 * ce qui rend justement ces trois-la remarquables.
 *
 * <p>Sans le garde ci-dessous, une variante sans calque afficherait le damier
 * noir et rose par-dessus la bete. On verifie donc que l'image existe <b>avant</b>
 * de dessiner quoi que ce soit.
 *
 * <h2>La convention de nommage</h2>
 *
 * <p>Celle de GeckoLib, pas une a nous : le calque de {@code truc.png} est
 * {@code truc_glowmask.png}, dans le meme dossier. On ne gagne rien a inventer un
 * autre suffixe, et on perd la possibilite de lire la documentation de la
 * bibliotheque.
 */
public class CalqueLumineux extends AutoGlowingGeoLayer<CompagnonEntity> {

	/**
	 * Les textures dont on sait deja si elles ont un calque.
	 *
	 * <p>La question se poserait sinon a chaque image et pour chaque bete : sur
	 * une cour a deux cents compagnons, ca fait douze mille recherches de fichier
	 * par seconde pour une reponse qui ne change jamais.
	 */
	private static final Map<ResourceLocation, Boolean> connues = new ConcurrentHashMap<>();

	public CalqueLumineux(GeoRenderer<CompagnonEntity> rendu) {
		super(rendu);
	}

	/**
	 * Le monde a change, ou les ressources ont ete rechargees.
	 *
	 * <p>Sans cet oubli, un pack de ressources qui ajouterait un calque ne serait
	 * pas vu avant le prochain lancement du jeu.
	 */
	public static void oublier() {
		connues.clear();
	}

	@Override
	public void render(PoseStack pile, CompagnonEntity compagnon, BakedGeoModel modele,
			RenderType type, MultiBufferSource tampons, VertexConsumer sommets,
			float partiel, int lumiere, int recouvrement) {

		// RIEN A DESSINER SI LA VARIANTE N'A PAS DE CALQUE.
		//
		// C'est le seul garde qui compte : GeckoLib ne verifie pas, et une image
		// manquante se dessine en damier noir et rose par-dessus la bete.
		if (!aUnCalque(getTextureResource(compagnon))) {
			return;
		}
		super.render(pile, compagnon, modele, type, tampons, sommets,
				partiel, lumiere, recouvrement);
	}

	/** Vrai si cette texture-la est accompagnee de son calque lumineux. */
	private static boolean aUnCalque(ResourceLocation texture) {
		if (texture == null) {
			return false;
		}
		return connues.computeIfAbsent(texture, base -> {
			Minecraft client = Minecraft.getInstance();
			if (client == null) {
				return false;
			}
			return client.getResourceManager()
					.getResource(AutoGlowingTexture.getEmissiveResource(base))
					.isPresent();
		});
	}
}
