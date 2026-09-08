package fr.lhdp.compagnon.progression;

import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.Icones;
import fr.lhdp.compagnon.entite.CompagnonEntity;
import fr.lhdp.compagnon.entite.Etincelles;
import fr.lhdp.compagnon.entite.Sons;
import fr.lhdp.compagnon.espece.Espece;
import fr.lhdp.compagnon.espece.Especes;
import fr.lhdp.compagnon.fiche.FicheCompagnon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Le jour ou il grandit.
 *
 * <h2>Pourquoi cette chose-la et pas une autre</h2>
 *
 * <p>Le mod a huit especes en double : un bebe et un adulte, chacun avec son
 * modele, ses animations et ses textures. Ils etaient deux listes separees —
 * l'equipe distribuait l'un ou l'autre, et l'adulte n'etait qu'un gros modele de
 * plus.
 *
 * <p>Relies, ils deviennent une histoire. Le bebe qu'on a recu dans un oeuf, a
 * qui on a donne un nom, qu'on a nourri pendant des semaines, <b>devient</b> la
 * bete de six blocs de haut qui marche a cote de son maitre dans la cour. Aucune
 * fonction ajoutee ne produit ca : c'est du temps deja passe qui prend un sens
 * retroactivement.
 *
 * <h2>Rien ne se perd</h2>
 *
 * <p>C'est la meme fiche : le meme identifiant, le meme nom, le meme niveau, les
 * memes souvenirs, les memes gens reconnus, le meme caractere, la meme manie et
 * le meme defaut. Seule l'espece change. Un compagnon qui grandit ne redevient
 * pas un inconnu.
 *
 * <h2>Une seule fois, et jamais en arriere</h2>
 *
 * <p>L'adulte ne declare pas de {@code devient} : la chaine s'arrete d'elle-meme,
 * sans compteur ni marqueur a maintenir. Et il n'existe aucun chemin de retour —
 * ce serait la seule chose du mod qu'on pourrait annuler, et grandir ne s'annule
 * pas.
 */
public final class Croissance {

	private Croissance() {
	}

	/** L'evenement s'inscrit dans son livre, comme un premier orage. */
	private static final String SOUVENIR = "grandi";

	private static final int APPARITION = 10;
	private static final int DUREE = 80;
	private static final int DISPARITION = 20;

	/**
	 * Regarde s'il est temps, et le fait grandir.
	 *
	 * <p>Appelee juste apres la montee de niveau, une fois par minute. Le delai
	 * est sans importance : ce qui se joue ici met des semaines a arriver.
	 *
	 * @return vrai si la fiche a change
	 */
	public static boolean regarder(FicheCompagnon fiche, ServerPlayer proprietaire,
			Progression table) {

		Espece petit = Especes.get(fiche.espece());
		if (petit == null || petit.devient().isEmpty() || petit.devientAuNiveau() <= 0) {
			return false;
		}
		if (fiche.niveau(table) < petit.devientAuNiveau()) {
			return false;
		}
		Espece grand = Especes.get(petit.devient());
		if (grand == null) {
			// L'espece adulte a disparu du jeu de donnees. On ne touche a rien :
			// une fiche qui pointerait vers une espece inconnue afficherait un
			// damier a la place de la bete.
			Compagnon.LOG.warn("Croissance : \"{}\" doit devenir \"{}\", qui n'existe pas.",
					petit.nom(), petit.devient());
			return false;
		}

		String couleur = couleurCorrespondante(petit, grand, fiche.variante());
		fiche.setEspece(grand.nom());
		fiche.setVariante(couleur);
		fiche.marquer(SOUVENIR, System.currentTimeMillis());

		refaireLaBete(proprietaire, fiche);
		annoncer(proprietaire, fiche, petit, grand);
		return true;
	}

	/**
	 * La meme couleur si l'adulte l'a, la sienne par defaut sinon.
	 *
	 * <p>Les deux ages ne portent pas toujours la meme palette : le bebe cindervane
	 * a deux robes, l'adulte en a six. Une femelle reste une femelle ; une couleur
	 * que l'adulte ignore ramene a sa robe ordinaire plutot qu'a un damier.
	 */
	private static String couleurCorrespondante(Espece petit, Espece grand, String actuelle) {
		if (grand.variantes().containsKey(actuelle)) {
			return actuelle;
		}
		Compagnon.LOG.info("Croissance : {} n'a pas la couleur \"{}\" de {}, on prend \"{}\".",
				grand.nom(), actuelle, petit.nom(), grand.varianteParDefaut());
		return grand.varianteParDefaut();
	}

	/**
	 * On jette l'entite : elle porte encore l'ancien squelette.
	 *
	 * <p>L'espece d'un compagnon est fixee a sa naissance et voyage jusqu'au client
	 * pour choisir le modele. La changer sur une bete deja posee ne rechargerait
	 * ni sa geometrie ni sa boite de collision. {@code Apparition} la remettra a
	 * la minute suivante, telle qu'elle est maintenant.
	 */
	private static void refaireLaBete(ServerPlayer proprietaire, FicheCompagnon fiche) {
		ServerLevel niveau = proprietaire.server.getLevel(fiche.dimension());
		if (niveau == null) {
			return;
		}
		Entity presente = niveau.getEntity(fiche.id());
		if (presente instanceof CompagnonEntity compagnon) {
			// Le bouquet part AVANT qu'elle disparaisse, sinon il n'y a personne
			// pour le porter.
			Etincelles.montee(compagnon);
			Sons.jouer(compagnon, Sons.CONTENT, 1.0F);
			compagnon.discard();
		}
	}

	/** Le grand texte au milieu de l'ecran. C'est un des rares moments qui le merite. */
	private static void annoncer(ServerPlayer joueur, FicheCompagnon fiche,
			Espece petit, Espece grand) {

		joueur.connection.send(new ClientboundSetTitlesAnimationPacket(
				APPARITION, DUREE, DISPARITION));
		joueur.connection.send(new ClientboundSetTitleTextPacket(
				Component.literal(Icones.de(Icones.ETOILE) + " " + fiche.nom() + " a grandi")
						.withStyle(ChatFormatting.GOLD)));

		joueur.sendSystemMessage(Icones.devant(Icones.COURONNE,
				Component.literal(fiche.nom() + " n'est plus un petit. "
						+ Especes.titre(petit.nom()) + " est devenu "
						+ Especes.titre(grand.nom()) + ".")
						.withStyle(ChatFormatting.GOLD)));
	}
}
