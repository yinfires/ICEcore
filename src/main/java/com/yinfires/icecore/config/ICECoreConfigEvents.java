package com.yinfires.icecore.config;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.food.ClientBoundFoodConsumptionConfigPacket;
import com.yinfires.icecore.network.ICECoreNetwork;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.server.MinecraftServer;

@Mod.EventBusSubscriber(modid = ICECore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ICECoreConfigEvents {
    // Accessed only on the server thread. Login already sends the current snapshot to new players.
    private static ClientBoundFoodConsumptionConfigPacket lastBroadcast;
    private ICECoreConfigEvents() {
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != ICECoreConfig.SPEC) {
            return;
        }
        synchronize(ServerLifecycleHooks.getCurrentServer());
    }

    public static void synchronize(MinecraftServer server) {
        if (server == null) {
            return;
        }
        server.execute(() -> {
            if (server != ServerLifecycleHooks.getCurrentServer()) {
                return;
            }
            ICECoreConfig.SPEC.afterReload();
            var packet = new ClientBoundFoodConsumptionConfigPacket(ICECoreConfig.disableFoodConsumption(),
                    ICECoreConfig.enableModCompatibility());
            if (packet.equals(lastBroadcast)) {
                return;
            }
            lastBroadcast = packet;
            for (var player : server.getPlayerList().getPlayers()) {
                ICECoreNetwork.sendToPlayer(packet, player);
            }
        });
    }
}
