package org.violetmoon.quark.base.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.PressurePlateBlock.Sensitivity;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.ToolActions;
import org.violetmoon.quark.base.Quark;
import org.violetmoon.zeta.block.OldMaterials;
import org.violetmoon.zeta.block.ZetaTrapdoorBlock;
import org.violetmoon.quark.base.client.render.QuarkBoatRenderer;
import org.violetmoon.quark.base.item.boat.QuarkBoat;
import org.violetmoon.quark.base.item.boat.QuarkBoatDispenseItemBehavior;
import org.violetmoon.quark.base.item.boat.QuarkBoatItem;
import org.violetmoon.quark.base.item.boat.QuarkChestBoat;
import org.violetmoon.quark.content.building.block.HollowLogBlock;
import org.violetmoon.quark.content.building.block.VariantBookshelfBlock;
import org.violetmoon.quark.content.building.block.VariantLadderBlock;
import org.violetmoon.quark.content.building.block.WoodPostBlock;
import org.violetmoon.quark.content.building.module.HollowLogsModule;
import org.violetmoon.quark.content.building.module.VariantBookshelvesModule;
import org.violetmoon.quark.content.building.module.VariantChestsModule;
import org.violetmoon.quark.content.building.module.VariantLaddersModule;
import org.violetmoon.quark.content.building.module.VerticalPlanksModule;
import org.violetmoon.quark.content.building.module.WoodenPostsModule;
import org.violetmoon.zeta.block.IZetaBlock;
import org.violetmoon.zeta.block.ZetaBlock;
import org.violetmoon.zeta.block.ZetaDoorBlock;
import org.violetmoon.zeta.block.ZetaFenceBlock;
import org.violetmoon.zeta.block.ZetaFenceGateBlock;
import org.violetmoon.zeta.block.ZetaPillarBlock;
import org.violetmoon.zeta.block.ZetaPressurePlateBlock;
import org.violetmoon.zeta.block.ZetaStandingSignBlock;
import org.violetmoon.zeta.block.ZetaWallSignBlock;
import org.violetmoon.zeta.block.ZetaWoodenButtonBlock;
import org.violetmoon.zeta.client.event.load.ZClientSetup;
import org.violetmoon.zeta.event.bus.LoadEvent;
import org.violetmoon.zeta.event.load.ZCommonSetup;
import org.violetmoon.zeta.event.load.ZRegister;
import org.violetmoon.zeta.item.ZetaSignItem;
import org.violetmoon.zeta.module.ZetaModule;

public class WoodSetHandler {

	public record QuarkBoatType(String name, Item boat, Item chestBoat, Block planks) {}
	private static final Map<String, QuarkBoatType> quarkBoatTypes = new HashMap<>();

	public static EntityType<QuarkBoat> quarkBoatEntityType = null;
	public static EntityType<QuarkChestBoat> quarkChestBoatEntityType = null;

	private static final List<WoodSet> woodSets = new ArrayList<>();

	@LoadEvent
	public static void register(ZRegister event) {
		quarkBoatEntityType = EntityType.Builder.<QuarkBoat>of(QuarkBoat::new, MobCategory.MISC)
				.sized(1.375F, 0.5625F)
				.clientTrackingRange(10)
				.setCustomClientFactory((spawnEntity, world) -> new QuarkBoat(quarkBoatEntityType, world))
				.build("quark_boat");

		quarkChestBoatEntityType = EntityType.Builder.<QuarkChestBoat>of(QuarkChestBoat::new, MobCategory.MISC)
				.sized(1.375F, 0.5625F)
				.clientTrackingRange(10)
				.setCustomClientFactory((spawnEntity, world) -> new QuarkChestBoat(quarkChestBoatEntityType, world))
				.build("quark_chest_boat");

		Quark.ZETA.registry.register(quarkBoatEntityType, "quark_boat", Registries.ENTITY_TYPE);
		Quark.ZETA.registry.register(quarkChestBoatEntityType, "quark_chest_boat", Registries.ENTITY_TYPE);
	}

	@LoadEvent
	public static void setup(ZCommonSetup event) {
		event.enqueueWork(() -> {
			Map<Item, DispenseItemBehavior> registry = DispenserBlock.DISPENSER_REGISTRY;
			for(WoodSet set : woodSets) {
				registry.put(set.boatItem, new QuarkBoatDispenseItemBehavior(set.name, false));
				registry.put(set.chestBoatItem, new QuarkBoatDispenseItemBehavior(set.name, true));
			}
		});
	}

	public static WoodSet addWoodSet(ZRegister event, ZetaModule module, String name, MapColor color, MapColor barkColor, boolean flammable) {
		return addWoodSet(event, module, name, color, barkColor, true, true, flammable);
	}

	public static WoodSet addWoodSet(ZRegister event, ZetaModule module, String name, MapColor color, MapColor barkColor, boolean hasLog, boolean hasBoat, boolean flammable) {
		//TODO 1.20: maybe expose stuff like canOpenByHand, sound types, etc
		BlockSetType setType = new BlockSetType(Quark.MOD_ID + ":" + name);

		WoodType type = WoodType.register(new WoodType(Quark.MOD_ID + ":" + name, setType));
		WoodSet set = new WoodSet(name, module, type);

		if(hasLog) {
			set.log = log(name + "_log", module, color, barkColor);
			set.wood = new ZetaPillarBlock(name + "_wood", module, OldMaterials.wood().mapColor(barkColor).strength(2.0F).sound(SoundType.WOOD)).setCreativeTab(CreativeModeTabs.BUILDING_BLOCKS);
			set.strippedLog = log("stripped_" + name + "_log", module, color, color);
			set.strippedWood = new ZetaPillarBlock("stripped_" + name + "_wood", module, OldMaterials.wood().mapColor(color).strength(2.0F).sound(SoundType.WOOD)).setCreativeTab(CreativeModeTabs.BUILDING_BLOCKS);
		}

		set.planks = new ZetaBlock(name + "_planks", module, OldMaterials.wood().mapColor(color).strength(2.0F, 3.0F).sound(SoundType.WOOD)).setCreativeTab(CreativeModeTabs.BUILDING_BLOCKS);

		set.slab = event.getVariantRegistry().addSlab((IZetaBlock) set.planks, null).getBlock();
		set.stairs = event.getVariantRegistry().addStairs((IZetaBlock) set.planks, null).getBlock();
		set.fence = new ZetaFenceBlock(name + "_fence", module, OldMaterials.wood().mapColor(color).strength(2.0F, 3.0F).sound(SoundType.WOOD));
		set.fenceGate = new ZetaFenceGateBlock(name + "_fence_gate", module, OldMaterials.wood().mapColor(color).strength(2.0F, 3.0F).sound(SoundType.WOOD));

		set.door = new ZetaDoorBlock(setType, name + "_door", module, OldMaterials.wood().mapColor(color).strength(3.0F).sound(SoundType.WOOD).noOcclusion());
		set.trapdoor = new ZetaTrapdoorBlock(setType, name + "_trapdoor", module, OldMaterials.wood().mapColor(color).strength(3.0F).sound(SoundType.WOOD).noOcclusion().isValidSpawn((s, g, p, e) -> false));

		set.button = new ZetaWoodenButtonBlock(setType, name + "_button", module, OldMaterials.decoration().noCollission().strength(0.5F).sound(SoundType.WOOD));
		set.pressurePlate = new ZetaPressurePlateBlock(Sensitivity.EVERYTHING, name + "_pressure_plate", module, "REDSTONE", OldMaterials.wood().mapColor(color).noCollission().strength(0.5F).sound(SoundType.WOOD), setType);

		set.sign = new ZetaStandingSignBlock(name + "_sign", module, type, OldMaterials.wood().mapColor(color).noCollission().strength(1.0F).sound(SoundType.WOOD));
		set.wallSign = new ZetaWallSignBlock(name + "_wall_sign", module, type, OldMaterials.wood().mapColor(color).noCollission().strength(1.0F).sound(SoundType.WOOD).lootFrom(() -> set.sign));

		set.bookshelf = new VariantBookshelfBlock(name, module, true).setCondition(() -> Quark.ZETA.modules.isEnabledOrOverlapping(VariantBookshelvesModule.class));
		set.ladder = new VariantLadderBlock(name, module, true).setCondition(() -> Quark.ZETA.modules.isEnabledOrOverlapping(VariantLaddersModule.class));

		set.post = new WoodPostBlock(module, set.fence, "", false).setCondition(() -> Quark.ZETA.modules.isEnabledOrOverlapping(WoodenPostsModule.class));
		set.strippedPost = new WoodPostBlock(module, set.fence, "stripped_", false).setCondition(() -> Quark.ZETA.modules.isEnabledOrOverlapping(WoodenPostsModule.class));

		set.verticalPlanks = VerticalPlanksModule.add(name, set.planks, module).setCondition(() -> Quark.ZETA.modules.isEnabledOrOverlapping(VerticalPlanksModule.class));

		if(hasLog)
			set.hollowLog = new HollowLogBlock(set.log, module, flammable).setCondition(() -> Quark.ZETA.modules.isEnabledOrOverlapping(HollowLogsModule.class));

		VariantChestsModule.makeChestBlocksExternal(module, name, Blocks.CHEST, () -> true);

		set.signItem = new ZetaSignItem(module, set.sign, set.wallSign);

		if(hasBoat) {
			set.boatItem = new QuarkBoatItem(name, module, false);
			set.chestBoatItem = new QuarkBoatItem(name, module, true);
		}

		makeSignWork(set.sign, set.wallSign);

		if(hasLog) {
			ToolInteractionHandler.registerInteraction(ToolActions.AXE_STRIP, set.log, set.strippedLog);
			ToolInteractionHandler.registerInteraction(ToolActions.AXE_STRIP, set.wood, set.strippedWood);
		}
		ToolInteractionHandler.registerInteraction(ToolActions.AXE_STRIP, set.post, set.strippedPost);

		VariantLaddersModule.variantLadders.add(set.ladder);

		if(hasBoat) {
			FuelHandler.addFuel(set.boatItem, 60 * 20);
			FuelHandler.addFuel(set.chestBoatItem, 60 * 20);

			addQuarkBoatType(name, new QuarkBoatType(name, set.boatItem, set.chestBoatItem, set.planks));
		}

		woodSets.add(set);

		return set;
	}

	public static void makeSignWork(Block sign, Block wallSign) {
		Set<Block> validBlocks = new HashSet<>();
		validBlocks.add(sign);
		validBlocks.add(wallSign);
		validBlocks.addAll(BlockEntityType.SIGN.validBlocks);
		BlockEntityType.SIGN.validBlocks = ImmutableSet.copyOf(validBlocks);
	}

	private static RotatedPillarBlock log(String name, ZetaModule module, MapColor topColor, MapColor sideColor) {
		return (RotatedPillarBlock) new ZetaPillarBlock(name, module,
			OldMaterials.wood()
				.mapColor(s -> s.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y ? topColor : sideColor)
				.strength(2.0F).sound(SoundType.WOOD))
				.setCreativeTab(CreativeModeTabs.BUILDING_BLOCKS);
	}

	public static void addQuarkBoatType(String name, QuarkBoatType type) {
		quarkBoatTypes.put(name, type);
	}

	public static QuarkBoatType getQuarkBoatType(String name) {
		return quarkBoatTypes.get(name);
	}

	public static Stream<String> boatTypes() {
		return quarkBoatTypes.keySet().stream();
	}

	public static class WoodSet {

		public final String name;
		public final WoodType type;
		public final ZetaModule module;

		public Block log, wood, planks, strippedLog, strippedWood,
		slab, stairs, fence, fenceGate,
		door, trapdoor, button, pressurePlate, sign, wallSign,
		bookshelf, ladder, post, strippedPost, verticalPlanks,
		hollowLog;

		public Item signItem, boatItem, chestBoatItem;

		public WoodSet(String name, ZetaModule module, WoodType type) {
			this.name = name;
			this.module = module;
			this.type = type;
		}

	}

	public static class Client {
		@LoadEvent
		public static void clientSetup(ZClientSetup event) {
			EntityRenderers.register(quarkBoatEntityType, r -> new QuarkBoatRenderer(r, false));
			EntityRenderers.register(quarkChestBoatEntityType, r -> new QuarkBoatRenderer(r, true));

			event.enqueueWork(() -> {
				for (WoodSet set : woodSets) {
					Sheets.addWoodType(set.type);
				}
			});
		}
	}
}
