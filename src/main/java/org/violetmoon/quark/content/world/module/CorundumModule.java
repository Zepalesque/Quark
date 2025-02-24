package org.violetmoon.quark.content.world.module;

import java.util.List;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.apache.commons.lang3.tuple.Pair;
import org.violetmoon.quark.base.Quark;
import org.violetmoon.quark.base.config.Config;
import org.violetmoon.quark.base.handler.ToolInteractionHandler;
import org.violetmoon.quark.base.util.CorundumColor;
import org.violetmoon.quark.content.tools.module.BeaconRedirectionModule;
import org.violetmoon.quark.content.world.block.CorundumBlock;
import org.violetmoon.quark.content.world.block.CorundumClusterBlock;
import org.violetmoon.quark.content.world.undergroundstyle.CorundumStyle;
import org.violetmoon.quark.content.world.undergroundstyle.base.AbstractUndergroundStyleModule;
import org.violetmoon.quark.content.world.undergroundstyle.base.UndergroundStyleConfig;
import org.violetmoon.zeta.api.IIndirectConnector;
import org.violetmoon.zeta.block.ZetaInheritedPaneBlock;
import org.violetmoon.zeta.event.bus.LoadEvent;
import org.violetmoon.zeta.event.bus.PlayEvent;
import org.violetmoon.zeta.event.load.ZCommonSetup;
import org.violetmoon.zeta.event.load.ZConfigChanged;
import org.violetmoon.zeta.event.load.ZRegister;
import org.violetmoon.zeta.event.play.loading.ZGatherHints;
import org.violetmoon.zeta.module.ZetaLoadModule;
import org.violetmoon.zeta.util.Hint;

@ZetaLoadModule(category = "world")
public class CorundumModule extends AbstractUndergroundStyleModule<CorundumStyle> {

	@Config
	@Config.Min(value = 0)
	@Config.Max(value = 1)
	public static double crystalChance = 0.16;

	@Config
	@Config.Min(value = 0)
	@Config.Max(value = 1)
	public static double crystalClusterChance = 0.2;

	@Config
	@Config.Min(value = 0)
	@Config.Max(value = 1)
	public static double crystalClusterOnSidesChance = 0.6;

	@Config
	@Config.Min(value = 0)
	@Config.Max(value = 1)
	public static double doubleCrystalChance = 0.2;

	@Config(description = "The chance that a crystal can grow, this is on average 1 in X world ticks, set to a higher value to make them grow slower. Minimum is 1, for every tick. Set to 0 to disable growth.")
	public static int caveCrystalGrowthChance = 5;

	@Config(flag = "cave_corundum_runes")
	public static boolean crystalsCraftRunes = true;

	@Config public static boolean enableCollateralMovement = true;

	public static boolean staticEnabled;

	public static List<CorundumBlock> crystals = Lists.newArrayList();
	public static List<CorundumClusterBlock> clusters = Lists.newArrayList();
	@Hint public static TagKey<Block> corundumTag;

	@LoadEvent
	public final void register(ZRegister event) {
		for (CorundumColor color : CorundumColor.values())
			add(color.name, color.beaconColor, color.mapColor);
	}

	@LoadEvent
	public final void configChanged(ZConfigChanged event) {
		staticEnabled = enabled;
	}

	@LoadEvent
	public final void mySetup(ZCommonSetup event) {
		corundumTag = BlockTags.create(new ResourceLocation(Quark.MOD_ID, "corundum"));
	}

	@PlayEvent
	public void addAdditionalHints(ZGatherHints consumer) {
		MutableComponent comp = Component.translatable("quark.jei.hint.corundum_cluster_grow");

		if(Quark.ZETA.modules.isEnabled(BeaconRedirectionModule.class))
			comp = comp.append(" ").append(Component.translatable("quark.jei.hint.corundum_cluster_redirect"));

		for(Block block : clusters)
			consumer.accept(block.asItem(), comp);
	}

	private void add(String name, int color, MapColor mapColor) {
		CorundumBlock crystal = new CorundumBlock(name + "_corundum", color, this, mapColor, false);
		crystals.add(crystal);

		CorundumBlock waxed = new CorundumBlock("waxed_" + name + "_corundum", color, this, mapColor, true);
		ToolInteractionHandler.registerWaxedBlock(this, crystal, waxed);

		new ZetaInheritedPaneBlock(crystal);
		CorundumClusterBlock cluster = new CorundumClusterBlock(crystal);
		clusters.add(cluster);

		ClusterConnection connection = new ClusterConnection(cluster);
		IIndirectConnector.INDIRECT_STICKY_BLOCKS.add(Pair.of(connection::isValidState, connection));
	}

	@Override
	protected String getStyleName() {
		return "corundum";
	}

	@Override
	protected UndergroundStyleConfig<CorundumStyle> getStyleConfig() {
		return new UndergroundStyleConfig<>(new CorundumStyle(), 400, true, BiomeTags.IS_OCEAN).setDefaultSize(72, 20, 22, 4);
	}

	public record ClusterConnection(CorundumClusterBlock cluster) implements IIndirectConnector {

		@Override
		public boolean isEnabled() {
			return enableCollateralMovement;
		}

		private boolean isValidState(BlockState state) {
			return state.getBlock() == cluster;
		}

		@Override
		public boolean canConnectIndirectly(Level world, BlockPos ourPos, BlockPos sourcePos, BlockState ourState, BlockState sourceState) {
			BlockPos offsetPos = ourPos.relative(ourState.getValue(CorundumClusterBlock.FACING).getOpposite());
			if (!offsetPos.equals(sourcePos))
				return false;

			return sourceState.getBlock() == cluster.base;
		}

	}

}
