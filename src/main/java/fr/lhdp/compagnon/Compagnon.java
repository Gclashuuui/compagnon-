package fr.lhdp.compagnon;

import fr.lhdp.compagnon.mission.Moules;
import fr.lhdp.compagnon.commande.Commandes;
import fr.lhdp.compagnon.contenu.Contenu;
import fr.lhdp.compagnon.entite.CompagnonEntity;
import fr.lhdp.compagnon.entite.Curiosite;
import fr.lhdp.compagnon.entite.Humeurs;
import fr.lhdp.compagnon.espece.Especes;
import fr.lhdp.compagnon.fiche.Apparition;
import fr.lhdp.compagnon.fiche.Secours;
import fr.lhdp.compagnon.fiche.Vie;
import fr.lhdp.compagnon.objet.Objets;
import fr.lhdp.compagnon.objet.Onglet;
import fr.lhdp.compagnon.progression.Niveaux;
import fr.lhdp.compagnon.competence.Competences;
import fr.lhdp.compagnon.reglage.Reglages;
import fr.lhdp.compagnon.reseau.Reseau;
import fr.lhdp.compagnon.voix.Vocabulaire;
import fr.lhdp.compagnon.voix.Voix;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Point d'entree du mod, cote serveur.
 *
 * <p>Le serveur fait autorite sur tout : aucune statistique, aucun deblocage,
 * aucun ordre ne se decide cote client.
 */
public class Compagnon implements ModInitializer {

	public static final String MOD_ID = "compagnon";

	public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

	/**
	 * Une seule entite pour toutes les especes : l'espece est une donnee portee
	 * par l'entite, pas une sous-classe.
	 *
	 * <p>La boite de collision est fixe pour l'instant — valeur inventee, a
	 * regler. Elle devra suivre l'espece le jour de la caresse (etape 8), qui a
	 * besoin de savoir ou poser la main.
	 */
	public static final EntityType<CompagnonEntity> COMPAGNON = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			id("compagnon"),
			EntityType.Builder.of(CompagnonEntity::new, MobCategory.CREATURE)
					.sized(0.9F, 0.9F)
					// 3 troncons = 48 blocs. Il disparait a 36, donc personne ne le voit
					// surgir ; et c'est un quart de trafic de suivi en moins par joueur.
					.clientTrackingRange(3)
					.build("compagnon"));

	/**
	 * Construit un identifiant du mod. Depuis la 1.21 le constructeur de
	 * {@link ResourceLocation} est prive : c'est cette fabrique qu'il faut appeler.
	 */
	public static ResourceLocation id(String chemin) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, chemin);
	}

	@Override
	public void onInitialize() {
		// Obligatoire. Sans lui l'entite plante au spawn, et le message d'erreur
		// ne dit pas clairement ce qui manque.
		FabricDefaultAttributeRegistry.register(COMPAGNON, CompagnonEntity.attributs());

		Objets.enregistrer();
		Onglet.enregistrer();
		Reseau.enregistrerTypes();
		Reseau.enregistrerReceptions();

		// La table des niveaux et le contenu vivent dans data/ : c'est le serveur
		// qui fait autorite. Un /reload les relit, sans recompiler.
		ResourceManagerHelper.get(PackType.SERVER_DATA)
				.registerReloadListener(new SimpleSynchronousResourceReloadListener() {

					@Override
					public ResourceLocation getFabricId() {
						return id("donnees");
					}

					@Override
					public void onResourceManagerReload(ResourceManager gestionnaire) {
						Especes.charger(gestionnaire);
						fr.lhdp.compagnon.espece.Ancrages.charger(gestionnaire);
						Niveaux.charger(gestionnaire);
						Reglages.charger(gestionnaire);
						Competences.charger(gestionnaire);
						Moules.charger(gestionnaire);
						Contenu.charger(gestionnaire);
						Vocabulaire.charger(gestionnaire);
					}
				});

		// Fait apparaitre et disparaitre les entites autour des joueurs. La fiche
		// reste, l'entite n'est qu'un affichage.
		Apparition.enregistrer();

		// La derive des barres, seulement pendant que le proprietaire joue.
		Vie.enregistrer();

		// Une copie de secours des fiches a chaque demarrage reussi, et une alerte
		// bruyante si elles ont disparu sans raison.
		ServerLifecycleEvents.SERVER_STARTED.register(Secours::verifierAuDemarrage);

		// Le classement est du cache : il ne doit rien savoir du monde precedent.
		ServerLifecycleEvents.SERVER_STARTED.register(serveur ->
				fr.lhdp.compagnon.classement.Classement.oublierTout());

		// Il remarque ce qu'on fait autour de lui : poser un bloc, en casser un.
		Curiosite.enregistrer();

		// Il a un avis sur l'endroit ou il est : l'orage, l'eau, le noir, l'ennui.
		Humeurs.enregistrer();
		// QUAND TU DORS, IL DORT. Deux evenements du jeu auxquels personne ne
		// repondait, et le seul moment de la journee ou tout un dortoir fait la
		// meme chose en meme temps.
		fr.lhdp.compagnon.entite.Sommeil.enregistrer();
		// Ce que le compagnon te montre s eteint tout seul.
		fr.lhdp.compagnon.entite.Reveles.enregistrer();

		// Les ordres a la voix, si le serveur a Plasmo Voice et un modele vocal.
		// Sans l'un ou l'autre, cette ligne ne fait que l'ecrire dans le log.
		Voix.enregistrer();

		Commandes.enregistrer();

		LOG.info("Compagnon : cote serveur charge.");
	}
}
