package fr.lhdp.compagnon.voix;

import fr.lhdp.compagnon.fiche.Mode;

/**
 * Ce qu'on peut demander a un compagnon a la voix.
 *
 * <p>Volontairement court : ce qu'un animal peut comprendre. Il ne parle pas, il
 * ne repond pas — il obeit, ou pas.
 *
 * <p>Il y a deux sortes d'ordres, et c'est la seule chose a comprendre ici :
 *
 * <ul>
 *   <li>ceux qui changent son <b>mode</b> — viens, assis, couche, reste. Ils
 *       durent jusqu'au suivant ;</li>
 *   <li>ceux qui lui font faire un <b>geste</b> — chope, lache. Ils ne changent
 *       pas ce qu'il fait de sa journee, ils lui font faire une chose et puis
 *       c'est fini.</li>
 * </ul>
 *
 * <p>Les <b>mots</b> qui declenchent chaque ordre vivent dans
 * {@code data/compagnon/voix.json}, pas ici : on ne parle pas tous pareil, et
 * ajouter une facon de dire ne doit pas demander de recompiler.
 */
public enum CommandeVocale {

	/** Il vient vers celui qui l'appelle, et le suit. */
	VIENS("viens", Mode.SUIT, null),

	ASSIS("assis", Mode.ASSIS, null),

	COUCHE("couche", Mode.COUCHE, null),

	/** Il se leve, ne suit plus, et vit sa vie la ou il est. */
	RESTE("reste", Mode.RESTE, null),

	/** Il va chercher l'objet pose par terre et le prend dans la gueule. */
	CHOPE("chope", null, Geste.PRENDRE),

	/** Il pose ce qu'il portait, devant lui. */
	LACHE("lache", null, Geste.LACHER),

	/**
	 * Il decolle et plane au-dessus de toi.
	 *
	 * <p>Reserve aux especes qui volent : a une bete qui marche, on repond
	 * qu'elle n'a pas d'ailes plutot que de la faire sauter sur place.
	 */
	MONTE("monte", null, Geste.MONTER),

	/** Il redescend se poser. */
	DESCENDS("descends", null, Geste.DESCENDRE),

	/** Il fait un tour sur lui-meme. Ca ne sert a rien, et c'est le but. */
	TOURNE("tourne", null, Geste.TOURNER),

	/**
	 * On le felicite.
	 *
	 * <p>Le seul ordre qui ne demande rien. C'est aussi le seul qui fasse
	 * monter la complicite a la voix : jusqu'ici il fallait le toucher pour lui
	 * dire qu'on etait content de lui.
	 */
	BRAVO("bravo", null, Geste.FELICITER);

	/** Un ordre qui ne dure pas : il le fait, et c'est fini. */
	public enum Geste {
		PRENDRE,
		LACHER,
		MONTER,
		DESCENDRE,
		TOURNER,
		FELICITER
	}

	private final String cle;
	private final Mode mode;
	private final Geste geste;

	CommandeVocale(String cle, Mode mode, Geste geste) {
		this.cle = cle;
		this.mode = mode;
		this.geste = geste;
	}

	/** Le nom utilise dans le fichier de donnees. */
	public String cle() {
		return this.cle;
	}

	/** Le mode dans lequel l'ordre le met, ou {@code null} si c'est un geste. */
	public Mode mode() {
		return this.mode;
	}

	/** Le geste demande, ou {@code null} si l'ordre change son mode. */
	public Geste geste() {
		return this.geste;
	}

	public static CommandeVocale depuis(String cle) {
		for (CommandeVocale commande : values()) {
			if (commande.cle.equals(cle)) {
				return commande;
			}
		}
		return null;
	}
}
