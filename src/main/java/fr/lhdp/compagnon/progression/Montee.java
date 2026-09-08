package fr.lhdp.compagnon.progression;

import fr.lhdp.compagnon.espece.Especes;
import fr.lhdp.compagnon.fiche.FicheCompagnon;
import fr.lhdp.compagnon.reglage.Reglages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Le moment ou il passe un niveau.
 *
 * <h2>Ce qui manquait</h2>
 *
 * <p>Un compagnon montait de niveau et personne ne le lui disait. L'annonce est
 * maintenant un petit carton dans le coin superieur droit : assez visible pour
 * marquer le moment, assez discret pour ne jamais cacher le jeu.
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
	 * Le petit carton du coin et son tintement.
	 *
	 * <p>Le titre dit le niveau, la seconde ligne dit ce qu'on y gagne : gestes,
	 * points de competence et monte. Un niveau sans recompense mecanique reste une
	 * nouvelle etape de leur histoire, jamais une annonce negative.
	 */
	private static void annoncer(ServerPlayer joueur, FicheCompagnon fiche, int niveau,
			List<String> ouverts, int points, boolean monte) {

		ServerPlayNetworking.send(joueur,
				new fr.lhdp.compagnon.reseau.PaquetMontee(
						fiche.nom(), niveau, ouverts, points, monte));

		// Un tintement court accompagne le carton, sans couvrir le jeu comme le
		// son vanilla de niveau qui etait prevu pour un plein ecran.
		joueur.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP,
				SoundSource.PLAYERS, 0.32F, 1.35F);
	}

}
