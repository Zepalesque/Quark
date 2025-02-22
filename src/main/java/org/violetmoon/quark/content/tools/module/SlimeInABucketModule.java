package org.violetmoon.quark.content.tools.module;

import org.violetmoon.quark.content.tools.item.SlimeInABucketItem;
import org.violetmoon.zeta.client.event.load.ZClientSetup;
import org.violetmoon.zeta.event.bus.LoadEvent;
import org.violetmoon.zeta.event.bus.PlayEvent;
import org.violetmoon.zeta.event.load.ZRegister;
import org.violetmoon.zeta.event.play.entity.player.ZPlayerInteract;
import org.violetmoon.zeta.module.ZetaLoadModule;
import org.violetmoon.zeta.module.ZetaModule;
import org.violetmoon.zeta.util.Hint;
import org.violetmoon.zeta.util.ItemNBTHelper;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;

@ZetaLoadModule(category = "tools")
public class SlimeInABucketModule extends ZetaModule {

	@Hint public static Item slime_in_a_bucket;

	@LoadEvent
	public final void register(ZRegister event) {
		slime_in_a_bucket = new SlimeInABucketItem(this);
	}

	@PlayEvent
	public void entityInteract(ZPlayerInteract.EntityInteract event) {
		if(event.getTarget() != null) {
			if(event.getTarget().getType() == EntityType.SLIME && ((Slime) event.getTarget()).getSize() == 1 && event.getTarget().isAlive()) {
				Player player = event.getEntity();
				InteractionHand hand = InteractionHand.MAIN_HAND;
				ItemStack stack = player.getMainHandItem();
				if(stack.isEmpty() || stack.getItem() != Items.BUCKET) {
					stack = player.getOffhandItem();
					hand = InteractionHand.OFF_HAND;
				}

				if(!stack.isEmpty() && stack.getItem() == Items.BUCKET) {
					if(!event.getLevel().isClientSide) {
						ItemStack outStack = new ItemStack(slime_in_a_bucket);
						CompoundTag cmp = event.getTarget().serializeNBT();
						ItemNBTHelper.setCompound(outStack, SlimeInABucketItem.TAG_ENTITY_DATA, cmp);
						
						if(stack.getCount() == 1)
							player.setItemInHand(hand, outStack);
						else {
							stack.shrink(1);
							if(stack.getCount() == 0)
								player.setItemInHand(hand, outStack);
							else if(!player.getInventory().add(outStack))
								player.drop(outStack, false);
						}

			            event.getLevel().gameEvent(player, GameEvent.ENTITY_INTERACT, event.getTarget().position());
						event.getTarget().discard();
					}
					else player.swing(hand);
					
					event.setCanceled(true);
					event.setCancellationResult(InteractionResult.SUCCESS);
				}
			}
		}
	}

	@ZetaLoadModule(clientReplacement = true)
	public static class Client extends SlimeInABucketModule {
		@LoadEvent
		public void clientSetup(ZClientSetup event) {
			event.enqueueWork(() -> ItemProperties.register(slime_in_a_bucket, new ResourceLocation("excited"),
					(stack, world, e, id) -> ItemNBTHelper.getBoolean(stack, SlimeInABucketItem.TAG_EXCITED, false) ? 1 : 0));
		}
	}
}
