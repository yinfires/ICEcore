package com.yinfires.icecore.quest;

import net.minecraft.server.level.ServerPlayer;

/**
 * Installed by the networking layer to push quest state to clients. Kept as an interface so the
 * server-side service does not depend on client packets; the default is a no-op for headless use.
 */
public interface QuestSyncHook {
    QuestSyncHook NOOP = player -> {
    };

    void syncPlayer(ServerPlayer player);
}
