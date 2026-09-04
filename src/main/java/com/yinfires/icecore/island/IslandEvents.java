package com.yinfires.icecore.island;

import com.yinfires.icecore.ICECore;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Wires the island manager into the server lifecycle and pumps the placement queue each server tick. */
@Mod.EventBusSubscriber(modid = ICECore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IslandEvents {
    private IslandEvents() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        IslandManager.get().start(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        IslandManager.get().stop();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            IslandPlacer.tick();
        }
    }
}
