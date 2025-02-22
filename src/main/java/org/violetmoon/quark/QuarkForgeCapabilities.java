package org.violetmoon.quark;

import org.violetmoon.quark.api.ICustomSorting;
import org.violetmoon.quark.api.IMagnetTracker;
import org.violetmoon.quark.api.IPistonCallback;
import org.violetmoon.quark.api.IRuneColorProvider;
import org.violetmoon.quark.api.ITransferManager;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

//TODO: put this somewhere in a Forge-only API package
public class QuarkForgeCapabilities {
	public static final Capability<ICustomSorting> SORTING = CapabilityManager.get(new CapabilityToken<>(){});

	public static final Capability<ITransferManager> TRANSFER = CapabilityManager.get(new CapabilityToken<>(){});

	public static final Capability<IPistonCallback> PISTON_CALLBACK = CapabilityManager.get(new CapabilityToken<>(){});

	public static final Capability<IMagnetTracker> MAGNET_TRACKER_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});

	public static final Capability<IRuneColorProvider> RUNE_COLOR = CapabilityManager.get(new CapabilityToken<>(){});
}
