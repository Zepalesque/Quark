package org.violetmoon.quark.content.building.module;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import org.violetmoon.quark.content.building.block.MudBrickLatticeBlock;
import org.violetmoon.zeta.block.ZetaBlock;
import org.violetmoon.zeta.block.ZetaPillarBlock;
import org.violetmoon.zeta.event.bus.LoadEvent;
import org.violetmoon.zeta.event.load.ZRegister;
import org.violetmoon.zeta.module.ZetaLoadModule;
import org.violetmoon.zeta.module.ZetaModule;

@ZetaLoadModule(category = "building")
public class MoreMudBlocksModule extends ZetaModule {

	@LoadEvent
	public final void register(ZRegister event) {
		BlockBehaviour.Properties props = Properties.copy(Blocks.MUD_BRICKS);
		
		new ZetaBlock("carved_mud_bricks", this, props).setCreativeTab(CreativeModeTabs.BUILDING_BLOCKS);
		new ZetaPillarBlock("mud_pillar", this, props).setCreativeTab(CreativeModeTabs.BUILDING_BLOCKS);
		new MudBrickLatticeBlock(this, props);
	}
	
}
