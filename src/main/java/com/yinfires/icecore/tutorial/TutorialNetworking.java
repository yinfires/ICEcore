package com.yinfires.icecore.tutorial;

import com.yinfires.icecore.network.ClientBoundTutorialSyncPacket;
import com.yinfires.icecore.network.ICECoreNetwork;
import net.minecraft.server.level.ServerPlayer;


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
        ICECoreNetwork.sendToPlayer(new ClientBoundTutorialSyncPacket(TutorialService.snapshot(player)), player);
    }
}
