package org.violetmoon.quark.content.building.block;

import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.violetmoon.quark.base.Quark;
import org.violetmoon.zeta.block.ZetaBlock;
import org.violetmoon.zeta.module.ZetaModule;
import org.violetmoon.zeta.registry.RenderLayerRegistry;

public class WoodPostBlock extends ZetaBlock implements SimpleWaterloggedBlock {

	private static final VoxelShape SHAPE_X = Block.box(0F, 6F, 6F, 16F, 10F, 10F);
	private static final VoxelShape SHAPE_Y = Block.box(6F, 0F, 6F, 10F, 16F, 10F);
	private static final VoxelShape SHAPE_Z = Block.box(6F, 6F, 0F, 10F, 10F, 16F);

	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	public static final EnumProperty<Axis> AXIS = BlockStateProperties.AXIS;

	public static final BooleanProperty[] CHAINED = new BooleanProperty[] {
			BooleanProperty.create("chain_down"),
			BooleanProperty.create("chain_up"),
			BooleanProperty.create("chain_north"),
			BooleanProperty.create("chain_south"),
			BooleanProperty.create("chain_west"),
			BooleanProperty.create("chain_east")
	};

	public WoodPostBlock(ZetaModule module, Block parent, String prefix, boolean nether) {
		super(Quark.ZETA.registryUtil.inherit(parent, s -> prefix + s.replace("_fence", "_post")),
				module, 
				Properties.copy(parent).sound(nether ? SoundType.STEM : SoundType.WOOD));

		BlockState state = stateDefinition.any().setValue(WATERLOGGED, false).setValue(AXIS, Axis.Y);
		for(BooleanProperty prop : CHAINED)
			state = state.setValue(prop, false);
		registerDefaultState(state);

		module.zeta.renderLayerRegistry.put(this, RenderLayerRegistry.Layer.CUTOUT);
		setCreativeTab(CreativeModeTabs.BUILDING_BLOCKS, parent, true);
	}

	@NotNull
	@Override
	public VoxelShape getShape(BlockState state, @NotNull BlockGetter worldIn, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return switch (state.getValue(AXIS)) {
			case X -> SHAPE_X;
			case Y -> SHAPE_Y;
			default -> SHAPE_Z;
		};
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, @NotNull BlockGetter reader, @NotNull BlockPos pos) {
		return !state.getValue(WATERLOGGED);
	}

	@NotNull
	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return getState(context.getLevel(), context.getClickedPos(), context.getClickedFace().getAxis());
	}

	@NotNull
	@Override
	public BlockState updateShape(BlockState state, @NotNull Direction facing, @NotNull BlockState facingState, @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos facingPos) {
		if (state.getValue(WATERLOGGED)) {
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}

		return super.updateShape(state, facing, facingState, level, pos, facingPos);
	}

	@Override
	public void neighborChanged(@NotNull BlockState state, @NotNull Level worldIn, @NotNull BlockPos pos, @NotNull Block blockIn, @NotNull BlockPos fromPos, boolean isMoving) {
		super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);

		BlockState newState = getState(worldIn, pos, state.getValue(AXIS));
		if(!newState.equals(state))
			worldIn.setBlockAndUpdate(pos, newState);
	}

	private BlockState getState(Level world, BlockPos pos, Axis axis) {
		BlockState state = defaultBlockState().setValue(WATERLOGGED, world.getFluidState(pos).getType() == Fluids.WATER).setValue(AXIS, axis);

		for(Direction d : Direction.values()) {
			if(d.getAxis() == axis)
				continue;

			BlockState sideState = world.getBlockState(pos.relative(d));
			if((sideState.getBlock() instanceof ChainBlock && sideState.getValue(BlockStateProperties.AXIS) == d.getAxis())
					|| (d == Direction.DOWN && sideState.getBlock() instanceof LanternBlock && sideState.getValue(LanternBlock.HANGING))) {
				BooleanProperty prop = CHAINED[d.ordinal()];
				state = state.setValue(prop, true);
			}
		}

		return state;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(WATERLOGGED, AXIS);
		for(BooleanProperty prop : CHAINED)
			builder.add(prop);
	}

}
