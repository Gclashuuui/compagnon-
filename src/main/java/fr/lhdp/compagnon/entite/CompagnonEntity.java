package fr.lhdp.compagnon.entite;

import fr.lhdp.compagnon.contenu.Caractere;
import fr.lhdp.compagnon.contenu.Contenu;
import fr.lhdp.compagnon.espece.Espece;
import fr.lhdp.compagnon.espece.Longueurs;
import fr.lhdp.compagnon.espece.Partie;
import fr.lhdp.compagnon.espece.Especes;
import fr.lhdp.compagnon.fiche.Barre;
import fr.lhdp.compagnon.fiche.FicheCompagnon;
import fr.lhdp.compagnon.fiche.Fiches;
import fr.lhdp.compagnon.progression.Niveaux;
import fr.lhdp.compagnon.progression.SourceXp;
import fr.lhdp.compagnon.fiche.Humeur;
import fr.lhdp.compagnon.fiche.Mode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;

/**
 * Le compagnon, en jeu.
 *
 * <p><b>Une seule classe pour toutes les especes.</b> L'espece est une donnee,
 * pas une sous-classe : cette classe ne sait pas qu'il existe un dragonnet, elle
 * sait qu'il existe un fichier d'espece.
 *
 * <p>A terme, cette entite ne sera qu'un <b>affichage temporaire</b> de la fiche
 * persistante (etape 3). Aujourd'hui elle porte encore son propre etat.
 */
public class CompagnonEntity extends TamableAnimal implements GeoEntity {

	private static final EntityDataAccessor<String> ESPECE =
			SynchedEntityData.defineId(CompagnonEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<String> VARIANTE =
			SynchedEntityData.defineId(CompagnonEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<String> ACTION =
			SynchedEntityData.defineId(CompagnonEntity.class, EntityDataSerializers.STRING);
	/**
	 * Change a chaque demande d'animation, meme si c'est la meme qu'avant.
	 *
	 * <p>Sans lui, recliquer deux fois de suite sur la meme case de la roue ne
	 * relancerait rien : la valeur synchronisee serait identique, donc le client
	 * ne verrait aucun changement.
	 */
	private static final EntityDataAccessor<Integer> ACTION_JETON =
			SynchedEntityData.defineId(CompagnonEntity.class, EntityDataSerializers.INT);

	/**
	 * L'objet dont il a envie, montre en image au-dessus de sa tete.
	 *
	 * <p>C'est un identifiant, pas un objet : le serveur decide, le client dessine
	 * ce qu'il connait sous ce nom.
	 */
	private static final EntityDataAccessor<String> ENVIE =
			SynchedEntityData.defineId(CompagnonEntity.class, EntityDataSerializers.STRING);

	/** Le mode, synchronise pour que le client sache quelle pose jouer. */
	private static final EntityDataAccessor<String> MODE =
			SynchedEntityData.defineId(CompagnonEntity.class, EntityDataSerializers.STRING);

	/**
	 * Ce qu'il porte dans la gueule, quand il rapporte quelque chose.
	 *
	 * <p>Synchronise parce que le client doit le dessiner. Voir
	 * {@link RapporterGoal}.
	 */
	/**
	 * Son humeur, pour que le client puisse la <b>montrer</b>.
	 *
	 * <p>Elle vivait dans la fiche, donc uniquement cote serveur, donc
	 * uniquement dans le livre. On pouvait avoir un compagnon au plus mal sans
	 * jamais s'en douter en le regardant — il fallait ouvrir une page pour
	 * l'apprendre.
	 *
	 * <p>Un octet, qui ne change qu'a la minute : le rang de l'humeur dans son
	 * enumeration. Il suffit a choisir une autre animation d'attente, et donc a
	 * ce que l'humeur se lise <b>sur la bete</b>.
	 */
	private static final EntityDataAccessor<Integer> HUMEUR =
			SynchedEntityData.defineId(CompagnonEntity.class, EntityDataSerializers.INT);

	/** Le nœud comportemental actif, visible par l'interface et le diagnostic. */
	private static final EntityDataAccessor<Integer> INTENTION =
			SynchedEntityData.defineId(CompagnonEntity.class, EntityDataSerializers.INT);

	private static final EntityDataAccessor<ItemStack> PORTE =
			SynchedEntityData.defineId(CompagnonEntity.class, EntityDataSerializers.ITEM_STACK);

	/**
	 * Il dort pour de bon.
	 *
	 * <p>Synchronise, parce que c'est le client qui choisit la pose : sans
	 * cela, le serveur saurait qu'il dort et personne ne le verrait.
	 */
	private static final EntityDataAccessor<Boolean> DORT =
			SynchedEntityData.defineId(CompagnonEntity.class, EntityDataSerializers.BOOLEAN);

	/**
	 * Sur la tete plutot que sur l'epaule quand le joueur le porte.
	 *
	 * <p>Synchronise parce que le point d'accroche est calcule des deux cotes :
	 * sans cette valeur, le serveur le poserait sur la tete et les autres joueurs
	 * continueraient de le voir sur l'epaule.
	 */
	private static final EntityDataAccessor<Boolean> SUR_LA_TETE =
			SynchedEntityData.defineId(CompagnonEntity.class, EntityDataSerializers.BOOLEAN);

	private static final String CLE_ESPECE = "Espece";
	private static final String CLE_VARIANTE = "Variante";

	/**
	 * Un depart en promenade tous les N ticks au plus. Le double du reglage de
	 * base : chaque depart coute un calcul de chemin, et mille compagnons dans un
	 * chateau en feraient beaucoup. Valeur inventee.
	 */
	private static final int INTERVALLE_PROMENADE = 240;

	/** A quelle distance il remarque un joueur, en blocs. Valeur inventee. */
	private static final float DISTANCE_REGARD = 8.0F;

	/**
	 * Chance, a chaque examen, qu'il tourne la tete vers quelqu'un. La moitie du
	 * reglage vanilla : c'est ce test qui declenche une recherche d'entites
	 * alentour. Valeur inventee.
	 */
	private static final float PROBABILITE_REGARD = 0.01F;

	/**
	 * Force avec laquelle il ecarte ce qui le traverse. Valeur inventee : assez
	 * pour qu'on ne passe pas au travers, assez peu pour ne pas projeter.
	 */
	private static final double POUSSEE = 0.09D;

	/**
	 * Combien de fois quelqu'un doit s'occuper de lui pour cesser d'etre un
	 * inconnu. Valeur inventee.
	 */
	public static final int FOIS_POUR_ETRE_FAMILIER = 5;

	/** A quelle frequence il oublie ce qui n'existe plus. Cinq secondes. */
	private static final int TICKS_MENAGE = 100;

	/** A quelle frequence on relit l'humeur dans la fiche. Valeur inventee. */
	private static final int TICKS_ENTRAIN = 100;

	/** Duree de fondu entre deux animations, en ticks. Valeur inventee. */
	/**
	 * Le fondu entre deux animations, en ticks.
	 *
	 * <p>Huit plutot que cinq : c'est ce qui separe un geste qui se termine
	 * d'un geste qu'on coupe. A cinq, on voyait encore la bascule ; a huit,
	 * elle se fond dans le mouvement.
	 */
	private static final int FONDU = 8;
	private static final int FONDU_LOCOMOTION = 3;

	/**
	 * Le nom interne du fondu de sortie.
	 *
	 * <p>Ce n'est pas un role d'espece et il n'a pas a l'etre : aucune fiche ne
	 * le declare, il se resout toujours sur l'animation d'attente. Le point
	 * d'exclamation le distingue des roles, qui commencent par une arobase.
	 */
	private static final String ROLE_SORTIE = "!sortie";

	/** Combien de temps dure ce fondu. Le fondu du moteur, plus une marge. */
	private static final int TICKS_DE_SORTIE = FONDU + 2;

	/** Combien de temps une reaction reste sur la couche action. Valeur inventee. */
	private static final int DUREE_REACTION = 40;

	/**
	 * Ce qui distingue un role de reaction, a traduire par la fiche d'espece, d'un
	 * nom d'animation ecrit tel quel.
	 */
	private static final String PREFIXE_ROLE = "@";

	/** Compte a rebours de la reaction en cours ; zero quand il n'y en a pas. */
	private int ticksAction;

	/** La derniere action lancee cote affichage, pour savoir quand elle s'acheve. */
	private String actionJouee = "";

	/** Le jeton de l'action en cours d'affichage. */
	private int actionJoueeJeton = -1;

	/** Son caractere. Pose par la fiche a l'apparition. */
	private Caractere caractere = Caractere.ORDINAIRE;

	/**
	 * Ce qu'il a le droit de faire maintenant.
	 *
	 * <p>Un seul petit geste a la fois, et jamais deux fois le meme coup sur
	 * coup. C'est ce qui permet d'en ajouter beaucoup sans qu'il devienne
	 * illisible. Voir {@link Attention}.
	 */
	private final Attention attention = new Attention();

	/** Mémoire de travail fixe ; la mémoire durable reste dans la fiche. */
	private final MemoireCourte memoireCourte = new MemoireCourte();

	/** Tensions lentes : stress, ennui, besoin de contact et envie d'explorer. */
	private final EtatInterieur etatInterieur = new EtatInterieur();

	/** Il s'habitue aux gestes répétés sans mémoriser une liste d'événements. */
	private final Habituation habituation = new Habituation();

	/** Deux rendez-vous légers avec la journée, bornés à deux dates. */
	private final RythmeQuotidien rythmeQuotidien = new RythmeQuotidien();

	/** Niveau de detail du cerveau, recalcule lentement par la perception. */
	private NiveauActiviteCerveau niveauActiviteCerveau = NiveauActiviteCerveau.PROCHE;

	/** Un geste commencé ne cède qu'à une raison plus importante. */
	private PrioriteAction prioriteAction = PrioriteAction.AMBIANCE;

	/** L'arbre visuel universel qui arbitre locomotion, poses et environnement. */
	private final CerveauAnimation cerveauAnimation = new CerveauAnimation();

	/**
	 * Ses manies et son defaut, recopies de la fiche a l'apparition.
	 *
	 * <p>Recopies, et non relus : {@link Presence} les consulte plusieurs fois
	 * par seconde, et aller chercher la fiche a chaque fois pour trois chaines
	 * qui ne changent jamais serait du gaspillage pur.
	 */
	/** Le dernier maitre trouve, et combien d'appels avant de le rechercher. */
	private LivingEntity maitreEnCache;
	private int avantDeRechercherLeMaitre;

	private java.util.List<String> manies = java.util.List.of();
	private String defaut = "";

	/** Depuis combien de ticks son maitre le fixe sans rien faire d'autre. */
	private int ticksDeRegard;

	/** Combien de ticks il lui reste a etre mouille. */
	private int ticksMouille;

	/** Son entrain, relu dans la fiche toutes les { #TICKS_ENTRAIN} ticks. */
	private float entrain = 1.0F;

	/**
	 * Son energie, relue en meme temps que l'entrain.
	 *
	 * <p>Le pilotage en a besoin a chaque tick, et la fiche vit dans la
	 * sauvegarde : la relire vingt fois par seconde pour un cavalier serait
	 * absurde. On la garde sous la main, elle ne change pas plus vite que ca.
	 */
	private float energieConnue = Barre.MAXIMUM;

	/** Sa complicite, relue au meme rythme lent que son humeur. */
	private float compliciteConnue = Barre.MAXIMUM;

	/** Les attentions du jour deja vues ; -1 evite de feter un chargement. */
	private int rituelsConnus = -1;

	/**
	 * Les gens qu'il connait assez pour aller vers eux de lui-meme.
	 *
	 * <p>Recopie depuis la fiche : les buts la consultent a chaque examen, et aller
	 * chercher la fiche a chaque fois serait du gaspillage.
	 */
	private final java.util.Set<UUID> familiers = new java.util.HashSet<>();

	/**
	 * Vrai s'il vient de bouder un ordre.
	 *
	 * <p>Le seul role de ce drapeau est de garantir que le deuxieme essai passe
	 * toujours. Sans lui, un joueur malchanceux pourrait voir sa bete refuser cinq
	 * fois d'affilee et croire le mod casse. Non sauvegarde exprès : bouder ne
	 * survit pas a une deconnexion.
	 */
	private boolean vientDeRefuser;


	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	/**
	 * La fiche dont cette entite est l'affichage, ou {@code null} pour une entite
	 * de test posee a la main.
	 */
	private UUID fiche;

	public CompagnonEntity(EntityType<? extends CompagnonEntity> type, Level niveau) {
		super(type, niveau);
		// Il ne disparait pas au dechargement d'un chunk (regle 1).
		setPersistenceRequired();
	}

	/**
	 * Sans cet enregistrement (voir le point d'entree du mod), l'entite plante au
	 * spawn et le message d'erreur ne dit pas ce qui manque.
	 *
	 * <p>La vie ici est celle de Minecraft : elle sert au moteur, jamais de barre
	 * de sante. Nos cinq barres sont a nous et arrivent a l'etape 4.
	 */
	public static AttributeSupplier.Builder attributs() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.25D)
				// Une marche entiere, comme un cheval. Sans cela le moindre bloc
				// pose au sol l'arrete net, et il cherche un detour inexistant.
				.add(Attributes.STEP_HEIGHT, 1.0D)
				.add(Attributes.FOLLOW_RANGE, 32.0D);
	}

	/**
	 * Il evite l'eau quand il peut faire autrement.
	 *
	 * <p>Il sait nager — le but de flottaison le garde en surface. Mais il ne
	 * traverse plus une mare parce que c'est le plus court : il prefere en faire le
	 * tour. C'est ce qu'il pense deja tout haut dans son livre, « il deteste avoir
	 * les pattes mouillees » ; c'est maintenant ce qu'il FAIT.
	 *
	 * <p>Un malus de chemin n'interdit rien : il dit seulement « ce passage me
	 * coute cher ». S'il n'y a pas d'autre route, il y va quand meme.
	 */
	private static final float COUT_DE_L_EAU = 12.0F;

	@Override
	protected void registerGoals() {
		// La vie ambiante est complete des la premiere seconde, a tous les niveaux :
		// un compagnon ne doit jamais avoir l'air mort. Ce qui se debloquera plus
		// tard, ce sont les actions volontaires, pas le fait de bouger.
		setPathfindingMalus(PathType.WATER, COUT_DE_L_EAU);
		setPathfindingMalus(PathType.WATER_BORDER, COUT_DE_L_EAU / 2.0F);

		ajouterBut(
				CerveauComportement.Noeud.SURVIE, new FloatGoal(this));
		// Le suivi passe avant la promenade : quand on lui a dit de suivre, il suit.
		// Un ordre passe avant tout le reste, meme avant suivre son maitre : on
		// vient de le lui demander a voix haute.
		// DEUX PRIORITES DIFFERENTES, et ce n'est pas un detail. Elles etaient
		// egales : deux buts de meme rang ne se remplacent jamais l'un l'autre, et
		// « viens » restait donc sans effet tant qu'il etait parti chercher un
		// objet. Un ordre de rappel doit toujours pouvoir interrompre le reste.
		ajouterBut(
				CerveauComportement.Noeud.RAPPEL, new VenirIciGoal(this));
		// Un meuble que le joueur vient de montrer est un ordre précis, au même
		// niveau que le rappel : la vie ambiante ne doit pas pouvoir le détourner.
		ajouterBut(
				CerveauComportement.Noeud.DESTINATION_DEMANDEE,
				new AllerAuPerchoirGoal(this));

		// « Monte. » Un ordre, donc au-dessus du suivi et de la flanerie — mais
		// sous « viens » : si on le rappelle, il redescend.
		ajouterBut(
				CerveauComportement.Noeud.VOL_DEMANDE, new PlanerGoal(this));

		ajouterBut(
				CerveauComportement.Noeud.OBJET_DEMANDE, new ChoperGoal(this));
		ajouterBut(
				CerveauComportement.Noeud.SUIVRE, new SuivreProprietaireGoal(this));
		// Avant les gens et les amis : quand il a un objet dans la gueule, il va
		// jusqu'au bout. Un compagnon qui abandonne un cadeau en chemin est triste.
		// AVANT le glanage : une balle qu'on vient de lui lancer n'est pas un
		// objet qui traine, et il ne doit pas hesiter entre les deux.
		ajouterBut(
				CerveauComportement.Noeud.JEU_DEMANDE, new JouerGoal(this));
		ajouterBut(
				CerveauComportement.Noeud.ABRI, new AbriGoal(this));
		ajouterBut(
				CerveauComportement.Noeud.REPAS, new MangerDansGamelleGoal(this));
		ajouterBut(
				CerveauComportement.Noeud.BOISSON, new BoireDansGamelleGoal(this));
		ajouterBut(
				CerveauComportement.Noeud.SOMMEIL, new SiesteGoal(this));
		ajouterBut(
				CerveauComportement.Noeud.HABITUDE, new AttendreLHeureGoal(this));
		ajouterBut(
				CerveauComportement.Noeud.AFFECTION, new SceneAffectiveGoal(this));
		ajouterBut(
				CerveauComportement.Noeud.CURIOSITE, new AllerVoirGoal(this));
		ajouterBut(
				CerveauComportement.Noeud.CADEAU_SPONTANE, new RapporterGoal(this));
		ajouterBut(
				CerveauComportement.Noeud.JOUEUR, new AllerVersLesGensGoal(this));
		// Il va voir les compagnons qu'il connait. Apres les gens : un ami de
		// passage ne doit pas lui faire ignorer un joueur qui vient le caresser.
		ajouterBut(
				CerveauComportement.Noeud.AMI, new RetrouverGoal(this));

		// « Il se souvient d'ici. » Juste avant la promenade : c'est un moment de
		// flanerie, pas un ordre. Il ne doit jamais interrompre quoi que ce soit,
		// et il ne coute rien tant qu'il fait autre chose.
		ajouterBut(
				CerveauComportement.Noeud.SOUVENIR, new SouvenirDuLieuGoal(this));

		// Deux betes qui se connaissent s'arretent une seconde en se croisant.
		// Tout en bas : ca ne coupe jamais un ordre, une course, ni un repas.
		ajouterBut(
				CerveauComportement.Noeud.SALUT, new SeSaluerGoal(this));
		ajouterBut(
				CerveauComportement.Noeud.FLANERIE, promenade());
		ajouterBut(
				CerveauComportement.Noeud.REGARD_JOUEUR,
				new LookAtPlayerGoal(this, Player.class, DISTANCE_REGARD, PROBABILITE_REGARD));
		// Il regarde ce que tu tiens. Sur la couche du REGARD seulement : il ne
		// quitte pas sa place, donc il peut cohabiter avec a peu pres tout.
		ajouterBut(
				CerveauComportement.Noeud.REGARD_OBJET,
				new RegarderCeQueTuTiensGoal(this));
		ajouterBut(
				CerveauComportement.Noeud.REGARD_LIBRE,
				new RandomLookAroundGoal(this));
	}

	/** Raccord court : chaque but passe par le profil JSON courant. */
	private void ajouterBut(CerveauComportement.Noeud noeud,
			net.minecraft.world.entity.ai.goal.Goal but) {
		CerveauComportement.ajouter(this.goalSelector, noeud, this, but);
	}

	// --- Le caractere ------------------------------------------------------------

	/**
	 * Son petit cerveau. Jamais {@code null} : sans fiche ou sans fichier lisible,
	 * il prend le caractere moyen plutot que de planter.
	 */
	public Caractere caractere() {
		return this.caractere;
	}

	/**
	 * Dort-il ?
	 *
	 * <p>Une vraie sieste, pas une pose demandee : il la prend tout seul et
	 * il en sort tout seul. Voir {@link SiesteGoal}.
	 */
	public boolean dort() {
		return this.entityData.get(DORT);
	}

	public void setDort(boolean dort) {
		if (this.entityData.get(DORT) != dort) {
			this.entityData.set(DORT, dort);
		}
	}

	/**
	 * Son maitre, mis en cache.
	 *
	 * <h2>Ce que ca coutait</h2>
	 *
	 * <p>La version d'origine cherche l'identifiant dans la liste des joueurs
	 * du monde, a chaque appel : c'est un parcours de toute la liste. Huit
	 * classes du mod appellent cette methode, certaines a chaque tick.
	 *
	 * <p>Sur un serveur a mille joueurs et mille compagnons, ca faisait
	 * plusieurs millions de comparaisons par seconde pour retrouver a chaque
	 * fois exactement le meme joueur. Un vrai gaspillage, invisible tant qu'on
	 * teste a deux.
	 *
	 * <h2>Le cache</h2>
	 *
	 * <p>Une vraie recherche tous les {@value #APPELS_AVANT_DE_RECHERCHER}
	 * appels, et immediatement des que le joueur retenu n'est plus valable —
	 * une deconnexion laisse une entite retiree, qu'on repere sans avoir a
	 * chercher quoi que ce soit.
	 *
	 * <p>Le compteur est un nombre d'appels qui descend, jamais une date qu'on
	 * compare : une comparaison de dates avait deja fait voler tous les
	 * compagnons en permanence le jour ou son compteur a deborde.
	 */
	@Override
	public LivingEntity getOwner() {
		if (this.maitreEnCache != null && !this.maitreEnCache.isRemoved()
				&& this.maitreEnCache.isAlive()
				&& --this.avantDeRechercherLeMaitre > 0) {
			return this.maitreEnCache;
		}
		this.avantDeRechercherLeMaitre = APPELS_AVANT_DE_RECHERCHER;
		this.maitreEnCache = super.getOwner();
		return this.maitreEnCache;
	}

	public java.util.List<String> manies() {
		return this.manies;
	}

	/** Son defaut, ou une chaine vide. Il n'en a qu'un et il ne change pas. */
	public String defaut() {
		return this.defaut;
	}

	/**
	 * Peut-il jouer un petit geste par-dessus ce qu'il fait ?
	 *
	 * <h2>Pourquoi ce n'est pas {@link #estLibre()}</h2>
	 *
	 * <p>{@code estLibre()} veut dire « il n'a rien a faire et peut suivre ses
	 * propres envies » : il exige le mode <b>reste</b>. C'est la bonne question
	 * pour un but de l'IA, qui va prendre la main et l'emmener ailleurs.
	 *
	 * <p>Ce n'est pas la bonne pour un geste d'une seconde. Un compagnon passe
	 * l'essentiel de son temps a <b>suivre</b> son maitre — et poser la
	 * mauvaise question revenait a n'avoir aucun de ces gestes pendant tout ce
	 * temps-la, c'est-a-dire presque jamais.
	 *
	 * <p>Ce qui l'en empeche vraiment : une action deja en cours, une pose
	 * demandee — on ne se releve pas pour saluer quand on nous a dit assis —,
	 * un cavalier sur le dos, un objet dans la gueule, et le sommeil.
	 */
	public boolean peutFaireUnPetitGeste() {
		return this.ticksAction == 0 && !estMonte() && !mode().pose()
			&& !dort() && lesMainsVides()
			// Une reaction jouee pendant un trajet gagnait visuellement contre la
			// marche : l'animal glissait. Les gestes attendent maintenant une vraie
			// pause entre deux morceaux du chemin.
			&& getDeltaMovement().horizontalDistanceSqr() < 0.0025D
			&& getNavigation().isDone();
	}

	/** Un ordre compris remplace immédiatement la petite réaction « j'écoute ». */
	public void annulerActionPourOrdre() {
		if (!action().isEmpty()) {
			jouerAction("");
		}
	}

	public Attention attention() {
		return this.attention;
	}

	public MemoireCourte memoireCourte() {
		return this.memoireCourte;
	}

	public EtatInterieur etatInterieur() {
		return this.etatInterieur;
	}

	RythmeQuotidien rythmeQuotidien() {
		return this.rythmeQuotidien;
	}

	/**
	 * Nourrit l'état intérieur avec les capteurs déjà calculés. Aucun balayage du
	 * monde n'est fait ici : même à mille compagnons, cette étape reste une poignée
	 * d'opérations sur des nombres.
	 */
	void actualiserEtatInterieur() {
		this.etatInterieur.actualiser(new EtatInterieur.Observation(
				this.memoireCourte.contient(MemoireCourte.Signal.MAITRE_PROCHE),
				this.memoireCourte.contient(MemoireCourte.Signal.MAITRE_EN_DANGER),
				this.memoireCourte.contient(MemoireCourte.Signal.MENACE),
				this.memoireCourte.contient(MemoireCourte.Signal.EVENEMENT_DU_MONDE),
				this.memoireCourte.contient(MemoireCourte.Signal.AMI_PROCHE),
				this.memoireCourte.contient(MemoireCourte.Signal.ORAGE),
				this.memoireCourte.contient(MemoireCourte.Signal.OBSCURITE),
				this.memoireCourte.contient(MemoireCourte.Signal.EAU),
				getNavigation().isDone()
						&& getDeltaMovement().horizontalDistanceSqr() < 0.0025D,
				dort(), energie() / Barre.MAXIMUM, complicite() / Barre.MAXIMUM,
				this.caractere.courage(), this.caractere.curiosite(),
				this.caractere.attachement(), this.caractere.vivacite(),
				this.caractere.calin()));
	}

	public NiveauActiviteCerveau niveauActiviteCerveau() {
		return this.niveauActiviteCerveau;
	}

	void setNiveauActiviteCerveau(NiveauActiviteCerveau niveau) {
		this.niveauActiviteCerveau = niveau == null
				? NiveauActiviteCerveau.PROCHE : niveau;
	}

	/**
	 * Les envies invisibles sont espacees quand aucun joueur n'est proche. Un but
	 * deja lance continue normalement : cette methode ne sert qu'a son examen.
	 */
	public boolean peutEvaluer(CerveauComportement.Noeud noeud) {
		int cadence = this.niveauActiviteCerveau.cadence(noeud);
		if (cadence <= 1) {
			return true;
		}
		return Math.floorMod(this.tickCount + getUUID().hashCode()
				+ noeud.ordinal() * 17, cadence) == 0;
	}

	/** Retrouve une entité sans la garder en référence dans la mémoire. */
	public Entity entiteMemorisee(MemoireCourte.Signal signal) {
		UUID id = this.memoireCourte.sourceDe(signal);
		return id != null && this.level() instanceof ServerLevel niveau
				? niveau.getEntity(id) : null;
	}

	public CompagnonEntity amiMemorise() {
		return entiteMemorisee(MemoireCourte.Signal.AMI_PROCHE)
				instanceof CompagnonEntity ami && ami.isAlive() ? ami : null;
	}

	/** Compte un pas de plus de regard, et rend le total en ticks. */
	public int compterLeRegard(int pas) {
		this.ticksDeRegard += pas;
		return this.ticksDeRegard;
	}

	public void oublierLeRegard() {
		this.ticksDeRegard = 0;
	}

	/**
	 * Sort-il tout juste de l'eau ?
	 *
	 * <p>Il s'ebroue une fois <b>dehors</b>, pas les vingt fois qu'il passe
	 * dans la riviere. D'ou ce petit compte a rebours plutot qu'un simple
	 * test : sans lui, le geste ne se verrait jamais, il serait toujours joue
	 * les pattes dans l'eau.
	 */
	public boolean vientDeSortirDeLEau() {
		return this.ticksMouille > 0;
	}

	/** Il vient se coller a son maitre, sans rien dire. */
	public void serrerLeMaitre() {
		LivingEntity maitre = getOwner();
		if (maitre != null) {
			getNavigation().moveTo(maitre, 1.0D);
		}
	}

	public void setCaractere(Caractere caractere) {
		this.caractere = caractere == null ? Caractere.ORDINAIRE : caractere;
	}

	/** Quelqu'un vient de s'occuper de lui. */
	public void seSouvenirDe(UUID qui, int fois) {
		if (fois >= FOIS_POUR_ETRE_FAMILIER) {
			this.familiers.add(qui);
		}
	}



	/**
	 * Ce dont il a envie, montre en image au-dessus de sa tete.
	 *
	 * <p>Une chaine vide veut dire qu'il n'a envie de rien. On transporte
	 * l'identifiant de l'objet et non l'objet lui-meme : cela marche aussi bien
	 * pour un biscuit de Minecraft que pour un aliment a nous.
	 */
	public void avoirEnvieDe(String identifiantDObjet) {
		this.entityData.set(ENVIE, identifiantDObjet == null ? "" : identifiantDObjet);
	}

	public String envie() {
		return this.entityData.get(ENVIE);
	}

	/** Quelque chose vient de se passer la. */
	public void remarquer(Vec3 endroit) {
		BlockPos position = BlockPos.containing(endroit);
		if (this.habituation.accepte(position, this.level().getGameTime())) {
			this.memoireCourte.retenir(MemoireCourte.Signal.EVENEMENT_DU_MONDE,
					position, 20 * 8);
		}
	}

	public Vec3 pointDInteret() {
		BlockPos retenu = this.memoireCourte.positionDe(
				MemoireCourte.Signal.EVENEMENT_DU_MONDE);
		return retenu == null ? null : Vec3.atCenterOf(retenu);
	}

	public void oublierLePoint() {
		this.memoireCourte.oublier(MemoireCourte.Signal.EVENEMENT_DU_MONDE);
	}

	/** Vrai si cette personne n'est plus une inconnue pour lui. */
	public boolean connait(java.util.UUID qui) {
		return qui.equals(getOwnerUUID()) || this.familiers.contains(qui);
	}

	/**
	 * Vrai quand il n'a rien a faire et peut suivre ses propres envies.
	 *
	 * <p>Il n'est libre ni quand on lui a dit de rester assis ou couche, ni quand
	 * il suit son proprietaire, ni pendant une action demandee a la roue.
	 * <b>L'ordre du joueur passe toujours avant l'envie de la bete</b> : sans cette
	 * regle, un compagnon a qui on a dit « assis » se leverait pour aller jouer, et
	 * plus personne ne comprendrait a quoi servent les ordres.
	 */
	public boolean vientDeRefuser() {
		return this.vientDeRefuser;
	}

	public void noterRefus(boolean refus) {
		this.vientDeRefuser = refus;
	}

	public boolean estLibre() {
		// Monte, il n'est libre de rien : c'est son cavalier qui decide ou il va.
		return mode() == Mode.RESTE && this.ticksAction == 0 && !estMonte();
	}

	/**
	 * Une monture occupee ou une petite bete perchee ne se laisse pas bousculer.
	 *
	 * <p>Sans cela, chaque bete qui passe deplace la monture, et le cavalier
	 * avec elle : on glisse sur place sans avoir touche a rien. Une bete qu'on
	 * chevauche doit etre stable — c'est la premiere chose qu'on attend d'une
	 * selle.
	 *
	 * <p>Elle reste traversable au sens ou elle peut encore se deplacer
	 * elle-meme : on l'empeche seulement d'etre poussee.
	 */
	@Override
	public boolean isPushable() {
		return !estMonte() && !estPerche() && super.isPushable();
	}

	/**
	 * Porter quelqu'un coute, et voler coute davantage.
	 *
	 * <p>Sans cela, une monture serait un vehicule : on l'enfourche et on
	 * traverse la carte sans jamais y penser. Avec, elle reste une bete — on
	 * regarde sa barre avant de partir loin, on la laisse souffler, on la
	 * nourrit. C'est ce qui rattache la monte a tout le reste du mod plutot que
	 * d'en faire une fonction posee a cote.
	 *
	 * <p>Une fois par seconde, jamais a chaque tick : la barre descend de moins
	 * d'un point par minute quand il ne fait rien, et l'ecrire vingt fois par
	 * seconde ne changerait qu'une chose — le nombre d'ecritures.
	 */
	private void porterFatigue() {
		if (this.tickCount % TICKS_ENTRAIN != 0
				|| this.fiche == null
				|| !(this.level() instanceof ServerLevel niveau)) {
			return;
		}
		Fiches fiches = Fiches.de(niveau.getServer());
		FicheCompagnon sienne = fiches.get(this.fiche);
		if (sienne == null) {
			return;
		}
		// En l'air, il travaille bien plus qu'au sol. Ses competences allegent
		// l'effort comme elles allegent le reste — c'est le meme facteur.
		float cout = (volDemande() ? COUT_DU_VOL : COUT_DU_PAS)
				* fr.lhdp.compagnon.competence.Competences.facteur(
						sienne.competences(), fr.lhdp.compagnon.competence.Competence.ENERGIE);
		sienne.ajouterBarre(Barre.ENERGIE, -cout, Niveaux.progression());
		this.energieConnue = sienne.barre(Barre.ENERGIE);
		fiches.setDirty();

		// A bout de forces, il se pose. On ne le laisse pas tomber du ciel : il
		// refuse simplement de remonter, et plane jusqu'au sol.
		// Une fois, au moment ou il flanche — et non toutes les secondes tant
		// qu'il est a plat. Un message qui se repete cesse d'etre lu.
		boolean aPlat = this.energieConnue <= 0.0F;
		if (aPlat && !this.etaitAPlat
			&& getControllingPassenger() instanceof Player cavalier) {
			cavalier.displayClientMessage(Component.translatable(
					"monte.compagnon.epuise", getName().getString()), true);
		}
		this.etaitAPlat = aPlat;
	}

	/** Vrai si quelqu'un est dessus et le dirige. */
	public boolean estMonte() {
		return getControllingPassenger() != null;
	}

	/**
	 * Il oublie ce qui n'existe plus.
	 *
	 * <h2>La fuite que ca ferme</h2>
	 *
	 * <p>Deux champs retiennent des entites : l'objet qu'on lui a demande d'aller
	 * chercher, et la personne qui vient de l'appeler. Les buts les effacent quand
	 * ils s'arretent — mais un but qui ne DEMARRE jamais ne s'arrete jamais.
	 *
	 * <p>Un joueur qui dit « viens » puis se deconnecte laissait donc son
	 * {@code ServerPlayer} accroche au compagnon, et avec lui tout ce qu'il tient.
	 * A mille joueurs qui vont et viennent, la memoire ne redescend jamais.
	 *
	 * <p>Ce menage coute deux tests de nullite toutes les cinq secondes.
	 */
	private void menage() {
		if (this.appelant != null
				&& (this.appelant.isRemoved() || this.appelant.level() != level())) {
			this.appelant = null;
		}
		if (this.mission != null && this.mission.isRemoved()) {
			this.mission = null;
		}
	}

	// --- Ce qu'il porte dans la gueule ------------------------------------------

	/**
	 * L'objet qu'on lui a demande d'aller chercher, ou {@code null}.
	 *
	 * <p>Vit uniquement cote serveur et uniquement le temps du trajet : ce n'est
	 * pas un etat du compagnon, c'est une course en cours. Voir {@link ChoperGoal}.
	 */
	private ItemEntity mission;

	/**
	 * Celui qui vient de lui dire « viens la », ou {@code null}.
	 *
	 * <p>Cote serveur seulement, et le temps du trajet. Voir {@link VenirIciGoal}.
	 */
	private LivingEntity appelant;

	public LivingEntity quiLAppelle() {
		return this.appelant;
	}

	/** « Viens la ! » — il cherchera un chemin jusqu'a cette personne. */
	public void venirVers(LivingEntity qui) {
		this.appelant = qui;
	}

	public void plusPersonneNeLAppelle() {
		this.appelant = null;
	}

	public ItemEntity mission() {
		return this.mission;
	}

	/**
	 * « Chope ca ! »
	 *
	 * <p>La liste de ses buts s'occupe du reste : il ira, il baissera la tete, et
	 * il gardera l'objet jusqu'a ce qu'on lui dise de le lacher.
	 */
	public void allerChercher(ItemEntity objet) {
		this.mission = objet;
	}

	public void oublierLaMission() {
		this.mission = null;
	}

	public ItemStack porte() {
		return this.entityData.get(PORTE);
	}

	public boolean lesMainsVides() {
		return porte().isEmpty();
	}

	public void prendreDansLaGueule(ItemStack pile) {
		this.entityData.set(PORTE, pile.copy());
	}

	/**
	 * Il pose ce qu'il portait, au sol, devant lui.
	 *
	 * <p>Poser plutot que donner : un objet qui apparait tout seul dans
	 * l'inventaire ne se voit pas, alors qu'un objet qui tombe devant vous est un
	 * cadeau. Et si l'inventaire est plein, il n'y a rien a gerer.
	 */
	/**
	 * Il lache ce qu'il portait sans rien poser au sol.
	 *
	 * <p>A n'appeler QUE lorsque la pile est deja partie ailleurs — dans un
	 * inventaire, par exemple. Sinon l'objet disparait pour de bon, et c'est
	 * exactement ce qu'on passe son temps a eviter.
	 */
	public void viderLaGueule() {
		this.entityData.set(PORTE, ItemStack.EMPTY);
	}

	public void poserCeQuIlPorte() {
		ItemStack pile = porte();
		this.entityData.set(PORTE, ItemStack.EMPTY);
		if (pile.isEmpty() || level().isClientSide()) {
			return;
		}
		spawnAtLocation(pile, 0.4F);
	}

	/**
	 * Quand l'entite s'en va, ce qu'elle portait retourne dans le monde.
	 *
	 * <p><b>Sans ceci, l'objet disparaitrait.</b> L'entite n'est pas sauvegardee —
	 * elle n'est qu'un affichage de la fiche — donc tout ce qu'elle porte au moment
	 * ou le joueur s'eloigne serait perdu pour de bon. Un compagnon qui ramasse
	 * votre pioche en diamant et l'efface serait un defaut inexcusable.
	 */
	@Override
	public void remove(RemovalReason raison) {
		if (!level().isClientSide() && !lesMainsVides()) {
			poserCeQuIlPorte();
		}
		super.remove(raison);
	}

	/**
	 * Son entrain du moment, entre presque rien et un.
	 *
	 * <p>C'est l'humeur traduite en energie : un compagnon affame ou epuise
	 * devient mou <b>quel que soit son caractere</b>. Sans ce lien, les cinq
	 * barres resteraient une decoration du livre sans consequence sur ce qu'on
	 * voit.
	 */
	public float entrain() {
		return this.entrain;
	}

	/**
	 * Son energie, telle que la fiche la connait, de zero au maximum d une barre.
	 *
	 * <p>A ne pas confondre avec {@link #entrain()}, qui traduit l humeur. Une
	 * bete peut etre de tres bonne humeur et vide d energie : ce sont deux
	 * choses differentes, et elles doivent pouvoir se contredire.
	 */
	public float energie() {
		return this.energieConnue;
	}

	/** Sa proximite avec son maitre, de zero a cent. */
	public float complicite() {
		return this.compliciteConnue;
	}

	/**
	 * Relit l'humeur dans la fiche, de temps en temps.
	 *
	 * <p>De temps en temps et pas a chaque tick : l'humeur change lentement, et
	 * chercher la fiche mille fois par seconde serait du gaspillage.
	 */
	private void rafraichirEntrain() {
		if (this.fiche == null || !(this.level() instanceof ServerLevel niveau)) {
			return;
		}
		FicheCompagnon fiche = Fiches.de(niveau.getServer()).get(this.fiche);
		if (fiche == null) {
			return;
		}
		// L'humeur va de la misere au rayonnement : on l'etale entre 0,25 et 1
		// pour qu'un compagnon au plus mal bouge encore un peu.
		int rangs = Humeur.values().length - 1;
		float part = rangs == 0 ? 1.0F : (float) fiche.humeur().ordinal() / rangs;
		this.entrain = 0.25F + 0.75F * part;
		this.energieConnue = fiche.barre(Barre.ENERGIE);
		this.compliciteConnue = fiche.barre(Barre.COMPLICITE);
		celebrerLesRituels(fiche);

		// Et on l'envoie au client, qui n'a aucun autre moyen de la connaitre.
		// Sur le meme rythme que l'entrain : l'humeur ne change pas plus vite.
		if (this.entityData.get(HUMEUR) != fiche.humeur().ordinal()) {
			this.entityData.set(HUMEUR, fiche.humeur().ordinal());
		}
	}

	/** Fete une seule fois le moment ou les trois attentions du jour se rejoignent. */
	private void celebrerLesRituels(FicheCompagnon fiche) {
		int faits = 0;
		long maintenant = System.currentTimeMillis();
		for (SourceXp source : SourceXp.values()) {
			if (fiche.aGagneAujourdhui(source, Niveaux.progression(), maintenant)) {
				faits |= 1 << source.ordinal();
			}
		}
		int tous = (1 << SourceXp.values().length) - 1;
		boolean vientDEtreComplet = this.rituelsConnus >= 0
				&& this.rituelsConnus != tous && faits == tous;
		this.rituelsConnus = faits;
		if (!vientDEtreComplet) {
			return;
		}

		jouerActionPendant("@joie", DUREE_REACTION, PrioriteAction.EVENEMENT);
		Etincelles.progres(this, 10);
		Sons.jouer(this, Sons.CONTENT, 0.9F);
		if (getOwner() instanceof ServerPlayer joueur) {
			joueur.displayClientMessage(Component.translatable(
					"rituel.compagnon.complet", fiche.nom()), true);
		}
	}

	/**
	 * La promenade ambiante.
	 *
	 * <p>Deux differences avec celle de base, et les deux comptent :
	 *
	 * <ul>
	 *   <li><b>Un ordre tient.</b> Assis ou couche, il ne part pas se promener.
	 *       Sans ce garde-fou, un compagnon a qui l'on dit de rester s'en allait
	 *       quand meme.</li>
	 *   <li>Elle est <b>deux fois plus rare</b>. Chaque depart declenche un calcul
	 *       de chemin, et c'est le poste le plus cher quand un chateau porte un
	 *       millier de compagnons a la fois.</li>
	 * </ul>
	 */
	private WaterAvoidingRandomStrollGoal promenade() {
		return new WaterAvoidingRandomStrollGoal(this, 1.0D) {
			{
				// L'intervalle est protege : on ne peut le poser que d'ici.
				this.interval = INTERVALLE_PROMENADE;
			}

			@Override
			public boolean canUse() {
				return !mode().pose() && super.canUse();
			}
		};
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder constructeur) {
		super.defineSynchedData(constructeur);
		constructeur.define(DORT, false);
		constructeur.define(SUR_LA_TETE, false);
		// Vide veut dire "la premiere espece chargee" : aucun nom d'espece n'est
		// ecrit en dur dans le code.
		constructeur.define(ESPECE, "");
		constructeur.define(VARIANTE, "");
		constructeur.define(ACTION, "");
		constructeur.define(ACTION_JETON, 0);
		constructeur.define(MODE, Mode.RESTE.name());
		constructeur.define(PORTE, ItemStack.EMPTY);
		constructeur.define(ENVIE, "");
		constructeur.define(HUMEUR, Humeur.MOYEN.ordinal());
		constructeur.define(INTENTION, CerveauComportement.Noeud.REPOS.ordinal());
	}

	// --- L'espece et la variante sont des donnees, pas du code -----------------

	public String espece() {
		String choisie = this.entityData.get(ESPECE);
		return choisie.isEmpty() ? Especes.premierNom() : choisie;
	}

	public void setEspece(String nom) {
		this.entityData.set(ESPECE, nom);
		// La boite de collision suit l'espece : il faut la recalculer tout de suite,
		// sinon un hibou garderait celle d'un dragonnet jusqu'a son prochain
		// deplacement.
		refreshDimensions();
	}

	/**
	 * La boite de collision vient de la fiche d'espece.
	 *
	 * <p>C'est elle qui empeche de traverser le compagnon, et elle qui dit ou poser
	 * la main quand on le caresse. Elle est en donnee : livrer une espece plus
	 * grande ne demande aucune ligne de code.
	 *
	 * <p>Attention, elle est appelee tres tot pendant la construction de l'entite,
	 * avant meme que ses donnees synchronisees existent. D'ou la prudence.
	 *
	 * <p><b>Limite du moteur :</b> Minecraft ne connait qu'une seule boite par
	 * entite, et elle est carree vue de dessus. Un compagnon long et etroit — un
	 * dragonnet fait 0,73 de large pour 1,98 de long — ne peut donc pas etre
	 * epouse par un seul carre. Pour coller au modele il faudrait plusieurs
	 * morceaux, comme le fait le dragon de l'End.
	 *
	 * <p>C'est bien {@code getDefaultDimensions} qu'il faut redefinir :
	 * {@code getDimensions} est declaree finale par {@code LivingEntity}, qui s'en
	 * sert pour appliquer les poses avant de deleguer ici.
	 */
	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		if (this.entityData == null) {
			return super.getDefaultDimensions(pose);
		}
		Espece espece = Especes.get(espece());
		if (espece == null) {
			return super.getDefaultDimensions(pose);
		}
		return EntityDimensions.scalable(espece.largeur(), espece.hauteur());
	}

	public String variante() {
		return this.entityData.get(VARIANTE);
	}

	public void setVariante(String nom) {
		this.entityData.set(VARIANTE, nom);
	}

	/**
	 * Joue une animation sur la couche action, sans limite de temps ; une chaine
	 * vide l'arrete. Sert a la commande de mise au point, ou l'on veut pouvoir
	 * regarder une pose aussi longtemps qu'on veut.
	 */
	public void jouerAction(String nomAnimation) {
		this.entityData.set(ACTION, nomAnimation);
		this.entityData.set(ACTION_JETON, this.entityData.get(ACTION_JETON) + 1);
		this.ticksAction = 0;
		this.prioriteAction = PrioriteAction.AMBIANCE;
	}

	/** Joue une animation puis revient tout seul a la vie ordinaire. */
	public boolean jouerActionPendant(String nomAnimation, int ticks) {
		return jouerActionPendant(nomAnimation, ticks, PrioriteAction.AMBIANCE);
	}

	/**
	 * Engage une animation. À priorité égale elle ne remplace jamais celle qui
	 * joue déjà : les gestes vont enfin jusqu'à leur dernière image.
	 */
	public boolean jouerActionPendant(String nomAnimation, int ticks,
			PrioriteAction priorite) {
		if (occupe() && !priorite.interrompt(this.prioriteAction)) {
			return false;
		}
		// UN ROLE DURE LE TEMPS DE SA VRAIE ANIMATION.
		//
		// Les anciens gestes faisaient deux secondes environ. Le nouveau dragon
		// en possede qui respirent, regardent puis reviennent a leur pose sur six
		// ou huit secondes. Garder le vieux nombre couperait chaque animation en
		// plein milieu. Le nombre donne par l'appelant reste le repli pour une
		// espece qui ne decrit pas ce role. Dans ce dernier cas on ne la fige pas
		// pour une animation invisible : le geste n'a simplement pas lieu.
		if (nomAnimation.startsWith(PREFIXE_ROLE)) {
			Espece espece = Especes.get(espece());
			String animation = espece == null
					? null : espece.reaction(nomAnimation.substring(PREFIXE_ROLE.length()));
			if (animation == null || animation.isEmpty()) {
				return false;
			}
			ticks = Longueurs.de(animation);
		}
		this.entityData.set(ACTION, nomAnimation);
		this.entityData.set(ACTION_JETON, this.entityData.get(ACTION_JETON) + 1);
		this.ticksAction = Math.max(1, ticks);
		this.prioriteAction = priorite;
		return true;
	}

	/**
	 * Est-il en train de jouer un geste ?
	 *
	 * <p>Un geste va jusqu'au bout. En couper un au milieu pour en lancer un
	 * autre donne deux mouvements tronques colles l'un a l'autre, et c'est
	 * exactement ce qui fait passer une animation debloquee pour un bug.
	 */
	/**
	 * Son cri d'ambiance, selon son humeur.
	 *
	 * <p>On passe par le systeme du jeu plutot que par un compte a rebours a
	 * nous : il espace deja les cris de facon naturelle, il les coupe quand la
	 * bete est loin, et il ne coute rien. Il suffisait de lui repondre.
	 *
	 * <p>Une bete qui va mal emprunte le son de tristesse quand son espece en
	 * declare un. C'est la seule facon dont son humeur s'entend, et elle vaut
	 * toutes les barres du monde.
	 */
	@Override
	protected net.minecraft.sounds.SoundEvent getAmbientSound() {
		if (humeur().ordinal() <= HUMEUR_TRISTE) {
			net.minecraft.sounds.SoundEvent triste = Sons.sonDe(this, Sons.MAL);
			if (triste != null) {
				return triste;
			}
		}
		return Sons.sonDe(this, Sons.AMBIANCE);
	}

	@Override
	protected net.minecraft.sounds.SoundEvent getHurtSound(
			net.minecraft.world.damagesource.DamageSource source) {
		return Sons.sonDe(this, Sons.MAL);
	}

	/**
	 * La hauteur de sa voix, propre a chaque bete.
	 *
	 * <p>C'est ce qui fait qu'on reconnait la sienne a l'oreille dans une salle
	 * commune pleine de compagnons de la meme espece. Ca ne coute rien, ca ne
	 * se sauvegarde pas, et rien d'autre dans le mod ne peut donner ca.
	 */
	@Override
	public float getVoicePitch() {
		return Sons.hauteurDe(this);
	}

	/**
	 * Entre deux cris d'ambiance.
	 *
	 * <p>Bien plus espace que chez les betes du jeu. Un compagnon vit a cote de
	 * son maitre pendant des heures : ce qui passe pour une poule dans un enclos
	 * deviendrait insupportable a un metre de l'oreille.
	 */
	@Override
	public int getAmbientSoundInterval() {
		return TICKS_ENTRE_DEUX_CRIS;
	}

	public boolean occupe() {
		// Le fondu de sortie ne compte pas : le geste est fini, il ne reste que la
		// demi-seconde ou la pose rejoint celle du repos. Refuser un ordre pendant
		// ce temps-la serait refuser pour rien.
		return this.ticksAction > 0 && !ROLE_SORTIE.equals(action());
	}

	public String action() {
		return this.entityData.get(ACTION);
	}

	// --- Le perchoir d'epaule --------------------------------------------------

	/**
	 * Au-dela de cette taille, on ne se perche pas sur une epaule.
	 *
	 * <p>Un demi-bloc de large et trois quarts de haut : la taille d'un
	 * perroquet. Au-dessus, la bete cache l'ecran de son maitre et lui pousse
	 * la tete hors du cadre — ce qui est drole une fois et insupportable la
	 * seconde.
	 */
	private static final float LARGEUR_MAXIMALE = 0.8F;
	private static final float HAUTEUR_MAXIMALE = 0.95F;

	/** De combien il se pose a droite du cou, en blocs. */
	private static final double EPAULE_DE_COTE = 0.32D;

	/**
	 * De combien il monte au-dessus du point d'accroche normal.
	 *
	 * <p><b>C'est le seul nombre a regler en jeu.</b> Trop bas, il flotte dans
	 * la poitrine ; trop haut, il vole au-dessus de la tete. Un essai suffit a
	 * le trouver.
	 */
	private static final double EPAULE_EN_HAUT = 0.72D;

	/** Plus haut et centre : le sommet du crane plutot que le cote du cou. */
	private static final double TETE_EN_HAUT = 1.05D;

	/** Assez petit pour tenir sur une epaule ? */
	public boolean peutSePercher() {
		Espece espece = Especes.get(espece());
		return espece != null
			&& espece.largeur() <= LARGEUR_MAXIMALE
			&& espece.hauteur() <= HAUTEUR_MAXIMALE;
	}

	/** Vrai s'il est en ce moment sur l'epaule de quelqu'un. */
	public boolean estPerche() {
		return getVehicle() instanceof net.minecraft.world.entity.player.Player;
	}

	/** Vrai s'il a choisi le sommet de la tete plutot que l'epaule. */
	public boolean estSurLaTete() {
		return estPerche() && this.entityData.get(SUR_LA_TETE);
	}

	/** Choisit le point du perchoir ; l'entite reste passagere du joueur. */
	public void sePoserSurLaTete(boolean surLaTete) {
		this.entityData.set(SUR_LA_TETE, surLaTete);
	}

	/**
	 * Ou il se pose sur celui qui le porte.
	 *
	 * <h2>Pourquoi ce vecteur est negatif</h2>
	 *
	 * <p>Le jeu place un passager a <b>position du vehicule + son point
	 * d'accroche a lui − le point rendu ici</b>. Ce qu'on rend est donc
	 * l'oppose du deplacement voulu : pour monter sur l'epaule, on descend le
	 * vecteur.
	 *
	 * <h2>Pourquoi on le fait tourner</h2>
	 *
	 * <p>Un vecteur constant est en coordonnees du <b>monde</b> : la bete se
	 * poserait toujours au nord de son maitre, quel que soit le cote ou il
	 * regarde. On tourne donc le decalage lateral avec le cap du porteur, pour
	 * qu'il reste sur la meme epaule.
	 */
	@Override
	public Vec3 getVehicleAttachmentPoint(Entity porteur) {
		if (!(porteur instanceof net.minecraft.world.entity.player.Player)) {
			return super.getVehicleAttachmentPoint(porteur);
		}
		if (this.entityData.get(SUR_LA_TETE)) {
			return new Vec3(0.0D, -TETE_EN_HAUT, 0.0D);
		}
		// Cap zero regarde vers le sud : la droite du porteur est (cos, 0, sin).
		float cap = porteur.getYRot() * net.minecraft.util.Mth.DEG_TO_RAD;
		double droiteX = net.minecraft.util.Mth.cos(cap) * EPAULE_DE_COTE;
		double droiteZ = net.minecraft.util.Mth.sin(cap) * EPAULE_DE_COTE;
		return new Vec3(-droiteX, -EPAULE_EN_HAUT, -droiteZ);
	}

	// --- Le retrouver dans une foule -------------------------------------------

	/**
	 * Combien de ticks il lui reste a briller.
	 *
	 * <h2>Le probleme qu il resout</h2>
	 *
	 * <p>Dans une cour d ecole a deux cents compagnons, tous de trois especes,
	 * beaucoup avec la meme variante : <b>on ne trouve plus le sien</b>. Le nom
	 * flotte au-dessus de chaque bete, ce qui fait deux cents noms.
	 *
	 * <p>Quand tu ouvres sa roue, il s entoure donc d un liseré, visible a
	 * travers les murs, pendant quelques secondes. C est l effet que le jeu
	 * utilise deja pour les cibles d une fleche de reperage : personne n a rien
	 * a apprendre, et ca ne coute aucune texture.
	 *
	 * <p>Rien a sauvegarder : ca dure trois secondes et ca s eteint tout seul.
	 */
	private int ticksDeLueur;

	/** Fais-toi voir, le temps qu on te retrouve. */
	public void brillerPendant(int ticks) {
		this.ticksDeLueur = Math.max(this.ticksDeLueur, ticks);
		setGlowingTag(true);
	}

	// --- Le banc d'essai -------------------------------------------------------

	/**
	 * Vrai pour une bete posee par {@code /compagnon banc}.
	 *
	 * <p>Elle tique exactement comme les autres — c'est tout l'interet, on
	 * mesure le vrai code — mais elle n'a pas de fiche, elle n'appartient a
	 * personne, et elle ne doit surtout pas finir dans la sauvegarde.
	 *
	 * <p>Pas de donnee synchronisee et rien dans le NBT : ce drapeau ne vit que
	 * le temps de la mesure, et c'est precisement ce qu'on veut de lui.
	 */
	private boolean banc;

	public void marquerBanc() {
		this.banc = true;
	}

	public boolean estDuBanc() {
		return this.banc;
	}

	// --- Le lien avec la fiche -------------------------------------------------

	/**
	 * Recopie la fiche dans l'affichage. La fiche reste la verite : tout ce qui
	 * est lu ici sera relu a la prochaine apparition.
	 */
	public void lierA(FicheCompagnon fiche) {
		this.fiche = fiche.id();
		setEspece(fiche.espece());
		setVariante(fiche.variante());
		setCustomName(Component.literal(fiche.nom()));
		setCustomNameVisible(true);
		setOwnerUUID(fiche.proprietaire());
		this.manies = Manies.siennes(fiche);
		this.defaut = Manies.defaut(fiche);
		// Le second argument met les effets d'apprivoisement vanilla (dont la
		// vie) : on ne les veut pas, nos barres sont a nous.
		setTame(true, false);
		setCaractere(Contenu.caractere(fiche.caractere()));
		this.familiers.clear();
		fiche.connaissances().forEach((qui, fois) -> {
			if (fois >= FOIS_POUR_ETRE_FAMILIER) {
				this.familiers.add(qui);
			}
		});
		appliquerMode(fiche.mode());
	}

	/** Le mode tel que l'entite l'affiche. La fiche reste la verite. */
	public Mode mode() {
		return Mode.depuis(this.entityData.get(MODE), Mode.RESTE);
	}

	public void appliquerMode(Mode mode) {
		this.entityData.set(MODE, mode.name());
		setInSittingPose(mode.pose());
		// Un ordre interrompt la reaction en cours. C'est particulierement visible
		// avec la voix : le petit geste « j'ecoute » immobilisait encore la bete
		// presque une seconde apres « viens », puis la faisait partir en retard.
		if (!this.entityData.get(ACTION).isEmpty()) {
			jouerAction("");
		}
		if (mode.pose()) {
			getNavigation().stop();
		}
		// UN ORDRE FAIT TAIRE LES PETITS GESTES.
		//
		// Sans ca, on lui dit « assis » et il baille dans la seconde : le geste
		// se joue par-dessus l'ordre, et on ne sait plus s'il a obei. Ce qui
		// vient du joueur doit avoir le silence autour de lui.
		this.attention.faireSilence(SILENCE_APRES_UN_ORDRE);
	}

	/**
	 * Le compagnon reagit a une caresse. Le nom de l'animation n'est pas ici : on
	 * pose le <b>role</b>, et le client va chercher dans la fiche d'espece ce
	 * qu'il faut jouer. Si la fiche ne dit rien, rien ne s'anime — la complicite
	 * monte quand meme.
	 */
	public void reagirCaresse() {
		jouerActionPendant(PREFIXE_ROLE + Espece.CARESSE, DUREE_REACTION,
				PrioriteAction.AFFECTIF);
	}

	/**
	 * Celebre une montee de niveau avec le geste propre a l'espece.
	 *
	 * <p>Le role {@code niveau} gagne s'il existe. Sinon, une reaction de joie
	 * deja disponible fait parfaitement l'affaire. Sans l'une ni l'autre, les
	 * particules et le son restent visibles et la bete n'est pas immobilisee pour
	 * une animation absente.
	 */
	public void reagirMonteeDeNiveau() {
		Espece espece = Especes.get(espece());
		if (espece == null) {
			return;
		}
		String role = Espece.NIVEAU;
		String animation = espece.reaction(role);
		if (animation == null || animation.isEmpty()) {
			role = Espece.JOIE;
			animation = espece.reaction(role);
		}
		if (animation == null || animation.isEmpty()) {
			return;
		}
		jouerActionPendant(PREFIXE_ROLE + role, Longueurs.de(animation),
				PrioriteAction.EVENEMENT);
	}

	public UUID ficheId() {
		return this.fiche;
	}

	/**
	 * Une entite liee a une fiche n'est <b>jamais</b> ecrite dans le monde : elle
	 * est recreee depuis la fiche quand un joueur approche. L'enregistrer creerait
	 * un doublon a chaque rechargement de la zone.
	 *
	 * <p>Et une bete du banc d'essai n'a pas de fiche : sans la seconde
	 * condition, elle passerait par la porte laissee ouverte pour les autres et
	 * deux cents fausses betes se reveilleraient dans la sauvegarde. C'est le
	 * filet du banc, et il tient meme si le serveur tombe en pleine mesure.
	 */
	@Override
	public boolean shouldBeSaved() {
		return this.fiche == null && !this.banc;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag balise) {
		super.addAdditionalSaveData(balise);
		balise.putString(CLE_ESPECE, this.entityData.get(ESPECE));
		balise.putString(CLE_VARIANTE, this.entityData.get(VARIANTE));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag balise) {
		super.readAdditionalSaveData(balise);
		if (balise.contains(CLE_ESPECE)) {
			this.entityData.set(ESPECE, balise.getString(CLE_ESPECE));
		}
		if (balise.contains(CLE_VARIANTE)) {
			this.entityData.set(VARIANTE, balise.getString(CLE_VARIANTE));
		}
	}

	// --- Interaction ------------------------------------------------------------

	/**
	 * Clic droit sur le compagnon. Tout se decide cote serveur : le client ne fait
	 * qu'accuser reception pour que le bras du joueur bouge.
	 */
	@Override
	public InteractionResult mobInteract(Player joueur, InteractionHand main) {
		if (this.level().isClientSide()) {
			return InteractionResult.sidedSuccess(true);
		}
		if (this.fiche == null || !(joueur instanceof ServerPlayer serveurJoueur)) {
			return InteractionResult.PASS;
		}
		return Interactions.cliqueDroit(this, serveurJoueur, main);
	}

	@Override
	public void tick() {
		// AVANT super.tick(), et c'est important.
		//
		// C'est super.tick() qui fait courir l'IA puis qui applique le mouvement,
		// gravite comprise. Poser le drapeau apres, c'etait le poser trop tard :
		// le premier tick de chaque envol subissait la gravite quand meme, et la
		// bete perdait un cran de hauteur juste au moment de decoller.
		tenirLeVol();

		super.tick();
		actualiserEtatVolVisuel();

		// Le serveur expose la branche qui a gagne. Elle ne change presque jamais,
		// SynchedEntityData n'enverra donc un paquet que lors d'une vraie transition.
		if (!this.level().isClientSide()) {
			this.entityData.set(INTENTION,
					CerveauComportement.actif(this.goalSelector).ordinal());
		}

		// Des deux cotes : le client doit repousser lui aussi, sinon il traverserait
		// puis serait remis en place par le serveur, et tu verrais un elastique.
		if (Chrono.enMarche()) {
			long avant = System.nanoTime();
			repousserDesParties();
			Chrono.COLLISION.ajouter(System.nanoTime() - avant);
		} else {
			repousserDesParties();
		}

		if (this.level().isClientSide()) {
			return;
		}

		// Les capteurs sont cadencés et décalés entre entités. Le chronomètre est
		// entièrement inerte tant que /compagnon perf n'est pas lancé.
		if (Chrono.enMarche()) {
			long avant = System.nanoTime();
			PerceptionCerveau.actualiser(this);
			Chrono.CERVEAU.ajouter(System.nanoTime() - avant);
		} else {
			PerceptionCerveau.actualiser(this);
		}
		if (dort() && PerceptionCerveau.leTourDe(this, 20 * 3, 71)) {
			Etincelles.reve(this);
		}

		if (this.tickCount % TICKS_ENTRAIN == 0) {
			rafraichirEntrain();
		}

		// Pendant une action volontaire, il s'arrete. Sinon il jouerait son
		// animation de sommeil tout en continuant a se promener.
		// Il s arrete pendant le geste, mais pas pendant le fondu qui le suit :
		// celui-la est de l affichage, il n a pas a clouer la bete au sol.
		if (occupe()) {
			getNavigation().stop();
			setDeltaMovement(getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
		}

		// La reaction ne dure qu'un temps : sans ca, la couche action resterait
		// bloquee sur la derniere animation jouee.
		if (this.ticksAction > 0 && --this.ticksAction == 0) {
			// LE GESTE NE S'ARRETE PAS NET.
			//
			// Couper la couche d'action d'un coup fait revenir la bete a sa pose
			// d'attente en une image. C'est ce claquement qu'on voyait a la fin de
			// chaque animation, et qui donnait a un geste qu'on vient de debloquer
			// l'air d'un bug.
			//
			// On enchaine donc sur l'animation d'attente, TOUJOURS SUR LA MEME
			// COUCHE : le moteur d'animation sait fondre d'une animation a l'autre,
			// il ne sait pas fondre vers rien du tout. Le temps du fondu, la pose
			// de la couche d'action rejoint celle du dessous — et quand on l'eteint
			// pour de bon, elles sont identiques : on ne voit plus rien passer.
			if (!ROLE_SORTIE.equals(action())) {
				jouerActionPendant(ROLE_SORTIE, TICKS_DE_SORTIE);
			} else {
				jouerAction("");
			}
		}

		if (this.tickCount % TICKS_MENAGE == 0) {
			menage();
		}

		// La lueur s eteint toute seule. Un compagnon qui reste allume parce qu on
		// a ferme l ecran trop vite serait pire que pas de lueur du tout.
		if (this.ticksDeLueur > 0 && --this.ticksDeLueur == 0) {
			setGlowingTag(false);
		}

		this.attention.avancer();
		if (isInWater()) {
			this.ticksMouille = TICKS_MOUILLE;
		} else if (this.ticksMouille > 0) {
			this.ticksMouille--;
		}
		if (this.tickCount % Presence.TOUS_LES == 0 && !enVol()
				&& !isInWater() && !estMonte()) {
			// Mesure, comme pour les morceaux de collision : on ne devine pas ce
			// que coute un comportement, on le chronometre. Voir /compagnon perf.
			if (Chrono.enMarche()) {
				long avant = System.nanoTime();
				Presence.jouer(this, this.attention);
				Chrono.PRESENCE.ajouter(System.nanoTime() - avant);
			} else {
				Presence.jouer(this, this.attention);
			}
		}

		// MONTE, IL N'A PLUS D'AVIS.
		//
		// Les buts continuent de tourner et de demander des chemins ; le controle
		// de deplacement les suit et fait pivoter la bete. Le cavalier, lui, la
		// fait pivoter aussi — et les deux se disputent le cap a chaque tick, ce
		// qui donne une monture qui tremble et refuse de tourner droit.
		//
		// On coupe la navigation. Les buts peuvent bien vouloir ce qu'ils veulent :
		// sans chemin, ils ne bougent rien. Et estLibre() est deja faux, donc les
		// buts de flanerie ne demarrent meme pas.
		if (estMonte()) {
			getNavigation().stop();
			porterFatigue();
			tenirLeVolDuCavalier();

			// LE CORPS TOURNE AUSSI SUR LE SERVEUR.
			//
			// tickRidden ne tourne que chez le cavalier : le serveur gardait donc
			// l'orientation de corps d'avant la monte. Or c'est elle qui place la
			// selle — le serveur et le client n'etaient pas d'accord sur l'endroit ou
			// se trouve le joueur, ce qui suffit a produire des decalages et des
			// verifications d'etouffement au mauvais endroit.
			this.yBodyRot = tournerDouxVers(this.yBodyRot, getYRot());
			this.yHeadRot = getYRot();
		}
	}

	// --- Le vol ------------------------------------------------------------------

	/**
	 * Combien de ticks il lui reste a battre des ailes.
	 *
	 * <p>Un compte a rebours, et non la date de la derniere demande. La version
	 * en date comparait {@code tickCount - volDemandeA <= 1} avec un depart a
	 * {@link Integer#MIN_VALUE} : la soustraction <b>debordait</b>, rendait un
	 * grand nombre negatif, et la condition etait donc vraie <b>des la premiere
	 * seconde</b>.
	 *
	 * <p>Consequence : toute bete ailee perdait sa gravite et battait des ailes
	 * en permanence, y compris posee dans l'herbe sans rien demander a personne.
	 * Un compte a rebours ne peut pas deborder — il part de zero et n'y revient.
	 *
	 * <p>Deux ticks et non un : le but pose sa demande pendant l'IA, et cette
	 * valeur est lue apres. Un seul tick le ferait clignoter.
	 *
	 * <p>Le principe ne change pas pour autant : personne n'eteint le vol. Il
	 * s'eteint tout seul des que plus aucun but ne le redemande. Voir {@link Vol}.
	 */
	/**
	 * Combien de ticks il continue de voler apres la derniere demande.
	 *
	 * <h2>Pourquoi ce n'est plus 2</h2>
	 *
	 * <p>A deux ticks, il suffisait qu'un seul tick passe sans demande pour que
	 * la gravite revienne. Et un tick sans demande, ca arrive tout le temps : un
	 * but qui reflechit, une condition qui vacille une image, un chemin
	 * momentanement trouve. La bete perdait alors un cran de hauteur, puis
	 * remontait, puis en reperdait un. C'est le vol en dents de scie qu'on
	 * voyait.
	 *
	 * <p>A huit ticks — quatre dixiemes de seconde — un trou passe inapercu,
	 * comme un oiseau qui plane entre deux battements. Et le principe ne change
	 * pas : personne n'eteint le vol, il s'eteint tout seul quand plus aucun but
	 * ne le redemande. Il met simplement une demi-seconde de plus a le faire,
	 * ce qui est exactement le temps qu'il faut pour ne pas le voir.
	 */
	private static final int TICKS_DE_PLANE = 8;

	/** A quelle vitesse il monte quand son cavalier appuie, en blocs par tick. */
	private static final double MONTEE_DU_CAVALIER = 0.32D;

	/** Et a quelle vitesse il redescend. Plus doucement : il se pose. */
	/**
	 * De combien il descend quand on ne demande rien et qu'il est en l'air.
	 *
	 * <p>Assez lent pour que ce soit un vol plane, assez franc pour qu'on
	 * redescende sans avoir a rien faire. Lacher la barre d'espace suffit donc a
	 * se poser : il n'y a aucune touche a apprendre pour descendre.
	 */
	private static final double PLANE_DU_CAVALIER = 0.06D;

	/** Ce que porter quelqu'un coute par seconde, au pas. */
	private static final float COUT_DU_PAS = 0.05F;

	/** Et en vol : voler avec quelqu'un sur le dos est un vrai effort. */
	private static final float COUT_DU_VOL = 0.35F;

	/**
	 * En dessous, il ne prend plus d'altitude.
	 *
	 * <p>Il plane quand meme : on ne tombe jamais du ciel parce qu'une barre est
	 * arrivee a zero. Il refuse de monter, et se pose.
	 */
	private static final float ENERGIE_POUR_MONTER = 5.0F;

	/**
	 * Combien de temps il reste mouille en sortant de l'eau, en ticks.
	 *
	 * <p>Trois secondes : le temps de faire deux pas sur la berge et de
	 * s'ebrouer la ou on le voit.
	 */
	private static final int TICKS_MOUILLE = 20 * 3;

	/**
	 * Combien d'appels a {@code getOwner()} avant de rechercher pour de vrai.
	 *
	 * <p>Vingt : le maitre ne change pas d'identite en une seconde, et une
	 * deconnexion se voit tout de suite par un autre chemin.
	 */
	private static final int APPELS_AVANT_DE_RECHERCHER = 20;

	/** Ce que la vitesse de marche devient quand on le monte. */
	private static final float VITESSE_AU_SOL = 2.2F;

	/** Et en vol : un oiseau qui vole moins vite qu'il ne marche n'a aucun interet. */
	private static final float VITESSE_EN_VOL = 4.5F;

	/**
	 * A quelle vitesse le corps rattrape le cap, par tick.
	 *
	 * <p>Un quart de l'ecart : le corps est aligne en une demi-seconde environ.
	 * Assez lent pour qu'on voie le virage, assez rapide pour qu'on n'ait
	 * jamais l'impression de deraper.
	 */
	private static final float SOUPLESSE_DU_CORPS = 0.25F;

	/** De combien la bete penche du nez quand le cavalier regarde en bas. */
	private static final float INCLINAISON_EN_VOL = 0.5F;

	/** Ce que l'allure rapide multiplie, au sol comme en vol. */
	private static final float GALOP = 1.6F;

	/** La hauteur d'un saut simple, en blocs par tick. */
	private static final double SAUT_EN_MONTURE = 0.52D;

	/** Le premier battement d'ailes, plus fort que les suivants. */
	private static final double DECOLLAGE = 0.62D;

	private int ticksDeVol;

	/** Volait-il au tick precedent ? Sert a ne compter qu'un envol par envol. */
	private boolean volaitAvant;

	/**
	 * La sequence de locomotion actuellement confiee a GeckoLib.
	 *
	 * <p>Elle est reconstruite uniquement lorsque le role change. La recreer a
	 * chaque image ferait repartir une transition de son debut et le dragon ne
	 * parviendrait jamais a sa boucle de marche, de vol ou de repos.
	 */
	private String especeLocomotionJouee = "";
	private String roleLocomotionJoue = "";
	private CerveauAnimation.Noeud noeudAnimationJoue = CerveauAnimation.Noeud.HUMEUR;
	private RawAnimation sequenceLocomotion;
	/** Jusqu'à la fin de la transition engagée, une hésitation ne la relance pas. */
	private long locomotionVerrouilleeJusquaTick;

	/**
	 * Combien de ticks il lui reste a planer parce qu'on le lui a demande.
	 *
	 * <p>Different de {@link #ticksDeVol} : celui-la est la <b>consequence</b>
	 * d'un but qui veut aller quelque part, celui-ci est une <b>envie</b> qu'on
	 * lui a donnee. « Monte » ne le fait aller nulle part : il monte, il tourne,
	 * il redescend quand il en a assez ou quand on le lui dit.
	 *
	 * <p>Il redescend aussi tout seul au bout du compte a rebours : une bete
	 * qu'on aurait laissee en l'air et oubliee serait une bete perdue.
	 */
	private int ticksDeCielLibre;

	/** Un but demande un coup d'ailes pour ce tick. */
	/** « Monte. » Il prend de la hauteur et y reste ce temps-la. */
	public void monterDansLeCiel(int ticks) {
		this.ticksDeCielLibre = saitVoler() ? ticks : 0;
	}

	/** « Descends. » Il arrete de planer ; la gravite fait le reste. */
	public void redescendre() {
		this.ticksDeCielLibre = 0;
	}

	/** Son humeur telle que le client la voit. La fiche reste la verite. */
	public Humeur humeur() {
		Humeur[] toutes = Humeur.values();
		int rang = this.entityData.get(HUMEUR);
		return rang >= 0 && rang < toutes.length ? toutes[rang] : Humeur.MOYEN;
	}

	/** La décision de comportement que le serveur a réellement prise. */
	public CerveauComportement.Noeud intention() {
		CerveauComportement.Noeud[] toutes = CerveauComportement.Noeud.values();
		int rang = this.entityData.get(INTENTION);
		return rang >= 0 && rang < toutes.length ? toutes[rang]
				: CerveauComportement.Noeud.REPOS;
	}

	/** La branche visuelle choisie sur ce client, pour le mode diagnostic. */
	public CerveauAnimation.Noeud noeudAnimationJoue() {
		return this.noeudAnimationJoue;
	}

	/** Le rôle d'animation exact confié à GeckoLib sur ce client. */
	public String roleLocomotionJoue() {
		return this.roleLocomotionJoue;
	}

	/** Vrai s'il a envie de rester en l'air. */
	public boolean veutLeCiel() {
		return this.ticksDeCielLibre > 0;
	}

	/** Un tick de plane consomme. Appele par le but qui le fait planer. */
	public void consommerDuCiel() {
		if (this.ticksDeCielLibre > 0) {
			this.ticksDeCielLibre--;
		}
	}

	public void demanderLeVol() {
		this.ticksDeVol = TICKS_DE_PLANE;
	}

	/** Vrai si son espece a des ailes et sait s'en servir. */
	public boolean saitVoler() {
		Espece espece = Especes.get(espece());
		return espece != null && espece.vole();
	}

	/** Le cerveau de l'espèce actuelle, avec un repli toujours valide. */
	public ProfilCerveau profilCerveau() {
		Espece trouvee = Especes.get(espece());
		return trouvee == null ? ProfilCerveau.parDefaut(false) : trouvee.cerveau();
	}

	/** Vrai depuis qu'il a decolle, jusqu'a ce qu'il retouche le sol. */
	private boolean aVole;

	/** État visuel amorti : un bord de bloc ne replie plus les ailes une image. */
	private boolean enVolVisuel;
	private int ticksVersAutreEtatDeVol;

	/** Vrai si un but lui demande de battre des ailes en ce moment. */
	public boolean volDemande() {
		return this.ticksDeVol > 0;
	}

	/**
	 * Rend la gravite des que plus personne ne demande a voler.
	 *
	 * <p>Et remet la distance de chute a zero pendant le vol : sans cela il
	 * arriverait en haut de la tour et se ferait mal en se posant, pour une chute
	 * qu'il n'a jamais faite.
	 */
	private void tenirLeVol() {
		if (this.ticksDeVol > 0) {
			this.ticksDeVol--;
		}
		boolean vole = volDemande() && saitVoler();
		// UN ENVOL SE COMPTE UNE FOIS. Sur front, jamais par tick : sinon une
		// mission « fais-le voler trois fois » se validerait en trois ticks.
		if (vole && !this.volaitAvant && !level().isClientSide()) {
			fr.lhdp.compagnon.mission.Compteurs.compter(this,
				fr.lhdp.compagnon.mission.Compteurs.ENVOLS);
			// On l'entend et on le voit decoller : c'est le moment le plus
			// spectaculaire de la vie d'une bete qui vole, il ne se passait en
			// silence et sans rien.
			Sons.jouer(this, Sons.ENVOL, 1.2F);
			Etincelles.envol(this);
		}
		this.volaitAvant = vole;

		// ET IL SE POSE.
		//
		// Le decollage se voyait et s'entendait, l'arrivee non : la bete
		// descendait de trente blocs et touchait le sol sans rien du tout. On
		// attend d'avoir vraiment vole, et d'avoir vraiment retouche terre —
		// pas un rebond de deux ticks au bord d'un toit.
		if (vole) {
			this.aVole = true;
		} else if (this.aVole && onGround() && !level().isClientSide()) {
			this.aVole = false;
			Etincelles.atterrissage(this);
			Sons.jouer(this, Sons.ENVOL, 0.55F);
		}

		if (vole != isNoGravity()) {
			setNoGravity(vole);
		}
		if (vole) {
			this.fallDistance = 0.0F;
		}
	}

	// --- On ne traverse pas un compagnon ---------------------------------------

	/**
	 * Repousse ce qui entre dans un de ses morceaux.
	 *
	 * <p>Minecraft ne sait donner qu'une boite carree par entite : on traversait
	 * donc la tete et la queue du dragonnet, qui est trois fois plus long que
	 * large. Le mod fabrique donc ses propres morceaux et fait le travail
	 * lui-meme.
	 *
	 * <p>Une seule recherche d'entites est faite, sur la boite qui englobe tous
	 * les morceaux : c'est le seul appel qui coute vraiment, et il ne se paie
	 * qu'une fois.
	 */
	private void repousserDesParties() {
		Espece espece = Especes.get(espece());
		if (espece == null || espece.parties().isEmpty()) {
			return;
		}

		List<Partie> parties = espece.parties();
		AABB[] boites = new AABB[parties.size()];
		AABB englobante = null;

		// MESURE, pour qu'on ne recommence pas. Voir outils/BancParties.java :
		//
		//   sin/cos par partie  110 ns par compagnon et par tick
		//   sin/cos une fois     32 ns          — soit 71 % de gagne
		//
		// Mais en valeur absolue, 200 compagnons des deux cotes coutent 0,013 ms
		// par tick, sur 50 disponibles : 0,026 %. Le calcul des boites n'est donc
		// PAS un probleme, et il ne sert a rien de l'optimiser davantage.
		//
		// Ce qui n'a jamais ete mesure, en revanche, c'est le getEntities() plus
		// bas : il demande un serveur en marche. Si le jeu rame un jour avec
		// beaucoup de compagnons, c'est LUI qu'il faut regarder.
		//
		// Le sinus et le cosinus une seule fois pour toutes les parties : elles
		// partagent la meme orientation. Voir Partie#boiteMonde.
		double angle = Math.toRadians(this.yBodyRot);
		double sin = Math.sin(angle);
		double cos = Math.cos(angle);
		Vec3 ou = position();

		for (int i = 0; i < boites.length; i++) {
			boites[i] = parties.get(i).boiteMonde(ou, sin, cos);
			englobante = englobante == null ? boites[i] : englobante.minmax(boites[i]);
		}
		if (englobante == null) {
			return;
		}

		// UNE MONTURE NE REPOUSSE PAS CELUI QUI VA LA MONTER.
		//
		// Ses morceaux ecartaient son proprietaire avant qu'il ait pu la toucher :
		// il fallait trouver le seul angle ou le clic passait encore. Sur une bete
		// de trois blocs de haut, ca revenait a chercher un point precis.
		//
		// Elle continue d'ecarter tout le monde — on ne traverse pas un oiseau —
		// mais pas la personne qui doit pouvoir s'en approcher pour s'asseoir
		// dessus.
		java.util.UUID maitre = getOwnerUUID();
		boolean monture = maitre != null && Especes.get(espece()) != null
				&& Especes.get(espece()).seMonte();

		for (Entity autre : this.level().getEntities(this, englobante, Entity::isPushable)) {
			// ON NE POUSSE JAMAIS SON PROPRE PASSAGER.
			//
			// Le cavalier est assis DANS les morceaux de collision — c'est meme la
			// definition d'etre assis dessus. Le mod le chassait donc de sa propre
			// selle a chaque tick, et il glissait sans arret.
			//
			// Cette regle-ci ne depend d'aucun reglage et vaut pour toutes les
			// especes : rien de ce qui est porte ne doit etre repousse.
			if (autre.getVehicle() == this) {
				continue;
			}
			if (monture && autre.getUUID().equals(maitre)) {
				continue;
			}
			// Il ne bouscule pas son maitre quand il le suit. Sinon, plante devant
			// lui, il le pousserait doucement a travers la piece — tres drole, mais
			// insupportable.
			if (mode() == Mode.SUIT && autre.getUUID().equals(getOwnerUUID())) {
				continue;
			}
			for (AABB boite : boites) {
				if (autre.getBoundingBox().intersects(boite)) {
					repousser(autre, boite);
					break;
				}
			}
		}
	}

	/** Ecarte a l'horizontale, jamais vers le haut : on ne le fait pas sauter. */
	private void repousser(Entity autre, AABB boite) {
		Vec3 centre = boite.getCenter();
		double dx = autre.getX() - centre.x;
		double dz = autre.getZ() - centre.z;
		double distance = Math.sqrt(dx * dx + dz * dz);

		if (distance < 1.0E-4D) {
			// Pile au centre : il faut bien choisir un cote.
			dx = 1.0D;
			dz = 0.0D;
			distance = 1.0D;
		}

		double force = POUSSEE / Math.max(distance, 0.3D);
		autre.push(dx / distance * force, 0.0D, dz / distance * force);
	}

	// --- Regle 1 : le compagnon ne meurt jamais --------------------------------
	// Ni chute, ni feu, ni noyade, ni mob, ni joueur. La resistance aux commandes
	// de nettoyage arrivera avec la fiche (etape 3), quand "retirer l'entite" et
	// "supprimer le compagnon" deviendront deux choses differentes.

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		return true;
	}

	@Override
	public boolean causeFallDamage(float distance, float multiplicateur, DamageSource source) {
		return false;
	}

	@Override
	public boolean fireImmune() {
		return true;
	}

	@Override
	public boolean canBeSeenAsEnemy() {
		return false;
	}

	// --- Il ne se reproduit pas ------------------------------------------------

	@Override
	public AgeableMob getBreedOffspring(ServerLevel niveau, AgeableMob partenaire) {
		return null;
	}

	@Override
	public boolean isFood(ItemStack pile) {
		return false;
	}

	// --- Animation -------------------------------------------------------------

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar registre) {
		// LES REPERES POSES DANS L ANIMATION.
		//
		// Blockbench permet de poser des images-cles de son et de particule sur la
		// barre de temps. GeckoLib les fait remonter au moment exact ou la tete de
		// lecture les traverse : le bruit du battement tombe donc sur l image du
		// battement, et ca se regle dans Blockbench, sans recompiler.
		//
		// Et ca ne coute rien sur le reseau : chaque client joue deja l animation,
		// il en connait l instant tout seul. Voir Reperes.
		registre.add(Reperes.ecouter(
			new AnimationController<>(this, "locomotion", FONDU_LOCOMOTION, this::locomotion)));
		registre.add(Reperes.ecouter(
			new AnimationController<>(this, "action", FONDU, this::action)));
	}

	/**
	 * La couche locomotion ne renvoie <b>jamais</b> {@code PlayState.STOP}. Sans
	 * animation, les os retombent sur la pose de geometrie et les ailes se
	 * deploient.
	 */
	private PlayState locomotion(AnimationState<CompagnonEntity> etat) {
		Espece espece = Especes.get(espece());
		if (espece == null) {
			return PlayState.CONTINUE;
		}

		// Un seul arbre, dans un seul ordre, pour toutes les espèces. Les fichiers
		// gardent la liberté artistique ; le code décide seulement quel rôle a la
		// priorité à cet instant.
		CerveauAnimation.Decision decision = this.cerveauAnimation.choisir(
				new CerveauAnimation.Observation(isInWater(), enVol(), dort(), mode(),
						etat.isMoving(), etat.getLimbSwingAmount(), getDeltaMovement().y,
						humeur()),
				roleTeste -> espece.animation(roleTeste) != null, espece.cerveau());
		String role = decision.role();
		this.noeudAnimationJoue = decision.noeud();
		if (!this.roleLocomotionJoue.isEmpty()
				&& !this.roleLocomotionJoue.equals(role)
				&& this.level().getGameTime() < this.locomotionVerrouilleeJusquaTick
				&& memeFamilleVerrouillable(this.roleLocomotionJoue, role)) {
			// Il a commencé à partir, accélérer ou planer : il finit d'abord ce
			// passage au lieu de reconstruire sa séquence à chaque hésitation.
			role = this.roleLocomotionJoue;
		}

		// Un role de pose que la fiche d'espece ne decrit pas retombe sur immobile,
		// jamais sur rien : sans animation, les ailes se deploieraient.
		if (!espece.nom().equals(this.especeLocomotionJouee)
				|| !role.equals(this.roleLocomotionJoue)
				|| this.sequenceLocomotion == null) {
			String avant = this.roleLocomotionJoue;
			this.especeLocomotionJouee = espece.nom();
			this.roleLocomotionJoue = role;
			String transition = transitionLocomotion(espece, avant, role);
			this.sequenceLocomotion = sequenceLocomotion(espece, transition, role);
			long maintenant = this.level().getGameTime();
			this.locomotionVerrouilleeJusquaTick = transition == null ? maintenant
					: maintenant + Math.min(20 * 3, Math.max(2, Longueurs.de(transition)));
			etat.resetCurrentAnimation();
		}
		return etat.setAndContinue(this.sequenceLocomotion);
	}

	/**
	 * Une transition non bouclee, puis la locomotion stable qui lui succede.
	 *
	 * <p>Les transitions restent dans la fiche d'espece : une autre creature qui
	 * ne les possede pas passe directement a sa boucle, sans nom d'animation
	 * dragonnet ecrit dans le code.
	 */
	private static RawAnimation sequenceLocomotion(Espece espece, String transition,
			String apres) {
		RawAnimation sequence = RawAnimation.begin();
		if (transition != null) {
			sequence = sequence.thenPlay(transition);
		}
		return sequence.thenLoop(espece.animationOuImmobile(apres));
	}

	/** Choisit le passage le plus important entre les deux roles visuels. */
	private static String transitionLocomotion(Espece espece, String avant, String apres) {
		if (avant.isEmpty() || avant.equals(apres)) {
			return null;
		}

		String role;
		if (roleAerien(avant) && !roleAerien(apres)) {
			role = Espece.ATTERRISSAGE;
		} else if (!roleAerien(avant) && roleAerien(apres)) {
			role = Espece.DECOLLAGE;
		} else if (Espece.VOL.equals(avant) && Espece.PLANE.equals(apres)) {
			role = Espece.VOL_VERS_PLANE;
		} else if (Espece.PLANE.equals(avant) && Espece.VOL.equals(apres)) {
			role = Espece.PLANE_VERS_VOL;
		} else if (Espece.COUCHE.equals(avant)) {
			role = Espece.REVEIL;
		} else if (Espece.ASSIS.equals(avant)) {
			role = Espece.VERS_DEBOUT;
		} else if (Espece.COUCHE.equals(apres)) {
			role = Espece.VERS_COUCHE;
		} else if (Espece.ASSIS.equals(apres)) {
			role = Espece.VERS_ASSIS;
		} else if (Espece.MARCHE.equals(avant) && Espece.COURSE.equals(apres)) {
			role = Espece.PASSAGE_COURSE;
		} else if (Espece.COURSE.equals(avant) && !roleDeplacementAuSol(apres)) {
			role = Espece.ARRET_COURSE;
		} else if (roleDeplacementAuSol(avant) && !roleDeplacementAuSol(apres)) {
			role = Espece.ARRET_MARCHE;
		} else if (!roleDeplacementAuSol(avant) && roleDeplacementAuSol(apres)) {
			role = Espece.DEPART_MARCHE;
		} else {
			return null;
		}

		String animation = espece.reaction(role);
		return animation == null || animation.isBlank() ? null : animation;
	}

	private static boolean roleAerien(String role) {
		return Espece.VOL.equals(role) || Espece.PLANE.equals(role);
	}

	private static boolean roleDeplacementAuSol(String role) {
		return Espece.MARCHE.equals(role) || Espece.COURSE.equals(role);
	}

	/** Seules les hésitations d'une même famille attendent la fin du passage. */
	private static boolean memeFamilleVerrouillable(String avant, String apres) {
		boolean solAvant = Espece.IMMOBILE.equals(avant) || roleDeplacementAuSol(avant);
		boolean solApres = Espece.IMMOBILE.equals(apres) || roleDeplacementAuSol(apres);
		return solAvant && solApres || roleAerien(avant) && roleAerien(apres);
	}

	/**
	 * Vrai s'il est reellement en l'air.
	 *
	 * <p>On regarde les blocs sous lui, et non {@code onGround()} : cet indicateur
	 * n'est pas fiable cote client pour les entites qu'on ne pilote pas, et le
	 * compagnon se retrouvait alors a jouer l'animation de vol en permanence — il
	 * glissait sur le sol, ailes deployees, au lieu de marcher.
	 *
	 * <p>Il faut deux blocs vides dessous : un simple pas ne doit pas le faire
	 * decoller.
	 */
	private boolean enVol() {
		return this.enVolVisuel;
	}

	/** Met à jour le vol une seule fois par tick, jamais une fois par contrôleur. */
	private void actualiserEtatVolVisuel() {
		boolean brut = etatVolBrut();
		if (brut == this.enVolVisuel) {
			this.ticksVersAutreEtatDeVol = 0;
			return;
		}
		// Un vrai ordre de décollage se voit immédiatement. Les détections dues au
		// terrain, elles, doivent rester stables quelques ticks avant de gagner.
		if (brut && (volDemande() || isNoGravity())) {
			this.enVolVisuel = true;
			this.ticksVersAutreEtatDeVol = 0;
			return;
		}
		if (++this.ticksVersAutreEtatDeVol >= profilCerveau().stabiliteVolTicks()) {
			this.enVolVisuel = brut;
			this.ticksVersAutreEtatDeVol = 0;
		}
	}

	/** Observation brute du terrain, avant l'hystérésis du cerveau. */
	private boolean etatVolBrut() {
		if (!saitVoler()) {
			return false;
		}
		// Un coup d'ailes demande compte tout de suite, avant meme d'avoir
		// quitte le sol : sinon on le voit s'elever pendant deux ou trois ticks
		// les pattes bien a plat, ce qui est exactement le contraire de l'effet
		// recherche.
		// volDemande() n'existe que cote serveur ; isNoGravity() est synchronise
		// tout seul par le jeu. Les deux ensemble donnent la bonne animation des
		// deux cotes, et des la premiere image du decollage.
		if (volDemande() || isNoGravity()) {
			return true;
		}
		if (isInWater()) {
			return false;
		}
		BlockPos sous = BlockPos.containing(getX(), getBoundingBox().minY - 0.2D, getZ());
		return this.level().getBlockState(sous).isAir()
				&& this.level().getBlockState(sous.below()).isAir();
	}

	/**
	 * Sur la couche action, {@code STOP} est normal : il n'y a rien a jouer.
	 *
	 * <p>Une fois l'animation allee au bout, on rend la main a la locomotion au
	 * lieu de figer le compagnon sur sa derniere image jusqu'a ce que le serveur
	 * efface la demande.
	 */
	private PlayState action(AnimationState<CompagnonEntity> etat) {
		// Une reaction de sol lancee juste avant l'envol ne doit pas continuer sur
		// la seconde couche et replier une aile pendant que le vol la deploie.
		if (enVol() || isInWater()) {
			this.actionJouee = "";
			return PlayState.STOP;
		}
		String demande = this.entityData.get(ACTION);
		if (demande.isEmpty()) {
			this.actionJouee = "";
			return PlayState.STOP;
		}
		int jeton = this.entityData.get(ACTION_JETON);
		boolean memeDemande = demande.equals(this.actionJouee) && jeton == this.actionJoueeJeton;
		// La sortie boucle sur l'attente : c'est tick() qui l'eteindra, pas elle.
		if (memeDemande && !ROLE_SORTIE.equals(demande)
			&& etat.getController().hasAnimationFinished()) {
			return PlayState.STOP;
		}
		if (!memeDemande) {
			// GeckoLib ne relance pas une animation qu'il juge identique a celle en
			// cours. Sans cette remise a zero, recliquer la meme case ne ferait rien.
			etat.resetCurrentAnimation();
		}

		// LA SORTIE : l'animation d'attente, jouee sur la couche d'action pour
		// que le moteur fonde vers elle au lieu de couper. Voir tick().
		if (ROLE_SORTIE.equals(demande)) {
			Espece espece = Especes.get(espece());
			if (espece == null) {
				return PlayState.STOP;
			}
			this.actionJouee = demande;
			this.actionJoueeJeton = jeton;
			return etat.setAndContinue(RawAnimation.begin()
				.thenLoop(espece.animationOuImmobile(Espece.IMMOBILE)));
		}

		// Prefixe : un role de reaction, que la fiche d'espece doit traduire.
		// Sans prefixe : un nom d'animation, pose par la commande de mise au point.
		if (demande.startsWith(PREFIXE_ROLE)) {
			Espece espece = Especes.get(espece());
			String nom = espece == null ? null : espece.reaction(demande.substring(1));
			if (nom == null) {
				// La fiche ne decrit pas cette reaction. On ne joue rien plutot que
				// de reclamer une animation qui n'existe pas.
				return PlayState.STOP;
			}
			this.actionJouee = demande;
			this.actionJoueeJeton = jeton;
			return etat.setAndContinue(RawAnimation.begin().thenPlay(nom));
		}

		this.actionJouee = demande;
		this.actionJoueeJeton = jeton;
		return etat.setAndContinue(RawAnimation.begin().thenPlay(demande));
	}

	// --- Le monter ------------------------------------------------------------

	/**
	 * Ce que le cavalier demande a la verticale : 1 monter, -1 descendre, 0 tenir.
	 *
	 * <p>L'avant, l'arriere et les cotes arrivent tout seuls — {@code xxa} et
	 * {@code zza} sont publics et synchronises pour celui qui pilote. Le saut,
	 * lui, ne l'est pas : {@code jumping} est protege. C'est la seule chose que
	 * le client doit nous envoyer, et il ne l'envoie qu'au changement.
	 */
	/**
	 * Ce que le cavalier demande a la verticale : 0 rien, 1 monte, 2 plane.
	 *
	 * <h2>Pourquoi il est pose des DEUX cotes</h2>
	 *
	 * <p>Une monture pilotee par un joueur est simulee <b>par le client de ce
	 * joueur</b> : {@code isControlledByLocalInstance} est vraie chez lui et
	 * fausse sur le serveur, donc {@link #tickRidden} ne tourne que chez lui.
	 *
	 * <p>L'intention n'arrivait qu'au serveur, par paquet. Elle valait donc
	 * toujours zero la ou elle sert, et l'oiseau ne decollait jamais : on
	 * appuyait sur espace et il restait sur place.
	 *
	 * <p>Les touches la posent maintenant <b>sur l'entite du client</b> — qui
	 * fait voler — et l'envoient au serveur — qui tient le drapeau de vol, donc
	 * l'animation d'ailes que voient les autres.
	 */
	private int intentionVerticale;

	/** Entre deux cris d'ambiance : une demi-minute environ. */
	private static final int TICKS_ENTRE_DEUX_CRIS = 20 * 30;

	/**
	 * En dessous de ce rang d humeur, sa voix change.
	 *
	 * <p>Les deux plus mauvaises : au-dela, une bete simplement moyenne aurait
	 * l air malheureuse en permanence, ce qui est faux et fatigant.
	 */
	private static final int HUMEUR_TRISTE = 1;

	/** Le temps de silence apres un ordre, en ticks. Trois secondes. */
	private static final int SILENCE_APRES_UN_ORDRE = 20 * 3;

	/** Etait-il deja a bout de forces ? Sert a ne le dire qu'une fois. */
	private boolean etaitAPlat;

	/** Un saut demande, a consommer au prochain tick de pilotage. */
	private boolean sautDemande;

	/** Le cavalier a demande l'allure rapide. */
	private boolean galop;

	public void poserLIntentionVerticale(int vers) {
		this.intentionVerticale = Math.max(0, Math.min(2, vers));
	}

	/** Le cavalier a tape une fois sur espace : un saut, pas un decollage. */
	public void demanderLeSaut() {
		this.sautDemande = true;
	}

	public void poserLeGalop(boolean vite) {
		this.galop = vite;
	}

	/**
	 * Cote serveur : le drapeau de vol suit ce que fait le cavalier.
	 *
	 * <p>{@code setNoGravity} est une donnee synchronisee : le serveur la
	 * repousse vers le client a chaque envoi. Si le serveur croyait la bete au
	 * sol pendant que le client la fait voler, il lui rendrait la gravite vingt
	 * fois par seconde et elle tomberait par a-coups.
	 *
	 * <p>C'est aussi ce drapeau qui choisit l'animation d'ailes, pour le
	 * cavalier comme pour ceux qui le regardent passer.
	 */
	private void tenirLeVolDuCavalier() {
		// UN APPUI = UN SAUT. C'est la premiere chose qu'une main essaie, et
		// jusqu'ici il ne se passait rien du tout.
		if (this.sautDemande) {
			this.sautDemande = false;
			if (onGround()) {
				setDeltaMovement(getDeltaMovement().x, SAUT_EN_MONTURE, getDeltaMovement().z);
			}
		}

		if (!saitVoler()) {
			return;
		}
		if (this.intentionVerticale > 0) {
			demanderLeVol();
		}
	}

	/**
	 * Peut-il porter quelqu'un, et a partir de quel niveau ?
	 *
	 * <p>La reponse vient de sa fiche d'espece et de son niveau, jamais du code :
	 * une espece qui ne declare pas {@code monter_au_niveau} ne se monte pas.
	 */
	public boolean seLaisseMonter(int sonNiveau) {
		Espece espece = Especes.get(espece());
		return espece != null && espece.seMonte() && sonNiveau >= espece.monterAuNiveau();
	}

	/**
	 * Celui qui tient les renes.
	 *
	 * <p>Uniquement son proprietaire. Un passager qui ne serait pas le maitre est
	 * transporte, mais ne dirige rien — on ne pilote pas la bete de quelqu'un
	 * d'autre parce qu'on a saute dessus.
	 */
	@Override
	public LivingEntity getControllingPassenger() {
		return getFirstPassenger() instanceof Player cavalier
				&& cavalier.getUUID().equals(getOwnerUUID())
				? cavalier
				: null;
	}

	@Override
	protected boolean canAddPassenger(Entity passager) {
		// Une place, et une seule.
		return getPassengers().isEmpty();
	}

	/**
	 * Ou s'assoit le cavalier.
	 *
	 * <p>Sur la selle du modele, dont la position est dans la fiche d'espece : le
	 * code ne lit pas les modeles et ne peut pas la deviner. Sans elle, on
	 * s'assoit au milieu de la bete, ce qui est toujours faux et toujours visible.
	 */
	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passager, EntityDimensions taille,
			float echelle) {

		Espece espece = Especes.get(espece());
		if (espece == null || espece.selle() == null) {
			return super.getPassengerAttachmentPoint(passager, taille, echelle);
		}
		Partie selle = espece.selle();
		// Le meme repere que les morceaux de collision : avant = -sin/cos.
		double angle = Math.toRadians(this.yBodyRot);
		double sin = Math.sin(angle);
		double cos = Math.cos(angle);
		return new Vec3(
				(-cos * selle.droite()) + (-sin * selle.avant()),
				selle.haut(),
				(-sin * selle.droite()) + (cos * selle.avant()));
	}

	/**
	 * Il regarde ou regarde son cavalier.
	 *
	 * <p>Et il vole tant que le cavalier demande a monter. Le vol passe par le
	 * meme chemin que partout ailleurs — {@code demanderLeVol}, redemande a chaque
	 * tick — donc lacher la barre d'espace suffit a redescendre : personne n'a
	 * besoin d'eteindre quoi que ce soit.
	 */
	@Override
	protected void tickRidden(Player cavalier, Vec3 pousse) {
		super.tickRidden(cavalier, pousse);

		// LE CAP EST CELUI DU CAVALIER, TOUT DE SUITE.
		//
		// C'est lui qui decide ou l'on va, et il ne doit y avoir aucun delai
		// entre la souris et la direction : une monture qui repond en retard est
		// une monture qu'on ne sait pas conduire.
		setYRot(cavalier.getYRot());
		this.yRotO = getYRot();

		// LE CORPS, LUI, SUIT AVEC UN TEMPS DE RETARD.
		//
		// Le cap et le corps etaient la meme chose : la bete pivotait sur
		// elle-meme d'un bloc a l'autre, exactement comme une camera. Une bete de
		// trois metres ne fait pas ca.
		//
		// Le controle reste exact — c'est le cap qui commande le deplacement —
		// mais la silhouette s'incline dans le virage. C'est la difference entre
		// piloter un modele et monter un animal.
		this.yBodyRot = tournerDouxVers(this.yBodyRot, getYRot());
		this.yHeadRot = getYRot();

		// LE NEZ NE PIQUE QU'EN VOL. Une bete qui marche et dont le cavalier
		// regarde ses pieds ne plonge pas la tete dans le sol.
		setXRot(volDemande() ? cavalier.getXRot() * INCLINAISON_EN_VOL : 0.0F);

		if (!saitVoler()) {
			return;
		}

		// LE VOL EST UN MODE, PAS UN APPUI.
		//
		// Trois etats arrivent du cavalier : 0 il ne vole pas, 1 il monte, 2 il
		// vole sans monter. La distinction compte : sans elle, un simple saut
		// coupait la gravite et la bete redescendait en flottant pendant dix
		// secondes au lieu de retomber.
		if (this.intentionVerticale <= 0) {
			return;
		}

		if (this.intentionVerticale == 1 && this.energieConnue > ENERGIE_POUR_MONTER) {
			demanderLeVol();
			// LE DECOLLAGE DEPUIS LE SOL.
			//
			// Une bete posee a sa vitesse verticale remise a zero par le sol a
			// chaque tick : lui donner 0,32 ne la decollait pas, elle rebondissait
			// sur place. Le premier battement d'ailes est donc plus fort que les
			// suivants, comme chez un vrai oiseau.
			double pousseHaut = onGround() ? DECOLLAGE : MONTEE_DU_CAVALIER;
			setDeltaMovement(getDeltaMovement().x, pousseHaut, getDeltaMovement().z);
			return;
		}

		// Il vole, mais on ne lui demande plus de monter — soit qu'on ait lache la
		// touche, soit qu'il n'ait plus l'energie de prendre de l'altitude. Il
		// perd doucement du terrain : c'est un oiseau qui plane, pas une pierre
		// qu'on lache, et personne ne tombe du ciel parce qu'une barre est vide.
		demanderLeVol();
		setDeltaMovement(getDeltaMovement().x, -PLANE_DU_CAVALIER, getDeltaMovement().z);
	}

	/**
	 * Ou le cavalier se retrouve en descendant.
	 *
	 * <h2>Pourquoi ne pas laisser faire le jeu</h2>
	 *
	 * <p>Le comportement d'origine pose le cavalier a cote du vehicule, sans
	 * verifier grand-chose. Sur une bete d'un metre et demi de large, ca suffit
	 * la plupart du temps — mais dans un couloir, contre une falaise ou au bord
	 * d'un trou, on descend dans le mur ou dans le vide.
	 *
	 * <p>On essaie donc les quatre cotes, puis les quatre diagonales, et on
	 * garde le premier ou le joueur tient debout. Si aucun ne convient — on est
	 * emmure, ou en plein ciel — on le pose <b>sur la bete</b> plutot que dans
	 * un bloc : au pire il retombe, ce qui est toujours mieux que d'etouffer.
	 */
	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passager) {
		double[][] essais = {
			{1.0D, 0.0D}, {-1.0D, 0.0D}, {0.0D, 1.0D}, {0.0D, -1.0D},
			{0.8D, 0.8D}, {-0.8D, 0.8D}, {0.8D, -0.8D}, {-0.8D, -0.8D}};

		double ecart = getBbWidth() / 2.0D + passager.getBbWidth() / 2.0D + 0.2D;
		for (double[] essai : essais) {
			Vec3 ou = position().add(essai[0] * ecart, 0.0D, essai[1] * ecart);
			AABB place = passager.getDimensions(passager.getPose())
				.makeBoundingBox(ou);
			if (level().noCollision(passager, place)) {
				return ou;
			}
		}
		return position();
	}

	/**
	 * Fait tourner un angle vers un autre, sans le rattraper d'un coup.
	 *
	 * <p>Les angles tournent en rond : il faut ramener l'ecart entre -180 et
	 * 180, faute de quoi une bete qui passe de 179 a -179 degres fait un tour
	 * complet sur elle-meme pour un demi-degre de virage.
	 */
	private static float tournerDouxVers(float depuis, float vers) {
		float ecart = net.minecraft.util.Mth.wrapDegrees(vers - depuis);
		return depuis + ecart * SOUPLESSE_DU_CORPS;
	}

	@Override
	protected Vec3 getRiddenInput(Player cavalier, Vec3 pousse) {
		// De cote a demi-vitesse, et l'arriere plus lentement encore : c'est ce que
		// fait le jeu pour ses propres montures, et ce a quoi la main s'attend.
		float cotes = cavalier.xxa * 0.5F;
		float devant = cavalier.zza;
		if (devant < 0.0F) {
			devant *= 0.3F;
		}
		return new Vec3(cotes, 0.0D, devant);
	}

	@Override
	protected float getRiddenSpeed(Player cavalier) {
		// Plus vite en l'air qu'au sol : un oiseau qui vole moins vite qu'il ne
		// marche n'a aucun interet.
		float base = (float) getAttributeValue(Attributes.MOVEMENT_SPEED);
		float allure = volDemande() ? VITESSE_EN_VOL : VITESSE_AU_SOL;

		// PIQUER ACCELERE, CABRER RALENTIT.
		//
		// C'est ce que fait un oiseau, et c'est surtout ce qui donne au vol une
		// commande de plus sans une touche de plus : on gagne de la vitesse en
		// regardant vers le bas, on la depense en remontant.
		if (volDemande()) {
			float piquer = cavalier.getXRot() / 90.0F;
			allure *= 1.0F + Math.max(-0.35F, Math.min(0.5F, piquer * 0.5F));
		}
		// L'ALLURE RAPIDE, sur deux appuis d'avance — la meme habitude que la
		// course a pied. Elle ne sert a rien si la bete est videe : une monture
		// epuisee ralentit au lieu de refuser, ce qui se comprend sans un mot.
		if ((this.galop || cavalier.isSprinting()) && this.energieConnue > ENERGIE_POUR_MONTER) {
			allure *= GALOP;
		}
		return base * allure;
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}
}
