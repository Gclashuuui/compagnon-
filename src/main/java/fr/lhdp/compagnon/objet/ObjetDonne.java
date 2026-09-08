package fr.lhdp.compagnon.objet;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Un objet qu'on donne au compagnon : un aliment ou un remede.
 *
 * <p>Il ne fait rien tout seul. Ce qu'il produit est decrit dans
 * {@code data/compagnon/aliments/} ou {@code data/compagnon/soins/}, et applique
 * par le serveur au moment du clic droit sur le compagnon.
 *
 * <p>Une seule classe pour les deux, et un seul objet enregistre par genre : la
 * <b>variete</b> est une donnee portee par l'objet. Ajouter un aliment, c'est
 * ajouter un fichier.
 */
public class ObjetDonne extends Item {

	/** Ce que l'objet est, cote regles du jeu. */
	public enum Genre {
		ALIMENT,
		SOIN
	}

	private final Genre genre;

	public ObjetDonne(Properties proprietes, Genre genre) {
		super(proprietes);
		this.genre = genre;
	}

	public Genre genre() {
		return this.genre;
	}

	@Override
	public void appendHoverText(ItemStack pile, TooltipContext contexte,
			List<Component> lignes, TooltipFlag drapeau) {
		String variete = pile.get(Objets.VARIETE);
		// Le detail de l'effet vit dans les donnees du serveur, que le client n'a
		// pas. On affiche donc ce qu'on sait a coup sur : de quoi il s'agit.
		lignes.add(Component.literal(variete == null ? "sans variete" : variete)
				.withStyle(variete == null ? ChatFormatting.RED : ChatFormatting.GRAY));
	}
}
