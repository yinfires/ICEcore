package com.yinfires.icecore.tutorial;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Server-authoritative tutorial API: unlock (LOCKED -> UNLOCKED_UNREAD) and mark read
 * (UNLOCKED_UNREAD -> UNLOCKED_READ). Opening a tutorial's detail in the UI marks it read.
 * Unlocks follow definition scope; read state is always per-player.
 */
public final class TutorialService {
    private static MinecraftServer server;
    private static TutorialPlayerData data;
    private static TutorialGlobalData global;
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
        global = value.overworld().getDataStorage()
                .computeIfAbsent(TutorialGlobalData::load, TutorialGlobalData::new, TutorialGlobalData.ID);
    }

    public static void stop() {
        server = null;
        data = null;
        global = null;
    }

    public static boolean isReady() {
        return server != null && data != null && global != null;
    }

    public static TutorialPlayerData data() {
        return data;
    }

    public static TutorialState state(ServerPlayer player, ResourceLocation tutorial) {
        if (!isReady()) return TutorialState.LOCKED;
        TutorialDefinition def = TutorialDefinitionManager.INSTANCE.get(tutorial);
        if (def == null) return TutorialState.LOCKED;
        return def.scope() == com.yinfires.icecore.quest.QuestScope.GLOBAL
                ? global.state(data, player.getUUID(), tutorial) : data.state(player.getUUID(), tutorial);
    }

    /** Unlocks a tutorial with a NEW badge if it is currently LOCKED. */
    public static boolean unlock(ServerPlayer player, ResourceLocation tutorial) {
        if (!isReady() || TutorialDefinitionManager.INSTANCE.get(tutorial) == null) {
            return false;
        }
        UUID id = player.getUUID();
        if (isGlobal(tutorial)) {
            boolean changed = global.unlock(tutorial);
            if (changed) syncAll();
            return changed;
        }
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
        if (state(player, tutorial) != TutorialState.UNLOCKED_UNREAD) {
            return false;
        }
        if (isGlobal(tutorial)) data.markGlobalRead(id, tutorial, global.generation(tutorial));
        else data.setState(id, tutorial, TutorialState.UNLOCKED_READ);
        sync(player);
        return true;
    }

    /** Resets a tutorial to LOCKED for this player. */
    public static boolean reset(ServerPlayer player, ResourceLocation tutorial) {
        if (!isReady()) {
            return false;
        }
        if (isGlobal(tutorial)) {
            boolean changed = global.reset(tutorial);
            if (changed) syncAll();
            return changed;
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
        data.clearGlobalReads(player.getUUID());
        data.setDirty();
        sync(player);
    }

    private static void sync(ServerPlayer player) {
        syncHook.syncPlayer(player);
    }

    private static boolean isGlobal(ResourceLocation id) {
        TutorialDefinition def = TutorialDefinitionManager.INSTANCE.get(id);
        return def != null && def.scope() == com.yinfires.icecore.quest.QuestScope.GLOBAL;
    }

    private static void syncAll() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) sync(player);
    }

    public static void reconcile() {
        if (isReady()) TutorialDefinitionManager.INSTANCE.all().values().forEach(def -> global.migrate(def, data));
    }

    public static java.util.Map<ResourceLocation, TutorialState> snapshot(ServerPlayer player) {
        java.util.Map<ResourceLocation, TutorialState> result = new java.util.HashMap<>();
        for (ResourceLocation id : TutorialDefinitionManager.INSTANCE.ids()) {
            TutorialState state = state(player, id);
            if (state != TutorialState.LOCKED) result.put(id, state);
        }
        return result;
    }
}
