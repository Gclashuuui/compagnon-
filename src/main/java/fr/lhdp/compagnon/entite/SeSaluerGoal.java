package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.mission.Compteurs;
import fr.lhdp.compagnon.fiche.FicheCompagnon;
import fr.lhdp.compagnon.fiche.Fiches;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Deux betes qui se connaissent s'arretent une seconde en se croisant.
 *
 * <h2>Ce que ca donne a une cour</h2>
 *
 * <p>Le mod compte deja les rencontres entre compagnons : au bout de cinq, ils
 * deviennent familiers l'un de l'autre (voir {@code Voisinages}). Cette donnee
 * ne servait qu'a s'aller voir de loin en loin.
 *
 * <p>Ici elle sert a autre chose : quand deux familiers se croisent, ils
 * s'arretent, se regardent, jouent leur geste de salut, et repartent chacun de
 * son cote.
 *
 * <p>Sur un serveur a mille joueurs, ca donne a une salle commune une vie que
 * <b>personne n'a scriptee</b> — les habitudes se forment toutes seules, entre
 * betes dont les proprietaires se croisent souvent. Et on ne peut pas la
 * fabriquer autrement : elle vient du fait que ces deux-la, precisement, se
 * connaissent.
 *
 * <h2>Ce qui empeche que ca devienne penible</h2>
 *
 * <p>Deux betes qui vivent dans la meme piece se salueraient sans arret. D'ou un
 * long delai avant de recommencer avec <b>la meme</b> — assez long pour que le
 * geste garde son sens, assez court pour qu'on le voie encore.
 *
 * <p>Et le but est en bas de la liste : il ne coupe jamais un ordre, une course,
 * ni un repas.
 */
public class SeSaluerGoal extends Goal {

	/** A quelle distance ils se remarquent, en blocs. */
	private static final double PORTEE = 6.0D;

	/** Combien de temps dure le salut, en ticks. */
	private static final int DUREE = 40;

	/** Il ne cherche pas a chaque tick : deux secondes suffisent. */
	private static final int ENTRE_DEUX_REGARDS = 20 * 2;

	/**
	 * Avant de resaluer la meme bete.
	 *
	 * <p>Deux minutes. Sans ce delai, deux compagnons qui partagent une chambre
	 * passeraient leur vie a se dire bonjour — ce qui aurait exactement l'effet
	 * inverse de celui qu'on cherche.
	 */
	private static final int AVANT_DE_RESALUER = 20 * 120;

	/**
	 * Le role d'animation du salut.
	 *
	 * <p>Le prefixe {@code @} veut dire « c'est un role, la fiche d'espece dira
	 * quelle animation ». Une espece qui ne le decrit pas s'arrete et regarde
	 * quand meme : le geste compte plus que l'animation.
	 */
	private static final String GESTE = "@salut";

	/** Ce que chaque point commun ajoute au salut, en ticks. */
	private static final int EN_PLUS_PAR_POINT_COMMUN = 15;

	private final CompagnonEntity compagnon;

	private CompagnonEntity ami;
	private int reste;
	private int avantDeRegarder;

	/** Le dernier salue, et dans combien de ticks on pourra le resaluer. */
	private java.util.UUID dernierSalue;
	private int avantDeResaluer;

	public SeSaluerGoal(CompagnonEntity compagnon) {
		this.compagnon = compagnon;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (this.avantDeResaluer > 0) {
			this.avantDeResaluer--;
		}
		// UN JALOUX NE SALUE PERSONNE. Jamais. C'est son defaut, et il rend une
		// salle commune pleine de compagnons franchement differente selon la
		// bete qu'on y amene.
		if (Manies.JALOUX.equals(this.compagnon.defaut())) {
			return false;
		}
		// Occupe : le salut peut attendre. Il n'interrompt jamais rien.
		if (!this.compagnon.estLibre() || !this.compagnon.lesMainsVides()) {
			return false;
		}
		if (--this.avantDeRegarder > 0) {
			return false;
		}
		this.avantDeRegarder = ENTRE_DEUX_REGARDS;

		this.ami = unFamilierACote();
		return this.ami != null;
	}

	@Override
	public void start() {
		// DEUX BETES QUI SE RESSEMBLENT SE SALUENT PLUS LONGUEMENT.
		//
		// Meme espece, et des choix de vie en commun : elles ont plus a se dire.
		// C'est une nuance de trois quarts de seconde, que personne ne remarquera
		// consciemment — et qui fait qu'un couloir plein de compagnons n'a pas
		// l'air de repeter le meme geste en boucle.
		int duree = DUREE + enCommunAvec(this.ami) * EN_PLUS_PAR_POINT_COMMUN;
		this.reste = duree;
		this.compagnon.getNavigation().stop();
		this.compagnon.jouerActionPendant(GESTE, duree, PrioriteAction.AFFECTIF);

		Compteurs.compter(this.compagnon, Compteurs.SALUTATIONS);
		this.dernierSalue = this.ami.getUUID();
		this.avantDeResaluer = AVANT_DE_RESALUER;
	}

	@Override
	public boolean canContinueToUse() {
		return this.reste > 0 && this.ami != null && this.ami.isAlive()
				&& this.compagnon.estLibre();
	}

	@Override
	public void stop() {
		this.ami = null;
		this.compagnon.getNavigation().stop();
	}

	@Override
	public void tick() {
		this.reste--;
		if (this.ami != null) {
			this.compagnon.getLookControl().setLookAt(this.ami, 30.0F, 30.0F);
		}
	}

	/**
	 * Ce que ces deux-la ont en commun, de zero a trois.
	 *
	 * <p>L'espece compte pour un, et chaque competence partagee pour un, jusqu'a
	 * deux. On ne va pas plus loin : au-dela, deux betes de haut niveau se
	 * salueraient pendant dix secondes, et le geste cesserait d'etre un salut.
	 */
	private int enCommunAvec(CompagnonEntity autre) {
		if (!(this.compagnon.level() instanceof ServerLevel niveau)
				|| this.compagnon.ficheId() == null || autre.ficheId() == null) {
			return 0;
		}
		Fiches fiches = Fiches.de(niveau.getServer());
		FicheCompagnon mienne = fiches.get(this.compagnon.ficheId());
		FicheCompagnon sienne = fiches.get(autre.ficheId());
		if (mienne == null || sienne == null) {
			return 0;
		}

		int commun = mienne.espece().equals(sienne.espece()) ? 1 : 0;
		int partagees = 0;
		for (String competence : mienne.competences()) {
			if (sienne.a(competence) && ++partagees >= 2) {
				break;
			}
		}
		return commun + partagees;
	}

	/**
	 * Une bete familiere a portee, qu'on n'a pas saluee recemment.
	 *
	 * <p>Familiere et non simplement presente : deux inconnus ne se disent pas
	 * bonjour, et c'est ce qui donne du prix au fait que ces deux-la le fassent.
	 */
	private CompagnonEntity unFamilierACote() {
		CompagnonEntity autre = this.compagnon.amiMemorise();
		if (autre == null || this.compagnon.distanceToSqr(autre) > PORTEE * PORTEE
				|| this.avantDeResaluer > 0 && autre.getUUID().equals(this.dernierSalue)) {
			return null;
		}
		return autre;
	}
}
