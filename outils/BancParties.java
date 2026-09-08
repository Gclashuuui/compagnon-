/**
 * Combien coute la boite de collision en plusieurs morceaux ?
 *
 * <h2>La question</h2>
 *
 * <p>{@code repousserDesParties()} tourne <b>a chaque tick, des deux cotes, pour
 * chaque compagnon charge</b>. Elle fabrique une boite par partie du corps, plus
 * une boite englobante, puis interroge le monde. Sur un chateau plein, cela fait
 * beaucoup de fois pas grand-chose — et « beaucoup de fois pas grand-chose » est
 * exactement le genre de chose qu'on croit couteux a tort.
 *
 * <p>Ce banc mesure la <b>partie calculable</b> : la fabrication des boites. Il ne
 * mesure PAS la requete au monde, qui demande un vrai serveur.
 *
 * <h2>Ce qu'il compare</h2>
 *
 * <p>La version d'origine recalculait sinus et cosinus <b>pour chaque partie</b>,
 * alors que l'orientation est la meme pour toutes. La version corrigee les calcule
 * une fois. Le banc chiffre l'ecart, au lieu de le supposer.
 *
 * <pre>javac -d outils outils/BancParties.java &amp;&amp; java -cp outils BancParties</pre>
 */
public final class BancParties {

	/** Le dragonnet en a cinq. */
	private static final int PARTIES = 5;

	/** Un chateau bien rempli : autant de compagnons charges autour des joueurs. */
	private static final int COMPAGNONS = 200;

	/** Vingt fois par seconde, et des deux cotes. */
	private static final int TICKS_PAR_SECONDE = 20;
	private static final int COTES = 2;

	private static final int CHAUFFE = 200_000;
	private static final int MESURES = 2_000_000;

	/** Les decalages des cinq parties du dragonnet, tires de son fichier d'espece. */
	private static final double[][] DECALAGES = {
			{ 0.0, 0.0, 0.0 },     // corps
			{ 0.0, 0.25, 0.45 },   // cou
			{ 0.0, 0.2, 0.95 },    // tete
			{ 0.0, 0.05, -0.7 },   // queue
			{ 0.0, 0.05, -1.25 },  // bout de la queue
	};

	/** Pour que la machine virtuelle ne supprime pas un calcul dont on ne se sert pas. */
	private static double gardeFou;

	public static void main(String[] args) {
		System.out.println("Fabrication des boites de collision d'un compagnon.");
		System.out.printf("%d parties, %d compagnons, %d ticks/s, %d cotes%n%n",
				PARTIES, COMPAGNONS, TICKS_PAR_SECONDE, COTES);

		for (int i = 0; i < CHAUFFE; i++) {
			avant(i * 0.7F);
			apres(i * 0.7F);
		}

		double nsAvant = mesurer(true);
		double nsApres = mesurer(false);

		afficher("avant (sin/cos par partie)", nsAvant);
		afficher("apres (sin/cos une fois) ", nsApres);
		System.out.printf("%ngain : %.0f %%%n", 100.0 * (nsAvant - nsApres) / nsAvant);

		double msParTick = nsApres * COMPAGNONS * COTES / 1_000_000.0;
		System.out.printf("%nCout par tick pour %d compagnons, les deux cotes : %.4f ms%n",
				COMPAGNONS, msParTick);
		System.out.printf("Un tick dure 50 ms : cela en represente %.3f %%.%n",
				100.0 * msParTick / 50.0);
		System.out.println();
		System.out.println("A LIRE AVEC PRUDENCE : ceci ne mesure que le calcul des boites.");
		System.out.println("La requete au monde — level().getEntities(...) — n'est PAS mesuree ;");
		System.out.println("elle demande un serveur en marche. Si un jour le jeu rame avec");
		System.out.println("beaucoup de compagnons, c'est elle qu'il faudra regarder, pas ceci.");
		System.out.println();
		System.out.println("(garde-fou : " + gardeFou + ")");
	}

	private static double mesurer(boolean ancienne) {
		long debut = System.nanoTime();
		for (int i = 0; i < MESURES; i++) {
			float orientation = i * 0.7F;
			gardeFou += ancienne ? avant(orientation) : apres(orientation);
		}
		return (System.nanoTime() - debut) / (double) MESURES;
	}

	/** La version d'origine : trigonometrie refaite pour chaque partie. */
	private static double avant(float orientation) {
		double total = 0;
		for (double[] partie : DECALAGES) {
			double angle = Math.toRadians(orientation);
			double sin = Math.sin(angle);
			double cos = Math.cos(angle);
			total += (-cos * partie[0]) + (-sin * partie[2])
					+ (-sin * partie[0]) + (cos * partie[2]) + partie[1];
		}
		return total;
	}

	/** La version corrigee : une seule fois pour toutes les parties. */
	private static double apres(float orientation) {
		double angle = Math.toRadians(orientation);
		double sin = Math.sin(angle);
		double cos = Math.cos(angle);

		double total = 0;
		for (double[] partie : DECALAGES) {
			total += (-cos * partie[0]) + (-sin * partie[2])
					+ (-sin * partie[0]) + (cos * partie[2]) + partie[1];
		}
		return total;
	}

	private static void afficher(String quoi, double ns) {
		System.out.printf("%s : %7.1f ns par compagnon et par tick%n", quoi, ns);
	}
}
