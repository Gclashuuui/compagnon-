package fr.lhdp.compagnon.fiche;

import fr.lhdp.compagnon.contenu.Contenu;
import fr.lhdp.compagnon.progression.Progression;
import fr.lhdp.compagnon.progression.SourceXp;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * La fiche d'un compagnon : la verite.
 *
 * <p>Le compagnon n'est pas une entite Minecraft, c'est cette fiche. L'entite en
 * jeu n'en est qu'un affichage temporaire, cree quand un joueur approche et
 * retire quand plus personne n'est la. On ne peut donc pas perdre un compagnon,
 * et le livre peut s'ouvrir meme quand il est a l'autre bout du chateau.
 *
 * <p>L'historique est enregistre <b>des le premier jour</b>. Si on ne le stockait
 * pas maintenant, ces pages seraient vides pour tous les joueurs existants le
 * jour ou on les ajouterait, et c'est impossible a rattraper.
 *
 * <p>Une cle absente prend sa valeur par defaut a la lecture : le format peut
 * grandir sans casser les sauvegardes deja ecrites.
 */
public class FicheCompagnon {

	// Compteurs cumules nommes par le cahier des charges. La carte reste ouverte :
	// ajouter un compteur n'oblige a toucher ni cette classe ni la sauvegarde.
	/**
	 * Combien de compteurs differents une fiche garde au plus.
	 *
	 * <p>Soixante-quatre : de quoi tenir tout ce que le mod compte aujourd hui,
	 * les quatre places de missions comprises, avec de la marge. La carte etant
	 * ouverte, sans plafond une faute de frappe dans une cle de mission ferait
	 * grossir la sauvegarde de mille joueurs sans que personne ne le voie.
	 */
	private static final int PLAFOND_DES_COMPTEURS = 64;

	public static final String REPAS = "repas";
	public static final String CARESSES = "caresses";
	public static final String BALADES = "balades";
	public static final String SOINS = "soins";

	/** Un jour reel, en millisecondes. C'est l'unite des plafonds d'experience. */
	private static final long MILLIS_PAR_JOUR = 86_400_000L;

	/** Sert seulement au tirage du caractere a la naissance. */
	private static final java.util.Random HASARD = new java.util.Random();

	private static final String CLE_ID = "Id";
	private static final String CLE_PROPRIETAIRE = "Proprietaire";
	private static final String CLE_ESPECE = "Espece";
	private static final String CLE_VARIANTE = "Variante";
	private static final String CLE_NOM = "Nom";
	private static final String CLE_DIMENSION = "Dimension";
	private static final String CLE_X = "X";
	private static final String CLE_Y = "Y";
	private static final String CLE_Z = "Z";
	private static final String CLE_ROTATION = "Rotation";
	private static final String CLE_MODE = "Mode";
	private static final String CLE_SORTI = "Sorti";
	private static final String CLE_DATE_OBTENTION = "DateObtention";
	private static final String CLE_TICKS_ENSEMBLE = "TicksEnsemble";
	private static final String CLE_VU_LE = "VuLe";
	private static final String CLE_MOMENTS = "Moments";
	private static final String CLE_COMPTEURS = "Compteurs";
	private static final String CLE_MOMENT_CLE = "Cle";
	private static final String CLE_MOMENT_DATE = "Date";
	private static final String CLE_BARRES = "Barres";
	private static final String CLE_XP = "Xp";
	private static final String CLE_JOUR = "Jour";
	private static final String CLE_XP_DU_JOUR = "XpDuJour";
	private static final String CLE_BOBO = "Bobo";
	private static final String CLE_BOBO_DEPUIS = "BoboDepuis";
	private static final String CLE_CARACTERE = "Caractere";
	private static final String CLE_CONNAISSANCES = "Connaissances";
	private static final String CLE_MOTS_APPRIS = "MotsAppris";
	private static final String CLE_LIEUX = "Lieux";
	private static final String CLE_HEURES_REPAS = "HeuresRepas";
	private static final String CLE_COMPETENCES = "Competences";
	private static final String CLE_LIEU_CLE = "Quoi";
	private static final String CLE_LIEU_X = "Lx";
	private static final String CLE_LIEU_Y = "Ly";
	private static final String CLE_LIEU_Z = "Lz";
	private static final String CLE_LIEU_QUAND = "Lquand";
	private static final String CLE_MOT = "Mot";
	private static final String CLE_GESTE = "Geste";
	private static final String CLE_CONNU = "Qui";
	private static final String CLE_FOIS = "Fois";

	private final UUID id;
	private final UUID proprietaire;
	private String espece;
	private String variante;
	private String nom;

	private ResourceKey<Level> dimension;
	private double x;
	private double y;
	private double z;
	private float rotation;

	private Mode mode;

	/**
	 * Vrai s'il a le droit d'exister dans le monde.
	 *
	 * <h2>Ce que ca veut dire, et ce que ca ne veut pas dire</h2>
	 *
	 * <p>Faux ne veut pas dire mort, ni perdu, ni endormi : la fiche est
	 * intacte, ses barres continuent de vivre, son livre s'ouvre. Il n'est
	 * simplement <b>pas la</b>. C'est la difference entre un compagnon reste
	 * dans une chambre a l'autre bout du chateau — celui-la est sorti, mais
	 * personne n'est assez pres pour qu'il apparaisse — et un compagnon range,
	 * qui n'apparaitra pour personne, ou qu'on aille.
	 *
	 * <p><b>Vrai par defaut, et surtout pour les fiches ecrites avant que ce
	 * champ existe.</b> Le contraire ferait disparaitre tous les compagnons du
	 * serveur au premier redemarrage apres la mise a jour.
	 */
	private boolean sorti = true;

	// --- Les cinq barres ---
	private final Map<Barre, Float> barres;
	private int xp;

	// --- Les plafonds par jour reel ---
	private long jour;

	/** Le jour reel en cours. Sert aussi de graine a ce dont il a envie. */
	public long jour() {
		return this.jour;
	}
	private final Map<SourceXp, Integer> xpDuJour;

	/**
	 * Son caractere, tire au sort a la naissance et fige pour toujours.
	 *
	 * <p>C'est ce qui fait que deux compagnons de meme espece et de meme couleur
	 * ne se comportent pas pareil.
	 */
	private final String caractere;

	/**
	 * Les gens qu'il connait, et combien de fois chacun l'a caresse.
	 *
	 * <p>Un joueur qui s'occupe de lui souvent finit par ne plus etre un inconnu :
	 * le compagnon vient vers lui de lui-meme. C'est ce qui fait qu'un couloir de
	 * chateau a une memoire.
	 */
	private final Map<UUID, Integer> connaissances;

	/**
	 * Les mots que <b>celui-ci</b> a appris, et ce qu'il fait en les entendant.
	 *
	 * <p>Du mot vers le nom d'animation. C'est ce qui rend deux compagnons de
	 * meme espece differents pour de bon : ils ne savent pas les memes tours,
	 * et on ne les appelle pas de la meme facon.
	 *
	 * <p>Un mot est toujours verifie dans le dictionnaire du micro avant d'etre
	 * accepte : un mot que le moteur ne sait pas dire ne serait jamais entendu,
	 * et le joueur croirait avoir dresse sa bete pour rien.
	 */
	private final Map<String, String> motsAppris;

	/**
	 * Les endroits ou il lui est arrive quelque chose.
	 *
	 * <p>Pas des chemins — ceux-la coutent cher et ne se voient pas. Des
	 * <b>endroits</b> : la ou on l'a soigne, la ou il a mange pour la premiere
	 * fois, la ou il a rencontre un ami.
	 *
	 * <p>Quand il y repasse, il ralentit et il regarde. Rien n'est ecrit, rien
	 * n'est explique. C'est au joueur de se souvenir de ce qui s'y est passe —
	 * et il s'en souvient, parce que c'est lui qui y etait.
	 */
	private final List<Lieu> lieux;

	/**
	 * Les heures du monde ou on l'a nourri, les dernieres d'abord effacees.
	 *
	 * <p>Quatre entiers entre 0 et 23. C'est tout ce qu'il faut pour qu'une
	 * bete sache <b>a quelle heure on s'occupe d'elle</b> — et pour qu'elle
	 * aille attendre a cette heure-la, sans qu'on le lui ait jamais demande.
	 */
	private final List<Integer> heuresDeRepas;

	/**
	 * Ce qu'il a appris a etre.
	 *
	 * <p>Des identifiants, jamais les competences elles-memes : celles-ci
	 * vivent dans les fichiers du serveur et peuvent changer, disparaitre,
	 * revenir. Une fiche qui garderait leur contenu se retrouverait un jour a
	 * appliquer des regles que plus personne n'a ecrites.
	 *
	 * <p>Un identifiant qui ne correspond plus a rien est simplement ignore —
	 * et il n'est pas efface : le jour ou le fichier revient, la competence
	 * revient avec lui. On ne prend pas a un joueur ce qu'il a choisi parce
	 * qu'un fichier a bouge.
	 */
	private final java.util.LinkedHashSet<String> competences;

	// --- Le bobo en cours ---
	/** Identifiant du bobo, ou une chaine vide s'il va bien. Un seul a la fois. */
	private String bobo;
	private long boboDepuis;

	// --- Historique ---
	private final long dateObtention;
	private long ticksEnsemble;

	/**
	 * La derniere fois qu'il a passe du temps dehors avec son maitre, en
	 * millisecondes reelles. Zero s'il n'a jamais eu l'occasion.
	 *
	 * <p>En temps <b>reel</b> et non en temps de jeu : c'est l'absence du
	 * joueur qu'on mesure, pas celle du personnage. Une semaine sans se
	 * connecter est une semaine, meme si le monde n'a pas tourne.
	 *
	 * <p>Absente d'une vieille sauvegarde, la cle vaut zero, et le premier
	 * retour ne declenche rien : on ne fete pas des retrouvailles qu'on n'a
	 * pas vecues.
	 */
	private long vuLe;
	private final List<Moment> moments;
	private final Map<String, Integer> compteurs;

	public FicheCompagnon(UUID id, UUID proprietaire, String espece, String variante, String nom,
			ResourceKey<Level> dimension, double x, double y, double z, float rotation,
			long dateObtention, Progression table) {
		this.id = id;
		this.proprietaire = proprietaire;
		this.espece = espece;
		this.variante = variante;
		this.nom = nom;
		this.dimension = dimension;
		this.x = x;
		this.y = y;
		this.z = z;
		this.rotation = rotation;
		this.mode = Mode.RESTE;
		this.dateObtention = dateObtention;
		this.ticksEnsemble = 0L;
		this.vuLe = 0L;
		this.moments = new ArrayList<>();
		this.compteurs = new LinkedHashMap<>();

		this.barres = new EnumMap<>(Barre.class);
		for (Barre barre : Barre.values()) {
			this.barres.put(barre, table.barres().get(barre).depart());
		}
		this.xp = 0;
		this.jour = dateObtention / MILLIS_PAR_JOUR;
		this.xpDuJour = new EnumMap<>(SourceXp.class);
		this.bobo = "";
		this.boboDepuis = 0L;
		this.caractere = Contenu.caractereAuHasard(HASARD).id();
		this.connaissances = new LinkedHashMap<>();
		this.motsAppris = new LinkedHashMap<>();
		this.lieux = new ArrayList<>();
		this.heuresDeRepas = new ArrayList<>();
		this.competences = new java.util.LinkedHashSet<>();
	}

	private FicheCompagnon(CompoundTag balise) {
		this.id = balise.getUUID(CLE_ID);
		this.proprietaire = balise.getUUID(CLE_PROPRIETAIRE);
		this.espece = balise.getString(CLE_ESPECE);
		this.variante = balise.getString(CLE_VARIANTE);
		this.nom = balise.getString(CLE_NOM);
		this.dimension = ResourceKey.create(Registries.DIMENSION,
				ResourceLocation.parse(balise.getString(CLE_DIMENSION)));
		this.x = balise.getDouble(CLE_X);
		this.y = balise.getDouble(CLE_Y);
		this.z = balise.getDouble(CLE_Z);
		this.rotation = balise.getFloat(CLE_ROTATION);
		this.mode = Mode.depuis(balise.getString(CLE_MODE), Mode.RESTE);
		// Absent = sorti. Une sauvegarde d'avant ce champ ne doit pas ranger
		// mille compagnons d'un coup.
		this.sorti = !balise.contains(CLE_SORTI) || balise.getBoolean(CLE_SORTI);
		this.dateObtention = balise.getLong(CLE_DATE_OBTENTION);
		this.ticksEnsemble = balise.getLong(CLE_TICKS_ENSEMBLE);
		this.vuLe = balise.getLong(CLE_VU_LE);

		this.moments = new ArrayList<>();
		ListTag listeMoments = balise.getList(CLE_MOMENTS, Tag.TAG_COMPOUND);
		for (int i = 0; i < listeMoments.size(); i++) {
			CompoundTag moment = listeMoments.getCompound(i);
			this.moments.add(new Moment(moment.getString(CLE_MOMENT_CLE), moment.getLong(CLE_MOMENT_DATE)));
		}

		this.compteurs = new LinkedHashMap<>();
		CompoundTag compteursBalise = balise.getCompound(CLE_COMPTEURS);
		for (String cle : compteursBalise.getAllKeys()) {
			this.compteurs.put(cle, compteursBalise.getInt(cle));
		}

		// Une barre absente de la sauvegarde repart au maximum : une fiche ecrite
		// avant l'arrivee des barres se relit sans casser.
		this.barres = new EnumMap<>(Barre.class);
		CompoundTag barresBalise = balise.getCompound(CLE_BARRES);
		for (Barre barre : Barre.values()) {
			this.barres.put(barre, barresBalise.contains(barre.cle())
					? barresBalise.getFloat(barre.cle())
					: Barre.MAXIMUM);
		}

		this.xp = balise.getInt(CLE_XP);
		this.jour = balise.getLong(CLE_JOUR);

		this.xpDuJour = new EnumMap<>(SourceXp.class);
		CompoundTag jourBalise = balise.getCompound(CLE_XP_DU_JOUR);
		for (SourceXp source : SourceXp.values()) {
			if (jourBalise.contains(source.cle())) {
				this.xpDuJour.put(source, jourBalise.getInt(source.cle()));
			}
		}

		this.bobo = balise.getString(CLE_BOBO);
		this.boboDepuis = balise.getLong(CLE_BOBO_DEPUIS);

		// Une fiche ecrite avant l'arrivee des caracteres en recoit un maintenant,
		// plutot que de rester sans.
		this.caractere = balise.contains(CLE_CARACTERE)
				? balise.getString(CLE_CARACTERE)
				: Contenu.caractereAuHasard(HASARD).id();

		this.connaissances = new LinkedHashMap<>();
		ListTag listeConnues = balise.getList(CLE_CONNAISSANCES, Tag.TAG_COMPOUND);
		for (int i = 0; i < listeConnues.size(); i++) {
			CompoundTag connu = listeConnues.getCompound(i);
			this.connaissances.put(connu.getUUID(CLE_CONNU), connu.getInt(CLE_FOIS));
		}

		this.motsAppris = new LinkedHashMap<>();
		ListTag listeMots = balise.getList(CLE_MOTS_APPRIS, Tag.TAG_COMPOUND);
		for (int i = 0; i < listeMots.size(); i++) {
			CompoundTag appris = listeMots.getCompound(i);
			this.motsAppris.put(appris.getString(CLE_MOT), appris.getString(CLE_GESTE));
		}

		this.competences = new java.util.LinkedHashSet<>();
		for (String prise : balise.getString(CLE_COMPETENCES).split(",")) {
			if (!prise.isBlank()) {
				this.competences.add(prise.trim());
			}
		}

		this.heuresDeRepas = new ArrayList<>();
		for (int heure : balise.getIntArray(CLE_HEURES_REPAS)) {
			if (this.heuresDeRepas.size() < REPAS_RETENUS && heure >= 0 && heure < 24) {
				this.heuresDeRepas.add(heure);
			}
		}

		this.lieux = new ArrayList<>();
		ListTag listeLieux = balise.getList(CLE_LIEUX, Tag.TAG_COMPOUND);
		for (int i = 0; i < listeLieux.size() && i < LIEUX_MAXIMUM; i++) {
			CompoundTag lieu = listeLieux.getCompound(i);
			this.lieux.add(new Lieu(lieu.getString(CLE_LIEU_CLE),
					lieu.getInt(CLE_LIEU_X), lieu.getInt(CLE_LIEU_Y), lieu.getInt(CLE_LIEU_Z),
					lieu.getLong(CLE_LIEU_QUAND)));
		}
	}

	public static FicheCompagnon charger(CompoundTag balise) {
		return new FicheCompagnon(balise);
	}

	public CompoundTag sauver() {
		CompoundTag balise = new CompoundTag();
		balise.putUUID(CLE_ID, this.id);
		balise.putUUID(CLE_PROPRIETAIRE, this.proprietaire);
		balise.putString(CLE_ESPECE, this.espece);
		balise.putString(CLE_VARIANTE, this.variante);
		balise.putString(CLE_NOM, this.nom);
		balise.putString(CLE_DIMENSION, this.dimension.location().toString());
		balise.putDouble(CLE_X, this.x);
		balise.putDouble(CLE_Y, this.y);
		balise.putDouble(CLE_Z, this.z);
		balise.putFloat(CLE_ROTATION, this.rotation);
		balise.putString(CLE_MODE, this.mode.name());
		balise.putBoolean(CLE_SORTI, this.sorti);
		balise.putLong(CLE_DATE_OBTENTION, this.dateObtention);
		balise.putLong(CLE_TICKS_ENSEMBLE, this.ticksEnsemble);
		balise.putLong(CLE_VU_LE, this.vuLe);

		ListTag listeMoments = new ListTag();
		for (Moment moment : this.moments) {
			CompoundTag baliseMoment = new CompoundTag();
			baliseMoment.putString(CLE_MOMENT_CLE, moment.cle());
			baliseMoment.putLong(CLE_MOMENT_DATE, moment.date());
			listeMoments.add(baliseMoment);
		}
		balise.put(CLE_MOMENTS, listeMoments);

		CompoundTag compteursBalise = new CompoundTag();
		this.compteurs.forEach(compteursBalise::putInt);
		balise.put(CLE_COMPTEURS, compteursBalise);

		CompoundTag barresBalise = new CompoundTag();
		this.barres.forEach((barre, valeur) -> barresBalise.putFloat(barre.cle(), valeur));
		balise.put(CLE_BARRES, barresBalise);

		balise.putInt(CLE_XP, this.xp);
		balise.putLong(CLE_JOUR, this.jour);

		CompoundTag jourBalise = new CompoundTag();
		this.xpDuJour.forEach((source, gagne) -> jourBalise.putInt(source.cle(), gagne));
		balise.put(CLE_XP_DU_JOUR, jourBalise);

		balise.putString(CLE_CARACTERE, this.caractere);

		ListTag listeConnues = new ListTag();
		this.connaissances.forEach((qui, fois) -> {
			CompoundTag connu = new CompoundTag();
			connu.putUUID(CLE_CONNU, qui);
			connu.putInt(CLE_FOIS, fois);
			listeConnues.add(connu);
		});
		balise.put(CLE_CONNAISSANCES, listeConnues);

		ListTag listeMots = new ListTag();
		this.motsAppris.forEach((mot, geste) -> {
			CompoundTag appris = new CompoundTag();
			appris.putString(CLE_MOT, mot);
			appris.putString(CLE_GESTE, geste);
			listeMots.add(appris);
		});
		balise.put(CLE_MOTS_APPRIS, listeMots);

		ListTag listeLieux = new ListTag();
		for (Lieu lieu : this.lieux) {
			CompoundTag ou = new CompoundTag();
			ou.putString(CLE_LIEU_CLE, lieu.cle());
			ou.putInt(CLE_LIEU_X, lieu.x());
			ou.putInt(CLE_LIEU_Y, lieu.y());
			ou.putInt(CLE_LIEU_Z, lieu.z());
			ou.putLong(CLE_LIEU_QUAND, lieu.quand());
			listeLieux.add(ou);
		}
		balise.put(CLE_LIEUX, listeLieux);

		int[] heures = new int[this.heuresDeRepas.size()];
		for (int i = 0; i < heures.length; i++) {
			heures[i] = this.heuresDeRepas.get(i);
		}
		balise.putIntArray(CLE_HEURES_REPAS, heures);
		// Une seule chaine plutot qu'une liste de balises : ce sont quelques
		// identifiants courts, et la sauvegarde reste lisible a l'oeil.
		balise.putString(CLE_COMPETENCES, String.join(",", this.competences));
		balise.putString(CLE_BOBO, this.bobo);
		balise.putLong(CLE_BOBO_DEPUIS, this.boboDepuis);

		return balise;
	}

	// --- Identite ---------------------------------------------------------------

	public UUID id() {
		return this.id;
	}

	public UUID proprietaire() {
		return this.proprietaire;
	}

	public String espece() {
		return this.espece;
	}

	public void setEspece(String espece) {
		this.espece = espece;
	}

	public String variante() {
		return this.variante;
	}

	public void setVariante(String variante) {
		this.variante = variante;
	}

	public String nom() {
		return this.nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	// --- Position ---------------------------------------------------------------

	public ResourceKey<Level> dimension() {
		return this.dimension;
	}

	public double x() {
		return this.x;
	}

	public double y() {
		return this.y;
	}

	public double z() {
		return this.z;
	}

	public float rotation() {
		return this.rotation;
	}

	/** Recopie la position de l'entite dans la fiche. La fiche reste la verite. */
	public void memoriser(Entity entite) {
		this.dimension = entite.level().dimension();
		this.x = entite.getX();
		this.y = entite.getY();
		this.z = entite.getZ();
		this.rotation = entite.getYRot();
	}

	public boolean loinDe(double autreX, double autreY, double autreZ, double distance) {
		double dx = this.x - autreX;
		double dy = this.y - autreY;
		double dz = this.z - autreZ;
		return dx * dx + dy * dy + dz * dz > distance * distance;
	}

	// --- Mode -------------------------------------------------------------------

	public Mode mode() {
		return this.mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	/** Vrai s'il a le droit d'apparaitre dans le monde. */
	public boolean sorti() {
		return this.sorti;
	}

	/**
	 * Le range, ou l'autorise a revenir.
	 *
	 * <p>Ne deplace rien et ne fait rien disparaitre : c'est {@code Apparition}
	 * qui, au tick suivant, retire l'entite d'un compagnon range. Cette fiche
	 * ne connait pas le monde et ne doit pas y toucher.
	 */
	public void setSorti(boolean sorti) {
		this.sorti = sorti;
	}

	/**
	 * Le pose ailleurs, sans passer par une entite.
	 *
	 * <p>C'est ce qu'on fait pour l'invoquer : la fiche fait autorite sur la
	 * position, donc la deplacer suffit — l'entite naitra la, ou s'y rendra.
	 */
	public void poserA(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
			double x, double y, double z, float rotation) {
		this.dimension = dimension;
		this.x = x;
		this.y = y;
		this.z = z;
		this.rotation = rotation;
	}

	// --- Les cinq barres --------------------------------------------------------

	public float barre(Barre barre) {
		return this.barres.getOrDefault(barre, Barre.MAXIMUM);
	}

	/** Pose une valeur, bornee par le reglage de la barre. */
	public void setBarre(Barre barre, float valeur, Progression table) {
		Progression.ReglageBarre reglage = table.barres().get(barre);
		float borne = Math.max(reglage.minimum(), Math.min(reglage.maximum(), valeur));
		this.barres.put(barre, borne);
	}

	/** Ajoute (ou retire) sans jamais sortir des bornes de la barre. */
	public void ajouterBarre(Barre barre, float delta, Progression table) {
		setBarre(barre, barre(barre) + delta, table);
	}

	/** L'humeur, calculee. Elle n'est jamais stockee. */
	public Humeur humeur() {
		return Humeur.calculer(this);
	}

	/** L'identifiant de son caractere. */
	public String caractere() {
		return this.caractere;
	}

	/** Combien de fois cette personne s'est occupee de lui. */
	public int connaissance(UUID qui) {
		return this.connaissances.getOrDefault(qui, 0);
	}

	/** Il retient un geste de plus de cette personne. */
	/**
	 * Combien de personnes et de betes il peut retenir.
	 *
	 * <h2>Pourquoi une limite</h2>
	 *
	 * <p>Cette table grandissait <b>sans fin</b> : une entree par joueur qui le
	 * caresse et par compagnon qu'il croise, gardee pour toujours. Dans une salle
	 * commune a mille joueurs, un compagnon populaire en accumulerait des
	 * centaines — et elles seraient toutes ecrites dans la sauvegarde, a chaque
	 * sauvegarde, pour mille compagnons.
	 *
	 * <p>Soixante-quatre est deja bien plus qu'une bete ne peut vraiment connaitre.
	 * Au-dela, on oublie le moins familier : c'est ce que ferait un animal, et ca
	 * borne le fichier une fois pour toutes.
	 */
	private static final int CONNAISSANCES_MAXIMUM = 64;

	/**
	 * Combien de mots un compagnon peut apprendre.
	 *
	 * <p>Huit, et c'est deja beaucoup. Ce n'est pas une limite de place — huit
	 * courtes chaines ne pesent rien — c'est une limite de <b>sens</b> : chaque
	 * mot appris entre dans la liste que le moteur guette, et plus cette liste
	 * est longue, moins il entend bien chacun d'eux. Un compagnon qui connait
	 * quarante mots les confondrait tous.
	 *
	 * <p>Et une bete qui sait huit tours, c'est une bete bien dressee.
	 */
	public static final int MOTS_APPRIS_MAXIMUM = 8;

	/**
	 * Combien d'endroits il retient.
	 *
	 * <p>Quatre. Une bete ne se souvient pas de tout, et c'est justement ce qui
	 * donne du poids aux quatre : si tout comptait, rien ne compterait.
	 *
	 * <p>Quatre entrees de vingt octets, c'est aussi ce qui garantit que la
	 * sauvegarde reste bornee a mille compagnons.
	 */
	public static final int LIEUX_MAXIMUM = 4;

	/**
	 * Combien de repas on retient pour deviner son heure.
	 *
	 * <p>Quatre. Assez pour qu'une habitude se voie, assez peu pour qu'elle
	 * change quand le joueur change de rythme. Quatre entiers par fiche.
	 */
	public static final int REPAS_RETENUS = 4;

	/**
	 * De combien d'heures les repas peuvent s'ecarter et rester une habitude.
	 *
	 * <p>Deux heures de jeu, soit une minute et quarante secondes reelles. On
	 * ne mange pas a la minute pres, et exiger la minute pres ferait qu'aucune
	 * habitude n'existerait jamais.
	 */
	private static final int ECART_TOLERE = 2;

	/** En dessous, ce n'est pas encore une habitude, juste des repas. */
	private static final int REPAS_POUR_UNE_HABITUDE = 3;

	/**
	 * Il retient un geste de plus de cette personne.
	 *
	 * <p>Quand sa memoire est pleine, il oublie celui qu'il connaissait le moins.
	 * Un visage croise une fois s'efface ; un ami de tous les jours reste.
	 */
	/** Les mots qu'il connait, et le geste de chacun. Jamais modifiable dehors. */
	/**
	 * Un endroit qui compte pour lui.
	 *
	 * @param cle   ce qui s'y est passe, pour ne pas retenir deux fois la meme chose
	 * @param quand la date reelle, pour que le livre puisse la dire un jour
	 */
	public record Lieu(String cle, int x, int y, int z, long quand) {

		/** Vrai si ce point est a portee de ce qu'on regarde. */
		public boolean proche(double ox, double oy, double oz, double distance) {
			double dx = this.x + 0.5D - ox;
			double dy = this.y + 0.5D - oy;
			double dz = this.z + 0.5D - oz;
			return dx * dx + dy * dy + dz * dz <= distance * distance;
		}
	}

	/**
	 * On vient de le nourrir a cette heure du monde.
	 *
	 * @param heure de 0 a 23, l'heure du monde et non l'heure reelle
	 */
	public void noterUnRepas(int heure) {
		if (heure < 0 || heure > 23) {
			return;
		}
		if (this.heuresDeRepas.size() >= REPAS_RETENUS) {
			this.heuresDeRepas.remove(0);
		}
		this.heuresDeRepas.add(heure);
	}

	/**
	 * L'heure a laquelle on s'occupe de lui d'habitude, ou -1.
	 *
	 * <h2>Une habitude, ou rien</h2>
	 *
	 * <p>On ne rend une heure que si au moins trois des quatre derniers repas
	 * tombent a deux heures pres les uns des autres. Sinon on rend -1, et le
	 * compagnon n'attend nulle part.
	 *
	 * <p>C'est le point qui fait toute la difference : une moyenne rendrait
	 * toujours un chiffre, meme pour quelqu'un qui nourrit sa bete n'importe
	 * quand. Elle irait alors attendre a une heure qui ne veut rien dire, et
	 * l'illusion tomberait tout de suite.
	 *
	 * <p>L'heure est cyclique : 23 h et 1 h sont voisines. On compare donc les
	 * ecarts dans les deux sens du cadran.
	 */
	public int heureHabituelle() {
		if (this.heuresDeRepas.size() < REPAS_POUR_UNE_HABITUDE) {
			return -1;
		}
		for (int candidate : this.heuresDeRepas) {
			int proches = 0;
			for (int autre : this.heuresDeRepas) {
				if (ecartDHeures(candidate, autre) <= ECART_TOLERE) {
					proches++;
				}
			}
			if (proches >= REPAS_POUR_UNE_HABITUDE) {
				return candidate;
			}
		}
		return -1;
	}

	/** L'ecart entre deux heures, en tenant compte du tour du cadran. */
	public static int ecartDHeures(int a, int b) {
		int brut = Math.abs(a - b);
		return Math.min(brut, 24 - brut);
	}

	/** Ce qu'il a appris a etre. */
	public java.util.Set<String> competences() {
		return java.util.Collections.unmodifiableSet(this.competences);
	}

	public boolean a(String competence) {
		return this.competences.contains(competence);
	}

	/** Il apprend. Vrai si c'est nouveau. */
	public boolean apprendreLaCompetence(String competence) {
		return this.competences.add(competence);
	}

	/**
	 * Combien de points il lui reste a depenser.
	 *
	 * <h2>Pourquoi un point tous les cinq niveaux</h2>
	 *
	 * <p>Un point par niveau donnerait cinquante points au bout du compte —
	 * plus qu'il n'existe de competences. Chacun finirait par tout prendre, et
	 * deux betes du meme niveau seraient de nouveau identiques.
	 *
	 * <p>C'est la <b>rarete des points</b> qui fait le choix, et le choix qui
	 * fait la difference entre deux compagnons.
	 *
	 * <p>Les competences dont le fichier a disparu ne comptent pas dans la
	 * depense : sinon retirer un fichier volerait un point a tout le monde.
	 */
	public int pointsRestants(Progression table, int parNiveaux) {
		int gagnes = parNiveaux <= 0 ? 0 : niveau(table) / parNiveaux;
		int depenses = fr.lhdp.compagnon.competence.Competences
				.nettoyer(this.competences).size();
		return Math.max(0, gagnes - depenses);
	}

	/** Les endroits qu'il retient, du plus ancien au plus recent. */
	public List<Lieu> lieux() {
		return java.util.Collections.unmodifiableList(this.lieux);
	}

	/**
	 * Retient cet endroit-ci.
	 *
	 * <p>Une seule fois par sorte : la deuxieme fois qu'on le soigne quelque
	 * part, c'est le nouvel endroit qui compte, pas les deux. Sinon les quatre
	 * places seraient prises par le meme evenement repete.
	 *
	 * @return vrai si quelque chose a change
	 */
	public boolean retenirLeLieu(String cle, int x, int y, int z, long quand) {
		Lieu nouveau = new Lieu(cle, x, y, z, quand);
		for (int i = 0; i < this.lieux.size(); i++) {
			if (this.lieux.get(i).cle().equals(cle)) {
				if (this.lieux.get(i).proche(x + 0.5D, y + 0.5D, z + 0.5D, 8.0D)) {
					// Le meme endroit, a huit blocs pres : rien de nouveau.
					return false;
				}
				this.lieux.set(i, nouveau);
				return true;
			}
		}
		if (this.lieux.size() >= LIEUX_MAXIMUM) {
			this.lieux.remove(0);
		}
		this.lieux.add(nouveau);
		return true;
	}

	public Map<String, String> motsAppris() {
		return java.util.Collections.unmodifiableMap(this.motsAppris);
	}

	/** Le geste associe a ce mot, ou {@code null} s'il ne le connait pas. */
	public String gesteDuMot(String mot) {
		return mot == null ? null : this.motsAppris.get(mot);
	}

	/**
	 * Lui apprend un mot.
	 *
	 * <p>Quand il en sait deja huit, le plus ancien s'efface — il apprend, mais
	 * il oublie aussi. Refuser aurait oblige a effacer d'abord, et un joueur qui
	 * ne sait plus lequel effacer reste coince.
	 *
	 * @return le mot oublie pour faire de la place, ou {@code null}
	 */
	public String apprendre(String mot, String geste) {
		String oublie = null;
		if (!this.motsAppris.containsKey(mot)
				&& this.motsAppris.size() >= MOTS_APPRIS_MAXIMUM) {
			java.util.Iterator<String> plusAncien = this.motsAppris.keySet().iterator();
			oublie = plusAncien.next();
			plusAncien.remove();
		}
		this.motsAppris.put(mot, geste);
		return oublie;
	}

	/** Il oublie ce mot. Vrai s'il le connaissait. */
	public boolean oublierLeMot(String mot) {
		return this.motsAppris.remove(mot) != null;
	}

	public void seSouvenirDe(UUID qui) {
		if (!this.connaissances.containsKey(qui)
				&& this.connaissances.size() >= CONNAISSANCES_MAXIMUM) {
			oublierLeMoinsFamilier();
		}
		this.connaissances.merge(qui, 1, Integer::sum);
	}

	private void oublierLeMoinsFamilier() {
		UUID leMoins = null;
		int fois = Integer.MAX_VALUE;

		for (Map.Entry<UUID, Integer> connu : this.connaissances.entrySet()) {
			if (connu.getValue() < fois) {
				fois = connu.getValue();
				leMoins = connu.getKey();
			}
		}
		if (leMoins != null) {
			this.connaissances.remove(leMoins);
		}
	}

	public Map<UUID, Integer> connaissances() {
		return Map.copyOf(this.connaissances);
	}

	// --- Le bobo en cours -------------------------------------------------------

	/** L'identifiant du bobo, ou une chaine vide s'il va bien. */
	public String bobo() {
		return this.bobo;
	}

	public boolean aUnBobo() {
		return !this.bobo.isEmpty();
	}

	public long boboDepuis() {
		return this.boboDepuis;
	}

	public void setBobo(String identifiant, long date) {
		this.bobo = identifiant;
		this.boboDepuis = date;
	}

	public void guerir() {
		this.bobo = "";
		this.boboDepuis = 0L;
	}

	// --- Experience et niveaux --------------------------------------------------

	public int xp() {
		return this.xp;
	}

	/**
	 * Pose directement l'experience. Sert a la commande d'equipe qui met un
	 * compagnon a un niveau donne, pour essayer un deblocage sans attendre.
	 */
	public void setXp(int valeur) {
		this.xp = Math.max(0, valeur);
	}

	public int niveau(Progression table) {
		return table.niveauPour(this.xp);
	}

	public Set<String> deblocages(Progression table) {
		return table.debloquesJusqua(niveau(table));
	}

	/**
	 * Fait gagner de l'experience depuis une source, en respectant son plafond du
	 * jour. Renvoie ce qui a reellement ete accorde — zero si le plafond est
	 * atteint.
	 *
	 * <p>C'est ici que se joue la regularite : nourrir dix fois d'affilee ne
	 * rapporte pas dix fois, revenir demain oui.
	 */
	public int gagnerXp(SourceXp source, Progression table, long maintenant) {
		majJour(maintenant);

		Progression.Source reglage = table.sources().get(source);
		if (reglage == null) {
			return 0;
		}

		int dejaGagne = this.xpDuJour.getOrDefault(source, 0);
		int reste = reglage.plafondParJour() - dejaGagne;
		if (reste <= 0) {
			return 0;
		}

		// Ses competences peuvent le faire apprendre plus vite. Le plafond
		// journalier, lui, ne bouge pas : il est la pour que le niveau se gagne
		// sur la duree, et une competence ne doit pas court-circuiter ca.
		int avecCompetences = Math.round(reglage.xp()
				* fr.lhdp.compagnon.competence.Competences.facteur(
						this.competences, fr.lhdp.compagnon.competence.Competence.EXPERIENCE));
		int gagne = Math.min(Math.max(1, avecCompetences), reste);
		this.xp += gagne;
		this.xpDuJour.put(source, dejaGagne + gagne);
		return gagne;
	}

	/** Ce qu'il reste a gagner aujourd'hui pour cette source. */
	public int resteAujourdhui(SourceXp source, Progression table, long maintenant) {
		majJour(maintenant);
		Progression.Source reglage = table.sources().get(source);
		if (reglage == null) {
			return 0;
		}
		return Math.max(0, reglage.plafondParJour() - this.xpDuJour.getOrDefault(source, 0));
	}

	/** A-t-on deja partage cette sorte d'attention pendant la journee reelle ? */
	public boolean aGagneAujourdhui(SourceXp source, Progression table, long maintenant) {
		majJour(maintenant);
		return table.sources().containsKey(source)
				&& this.xpDuJour.getOrDefault(source, 0) > 0;
	}

	/** Remet les compteurs du jour a zero quand on change de jour reel. */
	private void majJour(long maintenant) {
		long jourCourant = maintenant / MILLIS_PAR_JOUR;
		if (jourCourant != this.jour) {
			this.jour = jourCourant;
			this.xpDuJour.clear();
		}
	}

	// --- Historique -------------------------------------------------------------

	public long dateObtention() {
		return this.dateObtention;
	}

	/** Quand on l'a vu dehors pour la derniere fois. Zero = jamais. */
	public long vuLe() {
		return this.vuLe;
	}

	/** Il est dehors avec son maitre en ce moment. */
	public void noterUneVisite(long quand) {
		this.vuLe = quand;
	}

	public long ticksEnsemble() {
		return this.ticksEnsemble;
	}

	public void ajouterTemps(long ticks) {
		this.ticksEnsemble += ticks;
	}

	public List<Moment> moments() {
		return List.copyOf(this.moments);
	}

	/**
	 * Enregistre un moment marquant, une seule fois. Un premier vol n'est premier
	 * qu'une fois.
	 */
	public boolean marquer(String cle, long date) {
		for (Moment moment : this.moments) {
			if (moment.cle().equals(cle)) {
				return false;
			}
		}
		// La liste ne grandit pas sans fin : voir Moment.faireDeLaPlace, qui dit
		// aussi ce qu'on accepte d'oublier et ce qu'on ne jette jamais.
		if (this.moments.size() >= Moment.PLAFOND) {
			Moment.faireDeLaPlace(this.moments);
		}
		this.moments.add(new Moment(cle, date));
		return true;
	}


	public int compteur(String cle) {
		return this.compteurs.getOrDefault(cle, 0);
	}

	public void incrementer(String cle) {
		this.compteurs.merge(cle, 1, Integer::sum);
	}

	/**
	 * Pose la valeur exacte d un compteur.
	 *
	 * <p>Poser zero <b>efface</b> le compteur au lieu de ranger un zero. La
	 * carte est sauvegardee telle quelle : garder des zeros ferait grossir le
	 * fichier de tout le monde avec des lignes qui ne disent rien.
	 *
	 * <p>Et elle ne grandit pas sans fin : au-dela du plafond, on refuse un
	 * compteur nouveau plutot que d en oublier un ancien — un compteur oublie
	 * ferait repartir une mission a zero, ce qui est bien pire.
	 */
	public void poserCompteur(String cle, int valeur) {
		if (valeur == 0) {
			this.compteurs.remove(cle);
			return;
		}
		if (this.compteurs.size() >= PLAFOND_DES_COMPTEURS && !this.compteurs.containsKey(cle)) {
			return;
		}
		this.compteurs.put(cle, valeur);
	}

	public Map<String, Integer> compteurs() {
		return Map.copyOf(this.compteurs);
	}
}
