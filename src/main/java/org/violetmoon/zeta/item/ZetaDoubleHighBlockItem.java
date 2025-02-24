package org.violetmoon.zeta.item;

import java.util.function.BooleanSupplier;

import org.jetbrains.annotations.NotNull;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.ItemStack;
import org.violetmoon.zeta.block.IZetaBlock;
import org.violetmoon.zeta.module.ZetaModule;

public class ZetaDoubleHighBlockItem extends DoubleHighBlockItem implements IZetaItem {

	private final ZetaModule module;

	private BooleanSupplier enabledSupplier = () -> true;

	public ZetaDoubleHighBlockItem(IZetaBlock baseBlock, Properties props) {
		super(baseBlock.getBlock(), props);

		this.module = baseBlock.getModule();
	}

	@Override
	public ZetaDoubleHighBlockItem setCondition(BooleanSupplier enabledSupplier) {
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

}
