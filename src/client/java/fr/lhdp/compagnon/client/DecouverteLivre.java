package fr.lhdp.compagnon.client;

import fr.lhdp.compagnon.Compagnon;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Retient localement si ce joueur a deja vu l'ouverture speciale du livre. */
final class DecouverteLivre {

	private static final String FICHIER = Compagnon.MOD_ID + "-livre-decouvert.txt";
	private static boolean verifie;

	private DecouverteLivre() {
	}

	/**
	 * Vrai une seule fois, sur cette installation du jeu.
	 *
	 * <p>Ce n'est pas une donnee du compagnon et elle ne part jamais au serveur :
	 * c'est uniquement une petite mise en scene d'interface. Si le fichier ne peut
	 * pas etre ecrit, on la montre une fois dans la session et on continue.
	 */
	static boolean premiereOuverture() {
		if (verifie) {
			return false;
		}
		verifie = true;
		Path marque = FabricLoader.getInstance().getConfigDir().resolve(FICHIER);
		if (Files.exists(marque)) {
			return false;
		}
		try {
			Files.createDirectories(marque.getParent());
			Files.writeString(marque, "vu", StandardCharsets.UTF_8);
		} catch (Exception echec) {
			Compagnon.LOG.warn("L'ouverture du livre n'a pas pu etre retenue : {}",
					echec.toString());
		}
		return true;
	}
}
