package org.violetmoon.quark.addons.oddities.block.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.violetmoon.quark.addons.oddities.block.be.PipeBlockEntity;
import org.violetmoon.quark.addons.oddities.module.PipesModule;
import org.violetmoon.zeta.block.ZetaBlock;
import org.violetmoon.zeta.module.ZetaModule;
import org.violetmoon.zeta.registry.RenderLayerRegistry;

import org.jetbrains.annotations.NotNull;
import java.util.HashSet;
import java.util.Set;

import static org.violetmoon.quark.base.handler.MiscUtil.directionProperty;

public abstract class BasePipeBlock extends ZetaBlock implements EntityBlock {

	protected static final BooleanProperty DOWN = BlockStateProperties.DOWN;
	protected static final BooleanProperty UP = BlockStateProperties.UP;
	protected static final BooleanProperty NORTH = BlockStateProperties.NORTH;
	protected static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
	protected static final BooleanProperty WEST = BlockStateProperties.WEST;
	protected static final BooleanProperty EAST = BlockStateProperties.EAST;

	protected static BooleanProperty property(Direction direction) {
		return switch (direction) {
			case DOWN -> DOWN;
			case UP -> UP;
			case NORTH -> NORTH;
			case SOUTH -> SOUTH;
			case WEST -> WEST;
			case EAST -> EAST;
		};
	}

	protected BasePipeBlock(String name, ZetaModule module) {
		super(name, module,
				Block.Properties.of()
						.instrument(NoteBlockInstrument.HAT)
						.strength(3F, 10F)
						.sound(SoundType.GLASS)
						.noOcclusion()
		);

		registerDefaultState(getDefaultPipeState());
		setCreativeTab(CreativeModeTabs.REDSTONE_BLOCKS);
		module.zeta.renderLayerRegistry.put(this, RenderLayerRegistry.Layer.CUTOUT);
	}

	public BlockState getDefaultPipeState() {
		return defaultBlockState()
				.setValue(DOWN, false).setValue(UP, false)
				.setValue(NORTH, false).setValue(SOUTH, false)
				.setValue(WEST, false).setValue(EAST, false);
	}

	boolean isPipeWaterlogged(BlockState state) {
		return false;
	}

	public boolean allowsFullConnection(PipeBlockEntity.ConnectionType conn) {
		return conn.isSolid;
	}

	@NotNull
	@Override
	public InteractionResult use(@NotNull BlockState state, @NotNull Level worldIn, @NotNull BlockPos pos, Player player, @NotNull InteractionHand handIn, @NotNull BlockHitResult hit) {
		ItemStack stack = player.getItemInHand(handIn);

		// fix pipes if they're ruined
		if(stack.getItem() == Items.STICK) {
			Set<BlockPos> found = new HashSet<>();
			boolean fixedAny = false;

			Set<BlockPos> candidates = new HashSet<>();
			Set<BlockPos> newCandidates = new HashSet<>();

			candidates.add(pos);
			do {
				for(BlockPos cand : candidates) {
					for(Direction d : Direction.values()) {
						BlockPos offPos = cand.relative(d);
						BlockState offState = worldIn.getBlockState(offPos);
						if(offState.getBlock() == this && !candidates.contains(offPos) && !found.contains(offPos))
							newCandidates.add(offPos);
					}

					BlockState curr = worldIn.getBlockState(cand);
					BlockState target = getTargetState(worldIn, cand);
					if(!target.equals(curr)) {
						fixedAny = true;
						worldIn.setBlock(cand, target, 2 | 4);
					}
				}

				found.addAll(candidates);
				candidates = newCandidates;
				newCandidates = new HashSet<>();
			} while(!candidates.isEmpty());

			if(fixedAny)
				return InteractionResult.SUCCESS;
		}

		return super.use(state, worldIn, pos, player, handIn, hit);
	}

	@SuppressWarnings("deprecation") //Don't need the Forge extension
	@Override
	public BlockState rotate(BlockState state, Rotation direction) {
		BlockState newState = state;
		for (Direction dir : Direction.values())
			newState = newState.setValue(property(dir), state.getValue(property(direction.rotate(dir))));
		return newState;
	}

	@NotNull
	@Override
	public BlockState mirror(@NotNull BlockState state, @NotNull Mirror mirror) {
		BlockState newState = state;
		for (Direction dir : Direction.values())
			newState = newState.setValue(property(dir), state.getValue(property(mirror.mirror(dir))));
		return newState;
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if(level.getBlockEntity(pos) instanceof PipeBlockEntity tile){
			tile.refreshAllConnections();
		}
		return getTargetState(level, pos);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return getTargetState(context.getLevel(), context.getClickedPos());
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
		if(level.getBlockEntity(pos) instanceof PipeBlockEntity tile){
			tile.refreshAllConnections();
		}
		super.setPlacedBy(level, pos, state, entity, stack);
	}

	protected BlockState getTargetState(LevelAccessor level, BlockPos pos) {
		BlockState newState = defaultBlockState();

		for(Direction facing : Direction.values()) {
			PipeBlockEntity.ConnectionType type = PipeBlockEntity.computeConnectionTo(level, pos, facing);

			newState = newState.setValue(directionProperty(facing), allowsFullConnection(type));
		}

		return newState;
	}

	public static boolean isConnected(BlockState state, Direction side) {
		return state.getValue(directionProperty(side));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(UP, DOWN, NORTH, SOUTH, WEST, EAST);
	}

	@Override
	public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(@NotNull BlockState blockState, Level worldIn, @NotNull BlockPos pos) {
		BlockEntity tile = worldIn.getBlockEntity(pos);
		if(tile instanceof PipeBlockEntity pipe)
			return pipe.getComparatorOutput();
		return 0;
	}

	@Override
	public void onRemove(@NotNull BlockState state, Level worldIn, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
		BlockEntity be = worldIn.getBlockEntity(pos);

		if(be instanceof PipeBlockEntity pipe)
			pipe.dropAllItems();

		super.onRemove(state, worldIn, pos, newState, isMoving);
	}

	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return new PipeBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level world, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
		return createTickerHelper(type, PipesModule.blockEntityType, PipeBlockEntity::tick);
	}
}
