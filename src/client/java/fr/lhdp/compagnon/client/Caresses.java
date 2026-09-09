package fr.lhdp.compagnon.client;

import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.reseau.PaquetResultatCaresse;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
	private static final float[] TEMPS_RYTHME = {0.34F, 0.55F, 0.76F};
	private static final float FENETRE_RYTHME = 0.075F;
	private static int scoreAffiche = -1;
	private static int ticksScoreAffiche;
	private static boolean gaucheAvant;
	private static boolean droiteAvant;

	private static final class Caresse {
		private final int compagnon;
		private final int ticksTotal;
		private int ticksRestants;
		private int etape;
		private int score;
		private int reussites;

		private Caresse(int compagnon, int ticksRestants, int ticksTotal) {
			this.compagnon = compagnon;
			this.ticksRestants = ticksRestants;
			this.ticksTotal = ticksTotal;
		}

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
		if (ticksScoreAffiche > 0) {
			ticksScoreAffiche--;
		}
		if (enCours.isEmpty()) {
			return;
		}
		jouerLeRythmeLocal();
		immobiliserJoueurLocal();

		enCours.entrySet().removeIf(entree -> {
			Caresse caresse = entree.getValue();
			if (caresse.ticksRestants > 1) {
				caresse.ticksRestants--;
				return false;
			}
			Minecraft client = Minecraft.getInstance();
			if (client.player != null && entree.getKey() == client.player.getId()) {
				ClientPlayNetworking.send(new PaquetResultatCaresse(
						caresse.compagnon, caresse.score));
				scoreAffiche = caresse.score;
				ticksScoreAffiche = 20 * 2;
			}
			return true;
		});
	}

	/** Trois appuis alternés, pris avant d'annuler le déplacement du joueur. */
	private static void jouerLeRythmeLocal() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		Caresse caresse = enCours.get(client.player.getId());
		if (caresse == null || caresse.etape >= TEMPS_RYTHME.length) {
			return;
		}
		boolean gauche = client.options.keyLeft.isDown();
		boolean droite = client.options.keyRight.isDown();
		boolean nouvelAppui = caresse.etape % 2 == 0
				? gauche && !gaucheAvant : droite && !droiteAvant;
		float t = caresse.progression(0.0F);
		float cible = TEMPS_RYTHME[caresse.etape];
		if (nouvelAppui && Math.abs(t - cible) <= FENETRE_RYTHME) {
			caresse.reussites |= 1 << caresse.etape;
			caresse.score++;
			caresse.etape++;
		} else if (t > cible + FENETRE_RYTHME) {
			caresse.etape++;
		}
		gaucheAvant = gauche;
		droiteAvant = droite;
	}

	/** Pendant le geste, les touches et l'elan ne peuvent pas lancer une seconde action. */
	private static void immobiliserJoueurLocal() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || !enCours.containsKey(client.player.getId())) {
			return;
		}
		client.player.input.leftImpulse = 0.0F;
		client.player.input.forwardImpulse = 0.0F;
		client.player.input.up = false;
		client.player.input.down = false;
		client.player.input.left = false;
		client.player.input.right = false;
		client.player.input.jumping = false;
		Vec3 mouvement = client.player.getDeltaMovement();
		client.player.setDeltaMovement(0.0D, Math.min(0.0D, mouvement.y), 0.0D);
	}

	/** Le monde a change ou on s'est deconnecte : on repart de zero. */
	public static void oublier() {
		enCours.clear();
		scoreAffiche = -1;
		ticksScoreAffiche = 0;
		gaucheAvant = false;
		droiteAvant = false;
	}

	/** Petit bandeau au-dessus de la barre d'objets : jamais au milieu de l'écran. */
	public static void dessiner(GuiGraphics g, float partiel) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.options.hideGui || client.screen != null) {
			return;
		}
		Caresse caresse = enCours.get(client.player.getId());
		if (caresse == null) {
			if (ticksScoreAffiche > 0 && scoreAffiche >= 0) {
				String texte = net.minecraft.network.chat.Component.translatable(
						"caresse.compagnon.score", scoreAffiche, 3).getString();
				g.drawCenteredString(client.font, texte, g.guiWidth() / 2,
						g.guiHeight() - 66, scoreAffiche == 3 ? 0xFFFFD76A : 0xFFF1E4C4);
			}
			return;
		}

		int largeur = 142;
		int x = (g.guiWidth() - largeur) / 2;
		int y = g.guiHeight() - 76;
		g.fill(x, y, x + largeur, y + 26, 0xCC2B2118);
		g.fill(x + 1, y + 1, x + largeur - 1, y + 25, 0xE6EBD2A5);
		String titre = net.minecraft.network.chat.Component.translatable(
				"caresse.compagnon.rythme").getString();
		g.drawCenteredString(client.font, titre, x + largeur / 2, y + 4, 0xFF3A2A18);

		for (int i = 0; i < TEMPS_RYTHME.length; i++) {
			int centre = x + 36 + i * 35;
			boolean fait = i < caresse.etape;
			boolean reussi = (caresse.reussites & (1 << i)) != 0;
			int couleur = reussi ? 0xFF4C9A55 : fait ? 0xFF8B7660 : 0xFFD39B32;
			g.fill(centre - 9, y + 15, centre + 9, y + 23, couleur);
			String touche = i % 2 == 0
					? client.options.keyLeft.getTranslatedKeyMessage().getString()
					: client.options.keyRight.getTranslatedKeyMessage().getString();
			g.drawCenteredString(client.font, touche, centre, y + 15, 0xFFFFFFFF);
		}
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

			float vise = angleVise(joueur, caresse.compagnon);
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
