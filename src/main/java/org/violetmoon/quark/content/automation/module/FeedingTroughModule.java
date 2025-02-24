package org.violetmoon.quark.content.automation.module;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;

import org.jetbrains.annotations.Nullable;

import org.joml.Vector3i;
import org.violetmoon.quark.base.Quark;
import org.violetmoon.quark.base.config.Config;
import org.violetmoon.quark.content.automation.block.FeedingTroughBlock;
import org.violetmoon.quark.content.automation.block.be.FeedingTroughBlockEntity;
import org.violetmoon.zeta.event.bus.LoadEvent;
import org.violetmoon.zeta.event.bus.PlayEvent;
import org.violetmoon.zeta.event.load.ZRegister;
import org.violetmoon.zeta.event.play.entity.ZEntityJoinLevel;
import org.violetmoon.zeta.event.play.entity.living.ZBabyEntitySpawn;
import org.violetmoon.zeta.module.ZetaLoadModule;
import org.violetmoon.zeta.module.ZetaModule;
import org.violetmoon.zeta.util.Hint;

import java.util.Set;
import java.util.function.Predicate;

/**
 * @author WireSegal
 * Created at 9:48 AM on 9/20/19.
 */
@ZetaLoadModule(category = "automation")
public class FeedingTroughModule extends ZetaModule {
	
	public static BlockEntityType<FeedingTroughBlockEntity> blockEntityType;
	public static PoiType feedingTroughPoi;
	@Hint Block feeding_trough;
	
	private static final String TAG_CACHE = "quark:feedingTroughCache";

	public static final Predicate<Holder<PoiType>> IS_FEEDER = (holder) -> holder.is(BuiltInRegistries.POINT_OF_INTEREST_TYPE.getKey(feedingTroughPoi));

	@Config(description = "How long, in game ticks, between animals being able to eat from the trough")
	@Config.Min(1)
	public static int cooldown = 30;

	@Config(description = "The maximum amount of animals allowed around the trough's range for an animal to enter love mode")
	public static int maxAnimals = 32;

	@Config(description = "The chance (between 0 and 1) for an animal to enter love mode when eating from the trough")
	@Config.Min(value = 0.0, exclusive = true)
	@Config.Max(1.0)
	public static double loveChance = 0.333333333;

	@Config public static double range = 10;
	
	@Config(description = "Set to false to make it so animals look for a nearby trough every time they want to eat instead of remembering the last one. Can affect performance if false.")
	public static boolean enableTroughCaching = true;

	private static final ThreadLocal<Boolean> breedingOccurred = ThreadLocal.withInitial(() -> false);

	@PlayEvent
	public void onBreed(ZBabyEntitySpawn.Lowest event) {
		if (event.getCausedByPlayer() == null && event.getParentA().level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT))
			breedingOccurred.set(true);
	}

	@PlayEvent
	public void onOrbSpawn(ZEntityJoinLevel event) {
		if (event.getEntity() instanceof ExperienceOrb && breedingOccurred.get()) {
			event.setCanceled(true);
			breedingOccurred.remove();
		}
	}

	public static Player temptWithTroughs(TemptGoal goal, Player found, ServerLevel level) {
		if (!Quark.ZETA.modules.isEnabled(FeedingTroughModule.class) ||
				(found != null && (goal.items.test(found.getMainHandItem()) || goal.items.test(found.getOffhandItem()))))
			return found;

		if (!(goal.mob instanceof Animal animal) ||
				!animal.canFallInLove() ||
				animal.getAge() != 0)
			return found;

		Vec3 position = animal.position();
		TroughPointer pointer = null;
		
		boolean cached = false;
		if(enableTroughCaching) {
			TroughPointer candidate = TroughPointer.fromEntity(animal, goal);
			if(candidate != null) {
				pointer = candidate;
				cached = true;
			}
		}
		
		if(!cached)
			pointer = level.getPoiManager().findAllClosestFirstWithType(
				IS_FEEDER, p -> p.distSqr(new Vec3i((int) position.x, (int) position.y, (int) position.z)) <= range * range,
				animal.blockPosition(), (int) range, PoiManager.Occupancy.ANY)
				.map(Pair::getSecond)
				.map(pos -> getTroughFakePlayer(level, pos, goal))
				.filter(Predicates.notNull())
				.findFirst()
				.orElse(null);

		if(pointer != null && pointer.exists()) {
			BlockPos location = pointer.pos();
			Vec3 eyesPos = goal.mob.position().add(0, goal.mob.getEyeHeight(), 0);
			Vec3 targetPos = new Vec3(location.getX(), location.getY(), location.getZ()).add(0.5, 0.0625, 0.5);
			BlockHitResult ray = goal.mob.level().clip(new ClipContext(eyesPos, targetPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, goal.mob));

			if (ray.getType() == HitResult.Type.BLOCK && ray.getBlockPos().equals(location)) {
				if(!cached)
					pointer.save(animal);
				
				return pointer.player();
			}
		}
		
		// if we got here that means the cache is invalid
		if(cached)
			animal.getPersistentData().remove(TAG_CACHE);

		return found;
	}
	
	private static @Nullable TroughPointer getTroughFakePlayer(Level level, BlockPos pos, TemptGoal goal) {
		if(level.getBlockEntity(pos) instanceof FeedingTroughBlockEntity trough)
			return new TroughPointer(pos, trough.getFoodHolder(goal));
		
		return null;
	}

	@LoadEvent
	public final void register(ZRegister event) {
		feeding_trough = new FeedingTroughBlock("feeding_trough", this,
				Block.Properties.of().mapColor(MapColor.WOOD).ignitedByLava().strength(0.6F).sound(SoundType.WOOD));

		blockEntityType = BlockEntityType.Builder.of(FeedingTroughBlockEntity::new, feeding_trough).build(null);
		Quark.ZETA.registry.register(blockEntityType, "feeding_trough", Registries.BLOCK_ENTITY_TYPE);

		feedingTroughPoi = new PoiType(getBlockStates(feeding_trough), 1, 32);
		Quark.ZETA.registry.register(feedingTroughPoi, "feeding_trough", Registries.POINT_OF_INTEREST_TYPE);
	}

	private static Set<BlockState> getBlockStates(Block p_218074_) {
		return ImmutableSet.copyOf(p_218074_.getStateDefinition().getPossibleStates());
	}
	
	private record TroughPointer(BlockPos pos, FakePlayer player) {
		
		public boolean exists() {
			return player != null;
		}
		
		public void save(Entity e) {
			CompoundTag data = e.getPersistentData();
			CompoundTag tag = new CompoundTag();
			tag.putInt("x", pos.getX());
			tag.putInt("y", pos.getY());
			tag.putInt("z", pos.getZ());
			
			data.put(TAG_CACHE, tag);
		}
		
		public static TroughPointer fromEntity(Entity e, TemptGoal goal) {
			CompoundTag data = e.getPersistentData();
			if(!data.contains(TAG_CACHE, data.getId()))
				return null;
			
			CompoundTag tag = data.getCompound(TAG_CACHE);
			int x = tag.getInt("x");
			int y = tag.getInt("y");
			int z = tag.getInt("z");
			
			BlockPos pos = new BlockPos(x, y, z);
			return getTroughFakePlayer(e.level(), pos, goal);
		}
		
	}
}
