package org.violetmoon.quark.addons.oddities.block.pipe;

import org.jetbrains.annotations.NotNull;

import org.violetmoon.quark.addons.oddities.block.be.PipeBlockEntity.ConnectionType;
import org.violetmoon.zeta.module.ZetaModule;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EncasedPipeBlock extends BasePipeBlock {

	public EncasedPipeBlock(ZetaModule module) {
		super("encased_pipe", module);
	}
	
	@Override
	public boolean allowsFullConnection(ConnectionType conn) {
		return conn.isFlared || conn.isSolid;
	}
	
	@Override
	public boolean skipRendering(@NotNull BlockState state, BlockState adjacentBlockState, @NotNull Direction side) {
		return adjacentBlockState.is(this) || adjacentBlockState.is(Blocks.GLASS) || super.skipRendering(state, adjacentBlockState, side);
	}

	@Override
	@NotNull
	public VoxelShape getVisualShape(@NotNull BlockState state, @NotNull BlockGetter worldIn, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public float getShadeBrightness(@NotNull BlockState state, @NotNull BlockGetter worldIn, @NotNull BlockPos pos) {
		return 1.0F;
	}

	@Override
	public boolean propagatesSkylightDown(@NotNull BlockState state, @NotNull BlockGetter reader, @NotNull BlockPos pos) {
		return true;
	}

	@Override
	public boolean shouldDisplayFluidOverlay(BlockState state, BlockAndTintGetter world, BlockPos pos, FluidState fluidState) {
		return true;
	}

}
