package com.yinfires.icecore.config;

import com.yinfires.icecore.ICECore;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ICECore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ICECoreClientConfigEvents {
    private ICECoreClientConfigEvents() {
    }

    @SubscribeEvent
    public static void onScreenClosed(ScreenEvent.Closing event) {
        // Closing fires before Screen.removed(), which some editors use to save their values.
        Minecraft.getInstance().tell(ICECoreClientConfigEvents::refreshAfterSave);
    }

    private static void refreshAfterSave() {
        if (ICECoreClientConfig.SPEC.isLoaded()) {
            ICECoreClientConfig.SPEC.afterReload();
        }
        if (ICECoreConfig.SPEC.isLoaded()) {
            ICECoreConfig.SPEC.afterReload();
        }
        // A remote client's local COMMON config must never override the server's rules.
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null && ICECoreConfig.SPEC.isLoaded()) {
            ICECoreConfigEvents.synchronize(server);
        }
    }
}
