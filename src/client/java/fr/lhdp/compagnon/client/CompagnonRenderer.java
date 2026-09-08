package fr.lhdp.compagnon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.entite.CompagnonEntity;
import fr.lhdp.compagnon.espece.Espece;
import fr.lhdp.compagnon.espece.Especes;
import fr.lhdp.compagnon.espece.Partie;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.component.CustomModelData;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import org.joml.Vector3d;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.HashMap;
import java.util.Map;
import software.bernie.geckolib.util.RenderUtil;

/**
 * Le rendu du compagnon. Tout ce qui varie vient de la fiche d'espece.
 *
 * <p>Il dessine aussi la <b>bulle</b> au-dessus de sa tete : l'objet qu'il
 * reclame quand il a faim. Rien d'ecrit — un compagnon ne parle pas.
 */
public class CompagnonRenderer extends GeoEntityRenderer<CompagnonEntity> {

	/** Hauteur de la bulle au-dessus de sa tete, en blocs. Valeur inventee. */
	private static final float HAUTEUR_BULLE = 0.75F;

	/** Au-dela, on ne la dessine plus : elle serait illisible. Valeur inventee. */
	private static final double PORTEE_LISIBLE = 12.0D;

	/**
	 * Les nuages, du plus plat au plus haut, et ce qu'ils peuvent contenir.
	 *
	 * <p>Les nombres ne sont pas estimes : ils sont <b>mesures</b> sur les images
	 * elles-memes, par recherche du plus grand rectangle plein a l'interieur du
	 * contour (voir {@code outils/Interieur.java}). Les bosses du pourtour n'ont
	 * pas la meme epaisseur partout, et une marge devinee ferait deborder une
	 * phrase sur le contour.
	 *
	 * @param image     le fichier
	 * @param largeur   taille de l'image, en pixels
	 * @param hauteur   taille de l'image, en pixels
	 * @param creuxX    ou commence le creux utilisable
	 * @param creuxY    ou commence le creux utilisable
	 * @param creuxL    taille du creux
	 * @param creuxH    taille du creux
	 */
	private record Nuage(ResourceLocation image, int largeur, int hauteur,
			int creuxX, int creuxY, int creuxL, int creuxH) {
	}

	private static final Nuage[] NUAGES = {
			new Nuage(Compagnon.id("textures/gui/nuage_1.png"), 78, 25, 6, 6, 63, 12),
			new Nuage(Compagnon.id("textures/gui/nuage_2.png"), 78, 34, 6, 6, 67, 20),
			new Nuage(Compagnon.id("textures/gui/nuage_3.png"), 78, 38, 6, 6, 66, 24),
	};

	/** Les trois petites bulles qui montent vers le nuage, de la plus petite. */
	private static final ResourceLocation[] PETITES = {
			Compagnon.id("textures/gui/bulle_1.png"),
			Compagnon.id("textures/gui/bulle_2.png"),
			Compagnon.id("textures/gui/bulle_3.png"),
	};

	private static final int[] TAILLES_PETITES = { 9, 14, 28 };

	/**
	 * Combien d'unites d'affichage vaut un pixel de nuage.
	 *
	 * <p>A 1,8 le nuage fait trois blocs et demi de large. On l'a essaye a 2,2 :
	 * lisible de l'autre bout de la cour, mais on ne voyait plus que ca. La bulle
	 * doit se remarquer, pas ecraser la bete qui la porte.
	 *
	 * <p>C'est bien le NUAGE qu'on regle ici, pas l'objet dedans : celui-ci occupe
	 * une part fixe du creux, voir REMPLISSAGE_OBJET.
	 */
	private static final float ECHELLE_NUAGE = 1.8F;

	/** Les petites bulles sont dessinees plus fin que le nuage, pour rester discretes. */
	private static final float ECHELLE_PETITE = 0.8F;

	/** Ecart vertical entre deux petites bulles, en unites. */
	private static final float ECART_PETITES = 3.0F;

	/** Quelle part du creux du nuage l'objet reclame occupe. Valeur inventee. */
	private static final float REMPLISSAGE_OBJET = 0.95F;

	/**
	 * Taille de ce qu'il porte dans la gueule.
	 *
	 * <p>Reglage a faire a l'oeil, comme les trois qui suivent : la bonne valeur
	 * depend du modele, et aucune mesure ne remplace un coup d'oeil en jeu.
	 */
	private static final float TAILLE_GUEULE = 0.5F;

	/** L'os retenu au dernier rendu, pour lire sa position dans le monde. */
	private GeoBone bouche;

	/**
	 * De combien l'objet avance devant la bouche, en blocs.
	 *
	 * <p>La position de l'os tombe au milieu du museau : a 0,30 l'objet etait bien
	 * a la bonne hauteur, mais a moitie rentre dans le nez. Il ressort maintenant
	 * assez pour se voir en entier — il TIENT la chose, il ne l'a pas avalee.
	 *
	 * <p>Le sens, lui, ne se devine plus : on avance dans la direction ou la bete
	 * REGARDE, calculee depuis son orientation. C'est la meme formule que celle
	 * des boites de collision, qui marche depuis le debut.
	 */
	private static final double GUEULE_AVANT = 0.48D;

	/** Vers le bas, pour que l'objet repose sur la machoire et non dedans. */
	private static final float GUEULE_BAS = 0.04F;

	/** Il le tient en travers, comme un chien tient un baton. */
	private static final float GUEULE_ANGLE = 90.0F;

	/**
	 * A PLAT, et non debout.
	 *
	 * <p>La plupart des objets de Minecraft sont des images plates, epaisses
	 * d'un seizieme de bloc. Debout et en travers, on les voyait <b>par la
	 * tranche</b> : un trait. Un steak entier ressemblait a une baguette.
	 *
	 * <p>Couche, on voit toute l'image. C'est aussi ce que fait un animal qui
	 * tient quelque chose : il le pose sur sa machoire, il ne le brandit pas.
	 *
	 * <p>La rotation se fait autour de l'axe de son regard — donc apres la
	 * rotation d'orientation, jamais avant. Pour l'incliner au lieu de le
	 * coucher tout a fait, baissez cette valeur : 60 le laisse penche.
	 */
	private static final float GUEULE_PLAT = 90.0F;

	/**
	 * L'os du modele ou poser ce qu'il porte.
	 *
	 * <p>Trouve une fois par espece, au premier rendu. Voir {@link Bouches}.
	 */
	private String osDeLaBouche = "";

	public CompagnonRenderer(EntityRendererProvider.Context contexte) {
		super(contexte, new CompagnonModel());
		// CE QUI BRILLE DANS LE NOIR.
		//
		// Le calque ne dessine rien tant que la variante n en declare pas un : une
		// espece sans calque ne paie que la lecture d un booleen deja en cache.
		addRenderLayer(new CalqueLumineux(this));
		// Ce qu'il tient dans la gueule. Le comportement existait depuis
		// longtemps ; rien ne le dessinait.
		addRenderLayer(new CalqueObjet(this));
	}

	/**
	 * GeckoLib nous remet ici le modele deja assemble : c'est le seul endroit ou
	 * l'on peut chercher l'os de la bouche, et il ne coute rien puisque le
	 * resultat est retenu par espece.
	 */
	@Override
	public void actuallyRender(PoseStack pile, CompagnonEntity compagnon, BakedGeoModel modele,
			RenderType type, MultiBufferSource tampons, VertexConsumer sommets,
			boolean reRendu, float partiel, int lumiere, int recouvrement, int couleur) {

		this.osDeLaBouche = Bouches.de(compagnon.espece(), modele);
		super.actuallyRender(pile, compagnon, modele, type, tampons, sommets,
				reRendu, partiel, lumiere, recouvrement, couleur);
	}

	/**
	 * Ce qu'il porte, accroche a l'OS de sa bouche.
	 *
	 * <p>C'est tout l'interet : l'objet suit alors l'animation. Quand il baisse la
	 * tete ou ouvre la gueule, l'objet bouge avec, sans un calcul de plus.
	 *
	 * <p>La version d'avant le posait a un decalage tire de la boite de collision
	 * de la tete. Ca flottait devant lui, ca ne suivait rien, et il aurait fallu
	 * regler ce decalage a la main pour chaque nouvelle creature.
	 */
	@Override
	public void renderRecursively(PoseStack pile, CompagnonEntity compagnon, GeoBone os,
			RenderType type, MultiBufferSource tampons, VertexConsumer sommets,
			boolean reRendu, float partiel, int lumiere, int recouvrement, int couleur) {

		// On ne DESSINE rien ici : on retient seulement l'os, car c'est le seul
		// moment ou GeckoLib met a jour sa position dans le monde.
		//
		// La premiere version dessinait l'objet directement dans le repere de l'os.
		// Il se retrouvait aux pieds de la bete. Raisonner sur ce repere — sens des
		// axes, retournements, transformations heritees du parent — m'a fait me
		// tromper trois fois de suite. On prend donc la seule chose qui ne se
		// discute pas : la POSITION MONDIALE que GeckoLib calcule lui-meme.
		// La condition sur ce qu'il porte vient EN PREMIER, et ce n'est pas un
		// detail : ce modele a quatre-vingt-onze os, et cette methode est appelee
		// une fois par os, par compagnon et par image. Comparer des chaines pour
		// une bete qui ne porte rien, c'etait des centaines de milliers de
		// comparaisons par seconde pour rien.
		if (!reRendu && !compagnon.porte().isEmpty()
				&& this.osDeLaBouche.equals(os.getName())) {
			this.bouche = os;
		}

		super.renderRecursively(pile, compagnon, os, type, tampons, sommets,
				reRendu, partiel, lumiere, recouvrement, couleur);
	}

	@Override
	public void render(CompagnonEntity compagnon, float lacet, float partiel,
			PoseStack pile, MultiBufferSource tampons, int lumiere) {

		super.render(compagnon, lacet, partiel, pile, tampons, lumiere);
		// APRES le modele : la position de l'os n'est connue qu'une fois celui-ci
		// dessine.
		dansLaGueule(compagnon, partiel, pile, tampons, lumiere);
		bulle(compagnon, pile, tampons, lumiere);
	}

	/**
	 * Ce qu'il porte, a l'endroit exact de sa bouche.
	 *
	 * <h2>Comment on trouve cet endroit</h2>
	 *
	 * <p>GeckoLib calcule, a chaque image, la position de chaque os <b>dans le
	 * monde</b>. On lit celle de l'os de la bouche et on s'y place : c'est un point
	 * en coordonnees de monde, il n'y a donc aucun repere a interpreter, aucun
	 * retournement a deviner, aucun signe a supposer.
	 *
	 * <p>C'est le troisieme essai. Les deux premiers raisonnaient sur le repere
	 * local de l'os, et l'objet s'est retrouve une fois dans le crane et une fois
	 * entre les pattes. La position mondiale, elle, ne se discute pas.
	 *
	 * <p>Consequence assumee : l'objet suit la bouche quand elle BOUGE, mais ne
	 * tourne pas avec elle quand elle s'ouvre. Il est oriente selon le corps. C'est
	 * un tres bon echange contre trois essais rates.
	 */
	private void dansLaGueule(CompagnonEntity compagnon, float partiel, PoseStack pile,
			MultiBufferSource tampons, int lumiere) {

		ItemStack porte = compagnon.porte();
		if (porte.isEmpty() || this.bouche == null) {
			return;
		}
		Vector3d ou = this.bouche.getWorldPosition();

		// L'orientation du corps, interpolee : sinon l'objet saute d'un tick a
		// l'autre pendant qu'il tourne.
		float orientation = Mth.rotLerp(partiel, compagnon.yBodyRotO, compagnon.yBodyRot);
		double angle = Math.toRadians(orientation);

		pile.pushPose();
		// Du centre de la bete jusqu'a sa bouche.
		pile.translate(ou.x - compagnon.getX(), ou.y - compagnon.getY(), ou.z - compagnon.getZ());
		// Puis un peu vers l'avant, pour sortir du museau plutot que rester dedans.
		pile.translate(-Math.sin(angle) * GUEULE_AVANT, -GUEULE_BAS, Math.cos(angle) * GUEULE_AVANT);

		pile.mulPose(Axis.YP.rotationDegrees(-orientation + GUEULE_ANGLE));
		// Puis on le couche. Cet ordre est le bon : l'axe X est desormais celui
		// de son regard, et tourner autour fait basculer l'objet a plat. Faire
		// l'inverse le ferait pivoter dans le vide, sans rapport avec sa tete.
		pile.mulPose(Axis.XP.rotationDegrees(GUEULE_PLAT));
		pile.scale(TAILLE_GUEULE, TAILLE_GUEULE, TAILLE_GUEULE);

		// GROUND et non THIRD_PERSON_RIGHT_HAND : la pose « en main » du jeu
		// incline les objets de 55 degres pour qu'ils tiennent dans un poing
		// humain. Un dragon n'a pas de poing, et cette inclinaison se battait
		// avec la notre. La pose « au sol » n'ajoute aucune rotation : l'objet
		// se pose exactement ou on le met.
		Minecraft.getInstance().getItemRenderer().renderStatic(porte,
				ItemDisplayContext.GROUND, lumiere,
				OverlayTexture.NO_OVERLAY, pile, tampons, compagnon.level(), 0);

		pile.popPose();
	}

	/**
	 * La bulle : ce dont il a envie, et rien d'autre.
	 *
	 * <p><b>Il n'y a plus de texte.</b> On a essaye d'y ecrire ses pensees, et
	 * c'etait une erreur de conception, pas seulement d'affichage : un compagnon ne
	 * parle pas, et on n'est pas cense savoir ce qu'il a dans la tete. Ce qu'il
	 * pense va desormais dans son livre, sous forme de souvenirs — c'est la qu'on
	 * lit son histoire, pas au-dessus de sa tete.
	 *
	 * <p>Reste ce qui se comprend sans un mot : l'objet qu'il reclame.
	 */
	private void bulle(CompagnonEntity compagnon, PoseStack pile,
			MultiBufferSource tampons, int lumiere) {

		if (this.entityRenderDispatcher.distanceToSqr(compagnon) > PORTEE_LISIBLE * PORTEE_LISIBLE) {
			return;
		}
		ItemStack reclame = objetReclame(compagnon);
		if (reclame.isEmpty()) {
			return;
		}
		bulleObjet(compagnon, reclame, pile, tampons);
	}

	/**
	 * L'objet dont il a envie, <b>dans la bulle</b>.
	 *
	 * <p>Il flottait avant tout seul au-dessus de sa tete. C'etait lisible, mais
	 * ca ne racontait rien : un objet posé en l'air n'appartient a personne. Dans
	 * le nuage, c'est une pensee — il y pense, donc il le demande.
	 *
	 * <p>Toujours le nuage le plus haut : un objet est carre, et le nuage plat
	 * l'ecrasait a la hauteur de son creux — c'est ce qui donnait la pomme minuscule
	 * au milieu d'un grand vide.
	 */
	private void bulleObjet(CompagnonEntity compagnon, ItemStack objet,
			PoseStack pile, MultiBufferSource tampons) {

		Nuage nuage = NUAGES[NUAGES.length - 1];

		pile.pushPose();
		pile.translate(0.0F, compagnon.getBbHeight() + HAUTEUR_BULLE, 0.0F);
		pile.mulPose(this.entityRenderDispatcher.cameraOrientation());
		pile.scale(-0.025F, -0.025F, 0.025F);

		float trainee = petitesBulles(pile, tampons);
		float largeurNuage = nuage.largeur() * ECHELLE_NUAGE;
		float hauteurNuage = nuage.hauteur() * ECHELLE_NUAGE;
		float basDuNuage = -trainee;

		image(pile, tampons, nuage.image(),
				-largeurNuage / 2.0F, basDuNuage - hauteurNuage,
				largeurNuage, hauteurNuage, 0.0F);

		// Le milieu du creux, et une taille qui tient dedans avec un peu de marge.
		float centre = basDuNuage - hauteurNuage
				+ (nuage.creuxY() + nuage.creuxH() / 2.0F) * ECHELLE_NUAGE;
		// ERREUR CORRIGEE, et elle expliquait a elle seule l'objet invisible : on
		// est DEJA dans le repere de la bulle, ou tout est exprime en « unites de
		// nuage ». Un objet dessine a l'echelle un y occupe une unite, pas un bloc.
		// L'ancien calcul divisait encore par quarante et rendait une pomme de la
		// taille d'un grain de poussiere.
		float taille = nuage.creuxH() * ECHELLE_NUAGE * REMPLISSAGE_OBJET;

		pile.pushPose();
		pile.translate(0.0F, centre, 0.0F);
		// Les deux signes negatifs annulent ceux du repere : sans eux l'objet
		// serait dessine a l'envers et en miroir.
		pile.scale(-taille, -taille, taille);

		Minecraft.getInstance().getItemRenderer().renderStatic(objet,
				ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
				OverlayTexture.NO_OVERLAY, pile, tampons, compagnon.level(), 0);

		pile.popPose();
		pile.popPose();
	}

	/**
	 * Ce qu'il reclame, lu depuis la donnee synchronisee.
	 *
	 * <p>« compagnon:aliment|12 » : l'objet, puis le numero de modele qui dit
	 * laquelle des soixante-dix varietes dessiner. Voir {@code Objets#envie}.
	 *
	 * <p>Un identifiant que ce client ne connait pas — un autre mod absent — rend
	 * une pile vide, et il n'y a simplement pas de bulle.
	 */
	/**
	 * Ce qu'il reclame, RETENU d'une image a l'autre.
	 *
	 * <p>Sans ce cache, on refaisait tout le travail soixante fois par seconde et
	 * par compagnon visible : analyser la chaine, chercher l'objet dans le
	 * registre, fabriquer une pile, y poser une donnee. A cinquante compagnons
	 * affames dans une cour, cela faisait trois mille piles jetees par seconde —
	 * du travail pour le ramasse-miettes, et des a-coups a l'image.
	 *
	 * <p>L'envie ne change qu'une fois par minute au plus. Une petite table suffit :
	 * il n'y a qu'une poignee d'identifiants possibles.
	 */
	private static final Map<String, ItemStack> reclamesRetenus = new HashMap<>();

	private static ItemStack objetReclame(CompagnonEntity compagnon) {
		String identifiant = compagnon.envie();
		if (identifiant.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack deja = reclamesRetenus.get(identifiant);
		if (deja != null) {
			return deja;
		}
		ItemStack fabriquee = fabriquerLeReclame(identifiant);
		reclamesRetenus.put(identifiant, fabriquee);
		return fabriquee;
	}

	private static ItemStack fabriquerLeReclame(String identifiant) {

		int barre = identifiant.indexOf('|');
		int modele = 0;
		if (barre >= 0) {
			try {
				modele = Integer.parseInt(identifiant.substring(barre + 1));
			} catch (NumberFormatException maisNon) {
				// Un serveur plus ancien, ou plus recent : on montrera l'objet nu.
			}
			identifiant = identifiant.substring(0, barre);
		}

		ResourceLocation cle = ResourceLocation.tryParse(identifiant);
		if (cle == null) {
			return ItemStack.EMPTY;
		}
		Item objet = BuiltInRegistries.ITEM.getOptional(cle).orElse(null);
		if (objet == null) {
			return ItemStack.EMPTY;
		}
		ItemStack pile = new ItemStack(objet);
		if (modele > 0) {
			pile.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(modele));
		}
		return pile;
	}

	/**
	 * Les trois petites bulles qui montent de sa tete vers le nuage.
	 *
	 * <p>Elles partent en biais, comme dans une bande dessinee : trois ronds
	 * parfaitement alignes feraient une colonne, pas une pensee qui s'echappe.
	 *
	 * @return la hauteur occupee, pour savoir ou poser le nuage
	 */
	private float petitesBulles(PoseStack pile, MultiBufferSource tampons) {
		float haut = 0.0F;
		for (int i = 0; i < PETITES.length; i++) {
			float cote = TAILLES_PETITES[i] * ECHELLE_PETITE;
			// Chaque bulle est un peu plus decalee que la precedente.
			float biais = i * 2.5F;
			image(pile, tampons, PETITES[i], biais - cote / 2.0F, -haut - cote,
					cote, cote, 0.0F);
			haut += cote + ECART_PETITES;
		}
		return haut;
	}

	/**
	 * Un rectangle texture, face a la camera.
	 *
	 * <p>Sans culling : le repere a un x negatif, donc l'ordre des sommets est
	 * inverse par rapport a l'intuition, et une face serait supprimee une fois sur
	 * deux selon l'angle de vue.
	 *
	 * <p>Toujours en pleine lumiere : une pensee n'est pas un objet du monde, elle
	 * ne doit pas s'assombrir la nuit ou dans une cave.
	 */
	private static void image(PoseStack pile, MultiBufferSource tampons,
			ResourceLocation texture, float x, float y, float l, float h, float z) {

		// entityCutoutNoCull, et on n'y touche plus : c'est le seul type de rendu
		// dont on ait la preuve qu'il dessine correctement ce nuage. Un essai avec
		// le type « texte traversant » n'a plus rien affiche du tout et a laisse un
		// carre blanc dans le ciel. Le texte, lui, se regle autrement — sans
		// profondeur — et n'a pas besoin que le nuage change.
		VertexConsumer sommets = tampons.getBuffer(RenderType.entityCutoutNoCull(texture));
		PoseStack.Pose pose = pile.last();

		coin(sommets, pose, x, y + h, z, 0.0F, 1.0F);
		coin(sommets, pose, x + l, y + h, z, 1.0F, 1.0F);
		coin(sommets, pose, x + l, y, z, 1.0F, 0.0F);
		coin(sommets, pose, x, y, z, 0.0F, 0.0F);
	}

	private static void coin(VertexConsumer sommets, PoseStack.Pose pose,
			float x, float y, float z, float u, float v) {
		sommets.addVertex(pose, x, y, z)
				.setColor(0xFFFFFFFF)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(LightTexture.FULL_BRIGHT)
				.setNormal(pose, 0.0F, 0.0F, -1.0F);
	}


}
