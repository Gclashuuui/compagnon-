package fr.lhdp.compagnon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.lhdp.compagnon.entite.CompagnonEntity;
import fr.lhdp.compagnon.espece.Ancrage;
import fr.lhdp.compagnon.espece.Ancrages;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Ce qu'il tient dans la gueule, enfin visible.
 *
 * <h2>Ce qui manquait</h2>
 *
 * <p>Le compagnon savait deja ramasser un objet, le porter et venir le deposer
 * aux pieds de son maitre — {@code RapporterGoal} le fait depuis longtemps. Mais
 * rien ne le <b>dessinait</b> : la bete traversait la cour la gueule vide en
 * transportant une balle invisible. Tout le geste etait la sauf la seule chose
 * qu'on regarde.
 *
 * <h2>Pourquoi une position par espece</h2>
 *
 * <p>Une balle dans la gueule d'un dragonnet et la meme balle dans celle d'une
 * mouette ne sont pas au meme endroit, ni au meme angle, ni a la meme taille.
 * Il n'existe aucun reglage qui marche partout — c'est le defaut de toutes les
 * animations universelles, et il se voit tout de suite.
 *
 * <p>On lit donc {@link Ancrages}, reglee espece par espece a l'oeil, en jeu.
 * Une espece qu'on n'a pas encore reglee <b>n'affiche rien</b> : un trou se
 * remarque et se corrige, une approximation se garde des mois.
 *
 * <h2>Les axes sont ceux de l'animateur</h2>
 *
 * <p>Le decalage se lit droite, haut, avant — comme dans Blockbench, et en
 * unites de modele. GeckoLib dessine le modele retourne sur X et Y ; les signes
 * sont inverses ici une fois pour toutes, pour que les chiffres du fichier
 * gardent le sens qu'ils ont sous les yeux de celui qui les regle.
 */
public class CalqueObjet extends BlockAndItemGeoLayer<CompagnonEntity> {

	/** Seize unites de modele font un bloc. */
	private static final float UNITES_PAR_BLOC = 16.0F;

	public CalqueObjet(GeoRenderer<CompagnonEntity> rendu) {
		super(rendu);
	}

	/**
	 * L'objet a dessiner sur cet os-la, ou rien.
	 *
	 * <p>Appelee pour chaque os de la bete a chaque image : elle doit rendre
	 * {@code EMPTY} le plus vite possible. La comparaison de nom passe donc
	 * apres le test de la gueule vide, qui ecarte le cas courant d'un coup.
	 */
	@Override
	protected ItemStack getStackForBone(GeoBone os, CompagnonEntity compagnon) {
		ItemStack porte = compagnon.porte();
		if (porte.isEmpty()) {
			return ItemStack.EMPTY;
		}
		Ancrage ancrage = Ancrages.de(compagnon.espece(), nomDe(porte));
		if (!ancrage.regle() || !ancrage.os().equals(os.getName())) {
			return ItemStack.EMPTY;
		}
		return porte;
	}

	/**
	 * Pose l'objet a l'endroit regle, puis laisse GeckoLib le dessiner.
	 *
	 * <p>La matrice est deja placee sur l'os et suit toutes ses animations : la
	 * balle bouge avec la machoire sans qu'on ait un seul calcul a faire. On
	 * n'ajoute que l'ecart entre le pivot de l'os et l'endroit ou la chose doit
	 * vraiment se trouver.
	 */
	@Override
	protected void renderStackForBone(PoseStack pile, GeoBone os, ItemStack objet,
			CompagnonEntity compagnon, MultiBufferSource tampons, float partiel,
			int lumiere, int recouvrement) {

		Ancrage ancrage = Ancrages.de(compagnon.espece(), nomDe(objet));

		pile.pushPose();
		pile.translate(
				-ancrage.x() / UNITES_PAR_BLOC,
				-ancrage.y() / UNITES_PAR_BLOC,
				-ancrage.z() / UNITES_PAR_BLOC);
		// L'EDITEUR A BESOIN DE CETTE MATRICE, ET DE CELLE-CI PRECISEMENT.
		//
		// Prise ici, elle porte la vue, la pose de la bete et l'os d'accroche,
		// mais PAS encore la rotation ni la taille de l'ancrage. Sa translation
		// est donc la position de l'objet a l'ecran, et ses colonnes sont les
		// axes de l'OS — ceux que les fleches doivent suivre.
		//
		// Aucune projection a refaire de notre cote, donc aucune occasion de se
		// tromper : on lit ce que le jeu vient de calculer.
		if (Mannequin.actif()) {
			Mannequin.noterLaMatrice(pile.last().pose());
		}
		if (ancrage.tangage() != 0.0F) {
			pile.mulPose(Axis.XP.rotationDegrees(ancrage.tangage()));
		}
		if (ancrage.lacet() != 0.0F) {
			pile.mulPose(Axis.YP.rotationDegrees(ancrage.lacet()));
		}
		if (ancrage.roulis() != 0.0F) {
			pile.mulPose(Axis.ZP.rotationDegrees(ancrage.roulis()));
		}
		if (ancrage.echelle() != 1.0F) {
			pile.scale(ancrage.echelle(), ancrage.echelle(), ancrage.echelle());
		}
		super.renderStackForBone(pile, os, objet, compagnon, tampons,
				partiel, lumiere, recouvrement);
		pile.popPose();
	}

	/**
	 * Le nom sous lequel cet objet est range dans les ancrages.
	 *
	 * <p>Son identifiant de registre : {@code compagnon:balle}. Une espece peut
	 * ainsi tenir sa balle autrement que son os — c'est le seul moyen que les
	 * deux tombent juste, puisqu'ils n'ont ni la meme forme ni le meme sens.
	 */
	public static String nomDe(ItemStack pile) {
		return net.minecraft.core.registries.BuiltInRegistries.ITEM
			.getKey(pile.getItem()).toString();
	}
}
