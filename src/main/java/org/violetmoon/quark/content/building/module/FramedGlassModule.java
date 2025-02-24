package org.violetmoon.quark.content.building.module;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

import org.violetmoon.zeta.block.IZetaBlock;
import org.violetmoon.zeta.block.ZetaGlassBlock;
import org.violetmoon.zeta.block.ZetaInheritedPaneBlock;
import org.violetmoon.zeta.event.bus.LoadEvent;
import org.violetmoon.zeta.event.load.ZRegister;
import org.violetmoon.zeta.module.ZetaLoadModule;
import org.violetmoon.zeta.module.ZetaModule;

@ZetaLoadModule(category = "building")
public class FramedGlassModule extends ZetaModule {

	@LoadEvent
	public final void register(ZRegister event) {
		Block.Properties props = Block.Properties.of()
				.strength(3F, 10F)
				.sound(SoundType.GLASS);
		
		new ZetaInheritedPaneBlock((IZetaBlock) new ZetaGlassBlock("framed_glass", this, false, props).setCreativeTab(CreativeModeTabs.BUILDING_BLOCKS))
			.setCreativeTab(CreativeModeTabs.BUILDING_BLOCKS);
		
		for(DyeColor dye : DyeColor.values())
			new ZetaInheritedPaneBlock((IZetaBlock) new ZetaGlassBlock(dye.getName() + "_framed_glass", this, true, props).setCreativeTab(CreativeModeTabs.COLORED_BLOCKS))
				.setCreativeTab(CreativeModeTabs.COLORED_BLOCKS);
	}

}
