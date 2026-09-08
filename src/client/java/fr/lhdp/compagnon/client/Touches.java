package fr.lhdp.compagnon.client;

import com.mojang.blaze3d.platform.InputConstants;
import fr.lhdp.compagnon.entite.CompagnonEntity;
import fr.lhdp.compagnon.reseau.PaquetCarnet;
import fr.lhdp.compagnon.reseau.PaquetClassement;
import fr.lhdp.compagnon.reseau.PaquetChevaucher;
import fr.lhdp.compagnon.reseau.PaquetLivre;
import fr.lhdp.compagnon.reseau.PaquetRoue;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Les touches du mod. Le joueur peut les changer dans les options du jeu.
 *
 * <p>La touche ne fait qu'<b>envoyer une demande</b>. C'est le serveur qui
 * repond avec le contenu du livre, et c'est sa reponse qui ouvre l'ecran : le
 * client ne decide pas de ce qu'il a le droit de voir.
 */
public final class Touches {

	/** Categorie affichee dans les options. */
	private static final String CATEGORIE = "key.categories.compagnon";

	public static final KeyMapping LIVRE = new KeyMapping(
			"key.compagnon.livre",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_B,
			CATEGORIE);

	public static final KeyMapping ROUE = new KeyMapping(
			"key.compagnon.roue",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_R,
			CATEGORIE);

	/**
	 * Le carnet : tous ses compagnons, et lequel sort.
	 *
	 * <p>C sur un clavier francais, et libre dans Minecraft. Surtout pas V, qui
	 * est la touche pour parler de Plasmo Voice : ouvrir un carnet chaque fois
	 * qu'on prend la parole serait une punition.
	 */
	/**
	 * Replie ou deplie le petit panneau du coin.
	 *
	 * <p>Un panneau qu'on ne peut pas faire taire n'est pas discret : c'est un
	 * panneau de plus. H comme « le cacher ».
	 */
	public static final KeyMapping PANNEAU = new KeyMapping(
			"key.compagnon.panneau",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_H,
			CATEGORIE);

	public static final KeyMapping CARNET = new KeyMapping(
			"key.compagnon.carnet",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_C,
			CATEGORIE);

	/**
	 * Le classement du serveur.
	 *
	 * <p>Une touche, et pas une commande : sur ce serveur, un joueur ordinaire
	 * ne tape jamais rien. Les commandes sont pour le staff.
	 *
	 * <p>K comme klassement — J, L et C etaient pris, et K est libre dans les
	 * touches par defaut du jeu. Chacun peut la changer dans les options.
	 */
	public static final KeyMapping CLASSEMENT = new KeyMapping(
			"key.compagnon.classement",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_K,
			CATEGORIE);

	/** Diagnostic facultatif du cerveau, surtout utile aux créateurs d'espèces. */
	public static final KeyMapping DIAGNOSTIC = new KeyMapping(
			"key.compagnon.diagnostic",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_F8,
			CATEGORIE);

	/**
	 * Celui dont on ouvre le livre et la roue.
	 *
	 * <h2>A quoi ca sert</h2>
	 *
	 * <p>Les touches envoyaient toujours zero : le premier compagnon, et lui
	 * seul. Avec deux betes, il fallait ouvrir le livre, faire Tab, et ne
	 * jamais rien fermer — sinon on repartait du premier.
	 *
	 * <p>Maintenant le client retient le dernier ouvert. Tab dans le livre ou
	 * dans la roue change de bete, et la touche suivante ouvre celle-la.
	 *
	 * <p>Ce n'est qu'un souvenir d'affichage : le serveur ramene toujours cet
	 * index dans ses bornes et verifie a qui appartient la bete. Un client qui
	 * mentirait ici n'obtiendrait rien de plus.
	 */
	private static int actif;

	/** La derniere intention verticale envoyee, pour ne pas la repeter. */
	private static int dernierVers;

	// --- Le pilotage d'une monture ------------------------------------------

	/**
	 * Le delai qui separe deux appuis d'un double appui, en ticks.
	 *
	 * <p>Un quart de seconde. C'est la fenetre que le jeu se donne lui-meme pour
	 * le double appui d'avance qui declenche la course : on reprend la meme,
	 * pour que la main n'ait pas deux reflexes a apprendre.
	 */
	private static final int FENETRE_DU_DOUBLE = 5;

	private static boolean appuyaitEspace;
	private static int fenetreEspace;
	private static boolean volArme;

	private static boolean appuyaitAvant;
	private static int fenetreAvant;
	private static boolean galop;

	/** Le compagnon vise par les touches. */
	public static int actif() {
		return actif;
	}

	/** Le serveur vient d'ouvrir celui-la : les touches le suivent. */
	public static void retenir(int index) {
		actif = Math.max(0, index);
	}

	private Touches() {
	}

	public static void enregistrer() {
		KeyBindingHelper.registerKeyBinding(LIVRE);
		KeyBindingHelper.registerKeyBinding(ROUE);
		KeyBindingHelper.registerKeyBinding(CARNET);
		KeyBindingHelper.registerKeyBinding(PANNEAU);
		KeyBindingHelper.registerKeyBinding(CLASSEMENT);
		KeyBindingHelper.registerKeyBinding(DIAGNOSTIC);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// consumeClick vide la file : sans lui, maintenir la touche ouvrirait
			// l'ecran a chaque tick.
			while (LIVRE.consumeClick()) {
				if (client.player != null) {
					ClientPlayNetworking.send(new PaquetLivre(actif));
				}
			}
			while (ROUE.consumeClick()) {
				if (client.player != null) {
					ClientPlayNetworking.send(new PaquetRoue(actif));
				}
			}
			while (CARNET.consumeClick()) {
				if (client.player != null) {
					ClientPlayNetworking.send(new PaquetCarnet(actif));
				}
			}
			while (CLASSEMENT.consumeClick()) {
				if (client.player != null) {
					ClientPlayNetworking.send(new PaquetClassement());
				}
			}
			while (DIAGNOSTIC.consumeClick()) {
				if (client.player != null) {
					BandeauIntention.basculerDiagnostic();
					client.player.displayClientMessage(BandeauIntention.motDeLEtat(), true);
				}
			}
			// LE PILOTAGE D'UNE MONTURE.
			//
			// L'avant, l'arriere et les cotes arrivent au serveur tout seuls. Le saut,
			// non : le champ est protege. On le lit donc ici.
			//
			// Et surtout on pose l'intention SUR L'ENTITE DU CLIENT autant qu'on
			// l'envoie au serveur. Une monture pilotee est simulee par le client de
			// son cavalier : c'est ici que le vol se fait. L'envoyer seulement au
			// serveur revenait a le dire a celui qui ne s'en sert pas — et c'est pour
			// ca qu'appuyer sur espace ne decollait pas.
			//
			// UN APPUI SAUTE, DEUX APPUIS FONT VOLER. Le meme geste que le vol
			// creatif : personne n'a besoin qu'on le lui explique, et il n'y a pas
			// une touche de plus a retenir. Deux appuis a nouveau, ou se poser, et il
			// redevient une bete qui marche.
			//
			// L'ACCROUPI N'EST PAS A NOUS : c'est la touche que Minecraft reserve pour
			// mettre pied a terre, et elle le fait avant que quoi que ce soit d'autre
			// ne parte.
			CompagnonEntity monture = client.player != null
					&& client.player.getVehicle() instanceof CompagnonEntity porteur
					? porteur : null;

			if (monture == null) {
				volArme = false;
				galop = false;
				appuyaitEspace = false;
				appuyaitAvant = false;
			} else {
				if (fenetreEspace > 0) {
					fenetreEspace--;
				}
				if (fenetreAvant > 0) {
					fenetreAvant--;
				}

				boolean espace = client.options.keyJump.isDown();
				if (espace && !appuyaitEspace) {
					if (fenetreEspace > 0) {
						// LE DOUBLE APPUI NE COUPE JAMAIS LE VOL EN PLEIN CIEL.
						//
						// Il n'y a rien a couper : lacher la touche fait deja planer jusqu'au
						// sol, et se poser desarme tout seul. Un interrupteur en l'air ne
						// servirait qu'a tuer quelqu'un qui l'actionne par reflexe.
						volArme = true;
						fenetreEspace = 0;
					} else {
						fenetreEspace = FENETRE_DU_DOUBLE;
						if (!volArme) {
							monture.demanderLeSaut();
						}
					}
				}
				appuyaitEspace = espace;

				// Pose : il a fini de voler. Sans ca, il faudrait redoubler l'appui
				// apres chaque atterrissage pour retrouver un saut.
				if (monture.onGround() && !espace) {
					volArme = false;
				}

				// Arme au sol : le premier battement d'ailes part tout seul, sinon on
				// double l'appui et il ne se passe rien tant qu'on ne tient pas la touche.
				boolean monte = volArme && (espace || monture.onGround());

				// L'ALLURE RAPIDE, sur deux appuis d'avance.
				boolean avant = client.options.keyUp.isDown();
				if (avant && !appuyaitAvant) {
					if (fenetreAvant > 0) {
						galop = true;
						fenetreAvant = 0;
					} else {
						fenetreAvant = FENETRE_DU_DOUBLE;
					}
				}
				if (!avant) {
					galop = false;
				}
				appuyaitAvant = avant;
				monture.poserLeGalop(galop);

				// 1 il monte, 2 il vole sans monter, 0 il ne vole pas. Le 2 existe pour
				// que la bete sache faire la difference entre un cavalier qui plane et un
				// cavalier qui vient simplement de sauter.
				int vers = monte ? 1 : (volArme ? 2 : 0);
				monture.poserLIntentionVerticale(vers);
				if (vers != dernierVers) {
					dernierVers = vers;
					ClientPlayNetworking.send(new PaquetChevaucher(vers));
				}
			}

			while (PANNEAU.consumeClick()) {
				if (client.player != null) {
					Panneau.basculer();
					client.player.displayClientMessage(Panneau.motDeLEtat(), true);
				}
			}
		});
	}
}
