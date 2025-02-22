package org.violetmoon.quark.content.tweaks.client.emote;

import org.violetmoon.quark.content.tweaks.module.EmotesModule;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.player.Player;

public class CustomEmote extends TemplateSourcedEmote {

	public CustomEmote(EmoteDescriptor desc, Player player, HumanoidModel<?> model, HumanoidModel<?> armorModel, HumanoidModel<?> armorLegsModel) {
		super(desc, player, model, armorModel, armorLegsModel);
	}

	@Override
	public boolean shouldLoadTimelineOnLaunch() {
		return EmotesModule.customEmoteDebug;
	}

}
