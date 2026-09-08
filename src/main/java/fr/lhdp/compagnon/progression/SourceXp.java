package fr.lhdp.compagnon.progression;

/**
 * Les trois sources d'experience, et il n'y en a que trois.
 *
 * <p>Chacune a un plafond par jour reel. Nourrir dix fois d'affilee ne rapporte
 * pas dix fois ; revenir demain, oui. Ce qui cree l'attachement, c'est la
 * regularite.
 *
 * <p><b>Gagner de l'experience juste en etant a cote de lui : non.</b> Ca
 * recompenserait le fait de rester connecte, pas de s'occuper de son compagnon.
 * Il n'y a donc volontairement aucune source de ce genre ici.
 */
public enum SourceXp {

	/** Le nourrir et le soigner : s'occuper de ses besoins. */
	SOINS("soins"),

	/** Le caresser et jouer avec lui : les interactions d'affection. */
	AFFECTION("affection"),

	/** L'emmener avec soi : marcher, explorer pendant qu'il suit. */
	BALADE("balade");

	private final String cle;

	SourceXp(String cle) {
		this.cle = cle;
	}

	/** Le nom utilise dans les fichiers de donnees et dans la sauvegarde. */
	public String cle() {
		return this.cle;
	}

	public static SourceXp depuis(String cle) {
		for (SourceXp source : values()) {
			if (source.cle.equals(cle)) {
				return source;
			}
		}
		return null;
	}
}
