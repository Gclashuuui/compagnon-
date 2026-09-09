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
			0xCC292135, 0xEE3F6E66, 0xFFBFA1C8, 0xFFE8FCF2);

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
