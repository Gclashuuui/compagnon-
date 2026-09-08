package fr.lhdp.compagnon.progression;

import fr.lhdp.compagnon.fiche.Barre;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tout le reglage de la progression, tel qu'il est ecrit dans
 * {@code data/compagnon/niveaux.json}.
 *
 * <p><b>Le code ne sait pas combien il y a de niveaux ni ce qu'ils contiennent.</b>
 * Il lit la liste. Reorganiser, ajouter, deplacer un palier se fait dans le
 * fichier, sans recompiler — un {@code /reload} suffit.
 *
 * @param sources             ce que rapporte chaque source, et son plafond par jour
 * @param barres              la valeur de depart et la derive de chaque barre
 * @param bobosParHeureDeJeu  combien de petits bobos arrivent par heure de jeu du
 *                            proprietaire. Les bobos n'arrivent que quand il est
 *                            connecte : personne n'est puni d'avoir eu une vie
 * @param paliers             la table niveau -> deblocages, triee par experience
 *                            croissante
 */
public record Progression(
		Map<SourceXp, Source> sources,
		Map<Barre, ReglageBarre> barres,
		float bobosParHeureDeJeu,
		Action action,
		List<Palier> paliers) {

	/**
	 * Ce que coute une action volontaire, et l'etat qu'il faut pour la faire.
	 *
	 * <p>C'est la reponse a « le compagnon est-il en etat de la faire ? ». Elle
	 * vit dans le fichier pour qu'on puisse la regler sans recompiler.
	 *
	 * @param energieDepensee ce que l'action retire a l'energie ; un nombre negatif
	 * @param energieMinimum  en dessous, il est trop fatigue et refuse
	 */
	public record Action(float energieDepensee, float energieMinimum) {
	}

	/**
	 * @param xp             experience gagnee par action
	 * @param plafondParJour experience maximale gagnable par jour reel
	 */
	public record Source(int xp, int plafondParJour) {
	}

	/**
	 * @param depart         valeur d'une barre a la creation du compagnon
	 * @param parMinute      derive par minute de jeu du proprietaire quand le
	 *                       compagnon est actif ; negative pour une barre qui
	 *                       descend toute seule
	 * @param parMinuteRepos derive quand il est assis ou couche. C'est ce qui
	 *                       fait remonter l'energie au repos. Absente du fichier,
	 *                       elle vaut {@code parMinute} : la faim, elle, descend
	 *                       qu'il bouge ou non
	 * @param minimum        plancher ; la sante ne tombe jamais a zero
	 * @param maximum        plafond
	 */
	public record ReglageBarre(float depart, float parMinute, float parMinuteRepos,
			float minimum, float maximum) {

		/** Le taux qui s'applique selon ce que fait le compagnon. */
		public float taux(boolean auRepos) {
			return auRepos ? this.parMinuteRepos : this.parMinute;
		}
	}

	/**
	 * @param niveau   le numero du niveau
	 * @param xp       l'experience totale qu'il faut pour l'atteindre
	 * @param debloque ce qu'il ouvre — des noms d'animation, libres
	 */
	public record Palier(int niveau, int xp, List<String> debloque) {
	}

	/**
	 * De combien l'ecart entre deux niveaux grandit au-dela de la table.
	 *
	 * <p>Huit pour cent par niveau. Assez pour que la montee ralentisse
	 * nettement, assez peu pour qu'elle ne s'arrete jamais tout a fait : au
	 * centieme niveau au-dessus de la table, un niveau coute encore environ
	 * deux mille fois le dernier ecart ecrit, ce qui est long mais fini.
	 */
	private static final double CROISSANCE = 1.08D;

	/**
	 * La table s'arrete-t-elle assez proprement pour qu'on la prolonge ?
	 *
	 * <p>Il faut au moins deux paliers, et un ecart positif entre les deux
	 * derniers : c'est lui qui sert de mesure. Une table d'une seule ligne ne
	 * se prolonge pas, elle plafonne — et c'est le comportement d'avant.
	 */
	private boolean prolongeable() {
		return this.paliers.size() >= 2 && dernierEcart() > 0;
	}

	/** L'ecart d'experience entre les deux derniers paliers ecrits. */
	private int dernierEcart() {
		int taille = this.paliers.size();
		return this.paliers.get(taille - 1).xp() - this.paliers.get(taille - 2).xp();
	}

	/**
	 * Le niveau atteint avec cette experience totale.
	 *
	 * <h2>Il n'y a plus de plafond</h2>
	 *
	 * <p>La table s'arretait net a sa derniere ligne. Sur un serveur qui dure
	 * des annees, un joueur arrive au bout n'a plus aucune raison de continuer
	 * a jouer avec sa bete — c'est exactement ce qu'on cherche a eviter.
	 *
	 * <p>Au-dela, les niveaux continuent donc, chacun un peu plus cher que le
	 * precedent. La formule est fermee des deux cotes — on ne boucle jamais
	 * pour trouver un niveau, quelle que soit l'experience.
	 *
	 * <p>Tout ce qui etait ecrit dans la table reste vrai : les deblocages, et
	 * les seuils que les especes citent — la monte au niveau cinquante — ne
	 * bougent pas d'un pouce.
	 */
	public int niveauPour(int xp) {
		int niveau = this.paliers.isEmpty() ? 0 : this.paliers.get(0).niveau();
		for (Palier palier : this.paliers) {
			if (xp >= palier.xp()) {
				niveau = palier.niveau();
			} else {
				return niveau;
			}
		}
		// Il est au bout de la table : on prolonge.
		if (!prolongeable()) {
			return niveau;
		}
		double reste = xp - (double) this.paliers.get(this.paliers.size() - 1).xp();
		if (reste <= 0) {
			return niveau;
		}
		// Somme geometrique inversee : combien de termes tiennent dans ce reste.
		double premier = dernierEcart() * CROISSANCE;
		double dedans = 1.0D + reste * (CROISSANCE - 1.0D) / premier;
		// Le petit ajout absorbe l arrondi : xpDu et niveauPour doivent se
		// repondre exactement, et un cheveu de flottant les desaccorderait d un
		// niveau entier. Un test verifie l aller-retour sur cent niveaux.
		int enPlus = (int) Math.floor(Math.log(dedans) / Math.log(CROISSANCE) + 1.0E-9D);
		return niveau + Math.max(0, enPlus);
	}

	/** Le niveau le plus haut que la table decrit. Le code ne le connait pas. */
	public int niveauMaximum() {
		return this.paliers.isEmpty() ? 0 : this.paliers.get(this.paliers.size() - 1).niveau();
	}

	/** L'experience qu'il fallait pour atteindre ce niveau, table prolongee. */
	public int xpDu(int niveau) {
		for (Palier palier : this.paliers) {
			if (palier.niveau() == niveau) {
				return palier.xp();
			}
		}
		int max = niveauMaximum();
		if (niveau <= max || !prolongeable()) {
			return 0;
		}
		// Arrondi vers le haut : l experience rendue doit atteindre le niveau
		// demande, jamais rester un point en dessous.
		return (int) Math.min(Integer.MAX_VALUE, Math.ceil(xpProlonge(niveau - max)));
	}

	/**
	 * L'experience totale a {@code enPlus} niveaux au-dessus de la table.
	 *
	 * <p>Somme geometrique : le dernier ecart, puis le meme multiplie par la
	 * croissance, et ainsi de suite. C'est l'inverse exact de ce que fait
	 * {@link #niveauPour} — les deux doivent se repondre, un test le verifie.
	 */
	private double xpProlonge(int enPlus) {
		double base = this.paliers.get(this.paliers.size() - 1).xp();
		double premier = dernierEcart() * CROISSANCE;
		double somme = premier * (Math.pow(CROISSANCE, enPlus) - 1.0D) / (CROISSANCE - 1.0D);
		return base + somme;
	}

	/**
	 * L'experience du niveau suivant. Sert a la barre d'experience du livre.
	 *
	 * <p>Elle ne rend plus jamais {@code -1} tant que la table est prolongeable :
	 * la barre du livre continue de se remplir au-dela du dernier palier ecrit,
	 * au lieu de rester pleine pour toujours.
	 */
	public int xpDuSuivant(int niveau) {
		for (Palier palier : this.paliers) {
			if (palier.niveau() > niveau) {
				return palier.xp();
			}
		}
		if (!prolongeable()) {
			return -1;
		}
		return (int) Math.min(Integer.MAX_VALUE,
				Math.ceil(xpProlonge(niveau - niveauMaximum() + 1)));
	}

	/** Tout ce qui est ouvert jusqu'a ce niveau inclus, sans doublon. */
	public Set<String> debloquesJusqua(int niveau) {
		Set<String> ouverts = new LinkedHashSet<>();
		for (Palier palier : this.paliers) {
			if (palier.niveau() <= niveau) {
				ouverts.addAll(palier.debloque());
			}
		}
		return ouverts;
	}

	/** Tout ce que la table decrit, ouvert ou non. Sert aux cadenas de la roue. */
	public List<String> tousLesDeblocages() {
		List<String> tous = new ArrayList<>();
		for (Palier palier : this.paliers) {
			for (String deblocage : palier.debloque()) {
				if (!tous.contains(deblocage)) {
					tous.add(deblocage);
				}
			}
		}
		return tous;
	}

	/** Le niveau auquel une chose s'ouvre, ou {@code -1} si la table l'ignore. */
	public int niveauDe(String deblocage) {
		for (Palier palier : this.paliers) {
			if (palier.debloque().contains(deblocage)) {
				return palier.niveau();
			}
		}
		return -1;
	}
}
