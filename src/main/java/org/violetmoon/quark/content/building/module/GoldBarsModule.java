package org.violetmoon.quark.content.building.module;

import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import org.violetmoon.quark.base.config.Config;
import org.violetmoon.quark.base.handler.StructureBlockReplacementHandler;
import org.violetmoon.quark.base.handler.StructureBlockReplacementHandler.StructureHolder;
import org.violetmoon.zeta.block.ZetaPaneBlock;
import org.violetmoon.zeta.event.bus.LoadEvent;
import org.violetmoon.zeta.event.load.ZConfigChanged;
import org.violetmoon.zeta.event.load.ZRegister;
import org.violetmoon.zeta.module.ZetaLoadModule;
import org.violetmoon.zeta.module.ZetaModule;
import org.violetmoon.zeta.registry.RenderLayerRegistry;

@ZetaLoadModule(category = "building")
public class GoldBarsModule extends ZetaModule {

	@Config public static boolean generateInNetherFortress = true;

	public static boolean staticEnabled;

	public static Block gold_bars;

	@LoadEvent
	public final void register(ZRegister event) {
		gold_bars = new ZetaPaneBlock("gold_bars", this, Properties.copy(Blocks.IRON_BARS), RenderLayerRegistry.Layer.CUTOUT);

		StructureBlockReplacementHandler.addReplacement(GoldBarsModule::getGenerationBarBlockState);
	}

	@LoadEvent
	public final void configChanged(ZConfigChanged event) {
		staticEnabled = enabled;
	}

	private static BlockState getGenerationBarBlockState(ServerLevelAccessor accessor, BlockState current, StructureHolder structure) {
		if(staticEnabled && generateInNetherFortress 
				&& current.getBlock() == Blocks.NETHER_BRICK_FENCE 
				&& StructureBlockReplacementHandler.isStructure(accessor, structure, BuiltinStructures.FORTRESS)) {
			
			return gold_bars.withPropertiesOf(current);
		}

		return null; // no change
	}


}
