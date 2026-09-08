package fr.lhdp.compagnon.objet;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Un meuble dont l'inventaire explique immédiatement l'utilité. */
public final class ObjetMeuble extends BlockItem {

	private final String aide;

	public ObjetMeuble(Block bloc, Item.Properties proprietes, String aide) {
		super(bloc, proprietes);
		this.aide = aide;
	}

	@Override
	public void appendHoverText(ItemStack pile, Item.TooltipContext contexte,
			List<Component> lignes, TooltipFlag drapeau) {
		super.appendHoverText(pile, contexte, lignes, drapeau);
		lignes.add(Component.translatable(this.aide).withStyle(ChatFormatting.GRAY));
	}
}
