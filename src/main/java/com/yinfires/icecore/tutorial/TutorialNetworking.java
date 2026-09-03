package com.yinfires.icecore.tutorial;

import com.yinfires.icecore.network.ClientBoundTutorialSyncPacket;
import com.yinfires.icecore.network.ICECoreNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

/** Builds and sends the client tutorial snapshot (unlocked states). Installed as sync hook. */
public final class TutorialNetworking {
    private TutorialNetworking() {
    }

    public static void install() {
        TutorialService.installSyncHook(TutorialNetworking::syncPlayer);
    }

    public static void syncPlayer(ServerPlayer player) {
        if (!TutorialService.isReady()) {
            return;
        }
        Map<ResourceLocation, TutorialState> states = new HashMap<>(TutorialService.data().statesFor(player.getUUID()));
        ICECoreNetwork.sendToPlayer(new ClientBoundTutorialSyncPacket(states), player);
    }
}
