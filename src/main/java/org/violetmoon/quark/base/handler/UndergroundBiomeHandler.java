package org.violetmoon.quark.base.handler;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import org.violetmoon.quark.base.Quark;
import org.violetmoon.zeta.event.bus.LoadEvent;
import org.violetmoon.zeta.event.load.ZLoadComplete;
import org.violetmoon.zeta.module.ZetaModule;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class UndergroundBiomeHandler {

	private static Proxy proxy = null;

	@LoadEvent
	public static void init(ZLoadComplete event) {
		proxy().init(event);
	}

	public static void addUndergroundBiomes(OverworldBiomeBuilder builder, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> consumer) {
		proxy().addUndergroundBiomes(builder, consumer);
	}

	public static void addUndergroundBiome(ZetaModule module, Climate.ParameterPoint climate, ResourceLocation biome) {
		UndergroundBiomeSkeleton skeleton = new UndergroundBiomeSkeleton(module, climate, biome);
		proxy().addUndergroundBiome(skeleton);
	}

	@SuppressWarnings("unchecked")
	private static Proxy proxy() {
		if(proxy == null) {
			try {
				Class<?> testClazz = Class.forName("terrablender.api.Region");

				if(testClazz != null) try {
					Class<?> clazz = Class.forName("org.violetmoon.quark.integration.terrablender.TerraBlenderIntegration");
					Supplier<UndergroundBiomeHandler.Proxy> supplier = (Supplier<Proxy>) clazz.getConstructor().newInstance();
					proxy = supplier.get();
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
			} catch (ClassNotFoundException e1) {
				Quark.LOG.info("TerraBlender not found, using injection method");
			}

			if(proxy == null)
				proxy = new Proxy();
		}

		return proxy;
	}

	public static class Proxy {

		public List<UndergroundBiomeSkeleton> skeletons = new ArrayList<>();

		public void init(ZLoadComplete event) {
			// NO-OP
		}

		public void addUndergroundBiomes(OverworldBiomeBuilder builder, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> consumer) {
			for(UndergroundBiomeSkeleton skeleton : skeletons)
				if(skeleton.module().enabled) {
					ResourceKey<Biome> resourceKey = ResourceKey.create(Registries.BIOME, skeleton.biome());
					consumer.accept(Pair.of(skeleton.climate(), resourceKey));
				}
		}

		public void addUndergroundBiome(UndergroundBiomeSkeleton skeleton) {
			skeletons.add(skeleton);
		}

	}

	public record UndergroundBiomeSkeleton(ZetaModule module, Climate.ParameterPoint climate, ResourceLocation biome) {}

}
