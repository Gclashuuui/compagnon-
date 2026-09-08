package fr.lhdp.compagnon.objet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Un coussin légèrement rentré dans son bloc : deux voisins ne se traversent jamais. */
public final class BlocCoussin extends Block {

	private static final VoxelShape FORME = Block.box(1, 0, 1, 15, 3.25D, 15);

	public BlocCoussin(BlockBehaviour.Properties proprietes) {
		super(proprietes);
	}

	@Override
	protected VoxelShape getShape(BlockState etat, BlockGetter monde, BlockPos position,
			CollisionContext contexte) {
		return FORME;
	}
}
