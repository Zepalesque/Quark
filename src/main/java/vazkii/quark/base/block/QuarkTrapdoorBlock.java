package vazkii.quark.base.block;

import java.util.function.BooleanSupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.TrapDoorBlock;
import vazkii.quark.base.Quark;
import vazkii.quark.base.handler.CreativeTabHandler;
import vazkii.quark.base.handler.RenderLayerHandler;
import vazkii.quark.base.module.QuarkModule;

public class QuarkTrapdoorBlock extends TrapDoorBlock implements IQuarkBlock {

	private final QuarkModule module;
	private BooleanSupplier enabledSupplier = () -> true;

	public QuarkTrapdoorBlock(String regname, QuarkModule module, CreativeModeTab creativeTab, Properties properties) {
		super(properties);
		this.module = module;

		RenderLayerHandler.setRenderType(this, RenderLayerHandler.RenderTypeSkeleton.CUTOUT);
		Quark.ZETA.registry.registerBlock(this, regname, true);

		CreativeTabHandler.addTab(this, creativeTab);

	}

	@Override
	public void fillItemCategory(@Nonnull CreativeModeTab group, @Nonnull NonNullList<ItemStack> items) {
		if(isEnabled() || group == CreativeModeTab.TAB_SEARCH)
			super.fillItemCategory(group, items);
	}

	@Override
	public QuarkTrapdoorBlock setCondition(BooleanSupplier enabledSupplier) {
		this.enabledSupplier = enabledSupplier;
		return this;
	}

	@Override
	public boolean doesConditionApply() {
		return enabledSupplier.getAsBoolean();
	}

	@Nullable
	@Override
	public QuarkModule getModule() {
		return module;
	}

}
