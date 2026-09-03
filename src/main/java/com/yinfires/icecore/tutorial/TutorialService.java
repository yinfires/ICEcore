package com.yinfires.icecore.tutorial;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Server-authoritative tutorial API: unlock (LOCKED -> UNLOCKED_UNREAD) and mark read
 * (UNLOCKED_UNREAD -> UNLOCKED_READ). Opening a tutorial's detail in the UI marks it read.
 * State is per-player; pushes a client sync via the installed {@link TutorialSyncHook}.
 */
public final class TutorialService {
    private static MinecraftServer server;
    private static TutorialPlayerData data;
    private static TutorialSyncHook syncHook = TutorialSyncHook.NOOP;

    private TutorialService() {
    }

    public static void installSyncHook(TutorialSyncHook hook) {
        syncHook = hook == null ? TutorialSyncHook.NOOP : hook;
    }

    public static void start(MinecraftServer value) {
        server = value;
        data = value.overworld().getDataStorage()
                .computeIfAbsent(TutorialPlayerData::load, TutorialPlayerData::new, TutorialPlayerData.ID);
    }

    public static void stop() {
        server = null;
        data = null;
    }

    public static boolean isReady() {
        return server != null && data != null;
    }

    public static TutorialPlayerData data() {
        return data;
    }

    public static TutorialState state(ServerPlayer player, ResourceLocation tutorial) {
        return isReady() ? data.state(player.getUUID(), tutorial) : TutorialState.LOCKED;
    }

    /** Unlocks a tutorial with a NEW badge if it is currently LOCKED. */
    public static boolean unlock(ServerPlayer player, ResourceLocation tutorial) {
        if (!isReady() || TutorialDefinitionManager.INSTANCE.get(tutorial) == null) {
            return false;
        }
        UUID id = player.getUUID();
        if (data.state(id, tutorial) != TutorialState.LOCKED) {
            return false;
        }
        data.setState(id, tutorial, TutorialState.UNLOCKED_UNREAD);
        sync(player);
        return true;
    }

    /** Marks an unlocked tutorial read (removes the NEW badge). No-op if locked or already read. */
    public static boolean markRead(ServerPlayer player, ResourceLocation tutorial) {
        if (!isReady()) {
            return false;
        }
        UUID id = player.getUUID();
        if (data.state(id, tutorial) != TutorialState.UNLOCKED_UNREAD) {
            return false;
        }
        data.setState(id, tutorial, TutorialState.UNLOCKED_READ);
        sync(player);
        return true;
    }

    /** Resets a tutorial to LOCKED for this player. */
    public static boolean reset(ServerPlayer player, ResourceLocation tutorial) {
        if (!isReady()) {
            return false;
        }
        boolean changed = data.setState(player.getUUID(), tutorial, TutorialState.LOCKED);
        if (changed) {
            sync(player);
        }
        return changed;
    }

    public static void resetAll(ServerPlayer player) {
        if (!isReady()) {
            return;
        }
        data.statesFor(player.getUUID()).clear();
        data.setDirty();
        sync(player);
    }

    private static void sync(ServerPlayer player) {
        syncHook.syncPlayer(player);
    }
}
