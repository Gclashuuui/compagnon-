package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.mission.Compteurs;
import fr.lhdp.compagnon.fiche.FicheCompagnon;
import fr.lhdp.compagnon.fiche.Fiches;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.UUID;

/**
 * Les compagnons finissent par se connaitre.
 *
 * <p>Deux dragonnets qui passent leurs journees dans la meme salle commune se
 * reconnaissent au bout d'un moment, exactement comme un compagnon finit par
 * reconnaitre les amis de son proprietaire. Passe un certain nombre de
 * rencontres, ils vont se voir et jouent ensemble — voir {@link RetrouverGoal}.
 *
 * <h2>Pourquoi on reutilise la meme liste</h2>
 *
 * <p>La fiche porte deja des {@code connaissances} : une table
 * « identifiant → combien de fois ». Elle etait remplie par les joueurs qui
 * caressent ou nourrissent. Les compagnons ont aussi un identifiant, et la
 * question posee est exactement la meme — <b>qui a-t-il assez cotoye pour le
 * reconnaitre ?</b> Une seconde table aurait duplique le code, la sauvegarde et
 * les bogues.
 *
 * <p>Le melange est sans risque : {@code estFamilier} n'est jamais interroge
 * qu'avec l'identifiant d'un joueur, et {@link RetrouverGoal} ne cherche que des
 * compagnons. Personne ne peut confondre les deux.
 *
 * <h2>Le cout</h2>
 *
 * <p>Une recherche par compagnon et par minute, dans une boite de vingt blocs, et
 * seulement pour les compagnons des joueurs <b>connectes</b>. Sur un serveur ou
 * la plupart des proprietaires sont absents, cette classe ne fait rien du tout.
 */
public final class Voisinages {

	/** Jusqu'ou deux compagnons se remarquent, en blocs. Valeur inventee. */
	private static final double PORTEE = 10.0D;

	/**
	 * Au-dela, on arrete de compter.
	 *
	 * <p>Sans ce plafond, deux compagnons poses cote a cote dans une salle commune
	 * accumuleraient des milliers de rencontres, et la sauvegarde grossirait pour
	 * une information qui ne dit plus rien : au-dela de « ils se connaissent », le
	 * chiffre n'apporte rien.
	 */
	private static final int PLAFOND = 60;

	private Voisinages() {
	}

	/**
	 * Compte une rencontre avec chaque compagnon a portee.
	 *
	 * @return vrai si quelque chose a change et merite d'etre sauvegarde
	 */
	public static boolean seRemarquer(MinecraftServer serveur, FicheCompagnon fiche) {
		ServerLevel niveau = serveur.getLevel(fiche.dimension());
		if (niveau == null || !(niveau.getEntity(fiche.id()) instanceof CompagnonEntity compagnon)) {
			return false;
		}

		List<CompagnonEntity> autour = niveau.getEntitiesOfClass(CompagnonEntity.class,
				compagnon.getBoundingBox().inflate(PORTEE),
				autre -> autre != compagnon && autre.ficheId() != null);
		if (autour.isEmpty()) {
			return false;
		}

		Fiches fiches = Fiches.de(serveur);
		boolean change = false;

		for (CompagnonEntity autre : autour) {
			UUID id = autre.ficheId();
			if (fiche.connaissance(id) < PLAFOND) {
				fiche.seSouvenirDe(id);
				fiche.incrementer(Compteurs.RENCONTRES);
				change = true;
			}
			// La reconnaissance est mutuelle : sans cette ligne, seul le compagnon
			// dont le proprietaire est connecte apprendrait a connaitre l'autre, et
			// l'amitie serait a sens unique selon qui joue le plus.
			FicheCompagnon sienne = fiches.get(id);
			if (sienne != null && sienne.connaissance(fiche.id()) < PLAFOND) {
				sienne.seSouvenirDe(fiche.id());
				change = true;
			}
			// Le jour ou il se fait son premier ami compte, et le livre le racontera.
			if (fiche.connaissance(id) >= CompagnonEntity.FOIS_POUR_ETRE_FAMILIER) {
				change |= fiche.marquer("premier_ami", System.currentTimeMillis());
			}
			if (change) {
				compagnon.seSouvenirDe(id, fiche.connaissance(id));
				autre.seSouvenirDe(fiche.id(),
						sienne == null ? 0 : sienne.connaissance(fiche.id()));
			}
		}
		return change;
	}
}
