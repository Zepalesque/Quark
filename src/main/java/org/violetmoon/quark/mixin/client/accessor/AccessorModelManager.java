package org.violetmoon.quark.mixin.client.accessor;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ModelManager.class)
public interface AccessorModelManager {

	@Accessor
	Map<ResourceLocation, BakedModel> getBakedRegistry();
}
