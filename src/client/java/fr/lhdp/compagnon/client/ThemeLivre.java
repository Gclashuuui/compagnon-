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
import java.util.Locale;
import java.util.Properties;

/**
 * Une apparence complete du journal, avec sa palette lisible.
 *
 * <p>Changer uniquement l'image rendrait le texte brun presque invisible sur
 * Obsidienne. Le fond, les boutons, les signets et les encres changent donc
 * ensemble. Le choix est purement client et reste dans le dossier config : il
 * ne cree aucun paquet reseau et ne coute rien au serveur.
 */
enum ThemeLivre {

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
	private static final Path FICHIER = FabricLoader.getInstance().getConfigDir()
			.resolve("compagnon-client.properties");

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

	static ThemeLivre charger() {
		if (!Files.isRegularFile(FICHIER)) {
			return CLASSIQUE;
		}
		Properties proprietes = new Properties();
		try (Reader lecteur = Files.newBufferedReader(FICHIER, StandardCharsets.UTF_8)) {
			proprietes.load(lecteur);
			String id = proprietes.getProperty(CLE, CLASSIQUE.id)
					.trim().toLowerCase(Locale.ROOT);
			for (ThemeLivre theme : values()) {
				if (theme.id.equals(id)) {
					return theme;
				}
			}
		} catch (IOException exception) {
			Compagnon.LOG.warn("Impossible de lire le theme du livre", exception);
		}
		return CLASSIQUE;
	}

	void sauvegarder() {
		Properties proprietes = new Properties();
		if (Files.isRegularFile(FICHIER)) {
			try (Reader lecteur = Files.newBufferedReader(FICHIER, StandardCharsets.UTF_8)) {
				proprietes.load(lecteur);
			} catch (IOException exception) {
				Compagnon.LOG.warn("Impossible de relire les reglages client", exception);
			}
		}
		proprietes.setProperty(CLE, this.id);
		try {
			Files.createDirectories(FICHIER.getParent());
			try (Writer ecrivain = Files.newBufferedWriter(FICHIER, StandardCharsets.UTF_8)) {
				proprietes.store(ecrivain, "Reglages client du mod Compagnon");
			}
		} catch (IOException exception) {
			Compagnon.LOG.warn("Impossible de sauvegarder le theme du livre", exception);
		}
	}
}
