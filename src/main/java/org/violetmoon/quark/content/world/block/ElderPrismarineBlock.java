package org.violetmoon.quark.content.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.violetmoon.zeta.block.ZetaBlock;
import org.violetmoon.zeta.module.ZetaModule;

public class ElderPrismarineBlock extends ZetaBlock {

	public ElderPrismarineBlock(String regname, ZetaModule module, Properties properties) {
		super(regname, module, properties);
	}
	
	@Override
	public boolean isConduitFrameZeta(BlockState state, LevelReader world, BlockPos pos, BlockPos conduit) {
		return true;
	}

}
