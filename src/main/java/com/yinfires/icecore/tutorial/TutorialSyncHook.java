package com.yinfires.icecore.tutorial;

import net.minecraft.server.level.ServerPlayer;

/** Installed by the networking layer to push tutorial state to clients. Default is a no-op. */
public interface TutorialSyncHook {
    TutorialSyncHook NOOP = player -> {
    };

    void syncPlayer(ServerPlayer player);
}
