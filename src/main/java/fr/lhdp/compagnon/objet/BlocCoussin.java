package fr.lhdp.compagnon.objet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Un coussin dont les bordures disparaissent lorsqu'on en assemble plusieurs. */
public final class BlocCoussin extends Block {

	public static final BooleanProperty NORD = BooleanProperty.create("north");
	public static final BooleanProperty EST = BooleanProperty.create("east");
	public static final BooleanProperty SUD = BooleanProperty.create("south");
	public static final BooleanProperty OUEST = BooleanProperty.create("west");
	private static final VoxelShape FORME = Block.box(0, 0, 0, 16, 3, 16);

	public BlocCoussin(BlockBehaviour.Properties proprietes) {
		super(proprietes);
		registerDefaultState(this.stateDefinition.any()
				.setValue(NORD, false).setValue(EST, false)
				.setValue(SUD, false).setValue(OUEST, false));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext contexte) {
		BlockGetter monde = contexte.getLevel();
		BlockPos position = contexte.getClickedPos();
		return defaultBlockState()
				.setValue(NORD, connecte(monde, position.north()))
				.setValue(EST, connecte(monde, position.east()))
				.setValue(SUD, connecte(monde, position.south()))
				.setValue(OUEST, connecte(monde, position.west()));
	}

	@Override
	protected BlockState updateShape(BlockState etat, Direction direction, BlockState voisin,
			LevelAccessor monde, BlockPos position, BlockPos positionVoisine) {
		return switch (direction) {
			case NORTH -> etat.setValue(NORD, voisin.is(this));
			case EAST -> etat.setValue(EST, voisin.is(this));
			case SOUTH -> etat.setValue(SUD, voisin.is(this));
			case WEST -> etat.setValue(OUEST, voisin.is(this));
			default -> etat;
		};
	}

	private boolean connecte(BlockGetter monde, BlockPos position) {
		return monde.getBlockState(position).is(this);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> definition) {
		definition.add(NORD, EST, SUD, OUEST);
	}

	@Override
	protected VoxelShape getShape(BlockState etat, BlockGetter monde, BlockPos position,
			CollisionContext contexte) {
		return FORME;
	}
}
