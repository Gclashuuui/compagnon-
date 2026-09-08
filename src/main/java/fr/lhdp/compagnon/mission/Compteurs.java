package fr.lhdp.compagnon.mission;

import fr.lhdp.compagnon.entite.CompagnonEntity;
import fr.lhdp.compagnon.fiche.FicheCompagnon;
import fr.lhdp.compagnon.fiche.Fiches;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/**
 * Ce que le mod compte.
 *
 * <h2>A quoi ca sert</h2>
 *
 * <p>Les missions ne surveillent rien : elles regardent grandir un compteur.
 * Ajouter un type de mission ne demande donc aucun code de suivi — il suffit
 * qu'un compteur existe et que quelque chose l'incremente. Cette classe est cet
 * « quelque chose », et l'endroit unique ou l'on voit ce que le mod observe.
 *
 * <h2>Deux sortes de compteurs</h2>
 *
 * <ul>
 *   <li>Les <b>ponctuels</b>, incrementes la ou l'evenement se produit : un
 *       repas, un mot appris, un envol. Une ligne a l'endroit qui va bien.</li>
 *   <li>Les <b>ambiants</b>, releves une fois par minute : la nuit dehors, la
 *       pluie, l'altitude, le biome. On ne peut pas les « attraper », alors on
 *       les echantillonne — et une minute est bien assez fin pour des missions
 *       qui se comptent en jours.</li>
 * </ul>
 *
 * <h2>Les fronts</h2>
 *
 * <p>Certains compteurs doivent compter des <b>occasions</b>, pas des minutes :
 * un orage traverse compte une fois, pas deux cents fois. On garde donc un petit
 * temoin a cote, remis a zero quand la situation cesse. Sans lui, une mission
 * « traverse deux orages » se validerait en deux minutes de pluie.
 */
public final class Compteurs {

	private Compteurs() {
	}

	// --- Les noms, cites par les fichiers de missions -------------------------

	public static final String MOTS = "mots";
	public static final String ENVOLS = "envols";
	public static final String MONTES = "montes";
	public static final String SIESTES = "siestes";
	public static final String MINE = "mine";
	public static final String POSE = "pose";
	public static final String NUITS = "nuits";
	public static final String BIOMES = "biomes";
	public static final String HAUTEUR = "hauteur";
	public static final String NAGE = "nage";
	public static final String PLUIE = "pluie";
	public static final String ORAGES = "orages";
	public static final String RENCONTRES = "rencontres";
	public static final String SALUTATIONS = "salutations";
	public static final String CARESSES_AMIS = "caresses_amis";

	// Les compteurs des jouets — jeux, balle, jouets offerts — arriveront avec
	// les jouets. Les declarer avant qu'ils n'existent aurait laisse croire
	// qu'ils comptent quelque chose, et une mission qui les citerait resterait
	// bloquee a zero pour toujours.

	/** Les temoins de front. Prefixes pour ne pas se meler aux vrais compteurs. */
	private static final String TEMOIN_HAUTEUR = "t.hauteur";
	private static final String TEMOIN_ORAGE = "t.orage";
	private static final String TEMOIN_NUIT = "t.nuit";

	/** A partir de quelle altitude on considere qu'il a pris de la hauteur. */
	private static final int HAUT = 150;

	// --- Les compteurs ponctuels ---------------------------------------------

	/**
	 * Ajoute un a un compteur, depuis n'importe ou dans le jeu.
	 *
	 * <p>Prend l'entite plutot que la fiche : les buts et les interactions ont la
	 * bete sous la main, pas sa fiche, et aller la chercher a chaque fois serait
	 * du bruit dans chaque appelant.
	 *
	 * <p>Ne fait rien cote client, ni pour une bete sans fiche.
	 */
	public static void compter(CompagnonEntity compagnon, String cle) {
		if (compagnon == null || compagnon.ficheId() == null
				|| !(compagnon.level() instanceof ServerLevel niveau)) {
			return;
		}
		compter(niveau.getServer(), compagnon.ficheId(), cle);
	}

	/** La meme chose, quand on n'a que l'identifiant de la fiche. */
	public static void compter(MinecraftServer serveur, UUID ficheId, String cle) {
		if (serveur == null || ficheId == null) {
			return;
		}
		Fiches fiches = Fiches.de(serveur);
		FicheCompagnon fiche = fiches.get(ficheId);
		if (fiche == null) {
			return;
		}
		fiche.incrementer(cle);
		fiches.setDirty();
	}

	// --- Les compteurs ambiants ----------------------------------------------

	/**
	 * Releve ce que le monde fait a cette bete. A appeler une fois par minute.
	 *
	 * @return vrai si la fiche a change
	 */
	public static boolean releverLaMinute(MinecraftServer serveur, FicheCompagnon fiche) {
		if (!fiche.sorti()) {
			return false;
		}
		ServerLevel niveau = serveur.getLevel(fiche.dimension());
		if (niveau == null) {
			return false;
		}
		Entity trouvee = niveau.getEntity(fiche.id());
		if (!(trouvee instanceof CompagnonEntity compagnon) || compagnon.isRemoved()) {
			return false;
		}

		BlockPos ou = compagnon.blockPosition();
		boolean change = false;

		// LA NUIT DEHORS. Une par nuit, pas une par minute : le temoin est le
		// numero du jour, donc deux nuits de suite comptent bien pour deux.
		if (niveau.isNight() && niveau.canSeeSky(ou)) {
			int jour = (int) (niveau.getDayTime() / 24000L);
			if (fiche.compteur(TEMOIN_NUIT) != jour + 1) {
				fiche.poserCompteur(TEMOIN_NUIT, jour + 1);
				fiche.incrementer(NUITS);
				change = true;
			}
		}

		// LE BIOME. Un moment marque par biome decouvert : c'est lui qui empeche
		// de compter deux fois la meme foret, et c'est aussi ce que la page des
		// premieres fois lira un jour.
		ResourceLocation biome = niveau.registryAccess()
				.registryOrThrow(Registries.BIOME)
				.getKey(niveau.getBiome(ou).value());
		if (biome != null
				&& fiche.marquer("premiere_fois.biome." + biome.getPath(),
						System.currentTimeMillis())) {
			fiche.incrementer(BIOMES);
			change = true;
		}

		// L'ALTITUDE, sur front : une montee compte une fois, quel que soit le
		// temps passe la-haut.
		boolean enHaut = ou.getY() >= HAUT;
		if (enHaut && fiche.compteur(TEMOIN_HAUTEUR) == 0) {
			fiche.poserCompteur(TEMOIN_HAUTEUR, 1);
			fiche.incrementer(HAUTEUR);
			change = true;
		} else if (!enHaut && fiche.compteur(TEMOIN_HAUTEUR) != 0) {
			fiche.poserCompteur(TEMOIN_HAUTEUR, 0);
			change = true;
		}

		// L'ORAGE, sur front aussi.
		boolean orage = niveau.isThundering() && niveau.canSeeSky(ou);
		if (orage && fiche.compteur(TEMOIN_ORAGE) == 0) {
			fiche.poserCompteur(TEMOIN_ORAGE, 1);
			fiche.incrementer(ORAGES);
			change = true;
		} else if (!orage && fiche.compteur(TEMOIN_ORAGE) != 0) {
			fiche.poserCompteur(TEMOIN_ORAGE, 0);
			change = true;
		}

		// LA PLUIE ET L'EAU, en minutes : celles-la sont des durees, pas des
		// occasions, et c'est ce que leurs missions demandent.
		if (niveau.isRainingAt(ou.above())) {
			fiche.incrementer(PLUIE);
			change = true;
		}
		if (compagnon.isInWater()) {
			fiche.incrementer(NAGE);
			change = true;
		}
		return change;
	}
}
