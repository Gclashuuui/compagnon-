package fr.lhdp.compagnon.entite;

/**
 * Les quatre tensions lentes qui donnent une continuité au comportement.
 *
 * <p>Ce n'est ni un modèle génératif ni un second arbre de buts. La perception
 * produit déjà les faits utiles ; cette classe les transforme en envies qui
 * montent et redescendent progressivement. Le compagnon ne passe donc plus de
 * « rien » à « très curieux » sur une seule image, et deux caractères
 * différents ne prennent pas la même décision devant la même scène.
 *
 * <p>Quatre nombres, aucune collection et aucun accès au monde : le coût et la
 * mémoire restent constants, quel que soit le nombre d'heures de jeu.
 */
public final class EtatInterieur {

	/** Tout ce que la couche émotionnelle a besoin de savoir à cet instant. */
	public record Observation(
			boolean maitreProche,
			boolean maitreEnDanger,
			boolean menace,
			boolean evenement,
			boolean amiProche,
			boolean orage,
			boolean obscurite,
			boolean eau,
			boolean inactif,
			boolean dort,
			float energie,
			float lien,
			float courage,
			float curiosite,
			float attachement,
			float vivacite,
			float calin) {

		public Observation {
			energie = borner(energie);
			lien = borner(lien);
			courage = borner(courage);
			curiosite = borner(curiosite);
			attachement = borner(attachement);
			vivacite = borner(vivacite);
			calin = borner(calin);
		}
	}

	private float stress = 0.10F;
	private float ennui = 0.20F;
	private float besoinDeContact = 0.25F;
	private float envieDExplorer = 0.25F;

	/** Une mise à jour lente : les valeurs s'approchent de leur cible sans saut. */
	public void actualiser(Observation vue) {
		float danger = 0.04F;
		if (vue.menace()) {
			danger = 1.0F;
		} else if (vue.maitreEnDanger()) {
			danger = 0.90F;
		} else {
			if (vue.orage()) {
				danger += 0.25F + (1.0F - vue.courage()) * 0.45F;
			}
			if (vue.obscurite()) {
				danger += (1.0F - vue.courage()) * 0.28F;
			}
			if (vue.eau()) {
				danger += 0.14F;
			}
		}
		if (vue.maitreProche() && !vue.maitreEnDanger()) {
			danger *= 0.78F;
		}
		float stressVise = borner(danger + (1.0F - vue.energie()) * 0.16F);
		this.stress = approcher(this.stress, stressVise, 0.24F);

		float ennuiVise;
		if (vue.dort()) {
			ennuiVise = 0.0F;
		} else if (vue.evenement()) {
			ennuiVise = 0.04F;
		} else if (vue.inactif()) {
			ennuiVise = 0.28F + vue.vivacite() * 0.52F;
		} else {
			ennuiVise = 0.10F;
		}
		if (vue.amiProche()) {
			ennuiVise -= 0.22F;
		}
		this.ennui = approcher(this.ennui, borner(ennuiVise), 0.13F);

		float contactVise = vue.maitreProche()
				? 0.06F + vue.calin() * (1.0F - vue.lien()) * 0.24F
				: 0.25F + vue.attachement() * 0.45F + vue.calin() * 0.20F;
		if (vue.amiProche()) {
			contactVise -= 0.18F;
		}
		if (vue.maitreEnDanger()) {
			contactVise = Math.max(contactVise, 0.88F);
		}
		contactVise += this.stress * 0.12F;
		this.besoinDeContact = approcher(this.besoinDeContact,
				borner(contactVise), 0.14F);

		float explorationVisee = vue.evenement()
				? 0.52F + vue.curiosite() * 0.44F
				: 0.10F + this.ennui * 0.52F + vue.curiosite() * 0.26F;
		explorationVisee *= (0.32F + vue.energie() * 0.68F)
				* (1.0F - this.stress * 0.72F);
		if (vue.menace() || vue.maitreEnDanger() || vue.dort()) {
			explorationVisee = 0.0F;
		}
		this.envieDExplorer = approcher(this.envieDExplorer,
				borner(explorationVisee), 0.19F);
	}

	public float stress() {
		return this.stress;
	}

	public float ennui() {
		return this.ennui;
	}

	public float besoinDeContact() {
		return this.besoinDeContact;
	}

	public float envieDExplorer() {
		return this.envieDExplorer;
	}

	private static float approcher(float valeur, float cible, float vitesse) {
		return borner(valeur + (cible - valeur) * vitesse);
	}

	private static float borner(float valeur) {
		return Math.max(0.0F, Math.min(1.0F, valeur));
	}
}
