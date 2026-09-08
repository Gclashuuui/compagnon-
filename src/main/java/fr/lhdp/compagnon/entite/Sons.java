package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.espece.Espece;
import fr.lhdp.compagnon.espece.Especes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.UUID;

/**
 * La voix des compagnons.
 *
 * <h2>Le mod etait muet</h2>
 *
 * <p>Pas un son. Ni quand on le caresse, ni quand il decolle, ni quand il a mal.
 * C'est ce qui separe une marionnette d'un animal : on regarde une marionnette,
 * on <b>vit avec</b> quelque chose qui fait du bruit.
 *
 * <h2>Aucun fichier son a livrer</h2>
 *
 * <p>Les sons sont ceux du jeu, nommes dans la fiche d'espece. Le jeu en a des
 * centaines, dont des dizaines de cris d'animaux : une mouette qui emprunte la
 * voix du perroquet et un dragonnet qui emprunte celle du renard sonnent tres
 * bien, et le mod ne grossit pas d'un octet.
 *
 * <p>Une espece qui ne declare rien reste silencieuse, exactement comme avant.
 * Rien ne casse, et on remplit les fiches a son rythme.
 *
 * <h2>Chaque bete a sa propre voix</h2>
 *
 * <p>La hauteur du son depend de l'identifiant du compagnon. Deux mouettes ne
 * crient donc pas pareil, et on finit par reconnaitre la sienne <b>a l'oreille</b>
 * dans une salle commune pleine — ce qui ne coute rien du tout et qu'aucune
 * autre partie du mod ne peut donner.
 */
public final class Sons {

	private Sons() {
	}

	// --- Les roles, cites par les fiches d'espece ----------------------------

	/** De temps en temps, sans raison. C'est sa presence sonore. */
	public static final String AMBIANCE = "ambiance";

	/** Quand il est content : une caresse, un repas, une reussite. */
	public static final String CONTENT = "content";

	/** Quand il a mal, ou qu'on le bouscule. */
	public static final String MAL = "mal";

	/** Quand il decolle. */
	public static final String ENVOL = "envol";

	/** Quand il a repere quelque chose et qu'il te le signale. */
	public static final String ALERTE = "alerte";

	/**
	 * De combien la hauteur varie d'une bete a l'autre.
	 *
	 * <p>Un cinquieme au-dessus et au-dessous. Assez pour qu'on entende la
	 * difference entre deux betes de la meme espece, assez peu pour qu'aucune ne
	 * sonne ridicule.
	 */
	private static final float ECART_DE_VOIX = 0.20F;

	/** Le volume de base. Discret : ce sont des betes, pas des enceintes. */
	private static final float VOLUME = 0.55F;

	/**
	 * Joue un son de cette bete, s'il en a un pour ce role.
	 *
	 * <p>Sans effet cote client : le serveur le joue pour tout le monde, y compris
	 * pour celui qui a declenche l'action. Le jouer des deux cotes le ferait
	 * entendre deux fois a une seule personne.
	 */
	public static void jouer(CompagnonEntity compagnon, String role) {
		jouer(compagnon, role, 1.0F);
	}

	/**
	 * La meme chose, avec un volume module.
	 *
	 * @param force multiplie le volume : un decollage se remarque plus qu'un
	 *              soupir d'ambiance
	 */
	public static void jouer(CompagnonEntity compagnon, String role, float force) {
		if (compagnon == null || compagnon.level().isClientSide()) {
			return;
		}
		SoundEvent son = sonDe(compagnon, role);
		if (son == null) {
			return;
		}
		compagnon.level().playSound(null, compagnon.getX(), compagnon.getY(), compagnon.getZ(),
				son, SoundSource.NEUTRAL, VOLUME * force, hauteurDe(compagnon));
	}

	/**
	 * Joue un son du jeu, a la voix de cette bete.
	 *
	 * <p>Pour les bruits qu'aucune espece n'a a declarer parce qu'ils ne lui
	 * appartiennent pas : machouiller, boire. Une mouette et un dragonnet
	 * mangent du meme bruit — seule la hauteur change, et c'est deja la sienne.
	 */
	public static void jouerCeSon(CompagnonEntity compagnon, SoundEvent son,
			float force) {

		if (compagnon == null || son == null || compagnon.level().isClientSide()) {
			return;
		}
		compagnon.level().playSound(null, compagnon.getX(), compagnon.getY(),
			compagnon.getZ(), son, SoundSource.NEUTRAL, VOLUME * force,
			hauteurDe(compagnon));
	}

	/**
	 * Le son du jeu qui correspond a ce role chez cette espece, ou {@code null}.
	 *
	 * <p>Un nom qui ne designe aucun son du jeu rend {@code null} plutot que de
	 * lever une erreur : une faute de frappe dans une fiche d'espece rend une bete
	 * muette, elle ne fait pas tomber le serveur.
	 */
	public static SoundEvent sonDe(CompagnonEntity compagnon, String role) {
		Espece espece = Especes.get(compagnon.espece());
		if (espece == null) {
			return null;
		}
		String nom = espece.son(role);
		if (nom == null || nom.isEmpty()) {
			return null;
		}
		ResourceLocation chemin = ResourceLocation.tryParse(nom);
		return chemin == null ? null : BuiltInRegistries.SOUND_EVENT.get(chemin);
	}

	/**
	 * La hauteur de voix de cette bete-la.
	 *
	 * <p>Tiree de son identifiant, donc stable pour toujours et sans rien a
	 * sauvegarder. Une bete sans fiche — un apercu dans un ecran — prend la voix
	 * du milieu.
	 */
	public static float hauteurDe(CompagnonEntity compagnon) {
		UUID id = compagnon.ficheId();
		if (id == null) {
			return 1.0F;
		}
		// Les bits de poids faible, ramenes entre -1 et 1. On evite le modulo sur
		// un nombre signe, qui rend des negatifs et decalerait la moyenne.
		float part = (Math.abs(id.getLeastSignificantBits() % 1000) / 500.0F) - 1.0F;
		return 1.0F + part * ECART_DE_VOIX;
	}
}
