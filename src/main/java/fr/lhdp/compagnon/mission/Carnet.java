package fr.lhdp.compagnon.mission;

import fr.lhdp.compagnon.fiche.FicheCompagnon;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Les missions en cours d'un compagnon, et leur renouvellement.
 *
 * <h2>On ne les accepte pas</h2>
 *
 * <p>Trois missions sont proposees, plus une longue. Aucun bouton pour les
 * prendre : elles sont la, et elles se cochent toutes seules quand on les a
 * vecues. La difference n'est pas cosmetique — une mission qu'on <b>accepte</b>
 * devient une tache a faire, une mission qu'on <b>decouvre avoir accomplie</b>
 * reste un souvenir.
 *
 * <h2>Comment elles se suivent</h2>
 *
 * <p>Chaque mission retient la valeur du compteur qu'elle regarde, au moment ou
 * elle a ete proposee. Sa progression est la difference. Aucun code de suivi,
 * aucune ecoute d'evenement en plus : il suffit qu'un compteur existe.
 *
 * <p>Consequence voulue : ce qu'on avait deja fait avant qu'elle n'arrive ne
 * compte pas. Une mission « nourris-le trois fois » ne se valide pas parce qu'on
 * l'a nourri trois cents fois le mois dernier.
 *
 * <h2>Ou c'est range</h2>
 *
 * <p>Dans les compteurs de la fiche, qui sont deja sauvegardes : trois nombres
 * par mission, plus une date de renouvellement. <b>Aucun changement de format de
 * sauvegarde.</b>
 */
public final class Carnet {

	private Carnet() {
	}

	/** Combien de missions courtes on propose a la fois. */
	public static final int COMBIEN = 3;

	/**
	 * Tous les combien de jours joues on renouvelle.
	 *
	 * <p>Des jours <b>joues</b>, jamais des jours de calendrier : quelqu'un qui
	 * joue deux heures le samedi ne doit pas rater onze missions sur douze.
	 */
	public static final int TOUS_LES_JOURS = 3;

	/** Combien de missions on peut ecarter par renouvellement. */
	public static final int REJETS = 1;

	// Les cles dans les compteurs de la fiche. Prefixees pour ne rien heurter.
	private static final String CLE_MOULE = "m.moule.";
	private static final String CLE_CRAN = "m.cran.";
	private static final String CLE_DEPART = "m.depart.";
	private static final String CLE_FAITE = "m.faite.";
	private static final String CLE_JOUR = "m.jour";
	private static final String CLE_REJETS = "m.rejets";

	/**
	 * L'empreinte d'un moule, rangee dans la fiche a la place de son nom.
	 *
	 * <h2>Pourquoi pas son numero d'ordre</h2>
	 *
	 * <p>C'est ce que faisait la premiere version : les compteurs ne retiennent
	 * que des entiers, alors on rangeait la position du moule dans la liste
	 * chargee. Sauf que cette liste est triee par nom de fichier — <b>ajouter
	 * une seule mission decale toutes celles qui viennent apres</b>, et les
	 * missions en cours de tous les joueurs du serveur changent d'enonce d'un
	 * coup, en gardant leur progression. Le joueur voit sa mission se
	 * transformer sous ses yeux.
	 *
	 * <p>L'empreinte du nom, elle, ne bouge jamais. Deux noms differents
	 * pourraient theoriquement tomber sur la meme, mais sur une trentaine de
	 * fichiers c'est hors de question — et un test le verifie a chaque
	 * chargement plutot que de l'esperer.
	 *
	 * <p>Le {@code & 0x3FFFFFFF} garde un nombre positif : les compteurs sont
	 * ecrits comme des entiers signes, et on utilise le zero pour dire
	 * « aucune mission ici ».
	 */
	public static int empreinteDe(Moule moule) {
		return (moule.id().hashCode() & 0x3FFFFFFF) + 1;
	}

	/** Le moule d'une empreinte, ou {@code null} si le fichier a disparu. */
	private static Moule parEmpreinte(int empreinte) {
		if (empreinte <= 0) {
			return null;
		}
		for (Moule moule : Moules.tous()) {
			if (empreinteDe(moule) == empreinte) {
				return moule;
			}
		}
		return null;
	}

	/**
	 * Une mission proposee.
	 *
	 * @param moule    le moule d'ou elle vient, ou {@code null} s'il a disparu
	 * @param cran     sa difficulte
	 * @param quantite ce qu'elle demande
	 * @param faits    ce qui est deja fait
	 * @param finie    vrai quand elle est accomplie
	 */
	public record Ligne(Moule moule, int cran, int quantite, int faits, boolean finie) {

		/** De zero a un, pour la petite barre du livre. */
		public float part() {
			return this.quantite <= 0 ? 1.0F
					: Math.min(1.0F, this.faits / (float) this.quantite);
		}
	}

	// --- Ce que le livre affiche ---------------------------------------------

	/** Les missions du moment, la longue en dernier. */
	public static List<Ligne> lire(FicheCompagnon fiche) {
		List<Ligne> lignes = new ArrayList<>(COMBIEN + 1);
		for (int place = 0; place <= COMBIEN; place++) {
			Ligne ligne = ligne(fiche, place);
			if (ligne != null) {
				lignes.add(ligne);
			}
		}
		return lignes;
	}

	private static Ligne ligne(FicheCompagnon fiche, int place) {
		Moule moule = parEmpreinte(fiche.compteur(CLE_MOULE + place));
		if (moule == null) {
			return null;
		}
		int cran = fiche.compteur(CLE_CRAN + place);
		int quantite = moule.quantite(cran);
		boolean finie = fiche.compteur(CLE_FAITE + place) > 0;

		int depart = fiche.compteur(CLE_DEPART + place);
		int faits = finie ? quantite
				: Math.max(0, Math.min(quantite, fiche.compteur(moule.compteur()) - depart));
		return new Ligne(moule, cran, quantite, faits, finie);
	}

	// --- Ce qui tourne cote serveur ------------------------------------------

	/**
	 * Ce qu'un passage du carnet a change.
	 *
	 * <p>La liste seule ne suffisait pas : un renouvellement modifie la fiche
	 * sans rien accomplir, et l'appelant doit le savoir pour la sauvegarder.
	 * A l'inverse, une minute ou rien ne bouge ne doit pas la salir — sinon on
	 * force une ecriture par minute et par compagnon, pour rien.
	 */
	public record Avancee(List<Moule> accomplies, boolean change) {
	}

	/**
	 * Fait avancer le carnet : renouvellement, puis validation.
	 *
	 * <p>A appeler de temps en temps, pas a chaque tick — une fois par minute
	 * suffit largement pour des missions qui se comptent en jours.
	 *
	 * @param jour le jour de jeu en cours, compte en temps passe ensemble
	 */
	public static Avancee avancer(FicheCompagnon fiche, long jour, Random hasard) {
		if (Moules.tous().isEmpty()) {
			return new Avancee(List.of(), false);
		}

		// Le renouvellement d'abord : une mission qui vient d'etre posee ne peut
		// pas etre finie dans la foulee, puisque son depart est le compteur actuel.
		boolean change = false;
		if (jour - fiche.compteur(CLE_JOUR) >= TOUS_LES_JOURS || rienDePropose(fiche)) {
			renouveler(fiche, jour, hasard);
			change = true;
		}

		List<Moule> accomplies = new ArrayList<>(2);
		for (int place = 0; place <= COMBIEN; place++) {
			Ligne ligne = ligne(fiche, place);
			if (ligne == null || ligne.finie() || ligne.faits() < ligne.quantite()) {
				continue;
			}
			// La ligne est relue AVANT d'etre cochee, pour que l'appelant puisse
			// encore connaitre son cran et donc ce qu'elle rapporte.
			accomplies.add(ligne.moule());
		}
		return new Avancee(accomplies, change || !accomplies.isEmpty());
	}

	/** Coche une mission accomplie, une fois qu'elle a ete payee. */
	public static void cocher(FicheCompagnon fiche, Moule moule) {
		for (int place = 0; place <= COMBIEN; place++) {
			if (fiche.compteur(CLE_MOULE + place) == empreinteDe(moule)) {
				fiche.poserCompteur(CLE_FAITE + place, 1);
				return;
			}
		}
	}

	private static boolean rienDePropose(FicheCompagnon fiche) {
		for (int place = 0; place < COMBIEN; place++) {
			if (fiche.compteur(CLE_MOULE + place) > 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Tire trois nouvelles missions courtes, et une longue s'il n'y en a pas.
	 *
	 * <p>Jamais deux de la meme famille : trois missions de nourriture le meme
	 * jour donneraient l'impression d'un tirage casse, meme si c'est le hasard.
	 *
	 * <p>La mission longue, elle, ne se renouvelle pas. Elle court sur des
	 * semaines et s'acheve une fois. C'est le seul objectif du mod qui demande de
	 * la duree plutot que de l'attention.
	 */
	public static void renouveler(FicheCompagnon fiche, long jour, Random hasard) {
		List<Moule> courts = new ArrayList<>();
		List<Moule> longs = new ArrayList<>();
		for (Moule moule : Moules.tous()) {
			if (!moule.concerne(fiche.espece())) {
				continue;
			}
			(moule.longue() ? longs : courts).add(moule);
		}

		Set<String> familles = new LinkedHashSet<>();
		for (int place = 0; place < COMBIEN; place++) {
			Moule choisi = tirer(courts, familles, hasard);
			if (choisi == null) {
				vider(fiche, place);
				continue;
			}
			familles.add(choisi.famille());
			poser(fiche, place, choisi, hasard.nextInt(choisi.crans()));
		}

		// La longue : seulement si la place est libre.
		if (fiche.compteur(CLE_MOULE + COMBIEN) == 0 && !longs.isEmpty()) {
			Moule choisi = longs.get(hasard.nextInt(longs.size()));
			poser(fiche, COMBIEN, choisi, choisi.crans() - 1);
		}

		fiche.poserCompteur(CLE_JOUR, (int) Math.max(0L, Math.min(Integer.MAX_VALUE, jour)));
		fiche.poserCompteur(CLE_REJETS, 0);
	}

	private static Moule tirer(List<Moule> parmi, Set<String> dejaVues, Random hasard) {
		List<Moule> possibles = new ArrayList<>(parmi.size());
		for (Moule moule : parmi) {
			if (!dejaVues.contains(moule.famille())) {
				possibles.add(moule);
			}
		}
		if (possibles.isEmpty()) {
			possibles = parmi;
		}
		return possibles.isEmpty() ? null : possibles.get(hasard.nextInt(possibles.size()));
	}

	private static void poser(FicheCompagnon fiche, int place, Moule moule, int cran) {
		fiche.poserCompteur(CLE_MOULE + place, empreinteDe(moule));
		fiche.poserCompteur(CLE_CRAN + place, cran);
		fiche.poserCompteur(CLE_DEPART + place, fiche.compteur(moule.compteur()));
		fiche.poserCompteur(CLE_FAITE + place, 0);
	}

	private static void vider(FicheCompagnon fiche, int place) {
		fiche.poserCompteur(CLE_MOULE + place, 0);
		fiche.poserCompteur(CLE_CRAN + place, 0);
		fiche.poserCompteur(CLE_DEPART + place, 0);
		fiche.poserCompteur(CLE_FAITE + place, 0);
	}

	/**
	 * Ecarte une mission et la remplace tout de suite.
	 *
	 * <p>Une seule par renouvellement. Sans cette limite, on retirerait jusqu'a
	 * tomber sur la plus facile, et le carnet ne voudrait plus rien dire.
	 *
	 * @return vrai si le remplacement a eu lieu
	 */
	public static boolean ecarter(FicheCompagnon fiche, int place, Random hasard) {
		if (place < 0 || place >= COMBIEN || fiche.compteur(CLE_REJETS) >= REJETS) {
			return false;
		}
		Ligne ligne = ligne(fiche, place);
		if (ligne == null || ligne.finie()) {
			return false;
		}

		Set<String> familles = new LinkedHashSet<>();
		for (int autre = 0; autre < COMBIEN; autre++) {
			if (autre == place) {
				continue;
			}
			Ligne voisine = ligne(fiche, autre);
			if (voisine != null && voisine.moule() != null) {
				familles.add(voisine.moule().famille());
			}
		}
		familles.add(ligne.moule().famille());

		List<Moule> courts = new ArrayList<>();
		for (Moule moule : Moules.tous()) {
			if (!moule.longue() && moule.concerne(fiche.espece())) {
				courts.add(moule);
			}
		}
		Moule choisi = tirer(courts, familles, hasard);
		if (choisi == null) {
			return false;
		}
		poser(fiche, place, choisi, hasard.nextInt(choisi.crans()));
		fiche.poserCompteur(CLE_REJETS, fiche.compteur(CLE_REJETS) + 1);
		return true;
	}

	/** Reste-t-il un droit d'ecarter une mission ? */
	public static boolean peutEcarter(FicheCompagnon fiche) {
		return fiche.compteur(CLE_REJETS) < REJETS;
	}
}
