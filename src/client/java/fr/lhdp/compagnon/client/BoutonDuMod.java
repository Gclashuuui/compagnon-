package fr.lhdp.compagnon.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Un bouton aux couleurs du mod.
 *
 * <h2>Deux ecrans sur six ressemblaient au menu des options</h2>
 *
 * <p>« Apprendre un mot » et « Nommer sa bete » se servaient des boutons du jeu :
 * gris pierre, bord clair, exactement ceux du menu Options. Les quatre autres
 * ecrans etaient violets et dores. On passait donc de l'un a l'autre en changeant
 * de mod, ce qui est le contraire de ce qu'on veut au moment ou l'on donne un nom
 * a son compagnon.
 *
 * <p>Celui-ci prend les memes textures que le reste — les quatre coins nets, le
 * cadre qui ne se deforme pas — et le meme allumage progressif. Il ne fait rien
 * d'autre qu'un bouton du jeu, il en a juste l'air d'appartenir a cet ecran-ci.
 *
 * <h2>Et il clique</h2>
 *
 * <p>Le bruit part au moment ou l'on presse, pas au relachement : c'est ainsi
 * dans le jeu, et c'est ce qui fait qu'un bouton repond « tout de suite » plutot
 * que « presque tout de suite ».
 */
public class BoutonDuMod extends Button {

	/** De zero a un : le bouton est en train de s'allumer. */
	private float chaleur;

	public BoutonDuMod(int x, int y, int large, int haut, Component texte, OnPress geste) {
		super(x, y, large, haut, texte, geste, DEFAULT_NARRATION);
	}

	/** Raccourci : la meme chose, sans avoir a repeter le constructeur. */
	public static BoutonDuMod de(int x, int y, int large, int haut, Component texte,
			OnPress geste) {

		return new BoutonDuMod(x, y, large, haut, texte, geste);
	}

	@Override
	public void onPress() {
		Bruits.clic();
		super.onPress();
	}

	@Override
	protected void renderWidget(GuiGraphics g, int sourisX, int sourisY, float partiel) {
		boolean chaud = isHoveredOrFocused() && isActive();
		this.chaleur = Peinture.vers(this.chaleur, chaud ? 1.0F : 0.0F, 0.30F, partiel);

		Peinture.bouton(g, getX(), getY(), getWidth(), getHeight(), this.chaleur, false);

		// Le texte plus pale quand le bouton ne repond pas : c'est la seule chose
		// qui distingue « il ne se passe rien » de « ce bouton ne marche pas ».
		int encre = isActive()
				? Peinture.melanger(Peinture.TEXTE, Peinture.OR, Peinture.adoucir(this.chaleur))
				: Peinture.TEXTE_PALE;

		Minecraft client = Minecraft.getInstance();
		g.drawCenteredString(client.font, getMessage(),
				getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, encre);
	}
}
