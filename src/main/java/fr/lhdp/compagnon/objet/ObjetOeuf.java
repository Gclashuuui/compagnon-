package fr.lhdp.compagnon.objet;

import fr.lhdp.compagnon.reseau.PaquetDemandeNom;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * L'oeuf : l'objet que l'equipe remet a un joueur.
 *
 * <p>Il porte deja l'espece et la variante. Le joueur l'utilise <b>ou il veut,
 * quand il veut</b> — ce qui evite d'avoir un membre de l'equipe et le joueur
 * connectes en meme temps.
 *
 * <p>Il n'est pas consomme ici : il l'est quand le nom revient du client, une
 * fois la fiche reellement creee. Fermer l'ecran ne coute donc rien.
 */
public class ObjetOeuf extends Item {

	public ObjetOeuf(Properties proprietes) {
		super(proprietes);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level niveau, Player joueur, InteractionHand main) {
		ItemStack pile = joueur.getItemInHand(main);

		if (!(joueur instanceof ServerPlayer serveurJoueur)) {
			return InteractionResultHolder.success(pile);
		}

		Origine origine = pile.get(Objets.ORIGINE);
		if (origine == null) {
			serveurJoueur.sendSystemMessage(Component
					.literal("Cet oeuf est vide : il ne dit ni l'espece ni la variante.")
					.withStyle(ChatFormatting.RED));
			return InteractionResultHolder.fail(pile);
		}

		// Le serveur a valide : le client peut ouvrir l'ecran de nom.
		ServerPlayNetworking.send(serveurJoueur,
				new PaquetDemandeNom(origine.espece(), origine.variante()));

		return InteractionResultHolder.consume(pile);
	}

	@Override
	public void appendHoverText(ItemStack pile, TooltipContext contexte,
			List<Component> lignes, TooltipFlag drapeau) {
		Origine origine = pile.get(Objets.ORIGINE);
		if (origine == null) {
			lignes.add(Component.literal("Oeuf vide").withStyle(ChatFormatting.RED));
			return;
		}
		lignes.add(Component.literal(
				fr.lhdp.compagnon.espece.Especes.titre(origine.espece())
					+ " · " + origine.variante())
				.withStyle(ChatFormatting.GRAY));
	}
}
