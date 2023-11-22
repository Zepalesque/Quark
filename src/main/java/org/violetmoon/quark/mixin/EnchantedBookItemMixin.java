package org.violetmoon.quark.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.violetmoon.quark.content.experimental.module.EnchantmentsBegoneModule;

@Mixin(EnchantedBookItem.class)
public class EnchantedBookItemMixin {

	//TODO 1.20
//	@Inject(method = "fillItemCategory", at = @At("RETURN"))
//	private void canApply(CreativeModeTab tab, NonNullList<ItemStack> stacks, CallbackInfo ci) {
//		EnchantmentsBegoneModule.begoneItems(stacks);
//	}

}
