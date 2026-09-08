package fr.lhdp.compagnon.client;

import fr.lhdp.compagnon.contenu.Noms;
import fr.lhdp.compagnon.reseau.PaquetNaissance;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

/**
 * L'ecran ou le joueur donne son nom au compagnon, au moment ou il utilise le
 * certificat.
 *
 * <p>Il ne fait qu'envoyer un nom. Il ne cree rien, ne consomme rien, ne verifie
 * rien : le serveur relit le certificat et decide.
 *
 * <p>Fermer sans valider ne coute rien — le certificat est toujours en main.
 */
public class EcranNommer extends EcranCompagnon {

	private static final int LARGEUR_CHAMP = 200;
	private static final int HAUTEUR_LIGNE = 20;

	/**
	 * Ce qu'on demande au joueur, et pourquoi.
	 *
	 * <p>Dit comme une preference, pas comme une regle : le nom sera accepte quoi
	 * qu'il arrive. Ce qui se perd avec un nom invente, c'est seulement de pouvoir
	 * l'appeler a la voix.
	 */
	private static final Component CONSIGNE = Component.literal(
			"Un nom français, pour pouvoir l'appeler à la voix.");

	private final String espece;
	private final String variante;

	private final Random hasard = new Random();

	private EditBox champ;

	public EcranNommer(String espece, String variante) {
		super(Component.literal("Comment veux-tu l'appeler ?"));
		this.espece = espece;
		this.variante = variante;
	}

	@Override
	protected void init() {
		int gauche = this.width / 2 - LARGEUR_CHAMP / 2;
		int milieu = this.height / 2;

		this.champ = new EditBox(this.font, gauche, milieu - 10,
				LARGEUR_CHAMP, HAUTEUR_LIGNE, Component.literal("nom"));
		this.champ.setMaxLength(PaquetNaissance.LONGUEUR_MAX);
		addRenderableWidget(this.champ);
		setInitialFocus(this.champ);

		// Un bouton qui remplit le champ d'un nom que le micro sait entendre.
		// C'est la reponse la plus douce au probleme des noms inventes : on ne
		// refuse rien, on rend juste le bon choix plus facile que le mauvais.
		addRenderableWidget(BoutonDuMod.de(gauche, milieu + 16, LARGEUR_CHAMP,
			HAUTEUR_LIGNE, Component.translatable("nommer.compagnon.proposer"),
			bouton -> proposer()));

		addRenderableWidget(BoutonDuMod.de(gauche, milieu + 40, LARGEUR_CHAMP,
			HAUTEUR_LIGNE, Component.translatable("nommer.compagnon.valider"),
			bouton -> valider()));
	}

	private void proposer() {
		String nom = Noms.auHasard(this.hasard);
		if (!nom.isEmpty()) {
			this.champ.setValue(nom);
			setFocused(this.champ);
		}
	}

	private void valider() {
		String nom = this.champ.getValue().trim();
		if (nom.isEmpty()) {
			return;
		}
		ClientPlayNetworking.send(new PaquetNaissance(nom));
		onClose();
	}

	@Override
	public boolean keyPressed(int touche, int codeMateriel, int modificateurs) {
		if (touche == GLFW.GLFW_KEY_ENTER || touche == GLFW.GLFW_KEY_KP_ENTER) {
			valider();
			return true;
		}
		return super.keyPressed(touche, codeMateriel, modificateurs);
	}

	@Override
	public void render(GuiGraphics graphismes, int sourisX, int sourisY, float partiel) {
		// Screen.render dessine deja le fond puis les composants.
		super.render(graphismes, sourisX, sourisY, partiel);

		graphismes.drawCenteredString(this.font, this.title,
				this.width / 2, this.height / 2 - 54, 0xFFFFFF);
		graphismes.drawCenteredString(this.font,
				Component.literal(this.espece + " · " + this.variante),
				this.width / 2, this.height / 2 - 40, 0xA0A0A0);

		// La consigne va AU-DESSUS du champ, pas en dessous : elle etait ecrite par
		// dessus le champ de saisie, et on lisait le texte du joueur a travers.
		// Une consigne se lit d'ailleurs avant d'ecrire, pas apres.
		graphismes.drawCenteredString(this.font, CONSIGNE,
				this.width / 2, this.height / 2 - 26, 0x909090);

		clochette(graphismes, sourisX, sourisY, partiel);
	}

	@Override
	public boolean mouseClicked(double sourisX, double sourisY, int bouton) {
		if (clochetteCliquee(sourisX, sourisY, bouton)) {
			return true;
		}
		return super.mouseClicked(sourisX, sourisY, bouton);
	}
}
