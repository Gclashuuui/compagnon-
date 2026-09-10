package fr.lhdp.compagnon.client;

import fr.lhdp.compagnon.Compagnon;
import net.fabricmc.loader.api.FabricLoader;

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
			0xFF0D1119, 0xFF414C5D, 0xFF7E9BC5);

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

	ThemeSante(String id, String traduction, int fondHaut, int fondBas, int cadre,
			int encre, int encrePale, int creuxHaut, int creuxBas, int accent) {
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
	}

	String traduction() {
		return this.traduction;
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
