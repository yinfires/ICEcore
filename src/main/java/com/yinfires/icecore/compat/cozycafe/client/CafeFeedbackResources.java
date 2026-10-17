package com.yinfires.icecore.compat.cozycafe.client;

import com.yinfires.icecore.ICECore;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public interface CafeFeedbackResources {
    void icecore$releaseFeedbackTarget();

    @Mod.EventBusSubscriber(modid = ICECore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    final class Events {
        private Events() {}
        @SubscribeEvent public static void onClose(ScreenEvent.Closing event) {
            if (event.getScreen() instanceof CafeFeedbackResources resources) resources.icecore$releaseFeedbackTarget();
        }
    }
}
