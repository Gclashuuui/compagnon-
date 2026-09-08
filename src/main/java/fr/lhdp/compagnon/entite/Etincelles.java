package fr.lhdp.compagnon.entite;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Les particules des compagnons.
 *
 * <h2>Le mod n'en emettait aucune</h2>
 *
 * <p>Pas une. Une bete qu'on caresse et qui ne produit rien, un niveau qui monte
 * sans que rien ne scintille : tout se passait dans les chiffres. Or les
 * particules sont le langage que Minecraft utilise pour dire « il vient de se
 * passer quelque chose », et tout joueur le lit sans qu'on le lui apprenne.
 *
 * <h2>La regle qu'on se donne</h2>
 *
 * <p><b>Une particule marque un evenement, jamais un etat.</b> Un compagnon
 * heureux n'emet rien en permanence — il emet quelque chose au moment ou il
 * devient heureux. Des particules continues deviennent du bruit visuel en une
 * heure, et on finit par installer un autre mod pour les enlever.
 *
 * <p>Corollaire : chacune de ces methodes se declenche sur un evenement precis,
 * et aucune ne tourne dans un tick.
 */
public final class Etincelles {

	private Etincelles() {
	}

	/**
	 * Des coeurs, comme quand on apprivoise une bete.
	 *
	 * <p>C'est le geste du jeu lui-meme pour dire « ca lui a fait plaisir ». On
	 * n'en invente pas un autre : celui-la est deja compris de tout le monde.
	 */
	public static void coeurs(CompagnonEntity compagnon, int combien) {
		semer(compagnon, ParticleTypes.HEART, combien, 0.6D);
	}

	/** Les etoiles vertes du villageois content : un progres, une reussite. */
	public static void progres(CompagnonEntity compagnon, int combien) {
		semer(compagnon, ParticleTypes.HAPPY_VILLAGER, combien, 0.7D);
	}

	/**
	 * La montee de niveau : des etoiles, plus haut et plus larges.
	 *
	 * <p>Le seul endroit ou l'on se permet d'etre voyant. Un niveau se gagne en
	 * quelques jours de jeu — il a le droit de se voir de loin.
	 */
	public static void montee(CompagnonEntity compagnon) {
		haloDeMontee(compagnon);
		semer(compagnon, ParticleTypes.HAPPY_VILLAGER, 14, 0.8D);
	}

	/**
	 * Deux rubans de lumiere qui s'enroulent autour de la bete et montent.
	 *
	 * <p>Une bouffee aleatoire marque bien l'evenement, mais ne ressemble pas a
	 * une petite animation. Ici les particules naissent deja en spirale et leur
	 * vitesse les fait monter toutes seules : aucun compteur ne reste dans
	 * l'entite et aucun travail ne se repete dans son {@code tick()}.
	 *
	 * <p>Le rayon et la hauteur viennent de la boite de la creature. Le meme effet
	 * entoure donc le dragonnet sans le cacher et reste lisible autour des grandes
	 * especes.
	 */
	private static void haloDeMontee(CompagnonEntity compagnon) {
		if (!(compagnon.level() instanceof ServerLevel niveau)) {
			return;
		}
		double rayon = Math.max(0.35D, compagnon.getBbWidth() * 0.7D);
		double hauteur = Math.max(0.8D, compagnon.getBbHeight());
		int points = 18;

		for (int ruban = 0; ruban < 2; ruban++) {
			for (int i = 0; i < points; i++) {
				double avance = i / (double) (points - 1);
				double angle = avance * Math.PI * 4.0D + ruban * Math.PI;
				double x = compagnon.getX() + Math.cos(angle) * rayon;
				double y = compagnon.getY() + hauteur * (0.12D + avance * 0.82D);
				double z = compagnon.getZ() + Math.sin(angle) * rayon;

				// Avec zero en quantite, Minecraft cree une particule et interprete
				// les trois ecarts comme sa vitesse exacte. Elle monte donc au lieu
				// d'exploser au hasard autour de son point de depart.
				niveau.sendParticles(ParticleTypes.END_ROD, x, y, z,
						0, 0.0D, 0.025D, 0.0D, 1.0D);
			}
		}
	}

	/** Un nuage sous les pattes : il vient de quitter le sol. */
	public static void envol(CompagnonEntity compagnon) {
		poussiere(compagnon, 8);
	}

	/**
	 * Il vient de se poser.
	 *
	 * <p>Le decollage se voyait et s'entendait ; l'arrivee, non. Une bete qui
	 * descend de trente blocs et touche le sol <b>sans rien</b> a l'air de
	 * traverser le decor. Un peu moins de poussiere qu'au depart : on se pose
	 * plus doucement qu'on ne s'arrache.
	 */
	public static void atterrissage(CompagnonEntity compagnon) {
		poussiere(compagnon, 6);
	}

	/** De la poussiere sous les pattes, pas au niveau du corps. */
	private static void poussiere(CompagnonEntity compagnon, int combien) {
		if (!(compagnon.level() instanceof ServerLevel niveau)) {
			return;
		}
		// Au niveau des pattes, pas du corps : c'est le sol qu'on chasse.
		niveau.sendParticles(ParticleTypes.CLOUD,
			compagnon.getX(), compagnon.getY() + 0.1D, compagnon.getZ(),
			combien, compagnon.getBbWidth() * 0.4D, 0.05D,
			compagnon.getBbWidth() * 0.4D, 0.02D);
	}

	/**
	 * La bouffee de celui qui arrive, ou de celui qui rentre.
	 *
	 * <p>Faire sortir sa bete etait le geste le plus magique du mod, et le plus
	 * decevant a regarder : elle <b>apparaissait</b>, comme un bloc qu'on pose.
	 * Rien ne disait qu'il venait de se passer quelque chose.
	 *
	 * <p>C'est la bouffee du jeu, celle du marchand qui s'en va : personne n'a
	 * a apprendre ce qu'elle veut dire.
	 *
	 * <p><b>Reservee au geste du joueur.</b> La bete qui apparait toute seule
	 * parce qu'on s'en approche, ou qui disparait parce qu'on s'en eloigne, ne
	 * doit surtout rien emettre : ce n'est pas un evenement, c'est de la
	 * cuisine interne, et elle se declencherait dix fois par promenade.
	 */
	public static void bouffee(CompagnonEntity compagnon) {
		semer(compagnon, ParticleTypes.POOF, 12, 0.7D);
	}

	/** Des gouttes : il s'ebroue en sortant de l'eau. */
	public static void gouttes(CompagnonEntity compagnon) {
		semer(compagnon, ParticleTypes.SPLASH, 14, 0.5D);
	}

	/**
	 * Les miettes de ce qu'il est en train de manger.
	 *
	 * <p>Ce sont exactement les particules que le jeu emet quand on mange, et
	 * quand on nourrit une bete : des morceaux de l'objet lui-meme. On voit
	 * donc <b>ce</b> qu'il mange, pas seulement qu'il mange — et une pomme ne
	 * ressemble pas a un poisson.
	 *
	 * <p>Elles partent de sa bouche et retombent : c'est ce qui fait qu'on lit
	 * un repas et non une explosion.
	 */
	public static void miettes(CompagnonEntity compagnon, ItemStack quoi) {
		if (quoi == null || quoi.isEmpty()
				|| !(compagnon.level() instanceof ServerLevel niveau)) {
			return;
		}
		double hauteur = compagnon.getBbHeight();
		niveau.sendParticles(
			new ItemParticleOption(ParticleTypes.ITEM, quoi.copyWithCount(1)),
			compagnon.getX(),
			compagnon.getY() + hauteur * 0.7D,
			compagnon.getZ(),
			10,
			compagnon.getBbWidth() * 0.3D, 0.1D, compagnon.getBbWidth() * 0.3D,
			0.06D);
	}

	/** Une petite fumee : il ne va pas bien, ou il vient de prendre un bobo. */
	public static void soupir(CompagnonEntity compagnon) {
		semer(compagnon, ParticleTypes.SMOKE, 5, 0.4D);
	}

	/**
	 * Repand des particules autour de sa tete.
	 *
	 * <p>Autour de la tete et non du centre : c'est la qu'on regarde une bete, et
	 * une particule emise a hauteur de ventre disparait sous elle.
	 */
	private static void semer(CompagnonEntity compagnon, ParticleOptions quoi, int combien,
			double etalement) {

		if (!(compagnon.level() instanceof ServerLevel niveau)) {
			return;
		}
		double hauteur = compagnon.getBbHeight();
		niveau.sendParticles(quoi,
				compagnon.getX(),
				compagnon.getY() + hauteur * 0.75D,
				compagnon.getZ(),
				combien,
				compagnon.getBbWidth() * etalement,
				hauteur * 0.25D,
				compagnon.getBbWidth() * etalement,
				0.02D);
	}
}
