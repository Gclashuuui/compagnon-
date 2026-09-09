package fr.lhdp.compagnon.livre;

import fr.lhdp.compagnon.contenu.Bobo;
import fr.lhdp.compagnon.contenu.Contenu;
import fr.lhdp.compagnon.fiche.Barre;
import fr.lhdp.compagnon.entite.CompagnonEntity;
import fr.lhdp.compagnon.fiche.FicheCompagnon;
import fr.lhdp.compagnon.fiche.Fiches;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.GameProfileCache;

import java.util.ArrayList;
import fr.lhdp.compagnon.fiche.Humeur;
import fr.lhdp.compagnon.fiche.Moment;
import fr.lhdp.compagnon.progression.Progression;
import fr.lhdp.compagnon.progression.SourceXp;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Tout ce que le livre affiche, mis a plat.
 *
 * <p>Le livre lit la fiche, pas l'entite : il s'ouvre donc <b>toujours</b>, meme
 * quand le compagnon est reste dans une chambre a l'autre bout du chateau.
 *
 * <p>C'est le serveur qui remplit cet objet et l'envoie. Le client n'a rien a
 * calculer : ni le niveau, ni l'humeur, ni le nom du remede. Il dessine.
 */
public record DonneesLivre(
		String nom,
		String espece,
		String variante,

		int niveau,
		int niveauMax,
		int xp,
		int xpDuNiveau,
		/** Experience du palier suivant, ou {@code -1} au dernier. */
		int xpDuSuivant,

		/**
		 * Les trois attentions deja partagees aujourd'hui, rangees dans trois bits.
		 * L'ordre est celui de {@link SourceXp} : soin, affection, balade.
		 */
		int rituelsDuJour,

		Map<Barre, Float> barres,

		String humeurLibelle,
		String humeurBouille,
		String mode,
		String caractereNom,
		float sociabilite,
		float attachement,
		float vivacite,
		float calin,
		float curiosite,

		/** Vide s'il va bien. */
		String boboNom,
		String boboDescription,
		String boboRemede,

		long dateObtention,
		long ticksEnsemble,
		List<Moment> moments,
		Map<String, Integer> compteurs,

		/**
		 * Les mots qu'on lui a appris, et le geste de chacun.
		 *
		 * <p>La roue les montre un par un, sous leur geste. Il manquait la vue
		 * d'ensemble : ce que <b>cette bete-la</b> sait faire, en une page.
		 */
		Map<String, String> motsAppris,

		/** Ce qu'il a appris a etre, et ce qu'il pourrait encore devenir. */
		List<EntreeCompetence> competences,

		/** Combien de points il lui reste a depenser. */
		int pointsDeCompetence,

		/**
		 * Les gens et les betes qu'il connait, par leur nom.
		 *
		 * <p>Le serveur les resout avant d'envoyer : le client n'a aucun moyen de
		 * transformer un identifiant en nom, surtout pour un joueur deconnecte.
		 */
		List<String> connait,
		List<FicheCompagnon.Lieu> lieux,

		/**
		 * Ses missions du moment, la longue en dernier.
		 *
		 * <p>Deja mises en forme par le serveur : le client ne connait ni les
		 * moules ni les compteurs, il recoit une phrase et deux nombres.
		 */
		List<EntreeMission> missions,

		/** Peut-il encore en ecarter une ? */
		boolean peutEcarter,

		/**
		 * Le niveau a partir duquel son espece se monte, ou zero si elle ne se
		 * monte pas.
		 *
		 * <p>La fiche d'espece le sait, le client non : il ne lit pas les fiches
		 * d'espece. Sans ce champ, le livre ne pouvait pas dire la seule chose
		 * qu'un joueur d'oiseau bleu a envie de lire — a partir de quand il
		 * pourra monter dessus.
		 */
		int monterAuNiveau) {

	/**
	 * Ses missions, traduites en lignes affichables.
	 *
	 * <p>Une mission dont le moule a disparu — un fichier retire entre deux
	 * demarrages — est simplement sautee. Le livre n'affichera pas de ligne
	 * vide, et le carnet en reproposera une au prochain renouvellement.
	 */
	/**
	 * Les compteurs qu'on montre au joueur.
	 *
	 * <h2>La convention</h2>
	 *
	 * <p>Un compteur dont le nom contient un point est un compteur <b>de
	 * cuisine</b> : la place d'une mission, son cran, son point de depart, un
	 * temoin de front. Ceux-la ne veulent rien dire pour personne.
	 *
	 * <p>Ils s'affichaient tels quels dans la page « Ce qu'on a fait
	 * ensemble » — « m.moule.0 : 14 », « m.jour : 2 ». C'etait laid et
	 * incomprehensible. On les ecarte ici, cote serveur : ils ne partent meme
	 * plus sur le reseau.
	 *
	 * <p>D'ou la regle, a tenir : <b>un compteur qu'on montre n'a jamais de
	 * point dans son nom ; un compteur interne en a toujours un.</b>
	 */
	/** A quel niveau son espece se laisse monter, ou zero. */
	private static int niveauDeMonte(FicheCompagnon fiche) {
		fr.lhdp.compagnon.espece.Espece espece =
			fr.lhdp.compagnon.espece.Especes.get(fiche.espece());
		return espece == null || !espece.seMonte() ? 0 : espece.monterAuNiveau();
	}

	private static Map<String, Integer> compteursMontrables(FicheCompagnon fiche) {
		Map<String, Integer> montrables = new java.util.LinkedHashMap<>();
		fiche.compteurs().forEach((cle, valeur) -> {
			if (cle.indexOf('.') < 0 && valeur > 0) {
				montrables.put(cle, valeur);
			}
		});
		return Map.copyOf(montrables);
	}

	private static List<EntreeMission> sesMissions(FicheCompagnon fiche) {
		List<EntreeMission> lignes = new java.util.ArrayList<>(4);
		for (fr.lhdp.compagnon.mission.Carnet.Ligne ligne
				: fr.lhdp.compagnon.mission.Carnet.lire(fiche)) {
			if (ligne.moule() == null) {
				continue;
			}
			lignes.add(new EntreeMission(ligne.moule().texte(), ligne.quantite(),
					ligne.faits(), ligne.finie(), ligne.moule().longue()));
		}
		return List.copyOf(lignes);
	}

	public boolean aUnBobo() {
		return !this.boboNom.isEmpty();
	}

	/** Ce qui a ete accompli dans le niveau en cours, entre 0 et 1. */
	public float avancementDuNiveau() {
		if (this.xpDuSuivant < 0) {
			return 1.0F;
		}
		int etendue = this.xpDuSuivant - this.xpDuNiveau;
		if (etendue <= 0) {
			return 1.0F;
		}
		return Math.max(0.0F, Math.min(1.0F, (this.xp - this.xpDuNiveau) / (float) etendue));
	}

	/** Cette sorte d'attention a-t-elle deja eu lieu aujourd'hui ? */
	public boolean rituelAccompli(SourceXp source) {
		return (this.rituelsDuJour & (1 << source.ordinal())) != 0;
	}

	/** Combien des trois attentions du jour ont deja eu lieu. */
	public int rituelsAccomplis() {
		return Integer.bitCount(this.rituelsDuJour);
	}

	private static int rituelsDuJour(FicheCompagnon fiche, Progression table, long maintenant) {
		int faits = 0;
		for (SourceXp source : SourceXp.values()) {
			if (fiche.aGagneAujourdhui(source, table, maintenant)) {
				faits |= 1 << source.ordinal();
			}
		}
		return faits;
	}

	/**
	 * Les noms de ceux qu'il connait assez pour aller les voir.
	 *
	 * <p>La meme table sert aux joueurs et aux compagnons — c'est la meme question
	 * posee : qui a-t-il assez cotoye ? On les separe ici pour la lecture, les
	 * betes apres les gens.
	 *
	 * <p>Un identifiant qu'on n'arrive pas a nommer est simplement passe. Ce n'est
	 * pas grave : c'est un joueur qui n'est jamais venu sur ce serveur, ou un
	 * compagnon supprime.
	 */
	/**
	 * Toutes les competences de son espece, prises ou non.
	 *
	 * <p>Toutes, y compris celles qu'il ne peut pas encore prendre. Montrer ce
	 * qui existe encore fait plus pour l'envie que de le cacher — c'est le meme
	 * raisonnement que les cadenas de la roue.
	 */
	private static List<EntreeCompetence> lesCompetences(FicheCompagnon fiche) {
		List<EntreeCompetence> entrees = new java.util.ArrayList<>();
		for (fr.lhdp.compagnon.competence.Competence competence
				: fr.lhdp.compagnon.competence.Competences.pour(fiche.espece())) {
			entrees.add(new EntreeCompetence(competence.id(), competence.nom(),
					competence.description(), competence.niveauRequis(),
					fiche.a(competence.id())));
		}
		return List.copyOf(entrees);
	}

	private static List<String> quiIlConnait(FicheCompagnon fiche, MinecraftServer serveur) {
		if (serveur == null) {
			return List.of();
		}
		Fiches fiches = Fiches.de(serveur);
		List<String> gens = new ArrayList<>();
		List<String> betes = new ArrayList<>();

		fiche.connaissances().forEach((qui, fois) -> {
			if (fois < CompagnonEntity.FOIS_POUR_ETRE_FAMILIER) {
				return;
			}
			FicheCompagnon autre = fiches.get(qui);
			if (autre != null) {
				betes.add(autre.nom());
				return;
			}
			// Le cache des profils, et non la liste des connectes : on veut pouvoir
			// nommer quelqu'un qui n'est pas la. Un joueur jamais venu sur ce
			// serveur reste introuvable, et on le passe simplement.
			GameProfileCache cache = serveur.getProfileCache();
			if (cache != null) {
				cache.get(qui).ifPresent(profil -> gens.add(profil.getName()));
			}
		});

		gens.sort(String::compareToIgnoreCase);
		betes.sort(String::compareToIgnoreCase);
		gens.addAll(betes);
		return List.copyOf(gens);
	}

	public static DonneesLivre de(FicheCompagnon fiche, Progression table, MinecraftServer serveur) {
		int niveau = fiche.niveau(table);
		long maintenant = System.currentTimeMillis();

		Map<Barre, Float> barres = new EnumMap<>(Barre.class);
		for (Barre barre : Barre.values()) {
			barres.put(barre, fiche.barre(barre));
		}

		Humeur humeur = fiche.humeur();
		fr.lhdp.compagnon.contenu.Caractere caractere = Contenu.caractere(fiche.caractere());
		if (caractere == null) {
			caractere = fr.lhdp.compagnon.contenu.Caractere.ORDINAIRE;
		}

		String boboNom = "";
		String boboDescription = "";
		String boboRemede = "";
		if (fiche.aUnBobo()) {
			Bobo bobo = Contenu.bobo(fiche.bobo());
			if (bobo != null) {
				boboNom = bobo.nom();
				boboDescription = bobo.description();
				boboRemede = bobo.soignePar();
			} else {
				// Le bobo a disparu des donnees : on le dit plutot que de faire
				// comme s'il allait bien.
				boboNom = fiche.bobo();
				boboDescription = "Ce bobo n'existe plus dans les donnees du serveur.";
			}
		}

		return new DonneesLivre(
				fiche.nom(), fiche.espece(), fiche.variante(),
				niveau, table.niveauMaximum(), fiche.xp(),
				table.xpDu(niveau), table.xpDuSuivant(niveau),
				rituelsDuJour(fiche, table, maintenant),
				Map.copyOf(barres),
				humeur.libelle(), humeur.bouille(),
				fiche.mode().cleDeTraduction(),
				caractere.nom(), caractere.sociabilite(), caractere.attachement(),
				caractere.vivacite(), caractere.calin(), caractere.curiosite(),
				boboNom, boboDescription, boboRemede,
				fiche.dateObtention(), fiche.ticksEnsemble(),
				fiche.moments(), compteursMontrables(fiche), fiche.motsAppris(),
				lesCompetences(fiche), fiche.pointsRestants(table,
						fr.lhdp.compagnon.reglage.Reglages.pointsTousLesNiveaux()),
				quiIlConnait(fiche, serveur),
				List.copyOf(fiche.lieux()),
				sesMissions(fiche),
				fr.lhdp.compagnon.mission.Carnet.peutEcarter(fiche),
				niveauDeMonte(fiche));
	}
}
