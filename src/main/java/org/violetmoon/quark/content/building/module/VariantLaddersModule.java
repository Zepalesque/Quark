package org.violetmoon.quark.content.building.module;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedList;
import java.util.List;

import org.violetmoon.quark.base.config.Config;
import org.violetmoon.quark.base.handler.FuelHandler;
import org.violetmoon.quark.base.util.VanillaWoods;
import org.violetmoon.quark.base.util.VanillaWoods.Wood;
import org.violetmoon.quark.content.building.block.VariantLadderBlock;
import org.violetmoon.zeta.event.bus.LoadEvent;
import org.violetmoon.zeta.event.load.ZConfigChanged;
import org.violetmoon.zeta.event.load.ZLoadComplete;
import org.violetmoon.zeta.event.load.ZRegister;
import org.violetmoon.zeta.module.ZetaLoadModule;
import org.violetmoon.zeta.module.ZetaModule;

@ZetaLoadModule(category = "building", antiOverlap = { "woodworks" })
public class VariantLaddersModule extends ZetaModule {

	@Config public static boolean changeNames = true;

	public static List<Block> variantLadders = new LinkedList<>();

	public static boolean moduleEnabled;

	@LoadEvent
	public final void register(ZRegister event) {
		for(Wood type : VanillaWoods.NON_OAK)
			variantLadders.add(new VariantLadderBlock(type.name(), this, !type.nether()));
	}

	@LoadEvent
	public void loadComplete(ZLoadComplete e) {
		variantLadders.forEach(FuelHandler::addWood);
	}

	@LoadEvent
	public final void configChanged(ZConfigChanged event) {
		moduleEnabled = this.enabled;
		zeta.nameChanger.changeBlock(Blocks.LADDER, "block.quark.oak_ladder", changeNames && enabled);
	}

}
