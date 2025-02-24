package org.violetmoon.quark.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.violetmoon.quark.content.management.module.EasyTransferingModule;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

@Mixin(AbstractContainerScreen.class)
public class ContainerScreenMixin {

	@ModifyVariable(method = "mouseClicked(DDI)Z",
			at = @At("STORE"),
			index = 15)
	private boolean hasShiftDownClick(boolean curr) {
		return EasyTransferingModule.Client.hasShiftDown(curr);
	}
	
	@ModifyVariable(method = "mouseReleased(DDI)Z",
			at = @At("STORE"),
			index = 12)
	private boolean hasShiftDownRelease(boolean curr) {
		return EasyTransferingModule.Client.hasShiftDown(curr);
	}
	
}
