package com.yinfires.icecore.recipehide;

import com.yinfires.icecore.ICECore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ICECore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RecipeHideEvents {
    private RecipeHideEvents() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        RecipeHideService.start(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        RecipeHideService.stop();
    }

    /** Fires on player login (with a player) and on {@code /reload} (player null). */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null) {
            // Recipe set may have changed on reload; recompute and push to everyone.
            RecipeHideService.recomputeRegistry();
            RecipeHideService.broadcast();
        } else {
            RecipeHideService.sendTo(player);
        }
    }
}
