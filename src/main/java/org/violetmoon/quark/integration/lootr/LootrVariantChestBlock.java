package org.violetmoon.quark.integration.lootr;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fml.ModList;
import noobanidus.mods.lootr.LootrTags;
import noobanidus.mods.lootr.block.entities.LootrChestBlockEntity;
import noobanidus.mods.lootr.config.ConfigManager;
import noobanidus.mods.lootr.util.ChestUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.violetmoon.quark.base.Quark;
import org.violetmoon.quark.content.building.block.VariantChestBlock;
import org.violetmoon.quark.content.building.module.VariantChestsModule;
import org.violetmoon.zeta.item.ZetaBlockItem;
import org.violetmoon.zeta.module.ZetaModule;
import org.violetmoon.zeta.registry.IZetaBlockItemProvider;
import org.violetmoon.zeta.registry.IZetaItemPropertiesFiller;

import java.util.function.Supplier;

/**
 * Copy of https://github.com/noobanidus/Lootr/blob/ded29b761ebf271f53a1b976cf859e0f4bfc8d60/src/main/java/noobanidus/mods/lootr/block/LootrChestBlock.java
 * All modifications are made purely to integrate with VariantChestBlock/quark
 */
public class LootrVariantChestBlock extends VariantChestBlock implements IZetaItemPropertiesFiller, IZetaBlockItemProvider {
	public LootrVariantChestBlock(String type, ZetaModule module, Supplier<BlockEntityType<? extends ChestBlockEntity>> supplier, Properties properties) {
		super("lootr", type, module, supplier, properties.strength(2.5f));
	}

	// BEGIN LOOTR COPY

	@Override
	public float getExplosionResistance() {
		if (ConfigManager.BLAST_IMMUNE.get()) {
			return Float.MAX_VALUE;
		} else if (ConfigManager.BLAST_RESISTANT.get()) {
			return 16.0f;
		} else {
			return super.getExplosionResistance();
		}
	}

	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult trace) {
		if (player.isShiftKeyDown()) {
			ChestUtil.handleLootSneak(this, world, pos, player);
		} else if (!ChestBlock.isChestBlockedAt(world, pos)) {
			ChestUtil.handleLootChest(this, world, pos, player);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LootrVariantChestBlockEntity(pos, state); // Modified
	}

	@Override
	public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {
		if (stateIn.getValue(WATERLOGGED)) {
			worldIn.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(worldIn));
		}

		return stateIn;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
		return AABB;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction direction = context.getHorizontalDirection().getOpposite();
		FluidState fluidstate = context.getLevel().getFluidState(context.getClickedPos());
		return this.defaultBlockState().setValue(FACING, direction).setValue(TYPE, ChestType.SINGLE).setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	@Nullable
	public MenuProvider getMenuProvider(BlockState state, Level worldIn, BlockPos pos) {
		return null;
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState pState) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState pBlockState, Level pLevel, BlockPos pPos) {
		if (ConfigManager.POWER_COMPARATORS.get()) {
			return 1;
		} else {
			return 0;
		}
	}

	@Override
	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
		return pLevel.isClientSide ? LootrChestBlockEntity::lootrLidAnimateTick : null;
	}

	@Override
	public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
		BlockEntity blockentity = pLevel.getBlockEntity(pPos);
		if (blockentity instanceof LootrChestBlockEntity) {
			((LootrChestBlockEntity) blockentity).recheckOpen();
		}

	}

	// END LOOTR COPY

	@Override
	public void fillItemProperties(Item.Properties props) {
		//fixme
		//props.tab(null);
	}

	@Override
	public BlockItem provideItemBlock(Block block, Item.Properties props) {
		return new Item(block, props, false);
	}

	public static class Item extends ZetaBlockItem {

		private final boolean trap;

		public Item(Block block, Properties props, boolean trap) {
			super(block, props);
			this.trap = trap;
		}

		@Override
		public InteractionResult onItemUseFirstZeta(ItemStack stack, UseOnContext context) {
			if (!context.isSecondaryUseActive()) {
				Player player = context.getPlayer();
				Level level = context.getLevel();
				BlockPos pos = context.getClickedPos();
				Block block = getBlock();

				if (player != null && player.isCreative()) {
					BlockState state = level.getBlockState(pos);
					TagKey<Block> key = trap ? LootrTags.Blocks.TRAPPED_CHESTS : LootrTags.Blocks.CHESTS;

					if (state.is(key) && !state.is(block)) {
						BlockEntity entity = level.getBlockEntity(pos);
						CompoundTag nbt = entity == null ? null : entity.serializeNBT();
						level.setBlock(pos, block.withPropertiesOf(state), 18); // Same as debug stick
						level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
						BlockEntity newEntity = level.getBlockEntity(pos);
						if (newEntity != null && nbt != null) newEntity.load(nbt);

						return InteractionResult.SUCCESS;
					}
				}
			}

			return super.onItemUseFirstZeta(stack, context);
		}
	}
}
