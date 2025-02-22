package org.violetmoon.quark.integration.jei;

import com.google.common.collect.Sets;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import org.violetmoon.quark.addons.oddities.block.be.MatrixEnchantingTableBlockEntity;
import org.violetmoon.quark.addons.oddities.client.screen.BackpackInventoryScreen;
import org.violetmoon.quark.addons.oddities.client.screen.CrateScreen;
import org.violetmoon.quark.addons.oddities.module.MatrixEnchantingModule;
import org.violetmoon.quark.addons.oddities.util.Influence;
import org.violetmoon.quark.base.Quark;
import org.violetmoon.quark.base.handler.GeneralConfig;
import org.violetmoon.quark.base.util.registryaccess.RegistryAccessUtil;
import org.violetmoon.quark.content.building.module.VariantFurnacesModule;
import org.violetmoon.quark.content.client.module.ImprovedTooltipsModule;
import org.violetmoon.quark.content.client.tooltip.EnchantedBookTooltips;
import org.violetmoon.quark.content.tools.item.AncientTomeItem;
import org.violetmoon.quark.content.tools.module.AncientTomesModule;
import org.violetmoon.quark.content.tools.module.ColorRunesModule;
import org.violetmoon.quark.content.tools.module.PickarangModule;
import org.violetmoon.quark.content.tweaks.module.DiamondRepairModule;
import org.violetmoon.quark.content.tweaks.recipe.ElytraDuplicationRecipe;
import org.violetmoon.quark.content.tweaks.recipe.SlabToBlockRecipe;
import org.violetmoon.zeta.event.play.loading.ZGatherHints;
import org.violetmoon.zeta.module.IDisableable;
import org.violetmoon.zeta.util.ItemNBTHelper;
import org.violetmoon.zeta.util.RegistryUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JeiPlugin
public class QuarkJeiPlugin implements IModPlugin {
	private static final ResourceLocation UID = new ResourceLocation(Quark.MOD_ID, Quark.MOD_ID);

	public static final RecipeType<InfluenceEntry> INFLUENCING =
			RecipeType.create(Quark.MOD_ID, "influence", InfluenceEntry.class);

	@NotNull
	@Override
	public ResourceLocation getPluginUid() {
		return UID;
	}

	@Override
	public void registerItemSubtypes(@NotNull ISubtypeRegistration registration) {
		registration.useNbtForSubtypes(AncientTomesModule.ancient_tome);
	}

	@Override
	public void onRuntimeAvailable(@NotNull final IJeiRuntime jeiRuntime) {
		List<ItemStack> disabledItems = Quark.ZETA.requiredModTooltipHandler.disabledItems();
		if (!disabledItems.isEmpty())
			jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, disabledItems);

		Quark.ZETA.configManager.setJeiReloadListener(configInternals -> {
			if(Quark.ZETA.modules.isEnabled(DiamondRepairModule.class))
				Minecraft.getInstance().submitAsync(() -> hideAnvilRepairRecipes(jeiRuntime.getRecipeManager()));

			if(!GeneralConfig.hideDisabledContent)
				return;

			Set<Potion> hidePotions = Sets.newHashSet();
			for (Potion potion : BuiltInRegistries.POTION) {
				ResourceLocation loc = BuiltInRegistries.POTION.getKey(potion);
				if (loc != null && loc.getNamespace().equals("quark")) {
					if (!Quark.ZETA.brewingRegistry.isEnabled(potion)) {
						hidePotions.add(potion);
					}
				}
			}

			NonNullList<ItemStack> stacksToHide = NonNullList.create();
			for (Item item : BuiltInRegistries.ITEM) {
				ResourceLocation loc = BuiltInRegistries.ITEM.getKey(item);
				if (loc.getNamespace().equals("quark") && !IDisableable.isEnabled(item)) {
					//TODO 1.20: this just enumerated the item's variants
					//item.fillItemCategory(CreativeModeTab.TAB_SEARCH, stacksToHide);
				}

				if (item instanceof PotionItem || item instanceof TippedArrowItem) {
					NonNullList<ItemStack> potionStacks = NonNullList.create();
					//TODO 1.20: this just enumerated the item's variants
					//item.fillItemCategory(CreativeModeTab.TAB_SEARCH, potionStacks);
					potionStacks.stream().filter(it -> hidePotions.contains(PotionUtils.getPotion(it))).forEach(stacksToHide::add);
				}
			}

			if (!stacksToHide.isEmpty())
				Minecraft.getInstance().submitAsync(() -> jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, stacksToHide));
		});
	}

	@Override
	public void registerVanillaCategoryExtensions(@NotNull IVanillaCategoryExtensionRegistration registration) {
		registration.getCraftingCategory().addCategoryExtension(ElytraDuplicationRecipe.class, ElytraDuplicationExtension::new);
		registration.getCraftingCategory().addCategoryExtension(SlabToBlockRecipe.class, SlabToBlockExtension::new);
	}

	private boolean matrix() {
		return Quark.ZETA.modules.isEnabled(MatrixEnchantingModule.class) && MatrixEnchantingModule.allowInfluencing && !MatrixEnchantingModule.candleInfluencingFailed;
	}

	@Override
	public void registerCategories(@NotNull IRecipeCategoryRegistration registration) {
		if (matrix())
			registration.addRecipeCategories(new InfluenceCategory(registration.getJeiHelpers().getGuiHelper()));
	}

	@Override
	public void registerRecipes(@NotNull IRecipeRegistration registration) {
		IVanillaRecipeFactory factory = registration.getVanillaRecipeFactory();

		if (Quark.ZETA.modules.isEnabled(AncientTomesModule.class))
			registerAncientTomeAnvilRecipes(registration, factory);

		if (Quark.ZETA.modules.isEnabled(PickarangModule.class)) {
			registerPickarangAnvilRepairs(PickarangModule.pickarang, Items.DIAMOND, registration, factory);
			registerPickarangAnvilRepairs(PickarangModule.flamerang, Items.NETHERITE_INGOT, registration, factory);
		}

		if (Quark.ZETA.modules.isEnabled(ColorRunesModule.class))
			registerRuneAnvilRecipes(registration, factory);

		if (matrix())
			registerInfluenceRecipes(registration);

		if(Quark.ZETA.modules.isEnabled(DiamondRepairModule.class))
			registerCustomAnvilRecipes(registration, factory);

		if(GeneralConfig.enableJeiItemInfo) {
			MutableComponent externalPreamble = Component.translatable("quark.jei.hint_preamble");
			externalPreamble.setStyle(externalPreamble.getStyle().withColor(0x0b5d4b));

			List<Item> blacklist = RegistryUtil.massRegistryGet(GeneralConfig.suppressedInfo, BuiltInRegistries.ITEM);

			Quark.ZETA.playBus.fire((item, component) -> {
				if(blacklist.contains(item))
					return;

				MutableComponent compound = Component.literal("");
				if(!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(Quark.MOD_ID))
					compound = compound.append(externalPreamble);
				compound = compound.append(component);

				registration.addItemStackInfo(new ItemStack(item), compound);
			}, ZGatherHints.class);
		}
	}

	@Override
	public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
		if(Quark.ZETA.modules.isEnabled(VariantFurnacesModule.class)) {
			registration.addRecipeCatalyst(new ItemStack(VariantFurnacesModule.deepslateFurnace), RecipeTypes.FUELING, RecipeTypes.SMELTING);
			registration.addRecipeCatalyst(new ItemStack(VariantFurnacesModule.blackstoneFurnace), RecipeTypes.FUELING, RecipeTypes.SMELTING);
		}

		if (matrix()) {
			if (MatrixEnchantingModule.automaticallyConvert)
				registration.addRecipeCatalyst(new ItemStack(Blocks.ENCHANTING_TABLE), INFLUENCING);
			else
				registration.addRecipeCatalyst(new ItemStack(MatrixEnchantingModule.matrixEnchanter), INFLUENCING);
		}
	}

	@Override
	public void registerGuiHandlers(@NotNull IGuiHandlerRegistration registration) {
		registration.addGuiContainerHandler(CrateScreen.class, new CrateGuiHandler());
		registration.addRecipeClickArea(BackpackInventoryScreen.class, 137, 29, 10, 13, RecipeTypes.CRAFTING);
	}

	@Override
	public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
		registration.addRecipeTransferHandler(new BackpackRecipeTransferHandler(registration.getTransferHelper()), RecipeTypes.CRAFTING);
	}

	private void registerAncientTomeAnvilRecipes(@NotNull IRecipeRegistration registration, @NotNull IVanillaRecipeFactory factory) {
		List<IJeiAnvilRecipe> recipes = new ArrayList<>();
		for (Enchantment enchant : AncientTomesModule.validEnchants) {
			EnchantmentInstance data = new EnchantmentInstance(enchant, enchant.getMaxLevel());
			recipes.add(factory.createAnvilRecipe(EnchantedBookItem.createForEnchantment(data),
					Collections.singletonList(AncientTomeItem.getEnchantedItemStack(enchant)),
					Collections.singletonList(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(data.enchantment, data.level + 1)))));
		}
		registration.addRecipes(RecipeTypes.ANVIL, recipes);
	}

	private void registerRuneAnvilRecipes(@NotNull IRecipeRegistration registration, @NotNull IVanillaRecipeFactory factory) {
		RandomSource random = RandomSource.create();
		Stream<ItemStack> displayItems;
		if (Quark.ZETA.modules.isEnabled(ImprovedTooltipsModule.class) && ImprovedTooltipsModule.enchantingTooltips) {
			displayItems = EnchantedBookTooltips.getTestItems().stream();
		} else {
			displayItems = Stream.of(Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE,
					Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE,
					Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, Items.ELYTRA, Items.SHIELD, Items.BOW, Items.CROSSBOW,
					Items.TRIDENT, Items.FISHING_ROD, Items.SHEARS, PickarangModule.pickarang).map(ItemStack::new);
		}

		List<ItemStack> used = displayItems
				.filter(it -> !(it.getItem() instanceof IDisableable<?> dis) || dis.isEnabled())
				.map(item -> makeEnchantedDisplayItem(item, random))
				.collect(Collectors.toList());

		List<IJeiAnvilRecipe> recipes = new ArrayList<>();
		for (Item rune : RegistryUtil.getTagValues(RegistryAccessUtil.getRegistryAccess(), ColorRunesModule.runesTag)) {
			ItemStack runeStack = new ItemStack(rune);
			recipes.add(factory.createAnvilRecipe(used, Collections.singletonList(runeStack),
					used.stream().map(stack -> {
						ItemStack output = stack.copy();
						ItemNBTHelper.setBoolean(output, ColorRunesModule.TAG_RUNE_ATTACHED, true);
						ItemNBTHelper.setCompound(output, ColorRunesModule.TAG_RUNE_COLOR, runeStack.serializeNBT());
						return output;
					}).collect(Collectors.toList())));
		}
		registration.addRecipes(RecipeTypes.ANVIL, recipes);
	}

	// Runes only show up and can be only anvilled on enchanted items, so make some random enchanted items
	@NotNull
	private static ItemStack makeEnchantedDisplayItem(ItemStack input, RandomSource random) {
		ItemStack stack = input.copy();
		stack.setHoverName(Component.translatable("quark.jei.any_enchanted"));
		if (Quark.ZETA.itemExtensions.get(stack).getEnchantmentValueZeta(stack) <= 0) { // If it can't take anything in ench. tables...
			stack.enchant(Enchantments.UNBREAKING, 3); // it probably accepts unbreaking anyways
			return stack;
		}
		return EnchantmentHelper.enchantItem(random, stack, 25, false);
	}

	private void registerPickarangAnvilRepairs(Item pickarang, Item repairMaterial, @NotNull IRecipeRegistration registration, @NotNull IVanillaRecipeFactory factory) {
		//Repair ratios taken from JEI anvil maker
		ItemStack nearlyBroken = new ItemStack(pickarang);
		nearlyBroken.setDamageValue(nearlyBroken.getMaxDamage());
		ItemStack veryDamaged = nearlyBroken.copy();
		veryDamaged.setDamageValue(veryDamaged.getMaxDamage() * 3 / 4);
		ItemStack damaged = nearlyBroken.copy();
		damaged.setDamageValue(damaged.getMaxDamage() * 2 / 4);

		IJeiAnvilRecipe materialRepair = factory.createAnvilRecipe(nearlyBroken,
				Collections.singletonList(new ItemStack(repairMaterial)), Collections.singletonList(veryDamaged));
		IJeiAnvilRecipe toolRepair = factory.createAnvilRecipe(veryDamaged,
				Collections.singletonList(veryDamaged), Collections.singletonList(damaged));

		registration.addRecipes(RecipeTypes.ANVIL, Arrays.asList(materialRepair, toolRepair));
	}

	private void registerInfluenceRecipes(@NotNull IRecipeRegistration registration) {
		registration.addRecipes(INFLUENCING,
				Arrays.stream(DyeColor.values()).map(color -> {
					Block candle = MatrixEnchantingTableBlockEntity.CANDLES.get(color.getId());
					Influence influence = MatrixEnchantingModule.candleInfluences.get(color);

					return new InfluenceEntry(candle, influence);
				}).filter(InfluenceEntry::hasAny).collect(Collectors.toList()));

		registration.addRecipes(INFLUENCING,
				MatrixEnchantingModule.customInfluences.entrySet().stream().map(entry -> {
					Block block = entry.getKey().getBlock();
					Influence influence = entry.getValue().influence();

					return new InfluenceEntry(block, influence);
				}).filter(InfluenceEntry::hasAny).collect(Collectors.toList()));
	}

	private void hideAnvilRepairRecipes(@NotNull IRecipeManager manager) {
		Stream<IJeiAnvilRecipe> anvilRecipe = manager.createRecipeLookup(RecipeTypes.ANVIL).get();
		List<IJeiAnvilRecipe> hidden =
				anvilRecipe.filter(r -> {
					ItemStack left = r.getLeftInputs().stream()
							.filter(st -> {
								Item i = st.getItem();
								return DiamondRepairModule.repairChanges.containsKey(i) || DiamondRepairModule.unrepairableItems.contains(i);
							})
							.findFirst()
							.orElse(null);

					if(left != null) {
						for(ItemStack right: r.getRightInputs()) {
							Item item = left.getItem();
							if(item.isValidRepairItem(left, right))
								return true;
						}
					}

					return false;
				}).collect(Collectors.toList());

		manager.hideRecipes(RecipeTypes.ANVIL, hidden);
	}

	private void registerCustomAnvilRecipes(@NotNull IRecipeRegistration registration, @NotNull IVanillaRecipeFactory factory) {
		for(Item item : DiamondRepairModule.repairChanges.keySet()) {
			ItemStack left = new ItemStack(item);
			ItemStack out = left.copy();

			int max = Quark.ZETA.itemExtensions.get(left).getMaxDamageZeta(left);

			left.setDamageValue(max - 1);
			out.setDamageValue(max - max / 4);

			for(Item repair : DiamondRepairModule.repairChanges.get(item)) {
				IJeiAnvilRecipe toolRepair = factory.createAnvilRecipe(left, Collections.singletonList(new ItemStack(repair)), Collections.singletonList(out));

				registration.addRecipes(RecipeTypes.ANVIL, List.of(toolRepair));
			}
		}
	}

	private static class CrateGuiHandler implements IGuiContainerHandler<CrateScreen> {

		@NotNull
		@Override
		public List<Rect2i> getGuiExtraAreas(@NotNull CrateScreen containerScreen) {
			return containerScreen.getExtraAreas();
		}

	}
}

