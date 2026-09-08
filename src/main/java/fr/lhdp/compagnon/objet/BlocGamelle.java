package fr.lhdp.compagnon.objet;

import fr.lhdp.compagnon.contenu.Contenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Une gamelle qui contient vraiment un repas.
 *
 * <p>Le contenu est une {@link ItemEntity} posee au centre. Ce choix garde la
 * vraie texture et les composants de n'importe quel aliment, sans inventer un
 * rendu special par nourriture. L'objet est sauvegarde avec le monde, flotte
 * juste au-dessus du fond et ne peut pas etre ramasse par accident.
 */
public final class BlocGamelle extends Block {

	private static final VoxelShape FORME = Block.box(3, 0, 3, 13, 3, 13);
	private static final AABB INTERIEUR = new AABB(0.16D, 0.0D, 0.16D, 0.84D, 0.58D, 0.84D);

	public BlocGamelle(BlockBehaviour.Properties proprietes) {
		super(proprietes);
	}

	@Override
	protected VoxelShape getShape(BlockState etat, BlockGetter monde, BlockPos position,
			CollisionContext contexte) {
		return FORME;
	}

	/** Vrai pour les repas du mod comme pour la nourriture normale de Minecraft. */
	public static boolean accepte(ItemStack pile) {
		if (pile.isEmpty()) {
			return false;
		}
		if (pile.has(DataComponents.FOOD)) {
			return true;
		}
		if (!pile.is(Objets.ALIMENT)) {
			return false;
		}
		String variete = pile.get(Objets.VARIETE);
		return variete != null && Contenu.aliment(variete) != null;
	}

	/** Le seul objet range dans cette gamelle, ou {@code null}. */
	public static ItemEntity contenu(Level monde, BlockPos position) {
		List<ItemEntity> objets = monde.getEntitiesOfClass(ItemEntity.class,
				INTERIEUR.move(position), BlocGamelle::estRange);
		return objets.isEmpty() ? null : objets.get(0);
	}

	private static boolean estRange(ItemEntity objet) {
		return objet.isAlive() && objet.isNoGravity() && accepte(objet.getItem());
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack pile, BlockState etat, Level monde,
			BlockPos position, Player joueur, InteractionHand main, BlockHitResult touche) {
		if (!accepte(pile)) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (monde.isClientSide()) {
			return ItemInteractionResult.SUCCESS;
		}
		if (contenu(monde, position) != null) {
			joueur.displayClientMessage(Component.translatable("gamelle.compagnon.pleine"), true);
			return ItemInteractionResult.FAIL;
		}

		ItemStack repas = pile.copyWithCount(1);
		ItemEntity pose = new ItemEntity(monde, position.getX() + 0.5D,
				position.getY() + 0.24D, position.getZ() + 0.5D, repas);
		pose.setNoGravity(true);
		pose.setDeltaMovement(Vec3.ZERO);
		pose.setNeverPickUp();
		pose.setUnlimitedLifetime();
		monde.addFreshEntity(pose);
		if (!joueur.getAbilities().instabuild) {
			pile.shrink(1);
		}
		joueur.displayClientMessage(Component.translatable("gamelle.compagnon.depose",
				repas.getHoverName()), true);
		return ItemInteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState etat, Level monde, BlockPos position,
			Player joueur, BlockHitResult touche) {
		if (monde.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		ItemEntity pose = contenu(monde, position);
		if (pose == null) {
			joueur.displayClientMessage(Component.translatable("gamelle.compagnon.vide"), true);
			return InteractionResult.SUCCESS;
		}

		ItemStack repas = pose.getItem().copy();
		pose.discard();
		if (!joueur.getInventory().add(repas)) {
			joueur.drop(repas, false);
		}
		joueur.displayClientMessage(Component.translatable("gamelle.compagnon.reprend",
				repas.getHoverName()), true);
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void onRemove(BlockState ancien, Level monde, BlockPos position,
			BlockState nouveau, boolean piston) {
		if (!ancien.is(nouveau.getBlock()) && !monde.isClientSide()) {
			ItemEntity pose = contenu(monde, position);
			if (pose != null) {
				pose.setNoGravity(false);
				pose.setDefaultPickUpDelay();
				pose.setUnlimitedLifetime();
			}
		}
		super.onRemove(ancien, monde, position, nouveau, piston);
	}
}
