package org.violetmoon.quark.content.world.gen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.violetmoon.quark.base.Quark;
import org.violetmoon.quark.base.world.generator.multichunk.ClusterBasedGenerator;
import org.violetmoon.quark.content.world.config.AirStoneClusterConfig;
import org.violetmoon.quark.content.world.config.BigStoneClusterConfig;
import org.violetmoon.quark.content.world.module.BigStoneClustersModule;

import java.util.Objects;
import java.util.Random;
import java.util.function.BooleanSupplier;

public class BigStoneClusterGenerator extends ClusterBasedGenerator {

	private final BigStoneClusterConfig config;
	private final BlockState placeState;

	public BigStoneClusterGenerator(BigStoneClusterConfig config, BlockState placeState, BooleanSupplier condition) {
		super(config.dimensions, () -> config.enabled && condition.getAsBoolean(), config, Objects.toString(Quark.ZETA.registry.getRegistryName(placeState.getBlock(), BuiltInRegistries.BLOCK)).hashCode());
		this.config = config;
		this.placeState = placeState;
	}

	@Override
	public boolean isSourceValid(WorldGenRegion world, ChunkGenerator generator, BlockPos pos) {
		return config.biomes.canSpawn(getBiome(world, pos, true));
	}

	@Override
	public BlockPos[] getSourcesInChunk(WorldGenRegion world, Random random, ChunkGenerator generator, BlockPos chunkLeft) {
		int chance = config.rarity;

		BlockPos[] sources;
		if(chance > 0 && random.nextInt(chance) == 0) {
			sources = new BlockPos[1];
			int lower = config.minYLevel;
			int range = Math.abs(config.maxYLevel - config.minYLevel);

			BlockPos pos = chunkLeft.offset(random.nextInt(16), random.nextInt(range) + lower, random.nextInt(16));
			sources[0] = pos;
		} else sources = new BlockPos[0];

		return sources;
	}	

	@Override
	public String toString() {
		return "BigStoneClusterGenerator[" + placeState + "]";
	}

	@Override
	public IGenerationContext createContext(BlockPos src, ChunkGenerator generator, Random random, BlockPos chunkCorner, WorldGenRegion world) {
		return (pos, noise) -> {
			if(canPlaceBlock(world, pos))
				world.setBlock(pos, placeState, 0);
		};
	}

	private boolean canPlaceBlock(ServerLevelAccessor world, BlockPos pos) {
		if(config instanceof AirStoneClusterConfig clusterConfig && clusterConfig.generateInAir)
			return world.getBlockState(pos).isAir();

		return BigStoneClustersModule.blockReplacePredicate.test(world.getLevel(), world.getBlockState(pos).getBlock());
	}

}
