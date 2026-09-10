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
 * L'apparence du carnet de sante compact.
 *
 * <p>Le fond et les encres voyagent ensemble : une texture sombre avec l'encre
 * brune du parchemin serait illisible. Les premieres variantes sont peintes par
 * le code afin que le selecteur fonctionne deja ; les futures planches PNG
 * pourront remplacer leur fond sans toucher aux jauges ni aux donnees.
 */
enum ThemeSante {

	PARCHEMIN("parchemin", "panneau.compagnon.theme.parchemin",
			0xFFF3E4C4, 0xFFE7D3AC, 0xFF5A4632, 0xFF3A2A18, 0xFF7A6A55,
			0xFFB9A886, 0xFFD9CBAE, 0xFF8A6339),
	SYLVESTRE("sylvestre", "panneau.compagnon.theme.sylvestre",
			0xFFDDE8CF, 0xFFC2D2AC, 0xFF3F5840, 0xFF243424, 0xFF577058,
			0xFF8DA47C, 0xFFC4D4B5, 0xFF527953),
	NOCTURNE("nocturne", "panneau.compagnon.theme.nocturne",
			0xFF29313F, 0xFF171C27, 0xFF8EA0BD, 0xFFF1F3F8, 0xFFB4C0D2,
			0xFF0D1119, 0xFF414C5D, 0xFF7E9BC5),
	DRAGON_RUBIS("dragon-rubis", "panneau.compagnon.theme.dragon_rubis",
			0xFFF4E4C4, 0xFFE8CFA5, 0xFF3A120D, 0xFF392419, 0xFF73503D,
			0xFF301915, 0xFF3A120D, 0xFFCF4734, true),
	DRAGON_GIVRE("dragon-givre", "panneau.compagnon.theme.dragon_givre",
			0xFFF3F1E8, 0xFFDDE7E9, 0xFF11243D, 0xFF1A3049, 0xFF455D72,
			0xFF173249, 0xFF11243D, 0xFF70CFFA, true),
	DRAGON_JADE("dragon-jade", "panneau.compagnon.theme.dragon_jade",
			0xFFE7E0C9, 0xFFD2C8A8, 0xFF102D22, 0xFF21392A, 0xFF4A6249,
			0xFF163A2C, 0xFF102D22, 0xFF42B37B, true),
	DRAGON_AMETHYSTE("dragon-amethyste", "panneau.compagnon.theme.dragon_amethyste",
			0xFF24182F, 0xFF171126, 0xFF171126, 0xFFFFF3FF, 0xFFD9CBEB,
			0xFF17102C, 0xFF2B1D43, 0xFFA776E8, true),
	DRAGON_ROSE("dragon-rose", "panneau.compagnon.theme.dragon_rose",
			0xFFF5E6E8, 0xFFEBCFD6, 0xFF733D50, 0xFF512C3A, 0xFF805761,
			0xFF493445, 0xFF733D50, 0xFFEC88AF, true),
	PHENIX("phenix", "panneau.compagnon.theme.phenix",
			0xFFF4E1BD, 0xFFEBC890, 0xFF241E34, 0xFF3E291C, 0xFF765735,
			0xFF30283D, 0xFF241E34, 0xFFF2A32F, true),
	RENARD_LUNAIRE("renard-lunaire", "panneau.compagnon.theme.renard_lunaire",
			0xFFF3F0E9, 0xFFE0E0E7, 0xFF17263F, 0xFF29344C, 0xFF58657F,
			0xFF202C49, 0xFF17263F, 0xFFADB5FA, true),
	GLYCINE("glycine", "panneau.compagnon.theme.glycine",
			0xFFF3E9EE, 0xFFE4D4E3, 0xFF3D244F, 0xFF44334F, 0xFF705977,
			0xFF40314A, 0xFF3D244F, 0xFFB688E0, true),
	GALAXIE("galaxie", "panneau.compagnon.theme.galaxie",
			0xFF18112D, 0xFF100C27, 0xFF100C27, 0xFFF7EEFF, 0xFFCBC0E9,
			0xFF161127, 0xFF2A1D49, 0xFFC184FF, true),
	CHAMPIGNONS("champignons", "panneau.compagnon.theme.champignons",
			0xFFF0E4CA, 0xFFE1D2B5, 0xFF31281C, 0xFF3C3322, 0xFF706147,
			0xFF322F21, 0xFF31281C, 0xFF92AC52, true);

	private static final String CLE = "theme_sante";
	private static final Path FICHIER = FabricLoader.getInstance().getConfigDir()
			.resolve("compagnon-client.properties");

	private final String id;
	private final String traduction;
	final int fondHaut;
	final int fondBas;
	final int cadre;
	final int encre;
	final int encrePale;
	final int creuxHaut;
	final int creuxBas;
	final int accent;
	private final boolean illustre;

	ThemeSante(String id, String traduction, int fondHaut, int fondBas, int cadre,
			int encre, int encrePale, int creuxHaut, int creuxBas, int accent) {
		this(id, traduction, fondHaut, fondBas, cadre, encre, encrePale,
				creuxHaut, creuxBas, accent, false);
	}

	ThemeSante(String id, String traduction, int fondHaut, int fondBas, int cadre,
			int encre, int encrePale, int creuxHaut, int creuxBas, int accent,
			boolean illustre) {
		this.id = id;
		this.traduction = traduction;
		this.fondHaut = fondHaut;
		this.fondBas = fondBas;
		this.cadre = cadre;
		this.encre = encre;
		this.encrePale = encrePale;
		this.creuxHaut = creuxHaut;
		this.creuxBas = creuxBas;
		this.accent = accent;
		this.illustre = illustre;
	}

	String traduction() {
		return this.traduction;
	}

	boolean illustre() {
		return this.illustre;
	}

	ResourceLocation texture(String nom) {
		return Compagnon.id("textures/gui/sante/themes/" + this.id + "/" + nom + ".png");
	}

	ThemeSante suivant() {
		ThemeSante[] tous = values();
		return tous[(this.ordinal() + 1) % tous.length];
	}

	ThemeSante precedent() {
		ThemeSante[] tous = values();
		return tous[(this.ordinal() - 1 + tous.length) % tous.length];
	}

	static ThemeSante charger() {
		if (!Files.isRegularFile(FICHIER)) {
			return PARCHEMIN;
		}
		Properties proprietes = new Properties();
		try (Reader lecteur = Files.newBufferedReader(FICHIER, StandardCharsets.UTF_8)) {
			proprietes.load(lecteur);
			String voulu = proprietes.getProperty(CLE, PARCHEMIN.id)
					.trim().toLowerCase(Locale.ROOT);
			for (ThemeSante theme : values()) {
				if (theme.id.equals(voulu)) {
					return theme;
				}
			}
		} catch (IOException exception) {
			Compagnon.LOG.warn("Impossible de lire le theme du carnet de sante", exception);
		}
		return PARCHEMIN;
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
			Compagnon.LOG.warn("Impossible de sauvegarder le theme du carnet de sante", exception);
		}
	}
}
