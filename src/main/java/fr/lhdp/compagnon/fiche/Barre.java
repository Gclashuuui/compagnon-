package fr.lhdp.compagnon.fiche;

/**
 * Les barres du compagnon, hors XP.
 *
 * <p>Ce sont <b>nos</b> chiffres. Aucune ne s'appuie sur la faim, la vie ou
 * l'experience de Minecraft.
 *
 * <p>L'humeur n'est pas ici : elle n'est pas une barre, elle se calcule a partir
 * de celles-ci. Voir {@link Humeur}.
 */
public enum Barre {

	/** Descend toute seule, remonte quand on le nourrit. */
	FAIM("faim"),

	/** Descend quand il agit, remonte quand il se repose. */
	ENERGIE("energie"),

	/** La relation. Monte quand on s'occupe de lui regulierement. */
	COMPLICITE("complicite"),

	/**
	 * Descend avec les petits bobos, remonte avec les objets de soin.
	 * Elle ne tombe jamais a zero : au plus bas, il est couche et il attend.
	 */
	SANTE("sante");

	/** Toutes les barres vont de zero a cette valeur. */
	public static final float MAXIMUM = 100.0F;

	private final String cle;

	Barre(String cle) {
		this.cle = cle;
	}

	/** Le nom utilise dans les fichiers de donnees et dans la sauvegarde. */
	public String cle() {
		return this.cle;
	}

	public static Barre depuis(String cle) {
		for (Barre barre : values()) {
			if (barre.cle.equals(cle)) {
				return barre;
			}
		}
		return null;
	}
}
