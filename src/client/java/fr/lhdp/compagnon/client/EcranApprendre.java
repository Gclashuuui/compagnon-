package fr.lhdp.compagnon.client;

import fr.lhdp.compagnon.reseau.PaquetApprendre;
import fr.lhdp.compagnon.reseau.PaquetRoue;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * « Apprends ce mot. »
 *
 * <h2>Pourquoi ca passe par un ecran et pas par la voix</h2>
 *
 * <p>On ne peut pas apprendre un mot <b>en le disant</b>, et ce n'est pas un
 * choix : le moteur travaille en liste fermee. Il ne peut entendre que des mots
 * qu'il guette deja. Un mot qu'on veut lui apprendre n'y est, par definition,
 * pas encore — il serait donc entendu comme du bruit, ou comme un autre mot.
 *
 * <p>On l'ecrit donc une fois. Ensuite, il se dit — et c'est la tout l'interet.
 *
 * <h2>Ce que cet ecran ne decide pas</h2>
 *
 * <p>Rien. Il envoie le mot et attend. C'est le serveur qui verifie que le geste
 * est ouvert, que le mot n'est pas deja pris, et surtout que <b>le micro sait le
 * dire</b> — sinon le joueur dresserait sa bete pour rien et crierait pendant des
 * semaines un mot qui ne peut pas etre entendu.
 *
 * <p>Quand le serveur refuse, il dit pourquoi et propose des mots qui
 * marcheraient.
 */
public class EcranApprendre extends EcranCompagnon {

	private static final int LARGEUR_CHAMP = 200;
	private static final int HAUTEUR_CHAMP = 20;
	private static final int LARGEUR_BOUTON = 98;
	private static final int HAUTEUR_BOUTON = 20;

	private final int index;
	private final String animation;
	private final String motActuel;
	private final String nomCompagnon;

	private EditBox champ;

	public EcranApprendre(int index, String nomCompagnon, String animation, String motActuel) {
		super(Component.translatable("roue.compagnon.apprendre"));
		this.index = index;
		this.nomCompagnon = nomCompagnon;
		this.animation = animation;
		this.motActuel = motActuel == null ? "" : motActuel;
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int cy = this.height / 2;

		this.champ = new EditBox(this.font, cx - LARGEUR_CHAMP / 2, cy - 4,
				LARGEUR_CHAMP, HAUTEUR_CHAMP,
				Component.translatable("roue.compagnon.le_mot"));
		this.champ.setMaxLength(PaquetApprendre.LONGUEUR_MAX);
		this.champ.setValue(this.motActuel);
		addRenderableWidget(this.champ);
		setInitialFocus(this.champ);

		addRenderableWidget(BoutonDuMod.de(
			cx - LARGEUR_BOUTON - 2, cy + 26, LARGEUR_BOUTON, HAUTEUR_BOUTON,
			Component.translatable("roue.compagnon.valider"), bouton -> valider()));

		// « Qu'il oublie » plutot qu'« Annuler » : annuler est deja la touche
		// d'echappement, et ce bouton-la fait quelque chose de different.
		addRenderableWidget(BoutonDuMod.de(
			cx + 2, cy + 26, LARGEUR_BOUTON, HAUTEUR_BOUTON,
			Component.translatable("roue.compagnon.oublier"), bouton -> oublier()));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void render(GuiGraphics g, int sourisX, int sourisY, float partiel) {
		renderBackground(g, sourisX, sourisY, partiel);
		super.render(g, sourisX, sourisY, partiel);

		int cx = this.width / 2;
		int cy = this.height / 2;

		String titre = Component.translatable("roue.compagnon.apprendre_a",
				this.nomCompagnon).getString();
		g.drawCenteredString(this.font, titre, cx, cy - 46, 0xFFFFFFFF);

		// Le nom d'animation tel quel : c'est celui du fichier, et c'est ce qui
		// permet de s'y retrouver quand on en a huit.
		g.drawCenteredString(this.font, this.animation, cx, cy - 32, 0xFFA0A0A0);

		String consigne = Component.translatable("roue.compagnon.consigne").getString();
		g.drawCenteredString(this.font, consigne, cx, cy - 18, 0xFF808080);

		clochette(g, sourisX, sourisY, partiel);
	}

	@Override
	public boolean mouseClicked(double sourisX, double sourisY, int bouton) {
		if (clochetteCliquee(sourisX, sourisY, bouton)) {
			return true;
		}
		return super.mouseClicked(sourisX, sourisY, bouton);
	}

	@Override
	public boolean keyPressed(int touche, int codeMateriel, int modificateurs) {
		if (touche == GLFW.GLFW_KEY_ENTER || touche == GLFW.GLFW_KEY_KP_ENTER) {
			valider();
			return true;
		}
		return super.keyPressed(touche, codeMateriel, modificateurs);
	}

	private void valider() {
		envoyer(this.champ.getValue().trim());
	}

	/** Un mot vide veut dire « oublie ce tour » : le serveur le comprend ainsi. */
	private void oublier() {
		envoyer("");
	}

	private void envoyer(String mot) {
		ClientPlayNetworking.send(new PaquetApprendre(this.index, this.animation, mot));
		// Le serveur repondra par la roue a jour. En attendant, on la redemande :
		// sans cela, un refus laisserait le joueur devant un ecran vide.
		ClientPlayNetworking.send(new PaquetRoue(this.index));
	}
}
