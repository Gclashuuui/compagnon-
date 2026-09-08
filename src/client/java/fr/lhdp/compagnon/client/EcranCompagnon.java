package fr.lhdp.compagnon.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base commune aux ecrans du mod.
 *
 * <p>Elle coupe le flou, et elle pose la clochette du son dans le meme coin
 * partout.
 *
 * <h2>Le flou</h2>
 *
 * <p>Depuis la 1.20.5, Minecraft trouble la scene derriere chaque interface. Sur
 * un carnet ou une roue qu'on ouvre en plein jeu, ca brouille tout ce qu'on
 * voulait regarder — son compagnon compris.
 *
 * <p>Le voile sombre du fond, lui, est conserve : c'est ce qui detache
 * l'interface du decor.
 */
public abstract class EcranCompagnon extends Screen {

	protected EcranCompagnon(Component titre) {
		super(titre);
	}

	/**
	 * Le nom lisible d'une animation.
	 *
	 * <p>Si le fichier de langue la traduit, on prend la traduction. Sinon on se
	 * rabat sur la derniere partie du nom, ce qui reste lisible. Dans les deux
	 * cas le nom vient de la table, jamais du code.
	 *
	 * <p>Ici et non dans un seul ecran : la roue et le livre nomment desormais
	 * les memes gestes, et deux facons de les nommer donneraient deux noms
	 * differents pour la meme chose.
	 */
	protected static String etiquette(String nom) {
		String cle = "roue.compagnon.action." + nom;
		if (net.minecraft.locale.Language.getInstance().has(cle)) {
			return Component.translatable(cle).getString();
		}
		int point = nom.lastIndexOf('.');
		String court = point >= 0 ? nom.substring(point + 1) : nom;
		return court.replace('_', ' ');
	}

	/**
	 * Dessine la clochette du son, en haut a droite.
	 *
	 * <p>A appeler <b>en dernier</b> dans le rendu : elle doit passer par-dessus
	 * les voiles et les panneaux, sans quoi elle se retrouve dans le noir sur la
	 * roue.
	 *
	 * <p>Ici et non dans chaque ecran : un reglage qui change de place d'un ecran
	 * a l'autre est un reglage qu'on ne retrouve jamais.
	 */
	protected void clochette(GuiGraphics g, int sourisX, int sourisY, float partiel) {
		Clochette.dessiner(g, this.width, sourisX, sourisY, partiel);
		if (Clochette.sous(sourisX, sourisY, this.width)) {
			// SOUS LA CLOCHE, JAMAIS AU-DESSUS.
			//
			// L infobulle se place au-dessus de la souris. Or la cloche est collee
			// au bord haut de l ecran : la phrase sortait de l ecran et se faisait
			// couper en deux dans le sens de la hauteur.
			g.renderTooltip(this.font, Clochette.motDeLEtat(), sourisX,
				Clochette.y() + Clochette.HAUTEUR + 20);
		}
	}

	/**
	 * A appeler en <b>premier</b> dans un clic : vrai si c'etait la clochette.
	 *
	 * <p>En premier, parce qu'elle est posee par-dessus tout le reste : si un
	 * autre element occupait le meme coin, c'est elle qu'on voit, donc c'est elle
	 * qu'on croit cliquer.
	 *
	 * <p>Au clic gauche seulement. Elle repondait a n'importe quel bouton : un
	 * clic droit, un clic molette, et le son du mod changeait sans qu'on ait
	 * rien demande.
	 */
	protected boolean clochetteCliquee(double sourisX, double sourisY, int bouton) {
		if (bouton == 0 && Clochette.sous(sourisX, sourisY, this.width)) {
			Clochette.tinter();
			return true;
		}
		return false;
	}

	/**
	 * Volontairement vide. C'est la seule methode qui applique le flou ; en la
	 * neutralisant on garde tout le reste du fond intact.
	 */
	@Override
	protected void renderBlurredBackground(float partiel) {
		// Rien.
	}
}
