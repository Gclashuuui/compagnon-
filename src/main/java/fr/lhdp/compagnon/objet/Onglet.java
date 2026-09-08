package fr.lhdp.compagnon.objet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.contenu.Donnable;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * L'onglet creatif du mod : tout ce qui se donne a un compagnon, au meme endroit.
 *
 * <p>Sans lui, l'equipe devait taper {@code /compagnon objet <joueur> <genre>
 * <variete>} pour voir a quoi ressemblait une seule friandise. Maintenant on
 * ouvre l'onglet et tout est la.
 *
 * <h2>Un seul objet, soixante-dix apparences</h2>
 *
 * <p>Le mod n'enregistre que deux objets, {@code aliment} et {@code soin}, qui
 * portent leur variete dans une donnee. C'est ce qui permet d'ajouter une
 * friandise sans recompiler. En echange, tous les exemplaires partageraient la
 * meme image — d'ou le numero de modele : chaque variete en a un, le fichier de
 * modele de l'objet liste les correspondances, et Minecraft affiche la bonne
 * texture.
 *
 * <h2>Pourquoi on relit les fichiers ici</h2>
 *
 * <p>{@link fr.lhdp.compagnon.contenu.Contenu} charge les memes fichiers, mais
 * <b>cote serveur</b>. L'onglet, lui, se remplit chez le joueur : sur un serveur
 * dedie, le client n'a jamais vu ces donnees. On les relit donc directement dans
 * le jar du mod, ce que les deux cotes savent faire.
 *
 * <p>Consequence assumee : un pack de donnees exterieur qui ajouterait un aliment
 * ne le verrait pas apparaitre ici. Ce n'est pas une perte — il faudrait de toute
 * facon fournir une texture et un modele, donc toucher aux ressources du client.
 */
public final class Onglet {

	public static final ResourceKey<CreativeModeTab> GENERAL =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, Compagnon.id("general"));

	private Onglet() {
	}

	public static void enregistrer() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, GENERAL, FabricItemGroup.builder()
				.icon(() -> new ItemStack(Objets.OEUF))
				.title(Component.translatable("itemGroup.compagnon.general"))
				.displayItems((parametres, sortie) -> {
					sortie.accept(new ItemStack(Objets.OEUF));
					sortie.accept(new ItemStack(Objets.BALLE));
					sortie.accept(new ItemStack(Objets.OS_A_MACHER));
					for (ItemStack pile : varietes("aliments", Objets.ALIMENT)) {
						sortie.accept(pile);
					}
					for (ItemStack pile : varietes("soins", Objets.SOIN)) {
						sortie.accept(pile);
					}
				})
				.build());
	}

	/**
	 * Un exemplaire par variete, chacun avec sa variete et son numero de modele.
	 *
	 * <p>Ranges par numero de modele, c'est-a-dire dans l'ordre des planches
	 * dessinees : les plats ensemble, les confiseries ensemble. Un tri
	 * alphabetique melangerait tout.
	 */
	private static List<ItemStack> varietes(String dossier, Item objet) {
		List<ItemStack> piles = new ArrayList<>();
		Optional<Path> racine = FabricLoader.getInstance()
				.getModContainer(Compagnon.MOD_ID)
				.flatMap(mod -> mod.findPath("data/" + Compagnon.MOD_ID + "/" + dossier));

		if (racine.isEmpty()) {
			Compagnon.LOG.warn("Onglet creatif : dossier {} introuvable dans le mod.", dossier);
			return piles;
		}

		record Entree(String id, int modele) {
		}
		List<Entree> entrees = new ArrayList<>();

		try (Stream<Path> fichiers = Files.list(racine.get())) {
			for (Path fichier : fichiers.toList()) {
				String nom = fichier.getFileName().toString();
				if (!nom.endsWith(".json")) {
					continue;
				}
				try (BufferedReader lecteur = Files.newBufferedReader(fichier)) {
					JsonObject contenu = JsonParser.parseReader(lecteur).getAsJsonObject();
					if (!contenu.has("modele")) {
						continue;
					}
					entrees.add(new Entree(nom.substring(0, nom.length() - 5),
							contenu.get("modele").getAsInt()));
				}
			}
		} catch (IOException | RuntimeException echec) {
			// Un onglet incomplet vaut mieux qu'un jeu qui refuse de s'ouvrir.
			Compagnon.LOG.error("Onglet creatif : {} illisible ({}).", dossier, echec.toString());
			return piles;
		}

		entrees.sort(Comparator.comparingInt(Entree::modele));
		for (Entree entree : entrees) {
			// Meme fabrique que la commande de distribution : voir Objets#exemplaire.
			piles.add(Objets.exemplaire(objet,
					new Donnable(entree.id(), entree.id(), java.util.Map.of(), "", entree.modele())));
		}
		return piles;
	}
}
