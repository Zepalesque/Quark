package org.violetmoon.quark.base;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.violetmoon.quark.base.proxy.ClientProxy;
import org.violetmoon.quark.base.proxy.CommonProxy;
import org.violetmoon.zeta.Zeta;
import org.violetmoon.zetaimplforge.ForgeZeta;

@Mod(Quark.MOD_ID)
public class Quark {

	public static final String MOD_ID = "quark";
	public static final String ODDITIES_ID = "quark"; ///SHHHhhh quat is testing, todo fix the modid

	public static Quark instance;
	public static CommonProxy proxy;

	public static final Logger LOG = LogManager.getLogger(MOD_ID);

	public static final Zeta ZETA = new ForgeZeta(MOD_ID, LogManager.getLogger("quark-zeta"));

	public Quark() {
		instance = this;

		ZETA.start();

		proxy = DistExecutor.runForDist(() -> ClientProxy::new, () -> CommonProxy::new);
		proxy.start();
	}

	public static ResourceLocation asResource(String name) {
		return new ResourceLocation(MOD_ID, name);
	}
}
