package fr.lhdp.compagnon.progression;

import fr.lhdp.compagnon.espece.Especes;
import fr.lhdp.compagnon.fiche.FicheCompagnon;
import fr.lhdp.compagnon.reglage.Reglages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Le moment ou il passe un niveau.
 *
 * <h2>Ce qui manquait</h2>
 *
 * <p>Un compagnon montait de niveau et personne ne le lui disait. Les gestes
 * s'ouvraient dans la roue en silence : il fallait ouvrir la roue, se souvenir
 * de ce qu'il y avait avant, et remarquer une case de plus. Autant dire que
 * personne ne le remarquait.
 *
 * <p>Debloquer un geste doit etre un evenement. On reprend donc les deux outils
 * que le jeu utilise pour ses propres moments : <b>le grand texte au milieu de
 * l'ecran</b> et <b>le son de montee de niveau</b>. Ce sont ceux que le joueur
 * associe deja a « il vient de se passer quelque chose de bien ».
 *
 * <h2>Pourquoi c'est verifie une fois par minute</h2>
 *
 * <p>L'experience se gagne a une demi-douzaine d'endroits — un repas, un soin,
 * une caresse, une mission, une commande. Chacun aurait pu annoncer sa propre
 * montee, et il aurait fallu y penser a chaque nouvelle source. On compare donc
 * le niveau reel avec le dernier niveau annonce, une fois par minute : aucune
 * source ne peut etre oubliee, et il n'y a qu'un seul endroit a lire.
 *
 * <p>Le prix est un delai d'une minute au plus. Pour un mod ou une bete met des
 * jours a grandir, c'est exactement rien.
 */
public final class Montee {

	private Montee() {
	}

	/** Le dernier niveau qu'on lui a annonce. Un compteur interne : il a un point. */
	private static final String CLE_ANNONCE = "t.niveau";

	/** Combien de gestes on nomme au plus dans le sous-titre. */
	private static final int NOMMES = 2;

	private static final int APPARITION = 10;
	private static final int DUREE = 60;
	private static final int DISPARITION = 20;

	/**
	 * Regarde s'il a monte, et le fait savoir. A appeler une fois par minute.
	 *
	 * @return vrai si la fiche a change
	 */
	public static boolean regarder(FicheCompagnon fiche, ServerPlayer proprietaire,
			Progression table) {

		int maintenant = fiche.niveau(table);
		int annonce = fiche.compteur(CLE_ANNONCE);

		// PREMIER PASSAGE : on enregistre sans rien dire. Sinon toutes les betes
		// deja existantes annonceraient leur niveau a la premiere minute de jeu,
		// et le joueur recevrait dix titres d'affilee pour rien.
		if (annonce == 0) {
			fiche.poserCompteur(CLE_ANNONCE, maintenant);
			return true;
		}
		if (maintenant <= annonce) {
			return false;
		}

		List<String> ouverts = cequiSOuvre(fiche, table, annonce, maintenant);
		int points = pointsGagnes(annonce, maintenant, Reglages.pointsTousLesNiveaux());
		boolean monte = monteDebloquee(fiche, annonce, maintenant);
		fiche.poserCompteur(CLE_ANNONCE, maintenant);
		fiche.marquer("niveau_" + maintenant, System.currentTimeMillis());
		annoncer(proprietaire, fiche, maintenant, ouverts, points, monte);
		faireBriller(proprietaire, fiche);
		return true;
	}

	/** Combien de paliers de competence ont ete franchis entre deux annonces. */
	static int pointsGagnes(int avant, int apres, int tousLesNiveaux) {
		if (tousLesNiveaux <= 0 || apres <= avant) {
			return 0;
		}
		return Math.max(0, apres / tousLesNiveaux - avant / tousLesNiveaux);
	}

	/** La monte vient-elle precisement d'etre ouverte pour cette espece ? */
	private static boolean monteDebloquee(FicheCompagnon fiche, int avant, int apres) {
		fr.lhdp.compagnon.espece.Espece espece = Especes.get(fiche.espece());
		if (espece == null || !espece.seMonte()) {
			return false;
		}
		return avant < espece.monterAuNiveau() && apres >= espece.monterAuNiveau();
	}

	/**
	 * Les gestes que cette bete-la vient d'obtenir.
	 *
	 * <p>Filtres par espece : la table decrit les animations de tout le monde, et
	 * annoncer a une mouette qu'elle a debloque un souffle de dragon serait pire
	 * que de ne rien annoncer du tout.
	 */
	private static List<String> cequiSOuvre(FicheCompagnon fiche, Progression table,
			int avant, int apres) {

		Set<String> hier = table.debloquesJusqua(avant);
		List<String> neufs = new ArrayList<>(2);
		for (String animation : table.debloquesJusqua(apres)) {
			if (!hier.contains(animation) && Especes.concerne(animation, fiche.espece())) {
				neufs.add(animation);
			}
		}
		return neufs;
	}

	/**
	 * Et la bete elle-meme scintille, si elle est la pour le voir.
	 *
	 * <p>Le titre dit ce qui se passe, les particules disent OU. Sans elles, on
	 * lit un texte au milieu de l ecran sans savoir laquelle de ses betes vient
	 * de monter — ce qui compte quand on en sort trois a la fois.
	 */
	private static void faireBriller(ServerPlayer joueur, FicheCompagnon fiche) {
		net.minecraft.server.level.ServerLevel niveau =
			joueur.server.getLevel(fiche.dimension());
		if (niveau != null && niveau.getEntity(fiche.id())
				instanceof fr.lhdp.compagnon.entite.CompagnonEntity compagnon) {
			fr.lhdp.compagnon.entite.Etincelles.montee(compagnon);
			compagnon.reagirMonteeDeNiveau();
		}
	}

	/**
	 * Le grand texte, le sous-titre et le son.
	 *
	 * <p>Le titre dit le niveau, le sous-titre dit ce qu'on y gagne : gestes,
	 * points de competence et monte. Un niveau sans recompense mecanique reste une
	 * nouvelle etape de leur histoire, jamais une annonce negative.
	 */
	private static void annoncer(ServerPlayer joueur, FicheCompagnon fiche, int niveau,
			List<String> ouverts, int points, boolean monte) {

		joueur.connection.send(new ClientboundSetTitlesAnimationPacket(
				APPARITION, DUREE, DISPARITION));

		// UNE ETOILE DEVANT LE TITRE.
		//
		// Elle vient de la police du mod : c est une lettre, elle se met donc dans
		// un titre comme dans une phrase, et elle prend l or du titre sans qu on
		// ait a la teinter.
		joueur.connection.send(new ClientboundSetTitleTextPacket(
				fr.lhdp.compagnon.Icones.devant(fr.lhdp.compagnon.Icones.ETOILE,
						Component.translatable("montee.compagnon.titre", fiche.nom(), niveau))
						.withStyle(ChatFormatting.GOLD)));

		joueur.connection.send(new ClientboundSetSubtitleTextPacket(
				sousTitre(ouverts, points, monte)));

		// Le son de montee de niveau du jeu, et pas un autre : c'est celui que le
		// joueur associe deja a une bonne nouvelle, sans avoir rien a apprendre.
		joueur.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.2F);
	}

	private static Component sousTitre(List<String> ouverts, int points, boolean monte) {
		List<Component> recompenses = new ArrayList<>();
		if (!ouverts.isEmpty()) {
			recompenses.add(gestes(ouverts));
		}
		if (points == 1) {
			recompenses.add(Component.translatable("montee.compagnon.point_competence"));
		} else if (points > 1) {
			recompenses.add(Component.translatable(
					"montee.compagnon.points_competence", points));
		}
		if (monte) {
			recompenses.add(Component.translatable("montee.compagnon.monte"));
		}
		if (recompenses.isEmpty()) {
			return Component.translatable("montee.compagnon.nouvelle_etape")
					.withStyle(ChatFormatting.YELLOW);
		}
		net.minecraft.network.chat.MutableComponent resultat = Component.empty();
		for (int i = 0; i < recompenses.size(); i++) {
			if (i > 0) {
				resultat.append(Component.literal("  •  "));
			}
			resultat.append(recompenses.get(i));
		}
		return resultat.withStyle(ChatFormatting.YELLOW);
	}

	/** Le morceau du sous-titre qui nomme les gestes ouverts. */
	private static Component gestes(List<String> ouverts) {
		if (ouverts.size() <= NOMMES) {
			List<Component> noms = new ArrayList<>(ouverts.size());
			for (String animation : ouverts) {
				noms.add(nomLisible(animation));
			}
			Component liste = noms.size() == 1
					? noms.get(0)
					: Component.translatable("montee.compagnon.et", noms.get(0), noms.get(1));
			return Component.translatable("montee.compagnon.nouveau_geste", liste)
					.withStyle(ChatFormatting.YELLOW);
		}
		return Component.translatable("montee.compagnon.nouveaux_gestes", ouverts.size())
				.withStyle(ChatFormatting.YELLOW);
	}

	/**
	 * Le nom lisible d'une animation, exactement comme la roue l'ecrit.
	 *
	 * <p>La meme regle que {@code EcranCompagnon.etiquette} : une traduction si
	 * elle existe, sinon le dernier morceau du nom technique. Les deux doivent
	 * dire la meme chose — sinon le titre annonce un geste sous un nom, et la
	 * roue le montre sous un autre.
	 *
	 * <p>C'est une duplication assumee : l'ecran vit cote client, ce texte-ci
	 * doit etre compose cote serveur, et partager huit lignes entre les deux
	 * couterait plus cher que de les ecrire deux fois.
	 */
	private static Component nomLisible(String animation) {
		String cle = "roue.compagnon.action." + animation;
		if (net.minecraft.locale.Language.getInstance().has(cle)) {
			return Component.translatable(cle);
		}
		int point = animation.lastIndexOf('.');
		String court = point >= 0 ? animation.substring(point + 1) : animation;
		return Component.literal(court.replace('_', ' '));
	}
}
