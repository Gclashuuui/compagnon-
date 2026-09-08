package fr.lhdp.compagnon.fiche;

/**
 * Ce que le compagnon est en train de faire. Un ordre tient jusqu'au suivant :
 * il survit a la deconnexion et au dechargement de la zone.
 */
public enum Mode {

	/** Il suit son proprietaire. Un seul compagnon a la fois peut etre dans ce mode. */
	SUIT,

	/** Il reste ou il a ete pose, et y vit sa vie. */
	RESTE,

	/** Assis. */
	ASSIS,

	/** Couche. */
	COUCHE;

	/** Le cycle du clic droit, comme le loup : suit -> assis -> couche -> suit. */
	public Mode suivant() {
		return switch (this) {
			case SUIT -> ASSIS;
			case ASSIS -> COUCHE;
			case COUCHE, RESTE -> SUIT;
		};
	}

	/** Vrai pour les modes ou il ne se deplace pas de lui-meme. */
	public boolean pose() {
		return this == ASSIS || this == COUCHE;
	}

	/**
	 * La cle de traduction du mode, pour le livre. Le texte lui-meme vit dans le
	 * fichier de langue : on peut le reformuler sans recompiler.
	 */
	public String cleDeTraduction() {
		return "livre.compagnon.mode." + name().toLowerCase();
	}

	public static Mode depuis(String nom, Mode defaut) {
		for (Mode mode : values()) {
			if (mode.name().equalsIgnoreCase(nom)) {
				return mode;
			}
		}
		return defaut;
	}
}
