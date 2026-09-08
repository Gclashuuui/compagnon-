package fr.lhdp.compagnon.espece;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Un morceau de compagnon qu'on ne peut pas traverser.
 *
 * <h2>Pourquoi ce fichier existe</h2>
 *
 * <p>Minecraft ne connait <b>qu'une seule boite par entite</b>, et elle est
 * carree vue de dessus. Un dragonnet fait 0,73 bloc de large pour 1,98 de long :
 * aucun carre ne peut couvrir sa tete et sa queue sans faire deux metres de
 * large et bloquer les couloirs. On traversait donc sa tete.
 *
 * <p>Le mod ne s'en remet donc pas au moteur : il decrit lui-meme une liste de
 * morceaux, et repousse ce qui entre dedans. C'est du code a nous, pas une
 * limite subie.
 *
 * <h2>Comment on ecrit un decalage</h2>
 *
 * <p>Les trois nombres sont donnes <b>dans le repere du compagnon</b>, en blocs,
 * et tournent avec lui :
 *
 * <ul>
 *   <li>{@code droite} — positif vers sa droite ;</li>
 *   <li>{@code haut} — positif vers le ciel, a partir de ses pattes ;</li>
 *   <li>{@code avant} — positif vers l'avant, la ou il regarde.</li>
 * </ul>
 *
 * <p>La tete se met donc en {@code avant} positif, la queue en negatif.
 *
 * @param nom     a quoi ca correspond, pour s'y retrouver dans le fichier
 * @param droite  decalage lateral, en blocs
 * @param haut    hauteur du bas du morceau, en blocs
 * @param avant   decalage vers l'avant, en blocs
 * @param largeur cote du morceau, en blocs
 * @param hauteur hauteur du morceau, en blocs
 */
public record Partie(String nom, double droite, double haut, double avant,
		double largeur, double hauteur) {

	/**
	 * La boite de ce morceau dans le monde, tournee selon l'orientation du corps.
	 *
	 * <p>On part de l'orientation du <b>corps</b> et non du regard : sinon la tete
	 * de collision partirait sur le cote des que le compagnon regarde ailleurs,
	 * alors que son modele, lui, reste droit.
	 */
	public AABB boiteMonde(Vec3 position, float orientationDuCorps) {
		double angle = Math.toRadians(orientationDuCorps);
		return boiteMonde(position, Math.sin(angle), Math.cos(angle));
	}

	/**
	 * La meme boite, quand l'appelant a deja le sinus et le cosinus.
	 *
	 * <p>Toutes les parties d'un compagnon partagent la meme orientation. La
	 * version ci-dessus recalculait pourtant le sinus et le cosinus <b>pour chacune
	 * d'elles</b> : cinq parties, deux cotes, vingt fois par seconde et par
	 * compagnon. Dans un chateau plein, cela faisait des dizaines de milliers
	 * d'appels trigonometriques par seconde pour cinq resultats identiques.
	 */
	public AABB boiteMonde(Vec3 position, double sin, double cos) {
		// Repere de Minecraft : a l'angle zero on regarde vers le sud, donc +Z.
		//   avant  = (-sin, 0,  cos)
		//   droite = (-cos, 0, -sin)
		double x = position.x + (-cos * this.droite) + (-sin * this.avant);
		double z = position.z + (-sin * this.droite) + (cos * this.avant);
		double y = position.y + this.haut;

		double demi = this.largeur / 2.0;
		return new AABB(x - demi, y, z - demi, x + demi, y + this.hauteur, z + demi);
	}
}
