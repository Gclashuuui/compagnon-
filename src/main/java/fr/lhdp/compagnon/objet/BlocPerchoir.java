package fr.lhdp.compagnon.objet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Un perchoir cliquable dont la forme épouse le pied, le montant et la barre. */
public final class BlocPerchoir extends Block {

	private static final VoxelShape FORME = Shapes.or(
			Block.box(2, 0, 3, 14, 2, 13),
			Block.box(7, 2, 7, 9, 12, 9),
			Block.box(1, 11, 7, 15, 13, 9));
	private static final long DUREE_SELECTION = 20L * 20L;
	private static final Map<UUID, Selection> SELECTIONS = new ConcurrentHashMap<>();

	private record Selection(ResourceKey<Level> dimension, BlockPos position, long expireA) {
	}

	public BlocPerchoir(BlockBehaviour.Properties proprietes) {
		super(proprietes);
	}

	@Override
	protected VoxelShape getShape(BlockState etat, BlockGetter monde, BlockPos position,
			CollisionContext contexte) {
		return FORME;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState etat, BlockGetter monde, BlockPos position,
			CollisionContext contexte) {
		return FORME;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState etat, Level monde, BlockPos position,
			Player joueur, BlockHitResult touche) {
		if (monde.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		SELECTIONS.put(joueur.getUUID(), new Selection(monde.dimension(), position.immutable(),
				monde.getGameTime() + DUREE_SELECTION));
		joueur.displayClientMessage(Component.translatable("perchoir.compagnon.selectionne"), true);
		return InteractionResult.SUCCESS;
	}

	/** Rend et consomme le perchoir que ce joueur vient de montrer. */
	public static BlockPos prendreSelection(ServerPlayer joueur) {
		Selection selection = SELECTIONS.remove(joueur.getUUID());
		if (selection == null || !selection.dimension().equals(joueur.level().dimension())
				|| selection.expireA() < joueur.level().getGameTime()
				|| !joueur.level().getBlockState(selection.position()).is(Objets.PERCHOIR)) {
			return null;
		}
		return selection.position();
	}
}
