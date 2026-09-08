package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.mission.Compteurs;
import fr.lhdp.compagnon.contenu.Bobo;
import fr.lhdp.compagnon.contenu.Contenu;
import fr.lhdp.compagnon.contenu.Donnable;
import fr.lhdp.compagnon.fiche.Barre;
import fr.lhdp.compagnon.fiche.FicheCompagnon;
import fr.lhdp.compagnon.fiche.Fiches;
import fr.lhdp.compagnon.fiche.Gouts;
import fr.lhdp.compagnon.fiche.Mode;
import fr.lhdp.compagnon.objet.Objets;
import fr.lhdp.compagnon.progression.Niveaux;
import fr.lhdp.compagnon.progression.Progression;
import fr.lhdp.compagnon.progression.SourceXp;
import fr.lhdp.compagnon.reseau.Reseau;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ce qui se passe quand un joueur clique droit sur un compagnon.
 *
 * <p>Tout se decide ici, cote serveur. Le partage des droits suit le cahier des
 * charges : les autres joueurs <b>voient</b> le compagnon et peuvent le
 * <b>caresser</b>, c'est ce qui fait vivre les couloirs. Ils ne peuvent pas le
 * nourrir, le soigner, ni lui donner un ordre.
 *
 * <p>Deux gestes differents sur le meme bouton :
 * <ul>
 *   <li><b>clic droit</b> a mains nues : l'ordre, qui tourne suit → assis →
 *       couche → suit, comme le loup</li>
 *   <li><b>accroupi + clic droit</b> : la caresse — le joueur se baisse, c'est le
 *       geste decrit pour l'etape de la caresse</li>
 * </ul>
 */
public final class Interactions {

	/** Complicite gagnee par caresse. Valeur inventee. */
	private static final float COMPLICITE_PAR_CARESSE = 2.0F;

	/** Petit bonus affectif d'un repas qu'il adore, sans accelerer fortement l'XP. */
	private static final float COMPLICITE_REPAS_PREFERE = 2.0F;

	/** Duree du geste de caresse, en ticks. Valeur inventee. */
	private static final int DUREE_CARESSE = 45;
	private static final Map<UUID, Long> CARESSES_EN_COURS = new HashMap<>();

	/**
	 * Distance maximale pour caresser, mesuree depuis la <b>boite de collision</b>
	 * du compagnon et non depuis son centre.
	 *
	 * <p>C'est ce qui compte : sinon un gros compagnon serait plus difficile a
	 * atteindre qu'un petit, alors qu'on est deja contre lui. Deux blocs depuis la
	 * boite font environ deux blocs et demi depuis son centre : a portee de bras,
	 * sans avoir a se coller pile sur son flanc.
	 */
	private static final double DISTANCE_CARESSE = 2.0D;

	/**
	 * Cosinus de l'angle permis entre le regard et le compagnon. 0,80 correspond
	 * a un cone d'environ 37 degres de part et d'autre. Valeur inventee.
	 */
	private static final double COSINUS_ANGLE = 0.80D;

	/**
	 * Combien de faim vaut un point de nourriture de Minecraft.
	 *
	 * <p>Trois : un steak (huit points) rend vingt-quatre de faim, une pomme
	 * doree douze. C'est du meme ordre qu'un aliment du mod, ce qui est voulu — la
	 * difference doit se jouer sur la complicite, pas sur le ventre.
	 */
	private static final float FAIM_PAR_POINT = 3.0F;

	/** Et ce qu'un point de saturation rend d'energie. Valeur inventee. */
	private static final float ENERGIE_PAR_SATURATION = 1.5F;

	private Interactions() {
	}

	public static InteractionResult cliqueDroit(CompagnonEntity compagnon,
			ServerPlayer joueur, InteractionHand main) {

		Fiches fiches = Fiches.de(joueur.server);
		FicheCompagnon fiche = fiches.get(compagnon.ficheId());
		if (fiche == null) {
			return InteractionResult.PASS;
		}

		boolean proprietaire = fiche.proprietaire().equals(joueur.getUUID());
		ItemStack pile = joueur.getItemInHand(main);

		if (!proprietaire) {
			// Un passant peut le caresser. Rien de plus, et ca ne compte pas dans
			// sa progression : sinon n'importe qui pourrait la faire monter.
			return caresser(compagnon, fiche, fiches, joueur, false);
		}

		if (pile.is(Objets.ALIMENT)) {
			return nourrir(compagnon, fiche, fiches, joueur, pile);
		}
		if (pile.is(Objets.SOIN)) {
			return soigner(compagnon, fiche, fiches, joueur, pile);
		}
		if (pile.has(DataComponents.FOOD)) {
			return nourrirVanilla(compagnon, fiche, fiches, joueur, pile);
		}
		if (joueur.isShiftKeyDown()) {
			return caresser(compagnon, fiche, fiches, joueur, true);
		}
		// Il rend ce qu'il portait. Avant le cycle des modes : quand une bete tient
		// quelque chose dans la gueule et qu'on tend la main, on veut l'objet, pas
		// la faire asseoir.
		if (pile.isEmpty() && !compagnon.lesMainsVides()) {
			return reprendre(compagnon, fiche, joueur);
		}
		// ON MONTE DESSUS.
		//
		// Avant le cycle des modes, et c'est un choix : sur une monture, la main
		// vide veut dire « je monte ». Les modes restent accessibles a la voix —
		// « assis », « reste » — qui est de toute facon la facon dont on parle a
		// sa bete dans ce mod.
		//
		// Les autres especes ne declarent pas monter_au_niveau : pour elles, cette
		// ligne ne fait rien et le clic droit continue de faire ce qu'il faisait.
		if (pile.isEmpty() && compagnon.seLaisseMonter(fiche.niveau(Niveaux.progression()))) {
			return monter(compagnon, fiche, joueur);
		}
		// Il garde ce qu'on lui confie. Un sac vivant : il transporte, il suit, il
		// rend. Rien d'autre, et une seule pile a la fois — ce n'est pas un coffre.
		if (!pile.isEmpty() && compagnon.lesMainsVides()) {
			return confier(compagnon, fiche, joueur, pile);
		}

		// EPAULE, PUIS TETE, PUIS SOL.
		//
		// Un seul bouton et trois positions faciles a retenir. La premiere fois,
		// il quitte l'epaule pour le sommet de la tete ; la suivante, il redescend.
		if (pile.isEmpty() && compagnon.estPerche()) {
			if (!compagnon.estSurLaTete()) {
				return monterSurLaTete(compagnon, fiche, joueur);
			}
			return descendreDeLEpaule(compagnon, fiche, joueur);
		}

		// IL MONTE SUR L'EPAULE.
		//
		// Seulement s'il est deja assis. On ne vole donc aucun geste : le clic a
		// main vide fait toujours tourner les ordres, et c'est le DEUXIEME clic —
		// celui qui ferait passer de « assis » a « couche » — qui le fait monter,
		// et seulement chez les petites betes.
		//
		// Ca se decouvre tout seul, et ca se lit comme une phrase : assieds-toi,
		// puis viens.
		if (pile.isEmpty() && fiche.mode() == Mode.ASSIS && compagnon.peutSePercher()) {
			return monterSurLEpaule(compagnon, fiche, fiches, joueur);
		}
		return ordre(compagnon, fiche, fiches, joueur);
	}

	// --- Nourrir ----------------------------------------------------------------

	private static InteractionResult nourrir(CompagnonEntity compagnon, FicheCompagnon fiche,
			Fiches fiches, ServerPlayer joueur, ItemStack pile) {

		String variete = pile.get(Objets.VARIETE);
		Donnable aliment = variete == null ? null : Contenu.aliment(variete);
		if (aliment == null) {
			echec(joueur, "Cet aliment n'existe pas dans les donnees du serveur.");
			return InteractionResult.FAIL;
		}

		if (fiche.barre(Barre.FAIM) >= Barre.MAXIMUM) {
			echec(joueur, fiche.nom() + " n'a plus faim.");
			return InteractionResult.FAIL;
		}

		Progression table = Niveaux.progression();
		Gouts.Avis avis = fiche.gouts().avis(aliment.id());
		appliquer(fiche, aliment.effets(), table);
		if (avis == Gouts.Avis.PREFERE) {
			fiche.ajouterBarre(Barre.COMPLICITE, COMPLICITE_REPAS_PREFERE, table);
		}
		fiche.incrementer(FicheCompagnon.REPAS);
		fiche.marquer("premier_repas", System.currentTimeMillis());
		if (avis == Gouts.Avis.PREFERE) {
			fiche.marquer("gout.aime." + aliment.id(), System.currentTimeMillis());
		} else if (avis == Gouts.Avis.BOUDE) {
			fiche.marquer("gout.boude." + aliment.id(), System.currentTimeMillis());
		}
		// La ou on le nourrit d habitude : c est son coin.
		retenirLeLieu(compagnon, fiche, "repas");
		// ET A QUELLE HEURE. Trois repas a la meme heure font une habitude, et
		// il ira attendre la, a cette heure, sans quon le lui ait demande.
		fiche.noterUnRepas(heureDuMonde(compagnon));

		int gagne = fiche.gagnerXp(SourceXp.SOINS, table, System.currentTimeMillis());
		leRepasSeVoit(compagnon, fiche, pile, avis);
		pile.shrink(1);
		fiches.setDirty();

		if (avis == Gouts.Avis.PREFERE) {
			dire(joueur, Component.translatable("repas.compagnon.prefere",
					fiche.nom(), aliment.nom()), gagne);
		} else if (avis == Gouts.Avis.BOUDE) {
			dire(joueur, Component.translatable("repas.compagnon.boude",
					fiche.nom(), aliment.nom()), gagne);
		} else {
			dire(joueur, fiche.nom() + " mange " + aliment.nom() + ".", gagne);
		}
		return InteractionResult.CONSUME;
	}

	/**
	 * Il mange aussi ce qui se mange dans Minecraft.
	 *
	 * <h2>Pourquoi c'etait a corriger</h2>
	 *
	 * <p>Il n'acceptait que les aliments du mod. Tendre un steak ou une pomme
	 * doree a son dragon ne faisait <b>rien du tout</b> — pas meme un message. Un
	 * joueur qui essaie la chose la plus evidente du monde et qui n'obtient aucune
	 * reaction croit le mod casse.
	 *
	 * <h2>Ce que ca vaut, et pourquoi c'est moins bien</h2>
	 *
	 * <p>La nourriture ordinaire <b>remplit le ventre, et rien d'autre</b> : de la
	 * faim et un peu d'energie, tirees des valeurs du jeu. Aucune complicite.
	 *
	 * <p>Les aliments du mod, eux, rapprochent. C'est ce qui garde un interet a les
	 * distribuer : un dragon nourri au steak est un dragon rassasie, un dragon
	 * nourri a la chocogrenouille est un dragon attache.
	 */
	private static InteractionResult nourrirVanilla(CompagnonEntity compagnon,
			FicheCompagnon fiche, Fiches fiches, ServerPlayer joueur, ItemStack pile) {

		FoodProperties nourriture = pile.get(DataComponents.FOOD);
		if (nourriture == null) {
			return InteractionResult.PASS;
		}
		if (fiche.barre(Barre.FAIM) >= Barre.MAXIMUM) {
			echec(joueur, fiche.nom() + " n'a plus faim.");
			return InteractionResult.FAIL;
		}

		// Le nom AVANT de consommer : une pile qui tombe a zero ne s'appelle plus
		// forcement pareil, et on l'annoncerait au joueur apres coup.
		String quoi = pile.getHoverName().getString();

		Map<Barre, Float> effets = new EnumMap<>(Barre.class);
		effets.put(Barre.FAIM, nourriture.nutrition() * FAIM_PAR_POINT);
		if (nourriture.saturation() > 0.0F) {
			effets.put(Barre.ENERGIE, nourriture.saturation() * ENERGIE_PAR_SATURATION);
		}

		Progression table = Niveaux.progression();
		appliquer(fiche, effets, table);
		fiche.incrementer(FicheCompagnon.REPAS);
		fiche.marquer("premier_repas", System.currentTimeMillis());
		// La ou on le nourrit d habitude : c est son coin.
		retenirLeLieu(compagnon, fiche, "repas");
		// ET A QUELLE HEURE. Trois repas a la meme heure font une habitude, et
		// il ira attendre la, a cette heure, sans quon le lui ait demande.
		fiche.noterUnRepas(heureDuMonde(compagnon));

		int gagne = fiche.gagnerXp(SourceXp.SOINS, table, System.currentTimeMillis());
		leRepasSeVoit(compagnon, fiche, pile, Gouts.Avis.ORDINAIRE);
		pile.shrink(1);
		fiches.setDirty();

		dire(joueur, fiche.nom() + " mange " + quoi + ".", gagne);
		return InteractionResult.CONSUME;
	}

	/**
	 * Mange de lui-meme le repas pose dans une gamelle.
	 *
	 * <p>C'est le meme repas que lorsqu'un joueur lui tend l'objet : memes barres,
	 * memes gouts, memes miettes et meme progression quotidienne. La difference
	 * est seulement le geste. Le lieu memorise est le bol et non la position un
	 * peu approximative de la creature, afin qu'elle puisse revenir exactement a
	 * son coin au prochain repas.
	 *
	 * @return vrai si un aliment a bien ete consomme
	 */
	public static boolean mangerDepuisGamelle(CompagnonEntity compagnon,
			FicheCompagnon fiche, Fiches fiches, ItemStack pile, BlockPos gamelle) {
		if (fiche == null || pile.isEmpty() || fiche.barre(Barre.FAIM) >= Barre.MAXIMUM) {
			return false;
		}

		Progression table = Niveaux.progression();
		long maintenant = System.currentTimeMillis();
		Gouts.Avis avis = Gouts.Avis.ORDINAIRE;
		if (pile.is(Objets.ALIMENT)) {
			String variete = pile.get(Objets.VARIETE);
			Donnable aliment = variete == null ? null : Contenu.aliment(variete);
			if (aliment == null) {
				return false;
			}
			avis = fiche.gouts().avis(aliment.id());
			appliquer(fiche, aliment.effets(), table);
			if (avis == Gouts.Avis.PREFERE) {
				fiche.ajouterBarre(Barre.COMPLICITE, COMPLICITE_REPAS_PREFERE, table);
				fiche.marquer("gout.aime." + aliment.id(), maintenant);
			} else if (avis == Gouts.Avis.BOUDE) {
				fiche.marquer("gout.boude." + aliment.id(), maintenant);
			}
		} else {
			FoodProperties nourriture = pile.get(DataComponents.FOOD);
			if (nourriture == null) {
				return false;
			}
			Map<Barre, Float> effets = new EnumMap<>(Barre.class);
			effets.put(Barre.FAIM, nourriture.nutrition() * FAIM_PAR_POINT);
			if (nourriture.saturation() > 0.0F) {
				effets.put(Barre.ENERGIE,
						nourriture.saturation() * ENERGIE_PAR_SATURATION);
			}
			appliquer(fiche, effets, table);
		}

		fiche.incrementer(FicheCompagnon.REPAS);
		fiche.marquer("premier_repas", maintenant);
		fiche.marquer("premier_repas_gamelle", maintenant);
		fiche.retenirLeLieu("repas", gamelle.getX(), gamelle.getY(), gamelle.getZ(), maintenant);
		fiche.noterUnRepas(heureDuMonde(compagnon));
		fiche.gagnerXp(SourceXp.SOINS, table, maintenant);
		leRepasSeVoit(compagnon, fiche, pile, avis);
		pile.shrink(1);
		fiches.setDirty();
		return true;
	}

	// --- Le sac vivant ----------------------------------------------------------

	/**
	 * Il prend l'objet qu'on lui tend et le garde dans la gueule.
	 *
	 * <p>Une seule pile a la fois : c'est une bete, pas un coffre. Et il la porte
	 * pour de vrai — on la voit dans sa gueule, et elle retombe au sol si l'entite
	 * disparait, jamais dans le neant.
	 */
	private static InteractionResult confier(CompagnonEntity compagnon, FicheCompagnon fiche,
			ServerPlayer joueur, ItemStack pile) {

		ItemStack confiee = pile.copy();
		confiee.setCount(1);
		compagnon.prendreDansLaGueule(confiee);
		pile.shrink(1);

		dire(joueur, fiche.nom() + " prend " + confiee.getHoverName().getString()
				+ " dans sa gueule.", 0);
		return InteractionResult.CONSUME;
	}

	/**
	 * Il rend ce qu'il portait, dans la main.
	 *
	 * <p>Dans l'inventaire et non par terre : ici c'est un echange, on tend la main
	 * et il donne. Le lacher au sol, c'est ce que fait l'ordre « lache ».
	 */
	private static InteractionResult reprendre(CompagnonEntity compagnon, FicheCompagnon fiche,
			ServerPlayer joueur) {

		ItemStack rendue = compagnon.porte().copy();
		if (!joueur.getInventory().add(rendue)) {
			// Inventaire plein : il la pose plutot que de la faire disparaitre.
			compagnon.poserCeQuIlPorte();
			echec(joueur, "Ton inventaire est plein — " + fiche.nom() + " l'a posee.");
			return InteractionResult.CONSUME;
		}
		compagnon.viderLaGueule();
		dire(joueur, fiche.nom() + " te rend " + rendue.getHoverName().getString() + ".", 0);
		return InteractionResult.CONSUME;
	}

	// --- Soigner ----------------------------------------------------------------

	/** Il retient l'endroit ou il se trouve, sous cette etiquette-la. */
	/**
	 * Lheure du monde, de 0 a 23.
	 *
	 * <p>Minecraft compte 24 000 ticks par jour et place minuit a 18 000 : on
	 * decale donc de six heures avant de diviser, sinon toutes les heures
	 * seraient fausses de six.
	 */
	private static int heureDuMonde(CompagnonEntity compagnon) {
		return (int) ((compagnon.level().getDayTime() / 1000L + 6L) % 24L);
	}

	private static void retenirLeLieu(CompagnonEntity compagnon, FicheCompagnon fiche,
			String cle) {
		fiche.retenirLeLieu(cle,
			net.minecraft.util.Mth.floor(compagnon.getX()),
			net.minecraft.util.Mth.floor(compagnon.getY()),
			net.minecraft.util.Mth.floor(compagnon.getZ()),
			System.currentTimeMillis());
	}

	/**
	 * Le proprietaire monte sur sa bete.
	 *
	 * <h2>Pourquoi au niveau maximum</h2>
	 *
	 * <p>Monter sa bete doit etre l'aboutissement de tout le reste, pas une chose
	 * qu'on fait le premier jour. C'est la seule recompense du mod qui change la
	 * facon de se deplacer sur le serveur, et elle se merite.
	 *
	 * <p>Le niveau exige est dans la fiche d'espece, pas ici : une autre monture
	 * pourra demander autre chose.
	 *
	 * <p>Il ne se laisse pas monter s'il porte quelque chose : on ne s'assoit pas
	 * sur une bete qui a les pattes prises.
	 */
	private static InteractionResult monter(CompagnonEntity compagnon, FicheCompagnon fiche,
			ServerPlayer joueur) {

		if (compagnon.isVehicle()) {
			echec(joueur, "Quelqu'un est deja dessus.");
			return InteractionResult.FAIL;
		}
		if (fiche.barre(Barre.ENERGIE) < ENERGIE_POUR_PORTER) {
			echec(joueur, fiche.nom() + " est trop fatigue pour te porter.");
			return InteractionResult.FAIL;
		}

		// IL NE PORTE PERSONNE QUAND IL A MAL.
		//
		// C'est la seule chose du mod qu'un bobo empeche vraiment de faire, et
		// c'est voulu : le reste continue, mais monter sur une bete blessee est
		// justement ce qu'on ne fait pas. Ca donne un motif clair de la soigner.
		if (fiche.aUnBobo()) {
			joueur.displayClientMessage(net.minecraft.network.chat.Component.translatable(
				"monte.compagnon.blesse", fiche.nom()), true);
			return InteractionResult.FAIL;
		}

		// Il se leve : on ne monte pas sur une bete assise, et le laisser assis
		// donnerait un cavalier qui flotte au-dessus d'un oiseau couche.
		compagnon.appliquerMode(Mode.RESTE);
		joueur.startRiding(compagnon);
		Compteurs.compter(compagnon, Compteurs.MONTES);
		dire(joueur, "Tu montes sur " + fiche.nom()
				+ ". Espace pour monter, lache pour planer, accroupi pour descendre.", 0);
		return InteractionResult.CONSUME;
	}

	/** En dessous, il n'a plus la force de porter quelqu'un. */
	private static final float ENERGIE_POUR_PORTER = 20.0F;

	private static InteractionResult soigner(CompagnonEntity compagnon, FicheCompagnon fiche,
			Fiches fiches, ServerPlayer joueur, ItemStack pile) {

		String variete = pile.get(Objets.VARIETE);
		Donnable soin = variete == null ? null : Contenu.soin(variete);
		if (soin == null) {
			echec(joueur, "Ce remede n'existe pas dans les donnees du serveur.");
			return InteractionResult.FAIL;
		}

		if (!fiche.aUnBobo()) {
			echec(joueur, fiche.nom() + " n'a rien a soigner.");
			return InteractionResult.FAIL;
		}

		Bobo bobo = Contenu.bobo(fiche.bobo());
		if (bobo != null && !bobo.soignePar().equals(variete)) {
			echec(joueur, "Ce n'est pas ce qu'il lui faut. Il lui faut : " + bobo.soignePar() + ".");
			return InteractionResult.FAIL;
		}

		Progression table = Niveaux.progression();
		appliquer(fiche, soin.effets(), table);
		fiche.guerir();
		fiche.incrementer(FicheCompagnon.SOINS);
		fiche.marquer("premier_soin", System.currentTimeMillis());
		// IL SE SOUVIENDRA D'ICI.
		//
		// Pas du chemin — celui-la coute cher et ne se voit pas. De l'endroit. Il
		// y repassera un jour, il ralentira, il regardera. Rien ne sera ecrit :
		// c'est au joueur de se souvenir de ce qui s'y est passe, et il s'en
		// souvient, parce que c'est lui qui y etait.
		retenirLeLieu(compagnon, fiche, "soigne");

		int gagne = fiche.gagnerXp(SourceXp.SOINS, table, System.currentTimeMillis());
		leSoinSeVoit(compagnon, pile);
		pile.shrink(1);
		fiches.setDirty();

		dire(joueur, fiche.nom() + " est soigne.", gagne);
		return InteractionResult.CONSUME;
	}

	/**
	 * Ce qu'on voit et ce qu'on entend quand il mange.
	 *
	 * <p>Nourrir sa bete est le geste qu'on repete le plus souvent dans ce mod,
	 * et c'etait le plus silencieux : la barre montait dans un fichier, un
	 * message passait au-dessus de la barre d'objets, et rien du tout ne se
	 * passait <b>sur la bete</b>. On nourrissait un tableau.
	 *
	 * <p>Le jeu, lui, fait machouiller et recrache des miettes de l'objet donne.
	 * On fait pareil, parce que c'est ce que tout le monde attend deja.
	 *
	 * <p>A appeler <b>avant</b> d'entamer la pile : une pile tombee a zero n'a
	 * plus de texture a montrer.
	 */
	private static void leRepasSeVoit(CompagnonEntity compagnon, FicheCompagnon fiche,
			ItemStack pile, Gouts.Avis avis) {

		// Si l'espece sait manger, elle joue toute l'animation ecrite dans son
		// fichier. Une espece sans ce role garde simplement les miettes et le son.
		compagnon.jouerActionPendant("@mange", 30);
		Etincelles.miettes(compagnon, pile);
		Sons.jouerCeSon(compagnon, net.minecraft.sounds.SoundEvents.GENERIC_EAT, 1.0F);
		if (avis == Gouts.Avis.PREFERE) {
			Sons.jouer(compagnon, Sons.CONTENT);
			Etincelles.coeurs(compagnon, 4);
		} else if (avis == Gouts.Avis.BOUDE) {
			Etincelles.soupir(compagnon);
		}

		// RASSASIE, IL LE DIT.
		//
		// C'est le seul moyen de savoir qu'on peut arreter de le nourrir sans
		// ouvrir son livre. Avant, on le decouvrait en recevant « il n'a plus
		// faim » — c'est-a-dire une fois de trop.
		if (fiche.barre(Barre.FAIM) >= Barre.MAXIMUM && avis != Gouts.Avis.PREFERE) {
			Sons.jouer(compagnon, Sons.CONTENT);
			Etincelles.coeurs(compagnon, 3);
		}
	}

	/** Ce qu'on voit quand on le soigne : des coeurs, et un bruit content. */
	private static void leSoinSeVoit(CompagnonEntity compagnon, ItemStack pile) {
		Etincelles.miettes(compagnon, pile);
		Etincelles.coeurs(compagnon, 4);
		Sons.jouer(compagnon, Sons.CONTENT);
	}

	// --- Caresser ---------------------------------------------------------------

	private static InteractionResult caresser(CompagnonEntity compagnon, FicheCompagnon fiche,
			Fiches fiches, ServerPlayer joueur, boolean proprietaire) {

		// On ne caresse pas quelqu'un a bout de bras. Il faut etre contre lui et
		// le regarder, sinon le geste n'a aucun sens a l'ecran.
		AABB portee = compagnon.getBoundingBox().inflate(DISTANCE_CARESSE);
		if (!portee.contains(joueur.position()) && !portee.contains(joueur.getEyePosition())) {
			echec(joueur, Component.translatable("caresse.compagnon.trop_loin", fiche.nom()));
			return InteractionResult.FAIL;
		}
		if (!regardeVers(joueur, compagnon)) {
			echec(joueur, Component.translatable("caresse.compagnon.pas_en_face", fiche.nom()));
			return InteractionResult.FAIL;
		}
		long maintenant = System.currentTimeMillis();
		if (CARESSES_EN_COURS.getOrDefault(joueur.getUUID(), 0L) > maintenant) {
			echec(joueur, Component.translatable("caresse.compagnon.en_cours"));
			return InteractionResult.FAIL;
		}
		CARESSES_EN_COURS.put(joueur.getUUID(), maintenant + DUREE_CARESSE * 50L);

		// Le compagnon reagit pour tout le monde : c'est ce qu'on voit dans les
		// couloirs. Seul le proprietaire fait monter la complicite.
		compagnon.reagirCaresse();
		Reseau.diffuserCaresse(joueur, compagnon, DUREE_CARESSE);

		// Il retient qui s'occupe de lui, meme si ce n'est pas son maitre. Au bout
		// de quelques fois, cette personne cesse d'etre une inconnue.
		fiche.seSouvenirDe(joueur.getUUID());
		compagnon.seSouvenirDe(joueur.getUUID(), fiche.connaissance(joueur.getUUID()));
		fiches.setDirty();

		if (!proprietaire) {
			return InteractionResult.SUCCESS;
		}

		Progression table = Niveaux.progression();
		fiche.ajouterBarre(Barre.COMPLICITE, COMPLICITE_PAR_CARESSE, table);
		fiche.incrementer(FicheCompagnon.CARESSES);
		// LE GESTE LE PLUS FREQUENT DU MOD ne produisait aucun son et aucune
		// particule. C'est celui qui merite le plus d'en avoir.
		fr.lhdp.compagnon.entite.Sons.jouer(compagnon, fr.lhdp.compagnon.entite.Sons.CONTENT);
		fr.lhdp.compagnon.entite.Etincelles.coeurs(compagnon, 5);
		// UNE CARESSE D'UN AUTRE QUE SON MAITRE ne compte pas pareil : c'est
		// elle qui fait les missions sociales, et elle seule.
		if (!joueur.getUUID().equals(fiche.proprietaire())) {
			fiche.incrementer(Compteurs.CARESSES_AMIS);
		}
		fiche.marquer("premiere_caresse", maintenant);

		int gagne = fiche.gagnerXp(SourceXp.AFFECTION, table, maintenant);
		fiches.setDirty();

		dire(joueur, fiche.nom() + " se laisse faire.", gagne);
		return InteractionResult.SUCCESS;
	}

	// --- Le perchoir d'epaule ---------------------------------------------------

	/**
	 * Il grimpe sur ton epaule.
	 *
	 * <p>C'est le geste du perroquet, et c'est celui qu'on attend d'une petite
	 * bete : elle ne te suit plus, elle est <b>avec</b> toi. Dans un couloir
	 * plein de monde, c'est la seule facon de ne pas la perdre.
	 */
	private static InteractionResult monterSurLEpaule(CompagnonEntity compagnon,
			FicheCompagnon fiche, Fiches fiches, ServerPlayer joueur) {

		if (compagnon.isVehicle()) {
			echec(joueur, "Quelqu'un est deja dessus.");
			return InteractionResult.FAIL;
		}
		// UNE SEULE A LA FOIS. Deux betes au meme endroit se traversent, et on
		// ne voit plus qu'un tas.
		for (net.minecraft.world.entity.Entity deja : joueur.getPassengers()) {
			if (deja instanceof CompagnonEntity) {
				echec(joueur, "Tu en portes deja un.");
				return InteractionResult.FAIL;
			}
		}

		// Il se releve avant de grimper : une bete assise sur une epaule est une
		// bete assise dans le vide.
		fiche.setMode(Mode.SUIT);
		compagnon.appliquerMode(Mode.SUIT);
		compagnon.sePoserSurLaTete(false);
		fiches.setDirty();

		compagnon.startRiding(joueur, true);
		Sons.jouer(compagnon, Sons.CONTENT, 0.8F);
		Etincelles.coeurs(compagnon, 3);
		dire(joueur, fiche.nom() + " grimpe sur ton epaule.", 0);
		return InteractionResult.CONSUME;
	}

	/** De l'epaule au sommet de la tete, sans descendre du joueur. */
	private static InteractionResult monterSurLaTete(CompagnonEntity compagnon,
			FicheCompagnon fiche, ServerPlayer joueur) {

		compagnon.sePoserSurLaTete(true);
		Sons.jouer(compagnon, Sons.CONTENT, 0.65F);
		dire(joueur, fiche.nom() + " se pose sur ta tete.", 0);
		return InteractionResult.CONSUME;
	}

	/** Il redescend, et se remet a te suivre. */
	private static InteractionResult descendreDeLEpaule(CompagnonEntity compagnon,
			FicheCompagnon fiche, ServerPlayer joueur) {

		compagnon.sePoserSurLaTete(false);
		compagnon.stopRiding();
		dire(joueur, fiche.nom() + " redescend.", 0);
		return InteractionResult.CONSUME;
	}

	// --- Ordonner ---------------------------------------------------------------

	private static InteractionResult ordre(CompagnonEntity compagnon, FicheCompagnon fiche,
			Fiches fiches, ServerPlayer joueur) {

		Mode suivant = fiche.mode().suivant();
		fiche.setMode(suivant);
		compagnon.appliquerMode(suivant);
		fiches.setDirty();

		// LE NOM BRUT DE L ORDRE S AFFICHAIT.
		//
		// « Plume : couche », sans accent, en minuscules, parce que c etait le nom
		// de la valeur dans le code. La cle de traduction existait pourtant deja et
		// servait dans le livre : c est le meme ordre, il se lit desormais pareil
		// aux deux endroits.
		joueur.displayClientMessage(Component.translatable("ordre.compagnon.mode",
				fiche.nom(), Component.translatable(suivant.cleDeTraduction())), true);
		// Un petit bruit content : il a entendu. Sans lui, on ne sait pas si le
		// clic est passe tant qu on ne l a pas regarde s asseoir.
		Sons.jouer(compagnon, Sons.CONTENT, 0.5F);
		return InteractionResult.SUCCESS;
	}

	// --- Outils -----------------------------------------------------------------

	/**
	 * Vrai si le joueur regarde reellement le compagnon.
	 *
	 * <p>On compare la direction du regard avec la direction du compagnon. Au-dela
	 * de l'angle permis, il est peut-etre a cote, mais il ne le regarde pas — et
	 * on ne caresse pas quelque chose qu'on ne regarde pas.
	 */
	private static boolean regardeVers(ServerPlayer joueur, CompagnonEntity compagnon) {
		Vec3 vers = compagnon.position()
				.add(0.0D, compagnon.getBbHeight() / 2.0D, 0.0D)
				.subtract(joueur.getEyePosition());
		if (vers.lengthSqr() < 1.0E-4D) {
			return true;
		}
		return joueur.getLookAngle().normalize().dot(vers.normalize()) >= COSINUS_ANGLE;
	}

	private static void appliquer(FicheCompagnon fiche, Map<Barre, Float> effets, Progression table) {
		effets.forEach((barre, valeur) -> fiche.ajouterBarre(barre, valeur, table));
	}

	private static void dire(ServerPlayer joueur, String message, int xpGagne) {
		dire(joueur, Component.literal(message), xpGagne);
	}

	private static void dire(ServerPlayer joueur, Component message, int xpGagne) {
		Component sortie = xpGagne > 0
				? message.copy().append(Component.literal(" (+" + xpGagne + " xp)"))
				: message;
		joueur.displayClientMessage(sortie, true);
	}

	private static void echec(ServerPlayer joueur, String message) {
		echec(joueur, Component.literal(message));
	}

	private static void echec(ServerPlayer joueur, Component message) {
		joueur.displayClientMessage(message.copy().withStyle(ChatFormatting.GRAY), true);
	}
}
