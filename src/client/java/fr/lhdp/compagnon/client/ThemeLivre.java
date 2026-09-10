package fr.lhdp.compagnon.client;

import fr.lhdp.compagnon.Compagnon;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/**
 * Une apparence complete du journal, avec sa palette lisible.
 *
 * <p>Changer uniquement l'image rendrait le texte brun presque invisible sur
 * Obsidienne. Le fond, les boutons, les signets et les encres changent donc
 * ensemble. Le choix est purement client et reste dans le dossier config : il
 * ne cree aucun paquet reseau et ne coute rien au serveur.
 */
enum ThemeLivre {

	SYLVESTRE_HD("sylvestre_hd", "livre.compagnon.theme.sylvestre_hd",
			0xFF3A2A18, 0xFF715238, 0xFF9B3333, 0x55715238, 0xFF436044,
			0xFF45362B, 0xFFBEA887, 0xFFE8D5B3, 0xFFF5E7C8,
			0xCC45362B, 0xEE436044, 0xFFD2B681, 0xFFFFF1D4),
	OBSERVATOIRE_HD("observatoire_hd", "livre.compagnon.theme.observatoire_hd",
			0xFF352C26, 0xFF6C533C, 0xFF9B3333, 0x556C533C, 0xFF3A566C,
			0xFF45362B, 0xFFBEA887, 0xFFE8D5B3, 0xFFF5E7C8,
			0xCC45362B, 0xEE3A566C, 0xFFD2B681, 0xFFFFF1D4),
	NUAGES_HD("nuages_hd", "livre.compagnon.theme.nuages_hd",
			0xFF364A59, 0xFF735540, 0xFF9B3333, 0x55735540, 0xFF426173,
			0xFF45362B, 0xFFBEA887, 0xFFE8D5B3, 0xFFF5E7C8,
			0xCC45362B, 0xEE426173, 0xFFD2B681, 0xFFFFF1D4),
	LUCIOLES_HD("lucioles_hd", "livre.compagnon.theme.lucioles_hd",
			0xFF422824, 0xFF77503B, 0xFF9B3333, 0x5577503B, 0xFF76522B,
			0xFF45362B, 0xFFBEA887, 0xFFE8D5B3, 0xFFF5E7C8,
			0xCC45362B, 0xEE76522B, 0xFFD2B681, 0xFFFFF1D4),
	MAREES_HD("marees_hd", "livre.compagnon.theme.marees_hd",
			0xFF2A494C, 0xFF576653, 0xFF9B3333, 0x55576653, 0xFF386D70,
			0xFF45362B, 0xFFBEA887, 0xFFE8D5B3, 0xFFF5E7C8,
			0xCC45362B, 0xEE386D70, 0xFFD2B681, 0xFFFFF1D4),
	CONFISERIE_HD("confiserie_hd", "livre.compagnon.theme.confiserie_hd",
			0xFF542F34, 0xFF785244, 0xFF9B3333, 0x55785244, 0xFF854054,
			0xFF45362B, 0xFFBEA887, 0xFFE8D5B3, 0xFFF5E7C8,
			0xCC45362B, 0xEE854054, 0xFFD2B681, 0xFFFFF1D4),
	MEDIEVAL_HD("medieval_hd", "livre.compagnon.theme.medieval_hd",
			0xFF503122, 0xFF79583C, 0xFF993E35, 0x5579583C, 0xFF687445,
			0xFF482B1E, 0xFFB28D59, 0xFFE1C394, 0xFFF2D9AF,
			0xCC482B1E, 0xEE7B3D31, 0xFFD1A75C, 0xFFFFE8BE),
	BESTIAIRE_HD("bestiaire_hd", "livre.compagnon.theme.bestiaire_hd",
			0xFF173C3F, 0xFF5D665A, 0xFF9B3E3E, 0x555D665A, 0xFF1E676B,
			0xFF123D42, 0xFF9E8B68, 0xFFE5D5B5, 0xFFF8EACD,
			0xCC123D42, 0xEE1E676B, 0xFFCDAA60, 0xFFFFF1D0),
	VITRAIL_HD("vitrail_hd", "livre.compagnon.theme.vitrail_hd",
			0xFF4A2D47, 0xFF79536F, 0xFFA04152, 0x5579536F, 0xFF2E7778,
			0xFF4B284B, 0xFFC09A6A, 0xFFF0D2DD, 0xFFFFEEF3,
			0xCC4B284B, 0xEE317D7E, 0xFFD5A15F, 0xFFFFF0DA),
	HORLOGERIE_HD("horlogerie_hd", "livre.compagnon.theme.horlogerie_hd",
			0xFF24463E, 0xFF66735E, 0xFF963D35, 0x5566735E, 0xFF466C55,
			0xFF263D36, 0xFFB38C50, 0xFFE7CC96, 0xFFF8E3B7,
			0xCC263D36, 0xEE496A50, 0xFFD2AA61, 0xFFFFEDC4),
	PORCELAINE_HD("porcelaine_hd", "livre.compagnon.theme.porcelaine_hd",
			0xFF264E82, 0xFF6B7180, 0xFFA04855, 0x556B7180, 0xFF50765D,
			0xFF8D774E, 0xFFC6B37F, 0xFFF0E5CC, 0xFFFFF8E8,
			0xCC8D774E, 0xEEE7D8BA, 0xFFD0AE61, 0xFF284F82),
	CLASSIQUE("classique", "livre.compagnon.theme.classique",
			0xFF3A2A18, 0xFF826137, 0xFF8A2F2F, 0x55826137, 0xFF456D59,
			0xFF39291F, 0xFFB49A70, 0xFFE3CCA3, 0xFFF2DFB8,
			0xCC39291F, 0xEE456D59, 0xFFA47548, 0xFFF6E6C8),
	OCEAN("ocean", "livre.compagnon.theme.ocean",
			0xFF183D55, 0xFF376F91, 0xFF8A2F2F, 0x55376F91, 0xFF347596,
			0xFF122F46, 0xFF81AABD, 0xFFD9EBF2, 0xFFEDF8FC,
			0xCC122F46, 0xEE2D6A8C, 0xFFA7D9ED, 0xFFF4FCFF),
	OBSIDIENNE("obsidienne", "livre.compagnon.theme.obsidienne",
			0xFFF2F4F7, 0xFFAEB8C5, 0xFFFF8B8B, 0x55AEB8C5, 0xFFDEE5EF,
			0xFF0E1014, 0xFF1C2027, 0xFF454D59, 0xFF30343B,
			0xCC0E1014, 0xEE414B59, 0xFF7D8591, 0xFFF6F8FC),
	AURORE_BOREALE("aurore_boreale", "livre.compagnon.theme.aurore_boreale",
			0xFF3F304C, 0xFF775B86, 0xFFA33F58, 0x55775B86, 0xFF3F6E66,
			0xFF292135, 0xFFA18CAF, 0xFFE2D7EA, 0xFFF2EBF7,
			0xCC292135, 0xEE3F6E66, 0xFFBFA1C8, 0xFFE8FCF2),
	CIEL("ciel", "livre.compagnon.theme.ciel",
			0xFF264D67, 0xFF4B718E, 0xFF8A2F2F, 0x554B718E, 0xFF416983,
			0xFF7CAAC9, 0xFFA8CDDF, 0xFFE8F5FB, 0xFFF8FCFF,
			0xCC7CAAC9, 0xEEDDF2FC, 0xFFFFFFFF, 0xFF315D79),
	NUAGE("nuage", "livre.compagnon.theme.nuage",
			0xFF354657, 0xFF586B7C, 0xFF8A2F2F, 0x55586B7C, 0xFF566D85,
			0xFF9CAAB5, 0xFFBAC6D0, 0xFFECF1F5, 0xFFFFFFFF,
			0xCC9CAAB5, 0xEEEDF4F9, 0xFFFFFFFF, 0xFF3E566F),
	PRINCESSE("princesse", "livre.compagnon.theme.princesse",
			0xFF653347, 0xFF975374, 0xFFA33F58, 0x55975374, 0xFF994468,
			0xFF593348, 0xFFC99AB2, 0xFFF6DDE9, 0xFFFFF1F7,
			0xCC593348, 0xEEF0C1DB, 0xFFFFF0F7, 0xFF73304F),
	VOLCAN("volcan", "livre.compagnon.theme.volcan",
			0xFFFFF0DF, 0xFFE5B393, 0xFFFF8B8B, 0x55E5B393, 0xFFFFB777,
			0xFF211416, 0xFF231719, 0xFF482C2A, 0xFF382324,
			0xCC211416, 0xEE883025, 0xFFF29A60, 0xFFFFE4BC),
	ARC_EN_CIEL("arc_en_ciel", "livre.compagnon.theme.arc_en_ciel",
			0xFF3E344F, 0xFF766080, 0xFFA33F58, 0x55766080, 0xFF6A4E82,
			0xFF42374F, 0xFFAD9CBF, 0xFFEEE5F3, 0xFFFFFAFF,
			0xCC42374F, 0xEE715986, 0xFFF4DFEF, 0xFFFFF8FF),
	CHAT("chat", "livre.compagnon.theme.chat",
			0xFF303035, 0xFF686872, 0xFF8A2F2F, 0x55686872, 0xFF5B5B68,
			0xFF19191D, 0xFFB7B7C0, 0xFFEEEEF3, 0xFFFAFAFD,
			0xCC19191D, 0xEEF4F4FA, 0xFFB6B6BF, 0xFF383841),
	GALAXIE("galaxie", "livre.compagnon.theme.galaxie",
			0xFFF1F1F6, 0xFFC2C2D1, 0xFFFF8B8B, 0x55C2C2D1, 0xFFD2D2E6,
			0xFF030305, 0xFF08080C, 0xFF191920, 0xFF101015,
			0xCC030305, 0xEE242430, 0xFF575762, 0xFFF5F5FF),
	POMME("pomme", "livre.compagnon.theme.pomme",
			0xFF304B22, 0xFF526C32, 0xFF8A2F2F, 0x55526C32, 0xFF4B702E,
			0xFF3C5E2B, 0xFF9CB87B, 0xFFE9F1D5, 0xFFF8FCEB,
			0xCC3C5E2B, 0xEEC8E696, 0xFFEFFBC8, 0xFF355721),
	ROUGE_NOIR("rouge_noir", "livre.compagnon.theme.rouge_noir",
			0xFFFFF1F2, 0xFFE5A3B0, 0xFFFF8B8B, 0x55E5A3B0, 0xFFF3B2C0,
			0xFF09090C, 0xFF121014, 0xFF2F2028, 0xFF21191F,
			0xCC09090C, 0xEE721B32, 0xFFE7465C, 0xFFFFE7EE),
	SOLEIL("soleil", "livre.compagnon.theme.soleil",
			0xFF594411, 0xFF80621D, 0xFF8A2F2F, 0x5580621D, 0xFF7D621B,
			0xFFA28334, 0xFFCCBA7A, 0xFFF6EDC9, 0xFFFFFBEA,
			0xCCA28334, 0xEEFBE39C, 0xFFFFF9D9, 0xFF685016),
	CERISIER_JAPON("cerisier_japon", "livre.compagnon.theme.cerisier_japon",
			0xFF573440, 0xFF8F5563, 0xFFA33F58, 0x558F5563, 0xFF8E475E,
			0xFF51313A, 0xFFC0A28C, 0xFFF3E3D3, 0xFFFFF5EA,
			0xCC51313A, 0xEE854B60, 0xFFF3C7D0, 0xFFFFF0F2),
	HALLOWEEN("halloween", "livre.compagnon.theme.halloween",
			0xFFFFF0DD, 0xFFEABB8E, 0xFFFF8B8B, 0x55EABB8E, 0xFFFFB768,
			0xFF100D12, 0xFF161118, 0xFF30232B, 0xFF231B24,
			0xCC100D12, 0xEE783C1B, 0xFFE5863F, 0xFFFFF0CE);

	private static final String CLE = "theme_livre";
	private static final String CLE_FAVORIS = "themes_livre_favoris";
	private static final Path FICHIER = FabricLoader.getInstance().getConfigDir()
			.resolve("compagnon-client.properties");
	private static final Set<String> FAVORIS = chargerFavoris();

	private final String id;
	private final String traduction;
	final int encre;
	final int encrePale;
	final int encreRouge;
	final int filet;
	final int encreVerte;
	final int cadre;
	final int creuxHaut;
	final int creuxBas;
	final int papier;
	final int ongletFond;
	final int ongletFondActif;
	final int ongletBord;
	final int ongletTexte;

	ThemeLivre(String id, String traduction, int encre, int encrePale,
			int encreRouge, int filet, int encreVerte, int cadre, int creuxHaut,
			int creuxBas, int papier, int ongletFond, int ongletFondActif,
			int ongletBord, int ongletTexte) {
		this.id = id;
		this.traduction = traduction;
		this.encre = encre;
		this.encrePale = encrePale;
		this.encreRouge = encreRouge;
		this.filet = filet;
		this.encreVerte = encreVerte;
		this.cadre = cadre;
		this.creuxHaut = creuxHaut;
		this.creuxBas = creuxBas;
		this.papier = papier;
		this.ongletFond = ongletFond;
		this.ongletFondActif = ongletFondActif;
		this.ongletBord = ongletBord;
		this.ongletTexte = ongletTexte;
	}

	boolean hauteDefinition() {
		return this.id.endsWith("_hd");
	}

	String traduction() {
		return this.traduction;
	}

	ResourceLocation texture(String nom) {
		return Compagnon.id("textures/gui/livre/themes/" + this.id + "/" + nom + ".png");
	}

	ThemeLivre suivant() {
		ThemeLivre[] tous = values();
		return tous[(this.ordinal() + 1) % tous.length];
	}

	ThemeLivre precedent() {
		ThemeLivre[] tous = values();
		return tous[(this.ordinal() - 1 + tous.length) % tous.length];
	}

	boolean favori() {
		return FAVORIS.contains(this.id);
	}

	void basculerFavori() {
		if (!FAVORIS.remove(this.id)) {
			FAVORIS.add(this.id);
		}
		sauvegarderFavoris();
	}

	/** Favoris d'abord, puis l'ordre stable du catalogue. */
	static List<ThemeLivre> catalogue(boolean seulementFavoris) {
		List<ThemeLivre> resultat = new ArrayList<>();
		for (ThemeLivre theme : values()) {
			if (!seulementFavoris || theme.favori()) {
				resultat.add(theme);
			}
		}
		resultat.sort(Comparator.comparing(ThemeLivre::favori).reversed()
				.thenComparingInt(Enum::ordinal));
		return List.copyOf(resultat);
	}

	static ThemeLivre charger() {
		if (!Files.isRegularFile(FICHIER)) {
			return SYLVESTRE_HD;
		}
		Properties proprietes = new Properties();
		try (Reader lecteur = Files.newBufferedReader(FICHIER, StandardCharsets.UTF_8)) {
			proprietes.load(lecteur);
			String id = proprietes.getProperty(CLE, SYLVESTRE_HD.id)
					.trim().toLowerCase(Locale.ROOT);
			for (ThemeLivre theme : values()) {
				if (theme.id.equals(id)) {
					return theme;
				}
			}
		} catch (IOException exception) {
			Compagnon.LOG.warn("Impossible de lire le theme du livre", exception);
		}
		return SYLVESTRE_HD;
	}

	void sauvegarder() {
		Properties proprietes = lireProprietes();
		proprietes.setProperty(CLE, this.id);
		ecrireProprietes(proprietes);
	}

	private static Set<String> chargerFavoris() {
		Set<String> resultat = new LinkedHashSet<>();
		String valeurs = lireProprietes().getProperty(CLE_FAVORIS, "");
		for (String valeur : valeurs.split(",")) {
			String id = valeur.trim().toLowerCase(Locale.ROOT);
			if (!id.isEmpty()) {
				resultat.add(id);
			}
		}
		return resultat;
	}

	private static void sauvegarderFavoris() {
		Properties proprietes = lireProprietes();
		proprietes.setProperty(CLE_FAVORIS, String.join(",", FAVORIS));
		ecrireProprietes(proprietes);
	}

	private static Properties lireProprietes() {
		Properties proprietes = new Properties();
		if (Files.isRegularFile(FICHIER)) {
			try (Reader lecteur = Files.newBufferedReader(FICHIER, StandardCharsets.UTF_8)) {
				proprietes.load(lecteur);
			} catch (IOException exception) {
				Compagnon.LOG.warn("Impossible de relire les reglages client", exception);
			}
		}
		return proprietes;
	}

	private static void ecrireProprietes(Properties proprietes) {
		try {
			Files.createDirectories(FICHIER.getParent());
			try (Writer ecrivain = Files.newBufferedWriter(FICHIER, StandardCharsets.UTF_8)) {
				proprietes.store(ecrivain, "Reglages client du mod Compagnon");
			}
		} catch (IOException exception) {
			Compagnon.LOG.warn("Impossible de sauvegarder les themes du livre", exception);
		}
	}
}
