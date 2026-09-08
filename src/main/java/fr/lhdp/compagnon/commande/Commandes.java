package fr.lhdp.compagnon.commande;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.lhdp.compagnon.classement.Classement;
import fr.lhdp.compagnon.contenu.Bobo;
import fr.lhdp.compagnon.contenu.Caractere;
import fr.lhdp.compagnon.contenu.Contenu;
import fr.lhdp.compagnon.contenu.Donnable;
import fr.lhdp.compagnon.entite.Chrono;
import fr.lhdp.compagnon.entite.CompagnonEntity;
import fr.lhdp.compagnon.espece.Espece;
import fr.lhdp.compagnon.espece.Especes;
import fr.lhdp.compagnon.reseau.PaquetNaissance;
import fr.lhdp.compagnon.fiche.Barre;
import fr.lhdp.compagnon.fiche.FicheCompagnon;
import fr.lhdp.compagnon.fiche.Fiches;
import fr.lhdp.compagnon.objet.Objets;
import fr.lhdp.compagnon.objet.Origine;
import fr.lhdp.compagnon.progression.Niveaux;
import fr.lhdp.compagnon.progression.Progression;
import fr.lhdp.compagnon.progression.SourceXp;
import fr.lhdp.compagnon.voix.EcouteVocale;
import fr.lhdp.compagnon.voix.Lexique;
import fr.lhdp.compagnon.voix.Vocabulaire;
import fr.lhdp.compagnon.voix.Voix;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Toutes les commandes du mod. Reservees a l'equipe (niveau 2).
 *
 * <p><b>La distribution</b> — la seule porte d'entree du mod. La vente se passe
 * sur Discord ; le mod ne connait que ces deux lignes.
 *
 * <pre>
 * /compagnon donner &lt;joueur&gt; &lt;espece&gt; &lt;variante&gt;   remet un oeuf
 * /compagnon objet  &lt;joueur&gt; &lt;genre&gt; &lt;variete&gt;     remet un aliment ou un soin
 * </pre>
 *
 * <p><b>Les outils de travail</b> — pour mettre au point, pas pour jouer.
 *
 * <pre>
 * /compagnon creer &lt;espece&gt; &lt;variante&gt; &lt;nom&gt;   cree une fiche ici, sans oeuf
 * /compagnon fiches                             liste toutes les fiches
 * /compagnon supprimer                          supprime la fiche la plus proche
 * /compagnon supprimer joueur &lt;joueur&gt;        efface tous ses compagnons
 * /compagnon supprimer tout confirmer          efface TOUT, fiches et entites
 * /compagnon info                               tout ce que porte le compagnon vise
 * /compagnon anim &lt;nom&gt;                         joue une animation ; "stop" l'arrete
 * /compagnon renommer &lt;nom&gt;                     rebaptise le compagnon vise
 * /compagnon variante &lt;nom&gt;                     change la variante a chaud
 * /compagnon espece &lt;nom&gt;                       change l'espece
 * /compagnon barre &lt;nom&gt; &lt;valeur&gt;               pose une barre
 * /compagnon xp &lt;source&gt;                        fait gagner de l'experience
 * /compagnon bobo &lt;id&gt;                          provoque un bobo
 * /compagnon envie &lt;aliment&gt;                    lui fait reclamer un aliment
 * /compagnon envie objet &lt;item&gt;                 ... ou n'importe quel objet du jeu
 * /compagnon voix &lt;phrase&gt;                      dit une phrase, sans micro
 * /compagnon voix etat                          pourquoi la voix ne repond pas
 * /compagnon perf                               mesure les trois postes de depense
 * /compagnon banc <combien>                     pose une foule de compagnons de test
 * /compagnon banc curiosite <passages>          mesure le cout d'un bloc casse
 * /compagnon banc net                           enleve les compagnons de test
 * </pre>
 *
 * <p>Les commandes qui visent un compagnon agissent sur le plus proche, dans un
 * rayon de {@value #PORTEE} blocs.
 */
public final class Commandes {

	/** Rayon de recherche du compagnon vise, en blocs. Valeur inventee. */
	public static final int PORTEE = 8;

	// --- Les listes proposees ---------------------------------------------------
	// Rien ne se tape a la main : le jeu propose ce qui existe reellement, et il
	// le lit dans les fichiers. Ajouter une espece ou un aliment le fait
	// apparaitre dans la liste sans toucher a une ligne de code.

	private static final SuggestionProvider<CommandSourceStack> ESPECES =
			(contexte, liste) -> SharedSuggestionProvider.suggest(Especes.noms(), liste);

	/** Les variantes de l'espece deja choisie, pas toutes les variantes du monde. */
	private static final SuggestionProvider<CommandSourceStack> VARIANTES = (contexte, liste) -> {
		Espece espece = Especes.get(StringArgumentType.getString(contexte, "espece"));
		return SharedSuggestionProvider.suggest(
				espece == null ? List.of() : espece.variantes().keySet(), liste);
	};

	/**
	 * Les variantes du compagnon qu'on regarde. Ici aucune espece n'a ete tapee :
	 * on la lit sur la bete elle-meme.
	 */
	private static final SuggestionProvider<CommandSourceStack> VARIANTES_SEULES = (contexte, liste) -> {
		Espece espece = null;
		try {
			CompagnonEntity compagnon = vise(contexte.getSource());
			if (compagnon != null) {
				espece = Especes.get(compagnon.espece());
			}
		} catch (CommandSyntaxException horsJeu) {
			// La commande vient de la console : il n'y a personne pour viser.
		}
		return SharedSuggestionProvider.suggest(
				espece == null ? List.of() : espece.variantes().keySet(), liste);
	};

	private static final SuggestionProvider<CommandSourceStack> GENRES =
			(contexte, liste) -> SharedSuggestionProvider.suggest(List.of("aliment", "soin"), liste);

	/** Les varietes du genre deja choisi : les aliments ou les remedes. */
	private static final SuggestionProvider<CommandSourceStack> VARIETES = (contexte, liste) -> {
		String genre = StringArgumentType.getString(contexte, "genre");
		return SharedSuggestionProvider.suggest(
				genre.equals("soin") ? Contenu.nomsSoins() : Contenu.nomsAliments(), liste);
	};

	/** Les aliments existants, plus "rien" pour effacer l'envie. */
	private static final SuggestionProvider<CommandSourceStack> ALIMENTS = (contexte, liste) -> {
		List<String> proposes = new ArrayList<>(Contenu.nomsAliments());
		proposes.add("rien");
		return SharedSuggestionProvider.suggest(proposes, liste);
	};

	private static final SuggestionProvider<CommandSourceStack> BOBOS =
			(contexte, liste) -> SharedSuggestionProvider.suggest(Contenu.nomsBobos(), liste);

	private static final SuggestionProvider<CommandSourceStack> BARRES =
			(contexte, liste) -> SharedSuggestionProvider.suggest(
					Arrays.stream(Barre.values()).map(Barre::cle).toList(), liste);

	private static final SuggestionProvider<CommandSourceStack> SOURCES =
			(contexte, liste) -> SharedSuggestionProvider.suggest(
					Arrays.stream(SourceXp.values()).map(SourceXp::cle).toList(), liste);

	/** Les animations de l'espece du compagnon vise, plus "stop". */
	private static final SuggestionProvider<CommandSourceStack> ANIMATIONS = (contexte, liste) -> {
		List<String> proposees = new ArrayList<>();
		proposees.add("stop");
		for (Espece espece : Especes.toutes()) {
			proposees.addAll(espece.locomotion().values());
			proposees.addAll(espece.reactions().values());
		}
		return SharedSuggestionProvider.suggest(proposees.stream().distinct().toList(), liste);
	};

	private Commandes() {
	}

	public static void enregistrer() {
		CommandRegistrationCallback.EVENT.register((repartiteur, acces, environnement) ->
				repartiteur.register(Commands.literal("compagnon")
						.requires(source -> source.hasPermission(2))

						.then(Commands.literal("donner")
								// Sans rien derriere : l'ecran, ou l'on VOIT la bete avant de
								// l'offrir. Avec les trois arguments : la forme d'origine, qui
								// se scripte et se repete.
								.executes(Commandes::ouvrirLaRemise)
								.then(Commands.argument("joueur", EntityArgument.player())
										.then(Commands.argument("espece", StringArgumentType.string()).suggests(ESPECES)
												.then(Commands.argument("variante", StringArgumentType.string()).suggests(VARIANTES)
														.executes(Commandes::donner)))))

						.then(Commands.literal("objet")
								.then(Commands.argument("joueur", EntityArgument.player())
										.then(Commands.argument("genre", StringArgumentType.string()).suggests(GENRES)
												.then(Commands.argument("variete", StringArgumentType.string()).suggests(VARIETES)
														.executes(Commandes::objet)))))

						.then(Commands.literal("creer")
								.then(Commands.argument("espece", StringArgumentType.string()).suggests(ESPECES)
										.then(Commands.argument("variante", StringArgumentType.string()).suggests(VARIANTES)
												.then(Commands.argument("nom", StringArgumentType.string())
														.executes(Commandes::creer)))))

						.then(Commands.literal("fiches")
								.executes(Commandes::fiches))

						.then(Commands.literal("classement")
								.executes(Commandes::classementEtat)
								.then(Commands.literal("ouvrir")
										.executes(contexte -> classementOuvrir(contexte, true)))
								.then(Commands.literal("fermer")
										.executes(contexte -> classementOuvrir(contexte, false))))

						.then(Commands.literal("supprimer")
								.executes(Commandes::supprimer)

								.then(Commands.literal("joueur")
										.then(Commands.argument("joueur", EntityArgument.player())
												.executes(Commandes::supprimerJoueur)))

								// "confirmer" n'est pas de la politesse : effacer toutes
								// les fiches est la seule chose du mod qui perd vraiment
								// des compagnons. On ne le fait pas par megarde.
								.then(Commands.literal("tout")
										.then(Commands.literal("confirmer")
												.executes(Commandes::supprimerTout))))

						.then(Commands.literal("anim")
								.then(Commands.argument("nom", StringArgumentType.string()).suggests(ANIMATIONS)
										.executes(Commandes::animation)))

						.then(Commands.literal("renommer")
								.then(Commands.argument("nom", StringArgumentType.string())
										.executes(Commandes::renommer)))

						.then(Commands.literal("variante")
								.then(Commands.argument("nom", StringArgumentType.string()).suggests(VARIANTES_SEULES)
										.executes(Commandes::variante)))

						.then(Commands.literal("espece")
								.then(Commands.argument("nom", StringArgumentType.string()).suggests(ESPECES)
										.executes(Commandes::espece)))

						.then(Commands.literal("barre")
								.then(Commands.argument("nom", StringArgumentType.string()).suggests(BARRES)
										.then(Commands.argument("valeur", FloatArgumentType.floatArg(0.0F, Barre.MAXIMUM))
												.executes(Commandes::barre))))

						.then(Commands.literal("xp")
								.then(Commands.argument("source", StringArgumentType.string()).suggests(SOURCES)
										.executes(Commandes::xp)))

						.then(Commands.literal("niveau")
								.then(Commands.argument("valeur", IntegerArgumentType.integer(1))
										.executes(Commandes::niveau)))

						.then(Commands.literal("bobo")
								.then(Commands.argument("id", StringArgumentType.string()).suggests(BOBOS)
										.executes(Commandes::bobo)))

						.then(Commands.literal("table")
								.executes(Commandes::table))

						.then(Commands.literal("info")
								.executes(Commandes::info))

						.then(Commands.literal("envie")
								// Un vrai objet du jeu, ou un des notres : la roue de
								// completion propose tout ce qui existe.
								.then(Commands.literal("objet")
										.then(Commands.argument("item", ItemArgument.item(acces))
												.executes(Commandes::envieObjet)))
								.then(Commands.argument("variete", StringArgumentType.string())
										.suggests(ALIMENTS)
										.executes(Commandes::envie)))

						.then(Commands.literal("ancrer")
								.executes(Commandes::ancrer))

						.then(Commands.literal("perf")
								.executes(Commandes::perf))

						.then(Commands.literal("banc")
								.then(Commands.literal("net")
										.executes(Commandes::bancNet))
								.then(Commands.literal("curiosite")
										.then(Commands.argument("passages",
												IntegerArgumentType.integer(1, Banc.PASSAGES_MAXIMUM))
												.executes(Commandes::bancCuriosite)))
								.then(Commands.argument("combien",
										IntegerArgumentType.integer(1, Banc.MAXIMUM))
										.executes(Commandes::bancPoser)))

						.then(Commands.literal("voix")
								.then(Commands.literal("etat")
										.executes(Commandes::voixEtat))
								.then(Commands.argument("phrase", StringArgumentType.greedyString())
										.executes(Commandes::voix)))));
	}

	// --- La vraie commande d'equipe ---------------------------------------------

	/**
	 * Remet un oeuf a un joueur. C'est la seule porte d'entree du mod : la
	 * vente et la distribution se passent sur Discord, et le mod n'en sait rien.
	 *
	 * <p>Le joueur n'a pas besoin d'etre la au bon moment — il utilisera son
	 * oeuf quand il voudra.
	 */
	private static int donner(CommandContext<CommandSourceStack> contexte) throws CommandSyntaxException {
		ServerPlayer destinataire = EntityArgument.getPlayer(contexte, "joueur");
		String espece = StringArgumentType.getString(contexte, "espece");
		String variante = StringArgumentType.getString(contexte, "variante");

		ItemStack oeuf = new ItemStack(Objets.OEUF);
		oeuf.set(Objets.ORIGINE, new Origine(espece, variante));
		remettre(destinataire, oeuf);

		destinataire.sendSystemMessage(Component.literal(
				"Tu as recu un oeuf de compagnon. Utilise-le ou tu veux."));

		return repondre(contexte, "Oeuf remis a " + destinataire.getGameProfile().getName()
				+ " : " + espece + " / " + variante);
	}

	/** Remet un aliment ou un objet de soin. Meme logique : l'equipe distribue. */
	private static int objet(CommandContext<CommandSourceStack> contexte) throws CommandSyntaxException {
		ServerPlayer destinataire = EntityArgument.getPlayer(contexte, "joueur");
		String genre = StringArgumentType.getString(contexte, "genre");
		String variete = StringArgumentType.getString(contexte, "variete");

		Item objet;
		boolean connu;
		if (genre.equals("aliment")) {
			objet = Objets.ALIMENT;
			connu = Contenu.aliment(variete) != null;
		} else if (genre.equals("soin")) {
			objet = Objets.SOIN;
			connu = Contenu.soin(variete) != null;
		} else {
			contexte.getSource().sendFailure(Component.literal(
					"Genre inconnu : " + genre + ". Attendu : aliment ou soin."));
			return 0;
		}

		if (!connu) {
			contexte.getSource().sendFailure(Component.literal(
					"Aucun " + genre + " nomme \"" + variete + "\" dans les donnees."));
			return 0;
		}

		remettre(destinataire, Objets.exemplaire(objet, genre.equals("soin")
				? Contenu.soin(variete) : Contenu.aliment(variete)));

		return repondre(contexte, variete + " remis a " + destinataire.getGameProfile().getName());
	}

	/** Dans l'inventaire si possible, aux pieds sinon : rien ne se perd. */
	/**
	 * Ouvre a l'equipe l'ecran qui offre un compagnon.
	 *
	 * <p>C'est la seule commande du mod dont le but est de ne plus avoir a
	 * taper de commande : on l'appelle une fois, on distribue ensuite a la
	 * souris. La forme longue reste pour les scripts.
	 */
	private static int ouvrirLaRemise(CommandContext<CommandSourceStack> contexte)
			throws CommandSyntaxException {

		ServerPlayer equipe = contexte.getSource().getPlayerOrException();
		java.util.List<String> joueurs = new java.util.ArrayList<>();
		for (ServerPlayer autre : equipe.getServer().getPlayerList().getPlayers()) {
			joueurs.add(autre.getGameProfile().getName());
		}
		joueurs.sort(String.CASE_INSENSITIVE_ORDER);
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(equipe,
			new fr.lhdp.compagnon.reseau.PaquetOuvrirRemise(java.util.List.copyOf(joueurs)));
		return joueurs.size();
	}

	private static void remettre(ServerPlayer destinataire, ItemStack pile) {
		if (!destinataire.getInventory().add(pile)) {
			destinataire.drop(pile, false);
		}
	}

	// --- La fiche ---------------------------------------------------------------

	private static int creer(CommandContext<CommandSourceStack> contexte) throws CommandSyntaxException {
		ServerPlayer joueur = contexte.getSource().getPlayerOrException();

		FicheCompagnon fiche = new FicheCompagnon(
				UUID.randomUUID(),
				joueur.getUUID(),
				StringArgumentType.getString(contexte, "espece"),
				StringArgumentType.getString(contexte, "variante"),
				StringArgumentType.getString(contexte, "nom"),
				joueur.level().dimension(),
				joueur.getX(), joueur.getY(), joueur.getZ(), joueur.getYRot(),
				System.currentTimeMillis(),
				Niveaux.progression());

		Fiches.de(contexte.getSource().getServer()).ajouter(fiche);

		return repondre(contexte, "Fiche creee : " + fiche.nom()
				+ " (" + fiche.espece() + " / " + fiche.variante() + "). "
				+ "Il apparaitra dans la seconde.");
	}

	private static int fiches(CommandContext<CommandSourceStack> contexte) {
		CommandSourceStack source = contexte.getSource();
		Fiches fiches = Fiches.de(source.getServer());

		if (fiches.nombre() == 0) {
			return repondre(contexte, "Aucune fiche.");
		}

		repondre(contexte, fiches.nombre() + " fiche(s) :");
		for (FicheCompagnon fiche : fiches.toutes()) {
			boolean affiche = source.getServer().getLevel(fiche.dimension()) != null
					&& source.getServer().getLevel(fiche.dimension()).getEntity(fiche.id()) != null;

			repondre(contexte, "  " + fiche.nom()
					+ " | " + fiche.espece() + "/" + fiche.variante()
					+ " | " + fiche.mode()
					+ " | " + fiche.dimension().location()
					+ " " + Math.round(fiche.x()) + " " + Math.round(fiche.y()) + " " + Math.round(fiche.z())
					+ " | ensemble " + duree(fiche.ticksEnsemble())
					+ " | " + (affiche ? "affiche" : "range"));
		}
		return 1;
	}

	private static int supprimer(CommandContext<CommandSourceStack> contexte) throws CommandSyntaxException {
		CompagnonEntity compagnon = vise(contexte.getSource());
		if (compagnon == null || compagnon.ficheId() == null) {
			contexte.getSource().sendFailure(
					Component.literal("Aucun compagnon lie a une fiche a moins de " + PORTEE + " blocs."));
			return 0;
		}

		Fiches fiches = Fiches.de(contexte.getSource().getServer());
		FicheCompagnon fiche = fiches.get(compagnon.ficheId());
		String nom = fiche != null ? fiche.nom() : "(inconnu)";

		fiches.supprimer(compagnon.ficheId());
		compagnon.discard();

		return repondre(contexte, "Fiche supprimee : " + nom);
	}

	/** Efface tous les compagnons d'un joueur. Utile pour reprendre a zero. */
	private static int supprimerJoueur(CommandContext<CommandSourceStack> contexte)
			throws CommandSyntaxException {

		ServerPlayer cible = EntityArgument.getPlayer(contexte, "joueur");
		MinecraftServer serveur = contexte.getSource().getServer();
		Fiches fiches = Fiches.de(serveur);

		int combien = 0;
		for (FicheCompagnon fiche : fiches.duProprietaire(cible.getUUID())) {
			retirer(serveur, fiches, fiche);
			combien++;
		}

		return repondre(contexte, combien + " compagnon(s) supprime(s) pour "
				+ cible.getGameProfile().getName() + ".");
	}

	/**
	 * Le grand menage : toutes les fiches, plus toutes les entites de compagnon
	 * qui traineraient sans fiche — celles posees au {@code /summon} pendant les
	 * essais.
	 *
	 * <p>C'est la seule commande du mod qui perd reellement des compagnons. Elle
	 * demande le mot {@code confirmer} pour cette raison.
	 */
	private static int supprimerTout(CommandContext<CommandSourceStack> contexte) {
		MinecraftServer serveur = contexte.getSource().getServer();
		Fiches fiches = Fiches.de(serveur);

		int avecFiche = 0;
		for (FicheCompagnon fiche : fiches.toutes()) {
			retirer(serveur, fiches, fiche);
			avecFiche++;
		}

		// Les entites sans fiche ne se tuent pas et ne se recreent pas : il faut
		// aller les chercher une par une dans les mondes charges.
		int orphelines = 0;
		for (ServerLevel niveau : serveur.getAllLevels()) {
			for (Entity entite : niveau.getAllEntities()) {
				if (entite instanceof CompagnonEntity compagnon) {
					compagnon.discard();
					orphelines++;
				}
			}
		}

		return repondre(contexte, avecFiche + " fiche(s) supprimee(s), "
				+ orphelines + " entite(s) retiree(s). Plus aucun compagnon.");
	}

	/**
	 * Retire l'affichage puis la fiche.
	 *
	 * <p>L'ordre compte : si on effacait la fiche d'abord, l'entite resterait
	 * plantee la sans plus rien pour la decrire.
	 */
	private static void retirer(MinecraftServer serveur, Fiches fiches, FicheCompagnon fiche) {
		ServerLevel niveau = serveur.getLevel(fiche.dimension());
		if (niveau != null) {
			Entity entite = niveau.getEntity(fiche.id());
			if (entite != null) {
				entite.discard();
			}
		}
		fiches.supprimer(fiche.id());
	}

	// --- Les barres et l'experience ---------------------------------------------

	private static int barre(CommandContext<CommandSourceStack> contexte) throws CommandSyntaxException {
		FicheCompagnon fiche = ficheVisee(contexte);
		if (fiche == null) {
			return 0;
		}

		String nom = StringArgumentType.getString(contexte, "nom");
		Barre barre = Barre.depuis(nom);
		if (barre == null) {
			contexte.getSource().sendFailure(Component.literal(
					"Barre inconnue : " + nom + ". Attendu : faim, energie, complicite, sante."));
			return 0;
		}

		float valeur = FloatArgumentType.getFloat(contexte, "valeur");
		fiche.setBarre(barre, valeur, Niveaux.progression());
		Fiches.de(contexte.getSource().getServer()).setDirty();

		return repondre(contexte, barre.cle() + " = " + arrondi(fiche.barre(barre))
				+ " | humeur : " + fiche.humeur().libelle() + " " + fiche.humeur().bouille());
	}

	private static int xp(CommandContext<CommandSourceStack> contexte) throws CommandSyntaxException {
		FicheCompagnon fiche = ficheVisee(contexte);
		if (fiche == null) {
			return 0;
		}

		String nom = StringArgumentType.getString(contexte, "source");
		SourceXp source = SourceXp.depuis(nom);
		if (source == null) {
			contexte.getSource().sendFailure(Component.literal(
					"Source inconnue : " + nom + ". Attendu : soins, affection, balade."));
			return 0;
		}

		Progression table = Niveaux.progression();
		long maintenant = System.currentTimeMillis();
		int avant = fiche.niveau(table);
		int gagne = fiche.gagnerXp(source, table, maintenant);
		int apres = fiche.niveau(table);
		Fiches.de(contexte.getSource().getServer()).setDirty();

		if (gagne == 0) {
			return repondre(contexte, "Plafond du jour atteint pour " + source.cle()
					+ ". Reviens demain — c'est le principe.");
		}

		if (apres > avant) {
			fiche.marquer("niveau_" + apres, maintenant);
			repondre(contexte, "Niveau " + apres + " !");
		}

		return repondre(contexte, "+" + gagne + " xp (" + source.cle() + ")"
				+ " | total " + fiche.xp()
				+ " | niveau " + apres + "/" + table.niveauMaximum()
				+ " | reste aujourd'hui " + fiche.resteAujourdhui(source, table, maintenant));
	}

	/**
	 * Met le compagnon a un niveau donne, en lui posant l'experience du palier.
	 *
	 * <p>Sert a essayer un deblocage tout de suite, au lieu d'attendre les jours
	 * que la progression demande normalement.
	 */
	private static int niveau(CommandContext<CommandSourceStack> contexte) throws CommandSyntaxException {
		FicheCompagnon fiche = ficheVisee(contexte);
		if (fiche == null) {
			return 0;
		}

		Progression table = Niveaux.progression();
		int demande = IntegerArgumentType.getInteger(contexte, "valeur");
		int vise = Math.min(demande, table.niveauMaximum());

		fiche.setXp(table.xpDu(vise));
		Fiches.de(contexte.getSource().getServer()).setDirty();

		return repondre(contexte, fiche.nom() + " passe au niveau " + fiche.niveau(table)
				+ " (" + fiche.xp() + " xp)"
				+ (demande > vise ? " — le niveau maximum de la table est " + vise + "." : ""));
	}

	/**
	 * Provoque un bobo, pour pouvoir essayer les soins sans attendre le hasard.
	 * En jeu ils arrivent quelques fois par semaine.
	 */
	private static int bobo(CommandContext<CommandSourceStack> contexte) throws CommandSyntaxException {
		FicheCompagnon fiche = ficheVisee(contexte);
		if (fiche == null) {
			return 0;
		}

		String id = StringArgumentType.getString(contexte, "id");
		Bobo bobo = Contenu.bobo(id);
		if (bobo == null) {
			contexte.getSource().sendFailure(Component.literal(
					"Bobo inconnu : " + id + ". Connus : " + Contenu.nomsBobos()));
			return 0;
		}

		fiche.setBobo(bobo.id(), System.currentTimeMillis());
		fiche.ajouterBarre(Barre.SANTE, bobo.sante(), Niveaux.progression());
		Fiches.de(contexte.getSource().getServer()).setDirty();

		return repondre(contexte, bobo.nom() + " — " + bobo.description()
				+ " Se soigne avec : " + bobo.soignePar()
				+ " | sante " + arrondi(fiche.barre(Barre.SANTE)));
	}

	/**
	 * Montre ce que le serveur a <b>reellement</b> charge : la table, les aliments,
	 * les bobos, les especes.
	 *
	 * <p>Sert a trancher la question « est-ce que mon fichier est bien pris en
	 * compte ? » sans aller fouiller dans le journal. Si un fichier est illisible,
	 * le mod se rabat sur un reglage de secours et cette commande le montre tout
	 * de suite.
	 */
	private static int table(CommandContext<CommandSourceStack> contexte) {
		Progression table = Niveaux.progression();

		repondre(contexte, "Table : " + table.paliers().size() + " palier(s), niveau max "
				+ table.niveauMaximum() + ".");

		List<String> deblocages = table.tousLesDeblocages();
		if (deblocages.isEmpty()) {
			repondre(contexte, "Aucun deblocage decrit — la roue sera vide.");
		} else {
			repondre(contexte, deblocages.size() + " deblocage(s) :");
			for (String nom : deblocages) {
				repondre(contexte, "  niveau " + table.niveauDe(nom) + " -> " + nom);
			}
		}

		repondre(contexte, "Especes : " + (Especes.noms().isEmpty() ? "aucune" : Especes.noms()));
		repondre(contexte, "Aliments : " + Contenu.nomsAliments());
		repondre(contexte, "Soins : " + Contenu.nomsSoins());
		repondre(contexte, "Bobos : " + Contenu.nomsBobos());
		return repondre(contexte, "Caracteres : " + Contenu.nomsCaracteres());
	}

	// --- L'affichage ------------------------------------------------------------

	private static int animation(CommandContext<CommandSourceStack> contexte) throws CommandSyntaxException {
		CompagnonEntity compagnon = vise(contexte.getSource());
		if (compagnon == null) {
			return aucun(contexte);
		}
		String nom = StringArgumentType.getString(contexte, "nom");
		if (nom.equals("stop")) {
			compagnon.jouerAction("");
			return repondre(contexte, "Couche action arretee.");
		}
		compagnon.jouerAction(nom);
		return repondre(contexte, "Animation jouee : " + nom);
	}

	/**
	 * Change le nom du compagnon vise.
	 *
	 * <p>Il manquait, et son absence coutait cher : un nom que le micro ne sait
	 * pas dire — une faute de frappe au bapteme, « Dargon » pour « Dragon » —
	 * etait definitif. On avertissait le joueur au bapteme, puis plus jamais, et
	 * il n'y avait aucun moyen de revenir dessus.
	 *
	 * <p>Le meme avertissement est redonne ici : si le nouveau nom ne se prononce
	 * pas, on le dit tout de suite, avec des noms qui marcheraient.
	 */
	/**
	 * Le meme nettoyage qu'au bapteme.
	 *
	 * <p>La borne est cote serveur des deux cotes, et c'est ce qui compte : un
	 * client modifie peut envoyer ce qu'il veut, et un nom sans fin finirait
	 * dans la sauvegarde de tout le monde.
	 */
	private static String nettoyer(String brut) {
		String propre = brut.replace('§', ' ').trim();
		return propre.length() > PaquetNaissance.LONGUEUR_MAX
				? propre.substring(0, PaquetNaissance.LONGUEUR_MAX).trim()
				: propre;
	}

	private static int renommer(CommandContext<CommandSourceStack> contexte) throws CommandSyntaxException {
		CompagnonEntity compagnon = vise(contexte.getSource());
		if (compagnon == null) {
			return aucun(contexte);
		}
		// Borne comme au bapteme : un nom sans fin finirait dans la sauvegarde et
		// au-dessus de sa tete. Meme regle des deux cotes, sinon l’une des deux
		// finit par etre oubliee.
		String nom = nettoyer(StringArgumentType.getString(contexte, "nom"));
		if (nom.isEmpty()) {
			return repondre(contexte, "Un nom vide, ce n'est pas un nom.");
		}
		compagnon.setCustomName(Component.literal(nom));
		ecrireDansLaFiche(contexte, compagnon, fiche -> fiche.setNom(nom));

		if (contexte.getSource().getEntity() instanceof ServerPlayer joueur) {
			Voix.prevenirSiInaudible(joueur, nom);
		}
		return repondre(contexte, "Il s'appelle maintenant " + nom + ".");
	}

	private static int variante(CommandContext<CommandSourceStack> contexte) throws CommandSyntaxException {
		CompagnonEntity compagnon = vise(contexte.getSource());
		if (compagnon == null) {
			return aucun(contexte);
		}
		String nom = StringArgumentType.getString(contexte, "nom");
		compagnon.setVariante(nom);
		ecrireDansLaFiche(contexte, compagnon, fiche -> fiche.setVariante(nom));
		// Le serveur ne lit pas les fiches d'espece : il ne peut pas verifier que
		// la variante existe. Le client retombera sur la variante par defaut si le
		// nom est inconnu. La verification arrivera avec les donnees serveur.
		return repondre(contexte, "Variante : " + nom);
	}

	private static int espece(CommandContext<CommandSourceStack> contexte) throws CommandSyntaxException {
		CompagnonEntity compagnon = vise(contexte.getSource());
		if (compagnon == null) {
			return aucun(contexte);
		}
		String nom = StringArgumentType.getString(contexte, "nom");
		compagnon.setEspece(nom);
		ecrireDansLaFiche(contexte, compagnon, fiche -> fiche.setEspece(nom));
		return repondre(contexte, "Espece : " + nom);
	}

	private static int info(CommandContext<CommandSourceStack> contexte) throws CommandSyntaxException {
		CompagnonEntity compagnon = vise(contexte.getSource());
		if (compagnon == null) {
			return aucun(contexte);
		}

		String action = compagnon.action().isEmpty() ? "(aucune)" : compagnon.action();
		String variante = compagnon.variante().isEmpty() ? "(par defaut)" : compagnon.variante();
		repondre(contexte, "espece " + compagnon.espece()
				+ " | variante " + variante + " | action " + action);

		if (compagnon.ficheId() == null) {
			return repondre(contexte, "Pas de fiche : entite de test, elle ne survivra pas au rechargement.");
		}

		FicheCompagnon fiche = Fiches.de(contexte.getSource().getServer()).get(compagnon.ficheId());
		if (fiche == null) {
			return repondre(contexte, "Fiche introuvable — anomalie.");
		}

		Progression table = Niveaux.progression();
		int niveau = fiche.niveau(table);
		int suivant = table.xpDuSuivant(niveau);

		repondre(contexte, "fiche " + fiche.nom()
				+ " | mode " + fiche.mode()
				+ " | ensemble " + duree(fiche.ticksEnsemble())
				+ " | moments " + fiche.moments().size()
				+ " | compteurs " + fiche.compteurs());

		repondre(contexte, "niveau " + niveau + "/" + table.niveauMaximum()
				+ " | xp " + fiche.xp()
				+ (suivant < 0 ? " (dernier palier)" : " / " + suivant));

		StringBuilder barres = new StringBuilder();
		for (Barre barre : Barre.values()) {
			barres.append(barre.cle()).append(' ').append(arrondi(fiche.barre(barre))).append("  ");
		}
		repondre(contexte, barres.toString().trim());

		repondre(contexte, "humeur : " + fiche.humeur().libelle()
				+ " " + fiche.humeur().bouille());

		Caractere caractere = Contenu.caractere(fiche.caractere());
		repondre(contexte, caractere == null
				? "caractere inconnu : " + fiche.caractere()
				: "caractere : " + caractere.nom()
						+ "  sociabilite " + caractere.sociabilite()
						+ "  attachement " + caractere.attachement()
						+ "  vivacite " + caractere.vivacite()
						+ "  calin " + caractere.calin()
						+ "  curiosite " + caractere.curiosite());

		if (!fiche.aUnBobo()) {
			return repondre(contexte, "aucun bobo");
		}
		Bobo bobo = Contenu.bobo(fiche.bobo());
		return repondre(contexte, bobo == null
				? "bobo inconnu dans les donnees : " + fiche.bobo()
				: "bobo : " + bobo.nom() + " — a soigner avec " + bobo.soignePar());
	}

	// --- Outils ----------------------------------------------------------------

	/** La fiche du compagnon vise, avec le message d'erreur qui va bien. */
	private static FicheCompagnon ficheVisee(CommandContext<CommandSourceStack> contexte)
			throws CommandSyntaxException {
		CompagnonEntity compagnon = vise(contexte.getSource());
		if (compagnon == null) {
			aucun(contexte);
			return null;
		}
		if (compagnon.ficheId() == null) {
			contexte.getSource().sendFailure(Component.literal(
					"Ce compagnon n'a pas de fiche. Utilise /compagnon creer."));
			return null;
		}
		FicheCompagnon fiche = Fiches.de(contexte.getSource().getServer()).get(compagnon.ficheId());
		if (fiche == null) {
			contexte.getSource().sendFailure(Component.literal("Fiche introuvable — anomalie."));
		}
		return fiche;
	}

	private static String arrondi(float valeur) {
		return String.valueOf(Math.round(valeur));
	}

	// --- Outils -----------------------------------------------------------------

	private static void ecrireDansLaFiche(CommandContext<CommandSourceStack> contexte,
			CompagnonEntity compagnon, java.util.function.Consumer<FicheCompagnon> action) {
		if (compagnon.ficheId() == null) {
			return;
		}
		Fiches fiches = Fiches.de(contexte.getSource().getServer());
		FicheCompagnon fiche = fiches.get(compagnon.ficheId());
		if (fiche != null) {
			action.accept(fiche);
			fiches.setDirty();
		}
	}

	/**
	 * Ouvre l editeur de position sur le compagnon a portee.
	 *
	 * <p>Et lui met quelque chose dans la gueule au passage : sans objet a
	 * regarder, il n y a rien a regler. On prend ce que l equipe tient en main,
	 * ou un os a defaut — c est exactement ce qu on veut y voir.
	 *
	 * <p>Il le reposera au sol tout seul en revenant vers son maitre.
	 */
	private static int ancrer(CommandContext<CommandSourceStack> contexte)
			throws CommandSyntaxException {

		ServerPlayer equipe = contexte.getSource().getPlayerOrException();
		CompagnonEntity compagnon = vise(contexte.getSource());
		if (compagnon == null) {
			return repondre(contexte, "Aucun compagnon a portee.");
		}
		if (compagnon.lesMainsVides()) {
			ItemStack enMain = equipe.getMainHandItem();
			compagnon.prendreDansLaGueule(enMain.isEmpty()
				? new ItemStack(net.minecraft.world.item.Items.BONE)
				: enMain);
		}
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(equipe,
			new fr.lhdp.compagnon.reseau.PaquetOuvrirAncrage(compagnon.espece()));
		return 1;
	}

	/** Le compagnon le plus proche du joueur, ou {@code null}. */
	private static CompagnonEntity vise(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer joueur = source.getPlayerOrException();
		AABB zone = joueur.getBoundingBox().inflate(PORTEE);
		List<CompagnonEntity> trouves =
				joueur.serverLevel().getEntitiesOfClass(CompagnonEntity.class, zone);
		return trouves.stream()
				.min(Comparator.comparingDouble(candidat -> candidat.distanceToSqr(joueur)))
				.orElse(null);
	}

	/** Minecraft avance de vingt ticks par seconde. */
	private static final long TICKS_PAR_SECONDE = 20L;

	private static String duree(long ticks) {
		long secondes = ticks / TICKS_PAR_SECONDE;
		long heures = secondes / 3600L;
		long minutes = (secondes % 3600L) / 60L;
		return heures > 0 ? heures + " h " + minutes + " min" : minutes + " min";
	}

	private static int aucun(CommandContext<CommandSourceStack> contexte) {
		contexte.getSource().sendFailure(
				Component.literal("Aucun compagnon a moins de " + PORTEE + " blocs."));
		return 0;
	}

	/**
	 * Dit une phrase a haute voix, sans micro.
	 *
	 * <p>Tout ce qui suit la reconnaissance vocale — trouver le compagnon appele,
	 * verifier qu'il entend, lui faire executer l'ordre — passe par le meme chemin
	 * que la vraie voix. Cette commande permet donc de mettre au point les mots,
	 * les noms et les distances sans jamais brancher un microphone, et de
	 * reproduire un probleme signale par un joueur en le retapant.
	 *
	 * <pre>/compagnon voix cacahuete assis</pre>
	 */
	private static int voix(CommandContext<CommandSourceStack> contexte) throws CommandSyntaxException {
		ServerPlayer joueur = contexte.getSource().getPlayerOrException();
		String phrase = StringArgumentType.getString(contexte, "phrase");

		repondre(contexte, "Entendu : \"" + Vocabulaire.pourLaComparaison(phrase) + "\"");
		return repondre(contexte, EcouteVocale.entendu(joueur, phrase).message());
	}

	/**
	 * Tout ce qu'il faut savoir quand la voix ne repond pas.
	 *
	 * <p>Une voix muette est un mystere sans compteurs : on ne sait meme pas si le
	 * son arrive jusqu'au serveur. Cette page repond dans l'ordre aux questions
	 * qu'on se pose, et chaque ligne elimine une cause.
	 */
	private static int voixEtat(CommandContext<CommandSourceStack> contexte) {
		if (!Voix.plasmoPresent()) {
			return repondre(contexte, "Plasmo Voice n'est pas installe : pas d'ordres a la voix.");
		}
		if (!Voix.active()) {
			return repondre(contexte, "Voix inactive — " + Voix.silence());
		}

		repondre(contexte, "Voix active. Frequence Plasmo : "
				+ (Voix.frequence() == 0 ? "inconnue (secours 48 kHz)" : Voix.frequence() + " Hz"));

		Lexique lexique = Lexique.dejaLu();
		repondre(contexte, "Vocabulaire : " + (lexique == null ? "en cours de lecture"
				: lexique.vide() ? "illisible — les noms ne sont pas verifies"
				: lexique.taille() + " mots"));

		repondre(contexte, "Ordres compris : " + Vocabulaire.tousLesMots());
		repondre(contexte, "Ecoutes en cours : " + Voix.ecoutes.get()
				+ " | profils en memoire : " + Voix.profilsEnMemoire());
		repondre(contexte, "Fragments recus : " + Voix.fragmentsRecus.get()
				+ " (dont " + Voix.fragmentsParUdp.get() + " par UDP brut)"
				+ " | illisibles : " + Voix.sonIllisible.get());
		repondre(contexte, "Phrases entendues : " + Voix.phrasesEntendues.get()
				+ " | ordres suivis : " + Voix.ordresCompris.get());

		// Ce que le moteur guette POUR CELUI QUI TAPE la commande : c'est la
		// seule facon de voir si le mot qu'on prononce a une chance d'exister.
		if (contexte.getSource().getEntity() instanceof ServerPlayer moi) {
			repondre(contexte, "Mots guettes pour vous — " + Voix.motsGuettes(moi.getUUID()));
		}

		if (!Voix.derniereEntendue().isEmpty()) {
			repondre(contexte, "Derniere phrase — " + Voix.derniereEntendue());
		}
		if (!Voix.dernierIncident().isEmpty()) {
			repondre(contexte, "Dernier incident — " + Voix.dernierIncident());
		}

		// Le cas qui trompe : le son arrive, mais rien n'en sort. C'est presque
		// toujours qu'aucun compagnon n'est a portee de voix de celui qui parle.
		if (Voix.fragmentsRecus.get() > 0 && Voix.phrasesEntendues.get() == 0) {
			repondre(contexte, "Le son arrive mais rien n'est reconnu : verifie qu'un compagnon "
					+ "est bien a moins de " + (int) EcouteVocale.PORTEE + " blocs de celui qui parle.");
		}
		if (Voix.fragmentsRecus.get() == 0) {
			repondre(contexte, "Aucun son recu : personne n'a encore parle, ou Plasmo Voice "
					+ "n'emet pas vers nous.");
		}
		return 1;
	}



	/**
	 * Lui fait reclamer un aliment precis, tout de suite.
	 *
	 * <p>Meme raison que pour la bulle : en jeu, l'envie n'apparait qu'une minute
	 * sur treize, et seulement une fois la faim descendue sous le seuil.
	 */
	private static int envie(CommandContext<CommandSourceStack> contexte) throws CommandSyntaxException {
		CompagnonEntity compagnon = vise(contexte.getSource());
		if (compagnon == null) {
			return aucun(contexte);
		}
		String variete = StringArgumentType.getString(contexte, "variete");
		if (variete.equals("rien")) {
			compagnon.avoirEnvieDe("");
			return repondre(contexte, "Il ne reclame plus rien.");
		}
		Donnable aliment = Contenu.aliment(variete);
		if (aliment == null) {
			contexte.getSource().sendFailure(Component.literal(
					"Aucun aliment nomme \"" + variete + "\"."));
			return 0;
		}
		compagnon.avoirEnvieDe(Objets.envie(aliment));
		return repondre(contexte, "Il reclame : " + aliment.nom()
				+ ". \"/compagnon envie rien\" pour arreter.");
	}

	/**
	 * Lui fait reclamer n'importe quel objet du jeu.
	 *
	 * <p>Utile pour deux choses : verifier que la bulle d'envie se place bien avec
	 * un objet dont on connait deja l'allure — une pomme, une torche — et essayer
	 * un objet d'un autre mod sans avoir a lui ecrire une fiche d'aliment.
	 *
	 * <pre>/compagnon envie objet minecraft:golden_apple</pre>
	 */
	private static int envieObjet(CommandContext<CommandSourceStack> contexte)
			throws CommandSyntaxException {

		CompagnonEntity compagnon = vise(contexte.getSource());
		if (compagnon == null) {
			return aucun(contexte);
		}
		Item objet = ItemArgument.getItem(contexte, "item").getItem();
		ResourceLocation cle = BuiltInRegistries.ITEM.getKey(objet);

		compagnon.avoirEnvieDe(cle.toString());
		return repondre(contexte, "Il reclame : " + cle
				+ ". \"/compagnon envie rien\" pour arreter.");
	}

	/**
	 * Chronometre la collision en plusieurs morceaux, et rend un chiffre.
	 *
	 * <p>Premier appel : ca demarre. Deuxieme : ca s'arrete et ca dit ce que ca
	 * coute. Laissez tourner une minute avec du monde autour, sinon le chiffre ne
	 * veut rien dire.
	 *
	 * <p>C'est le dernier cout du mod qui n'ait jamais ete mesure. On a longtemps
	 * suppose — dans les deux sens — au lieu de compter.
	 */
	/**
	 * Dit si le classement est ouvert.
	 *
	 * <p>Sans argument : on regarde avant de decider. C'est la meme habitude
	 * que partout ailleurs dans ces commandes.
	 */
	private static int classementEtat(CommandContext<CommandSourceStack> contexte) {
		return repondre(contexte, Classement.ouvert()
			? "Le classement est ouvert. /compagnon classement fermer pour le couper."
			: "Le classement est ferme. /compagnon classement ouvrir pour le rendre.");
	}

	/**
	 * Ouvre ou ferme le classement pour tout le monde.
	 *
	 * <h2>Pourquoi cette commande existe</h2>
	 *
	 * <p>Un classement peut mal tourner : un serveur ou tout le monde se
	 * compare finit par jouer pour le tableau plutot que pour sa bete. Si ca
	 * arrive, il faut pouvoir l'eteindre <b>tout de suite</b>, sans redemarrer,
	 * sans changer un fichier de reglage et sans recompiler.
	 *
	 * <p>Ferme, la touche des joueurs repond une phrase et n'ouvre rien. Rien
	 * n'est perdu : les niveaux continuent de monter, et le rouvrir retrouve le
	 * meme tableau.
	 *
	 * <p>Ce reglage ne survit pas a un redemarrage, et c'est voulu : une mesure
	 * prise dans l'urgence ne doit pas se transformer en decision permanente
	 * que plus personne ne se rappelle avoir prise.
	 */
	private static int classementOuvrir(CommandContext<CommandSourceStack> contexte,
			boolean ouvert) {

		Classement.ouvrir(ouvert);
		return repondre(contexte, ouvert
			? "Classement ouvert aux joueurs."
			: "Classement ferme. La touche ne repond plus, jusqu'au redemarrage "
				+ "ou jusqu'a /compagnon classement ouvrir.");
	}

	private static int perf(CommandContext<CommandSourceStack> contexte) {
		MinecraftServer serveur = contexte.getSource().getServer();

		if (!Chrono.enMarche()) {
			Chrono.demarrer();
			depart = serveur.getTickCount();
			return repondre(contexte, "Mesure en cours. Laisse tourner une minute, "
					+ "puis refais /compagnon perf.");
		}

		Chrono.arreter();
		long ticks = Math.max(1L, serveur.getTickCount() - depart);

		// UN POSTE APRES L'AUTRE, JAMAIS ADDITIONNES EN CHEMIN.
		//
		// Un total qui melange deux choses differentes ne repond a aucune
		// question. Quand on cherche ou passe le temps, on veut savoir lequel
		// des postes coute — et c'est seulement a la fin qu'on somme.
		for (Chrono.Poste poste : Chrono.postes()) {
			repondre(contexte, poste.nom() + ", sur " + ticks + " ticks :");
			repondre(contexte, "  " + poste.passages() + " passages, soit "
					+ Math.round(poste.passages() / (double) ticks) + " par tick");
			repondre(contexte, "  " + Math.round(poste.moyenneNanos())
					+ " ns par passage");
			double ms = poste.msParTick(ticks);
			repondre(contexte, String.format("  %.4f ms par tick, soit %.3f %% des 50 ms",
					ms, 100.0 * ms / 50.0));
		}

		// AVEC COMBIEN DE BETES ? Sans ce chiffre, les millisecondes ne veulent
		// rien dire : 0,3 ms avec trois compagnons et 0,3 ms avec deux cents ne
		// racontent pas du tout la meme histoire.
		repondre(contexte, "Mesure faite avec " + Banc.charges(serveur)
				+ " compagnons charges.");

		double total = Chrono.totalMsParTick(ticks);
		repondre(contexte, String.format("Total mesure : %.4f ms par tick (%.3f %%)",
				total, 100.0 * total / 50.0));

		return repondre(contexte, total > 2.0
				? "C'est beaucoup : il y a de quoi alleger."
				: "C'est negligeable. Ne pas optimiser.");
	}

	/**
	 * Pose une foule de compagnons de test.
	 *
	 * <p>Le mod est ecrit pour mille joueurs et teste par une personne. Sans
	 * cette commande, tous les chiffres de performance sont des suppositions
	 * polies faites a partir d'un seul compagnon.
	 */
	private static int bancPoser(CommandContext<CommandSourceStack> contexte)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {

		ServerPlayer joueur = contexte.getSource().getPlayerOrException();
		int combien = IntegerArgumentType.getInteger(contexte, "combien");
		for (String ligne : Banc.poser(joueur, combien)) {
			repondre(contexte, ligne);
		}
		return 1;
	}

	private static int bancNet(CommandContext<CommandSourceStack> contexte) {
		int enleves = Banc.nettoyer(contexte.getSource().getServer());
		return repondre(contexte, enleves == 0
			? "Aucun compagnon de test a enlever."
			: enleves + " compagnons de test enleves.");
	}

	/**
	 * Mesure le cout d'un bloc casse, et dit ce que ca donne a mille joueurs.
	 *
	 * <p>A lancer <b>pendant que le banc est pose</b> : cette recherche coute
	 * en fonction de ce qu'elle trouve, et une mesure dans un desert ne dit
	 * rien de ce que ca coutera dans une cour d'ecole.
	 */
	private static int bancCuriosite(CommandContext<CommandSourceStack> contexte)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {

		ServerPlayer joueur = contexte.getSource().getPlayerOrException();
		int passages = IntegerArgumentType.getInteger(contexte, "passages");
		for (String ligne : Banc.curiosite(joueur, passages)) {
			repondre(contexte, ligne);
		}
		return 1;
	}

	/** Le tick ou la mesure a commence. */
	private static int depart;

	private static int repondre(CommandContext<CommandSourceStack> contexte, String message) {
		contexte.getSource().sendSuccess(() -> Component.literal(message), false);
		return 1;
	}
}
