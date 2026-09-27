package com.yinfires.icecore.food;

import com.yinfires.icecore.ICECore;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ICECore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FoodConsumptionClientInputEvents {
    private FoodConsumptionClientInputEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || !FoodConsumptionClientState.disabled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        // Let the normal block interaction pipeline run first. Food blocks, containers and
        // other dual-purpose items must remain placeable/interactable while consumption is blocked.
        if (minecraft.hitResult instanceof BlockHitResult || minecraft.hitResult instanceof EntityHitResult) {
            return;
        }
        if (FoodConsumptionRules.isFood(minecraft.player.getItemInHand(event.getHand()), minecraft.player)) {
            event.setCanceled(true);
            event.setSwingHand(false);
            minecraft.player.stopUsingItem();
        }
    }
}
