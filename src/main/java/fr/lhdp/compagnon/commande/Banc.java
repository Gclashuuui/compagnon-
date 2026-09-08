package fr.lhdp.compagnon.commande;

import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.entite.CompagnonEntity;
import fr.lhdp.compagnon.entite.Curiosite;
import fr.lhdp.compagnon.espece.Espece;
import fr.lhdp.compagnon.espece.Especes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Le banc d'essai : mesurer une foule quand on est tout seul.
 *
 * <h2>Le probleme qu'il resout</h2>
 *
 * <p>Ce mod est ecrit pour un serveur de mille joueurs et de mille compagnons.
 * Il est teste par une personne. Tous les chiffres de performance qu'on pouvait
 * donner jusqu'ici etaient donc des extrapolations a partir d'un compagnon —
 * c'est-a-dire des suppositions polies.
 *
 * <p>Et on ne peut pas remplir un serveur pour verifier : il faudrait des gens,
 * disponibles en meme temps, a chaque fois qu'on change une ligne.
 *
 * <h2>Ce qu'il fait</h2>
 *
 * <p>Il fabrique la foule. {@code /compagnon banc 200} pose deux cents
 * compagnons autour de toi, qui tiquent pour de vrai — meme code, memes buts,
 * meme collision. {@code /compagnon perf} mesure alors une vraie charge.
 *
 * <p>Et {@code /compagnon banc curiosite} s'occupe de l'autre moitie du
 * probleme : le seul cout du mod qui ne depend pas du nombre de compagnons mais
 * du nombre de <b>joueurs qui creusent</b>. Il ne se simule pas avec des
 * entites, alors on le mesure directement, et on extrapole a partir d'un chiffre
 * reel au lieu d'une intuition.
 *
 * <h2>Ils ne laissent aucune trace</h2>
 *
 * <p>Les compagnons du banc n'ont <b>pas de fiche</b> : ils n'existent pas dans
 * la sauvegarde, ils ne comptent pour personne, ils ne montent pas de niveau.
 * Et {@link CompagnonEntity#shouldBeSaved()} les refuse a l'ecriture du monde —
 * un redemarrage les efface, meme si on oublie de faire le menage.
 */
public final class Banc {

	/** Au-dela, on fabrique un probleme au lieu de le mesurer. */
	public static final int MAXIMUM = 500;

	/** Passages maximum pour la mesure de la curiosite. */
	public static final int PASSAGES_MAXIMUM = 50_000;

	/** L'espace entre deux betes du banc, en blocs. */
	private static final double ECART = 2.0D;

	/**
	 * Combien de blocs un joueur casse ou pose par seconde, quand il travaille.
	 *
	 * <p>Valeur d'observation : une pioche en fer sur de la pierre tourne autour
	 * de cinq. C'est l'hypothese de l'extrapolation, et elle est ecrite ici pour
	 * qu'on puisse la contester.
	 */
	private static final double BLOCS_PAR_SECONDE = 5.0D;

	private static final double TICKS_PAR_SECONDE = 20.0D;

	/** Les tailles de serveur pour lesquelles on donne le resultat. */
	private static final int[] TAILLES = {100, 500, 1000};

	private Banc() {
	}

	// --- La foule -------------------------------------------------------------

	/**
	 * Pose {@code combien} compagnons de test autour du joueur.
	 *
	 * <p>En carre serre et non en cercle large : on veut qu'ils soient tous
	 * charges, tous dans la portee de simulation, et tous assez proches les uns
	 * des autres pour que la collision en plusieurs morceaux ait vraiment du
	 * travail. C'est le pire cas, et c'est celui qu'on veut connaitre.
	 */
	public static List<String> poser(ServerPlayer joueur, int combien) {
		int voulu = Math.max(1, Math.min(MAXIMUM, combien));
		ServerLevel niveau = joueur.serverLevel();

		List<Espece> especes = Especes.toutes();
		if (especes.isEmpty()) {
			return List.of("Aucune espece chargee : rien a poser.");
		}

		int cote = (int) Math.ceil(Math.sqrt(voulu));
		double depart = -(cote - 1) * ECART / 2.0D;
		int poses = 0;

		for (int i = 0; i < voulu; i++) {
			Espece espece = especes.get(i % especes.size());
			double x = joueur.getX() + depart + (i % cote) * ECART;
			double z = joueur.getZ() + depart + (i / cote) * ECART;

			CompagnonEntity bete = Compagnon.COMPAGNON.create(niveau);
			if (bete == null) {
				continue;
			}
			// Sur le sol, et non a la hauteur du joueur : pose en l'air, une bete
			// passe sa vie a tomber, et on mesurerait une chute.
			BlockPos sol = niveau.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					BlockPos.containing(x, joueur.getY(), z));

			bete.moveTo(x, sol.getY(), z, joueur.getRandom().nextFloat() * 360.0F, 0.0F);
			bete.setEspece(espece.nom());
			bete.setVariante(espece.varianteParDefaut());
			bete.marquerBanc();
			// Sans ca, le jeu les fait disparaitre en pleine mesure et le chiffre
			// ne veut plus rien dire.
			bete.setPersistenceRequired();
			niveau.addFreshEntity(bete);
			poses++;
		}

		return List.of(
			poses + " compagnons de test poses autour de toi, en carre de "
				+ cote + " sur " + cote + ".",
			"Ils n'ont pas de fiche et ne sont pas sauvegardes : un redemarrage les efface.",
			"",
			"Maintenant :  /compagnon perf   ->  attends une minute  ->  /compagnon perf",
			"Puis :        /compagnon banc net");
	}

	/** Enleve tous les compagnons de test, dans toutes les dimensions. */
	public static int nettoyer(MinecraftServer serveur) {
		int enleves = 0;
		for (ServerLevel niveau : serveur.getAllLevels()) {
			List<CompagnonEntity> duBanc = new ArrayList<>(
				niveau.getEntities(Compagnon.COMPAGNON, CompagnonEntity::estDuBanc));
			for (CompagnonEntity bete : duBanc) {
				bete.discard();
				enleves++;
			}
		}
		return enleves;
	}

	/** Combien de compagnons tiquent en ce moment, toutes dimensions confondues. */
	public static int charges(MinecraftServer serveur) {
		int total = 0;
		for (ServerLevel niveau : serveur.getAllLevels()) {
			total += niveau.getEntities(Compagnon.COMPAGNON, bete -> true).size();
		}
		return total;
	}

	// --- La curiosite ---------------------------------------------------------

	/**
	 * Mesure le cout d'un bloc casse, et dit ce que ca donne a mille joueurs.
	 *
	 * <p>C'est le seul cout du mod qui grandit avec le nombre de <b>joueurs</b>
	 * et non de compagnons : une recherche d'entites a chaque bloc casse et a
	 * chaque clic droit sur un bloc.
	 *
	 * <p><b>A lancer pendant que le banc est pose</b> : la recherche coute en
	 * fonction de ce qu'elle trouve, et une mesure dans un desert ne dit rien de
	 * ce que ca coutera dans une cour d'ecole.
	 */
	public static List<String> curiosite(ServerPlayer joueur, int combien) {
		int passages = Math.max(1, Math.min(PASSAGES_MAXIMUM, combien));
		BlockPos ou = joueur.blockPosition();

		// Un tour a vide d'abord : la premiere execution d'un code Java est
		// toujours la plus lente, et la compter fausserait tout vers le haut.
		for (int i = 0; i < Math.min(200, passages); i++) {
			Curiosite.mesurerUnPassage(joueur.serverLevel(), joueur, ou);
		}

		long avant = System.nanoTime();
		for (int i = 0; i < passages; i++) {
			Curiosite.mesurerUnPassage(joueur.serverLevel(), joueur, ou);
		}
		double nanosParPassage = (System.nanoTime() - avant) / (double) passages;

		List<String> lignes = new ArrayList<>();
		lignes.add(passages + " passages mesures, autour de toi, avec "
			+ charges(joueur.server) + " compagnons charges.");
		lignes.add(String.format("  %.1f microsecondes par bloc casse ou pose.",
			nanosParPassage / 1000.0D));
		lignes.add("");
		lignes.add("A " + (int) BLOCS_PAR_SECONDE + " blocs par seconde et par joueur :");

		for (int joueurs : TAILLES) {
			// Chaque joueur declenche BLOCS_PAR_SECONDE passages par seconde,
			// repartis sur vingt ticks.
			double parTick = joueurs * BLOCS_PAR_SECONDE / TICKS_PAR_SECONDE;
			double ms = parTick * nanosParPassage / 1_000_000.0D;
			lignes.add(String.format("  %4d joueurs : %.3f ms par tick  (%.2f %% des 50 ms)",
				joueurs, ms, 100.0D * ms / 50.0D));
		}

		double msMille = 1000 * BLOCS_PAR_SECONDE / TICKS_PAR_SECONDE
			* nanosParPassage / 1_000_000.0D;
		lignes.add("");
		lignes.add(msMille > 5.0D
			? "A mille, c'est trop : il faudra filtrer avant la recherche."
			: msMille > 1.0D
				? "A mille, c'est visible mais tenable. A surveiller."
				: "A mille, c'est negligeable. Ne pas optimiser.");
		return lignes;
	}
}
