package fr.lhdp.compagnon.client;

import fr.lhdp.compagnon.Compagnon;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Qui est en train de caresser, et comment on le montre.
 *
 * <p>C'est le morceau le plus risque du mod : le seul qui touche a la facon dont
 * Minecraft dessine le joueur. Il est donc <b>isole ici</b>, et il porte un
 * interrupteur : si quoi que ce soit se passe mal, il s'eteint tout seul et le
 * reste du mod continue de fonctionner.
 *
 * <h2>Le geste</h2>
 *
 * <p>Un bras fige a un angle ne ressemble a rien. Le geste se joue donc en trois
 * temps, comme un vrai :
 *
 * <ol>
 *   <li><b>il tend le bras</b> vers le compagnon, en accelerant puis
 *       ralentissant — un mouvement humain ne demarre pas a pleine vitesse ;</li>
 *   <li><b>il caresse</b> : la main va et vient le long du dos, avec un leger
 *       roulis du poignet, et le buste suit un peu ;</li>
 *   <li><b>il repose le bras</b>, de la meme facon adoucie.</li>
 * </ol>
 *
 * <p>La hauteur visee vient de la <b>taille reelle du compagnon</b> : on ne
 * caresse pas un chaton et un grand dragon au meme endroit.
 */
public final class Caresses {

	/** Hauteur de l'epaule du joueur, en blocs. Valeur inventee. */
	private static final float EPAULE = 1.35F;

	/** Ou l'on pose la main sur lui : une part de sa hauteur. Valeur inventee. */
	private static final float PART_DU_DOS = 0.75F;

	/** Part du geste passee a tendre le bras, puis a le reposer. */
	private static final float TEMPS_APPROCHE = 0.22F;
	private static final float TEMPS_RETOUR = 0.18F;

	/** Nombre d'allers-retours de la main pendant la caresse. Valeur inventee. */
	private static final float ALLERS_RETOURS = 2.5F;

	/** Amplitude du va-et-vient, en radians. Valeur inventee. */
	private static final float AMPLITUDE = 0.20F;

	/** Roulis du poignet, qui accompagne le va-et-vient. Valeur inventee. */
	private static final float ROULIS = 0.10F;

	/** Ecartement du bras vers l'exterieur, pour ne pas traverser le buste. */
	private static final float ECART = 0.14F;

	/** Bornes de l'angle du bras, pour qu'il ne parte jamais a l'envers. */
	private static final float ANGLE_MIN = -2.4F;
	private static final float ANGLE_MAX = -0.2F;

	/**
	 * Passe a faux au premier probleme. Le cahier des charges est clair : si la
	 * caresse coince, elle ne doit rien bloquer d'autre.
	 */
	private static boolean active = true;

	/** Numero d'entite du joueur, puis ce qu'il caresse et pour combien de temps. */
	private static final Map<Integer, Caresse> enCours = new HashMap<>();

	private record Caresse(int compagnon, int ticksRestants, int ticksTotal) {
		/** De zero au debut du geste a un a la fin. */
		float progression(float partiel) {
			if (this.ticksTotal <= 0) {
				return 1.0F;
			}
			float restant = Math.max(0.0F, this.ticksRestants - partiel);
			return Math.max(0.0F, Math.min(1.0F, 1.0F - restant / this.ticksTotal));
		}
	}

	private Caresses() {
	}

	// --- Ce que le serveur nous dit ---------------------------------------------

	public static void commencer(int joueur, int compagnon, int ticks) {
		if (!active) {
			return;
		}
		enCours.put(joueur, new Caresse(compagnon, ticks, ticks));

		// ON NE TOUCHE PAS A LA CAMERA.
		//
		// Elle passait en troisieme personne le temps du geste, pour qu'on se
		// voie caresser. C'est joli une fois et penible les cent suivantes : on
		// caresse souvent, et la vue sautait a chaque fois. Le joueur choisit sa
		// camera avec F5, et ce choix est le sien.
	}

	/** A appeler a chaque tick du client. */
	public static void tick() {
		if (enCours.isEmpty()) {
			return;
		}

		enCours.entrySet().removeIf(entree -> {
			Caresse caresse = entree.getValue();
			if (caresse.ticksRestants() > 1) {
				entree.setValue(new Caresse(caresse.compagnon(),
						caresse.ticksRestants() - 1, caresse.ticksTotal()));
				return false;
			}
			return true;
		});
	}

	/** Le monde a change ou on s'est deconnecte : on repart de zero. */
	public static void oublier() {
		enCours.clear();
	}

	// --- Le geste ---------------------------------------------------------------

	/**
	 * Anime le bras, la manche et la tete du joueur pendant qu'il caresse.
	 *
	 * @param partiel l'avancee dans le tick en cours, pour que le geste soit
	 *                fluide au-dela de vingt images par seconde
	 * @return vrai si le geste a ete pose
	 */
	public static boolean animer(Player joueur, ModelPart bras, ModelPart tete, float partiel) {
		if (!active || enCours.isEmpty()) {
			return false;
		}
		try {
			Caresse caresse = enCours.get(joueur.getId());
			if (caresse == null) {
				return false;
			}

			float t = caresse.progression(partiel);
			float tension = tension(t);
			if (tension <= 0.001F) {
				return false;
			}

			// La main va et vient le long de son dos, mais seulement une fois le
			// bras tendu : sinon elle gigoterait pendant l'approche.
			float vaEtVient = (float) Math.sin(t * Math.PI * 2.0 * ALLERS_RETOURS);

			float vise = angleVise(joueur, caresse.compagnon());
			float angle = vise * tension + AMPLITUDE * vaEtVient * tension;

			bras.xRot = Math.max(ANGLE_MIN, Math.min(ANGLE_MAX, angle));
			bras.yRot = 0.08F * tension;
			bras.zRot = ECART * tension + ROULIS * vaEtVient * tension;

			// Il regarde ce qu'il caresse, sinon le geste a l'air distrait.
			if (tete != null) {
				tete.xRot += 0.35F * tension;
			}
			return true;

		} catch (Exception echec) {
			// On coupe pour de bon plutot que de reessayer a chaque image.
			active = false;
			Compagnon.LOG.warn("Caresse desactivee : {}", echec.toString());
			return false;
		}
	}

	/**
	 * Combien le bras est tendu, de zero a un.
	 *
	 * <p>Il monte pendant l'approche, tient pendant la caresse, redescend a la
	 * fin. Chaque transition est adoucie : un bras qui part et s'arrete net ne
	 * ressemble a rien.
	 */
	private static float tension(float t) {
		if (t < TEMPS_APPROCHE) {
			return adoucir(t / TEMPS_APPROCHE);
		}
		if (t > 1.0F - TEMPS_RETOUR) {
			return adoucir((1.0F - t) / TEMPS_RETOUR);
		}
		return 1.0F;
	}

	/** Accelere puis ralentit, au lieu d'aller a vitesse constante. */
	private static float adoucir(float x) {
		float borne = Math.max(0.0F, Math.min(1.0F, x));
		return borne * borne * (3.0F - 2.0F * borne);
	}

	/**
	 * L'angle du bras une fois tendu, deduit de la <b>taille du compagnon</b>.
	 *
	 * <p>Plus il est bas, plus le bras descend ; plus il est haut, plus il part
	 * vers l'avant. C'est exactement ce que demande le cahier des charges : la
	 * main doit tomber au bon endroit sur un tout petit comme sur un grand.
	 */
	private static float angleVise(Player joueur, int numeroDuCompagnon) {
		Entity cible = joueur.level().getEntity(numeroDuCompagnon);
		float hauteurDos = cible == null ? 0.5F : cible.getBbHeight() * PART_DU_DOS;

		// Ecart vertical entre l'epaule et le point vise, ramene a une part de la
		// hauteur d'epaule.
		float descente = Math.max(0.0F, Math.min(1.0F, (EPAULE - hauteurDos) / EPAULE));

		// Bras a l'horizontale quand la cible est a hauteur d'epaule, bras presque
		// vertical quand elle est au sol.
		return -1.45F + descente * 0.85F;
	}

	/** La position visee, gardee pour un futur reglage fin du geste. */
	static Vec3 pointVise(Player joueur, int numeroDuCompagnon) {
		Entity cible = joueur.level().getEntity(numeroDuCompagnon);
		return cible == null ? joueur.position() : cible.position();
	}
}
