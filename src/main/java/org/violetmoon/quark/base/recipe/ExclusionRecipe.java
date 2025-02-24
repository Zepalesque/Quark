package org.violetmoon.quark.base.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.IShapedRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author WireSegal
 * Created at 2:08 PM on 8/24/19.
 */
public class ExclusionRecipe implements CraftingRecipe {
	public static final Serializer SERIALIZER = new Serializer();

	private final CraftingRecipe parent;
	private final List<ResourceLocation> excluded;

	public ExclusionRecipe(CraftingRecipe parent, List<ResourceLocation> excluded) {
		this.parent = parent;
		this.excluded = excluded;
	}

	@Override
	public boolean matches(@NotNull CraftingContainer inv, @NotNull Level worldIn) {
		for (ResourceLocation recipeLoc : excluded) {
			Optional<? extends Recipe<?>> recipeHolder = worldIn.getRecipeManager().byKey(recipeLoc);
			if (recipeHolder.isPresent()) {
				Recipe<?> recipe = recipeHolder.get();
				if (recipe instanceof CraftingRecipe craftingRecipe &&
						craftingRecipe.matches(inv, worldIn)) {
					return false;
				}
			}
		}

		return parent.matches(inv, worldIn);
	}

	@NotNull
	@Override
	public ItemStack assemble(@NotNull CraftingContainer inv, RegistryAccess gaming) {
		return parent.assemble(inv, gaming);
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return parent.canCraftInDimensions(width, height);
	}

	@NotNull
	@Override
	public ItemStack getResultItem(RegistryAccess acc) {
		return parent.getResultItem(acc);
	}

	@NotNull
	@Override
	public ResourceLocation getId() {
		return parent.getId();
	}

	@NotNull
	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}

	@NotNull
	@Override
	public RecipeType<?> getType() {
		return parent.getType();
	}

	@NotNull
	@Override
	public NonNullList<ItemStack> getRemainingItems(@NotNull CraftingContainer inv) {
		return parent.getRemainingItems(inv);
	}

	@NotNull
	@Override
	public NonNullList<Ingredient> getIngredients() {
		return parent.getIngredients();
	}

	@Override
	public boolean isSpecial() {
		return parent.isSpecial();
	}

	@NotNull
	@Override
	public String getGroup() {
		return parent.getGroup();
	}

	@NotNull
	@Override
	public ItemStack getToastSymbol() {
		return parent.getToastSymbol();
	}

	@Override
	public CraftingBookCategory category() {
		return CraftingBookCategory.MISC;
	}

	private static class ShapedExclusionRecipe extends ExclusionRecipe implements IShapedRecipe<CraftingContainer> {
		private final IShapedRecipe<CraftingContainer> parent;

		@SuppressWarnings("unchecked")
		public ShapedExclusionRecipe(CraftingRecipe parent, List<ResourceLocation> excluded) {
			super(parent, excluded);
			this.parent = (IShapedRecipe<CraftingContainer>) parent;
		}

		@Override
		public int getRecipeWidth() {
			return parent.getRecipeWidth();
		}

		@Override
		public int getRecipeHeight() {
			return parent.getRecipeHeight();
		}

		@Override
		public CraftingBookCategory category() {
			return CraftingBookCategory.MISC;
		}

		@Override
		public @NotNull ItemStack assemble(@NotNull CraftingContainer craftingContainer, RegistryAccess registryAccess) {
			return ItemStack.EMPTY;
		}

		@Override
		public ItemStack getResultItem(RegistryAccess registryAccess) {
			return ItemStack.EMPTY;
		}
	}

	public static class Serializer implements RecipeSerializer<ExclusionRecipe> {

		@NotNull
		@Override
		public ExclusionRecipe fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
			String trueType = GsonHelper.getAsString(json, "true_type");
			if (trueType.equals("quark:exclusion"))
				throw new JsonSyntaxException("Recipe type circularity");

			JsonArray excluded = GsonHelper.getAsJsonArray(json, "exclusions");
			List<ResourceLocation> excludedRecipes = new ArrayList<>();
			for (JsonElement el : excluded) {
				ResourceLocation loc = new ResourceLocation(el.getAsString());
				if (!loc.equals(recipeId))
					excludedRecipes.add(loc);
			}

			RecipeSerializer<?> serializer = BuiltInRegistries.RECIPE_SERIALIZER.get(new ResourceLocation(trueType));
			if (serializer == null)
				throw new JsonSyntaxException("Invalid or unsupported recipe type '" + trueType + "'");
			Recipe<?> parent = serializer.fromJson(recipeId, json);
			if (!(parent instanceof CraftingRecipe))
				throw new JsonSyntaxException("Type '" + trueType + "' is not a crafting recipe");

			if (parent instanceof IShapedRecipe)
				return new ShapedExclusionRecipe((CraftingRecipe) parent, excludedRecipes);
			return new ExclusionRecipe((CraftingRecipe) parent, excludedRecipes);
		}

		@NotNull
		@Override
		public ExclusionRecipe fromNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buffer) {
			int exclusions = buffer.readVarInt();
			List<ResourceLocation> excludedRecipes = new ArrayList<>();
			for (int i = 0; i < exclusions; i++) {
				ResourceLocation loc = new ResourceLocation(buffer.readUtf(32767));
				if (!loc.equals(recipeId))
					excludedRecipes.add(loc);
			}
			String trueType = buffer.readUtf(32767);

			RecipeSerializer<?> serializer = BuiltInRegistries.RECIPE_SERIALIZER.get(new ResourceLocation(trueType));
			if (serializer == null)
				throw new IllegalArgumentException("Invalid or unsupported recipe type '" + trueType + "'");
			Recipe<?> parent = serializer.fromNetwork(recipeId, buffer);
			if (!(parent instanceof CraftingRecipe craftingRecipe))
				throw new IllegalArgumentException("Type '" + trueType + "' is not a crafting recipe");

			if (parent instanceof IShapedRecipe)
				return new ShapedExclusionRecipe(craftingRecipe, excludedRecipes);
			return new ExclusionRecipe(craftingRecipe, excludedRecipes);
		}

		@Override
		@SuppressWarnings("unchecked")
		public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull ExclusionRecipe recipe) {
			buffer.writeVarInt(recipe.excluded.size());
			for (ResourceLocation loc : recipe.excluded)
				buffer.writeUtf(loc.toString(), 32767);
			buffer.writeUtf(Objects.toString(BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.parent.getSerializer())), 32767);
			((RecipeSerializer<Recipe<?>>) recipe.parent.getSerializer()).toNetwork(buffer, recipe.parent);
		}
	}
}
