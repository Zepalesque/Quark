package org.violetmoon.quark.base.handler;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.violetmoon.quark.addons.oddities.inventory.BackpackMenu;
import org.violetmoon.quark.addons.oddities.inventory.slot.CachedItemHandlerSlot;
import org.violetmoon.quark.api.ICustomSorting;
import org.violetmoon.quark.api.ISortingLockedSlots;
import org.violetmoon.quark.api.QuarkCapabilities;
import org.violetmoon.quark.base.Quark;
import org.violetmoon.quark.content.management.module.InventorySortingModule;

import java.util.*;
import java.util.function.Predicate;

public final class SortingHandler {

	private static final Comparator<ItemStack> FALLBACK_COMPARATOR = jointComparator(Arrays.asList(
		Comparator.comparingInt((ItemStack s) -> Item.getId(s.getItem())),
		SortingHandler::damageCompare,
		(ItemStack s1, ItemStack s2) -> s2.getCount() - s1.getCount(),
		(ItemStack s1, ItemStack s2) -> s2.hashCode() - s1.hashCode(),
		SortingHandler::fallbackNBTCompare));

	private static final Comparator<ItemStack> FOOD_COMPARATOR = jointComparator(Arrays.asList(
		SortingHandler::foodHealCompare,
		SortingHandler::foodSaturationCompare));

	private static final Comparator<ItemStack> TOOL_COMPARATOR = jointComparator(Arrays.asList(
		SortingHandler::toolPowerCompare,
		SortingHandler::enchantmentCompare,
		SortingHandler::damageCompare));

	private static final Comparator<ItemStack> SWORD_COMPARATOR = jointComparator(Arrays.asList(
		SortingHandler::swordPowerCompare,
		SortingHandler::enchantmentCompare,
		SortingHandler::damageCompare));

	private static final Comparator<ItemStack> ARMOR_COMPARATOR = jointComparator(Arrays.asList(
		SortingHandler::armorSlotAndToughnessCompare,
		SortingHandler::enchantmentCompare,
		SortingHandler::damageCompare));

	private static final Comparator<ItemStack> BOW_COMPARATOR = jointComparator(Arrays.asList(
		SortingHandler::enchantmentCompare,
		SortingHandler::damageCompare));

	private static final Comparator<ItemStack> POTION_COMPARATOR = jointComparator(Arrays.asList(
		SortingHandler::potionComplexityCompare,
		SortingHandler::potionTypeCompare));

	public static void sortInventory(Player player, boolean forcePlayer) {
		if (!Quark.ZETA.modules.isEnabled(InventorySortingModule.class))
			return;

		AbstractContainerMenu c = player.containerMenu;
		AbstractContainerMenu ogc = c;
		boolean backpack = c instanceof BackpackMenu;
		boolean sortingLocked = c instanceof ISortingLockedSlots;

		if ((!backpack && forcePlayer) || c == null)
			c = player.inventoryMenu;

		boolean playerContainer = c == player.inventoryMenu || backpack;
		int[] lockedSlots = null;

		if (sortingLocked) {
			ISortingLockedSlots sls = (ISortingLockedSlots) ogc;
			lockedSlots = sls.getSortingLockedSlots(playerContainer);
		}

		for (Slot s : c.slots) {
			Container inv = s.container;
			if ((inv == player.getInventory()) == playerContainer) {
				if (!playerContainer && s instanceof SlotItemHandler slot) {
					sortInventory(slot.getItemHandler(), lockedSlots);
				} else {
					InvWrapper wrapper = new InvWrapper(inv);
					if (playerContainer)
						sortInventory(wrapper, 9, 36, lockedSlots);
					else sortInventory(wrapper, lockedSlots);
				}
				break;
			}
		}

		if (backpack)
			for (Slot s : c.slots)
				if (s instanceof CachedItemHandlerSlot) {
					sortInventory(((CachedItemHandlerSlot) s).getItemHandler(), lockedSlots);
					break;
				}
	}

	public static void sortInventory(IItemHandler handler, int[] lockedSlots) {
		sortInventory(handler, 0, lockedSlots);
	}

	public static void sortInventory(IItemHandler handler, int iStart, int[] lockedSlots) {
		sortInventory(handler, iStart, handler.getSlots(), lockedSlots);
	}

	public static void sortInventory(IItemHandler handler, int iStart, int iEnd, int[] lockedSlots) {
		List<ItemStack> stacks = new ArrayList<>();
		List<ItemStack> restore = new ArrayList<>();

		for (int i = iStart; i < iEnd; i++) {
			ItemStack stackAt = handler.getStackInSlot(i);

			restore.add(stackAt.copy());
			if (!isLocked(i, lockedSlots) && !stackAt.isEmpty())
				stacks.add(stackAt.copy());
		}

		mergeStacks(stacks);
		sortStackList(stacks);

		if (setInventory(handler, stacks, iStart, iEnd, lockedSlots) == InteractionResult.FAIL)
			setInventory(handler, restore, iStart, iEnd, lockedSlots);
	}

	private static InteractionResult setInventory(IItemHandler inventory, List<ItemStack> stacks, int iStart, int iEnd, int[] lockedSlots) {
		int skipped = 0;
		for (int i = iStart; i < iEnd; i++) {
			if (isLocked(i, lockedSlots)) {
				skipped++;
				continue;
			}

			int j = i - iStart - skipped;
			ItemStack stack = j >= stacks.size() ? ItemStack.EMPTY : stacks.get(j);

			ItemStack stackInSlot = inventory.getStackInSlot(i);
			if (!stackInSlot.isEmpty()) {
				ItemStack extractTest = inventory.extractItem(i, inventory.getSlotLimit(i), true);
				if (extractTest.isEmpty() || extractTest.getCount() != stackInSlot.getCount())
					return InteractionResult.PASS;
			}

			if (!stack.isEmpty() && !inventory.isItemValid(i, stack))
				return InteractionResult.PASS;
		}

		for (int i = iStart; i < iEnd; i++) {
			if (isLocked(i, lockedSlots))
				continue;

			inventory.extractItem(i, inventory.getSlotLimit(i), false);
		}

		skipped = 0;
		for (int i = iStart; i < iEnd; i++) {
			if (isLocked(i, lockedSlots)) {
				skipped++;
				continue;
			}

			int j = i - iStart - skipped;
			ItemStack stack = j >= stacks.size() ? ItemStack.EMPTY : stacks.get(j);

			if (!stack.isEmpty())
				if (!inventory.insertItem(i, stack, false).isEmpty())
					return InteractionResult.FAIL;
		}

		return InteractionResult.SUCCESS;
	}

	private static boolean isLocked(int slot, int[] locked) {
		if (locked == null)
			return false;
		for (int i : locked)
			if (slot == i)
				return true;
		return false;
	}

	public static void mergeStacks(List<ItemStack> list) {
		for (int i = 0; i < list.size(); i++) {
			ItemStack set = mergeStackWithOthers(list, i);
			list.set(i, set);
		}

		list.removeIf((ItemStack stack) -> stack.isEmpty() || stack.getCount() == 0);
	}

	private static ItemStack mergeStackWithOthers(List<ItemStack> list, int index) {
		ItemStack stack = list.get(index);
		if (stack.isEmpty())
			return stack;

		for (int i = 0; i < list.size(); i++) {
			if (i == index)
				continue;

			ItemStack stackAt = list.get(i);
			if (stackAt.isEmpty())
				continue;

			if (stackAt.getCount() < stackAt.getMaxStackSize() && ItemStack.isSameItem(stack, stackAt) && ItemStack.isSameItemSameTags(stack, stackAt)) {
				int setSize = stackAt.getCount() + stack.getCount();
				int carryover = Math.max(0, setSize - stackAt.getMaxStackSize());
				stackAt.setCount(carryover);
				stack.setCount(setSize - carryover);

				if (stack.getCount() == stack.getMaxStackSize())
					return stack;
			}
		}

		return stack;
	}

	public static void sortStackList(List<ItemStack> list) {
		list.sort(SortingHandler::stackCompare);
	}

	private static int stackCompare(ItemStack stack1, ItemStack stack2) {
		if (stack1 == stack2)
			return 0;
		if (stack1.isEmpty())
			return -1;
		if (stack2.isEmpty())
			return 1;

		if (hasCustomSorting(stack1) && hasCustomSorting(stack2)) {
			ICustomSorting sort1 = getCustomSorting(stack1);
			ICustomSorting sort2 = getCustomSorting(stack2);
			if (sort1.getSortingCategory().equals(sort2.getSortingCategory()))
				return sort1.getItemComparator().compare(stack1, stack2);
		}

		ItemType type1 = getType(stack1);
		ItemType type2 = getType(stack2);

		if (type1 == type2)
			return type1.comparator.compare(stack1, stack2);

		return type1.ordinal() - type2.ordinal();
	}

	private static ItemType getType(ItemStack stack) {
		for (ItemType type : ItemType.values())
			if (type.fitsInType(stack))
				return type;

		throw new RuntimeException("Having an ItemStack that doesn't fit in any type is impossible.");
	}

	private static Predicate<ItemStack> classPredicate(Class<? extends Item> clazz) {
		return (ItemStack s) -> !s.isEmpty() && clazz.isInstance(s.getItem());
	}

	private static Predicate<ItemStack> inverseClassPredicate(Class<? extends Item> clazz) {
		return classPredicate(clazz).negate();
	}

	private static Predicate<ItemStack> itemPredicate(List<Item> list) {
		return (ItemStack s) -> !s.isEmpty() && list.contains(s.getItem());
	}

	public static Comparator<ItemStack> jointComparator(Comparator<ItemStack> finalComparator, List<Comparator<ItemStack>> otherComparators) {
		if (otherComparators == null)
			return jointComparator(List.of(finalComparator));

		List<Comparator<ItemStack>> newList = new ArrayList<>(otherComparators);
		newList.add(finalComparator);
		return jointComparator(newList);
	}

	public static Comparator<ItemStack> jointComparator(List<Comparator<ItemStack>> comparators) {
		return jointComparatorFallback((ItemStack s1, ItemStack s2) -> {
			for (Comparator<ItemStack> comparator : comparators) {
				if (comparator == null)
					continue;

				int compare = comparator.compare(s1, s2);
				if (compare == 0)
					continue;

				return compare;
			}

			return 0;
		}, FALLBACK_COMPARATOR);
	}

	private static Comparator<ItemStack> jointComparatorFallback(Comparator<ItemStack> comparator, Comparator<ItemStack> fallback) {
		return (ItemStack s1, ItemStack s2) -> {
			int compare = comparator.compare(s1, s2);
			if (compare == 0)
				return fallback == null ? 0 : fallback.compare(s1, s2);

			return compare;
		};
	}

	private static Comparator<ItemStack> listOrderComparator(List<Item> list) {
		return (ItemStack stack1, ItemStack stack2) -> {
			Item i1 = stack1.getItem();
			Item i2 = stack2.getItem();
			if (list.contains(i1)) {
				if (list.contains(i2))
					return list.indexOf(i1) - list.indexOf(i2);
				return 1;
			}

			if (list.contains(i2))
				return -1;

			return 0;
		};
	}

	private static List<Item> list(Object... items) {
		List<Item> itemList = new ArrayList<>();
		for (Object o : items)
			if (o != null) {
				if (o instanceof Item item)
					itemList.add(item);
				else if (o instanceof Block block)
					itemList.add(block.asItem());
				else if (o instanceof ItemStack stack)
					itemList.add(stack.getItem());
				else if (o instanceof String s) {
					Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(s));
					if (item != Items.AIR)
						itemList.add(item);
				}
			}

		return itemList;
	}

	private static int nutrition(FoodProperties properties) {
		if (properties == null)
			return 0;
		return properties.getNutrition();
	}

	private static int foodHealCompare(ItemStack stack1, ItemStack stack2) {
		return nutrition(stack2.getItem().getFoodProperties()) - nutrition(stack1.getItem().getFoodProperties());
	}

	private static float saturation(FoodProperties properties) {
		if (properties == null)
			return 0;
		return Math.min(20, properties.getNutrition() * properties.getSaturationModifier() * 2);
	}

	private static int foodSaturationCompare(ItemStack stack1, ItemStack stack2) {
		return (int) (saturation(stack2.getItem().getFoodProperties()) - saturation(stack1.getItem().getFoodProperties()));
	}

	private static int enchantmentCompare(ItemStack stack1, ItemStack stack2) {
		return enchantmentPower(stack2) - enchantmentPower(stack1);
	}

	private static int enchantmentPower(ItemStack stack) {
		if (!stack.isEnchanted())
			return 0;

		Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
		int total = 0;

		for (Integer i : enchantments.values())
			total += i;

		return total;
	}

	private static int toolPowerCompare(ItemStack stack1, ItemStack stack2) {
		Tier mat1 = ((DiggerItem) stack1.getItem()).getTier();
		Tier mat2 = ((DiggerItem) stack2.getItem()).getTier();
		return (int) (mat2.getSpeed() * 100 - mat1.getSpeed() * 100);
	}

	private static int swordPowerCompare(ItemStack stack1, ItemStack stack2) {
		Tier mat1 = ((SwordItem) stack1.getItem()).getTier();
		Tier mat2 = ((SwordItem) stack2.getItem()).getTier();
		return (int) (mat2.getAttackDamageBonus() * 100 - mat1.getAttackDamageBonus() * 100);
	}

	private static int armorSlotAndToughnessCompare(ItemStack stack1, ItemStack stack2) {
		ArmorItem armor1 = (ArmorItem) stack1.getItem();
		ArmorItem armor2 = (ArmorItem) stack2.getItem();

		EquipmentSlot slot1 = armor1.getEquipmentSlot();
		EquipmentSlot slot2 = armor2.getEquipmentSlot();

		if (slot1 == slot2)
			return armor2.getMaterial().getDefenseForType(armor2.getType()) - armor2.getMaterial().getDefenseForType(armor1.getType());

		return slot2.getIndex() - slot1.getIndex();
	}

	public static int damageCompare(ItemStack stack1, ItemStack stack2) {
		return stack1.getDamageValue() - stack2.getDamageValue();
	}

	public static int fallbackNBTCompare(ItemStack stack1, ItemStack stack2) {
		boolean hasTag1 = stack1.hasTag();
		boolean hasTag2 = stack2.hasTag();

		if (hasTag2 && !hasTag1)
			return -1;
		else if (hasTag1 && !hasTag2)
			return 1;
		else if (!hasTag1)
			return 0;

		return stack2.getTag().toString().hashCode() - stack1.getTag().toString().hashCode();
	}

	public static int potionComplexityCompare(ItemStack stack1, ItemStack stack2) {
		List<MobEffectInstance> effects1 = PotionUtils.getCustomEffects(stack1);
		List<MobEffectInstance> effects2 = PotionUtils.getCustomEffects(stack2);

		int totalPower1 = 0;
		int totalPower2 = 0;
		for (MobEffectInstance inst : effects1)
			totalPower1 += inst.getAmplifier() * inst.getDuration();
		for (MobEffectInstance inst : effects2)
			totalPower2 += inst.getAmplifier() * inst.getDuration();

		return totalPower2 - totalPower1;
	}

	public static int potionTypeCompare(ItemStack stack1, ItemStack stack2) {
		Potion potion1 = PotionUtils.getPotion(stack1);
		Potion potion2 = PotionUtils.getPotion(stack2);

		return BuiltInRegistries.POTION.getId(potion2) - BuiltInRegistries.POTION.getId(potion1);
	}

	static boolean hasCustomSorting(ItemStack stack) {
		return Quark.ZETA.capabilityManager.hasCapability(QuarkCapabilities.SORTING, stack);
	}

	static ICustomSorting getCustomSorting(ItemStack stack) {
		return Quark.ZETA.capabilityManager.getCapability(QuarkCapabilities.SORTING, stack);
	}

	private enum ItemType {

		FOOD(ItemStack::isEdible, FOOD_COMPARATOR),
		TORCH(list(Blocks.TORCH)),
		TOOL_PICKAXE(classPredicate(PickaxeItem.class), TOOL_COMPARATOR),
		TOOL_SHOVEL(classPredicate(ShovelItem.class), TOOL_COMPARATOR),
		TOOL_AXE(classPredicate(AxeItem.class), TOOL_COMPARATOR),
		TOOL_SWORD(classPredicate(SwordItem.class), SWORD_COMPARATOR),
		TOOL_GENERIC(classPredicate(DiggerItem.class), TOOL_COMPARATOR),
		ARMOR(classPredicate(ArmorItem.class), ARMOR_COMPARATOR),
		BOW(classPredicate(BowItem.class), BOW_COMPARATOR),
		CROSSBOW(classPredicate(CrossbowItem.class), BOW_COMPARATOR),
		TRIDENT(classPredicate(TridentItem.class), BOW_COMPARATOR),
		ARROWS(classPredicate(ArrowItem.class)),
		POTION(classPredicate(PotionItem.class), POTION_COMPARATOR),
		TIPPED_ARROW(classPredicate(TippedArrowItem.class), POTION_COMPARATOR),
		MINECART(classPredicate(MinecartItem.class)),
		RAIL(list(Blocks.RAIL, Blocks.POWERED_RAIL, Blocks.DETECTOR_RAIL, Blocks.ACTIVATOR_RAIL)),
		DYE(classPredicate(DyeItem.class)),
		ANY(inverseClassPredicate(BlockItem.class)),
		BLOCK(classPredicate(BlockItem.class));

		private final Predicate<ItemStack> predicate;
		private final Comparator<ItemStack> comparator;

		ItemType(List<Item> list) {
			this(itemPredicate(list), jointComparator(listOrderComparator(list), new ArrayList<>()));
		}

		ItemType(Predicate<ItemStack> predicate) {
			this(predicate, FALLBACK_COMPARATOR);
		}

		ItemType(Predicate<ItemStack> predicate, Comparator<ItemStack> comparator) {
			this.predicate = predicate;
			this.comparator = comparator;
		}

		public boolean fitsInType(ItemStack stack) {
			return predicate.test(stack);
		}

	}

}

