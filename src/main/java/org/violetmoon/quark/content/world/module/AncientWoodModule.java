package org.violetmoon.quark.content.world.module;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import org.violetmoon.quark.base.Quark;
import org.violetmoon.quark.base.config.Config;
import org.violetmoon.quark.base.config.Config.Min;
import org.violetmoon.quark.base.handler.GeneralConfig;
import org.violetmoon.quark.base.handler.WoodSetHandler;
import org.violetmoon.quark.base.handler.WoodSetHandler.WoodSet;
import org.violetmoon.quark.content.world.block.AncientSaplingBlock;
import org.violetmoon.quark.content.world.item.AncientFruitItem;
import org.violetmoon.zeta.advancement.ManualTrigger;
import org.violetmoon.zeta.advancement.modifier.BalancedDietModifier;
import org.violetmoon.zeta.block.ZetaLeavesBlock;
import org.violetmoon.zeta.event.bus.LoadEvent;
import org.violetmoon.zeta.event.bus.PlayEvent;
import org.violetmoon.zeta.event.load.ZCommonSetup;
import org.violetmoon.zeta.event.load.ZRegister;
import org.violetmoon.zeta.event.play.loading.ZLootTableLoad;
import org.violetmoon.zeta.module.ZetaLoadModule;
import org.violetmoon.zeta.module.ZetaModule;
import org.violetmoon.zeta.util.Hint;

@ZetaLoadModule(category = "world")
public class AncientWoodModule extends ZetaModule {

	@Config(flag = "ancient_fruit_xp")
	public static boolean ancientFruitGivesExp = true;

	@Config
	@Min(1)
	public static int ancientFruitExpValue = 10;

	@Config(description = "Set to 0 to disable loot chest generation")
	@Min(0)
	public static int ancientCityLootWeight = 8;

	@Config
	@Min(0)
	public static int ancientCityLootQuality = 1;

	public static WoodSet woodSet;
	public static Block ancient_leaves;
	@Hint public static Block ancient_sapling;
	@Hint public static Item ancient_fruit;

	public static ManualTrigger ancientFruitTrigger;

	@LoadEvent
	public void setup(ZCommonSetup e) {
		e.enqueueWork(() -> {
			ComposterBlock.COMPOSTABLES.put(ancient_sapling.asItem(), 0.3F);
			ComposterBlock.COMPOSTABLES.put(ancient_leaves.asItem(), 0.3F);
			ComposterBlock.COMPOSTABLES.put(ancient_fruit.asItem(), 0.65F);
		});
	}

	@LoadEvent
	public void register(ZRegister event) {
		woodSet = WoodSetHandler.addWoodSet(event, this, "ancient", MapColor.TERRACOTTA_WHITE, MapColor.TERRACOTTA_WHITE, true);
		ancient_leaves = new ZetaLeavesBlock(woodSet.name, this, MapColor.PLANT);
		ancient_sapling = new AncientSaplingBlock(this);
		ancient_fruit = new AncientFruitItem(this);

		event.getVariantRegistry().addFlowerPot(ancient_sapling, Quark.ZETA.registry.getRegistryName(ancient_sapling, BuiltInRegistries.BLOCK).getPath(), Functions.identity());

		event.getAdvancementModifierRegistry().addModifier(new BalancedDietModifier(this, ImmutableSet.of(ancient_fruit))
			.setCondition(() -> GeneralConfig.enableAdvancementModification));

		ancientFruitTrigger = event.getAdvancementModifierRegistry().registerManualTrigger("ancient_fruit_overlevel");
	}

	@PlayEvent
	public void onLootTableLoad(ZLootTableLoad event) {
		int weight = 0;

		if(event.getName().equals(BuiltInLootTables.ANCIENT_CITY))
			weight = ancientCityLootWeight;

		if(weight > 0) {
			LootPoolEntryContainer entry = LootItem.lootTableItem(ancient_sapling)
					.setWeight(weight)
					.setQuality(ancientCityLootQuality)
					.build();
			event.add(entry);
		}
	}

}
