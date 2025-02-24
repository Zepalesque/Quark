package org.violetmoon.zeta.item;

import java.util.function.BooleanSupplier;

import org.jetbrains.annotations.NotNull;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.violetmoon.zeta.module.ZetaModule;

public abstract class ZetaArrowItem extends ArrowItem implements IZetaItem {

	private final ZetaModule module;
	private BooleanSupplier enabledSupplier = () -> true;

	public ZetaArrowItem(String name, ZetaModule module) {
		super(new Item.Properties());

		module.zeta.registry.registerItem(this, name);
		this.module = module;
	}

	@Override
	public ZetaArrowItem setCondition(BooleanSupplier enabledSupplier) {
		this.enabledSupplier = enabledSupplier;
		return this;
	}

	@Override
	public ZetaModule getModule() {
		return module;
	}

	@Override
	public boolean doesConditionApply() {
		return enabledSupplier.getAsBoolean();
	}
	
	public static class Impl extends ZetaArrowItem {

		private final ArrowCreator creator;
		
		public Impl(String name, ZetaModule module, ArrowCreator creator) {
			super(name, module);
			this.creator = creator;
		}
		
		@Override
		public AbstractArrow createArrow(Level p_40513_, ItemStack p_40514_, LivingEntity p_40515_) {
			return creator.createArrow(p_40513_, p_40514_, p_40515_);
		}
		
		public static interface ArrowCreator {
			public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity living);
		}
		
	}
	
}
