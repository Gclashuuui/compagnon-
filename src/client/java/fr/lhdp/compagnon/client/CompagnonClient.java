package fr.lhdp.compagnon.client;

import fr.lhdp.compagnon.Compagnon;
import fr.lhdp.compagnon.espece.Especes;
import fr.lhdp.compagnon.reseau.PaquetCaresse;
import fr.lhdp.compagnon.reseau.PaquetDemandeNom;
import fr.lhdp.compagnon.reseau.PaquetDonneesCarnet;
import fr.lhdp.compagnon.reseau.PaquetDonneesClassement;
import fr.lhdp.compagnon.reseau.PaquetDonneesLivre;
import fr.lhdp.compagnon.reseau.PaquetDonneesRoue;
import fr.lhdp.compagnon.reseau.PaquetEspeces;
import fr.lhdp.compagnon.reseau.PaquetJauges;
import fr.lhdp.compagnon.reseau.PaquetMontee;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;

/**
 * Point d'entree du mod, cote client. N'existe que pour l'affichage et les
 * touches : il ne decide de rien.
 *
 * <p>Le client ne lit aucun fichier d'espece. Il attend que le serveur lui envoie
 * le catalogue — a la connexion, et apres chaque {@code /reload}.
 */
public class CompagnonClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(Compagnon.COMPAGNON, CompagnonRenderer::new);

		// Le catalogue des especes : geometrie, animations, textures des variantes.
		// Le jouet en vol se dessine comme l'objet qu'il porte.
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
				fr.lhdp.compagnon.objet.Objets.JOUET_LANCE,
				net.minecraft.client.renderer.entity.ThrownItemRenderer::new);

		ClientPlayNetworking.registerGlobalReceiver(
				fr.lhdp.compagnon.reseau.PaquetAncrages.TYPE, (paquet, contexte) ->
				contexte.client().execute(() ->
					fr.lhdp.compagnon.espece.Ancrages.poser(paquet.ancrages())));

		ClientPlayNetworking.registerGlobalReceiver(
				fr.lhdp.compagnon.reseau.PaquetOuvrirAncrage.TYPE, (paquet, contexte) ->
				contexte.client().execute(() ->
					contexte.client().setScreen(new EcranAncrage(paquet.espece()))));

		ClientPlayNetworking.registerGlobalReceiver(
				fr.lhdp.compagnon.reseau.PaquetOuvrirRemise.TYPE, (paquet, contexte) ->
				contexte.client().execute(() ->
					contexte.client().setScreen(new EcranRemise(paquet.joueurs()))));

		ClientPlayNetworking.registerGlobalReceiver(PaquetEspeces.TYPE, (paquet, contexte) ->
				contexte.client().execute(() -> Especes.poser(paquet.especes())));

		// Le serveur a valide le certificat : on ouvre l'ecran de nom. Le passage
		// par la file du client est obligatoire — un paquet arrive sur le fil
		// reseau, pas sur celui qui dessine.
		ClientPlayNetworking.registerGlobalReceiver(PaquetDemandeNom.TYPE, (paquet, contexte) ->
				contexte.client().execute(() ->
						Minecraft.getInstance().setScreen(
								new EcranNommer(paquet.espece(), paquet.variante()))));

		// Le livre : le serveur a repondu, on l'ouvre avec ce qu'il a envoye.
		ClientPlayNetworking.registerGlobalReceiver(PaquetDonneesLivre.TYPE, (paquet, contexte) ->
				contexte.client().execute(() -> {
					// Les touches suivent : rouvrir le livre rouvrira celui-ci, et non
					// le premier de la liste comme avant.
					Touches.retenir(paquet.index());
					Minecraft.getInstance().setScreen(new EcranLivre(
							paquet.donnees(), paquet.combien(), paquet.index(),
							paquet.compagnons()));
				}));

		// La roue : le serveur a dit ce qui est ouvert et ce qui est verrouille.
		ClientPlayNetworking.registerGlobalReceiver(PaquetDonneesRoue.TYPE, (paquet, contexte) ->
				contexte.client().execute(() -> {
					Touches.retenir(paquet.index());
					Minecraft.getInstance().setScreen(new EcranRoue(
							paquet.index(), paquet.nom(), paquet.niveau(),
							paquet.entrees(), paquet.compagnons()));
				}));

		// Le carnet. Si l'ecran est deja ouvert on remplace seulement ses donnees :
		// le rouvrir remettrait la liste a sa premiere page et ferait clignoter le
		// modele, juste apres un clic ou le joueur regarde.
		ClientPlayNetworking.registerGlobalReceiver(PaquetDonneesCarnet.TYPE, (paquet, contexte) ->
				contexte.client().execute(() -> {
					Touches.retenir(paquet.choisi());
					if (Minecraft.getInstance().screen instanceof EcranCarnet ouvert) {
						ouvert.mettreAJour(paquet.choisi(), paquet.entrees());
					} else {
						Minecraft.getInstance().setScreen(
								new EcranCarnet(paquet.choisi(), paquet.entrees()));
					}
				}));

		// LE CLASSEMENT. On ne l'ouvre que sur reponse du serveur : c'est lui qui
		// sait si le staff l'a ferme, et lui qui a les donnees.
		ClientPlayNetworking.registerGlobalReceiver(PaquetDonneesClassement.TYPE,
			(paquet, contexte) -> contexte.client().execute(() ->
				Minecraft.getInstance().setScreen(new EcranClassement(
					paquet.lignes(), paquet.monRang(), paquet.participants(),
					paquet.debutVoisinage()))));

		// La caresse : le serveur dit qui caresse quoi, et pour combien de temps.
		ClientPlayNetworking.registerGlobalReceiver(PaquetCaresse.TYPE, (paquet, contexte) ->
				contexte.client().execute(() ->
						Caresses.commencer(paquet.joueur(), paquet.compagnon(), paquet.ticks())));

		// Le petit panneau : trois nombres deja arrondis, envoyes seulement quand
		// ils ont change. Le client ne calcule rien.
		ClientPlayNetworking.registerGlobalReceiver(PaquetJauges.TYPE, (paquet, contexte) ->
				contexte.client().execute(() -> Panneau.poser(paquet.jauges())));
		ClientPlayNetworking.registerGlobalReceiver(PaquetMontee.TYPE, (paquet, contexte) ->
				contexte.client().execute(() -> AnnonceNiveau.poser(paquet)));

		HudRenderCallback.EVENT.register((graphismes, delta) -> {
			Panneau.dessiner(graphismes, delta.getRealtimeDeltaTicks());
			AnnonceNiveau.dessiner(graphismes, delta.getRealtimeDeltaTicks());
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			Caresses.tick();
			AnnonceNiveau.tick();
		});
		ClientPlayConnectionEvents.DISCONNECT.register((gestionnaire, client) -> {
			Caresses.oublier();
			// Sans ceci, le panneau garderait a l'ecran l'etat d'un compagnon d'un
			// autre serveur en attendant le premier paquet du nouveau.
			Panneau.oublier();
			AnnonceNiveau.oublier();
			// Un pack de ressources a pu changer : on redemandera quelles variantes
			// ont un calque lumineux.
			CalqueLumineux.oublier();
			Apercu.oublier();
		});

		Touches.enregistrer();

		Compagnon.LOG.info("Compagnon : cote client charge.");
	}
}
