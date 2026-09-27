package com.yinfires.icecore.config;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.food.ClientBoundFoodConsumptionConfigPacket;
import com.yinfires.icecore.network.ICECoreNetwork;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = ICECore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ICECoreConfigEvents {
    private ICECoreConfigEvents() {
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != ICECoreConfig.SPEC) {
            return;
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        var packet = new ClientBoundFoodConsumptionConfigPacket(ICECoreConfig.disableFoodConsumption(),
                ICECoreConfig.enableModCompatibility());
        for (var player : server.getPlayerList().getPlayers()) {
            ICECoreNetwork.sendToPlayer(packet, player);
        }
    }
}
