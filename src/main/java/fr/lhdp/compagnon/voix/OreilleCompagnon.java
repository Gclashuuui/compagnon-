package fr.lhdp.compagnon.voix;

import fr.lhdp.compagnon.Compagnon;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import su.plo.voice.api.addon.AddonInitializer;
import su.plo.voice.api.addon.AddonLoaderScope;
import su.plo.voice.api.addon.InjectPlasmoVoice;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.audio.codec.AudioDecoder;
import su.plo.voice.api.encryption.Encryption;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEndEvent;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEvent;
import su.plo.voice.api.server.event.connection.UdpClientDisconnectedEvent;
import su.plo.voice.api.server.event.connection.UdpPacketReceivedEvent;
import su.plo.voice.api.server.player.VoicePlayer;
import su.plo.voice.proto.packets.udp.serverbound.PlayerAudioPacket;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * L'oreille du compagnon : elle ecoute ce que dit son proprietaire.
 *
 * <p>Le trajet d'un mot : Plasmo capte le son → on le dechiffre → son decodeur le
 * rend en PCM → le moteur le compare a la liste des mots attendus → la phrase
 * reconnue repart vers {@link EcouteVocale}, exactement comme si elle avait ete
 * tapee. Le chat vocal, lui, n'est jamais modifie : on ne fait qu'ecouter au
 * passage.
 *
 * <h2>Ce que cette classe a appris de ses aines</h2>
 *
 * <p>Presque chaque garde ici correspond a une panne reellement vecue dans un
 * autre mod, souvent longue a diagnostiquer parce que <b>silencieuse</b> : la voix
 * cessait de repondre pour tout le monde, sans une ligne d'erreur nulle part.
 * Elles sont documentees une par une. Aucune n'est une precaution theorique.
 */
@Addon(id = "compagnon-voix", name = "Compagnon — voix", version = "1.0.0",
		authors = "LHDP", scope = AddonLoaderScope.SERVER)
public final class OreilleCompagnon implements AddonInitializer {

	@InjectPlasmoVoice
	private PlasmoVoiceServer serveurVocal;

	/**
	 * Date du dernier {@link PlayerSpeakEvent}, ou 0 s'il ne s'est jamais manifeste.
	 *
	 * <p><b>Pourquoi une date et non un simple drapeau.</b> Un drapeau leve une
	 * fois pour toutes condamnerait le chemin UDP pour <b>tout le serveur</b> et
	 * jusqu'au redemarrage. Si Plasmo cessait ensuite d'emettre cet evenement — ne
	 * serait-ce que pour un joueur ou un mode de parole particulier — plus aucun
	 * son ne nous parviendrait et la voix se tairait definitivement, sans erreur.
	 *
	 * <p>Avec une date, le repli redevient possible. Le delai evite d'analyser deux
	 * fois le meme fragment tant que l'evenement fonctionne : quand il fonctionne,
	 * il arrive cinquante fois par seconde, donc bien avant l'expiration.
	 */
	private volatile long dernierEvenementParole;

	/** Au-dela de ce silence de l'evenement, on refait confiance a l'UDP brut. */
	private static final long CONFIANCE_EVENEMENT = 3_000_000_000L;

	/**
	 * Le moteur ne conclut jamais tout seul.
	 *
	 * <p>Vosk ne rend sa transcription qu'a la fin d'un enonce, qu'il repere au
	 * silence. Plasmo, lui, cesse simplement d'envoyer des paquets quand le joueur
	 * relache sa touche : ce silence n'atteint donc jamais le moteur. Sans ce
	 * guetteur, le dernier mot — le seul qui compte — resterait indefiniment dans
	 * le tampon.
	 *
	 * <h3>Pourquoi neuf dixiemes de seconde et non trois</h3>
	 *
	 * <p>C'etait 350 millisecondes. C'est court — trop court pour beaucoup de
	 * monde. Quelqu'un qui begaie, qui parle lentement, ou qui cherche ses mots
	 * voyait sa phrase <b>coupee en deux</b> : « Cacahuete » d'un cote, « assis »
	 * de l'autre, et le second morceau perdu faute de nom.
	 *
	 * <p>A 900 millisecondes, on laisse le temps de finir sa phrase. On perd un
	 * demi-tiers de seconde de reactivite ; personne ne le remarque, et tout le
	 * monde est compris. Le mod n'a pas a etre plus rapide pour les uns au prix
	 * d'etre sourd aux autres.
	 */
	private static final long SEUIL_DE_SILENCE = 900_000_000L;

	/** Le guetteur passe a ce rythme. */
	private static final long PAS_DU_GUETTEUR = 150L;

	/**
	 * Combien de temps une ecoute survit a l'eloignement de son joueur.
	 *
	 * <p>Creer un moteur coute une vingtaine de millisecondes. Sans ce delai, un
	 * joueur qui pietine a la limite de la portee en creerait et en detruirait
	 * plusieurs par seconde, sur le fil reseau de Plasmo — qui a bien autre chose
	 * a faire.
	 */
	private static final long FERMETURE_APRES = 15_000_000_000L;

	/**
	 * Plafond d'ecoutes simultanees.
	 *
	 * <p>Un chateau a mille joueurs peut avoir cent personnes qui parlent en meme
	 * temps a portee de leur compagnon. Chaque moteur coute de la memoire native et
	 * du temps de calcul : au-dela de ce nombre, on prefere ne pas entendre
	 * quelques ordres plutot que de faire tomber le serveur. Le compte redescend
	 * tout seul, les ecoutes ne durent que le temps d'une phrase.
	 */
	private static final int ECOUTES_MAXIMUM = 64;

	/**
	 * Combien de fragments on garde de cote en attendant le profil d'un joueur.
	 *
	 * <p>Plasmo en envoie un toutes les vingt millisecondes : cinquante font une
	 * seconde de parole, largement de quoi couvrir l'attente d'un tick de serveur.
	 * Au-dela on jette les plus vieux — si le profil n'arrive pas en une seconde,
	 * c'est qu'il n'arrivera pas.
	 */
	private static final int FRAGMENTS_GARDES = 50;

	private final Map<UUID, Ecoute> ecoutes = new ConcurrentHashMap<>();

	/**
	 * Le son mis de cote pendant que le fil principal prepare le profil.
	 *
	 * <h2>Le defaut que ceci repare</h2>
	 *
	 * <p>Avant, un fragment arrive trop tot etait simplement <b>jete</b>, avec ce
	 * commentaire rassurant : « vingt millisecondes, personne ne les entend
	 * passer ». C'est faux. Ce ne sont pas vingt millisecondes n'importe ou dans
	 * la phrase : ce sont les <b>toutes premieres</b>, celles de l'attaque du
	 * premier mot. « dragon » y perdait son d.
	 *
	 * <p>Et ca n'arrivait pas qu'une fois : chaque silence un peu long faisait
	 * oublier le profil, donc chaque reprise de parole perdait son debut. D'ou un
	 * compagnon qui « entend une fois sur deux » sans qu'aucune erreur
	 * n'apparaisse nulle part.
	 *
	 * <p>Maintenant le son attend. Des que le profil arrive, il est avale dans
	 * l'ordre, et le moteur recoit la phrase entiere depuis sa premiere syllabe.
	 */
	private final Map<UUID, Deque<byte[]>> enAttenteDeProfil = new ConcurrentHashMap<>();

	/** Le joueur derriere chaque ecoute, pour conclure sans nouveau fragment. */
	private final Map<UUID, ServerPlayer> parleurs = new ConcurrentHashMap<>();

	private ScheduledExecutorService guetteur;

	@Override
	public void onAddonInitialize() {
		this.serveurVocal.getEventBus().register(this, this);
		demarrerLeGuetteur();
		Compagnon.LOG.info("Les compagnons ecoutent : ordres a la voix actifs "
				+ "(portee {} blocs, mots : {}).",
				(int) EcouteVocale.PORTEE, Vocabulaire.tousLesMots());
	}

	@Override
	public void onAddonShutdown() {
		if (this.guetteur != null) {
			this.guetteur.shutdownNow();
		}
		this.ecoutes.values().forEach(Ecoute::fermer);
		this.ecoutes.clear();
		this.parleurs.clear();
		this.enAttenteDeProfil.clear();
	}

	private void demarrerLeGuetteur() {
		this.guetteur = Executors.newSingleThreadScheduledExecutor(travail -> {
			Thread fil = new Thread(travail, "compagnon-voix-silence");
			// Demon : ce fil ne doit jamais retenir l'arret du serveur.
			fil.setDaemon(true);
			return fil;
		});
		this.guetteur.scheduleAtFixedRate(this::conclureLesPhrases,
				PAS_DU_GUETTEUR, PAS_DU_GUETTEUR, TimeUnit.MILLISECONDS);
	}

	/**
	 * Conclut la phrase des joueurs qui se sont tus.
	 *
	 * <p><b>Rien ne doit s'echapper d'ici.</b> Le contrat de
	 * {@code scheduleAtFixedRate} est sans pitie : <i>si une execution leve quoi
	 * que ce soit, les suivantes sont supprimees</i>. Definitivement.
	 *
	 * <p>C'est exactement le defaut vecu dans l'autre mod : la voix marchait, puis
	 * apres un moment elle cessait de repondre pour <b>tout le monde</b> jusqu'au
	 * redemarrage — alors que la commande d'essai, qui ne passe pas par ici,
	 * continuait de fonctionner. Il suffisait d'un joueur se deconnectant au
	 * mauvais moment pour tuer le guetteur, et avec lui la seule chose qui conclut
	 * les phrases.
	 *
	 * <p>D'ou deux filets : un par joueur, pour qu'un micro qui trebuche
	 * n'empeche pas les autres d'etre entendus, et un autour de tout.
	 */
	private void conclureLesPhrases() {
		try {
			Voix.ecoutes.set(this.ecoutes.size());
			long maintenant = System.nanoTime();
			fermerLesInactives(maintenant);

			for (Map.Entry<UUID, Ecoute> entree : this.ecoutes.entrySet()) {
				try {
					Ecoute ecoute = entree.getValue();
					if (!ecoute.enAttente || maintenant - ecoute.dernierSon < SEUIL_DE_SILENCE) {
						continue;
					}
					String phrase = ecoute.conclure();
					double force = ecoute.prendreLaForce();
					ServerPlayer joueur = this.parleurs.get(entree.getKey());
					if (phrase != null && joueur != null) {
						transmettre(joueur, phrase, force);
					}
					// La liste avait change pendant qu'il parlait : maintenant que la
					// phrase est dite, on peut refaire le moteur sans rien perdre.
					if (ecoute.aRefaire) {
						arreterDEcouter(entree.getKey());
					}
				} catch (Throwable incident) {
					// Throwable : une faute native de Vosk arrive en Error, qui
					// traverserait un catch (Exception) et tuerait le guetteur.
					Voix.noterIncident("conclusion de phrase : " + incident);
				}
			}
		} catch (Throwable incident) {
			Compagnon.LOG.error("Guetteur de silence : incident inattendu, la surveillance continue.",
					incident);
		}
	}

	/**
	 * Ferme les ecoutes dont plus rien ne justifie l'existence.
	 *
	 * <p>C'est ici, sur le fil du guetteur, que les ressources sont rendues — et
	 * non sur le fil reseau de Plasmo, qui doit rester libre de traiter le son.
	 *
	 * <p>Deux raisons de fermer, et il <b>faut</b> les deux :
	 *
	 * <ul>
	 *   <li>le joueur s'est eloigne de son compagnon ;</li>
	 *   <li><b>plus aucun son n'arrive.</b> Sans celle-ci, une ecoute fuit : un
	 *       joueur qui se deconnecte brutalement — plantage, coupure reseau — ne
	 *       declenche pas toujours l'evenement de deconnexion de Plasmo, et son
	 *       ecoute etait alors <i>active</i> au moment du depart, donc jamais
	 *       marquee inactive. Elle serait restee la jusqu'au redemarrage, avec sa
	 *       memoire native et le {@code ServerPlayer} qu'elle retient. A mille
	 *       joueurs, ce genre de fuite ne pardonne pas.</li>
	 * </ul>
	 */
	private void fermerLesInactives(long maintenant) {
		for (Map.Entry<UUID, Ecoute> entree : this.ecoutes.entrySet()) {
			Ecoute ecoute = entree.getValue();
			long inactiveDepuis = ecoute.inactiveDepuis;
			boolean eloigne = inactiveDepuis != 0L && maintenant - inactiveDepuis > FERMETURE_APRES;
			boolean muette = maintenant - ecoute.dernierSon > FERMETURE_APRES;
			if (eloigne || muette) {
				arreterDEcouter(entree.getKey());
			}
		}
	}

	// --- Les deux chemins du son --------------------------------------------------

	/** Chaque fragment de parole. On ne modifie jamais l'evenement. */
	@EventSubscribe
	public void quandIlParle(PlayerSpeakEvent evenement) {
		this.dernierEvenementParole = System.nanoTime();
		Voix.fragmentsRecus.incrementAndGet();
		traiter(joueurDe(evenement.getPlayer()), evenement.getPacket().getData());
	}

	/**
	 * Second chemin, volontairement plus bas niveau.
	 *
	 * <p>{@link PlayerSpeakEvent} n'est pas emis dans toutes les configurations de
	 * Plasmo Voice ; l'evenement UDP brut, lui, l'est toujours. On garde les deux
	 * et on se rabat sur celui-ci uniquement quand le premier se tait — sans cette
	 * condition, chaque fragment serait analyse deux fois.
	 */
	@EventSubscribe
	public void quandUnPaquetArrive(UdpPacketReceivedEvent evenement) {
		// Throwable et non Exception : une version de Plasmo dont l'API a bouge
		// echoue ici en NoSuchMethodError, pas en exception. Non rattrapee, elle
		// remonterait dans le fil UDP de Plasmo a chaque fragment recu — soit
		// cinquante fois par seconde et par joueur qui parle.
		try {
			if (evenementDeParoleVivant()
					|| !(evenement.getPacket() instanceof PlayerAudioPacket son)) {
				return;
			}
			Voix.fragmentsRecus.incrementAndGet();
			Voix.fragmentsParUdp.incrementAndGet();
			traiter(joueurDe(evenement.getConnection().getPlayer()), son.getData());
		} catch (Throwable incident) {
			Voix.noterIncident("API Plasmo Voice incompatible : " + incident);
		}
	}

	/**
	 * Fin de la prise de parole selon Plasmo.
	 *
	 * <p>Attention en lisant ce code : cet evenement <b>ne se declenche pas</b>
	 * dans la configuration testee, exactement comme {@link PlayerSpeakEvent}. Ce
	 * n'est pas lui qui conclut les phrases — c'est le guetteur de silence. On le
	 * garde parce qu'il est correct et gratuit la ou il fonctionne, mais ne
	 * comptez jamais dessus : s'y fier a deja coute une session entiere de
	 * diagnostic.
	 */
	@EventSubscribe
	public void quandIlSeTait(PlayerSpeakEndEvent evenement) {
		ServerPlayer joueur = joueurDe(evenement.getPlayer());
		if (joueur == null) {
			return;
		}
		Ecoute ecoute = this.ecoutes.get(joueur.getUUID());
		if (ecoute == null) {
			return;
		}
		String phrase = ecoute.conclure();
		if (phrase != null) {
			transmettre(joueur, phrase, ecoute.prendreLaForce());
		}
	}

	@EventSubscribe
	public void quandIlSeDeconnecte(UdpClientDisconnectedEvent evenement) {
		UUID id = evenement.getConnection().getPlayer().getInstance().getUuid();
		arreterDEcouter(id);
		Voix.oublier(id);
	}

	/** L'evenement de parole donne-t-il encore signe de vie ? */
	private boolean evenementDeParoleVivant() {
		long dernier = this.dernierEvenementParole;
		return dernier != 0 && System.nanoTime() - dernier < CONFIANCE_EVENEMENT;
	}

	// --- Le traitement ------------------------------------------------------------

	private void traiter(ServerPlayer joueur, byte[] opus) {
		if (joueur == null || !Voix.active()) {
			// L'oreille peut etre branchee sans que le moteur ait pu demarrer — un
			// modele absent, par exemple. Sans ce test on referait, cinquante fois
			// par seconde et par parleur, tout le travail qui mene a un moteur nul.
			return;
		}

		// Le fil audio ne regarde jamais le monde lui-meme : il lit ce que le fil
		// principal lui a prepare. Voir la note de classe de Voix.
		List<String> mots = Voix.motsPour(joueur.getUUID());
		if (mots == null) {
			// Profil pas encore pret. ON NE JETTE PAS CE SON : c'est le debut de
			// la phrase, et sans lui le premier mot est ampute. Voir la note de
			// enAttenteDeProfil — c'est la panne la plus visible qu'ait eue la voix.
			mettreDeCote(joueur.getUUID(), opus);
			return;
		}
		if (mots.isEmpty()) {
			// Personne pour l'entendre : ce qu'on gardait ne servira plus a rien.
			this.enAttenteDeProfil.remove(joueur.getUUID());
			// Personne pour l'entendre. On ne ferme PAS tout de suite : quelqu'un
			// qui marche a la limite de la portee entrerait et sortirait sans arret,
			// et reconstruire un moteur coute une vingtaine de millisecondes. Le
			// guetteur fermera si l'eloignement dure.
			Ecoute inactive = this.ecoutes.get(joueur.getUUID());
			if (inactive != null) {
				inactive.inactiveDepuis = System.nanoTime();
			}
			return;
		}

		// La liste a change — le joueur a appele un autre compagnon, ou en a
		// nomme un : on jette le moteur et on en refait un. Jamais setGrammar,
		// qui tue la machine virtuelle.
		Ecoute existante = this.ecoutes.get(joueur.getUUID());
		if (existante != null && !existante.ecouteDeja(mots)) {
			// MAIS PAS PENDANT QU'IL PARLE.
			//
			// Refaire le moteur jette tout le son deja avale. Le faire au milieu
			// d'une phrase, c'est perdre l'ordre en cours — et ca arrivait pour un
			// rien : un compagnon qui entre dans les seize blocs, un ami qui passe
			// avec le sien, et la liste des noms change.
			//
			// De toute facon un nom qui vient d'apparaitre ne peut pas se trouver
			// dans une phrase deja commencee. Attendre la fin ne coute donc rien
			// et sauve l'ordre en cours.
			if (existante.enAttente) {
				existante.aRefaire = true;
			} else {
				arreterDEcouter(joueur.getUUID());
				existante = null;
			}
		}

		Ecoute ecoute = existante;
		if (ecoute == null) {
			if (this.ecoutes.size() >= ECOUTES_MAXIMUM) {
				return;
			}
			// Creation HORS du verrou de la carte : l'appel descend dans du code
			// natif et dure une vingtaine de millisecondes. Le faire dans un
			// computeIfAbsent retiendrait le verrou pendant tout ce temps, et le
			// son des autres joueurs du meme casier attendrait pour rien.
			Ecoute creee = Ecoute.creer(this.serveurVocal, mots);
			if (creee == null) {
				return;
			}
			Ecoute gagnante = this.ecoutes.putIfAbsent(joueur.getUUID(), creee);
			if (gagnante != null) {
				// Un autre fragment du meme joueur a gagne la course : on rend nos
				// ressources natives au lieu de les abandonner a elles-memes.
				creee.fermer();
				ecoute = gagnante;
			} else {
				ecoute = creee;
			}
		}

		// L'ecoute redevient active : le compte a rebours de fermeture repart.
		ecoute.inactiveDepuis = 0L;
		this.parleurs.put(joueur.getUUID(), joueur);

		// Le son garde de cote passe EN PREMIER, et dans l'ordre ou il est arrive.
		// C'est le debut de la phrase : le donner apres le reste reviendrait a
		// faire entendre « ragon-d » au moteur.
		Deque<byte[]> attendait = this.enAttenteDeProfil.remove(joueur.getUUID());
		if (attendait != null) {
			for (byte[] garde : attendait) {
				String debut = ecoute.avaler(garde);
				if (debut != null) {
					transmettre(joueur, debut, ecoute.prendreLaForce());
				}
			}
		}

		String phrase = ecoute.avaler(opus);
		if (phrase != null) {
			transmettre(joueur, phrase, ecoute.prendreLaForce());
		}
	}

	/**
	 * Garde un fragment le temps que le profil arrive.
	 *
	 * <p>Borne : on ne garde qu'une seconde de son. Un joueur dont le profil
	 * n'arrive jamais — parce qu'il n'a aucun compagnon a portee, par exemple —
	 * ne doit pas remplir la memoire du serveur en parlant.
	 */
	private void mettreDeCote(UUID joueur, byte[] opus) {
		Deque<byte[]> garde = this.enAttenteDeProfil.computeIfAbsent(
				joueur, id -> new ArrayDeque<>(FRAGMENTS_GARDES));
		synchronized (garde) {
			while (garde.size() >= FRAGMENTS_GARDES) {
				garde.pollFirst();
			}
			garde.addLast(opus);
		}
	}

	/**
	 * La phrase repart vers le monde, et donc vers le fil principal.
	 *
	 * <p>Faire asseoir un compagnon touche a une entite : le faire depuis un fil
	 * reseau, c'est modifier le monde pendant que le serveur le fait tourner.
	 */
	private void transmettre(ServerPlayer joueur, String phrase, double force) {
		Voix.phrasesEntendues.incrementAndGet();
		Voix.noterEntendue(joueur.getGameProfile().getName(), phrase);

		// Sur quel ton ? La question se decide ici, sur le fil audio, tant qu'on a
		// la mesure sous la main. Voir Voix.tonDe.
		Voix.Ton ton = Voix.tonDe(joueur.getUUID(), force);

		// Un joueur qui se deconnecte entre le moment ou il a parle et celui ou sa
		// phrase est conclue n'a plus de serveur. Sans ce test, l'appel suivant leve
		// une exception sur le fil du guetteur — et le guetteur ne repart jamais.
		MinecraftServer serveur = joueur.getServer();
		if (serveur == null) {
			return;
		}
		serveur.execute(() -> {
			if (joueur.isRemoved()) {
				return;
			}
			if (EcouteVocale.entendu(joueur, phrase, ton).compris()) {
				Voix.ordresCompris.incrementAndGet();
			}
		});
	}

	private void arreterDEcouter(UUID joueur) {
		Ecoute partie = this.ecoutes.remove(joueur);
		if (partie != null) {
			partie.fermer();
		}
		// parleurs retient des ServerPlayer entiers : l'oublier ici garderait en
		// memoire le joueur complet — et le monde auquel il se rattache — bien
		// apres son depart.
		this.parleurs.remove(joueur);
		this.enAttenteDeProfil.remove(joueur);
	}

	private static ServerPlayer joueurDe(VoicePlayer joueurVocal) {
		Object poignee = joueurVocal.getInstance().getInstance();
		return poignee instanceof ServerPlayer joueur ? joueur : null;
	}

	// --- Une ecoute ---------------------------------------------------------------

	/** Le couple decodeur + moteur attache a un joueur qui parle pres du sien. */
	private static final class Ecoute {

		private final AudioDecoder decodeur;
		private final Reconnaisseur moteur;

		/**
		 * Plasmo chiffre la charge utile de ses paquets.
		 *
		 * <p>Sans ce dechiffrement, on remet des octets chiffres au decodeur Opus,
		 * qui repond « corrupted stream » — la cause de 830 echecs sur 1088 mesures
		 * dans l'autre mod, et d'une voix qui semblait simplement ne rien entendre.
		 */
		private final Encryption chiffrement;

		/**
		 * Date du dernier fragment, pour reperer nous-memes la fin de la phrase.
		 *
		 * <p>Mise a l'heure des la naissance, et pas laissee a zero : le guetteur
		 * s'en sert aussi pour fermer les ecoutes muettes, et un zero voudrait dire
		 * « muette depuis toujours ». Il fermerait donc une ecoute toute neuve dans
		 * l'intervalle entre sa creation et son premier fragment.
		 */
		volatile long dernierSon = System.nanoTime();

		/** Vrai si du son attend d'etre conclu : evite de vider un tampon deja vide. */
		volatile boolean enAttente;

		/** Depuis quand plus personne n'ecoute ce joueur, ou 0 si quelqu'un ecoute. */
		volatile long inactiveDepuis;

		/**
		 * Vrai si la liste des mots a change pendant qu'il parlait.
		 *
		 * <p>On ne refait pas le moteur tout de suite — ce serait perdre la phrase
		 * en cours. On note, et on le refait des qu'elle est finie.
		 */
		volatile boolean aRefaire;

		/**
		 * Le son le plus fort entendu depuis le debut de la phrase.
		 *
		 * <p>Le plus fort et non la moyenne : une phrase commence et finit toujours
		 * doucement, et une moyenne dirait de tout le monde qu'il parle bas. Ce
		 * qu'on veut savoir, c'est s'il a <b>hausse le ton quelque part</b>.
		 */
		private double leplusFort;

		/** Vrai des que les ressources natives ont ete rendues. */
		private boolean ferme;

		private Ecoute(AudioDecoder decodeur, Reconnaisseur moteur, Encryption chiffrement) {
			this.decodeur = decodeur;
			this.moteur = moteur;
			this.chiffrement = chiffrement;
		}

		static Ecoute creer(PlasmoVoiceServer serveurVocal, List<String> mots) {
			Reconnaisseur moteur = Reconnaisseur.creer(mots, frequenceDe(serveurVocal));
			if (moteur == null) {
				return null;
			}
			AudioDecoder decodeur = serveurVocal.createOpusDecoder(false);
			try {
				decodeur.open();
			} catch (Exception echec) {
				moteur.fermer();
				Voix.noterIncident("decodeur audio indisponible : " + echec);
				return null;
			}
			return new Ecoute(decodeur, moteur, serveurVocal.getDefaultEncryption());
		}

		/**
		 * La frequence <b>reellement</b> utilisee par ce serveur.
		 *
		 * <p>C'est un reglage, pas une constante. Voir
		 * {@link Reconnaisseur#FREQUENCE_DE_SECOURS} : la supposer rendait le son
		 * incomprehensible des qu'elle changeait, silencieusement et pour tout le
		 * monde.
		 */
		private static int frequenceDe(PlasmoVoiceServer serveurVocal) {
			try {
				int frequence = serveurVocal.getConfig().voice().sampleRate();
				Voix.noterFrequence(frequence);
				return frequence;
			} catch (Throwable apiDifferente) {
				// Une version de Plasmo dont l'API a bouge ne doit pas priver de
				// voix : on retombe sur sa valeur par defaut, et le diagnostic le dit.
				Voix.noterFrequence(0);
				return Reconnaisseur.FREQUENCE_DE_SECOURS;
			}
		}

		boolean ecouteDeja(List<String> mots) {
			return this.moteur.ecouteDeja(mots);
		}

		/**
		 * {@code synchronized} a cause de {@link #fermer()} : le decodeur Opus est
		 * de la memoire native, et la fermeture peut venir d'un autre fil — celui de
		 * la deconnexion — pendant qu'un fragment est en train d'y passer.
		 */
		synchronized String avaler(byte[] opus) {
			if (this.ferme) {
				return null;
			}
			this.dernierSon = System.nanoTime();
			this.enAttente = true;
			try {
				// Dechiffrer AVANT de decoder : l'ordre inverse de l'emission.
				short[] son = this.decodeur.decode(this.chiffrement.decrypt(opus));
				this.leplusFort = Math.max(this.leplusFort, force(son));
				String phrase = this.moteur.avaler(son);
				if (phrase != null) {
					// Le moteur a conclu tout seul : il n'y a plus rien en attente.
					// Sans cette ligne on croyait une phrase encore en cours, ce qui
					// repoussait indefiniment un changement de liste mis en attente —
					// et faisait vider au guetteur un tampon deja vide.
					this.enAttente = false;
				}
				return phrase;
			} catch (Throwable incident) {
				// Un paquet perdu ou corrompu est normal en UDP : on saute ce
				// fragment sans bruit. Throwable, car le decodeur natif leve Error.
				Voix.sonIllisible.incrementAndGet();
				Voix.noterIncident(incident.toString());
				// Le decodeur Opus garde un etat entre les paquets : une fois qu'il
				// a trebuche il reste desynchronise et rejette tout le reste. Le
				// remettre a zero lui permet de repartir du paquet suivant au lieu
				// de perdre toute la fin de la phrase.
				try {
					this.decodeur.reset();
				} catch (Throwable rienDeMieux) {
					// Le fragment suivant reessaiera.
				}
				return null;
			}
		}

		/** Conclut la phrase. Jamais le resultat partiel : voir {@link Reconnaisseur}. */
		synchronized String conclure() {
			this.enAttente = false;
			return this.ferme ? null : this.moteur.conclure();
		}

		/** La force de la phrase qui vient de finir, et remise a zero pour la suivante. */
		synchronized double prendreLaForce() {
			double force = this.leplusFort;
			this.leplusFort = 0.0D;
			return force;
		}

		/**
		 * La force d'un fragment : la moyenne quadratique de ses echantillons.
		 *
		 * <p>Une simple moyenne ne dirait rien — un son oscille autour de zero, et
		 * sa moyenne est donc toujours proche de zero, qu'il soit fort ou faible.
		 * On eleve au carre avant de moyenner, ce qui revient a mesurer l'energie.
		 */
		private static double force(short[] son) {
			if (son == null || son.length == 0) {
				return 0.0D;
			}
			double somme = 0.0D;
			for (short echantillon : son) {
				somme += (double) echantillon * echantillon;
			}
			return Math.sqrt(somme / son.length);
		}

		/** Rend les ressources natives. Meme serrure que {@link #avaler}, une seule fois. */
		synchronized void fermer() {
			if (this.ferme) {
				return;
			}
			this.ferme = true;
			this.moteur.fermer();
			try {
				this.decodeur.close();
			} catch (Throwable audepart) {
				// Rien a faire de plus au depart d'un joueur.
			}
		}
	}
}
