package org.violetmoon.quark.base.handler;

import com.google.common.collect.*;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;
import org.violetmoon.quark.api.event.RecipeCrawlEvent;
import org.violetmoon.quark.api.event.RecipeCrawlEvent.Visit;
import org.violetmoon.quark.base.Quark;
import org.violetmoon.quark.base.util.registryaccess.RegistryAccessUtil;
import org.violetmoon.zeta.event.bus.LoadEvent;
import org.violetmoon.zeta.event.bus.PlayEvent;
import org.violetmoon.zeta.event.load.ZAddReloadListener;
import org.violetmoon.zeta.event.load.ZTagsUpdated;
import org.violetmoon.zeta.event.play.ZServerTick;
import org.violetmoon.zeta.util.RegistryUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class RecipeCrawlHandler {

	private static List<Recipe<?>> recipesToLazyDigest = new ArrayList<>();
	private static Multimap<Item, ItemStack> recipeDigestion = HashMultimap.create();
	private static Multimap<Item, ItemStack> backwardsDigestion = HashMultimap.create();

	private static final Object mutex = new Object();
	private static boolean needsCrawl = false;
	private static boolean mayCrawl = false;

	@LoadEvent
	public static void addListener(ZAddReloadListener event) {
		event.addListener((barrier, manager, prepFiller, applyFiller, prepExec, applyExec) -> {
			return
				CompletableFuture.runAsync(() -> {
					clear();
				}, prepExec)

				.thenCompose(barrier::wait)

				.thenRunAsync(() -> {
					needsCrawl = true;
				}, applyExec);
		});
	}

	@LoadEvent
	public static void tagsHaveUpdated(ZTagsUpdated event) {
		mayCrawl = true;
	}

	private static void clear() {
		mayCrawl = false;
		MinecraftForge.EVENT_BUS.post(new RecipeCrawlEvent.Reset());
	}

	private static void load(RecipeManager manager) {
		if(!manager.getRecipes().isEmpty()) {
			MinecraftForge.EVENT_BUS.post(new RecipeCrawlEvent.CrawlStarting());

			recipesToLazyDigest.clear();
			recipeDigestion.clear();
			backwardsDigestion.clear();

			Collection<Recipe<?>> recipes = manager.getRecipes();

			for(Recipe<?> recipe : recipes) {
				try {
					if (recipe == null || recipe.getIngredients() == null || recipe.getResultItem(RegistryAccessUtil.getRegistryAccess()) == null)
						continue;

					RecipeCrawlEvent.Visit<?> event;

					if (recipe instanceof ShapedRecipe sr)
						event = new Visit.Shaped(sr);
					else if (recipe instanceof ShapelessRecipe sr)
						event = new Visit.Shapeless(sr);
					else if (recipe instanceof CustomRecipe cr)
						event = new Visit.Custom(cr);
					else if (recipe instanceof AbstractCookingRecipe acr)
						event = new Visit.Cooking(acr);
					else
						event = new Visit.Misc(recipe);

					recipesToLazyDigest.add(recipe);
					MinecraftForge.EVENT_BUS.post(event);
				} catch (Exception e) {
					Quark.LOG.warn("Failed to scan recipe " + recipe.getId() + ". This should be reported to " + recipe.getId().getNamespace() + "!", e);
				}
			}
		}
	}

	@PlayEvent
	public static void onTick(ZServerTick tick) {
		synchronized(mutex) {
			if(mayCrawl && needsCrawl) {
				RecipeManager manager = tick.getServer().getRecipeManager();
				load(manager);
				needsCrawl = false;
			}

			if(!recipesToLazyDigest.isEmpty()) {
				recipeDigestion.clear();
				backwardsDigestion.clear();

				for(Recipe<?> recipe : recipesToLazyDigest)
					digest(recipe);

				recipesToLazyDigest.clear();
				MinecraftForge.EVENT_BUS.post(new RecipeCrawlEvent.Digest(recipeDigestion, backwardsDigestion));
			}
		}
	}

	private static void digest(Recipe<?> recipe) {
		ItemStack out = recipe.getResultItem(RegistryAccessUtil.getRegistryAccess());
		Item outItem = out.getItem();

		NonNullList<Ingredient> ingredients = recipe.getIngredients();
		for(Ingredient ingredient : ingredients) {
			for (ItemStack inStack : ingredient.getItems()) {
				recipeDigestion.put(inStack.getItem(), out);
				backwardsDigestion.put(outItem, inStack);
			}
		}
	}

	/*
	 * Derivation list -> items to add and then derive (raw materials)
	 * Whitelist -> items to add and not derive from
	 * Blacklist -> items to ignore
	 */

	public static void recursivelyFindCraftedItemsFromStrings(@Nullable Collection<String> derivationList, @Nullable Collection<String> whitelist, @Nullable Collection<String> blacklist, Consumer<Item> callback) {
		List<Item> parsedDerivationList = derivationList == null ? null : RegistryUtil.massRegistryGet(derivationList, BuiltInRegistries.ITEM);
		List<Item> parsedWhitelist      = whitelist == null      ? null : RegistryUtil.massRegistryGet(whitelist, BuiltInRegistries.ITEM);
		List<Item> parsedBlacklist      = blacklist == null      ? null : RegistryUtil.massRegistryGet(blacklist, BuiltInRegistries.ITEM);

		recursivelyFindCraftedItems(parsedDerivationList, parsedWhitelist, parsedBlacklist, callback);
	}

	public static void recursivelyFindCraftedItems(@Nullable Collection<Item> derivationList, @Nullable Collection<Item> whitelist, @Nullable Collection<Item> blacklist, Consumer<Item> callback) {
		Collection<Item> trueDerivationList = derivationList == null  ? Lists.newArrayList() : derivationList;
		Collection<Item> trueWhitelist      = whitelist == null       ? Lists.newArrayList() : whitelist;
		Collection<Item> trueBlacklist      = blacklist == null       ? Lists.newArrayList() : blacklist;

		Streams.concat(trueDerivationList.stream(), trueWhitelist.stream()).forEach(callback);

		Set<Item> scanned = Sets.newHashSet(trueDerivationList);
		List<Item> toScan = Lists.newArrayList(trueDerivationList);

		while (!toScan.isEmpty()) {
			Item scan = toScan.remove(0);

			if (recipeDigestion.containsKey(scan)) {
				for (ItemStack digestedStack : recipeDigestion.get(scan)) {
					Item candidate = digestedStack.getItem();

					if (!scanned.contains(candidate)) {
						scanned.add(candidate);
						toScan.add(candidate);

						if(!trueBlacklist.contains(candidate))
							callback.accept(candidate);
					}
				}
			}
		}
	}

}
