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
import java.util.concurrent.ThreadLocalRandom;

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
	/** Une fenetre genereuse : le jeu accompagne la caresse, il ne la sanctionne pas. */
	private static final float FENETRE_RYTHME = 0.10F;
	private static final float FENETRE_RUBAN = 0.105F;
	private static final float FENETRE_ECHO = 0.11F;
	private static final int COULEUR_ENCRE = 0xFF3A2A38;
	private static final int COULEUR_OR = 0xFFE2B85E;
	private static final int COULEUR_REUSSITE = 0xFF6FA36C;
	private static final int COULEUR_RATE = 0xFF9C7F86;
	private static int scoreAffiche = -1;
	private static int ticksScoreAffiche;
	private static boolean gaucheAvant;
	private static boolean droiteAvant;
	private static int dernierDefi = -1;

	private enum Famille {
		RYTHME,
		RUBAN,
		ECHO
	}

	/**
	 * Trois gestes suffisent pour apprendre la regle ; le masque donne leur sens
	 * (bit a zero : gauche, bit a un : droite). Les trente variantes changent le
	 * rythme, la suite et le nom sans creer trente moteurs couteux.
	 */
	private record Defi(int numero, Famille famille, int directions,
			float premier, float deuxieme, float troisieme) {

		float temps(int etape) {
			return switch (etape) {
				case 0 -> this.premier;
				case 1 -> this.deuxieme;
				default -> this.troisieme;
			};
		}

		boolean droite(int etape) {
			return (this.directions & 1 << etape) != 0;
		}
	}

	private static final Defi[] DEFIS = {
			d(1, Famille.RYTHME, 0b010, 0.28F, 0.50F, 0.73F),
			d(2, Famille.RYTHME, 0b101, 0.30F, 0.53F, 0.76F),
			d(3, Famille.RYTHME, 0b100, 0.27F, 0.48F, 0.72F),
			d(4, Famille.RYTHME, 0b011, 0.31F, 0.52F, 0.75F),
			d(5, Famille.RYTHME, 0b001, 0.29F, 0.54F, 0.77F),
			d(6, Famille.RYTHME, 0b110, 0.26F, 0.49F, 0.74F),
			d(7, Famille.RYTHME, 0b000, 0.32F, 0.55F, 0.78F),
			d(8, Famille.RYTHME, 0b111, 0.28F, 0.51F, 0.71F),
			d(9, Famille.RYTHME, 0b101, 0.33F, 0.54F, 0.74F),
			d(10, Famille.RYTHME, 0b010, 0.25F, 0.47F, 0.70F),
			d(11, Famille.RUBAN, 0b010, 0.28F, 0.51F, 0.74F),
			d(12, Famille.RUBAN, 0b101, 0.30F, 0.53F, 0.76F),
			d(13, Famille.RUBAN, 0b001, 0.27F, 0.50F, 0.73F),
			d(14, Famille.RUBAN, 0b110, 0.31F, 0.54F, 0.77F),
			d(15, Famille.RUBAN, 0b100, 0.29F, 0.52F, 0.75F),
			d(16, Famille.RUBAN, 0b011, 0.26F, 0.49F, 0.72F),
			d(17, Famille.RUBAN, 0b000, 0.32F, 0.55F, 0.78F),
			d(18, Famille.RUBAN, 0b111, 0.28F, 0.52F, 0.76F),
			d(19, Famille.RUBAN, 0b101, 0.30F, 0.50F, 0.71F),
			d(20, Famille.RUBAN, 0b010, 0.27F, 0.54F, 0.77F),
			d(21, Famille.ECHO, 0b010, 0.43F, 0.62F, 0.81F),
			d(22, Famille.ECHO, 0b101, 0.42F, 0.61F, 0.80F),
			d(23, Famille.ECHO, 0b001, 0.44F, 0.63F, 0.82F),
			d(24, Famille.ECHO, 0b110, 0.41F, 0.60F, 0.79F),
			d(25, Famille.ECHO, 0b100, 0.45F, 0.64F, 0.83F),
			d(26, Famille.ECHO, 0b011, 0.42F, 0.62F, 0.82F),
			d(27, Famille.ECHO, 0b000, 0.43F, 0.63F, 0.83F),
			d(28, Famille.ECHO, 0b111, 0.41F, 0.61F, 0.81F),
			d(29, Famille.ECHO, 0b101, 0.44F, 0.62F, 0.80F),
			d(30, Famille.ECHO, 0b010, 0.42F, 0.64F, 0.84F)
	};

	private static Defi d(int numero, Famille famille, int directions,
			float premier, float deuxieme, float troisieme) {
		return new Defi(numero, famille, directions, premier, deuxieme, troisieme);
	}

	private static final class Caresse {
		private final int compagnon;
		private final int ticksTotal;
		private final Defi defi;
		private int ticksRestants;
		private int etape;
		private int score;
		private int reussites;
		private int retourTicks;
		private boolean dernierReussi;
		private int ticksDansRuban;
		private int ticksBienTenus;

		private Caresse(int compagnon, int ticksRestants, int ticksTotal, Defi defi) {
			this.compagnon = compagnon;
			this.ticksRestants = ticksRestants;
			this.ticksTotal = ticksTotal;
			this.defi = defi;
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
		int index;
		if (dernierDefi < 0) {
			index = ThreadLocalRandom.current().nextInt(DEFIS.length);
		} else {
			index = ThreadLocalRandom.current().nextInt(DEFIS.length - 1);
			if (index >= dernierDefi) {
				index++;
			}
		}
		dernierDefi = index;
		enCours.put(joueur, new Caresse(compagnon, ticks, ticks, DEFIS[index]));
		Minecraft client = Minecraft.getInstance();
		gaucheAvant = client.options.keyLeft.isDown();
		droiteAvant = client.options.keyRight.isDown();

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
			if (caresse.retourTicks > 0) {
				caresse.retourTicks--;
			}
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

	/** Lit le geste avant d'annuler le déplacement du joueur. */
	private static void jouerLeRythmeLocal() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		Caresse caresse = enCours.get(client.player.getId());
		if (caresse == null || caresse.etape >= 3) {
			return;
		}
		boolean gauche = client.options.keyLeft.isDown();
		boolean droite = client.options.keyRight.isDown();
		boolean nouvelAppuiGauche = gauche && !gaucheAvant;
		boolean nouvelAppuiDroite = droite && !droiteAvant;
		boolean attendDroite = caresse.defi.droite(caresse.etape);
		float t = caresse.progression(0.0F);
		float cible = caresse.defi.temps(caresse.etape);
		switch (caresse.defi.famille) {
			case RYTHME -> {
				boolean bonAppui = attendDroite ? nouvelAppuiDroite : nouvelAppuiGauche;
				if (bonAppui && Math.abs(t - cible) <= FENETRE_RYTHME) {
					terminerEtape(caresse, true);
				} else if (t > cible + FENETRE_RYTHME) {
					terminerEtape(caresse, false);
				}
			}
			case RUBAN -> {
				if (Math.abs(t - cible) <= FENETRE_RUBAN) {
					caresse.ticksDansRuban++;
					if (attendDroite ? droite : gauche) {
						caresse.ticksBienTenus++;
					}
				}
				if (t > cible + FENETRE_RUBAN) {
					boolean reussi = caresse.ticksDansRuban > 0
							&& caresse.ticksBienTenus * 2 >= caresse.ticksDansRuban;
					terminerEtape(caresse, reussi);
				}
			}
			case ECHO -> {
				boolean appui = nouvelAppuiGauche || nouvelAppuiDroite;
				if (appui && Math.abs(t - cible) <= FENETRE_ECHO) {
					boolean reussi = attendDroite
							? nouvelAppuiDroite && !nouvelAppuiGauche
							: nouvelAppuiGauche && !nouvelAppuiDroite;
					terminerEtape(caresse, reussi);
				} else if (t > cible + FENETRE_ECHO) {
					terminerEtape(caresse, false);
				}
			}
		}
		gaucheAvant = gauche;
		droiteAvant = droite;
	}

	private static void terminerEtape(Caresse caresse, boolean reussi) {
		if (reussi) {
			caresse.reussites |= 1 << caresse.etape;
			caresse.score++;
		}
		caresse.dernierReussi = reussi;
		caresse.retourTicks = 8;
		caresse.ticksDansRuban = 0;
		caresse.ticksBienTenus = 0;
		caresse.etape++;
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

	/**
	 * Un ruban de caresse lisible d'un coup d'oeil.
	 *
	 * <p>L'ancien panneau montrait Q-D-Q dans trois cases sans expliquer quand
	 * appuyer. Ici, une lueur parcourt une ligne et rejoint trois coeurs : une
	 * seule consigne, une seule touche affichee a la fois, et un retour immediat.
	 */
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

		int largeur = Math.min(230, g.guiWidth() - 12);
		int x = (g.guiWidth() - largeur) / 2;
		int y = g.guiHeight() - 102;
		int hauteur = 58;
		g.fill(x + 2, y + 2, x + largeur + 2, y + hauteur + 2, 0x70000000);
		g.fill(x + 2, y, x + largeur - 2, y + hauteur, 0xE6332737);
		g.fill(x, y + 2, x + largeur, y + hauteur - 2, 0xE6332737);
		g.fill(x + 3, y + 3, x + largeur - 3, y + hauteur - 3, 0xF3F1E4D2);
		g.fill(x + 4, y + 4, x + largeur - 4, y + 5, 0x80FFF5D6);
		String titre = net.minecraft.network.chat.Component.translatable(
				"caresse.compagnon.defi." + caresse.defi.numero).getString();
		g.drawCenteredString(client.font, titre, x + largeur / 2, y + 7, COULEUR_ENCRE);

		int debut = x + 16;
		int fin = x + largeur - 16;
		int pisteY = y + 29;
		g.fill(debut, pisteY, fin, pisteY + 2, 0x555B4555);
		int progression = debut + Math.round((fin - debut) * caresse.progression(partiel));
		g.fill(debut, pisteY, progression, pisteY + 2, COULEUR_OR);

		for (int i = 0; i < 3; i++) {
			float temps = caresse.defi.temps(i);
			int centre = debut + Math.round((fin - debut) * temps);
			boolean fait = i < caresse.etape;
			boolean reussi = (caresse.reussites & (1 << i)) != 0;
			boolean courant = i == caresse.etape;
			int couleur = reussi ? COULEUR_REUSSITE : fait ? COULEUR_RATE : 0xFFB69A8C;
			if (caresse.defi.famille == Famille.RUBAN && !fait) {
				int demi = Math.max(7, Math.round((fin - debut) * FENETRE_RUBAN));
				g.fill(centre - demi, pisteY - 2, centre + demi, pisteY + 4,
						courant ? 0x65E2B85E : 0x306D5968);
			}
			if (courant) {
				float pulsation = (float) (Math.sin(System.currentTimeMillis() / 110.0D) * 0.5D + 0.5D);
				couleur = Peinture.melanger(0xFFD39A55, 0xFFFFDF78, pulsation);
				g.fill(centre - 6, pisteY - 6, centre + 7, pisteY + 8, 0x303A2A38);
			}
			if (caresse.defi.famille == Famille.ECHO) {
				dessinerEtapeEcho(g, client, caresse, i, centre, pisteY, couleur);
			} else {
				dessinerCoeur(g, centre, pisteY, couleur);
			}
		}

		// La petite lueur suit la progression et rend le moment d'appui evident.
		g.fill(progression - 1, pisteY - 3, progression + 2, pisteY + 5, 0x80FFF1B0);
		g.fill(progression, pisteY - 2, progression + 1, pisteY + 4, 0xFFFFFFFF);

		String indication;
		if (caresse.etape < 3) {
			String touche = touche(client, caresse, caresse.etape);
			indication = switch (caresse.defi.famille) {
				case RYTHME -> net.minecraft.network.chat.Component.translatable(
						"caresse.compagnon.indication", touche).getString();
				case RUBAN -> net.minecraft.network.chat.Component.translatable(
						"caresse.compagnon.maintenir", touche).getString();
				case ECHO -> caresse.progression(partiel) < 0.28F
						? net.minecraft.network.chat.Component.translatable(
								"caresse.compagnon.memorise",
								touche(client, caresse, 0), touche(client, caresse, 1),
								touche(client, caresse, 2)).getString()
						: net.minecraft.network.chat.Component.translatable(
								"caresse.compagnon.reproduis", caresse.etape + 1).getString();
			};
		} else {
			indication = net.minecraft.network.chat.Component.translatable(
					"caresse.compagnon.profite").getString();
		}
		int couleurIndication = caresse.retourTicks > 0
				? caresse.dernierReussi ? COULEUR_REUSSITE : 0xFFB65D6A
				: COULEUR_ENCRE;
		dessinerTexteAjuste(g, client, indication, x + largeur / 2, y + 44,
				largeur - 16, couleurIndication);
	}

	private static String touche(Minecraft client, Caresse caresse, int etape) {
		return caresse.defi.droite(etape)
				? client.options.keyRight.getTranslatedKeyMessage().getString()
				: client.options.keyLeft.getTranslatedKeyMessage().getString();
	}

	private static void dessinerEtapeEcho(GuiGraphics g, Minecraft client,
			Caresse caresse, int etape, int x, int y, int couleur) {
		boolean visible = caresse.progression(0.0F) < 0.28F || etape < caresse.etape;
		g.fill(x - 6, y - 6, x + 7, y + 8, 0xD9F7EBD4);
		g.fill(x - 5, y - 5, x + 6, y + 7, couleur);
		String texte = visible ? touche(client, caresse, etape) : "?";
		g.drawCenteredString(client.font, texte, x, y - 3, 0xFFFFFFFF);
	}

	private static void dessinerTexteAjuste(GuiGraphics g, Minecraft client,
			String texte, int centreX, int y, int largeurMax, int couleur) {
		int largeur = client.font.width(texte);
		float echelle = Math.min(1.0F, largeurMax / (float) Math.max(1, largeur));
		g.pose().pushPose();
		g.pose().translate(centreX, y, 0.0F);
		g.pose().scale(echelle, echelle, 1.0F);
		g.drawString(client.font, texte, -largeur / 2, 0, couleur, false);
		g.pose().popPose();
	}

	/** Un coeur de cinq pixels, net meme avec une petite interface. */
	private static void dessinerCoeur(GuiGraphics g, int x, int y, int couleur) {
		g.fill(x - 4, y - 3, x - 1, y, couleur);
		g.fill(x + 1, y - 3, x + 4, y, couleur);
		g.fill(x - 5, y - 2, x + 5, y + 2, couleur);
		g.fill(x - 3, y + 2, x + 3, y + 4, couleur);
		g.fill(x - 1, y + 4, x + 1, y + 5, couleur);
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
