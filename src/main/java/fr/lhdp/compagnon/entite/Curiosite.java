package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.mission.Compteurs;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Ce qui attire l'attention d'un compagnon.
 *
 * <p>Un compagnon qui ne reagit qu'aux gens reste une decoration. Celui-ci
 * remarque ce qu'on <b>fait</b> : poser un bloc, en casser un. Il vient
 * regarder.
 *
 * <p>On n'ecoute que deux evenements, et on ne cherche des compagnons que
 * lorsqu'ils se produisent. Un joueur qui construit vite ne coute donc qu'une
 * recherche par bloc, dans un petit rayon.
 */
public final class Curiosite {

	/** Rayon dans lequel un compagnon peut remarquer la chose. Valeur inventee. */
	private static final double PORTEE = 8.0D;

	private Curiosite() {
	}

	public static void enregistrer() {
		PlayerBlockBreakEvents.AFTER.register((niveau, joueur, position, etat, bloc) ->
				signaler(niveau, joueur, position, Compteurs.MINE));

		UseBlockCallback.EVENT.register((joueur, niveau, main, coup) -> {
			// ON REMARQUE TOUT, MAIS ON NE COMPTE QUE CE QUI SE POSE.
			//
			// Cet evenement se declenche pour n'importe quel clic droit sur un
			// bloc : ouvrir un coffre, pousser un bouton, franchir une porte. Sa
			// curiosite s'y interesse — c'est bien qu'il regarde quand on ouvre un
			// coffre. Mais une mission « pose quarante blocs devant lui » se
			// serait remplie en ouvrant quarante fois le meme coffre.
			//
			// On ne compte donc que si la main tient bien un bloc.
			// LA MAIN PRINCIPALE SEULEMENT.
			//
			// Le jeu essaie les deux mains l une apres l autre : quand la premiere
			// ne fait rien, un second paquet part pour la seconde. Un seul clic
			// droit declenchait donc DEUX recherches d entites, et pouvait compter
			// deux blocs poses pour un.
			if (main != net.minecraft.world.InteractionHand.MAIN_HAND) {
				return InteractionResult.PASS;
			}
			String compteur = joueur.getItemInHand(main).getItem()
				instanceof net.minecraft.world.item.BlockItem ? Compteurs.POSE : null;
			signaler(niveau, joueur, coup.getBlockPos(), compteur);
			return InteractionResult.PASS;
		});
	}

	/**
	 * Quelque chose vient de se passer la.
	 *
	 * @param compteur ce que ca fait grandir chez les betes qui l'ont vu, ou
	 *                 {@code null} pour qu'elles le remarquent sans le compter.
	 *                 Les missions de travail n'ont ainsi aucun code de suivi a
	 *                 elles : elles regardent simplement ce compteur monter.
	 */
	private static void signaler(Level niveau, Player joueur, BlockPos position,
			String compteur) {

		if (niveau.isClientSide()) {
			return;
		}

		// LE CHEMIN LE PLUS FREQUENT DU MOD, ET LE SEUL QU'ON N'AVAIT JAMAIS
		// MESURE.
		//
		// Tous les autres couts grandissent avec le nombre de compagnons
		// charges. Celui-ci grandit avec le nombre de JOUEURS qui creusent :
		// une recherche d'entites par bloc casse, et une par clic droit sur un
		// bloc. Cinq par seconde pour un joueur qui mine, et personne n'a
		// jamais su ce que ca donnait a mille.
		//
		// Le chronometre eteint, ces deux lignes coutent la lecture d'un
		// booleen. Voir /compagnon perf.
		if (!Chrono.enMarche()) {
			regarder(niveau, joueur, position, compteur);
			return;
		}
		long avant = System.nanoTime();
		regarder(niveau, joueur, position, compteur);
		Chrono.CURIOSITE.ajouter(System.nanoTime() - avant);
	}

	/**
	 * Un passage, pour le banc d'essai, et rien d'autre.
	 *
	 * <p>Exactement le meme travail qu'un vrai bloc casse : meme recherche,
	 * meme rayon, meme filtre. Mesurer autre chose que le vrai code donnerait
	 * un chiffre rassurant et faux.
	 *
	 * <p><b>Sans compteur</b>, en revanche : une mesure ne doit rien changer a
	 * ce qui est ecrit dans les fiches. Dix mille passages qui rempliraient
	 * dix mille fois « pose un bloc » abimeraient les missions de tout le
	 * monde pour connaitre une microseconde.
	 */
	public static void mesurerUnPassage(Level niveau, Player joueur, BlockPos ou) {
		regarder(niveau, joueur, ou, null);
	}

	/** Le travail lui-meme : qui etait la, et est-ce que ca l'interesse. */
	private static void regarder(Level niveau, Player joueur, BlockPos position,
			String compteur) {

		Vec3 point = Vec3.atCenterOf(position);
		AABB zone = new AABB(point, point).inflate(PORTEE);

		List<CompagnonEntity> autour =
				niveau.getEntitiesOfClass(CompagnonEntity.class, zone);

		for (CompagnonEntity compagnon : autour) {
			// Seuls le maitre et les gens qu'il connait l'interessent. Un inconnu
			// qui creuse a cote de lui ne le regarde pas.
			if (compagnon.connait(joueur.getUUID())) {
				compagnon.remarquer(point);
				if (compteur != null) {
					Compteurs.compter(compagnon, compteur);
				}
			}
		}
	}
}
