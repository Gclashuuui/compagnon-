package fr.lhdp.compagnon;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/**
 * Les icones du mod, dessinees dans une police.
 *
 * <h2>Le probleme</h2>
 *
 * <p>Un mod qui veut un petit coeur a cote d'une barre a trois choix. Il dessine
 * la texture lui-meme, et alors elle ne marche que dans les ecrans, jamais dans
 * une phrase. Il utilise un emoji, et la police du jeu ne sait pas les dessiner.
 * Ou il ecrit « coeur », et c'est laid.
 *
 * <h2>La technique</h2>
 *
 * <p>C'est celle des gros serveurs, et elle est dans le jeu depuis toujours : on
 * livre <b>une police</b>. Un petit PNG de dix dessins de huit pixels, un
 * fichier qui dit quel caractere est quel dessin, et les icones deviennent des
 * <b>lettres</b>.
 *
 * <p>Ce qui change tout : une lettre se met partout. Dans un titre, dans un
 * message de chat, dans une barre d'action, au milieu d'une phrase du livre,
 * dans un nom de bouton. Elle s'aligne toute seule sur le texte autour, parce
 * que c'en est.
 *
 * <p>Aucune dependance, aucun mod a installer : le PNG fait deux cents octets et
 * il est fabrique par un script du depot.
 *
 * <h2>Elles prennent la couleur du texte</h2>
 *
 * <p>Les dessins sont en blanc. Le jeu teinte les glyphes par la couleur de la
 * phrase : la meme etoile est doree dans un titre de niveau, rouge dans une
 * alerte, et a l'encre sur un parchemin. On dessine une icone, on en obtient
 * autant qu'on a de couleurs.
 *
 * <h2>Les caracteres</h2>
 *
 * <p>Ils sont pris dans la zone privee d'Unicode, celle qui n'appartient a
 * aucune langue : ils ne peuvent donc voler la place d'aucun vrai caractere, et
 * un joueur ne peut pas les taper par accident.
 */
public final class Icones {

	private Icones() {
	}

	/** La police du mod. Sans elle, les caracteres ci-dessous sont des carres. */
	public static final ResourceLocation POLICE = Compagnon.id("icones");

	public static final String COEUR = "\ue000";
	public static final String FAIM = "\ue001";
	public static final String ENERGIE = "\ue002";
	public static final String ETOILE = "\ue003";
	public static final String PATTE = "\ue004";
	public static final String PLUME = "\ue005";
	public static final String SOIN = "\ue006";
	public static final String COURONNE = "\ue007";
	public static final String CLOCHE = "\ue008";
	public static final String PAROLE = "\ue009";

	/** Combien la police en contient. Le test s'en sert pour verifier le fichier. */
	public static final int COMBIEN = 10;

	/**
	 * Une icone, prete a etre affichee ou collee a une phrase.
	 *
	 * <p>Le style de police ne se met que sur ce morceau-la : la phrase qui suit
	 * reste dans la police du jeu. C'est pour ca qu'on peut les melanger.
	 */
	public static MutableComponent de(String glyphe) {
		return Component.literal(glyphe).withStyle(style -> style.withFont(POLICE));
	}

	/**
	 * Une icone suivie d'une phrase, avec l'espace entre les deux.
	 *
	 * <p>Le raccourci le plus utile : c'est la forme qu'on ecrit neuf fois sur
	 * dix, et l'oublier une fois donne une icone collee au premier mot.
	 */
	public static MutableComponent devant(String glyphe, Component phrase) {
		// Le style d'un composant parent est herite par ses enfants. Sans remettre
		// explicitement la police vanilla, toute la phrase est cherchee dans notre
		// minuscule planche d'icones et apparait sous forme de carres.
		MutableComponent texte = phrase.copy()
				.withStyle(style -> style.withFont(Style.DEFAULT_FONT));
		return de(glyphe).append(Component.literal(" ")
				.withStyle(style -> style.withFont(Style.DEFAULT_FONT))).append(texte);
	}
}
