package fr.lhdp.compagnon.reseau;

import fr.lhdp.compagnon.espece.Longueurs;
import fr.lhdp.compagnon.classement.Classement;
import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.entite.CompagnonEntity;
import fr.lhdp.compagnon.entite.Obeissance;
import fr.lhdp.compagnon.espece.Especes;
import fr.lhdp.compagnon.fiche.Barre;
import fr.lhdp.compagnon.fiche.FicheCompagnon;
import fr.lhdp.compagnon.fiche.Fiches;
import fr.lhdp.compagnon.fiche.Invocation;
import fr.lhdp.compagnon.competence.Competence;
import fr.lhdp.compagnon.competence.Competences;
import fr.lhdp.compagnon.fiche.Jauges;
import fr.lhdp.compagnon.reglage.Reglages;
import fr.lhdp.compagnon.livre.DonneesLivre;
import fr.lhdp.compagnon.livre.EntreeCarnet;
import fr.lhdp.compagnon.livre.EntreeRoue;
import fr.lhdp.compagnon.objet.Objets;
import fr.lhdp.compagnon.objet.Origine;
import fr.lhdp.compagnon.progression.Niveaux;
import fr.lhdp.compagnon.voix.Voix;
import fr.lhdp.compagnon.progression.Progression;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Les paquets du mod, et leur traitement cote serveur.
 *
 * <p>Regle unique et sans exception : <b>le client propose, le serveur dispose.</b>
 * Tout ce qui arrive d'un paquet est traite comme une demande a verifier, jamais
 * comme un fait acquis.
 */
public final class Reseau {

	/** Combien de temps la bete se fait voir quand on ouvre sa roue. */
	private static final int TICKS_DE_LUEUR = 20 * 4;

	/** Combien de temps une animation demandee reste jouee. Valeur inventee. */
	private static final int DUREE_ACTION = 100;

	/**
	 * Un apercu dure moins longtemps : on survole une case, puis une autre.
	 *
	 * <p>Assez court pour qu'il redevienne lui-meme des qu'on quitte la case, assez
	 * long pour qu'on voie de quoi il s'agit.
	 */
	private static final int DUREE_APERCU = 45;

	/** En dessous de tant de blocs, on considere qu'il est avec toi. */
	private static final int AVEC_TOI = 16;

	private Reseau() {
	}

	/** A appeler des les deux cotes : les types doivent etre connus des deux. */
	public static void enregistrerTypes() {
		PayloadTypeRegistry.playC2S().register(PaquetNaissance.TYPE, PaquetNaissance.CODEC);
		PayloadTypeRegistry.playS2C().register(PaquetDemandeNom.TYPE, PaquetDemandeNom.CODEC);
		PayloadTypeRegistry.playS2C().register(PaquetOuvrirRemise.TYPE, PaquetOuvrirRemise.CODEC);
		PayloadTypeRegistry.playS2C().register(PaquetAncrages.TYPE, PaquetAncrages.CODEC);
		PayloadTypeRegistry.playS2C().register(PaquetOuvrirAncrage.TYPE, PaquetOuvrirAncrage.CODEC);
		PayloadTypeRegistry.playC2S().register(PaquetPoserAncrage.TYPE, PaquetPoserAncrage.CODEC);
		PayloadTypeRegistry.playC2S().register(PaquetRemettre.TYPE, PaquetRemettre.CODEC);
		PayloadTypeRegistry.playS2C().register(PaquetEspeces.TYPE, PaquetEspeces.CODEC);
		PayloadTypeRegistry.playC2S().register(PaquetLivre.TYPE, PaquetLivre.CODEC);
		PayloadTypeRegistry.playS2C().register(PaquetDonneesLivre.TYPE, PaquetDonneesLivre.CODEC);
		PayloadTypeRegistry.playC2S().register(PaquetRoue.TYPE, PaquetRoue.CODEC);
		PayloadTypeRegistry.playS2C().register(PaquetDonneesRoue.TYPE, PaquetDonneesRoue.CODEC);
		PayloadTypeRegistry.playC2S().register(PaquetAction.TYPE, PaquetAction.CODEC);
		PayloadTypeRegistry.playS2C().register(PaquetCaresse.TYPE, PaquetCaresse.CODEC);
		PayloadTypeRegistry.playC2S().register(PaquetCarnet.TYPE, PaquetCarnet.CODEC);
		PayloadTypeRegistry.playC2S().register(PaquetInvoquer.TYPE, PaquetInvoquer.CODEC);
		PayloadTypeRegistry.playC2S().register(PaquetApprendre.TYPE, PaquetApprendre.CODEC);
		PayloadTypeRegistry.playC2S().register(PaquetCompetence.TYPE, PaquetCompetence.CODEC);
		PayloadTypeRegistry.playC2S().register(PaquetChevaucher.TYPE, PaquetChevaucher.CODEC);
		PayloadTypeRegistry.playS2C().register(PaquetDonneesCarnet.TYPE, PaquetDonneesCarnet.CODEC);
		PayloadTypeRegistry.playS2C().register(PaquetJauges.TYPE, PaquetJauges.CODEC);
		PayloadTypeRegistry.playS2C().register(PaquetMontee.TYPE, PaquetMontee.CODEC);
		PayloadTypeRegistry.playC2S().register(PaquetClassement.TYPE, PaquetClassement.CODEC);
		PayloadTypeRegistry.playC2S().register(PaquetEcarter.TYPE, PaquetEcarter.CODEC);
		PayloadTypeRegistry.playS2C().register(PaquetDonneesClassement.TYPE,
			PaquetDonneesClassement.CODEC);
	}

	/** A appeler cote serveur seulement. */
	public static void enregistrerReceptions() {
		ServerPlayNetworking.registerGlobalReceiver(PaquetNaissance.TYPE, (paquet, contexte) ->
				contexte.server().execute(() -> naissance(contexte.player(), paquet.nom())));

		ServerPlayNetworking.registerGlobalReceiver(PaquetLivre.TYPE, (paquet, contexte) ->
				contexte.server().execute(() -> ouvrirLivre(contexte.player(), paquet.index())));

		ServerPlayNetworking.registerGlobalReceiver(PaquetRoue.TYPE, (paquet, contexte) ->
				contexte.server().execute(() -> ouvrirRoue(contexte.player(), paquet.index())));

		ServerPlayNetworking.registerGlobalReceiver(PaquetCarnet.TYPE, (paquet, contexte) ->
				contexte.server().execute(() -> ouvrirCarnet(contexte.player(), paquet.choisi())));

		ServerPlayNetworking.registerGlobalReceiver(PaquetPoserAncrage.TYPE, (paquet, contexte) ->
				contexte.server().execute(() -> poserUnAncrage(contexte.player(), paquet)));

		ServerPlayNetworking.registerGlobalReceiver(PaquetRemettre.TYPE, (paquet, contexte) ->
				contexte.server().execute(() ->
					remettreUnOeuf(contexte.player(), paquet.joueur(),
						paquet.espece(), paquet.variante())));

		ServerPlayNetworking.registerGlobalReceiver(PaquetInvoquer.TYPE, (paquet, contexte) ->
				contexte.server().execute(() ->
					invoquer(contexte.player(), paquet.index(), paquet.sortir())));

		ServerPlayNetworking.registerGlobalReceiver(PaquetChevaucher.TYPE, (paquet, contexte) ->
				contexte.server().execute(() -> chevaucher(contexte.player(), paquet.vers())));

		ServerPlayNetworking.registerGlobalReceiver(PaquetClassement.TYPE, (paquet, contexte) ->
				contexte.server().execute(() -> ouvrirLeClassement(contexte.player())));

		ServerPlayNetworking.registerGlobalReceiver(PaquetEcarter.TYPE, (paquet, contexte) ->
				contexte.server().execute(() ->
					ecarterUneMission(contexte.player(), paquet.index(), paquet.place())));

		ServerPlayNetworking.registerGlobalReceiver(PaquetCompetence.TYPE, (paquet, contexte) ->
				contexte.server().execute(() ->
					prendreUneCompetence(contexte.player(), paquet.index(), paquet.id())));

		ServerPlayNetworking.registerGlobalReceiver(PaquetApprendre.TYPE, (paquet, contexte) ->
				contexte.server().execute(() -> apprendre(contexte.player(),
						paquet.index(), paquet.animation(), paquet.mot())));

		ServerPlayNetworking.registerGlobalReceiver(PaquetAction.TYPE, (paquet, contexte) ->
				contexte.server().execute(() ->
						jouerAction(contexte.player(), paquet.index(), paquet.nom(),
								paquet.apercu())));

		// Ce crochet se declenche a la connexion d'un joueur ET apres chaque
		// /reload : un seul endroit pour les deux cas, donc aucun risque d'en
		// oublier un.
		ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((joueur, connexion) ->
				envoyerEspeces(joueur));

		// Le souvenir de ce qu'on a envoye au panneau part avec le joueur : sinon
		// il resterait une entree par joueur ayant joue depuis le demarrage.
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT
				.register((gestionnaire, serveur) ->
						Jauges.oublier(gestionnaire.getPlayer().getUUID()));
	}

	/**
	 * Repond a une demande d'ouverture du livre.
	 *
	 * <p>Le client n'envoie qu'un numero. Le serveur va chercher les fiches du
	 * joueur qui demande, jamais celles d'un autre : on ne peut pas lire le livre
	 * du voisin.
	 */
	private static void ouvrirLivre(ServerPlayer joueur, int indexDemande) {
		List<FicheCompagnon> siennes = Fiches.de(joueur.server).duProprietaire(joueur.getUUID());

		if (siennes.isEmpty()) {
			joueur.displayClientMessage(
					Component.translatable("compagnon.aucun"), true);
			return;
		}

		// On borne le numero recu au lieu de faire confiance au client.
		int index = Math.floorMod(indexDemande, siennes.size());
		FicheCompagnon fiche = siennes.get(index);

		// CELUI DONT ON OUVRE LE LIVRE SE FAIT VOIR.
		//
		// Meme raison que pour la roue : dans une cour a deux cents compagnons,
		// lire une fiche sans savoir laquelle des betes on lit ne sert a rien.
		net.minecraft.server.level.ServerLevel la = joueur.server.getLevel(fiche.dimension());
		if (la != null && la.getEntity(fiche.id()) instanceof CompagnonEntity celui) {
			celui.brillerPendant(TICKS_DE_LUEUR);
		}

		List<String> noms = new ArrayList<>(siennes.size());
		for (FicheCompagnon sienne : siennes) {
			noms.add(sienne.nom());
		}

		ServerPlayNetworking.send(joueur, new PaquetDonneesLivre(
				siennes.size(), index,
				DonneesLivre.de(fiche, Niveaux.progression(), joueur.server),
				List.copyOf(noms)));
	}

	/**
	 * Envoie la liste des compagnons de ce joueur.
	 *
	 * <p>Les siens, et rien que les siens : on ne regarde pas le carnet du
	 * voisin, et le numero recu du client est ramene dans ses bornes plutot que
	 * cru sur parole.
	 */
	/**
	 * Envoie le classement au joueur qui le demande.
	 *
	 * <p>Le refus est traite ici et pas sur le client : c'est le serveur qui
	 * sait si le staff a ferme le tableau, et lui seul. Un client a jour ou pas
	 * ne peut rien forcer.
	 */
	/**
	 * Remplace une mission par une autre, si le droit est encore la.
	 *
	 * <p>Le serveur decide de tout : le droit de rejet, la mission tiree, et
	 * meme le fait que cette bete appartienne bien a ce joueur. Un client qui
	 * enverrait n'importe quoi n'obtiendrait rien de plus qu'un livre a jour.
	 */
	private static void ecarterUneMission(ServerPlayer joueur, int index, int place) {
		List<FicheCompagnon> siennes =
			Fiches.de(joueur.server).duProprietaireSansCopie(joueur.getUUID());
		if (siennes.isEmpty()) {
			return;
		}
		FicheCompagnon fiche = siennes.get(Math.floorMod(index, siennes.size()));
		if (fr.lhdp.compagnon.mission.Carnet.ecarter(fiche, place, HASARD)) {
			Fiches.de(joueur.server).setDirty();
		} else {
			joueur.displayClientMessage(
				Component.translatable("livre.compagnon.plus_decarter"), true);
		}
		// Dans les deux cas on renvoie le livre : c'est lui qui fait foi, et le
		// client n'a rien a deviner.
		ouvrirLivre(joueur, index);
	}

	/** Pour les tirages du serveur. Un seul, partage : il n'y a qu'un fil. */
	private static final java.util.Random HASARD = new java.util.Random();

	private static void ouvrirLeClassement(ServerPlayer joueur) {
		if (!Classement.ouvert()) {
			joueur.displayClientMessage(
				Component.translatable("classement.compagnon.ferme"), true);
			return;
		}
		Classement.Extrait extrait = Classement.pour(joueur.server, joueur);
		ServerPlayNetworking.send(joueur, new PaquetDonneesClassement(
			extrait.lignes(), extrait.monRang(), extrait.participants(),
			extrait.debutVoisinage()));
	}

	private static void ouvrirCarnet(ServerPlayer joueur, int choisiDemande) {
		List<FicheCompagnon> siennes = Fiches.de(joueur.server).duProprietaire(joueur.getUUID());

		if (siennes.isEmpty()) {
			joueur.displayClientMessage(Component.translatable("compagnon.aucun"), true);
			return;
		}

		Progression table = Niveaux.progression();
		List<EntreeCarnet> entrees = new ArrayList<>(siennes.size());
		for (FicheCompagnon fiche : siennes) {
			entrees.add(new EntreeCarnet(fiche.nom(), fiche.espece(), fiche.variante(),
					fiche.niveau(table), fiche.sorti(), ouEstIl(joueur, fiche)));
		}

		ServerPlayNetworking.send(joueur, new PaquetDonneesCarnet(
				Math.floorMod(choisiDemande, siennes.size()), List.copyOf(entrees)));
	}

	/**
	 * Ou se trouve cette bete, en une phrase.
	 *
	 * <h2>Pourquoi ca valait la peine</h2>
	 *
	 * <p>Le carnet disait « il est range » ou « il est avec toi », et rien
	 * entre les deux. Or entre les deux il y a le cas le plus frequent : il est
	 * dehors, quelque part, et on ne sait pas ou. Toute l'information etait deja
	 * dans la fiche — elle n'etait simplement pas affichee.
	 *
	 * <p>La distance et la direction sont donnees en toutes lettres plutot qu'en
	 * coordonnees : « a 340 blocs, au nord » se comprend sans carte, et ne
	 * transforme pas le carnet en tableau de bord.
	 */
	private static String ouEstIl(ServerPlayer joueur, FicheCompagnon fiche) {
		if (!fiche.sorti()) {
			return Component.translatable("carnet.compagnon.ou.range").getString();
		}
		if (!fiche.dimension().equals(joueur.level().dimension())) {
			return Component.translatable("carnet.compagnon.ou.ailleurs").getString();
		}

		double dx = fiche.x() - joueur.getX();
		double dz = fiche.z() - joueur.getZ();
		int distance = (int) Math.round(Math.sqrt(dx * dx + dz * dz));

		if (distance <= AVEC_TOI) {
			return Component.translatable("carnet.compagnon.ou.avec_toi").getString();
		}

		// « Il t'attend » : il est loin, mais il est a son coin des repas et c'est
		// l'heure. C'est le seul cas ou l'on peut dire ce qu'il FAIT et non
		// seulement ou il est — et c'est justement celui qu'on a envie de lire.
		if (ilAttend(joueur, fiche)) {
			return Component.translatable("carnet.compagnon.ou.attend", distance).getString();
		}
		return Component.translatable("carnet.compagnon.ou.loin",
				distance, direction(dx, dz)).getString();
	}

	/** Vrai s'il est a son coin des repas, a l'heure ou on le nourrit. */
	private static boolean ilAttend(ServerPlayer joueur, FicheCompagnon fiche) {
		int habituelle = fiche.heureHabituelle();
		if (habituelle < 0) {
			return false;
		}
		int maintenant = (int) ((joueur.level().getDayTime() / 1000L + 6L) % 24L);
		if (FicheCompagnon.ecartDHeures(maintenant, habituelle) > 2) {
			return false;
		}
		for (FicheCompagnon.Lieu lieu : fiche.lieux()) {
			if (lieu.cle().equals("repas")
				&& lieu.proche(fiche.x(), fiche.y(), fiche.z(), 6.0D)) {
				return true;
			}
		}
		return false;
	}

	/** Le point cardinal dominant, en toutes lettres. */
	private static String direction(double dx, double dz) {
		// Repere de Minecraft : +Z est le sud, +X est l'est.
		String cle = Math.abs(dx) > Math.abs(dz)
				? (dx > 0 ? "est" : "ouest")
				: (dz > 0 ? "sud" : "nord");
		return Component.translatable("carnet.compagnon.ou." + cle).getString();
	}

	/**
	 * Fait venir un compagnon, ou le range, puis renvoie le carnet a jour.
	 *
	 * <p>Le carnet est renvoye <b>dans tous les cas</b>, meme quand rien n'a
	 * change : c'est ce qui garantit que ce que le joueur voit correspond a ce
	 * que le serveur sait. Un bouton qui ne repond pas laisserait croire a un
	 * clic perdu.
	 */
	private static void invoquer(ServerPlayer joueur, int indexDemande, boolean sortir) {
		Fiches fiches = Fiches.de(joueur.server);
		List<FicheCompagnon> siennes = fiches.duProprietaire(joueur.getUUID());
		if (siennes.isEmpty()) {
			return;
		}

		int index = Math.floorMod(indexDemande, siennes.size());
		FicheCompagnon fiche = siennes.get(index);

		Invocation.Resultat resultat = sortir
				? Invocation.invoquer(joueur, fiches, fiche)
				: Invocation.ranger(joueur, fiches, fiche);

		joueur.displayClientMessage(Component.literal(resultat.message())
				.withStyle(resultat.fait() ? ChatFormatting.GREEN : ChatFormatting.GRAY), true);

		ouvrirCarnet(joueur, index);
	}

	/**
	 * Envoie le contenu de la roue.
	 *
	 * <p>Tout ce que la table decrit part, ouvert ou non : c'est la roue qui
	 * affichera les cadenas. Montrer ce qui existe encore fait plus pour l'envie
	 * que de le cacher.
	 */
	private static void ouvrirRoue(ServerPlayer joueur, int indexDemande) {
		FicheCompagnon fiche = ficheDe(joueur, indexDemande);
		if (fiche == null) {
			joueur.displayClientMessage(
					Component.translatable("compagnon.aucun"), true);
			return;
		}

		Progression table = Niveaux.progression();
		int niveau = fiche.niveau(table);

		List<EntreeRoue> entrees = new ArrayList<>();
		for (String nom : table.tousLesDeblocages()) {
			// Chacun sa roue : un dragonnet n'a pas les gestes d'une mouette, et
			// une case qui ne joue rien est pire qu'une case absente.
			if (!Especes.concerne(nom, fiche.espece())) {
				continue;
			}
			int requis = table.niveauDe(nom);
			entrees.add(new EntreeRoue(nom, requis, niveau >= requis, motPour(fiche, nom)));
		}

		// IL TE REGARDE QUAND TU OUVRES SA ROUE.
		//
		// Une ligne, et l ecran cesse d etre un menu pose devant une bete inerte :
		// on ouvre la roue, il tourne la tete. C est le genre de detail que
		// personne ne remarque consciemment et que tout le monde ressent.
		//
		// Sous reserve qu il ne soit pas deja occupe : un geste en cours passe
		// avant une politesse.
		net.minecraft.server.level.ServerLevel ou = joueur.server.getLevel(fiche.dimension());
		Entity trouvee = ou == null ? null : ou.getEntity(fiche.id());
		CompagnonEntity present = trouvee instanceof CompagnonEntity compagnon
				? compagnon : null;
		boolean etaitOccupe = present != null && present.occupe();
		boolean fatigue = fiche.barre(Barre.ENERGIE) < table.action().energieMinimum();
		if (present != null && !etaitOccupe && present.lesMainsVides()) {
			present.getLookControl().setLookAt(joueur, 30.0F, 30.0F);
			present.jouerActionPendant("@ecoute", 25);
			// ET IL SE FAIT VOIR.
			//
			// Dans une cour a deux cents compagnons, retrouver le sien est le vrai
			// probleme, et aucun nom flottant ne le resout — il y en a deux cents.
			// Un lisere de quatre secondes, visible a travers les murs, et on sait.
			present.brillerPendant(TICKS_DE_LUEUR);
		}

		List<FicheCompagnon> siennes = Fiches.de(joueur.server).duProprietaire(joueur.getUUID());
		int index = Math.floorMod(indexDemande, siennes.size());

		// Les noms de toutes ses betes partent avec : c'est ce qui permet a la
		// roue d'afficher un onglet par compagnon, sans un aller-retour de plus.
		List<String> noms = new ArrayList<>(siennes.size());
		for (FicheCompagnon sienne : siennes) {
			noms.add(sienne.nom());
		}

		ServerPlayNetworking.send(joueur, new PaquetDonneesRoue(
				index, fiche.nom(), niveau, List.copyOf(entrees), List.copyOf(noms),
				present != null, fatigue, etaitOccupe));
	}

	/**
	 * Joue une animation demandee depuis la roue, apres verification complete.
	 *
	 * <p>Quatre questions, dans cet ordre : est-ce son compagnon ? l'animation
	 * est-elle dans la table ? est-elle ouverte a son niveau ? est-il en etat de
	 * la faire ? Le client n'en decide aucune.
	 */
	private static void jouerAction(ServerPlayer joueur, int indexDemande, String nomDemande,
			boolean apercu) {
		FicheCompagnon fiche = ficheDe(joueur, indexDemande);
		if (fiche == null) {
			return;
		}

		Progression table = Niveaux.progression();

		// Le client ne choisit pas non plus l'espece : il pourrait demander une
		// animation qui existe dans la table mais pas chez cette bete.
		if (!Especes.concerne(nomDemande, fiche.espece())) {
			if (!apercu) {
				refus(joueur, Component.translatable("roue.compagnon.inconnue"));
			}
		return;
		}

		int requis = table.niveauDe(nomDemande);
		if (requis < 0) {
			// Meme pour un apercu : ce n'est pas au client de decider ce qui existe.
			if (!apercu) {
				refus(joueur, Component.translatable("roue.compagnon.inconnue"));
			}
			return;
		}
		if (!apercu && fiche.niveau(table) < requis) {
			refus(joueur, Component.translatable("roue.compagnon.verrouillee", requis));
			return;
		}

		ServerLevel niveau = joueur.server.getLevel(fiche.dimension());
		Entity entite = niveau == null ? null : niveau.getEntity(fiche.id());
		if (!(entite instanceof CompagnonEntity compagnon)) {
			if (!apercu) {
				refus(joueur, Component.translatable("roue.compagnon.absent", fiche.nom()));
			}
			return;
		}

		// UN APERCU NE COUTE RIEN et ne peut pas etre refuse : ce n'est pas un
		// ordre, c'est une image. On voit a quoi ressemble une case avant de la
		// choisir — y compris celles qu'on n'a pas encore debloquees, ce qui est
		// exactement ce qui donne envie de les avoir.
		if (apercu) {
			// Un apercu ne dure pas plus longtemps que le geste lui-meme : on
			// prend la plus courte des deux.
			compagnon.jouerActionPendant(nomDemande,
				Math.min(DUREE_APERCU, Longueurs.de(nomDemande)));
			return;
		}

		// IL FINIT CE QU'IL A COMMENCE.
		//
		// On pouvait cliquer la roue pendant qu'il jouait deja un geste : le
		// second coupait le premier au milieu, et deux animations enchainees a
		// la hache donnent exactement l'impression que le mod est casse.
		//
		// Le client grise deja la roue pendant ce temps-la. Ce test est la pour
		// le cas ou il ne le ferait pas : c'est le serveur qui decide.
		if (compagnon.occupe()) {
			refus(joueur, Component.translatable("roue.compagnon.occupe", fiche.nom()));
			return;
		}

		if (fiche.barre(Barre.ENERGIE) < table.action().energieMinimum()) {
			refus(joueur, Component.translatable("roue.compagnon.fatigue", fiche.nom()));
			return;
		}

		// Il peut bouder. Voir Obeissance : jamais deux fois de suite, et jamais
		// quand la complicite est bonne.
		if (!Obeissance.accepte(compagnon, fiche, null)) {
			refus(joueur, Component.translatable("roue.compagnon.boude", fiche.nom()));
			return;
		}

		// LA VRAIE DUREE DU GESTE, lue dans le fichier d'animation.
		//
		// C'etait cinq secondes pour tout le monde. Une animation courte
		// laissait donc la bete figee dans sa derniere pose pendant le reste du
		// temps, et une longue etait coupee en plein milieu. Voir Longueurs.
		compagnon.jouerActionPendant(nomDemande, Longueurs.de(nomDemande));
		fiche.ajouterBarre(Barre.ENERGIE, table.action().energieDepensee(), table);
		Fiches.de(joueur.server).setDirty();
	}

	/** La fiche demandee parmi celles du joueur, ou {@code null} s'il n'en a pas. */
	/**
	 * Apprend un mot au compagnon, ou le lui fait oublier.
	 *
	 * <h2>Tout est verifie ici, et rien ailleurs</h2>
	 *
	 * <ol>
	 *   <li>la bete est <b>a lui</b> — le numero est resolu dans ses fiches ;</li>
	 *   <li>l'animation existe dans la table et son niveau l'a <b>ouverte</b> :
	 *       sinon on apprendrait un mot pour un tour qu'il ne sait pas faire ;</li>
	 *   <li>le mot est <b>un seul mot</b>, et le micro sait le dire.</li>
	 * </ol>
	 *
	 * <p>Le troisieme point est le plus important, et c'est celui qu'on aurait
	 * envie de sauter. Un mot absent du dictionnaire francais est retire de la
	 * liste du moteur <b>en silence</b> : le joueur aurait dresse sa bete pour
	 * rien, et passe des semaines a crier un mot qui ne pouvait pas etre entendu.
	 *
	 * <p>On refuse aussi les mots deja pris — un mot d'ordre du mod, ou le nom
	 * d'une bete a portee. « Assis » ne peut pas vouloir dire deux choses.
	 */
	private static void apprendre(ServerPlayer joueur, int indexDemande,
			String animation, String motDemande) {

		Fiches fiches = Fiches.de(joueur.server);
		FicheCompagnon fiche = ficheDe(joueur, indexDemande);
		if (fiche == null) {
			return;
		}

		Progression table = Niveaux.progression();
		int requis = table.niveauDe(animation);
		if (requis < 0 || !Especes.concerne(animation, fiche.espece())) {
			refus(joueur, Component.translatable("roue.compagnon.inconnue"));
			return;
		}
		if (fiche.niveau(table) < requis) {
			refus(joueur, Component.translatable("roue.compagnon.verrouillee", requis));
			return;
		}

		String mot = Voix.motSimple(motDemande);

		// Un mot vide efface : c'est ainsi qu'on lui fait oublier un tour.
		if (mot.isEmpty()) {
			String ancien = motPour(fiche, animation);
			if (!ancien.isEmpty() && fiche.oublierLeMot(ancien)) {
				fiches.setDirty();
				refus(joueur, Component.translatable("roue.compagnon.oublie", fiche.nom(), ancien));
			}
			ouvrirRoue(joueur, indexDemande);
			return;
		}

		String pourquoi = Voix.pourquoiCeMotNeVaPas(joueur, fiche, mot);
		if (pourquoi != null) {
			refus(joueur, Component.literal(pourquoi));
			return;
		}

		// Le meme mot ne peut pas servir a deux tours : on retire l'ancien lien.
		String ancienPourCeGeste = motPour(fiche, animation);
		if (!ancienPourCeGeste.isEmpty()) {
			fiche.oublierLeMot(ancienPourCeGeste);
		}
		String oublie = fiche.apprendre(mot, animation);
		fiche.incrementer(fr.lhdp.compagnon.mission.Compteurs.MOTS);
		fiches.setDirty();

		joueur.displayClientMessage(Component.translatable(
				"roue.compagnon.appris", fiche.nom(), mot)
				.withStyle(ChatFormatting.GREEN), false);
		if (oublie != null) {
			refus(joueur, Component.translatable("roue.compagnon.oublie", fiche.nom(), oublie));
		}
		ouvrirRoue(joueur, indexDemande);
	}

	/**
	 * Le cavalier veut monter ou descendre.
	 *
	 * <p>On ne verifie rien d'autre que <b>ce qu'il chevauche vraiment</b> : un
	 * client qui enverrait ce paquet sans etre sur une bete ne ferait rien, et
	 * s'il est dessus, monter et descendre sont deja ce qu'il a le droit de
	 * faire. Il n'y a pas d'autre pouvoir a prendre ici.
	 */
	private static void chevaucher(ServerPlayer joueur, int vers) {
		if (joueur.getVehicle() instanceof CompagnonEntity monture
				&& monture.getControllingPassenger() == joueur) {
			monture.poserLIntentionVerticale(vers);
		}
	}

	/**
	 * Le joueur depense un point de competence.
	 *
	 * <h2>Cinq verifications, et aucune n'est facultative</h2>
	 *
	 * <ol>
	 *   <li>la bete est <b>a lui</b> — le numero est resolu dans ses fiches ;</li>
	 *   <li>la competence <b>existe</b> encore ;</li>
	 *   <li>elle concerne <b>son espece</b> ;</li>
	 *   <li>son <b>niveau</b> l'ouvre ;</li>
	 *   <li>il lui reste un <b>point</b>.</li>
	 * </ol>
	 *
	 * <p>Le client n'en decide aucune. Il envoie un identifiant, et c'est tout ce
	 * qu'il peut faire : un client fabrique ne peut donc pas s'offrir les neuf
	 * competences au niveau un.
	 *
	 * <p><b>Un choix ne se reprend pas.</b> C'est ce qui lui donne son poids :
	 * une competence qu'on peut annuler n'est pas un choix, c'est un reglage.
	 */
	private static void prendreUneCompetence(ServerPlayer joueur, int indexDemande,
			String id) {

		Fiches fiches = Fiches.de(joueur.server);
		FicheCompagnon fiche = ficheDe(joueur, indexDemande);
		if (fiche == null) {
			return;
		}

		Competence competence = Competences.get(id);
		if (competence == null || !competence.pour(fiche.espece())) {
			refus(joueur, Component.translatable("livre.compagnon.competence.inconnue"));
			return;
		}
		if (fiche.a(id)) {
			// Deja prise : on renvoie la page plutot que de rester muet, au cas ou
			// le client afficherait autre chose que la verite.
			ouvrirLivre(joueur, indexDemande);
			return;
		}

		Progression table = Niveaux.progression();
		if (fiche.niveau(table) < competence.niveauRequis()) {
			refus(joueur, Component.translatable("livre.compagnon.competence.verrouillee",
					competence.niveauRequis()));
			return;
		}
		if (fiche.pointsRestants(table, Reglages.pointsTousLesNiveaux()) <= 0) {
			refus(joueur, Component.translatable("livre.compagnon.competence.sans_point"));
			return;
		}

		fiche.apprendreLaCompetence(id);
		// Le jour ou il est devenu quelqu'un. Une seule fois — c'est le premier
		// choix qui marque, pas le neuvieme, et un moment par competence
		// remplirait la page pour ne rien dire de plus.
		fiche.marquer("premiere_competence", System.currentTimeMillis());
		fiches.setDirty();
		joueur.displayClientMessage(Component.translatable(
				"livre.compagnon.competence.prise", fiche.nom(), competence.nom())
				.withStyle(ChatFormatting.GREEN), false);
		ouvrirLivre(joueur, indexDemande);
	}

	/** Le mot appris pour ce geste, ou une chaine vide. */
	private static String motPour(FicheCompagnon fiche, String animation) {
		for (java.util.Map.Entry<String, String> appris : fiche.motsAppris().entrySet()) {
			if (appris.getValue().equals(animation)) {
				return appris.getKey();
			}
		}
		return "";
	}

	private static FicheCompagnon ficheDe(ServerPlayer joueur, int indexDemande) {
		List<FicheCompagnon> siennes = Fiches.de(joueur.server).duProprietaire(joueur.getUUID());
		if (siennes.isEmpty()) {
			return null;
		}
		return siennes.get(Math.floorMod(indexDemande, siennes.size()));
	}

	private static void refus(ServerPlayer joueur, Component message) {
		joueur.displayClientMessage(message.copy().withStyle(ChatFormatting.GRAY), true);
	}

	/**
	 * Previent tous ceux qui voient la scene qu'un joueur caresse un compagnon.
	 *
	 * <p>Le caresseur est inclus : sans ca, il serait le seul a ne pas voir son
	 * propre geste.
	 */
	public static void diffuserCaresse(ServerPlayer caresseur, Entity compagnon, int ticks) {
		PaquetCaresse paquet = new PaquetCaresse(caresseur.getId(), compagnon.getId(), ticks);
		for (ServerPlayer temoin : PlayerLookup.tracking(compagnon)) {
			ServerPlayNetworking.send(temoin, paquet);
		}
		ServerPlayNetworking.send(caresseur, paquet);
	}

	/** Envoie le catalogue a un joueur. */
	public static void envoyerEspeces(ServerPlayer joueur) {
		ServerPlayNetworking.send(joueur, new PaquetEspeces(Especes.toutes()));
		// Les positions des objets partent avec : sans elles, le client ne
		// dessinerait rien dans les gueules.
		ServerPlayNetworking.send(joueur,
			new PaquetAncrages(fr.lhdp.compagnon.espece.Ancrages.toutes()));
	}

	/**
	 * L'equipe enregistre une position reglee a l'oeil.
	 *
	 * <p>Le droit se verifie ICI. L'ecran qui a envoye ce paquet n'a rien
	 * verifie du tout, et c'est voulu : un client fabrique enverrait le meme
	 * paquet sans passer par lui.
	 *
	 * <p>Une fois ecrit, tout le monde le recoit — la balle se replace dans la
	 * gueule de toutes les betes de cette espece, chez tous les joueurs, sans
	 * que personne ait a se reconnecter.
	 */
	private static void poserUnAncrage(ServerPlayer equipe, PaquetPoserAncrage demande) {
		if (equipe == null || !equipe.hasPermissions(2)) {
			return;
		}
		if (Especes.get(demande.espece()) == null) {
			equipe.sendSystemMessage(net.minecraft.network.chat.Component.literal(
				"Espece inconnue : " + demande.espece()));
			return;
		}
		if (!fr.lhdp.compagnon.espece.Ancrages.enregistrer(
				demande.espece(), demande.emplacement(), demande.ancrage())) {
			equipe.sendSystemMessage(net.minecraft.network.chat.Component.literal(
				"Impossible d'ecrire le fichier des ancrages. Voir le journal."));
			return;
		}
		PaquetAncrages tout =
			new PaquetAncrages(fr.lhdp.compagnon.espece.Ancrages.toutes());
		for (ServerPlayer autre : equipe.getServer().getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(autre, tout);
		}
		equipe.sendSystemMessage(net.minecraft.network.chat.Component.literal(
			demande.emplacement() + " de " + Especes.titre(demande.espece())
				+ " enregistre sur l'os \"" + demande.ancrage().os() + "\"."));
	}

	/**
	 * Cree le compagnon demande, apres verification.
	 *
	 * <p>Le paquet ne dit ni l'espece ni la variante : elles sont lues sur le
	 * oeuf que le joueur tient reellement en main. Un client modifie ne peut
	 * donc pas s'offrir l'espece qu'il veut.
	 */
	private static void naissance(ServerPlayer joueur, String nomDemande) {
		InteractionHand main = mainAvecOeuf(joueur);
		if (main == null) {
			// Le joueur a lache l'oeuf entre l'ouverture de l'ecran et sa
			// reponse. Rien ne se passe, et rien n'est perdu.
			return;
		}

		ItemStack oeuf = joueur.getItemInHand(main);
		Origine origine = oeuf.get(Objets.ORIGINE);
		if (origine == null) {
			joueur.sendSystemMessage(Component.literal(
					"Cet oeuf est vide : il ne dit ni l'espece ni la variante."));
			return;
		}

		String nom = nettoyer(nomDemande);
		if (nom.isEmpty()) {
			joueur.sendSystemMessage(Component.literal("Il lui faut un nom."));
			return;
		}

		FicheCompagnon fiche = new FicheCompagnon(
				UUID.randomUUID(),
				joueur.getUUID(),
				origine.espece(),
				origine.variante(),
				nom,
				joueur.level().dimension(),
				joueur.getX(), joueur.getY(), joueur.getZ(), joueur.getYRot(),
				System.currentTimeMillis(),
				Niveaux.progression());

		Fiches.de(joueur.server).ajouter(fiche);
		oeuf.shrink(1);

		joueur.sendSystemMessage(Component.literal(nom + " est la."));
		// S'il ne pourra jamais l'appeler a la voix, autant qu'il l'apprenne
		// maintenant plutot qu'en criant dans le vide pendant trois semaines.
		Voix.prevenirSiInaudible(joueur, nom);
		Compagnon.LOG.info("Compagnon cree : {} ({} / {}) pour {}",
				nom, origine.espece(), origine.variante(), joueur.getGameProfile().getName());
	}

	private static InteractionHand mainAvecOeuf(ServerPlayer joueur) {
		for (InteractionHand main : InteractionHand.values()) {
			if (joueur.getItemInHand(main).is(Objets.OEUF)) {
				return main;
			}
		}
		return null;
	}

	/**
	 * Coupe a la longueur permise et retire les codes de couleur. Le nom finit
	 * au-dessus de la tete du compagnon : il ne doit pas pouvoir servir a autre
	 * chose qu'a le nommer.
	 */
	private static String nettoyer(String brut) {
		String propre = brut.replace('§', ' ').trim();
		if (propre.length() > PaquetNaissance.LONGUEUR_MAX) {
			propre = propre.substring(0, PaquetNaissance.LONGUEUR_MAX).trim();
		}
		return propre;
	}
	/**
	 * L'equipe offre un oeuf a quelqu'un.
	 *
	 * <h2>Le seul traitement du mod qui exige un droit</h2>
	 *
	 * <p>Partout ailleurs, un paquet montant ne peut toucher que les betes de
	 * celui qui l'envoie : le pire qu'un client fabrique puisse faire est de
	 * s'embeter lui-meme. Celui-ci cree quelque chose et le donne, alors il
	 * demande le niveau 2 — et il le demande <b>ici</b>, pas dans l'ecran.
	 *
	 * <p>Les trois chaines arrivent d'un client et ne sont crues sur rien :
	 * le joueur est cherche parmi ceux qui sont en ligne, l'espece parmi
	 * celles qui sont chargees, et la couleur parmi celles de cette espece-la.
	 */
	private static void remettreUnOeuf(ServerPlayer equipe, String nomDuJoueur,
			String espece, String variante) {

		if (equipe == null || !equipe.hasPermissions(2)) {
			return;
		}
		ServerPlayer destinataire =
			equipe.getServer().getPlayerList().getPlayerByName(nomDuJoueur);
		if (destinataire == null) {
			equipe.sendSystemMessage(net.minecraft.network.chat.Component.literal(
				nomDuJoueur + " n'est plus en ligne."));
			return;
		}
		fr.lhdp.compagnon.espece.Espece fiche =
			fr.lhdp.compagnon.espece.Especes.get(espece);
		if (fiche == null || !fiche.variantes().containsKey(variante)) {
			equipe.sendSystemMessage(net.minecraft.network.chat.Component.literal(
				"Espece ou couleur inconnue : " + espece + " / " + variante));
			return;
		}

		net.minecraft.world.item.ItemStack oeuf =
			new net.minecraft.world.item.ItemStack(fr.lhdp.compagnon.objet.Objets.OEUF);
		oeuf.set(fr.lhdp.compagnon.objet.Objets.ORIGINE,
			new fr.lhdp.compagnon.objet.Origine(espece, variante));
		if (!destinataire.getInventory().add(oeuf)) {
			destinataire.drop(oeuf, false);
		}

		destinataire.sendSystemMessage(net.minecraft.network.chat.Component.literal(
			"Tu as recu un oeuf de compagnon. Utilise-le ou tu veux."));
		equipe.sendSystemMessage(net.minecraft.network.chat.Component.literal(
			fr.lhdp.compagnon.espece.Especes.titre(espece) + " offert a "
				+ destinataire.getGameProfile().getName()));
	}

}
