package fr.lhdp.compagnon.objet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Un petit meuble du coin personnel, avec une collision qui suit son vrai volume. */
public final class BlocCoin extends Block {

	public enum Forme {
		PERCHOIR
	}

	private static final VoxelShape PERCHOIR = Block.box(2, 0, 3, 14, 13, 13);

	private final Forme forme;

	public BlocCoin(Forme forme, BlockBehaviour.Properties proprietes) {
		super(proprietes);
		this.forme = forme;
	}

	@Override
	protected VoxelShape getShape(BlockState etat, BlockGetter monde, BlockPos position,
			CollisionContext contexte) {
		return switch (this.forme) {
			case PERCHOIR -> PERCHOIR;
		};
	}
}
