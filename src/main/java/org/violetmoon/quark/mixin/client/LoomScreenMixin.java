package org.violetmoon.quark.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.violetmoon.quark.content.tweaks.module.MoreBannerLayersModule;

import net.minecraft.client.gui.screens.inventory.LoomScreen;

@Mixin(LoomScreen.class)
public class LoomScreenMixin {

	@ModifyConstant(method = "containerChanged", constant = @Constant(intValue = 6))
	private static int getLimit(int curr) {
		return MoreBannerLayersModule.getLimit(curr);
	}
	
}
