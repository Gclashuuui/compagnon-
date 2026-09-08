package fr.lhdp.compagnon.client;

import fr.lhdp.compagnon.Compagnon;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Les bruits des interfaces, et le volume qu'on leur laisse.
 *
 * <h2>Les ecrans du mod etaient muets</h2>
 *
 * <p>Un seul son dans tout le code client : la page qui tourne dans le carnet.
 * Tout le reste — choisir un geste dans la roue, changer de compagnon, tourner
 * une page du livre, feuilleter le classement — se faisait dans le silence.
 *
 * <p>Ce n'est pas un detail decoratif. Dans le jeu, <b>chaque bouton clique</b>,
 * sans exception. Une interface qui ne repond pas au clic donne l'impression que
 * le clic n'est pas passe : on reclique, on doute, et l'ecran a l'air casse
 * alors qu'il marche tres bien.
 *
 * <h2>Trois bruits, pas un de plus</h2>
 *
 * <p>Chacun veut dire une chose precise, et on les distingue les yeux fermes :
 *
 * <ul>
 *   <li>{@link #clic} — c'est pris. Le bruit de bouton du jeu, celui que le
 *       joueur connait deja par coeur.</li>
 *   <li>{@link #refus} — non. Le meme bruit, en grave : on entend que c'est le
 *       contraire du clic sans avoir jamais eu a l'apprendre.</li>
 *   <li>{@link #page} — on a change de page.</li>
 * </ul>
 *
 * <h2>Et de quoi les faire taire</h2>
 *
 * <p>Un mod qui impose ses bruits est un mod qu'on finit par desinstaller. Trois
 * niveaux — fort, doux, muet — que la {@link Clochette} fait tourner d'un clic,
 * depuis n'importe quel ecran du mod.
 *
 * <p>Le choix est <b>propre a chaque joueur</b> et se garde d'une partie a
 * l'autre, dans un fichier d'un octet a cote des autres reglages. Rien ne part
 * sur le reseau : personne d'autre n'a a savoir si tu joues avec le son.
 *
 * <h2>Aucun fichier son a livrer</h2>
 *
 * <p>Tout vient des sons du jeu, joues a des hauteurs differentes. Le mod ne
 * grossit pas d'un octet, et les bruits sonnent comme du Minecraft parce que
 * c'en est.
 */
public final class Bruits {

	private Bruits() {
	}

	// --- Le volume ------------------------------------------------------------

	/** Rien du tout. */
	public static final int MUET = 0;

	/** Presents, mais en fond. */
	public static final int DOUX = 1;

	/** Comme les menus du jeu. */
	public static final int FORT = 2;

	/** Combien de niveaux la clochette fait tourner. */
	public static final int NIVEAUX = 3;

	/**
	 * Ce que chaque niveau multiplie.
	 *
	 * <p>Doux est a 40 % et non a 50 : l'oreille n'entend pas les volumes de
	 * facon lineaire, et un demi-volume s'entend presque comme le plein. A 40 %
	 * la difference se remarque du premier coup, ce qui est tout l'interet
	 * d'avoir un cran intermediaire.
	 */
	private static final float[] FORCES = {0.0F, 0.40F, 1.0F};

	/** Le fichier ou le choix se garde, a cote des autres reglages du jeu. */
	private static final String FICHIER = Compagnon.MOD_ID + "-bruits.txt";

	private static int niveau = FORT;

	/** Vrai des qu'on a lu le fichier : on ne le lit qu'une fois par session. */
	private static boolean lu;

	/** Fort, doux ou muet. */
	public static int niveau() {
		if (!lu) {
			lu = true;
			relire();
		}
		return niveau;
	}

	/**
	 * Passe au niveau suivant, et le retient.
	 *
	 * <p>Fort, doux, muet, et on retombe sur fort. Trois crans se parcourent plus
	 * vite qu'ils ne se reglent : c'est voulu. Un curseur demanderait de viser,
	 * la clochette demande de cliquer.
	 */
	public static int tourner() {
		niveau = (niveau() + NIVEAUX - 1) % NIVEAUX;
		ecrire();
		return niveau;
	}

	public static boolean muet() {
		return niveau() == MUET;
	}

	/** Ce par quoi les volumes sont multiplies, entre zero et un. */
	public static float force() {
		return FORCES[Math.max(0, Math.min(FORCES.length - 1, niveau()))];
	}

	// --- Les trois bruits -----------------------------------------------------

	/** Le bruit de bouton du jeu, celui de tous les menus. */
	private static SoundEvent bouton() {
		return SoundEvents.UI_BUTTON_CLICK.value();
	}

	/** C'est pris. */
	public static void clic() {
		jouer(bouton(), 1.0F, 0.35F);
	}

	/**
	 * Non.
	 *
	 * <p>Un cadenas, un geste deja en cours. Avant, ces clics-la ne faisaient
	 * <b>rien du tout</b> : ni son, ni mouvement. Le joueur ne pouvait pas
	 * distinguer « c'est refuse » de « ca n'a pas marche ».
	 */
	public static void refus() {
		jouer(bouton(), 0.55F, 0.30F);
	}

	/** Une page tourne. */
	public static void page() {
		jouer(SoundEvents.BOOK_PAGE_TURN, 1.0F, 0.7F);
	}

	/**
	 * La clochette, quand on vient de la faire tourner.
	 *
	 * <p>Elle sonne <b>au volume qu'elle vient de choisir</b>. Le reglage se
	 * demontre donc tout seul : on n'a pas a lire ce qui est ecrit dessus, on
	 * entend ce qu'on a pris. Sur « muet » elle se balance sans un bruit, ce qui
	 * dit la meme chose encore mieux.
	 */
	public static void clochette() {
		jouer(SoundEvents.NOTE_BLOCK_BELL.value(), 1.4F, 0.5F);
	}

	private static void jouer(SoundEvent son, float hauteur, float volume) {
		float force = force();
		if (force <= 0.0F || son == null) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}
		client.getSoundManager().play(
				SimpleSoundInstance.forUI(son, hauteur, volume * force));
	}

	// --- Le fichier -----------------------------------------------------------
	//
	// Un caractere. Pas de JSON, pas de bibliotheque, pas de version a faire
	// evoluer : un fichier illisible ou absent rend simplement le mod bavard,
	// ce qui est l'etat par defaut de toute facon.

	private static Path fichier() {
		return FabricLoader.getInstance().getConfigDir().resolve(FICHIER);
	}

	private static void relire() {
		try {
			Path ou = fichier();
			if (!Files.exists(ou)) {
				return;
			}
			String contenu = Files.readString(ou, StandardCharsets.UTF_8).trim();
			int lu = Integer.parseInt(contenu);
			if (lu >= 0 && lu < NIVEAUX) {
				niveau = lu;
			}
		} catch (Exception echec) {
			// Rien a dire : on reste au niveau par defaut.
		}
	}

	private static void ecrire() {
		try {
			Path ou = fichier();
			Files.createDirectories(ou.getParent());
			Files.writeString(ou, Integer.toString(niveau), StandardCharsets.UTF_8);
		} catch (Exception echec) {
			// Le choix tiendra le temps de la session. Ca ne vaut pas un message
			// d'erreur au joueur, qui n'y peut rien.
			Compagnon.LOG.warn("Le niveau des bruits n'a pas pu etre garde : {}",
					echec.toString());
		}
	}
}
