package fr.lhdp.compagnon.client;

import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Le compagnon vu comme un mannequin d'atelier : immobile, et la gueule ouverte
 * si on le demande.
 *
 * <h2>Pourquoi il faut le figer</h2>
 *
 * <p>On ne cale pas une balle sur une machoire qui bouge. Le compagnon respire,
 * cligne, balance la queue et ouvre la gueule quatre fois par minute : viser le
 * bon endroit revient a viser une cible mouvante, et on ne sait jamais si
 * l'ecart qu'on voit vient du reglage ou de l'animation.
 *
 * <h2>Comment on le fige, et pourquoi pas autrement</h2>
 *
 * <p>La solution evidente serait de couper l'animation. Elle est mauvaise :
 * sans animation, les os d'un compagnon retombent sur la <b>pose de
 * geometrie</b> — ailes deployees, pattes ecartees, une pose que personne ne
 * voit jamais en jeu. Caler un objet dessus, c'est le caler sur une bete qui
 * n'existe pas.
 *
 * <p>On prend donc une <b>photographie</b> : a la premiere image, on note la
 * position de chaque os, et on la repose telle quelle a toutes les suivantes.
 * La bete se fige dans la pose ou on l'a trouvee — une vraie pose, celle de son
 * animation d'attente — et elle y reste tant que l'editeur est ouvert.
 *
 * <h2>La gueule s'ouvre</h2>
 *
 * <p>Une fois fige, plus rien ne conteste ce qu'on ecrit sur un os. On peut donc
 * faire pivoter la machoire de quelques degres et regarder la balle se placer
 * <b>entre les dents</b> plutot que derriere elles. C'est ce que fait le curseur
 * de l'editeur.
 */
public final class Mannequin {

	private Mannequin() {
	}

	/** Les noms d'os qui peuvent etre une machoire, du plus precis au plus large. */
	private static final String[] OS_DE_GUEULE = {
			"jawController", "lowerJawController", "lowerjawController",
			"jaw_act", "jaw", "lowerJaw", "lowerjaw", "machoire"};

	/** Vrai pendant que l'editeur dessine. Rien d'autre ne doit le lire. */
	private static boolean actif;

	/** De combien la gueule est ouverte, en degres. */
	private static float gueule;

	/** La photographie : nom d'os vers ses neuf valeurs. */
	private static final Map<String, float[]> pose = new LinkedHashMap<>();

	/**
	 * La derniere matrice de l'objet pose, telle qu'elle etait a l'ecran.
	 *
	 * <p>C'est la piece qui rend les fleches possibles. Au moment ou l'objet est
	 * dessine, cette matrice contient <b>tout</b> : la vue de l'editeur, la pose
	 * de la bete, l'os d'accroche et le reglage en cours. Sa translation est donc
	 * la position de l'objet <b>en pixels d'ecran</b>, et ses trois colonnes sont
	 * la direction de ses axes, en pixels par unite de modele.
	 *
	 * <p>Aucune projection a refaire de notre cote, donc aucune occasion de se
	 * tromper : on lit ce que le jeu vient de calculer.
	 */
	private static Matrix4f matriceDeLObjet;

	// --- L'editeur commande ---------------------------------------------------------

	/** A appeler juste avant de dessiner la bete dans l'editeur. */
	public static void commencer(float ouvertureDeLaGueule) {
		actif = true;
		gueule = ouvertureDeLaGueule;
	}

	/** Et juste apres : le monde ne doit jamais etre dessine en mode mannequin. */
	public static void finir() {
		actif = false;
	}

	public static boolean actif() {
		return actif;
	}

	/** On change de bete, ou on ferme : la photographie ne vaut plus rien. */
	public static void oublier() {
		pose.clear();
		matriceDeLObjet = null;
	}

	// --- Ce que le modele appelle ---------------------------------------------------

	/**
	 * Fige cet os-la, et ouvre la gueule si c'en est une.
	 *
	 * <p>Appelee pour chaque os a chaque image, apres l'animation. On ecrit des
	 * valeurs <b>absolues</b> et non des ajouts : rien ne s'accumule, quoi que
	 * l'animation ait fait juste avant.
	 */
	public static void figer(GeoBone os) {
		float[] photo = pose.get(os.getName());
		if (photo == null) {
			pose.put(os.getName(), new float[]{
					os.getRotX(), os.getRotY(), os.getRotZ(),
					os.getPosX(), os.getPosY(), os.getPosZ(),
					os.getScaleX(), os.getScaleY(), os.getScaleZ()});
			return;
		}
		os.setRotX(photo[0]);
		os.setRotY(photo[1]);
		os.setRotZ(photo[2]);
		os.setPosX(photo[3]);
		os.setPosY(photo[4]);
		os.setPosZ(photo[5]);
		os.setScaleX(photo[6]);
		os.setScaleY(photo[7]);
		os.setScaleZ(photo[8]);

		if (gueule != 0.0F && estUneGueule(os.getName())) {
			// Vers le bas : une machoire inferieure s'ouvre en descendant.
			os.setRotX(photo[0] + gueule * ((float) Math.PI / 180.0F));
		}
	}

	private static boolean estUneGueule(String nom) {
		for (String candidat : OS_DE_GUEULE) {
			if (candidat.equals(nom)) {
				return true;
			}
		}
		return false;
	}

	/** Le calque de l'objet depose ici ce que le jeu vient de calculer. */
	public static void noterLaMatrice(Matrix4f matrice) {
		matriceDeLObjet = new Matrix4f(matrice);
	}

	/** La matrice de l'objet, ou {@code null} si rien n'a ete dessine. */
	public static Matrix4f matrice() {
		return matriceDeLObjet;
	}
}
