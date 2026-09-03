package com.yinfires.icecore.quest;

import com.yinfires.icecore.quest.objective.QuestObjective;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative quest API: acquire, progress, complete, mark, reset. Progress is routed to
 * {@link QuestPlayerData} or {@link QuestGlobalData} by the quest's {@link QuestScope}; marking is
 * always per-player. Reward/side-effect command lists run with permission level 2. All mutations
 * push a client sync via the installed {@link QuestSyncHook}.
 */
public final class QuestService {
    private static MinecraftServer server;
    private static QuestPlayerData playerData;
    private static QuestGlobalData globalData;
    private static QuestSyncHook syncHook = QuestSyncHook.NOOP;

    private QuestService() {
    }

    public static void installSyncHook(QuestSyncHook hook) {
        syncHook = hook == null ? QuestSyncHook.NOOP : hook;
    }

    public static void start(MinecraftServer value) {
        server = value;
        playerData = value.overworld().getDataStorage()
                .computeIfAbsent(QuestPlayerData::load, QuestPlayerData::new, QuestPlayerData.ID);
        globalData = value.overworld().getDataStorage()
                .computeIfAbsent(QuestGlobalData::load, QuestGlobalData::new, QuestGlobalData.ID);
    }

    public static void stop() {
        server = null;
        playerData = null;
        globalData = null;
    }

    public static boolean isReady() {
        return server != null && playerData != null && globalData != null;
    }

    @Nullable
    public static QuestPlayerData playerData() {
        return playerData;
    }

    @Nullable
    public static QuestGlobalData globalData() {
        return globalData;
    }

    /** Returns the progress for a quest under the given player, honouring its scope; may be null. */
    @Nullable
    public static QuestProgress progressOf(ServerPlayer player, ResourceLocation questId) {
        QuestDefinition def = QuestDefinitionManager.INSTANCE.get(questId);
        if (def == null || !isReady()) {
            return null;
        }
        return progressMap(player, def).get(questId);
    }

    private static Map<ResourceLocation, QuestProgress> progressMap(ServerPlayer player, QuestDefinition def) {
        if (def.scope() == QuestScope.GLOBAL) {
            return globalData.progress();
        }
        return playerData.progressFor(player.getUUID());
    }

    private static void markDirty(QuestDefinition def) {
        if (def.scope() == QuestScope.GLOBAL) {
            globalData.setDirty();
        } else {
            playerData.setDirty();
        }
    }

    /** Ensures a progress entry exists and its objective array matches the current definition. */
    private static QuestProgress ensureProgress(ServerPlayer player, QuestDefinition def) {
        Map<ResourceLocation, QuestProgress> map = progressMap(player, def);
        QuestProgress progress = map.computeIfAbsent(def.id(),
                key -> new QuestProgress(QuestState.HIDDEN, def.objectives().size()));
        progress.resize(def.objectives().size());
        return progress;
    }

    private static void runCommands(ServerPlayer player, List<String> commands) {
        if (commands.isEmpty() || server == null) {
            return;
        }
        var source = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        for (String command : commands) {
            if (!command.isBlank()) {
                server.getCommands().performPrefixedCommand(source, command);
            }
        }
    }


    /**
     * Acquires a quest for the player: HIDDEN/absent -> ACTIVE, runs on_accept, and makes it the
     * player's single marked quest (replacing any prior mark). Returns false if unavailable
     * (unknown, already active/complete-and-not-repeatable, or prerequisites unmet).
     */
    public static boolean acquire(ServerPlayer player, ResourceLocation questId) {
        QuestDefinition def = QuestDefinitionManager.INSTANCE.get(questId);
        if (def == null || !isReady()) {
            return false;
        }
        QuestProgress progress = ensureProgress(player, def);
        if (progress.state() == QuestState.ACTIVE) {
            return false;
        }
        if (progress.state() == QuestState.COMPLETE && !def.repeatable()) {
            return false;
        }
        if (!prerequisitesMet(player, def)) {
            return false;
        }
        progress.setState(QuestState.ACTIVE);
        for (int i = 0; i < def.objectives().size(); i++) {
            progress.setCount(i, 0, def.objectives().get(i).targetCount());
        }
        markDirty(def);
        // Acquiring a new quest always becomes the tracked quest, per design.
        playerData.setMarked(player.getUUID(), questId);
        runCommands(player, def.onAccept());
        if (def.autoComplete() && !def.hasObjectives()) {
            complete(player, questId);
        } else {
            sync(player);
        }
        return true;
    }

    /** True if all prerequisite quests are COMPLETE for this player/scope. */
    public static boolean prerequisitesMet(ServerPlayer player, QuestDefinition def) {
        for (ResourceLocation required : def.requires()) {
            if (!isComplete(player, required)) {
                return false;
            }
        }
        return true;
    }

    /** Sets an objective's progress count; auto-completes the quest when all objectives are met. */
    public static boolean setObjectiveProgress(ServerPlayer player, ResourceLocation questId, int index, int value) {
        QuestDefinition def = QuestDefinitionManager.INSTANCE.get(questId);
        if (def == null || !isReady()) {
            return false;
        }
        QuestProgress progress = progressMap(player, def).get(questId);
        if (progress == null || progress.state() != QuestState.ACTIVE
                || index < 0 || index >= def.objectives().size()) {
            return false;
        }
        int target = def.objectives().get(index).targetCount();
        if (!progress.setCount(index, value, target)) {
            return false;
        }
        markDirty(def);
        if (def.autoComplete() && allObjectivesMet(def, progress)) {
            complete(player, questId);
        } else {
            sync(player);
        }
        return true;
    }

    /** Adds to an objective's current count (event-driven progress helper). */
    public static boolean addObjectiveProgress(ServerPlayer player, ResourceLocation questId, int index, int amount) {
        QuestProgress progress = progressOf(player, questId);
        if (progress == null) {
            return false;
        }
        return setObjectiveProgress(player, questId, index, progress.count(index) + amount);
    }

    private static boolean allObjectivesMet(QuestDefinition def, QuestProgress progress) {
        List<QuestObjective> objectives = def.objectives();
        for (int i = 0; i < objectives.size(); i++) {
            if (progress.count(i) < objectives.get(i).targetCount()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Completes a quest: state -> COMPLETE (hidden from the list), runs on_complete rewards, and
     * clears the mark if it pointed here (no auto fall-back to another active quest).
     */
    public static boolean complete(ServerPlayer player, ResourceLocation questId) {
        QuestDefinition def = QuestDefinitionManager.INSTANCE.get(questId);
        if (def == null || !isReady()) {
            return false;
        }
        QuestProgress progress = ensureProgress(player, def);
        if (progress.state() == QuestState.COMPLETE) {
            return false;
        }
        progress.setState(QuestState.COMPLETE);
        markDirty(def);
        if (questId.equals(playerData.marked(player.getUUID()))) {
            playerData.setMarked(player.getUUID(), null);
        }
        runCommands(player, def.onComplete());
        sync(player);
        return true;
    }

    public static boolean isComplete(ServerPlayer player, ResourceLocation questId) {
        QuestProgress progress = progressOf(player, questId);
        return progress != null && progress.state() == QuestState.COMPLETE;
    }

    public static boolean isActive(ServerPlayer player, ResourceLocation questId) {
        QuestProgress progress = progressOf(player, questId);
        return progress != null && progress.state() == QuestState.ACTIVE;
    }

    /**
     * Sets the player's marked quest. Only an ACTIVE quest may be marked; passing null, or the
     * already-marked id, clears the mark (HUD hidden). Returns true if the mark changed.
     */
    public static boolean setMarked(ServerPlayer player, @Nullable ResourceLocation questId) {
        if (!isReady()) {
            return false;
        }
        UUID id = player.getUUID();
        if (questId == null || questId.equals(playerData.marked(id)) || !isActive(player, questId)) {
            boolean changed = playerData.setMarked(id, null);
            if (changed) {
                sync(player);
            }
            return changed;
        }
        boolean changed = playerData.setMarked(id, questId);
        if (changed) {
            sync(player);
        }
        return changed;
    }

    @Nullable
    public static ResourceLocation marked(ServerPlayer player) {
        return isReady() ? playerData.marked(player.getUUID()) : null;
    }

    /** Resets a quest's progress to HIDDEN for this player/scope; clears the mark if it pointed here. */
    public static boolean reset(ServerPlayer player, ResourceLocation questId) {
        QuestDefinition def = QuestDefinitionManager.INSTANCE.get(questId);
        if (def == null || !isReady()) {
            return false;
        }
        boolean removed = progressMap(player, def).remove(questId) != null;
        boolean unmarked = questId.equals(playerData.marked(player.getUUID()))
                && playerData.setMarked(player.getUUID(), null);
        if (removed) {
            markDirty(def);
        }
        if (removed || unmarked) {
            sync(player);
        }
        return removed || unmarked;
    }

    /** Resets every quest for the player (PLAYER scope) and clears their mark. */
    public static void resetAll(ServerPlayer player) {
        if (!isReady()) {
            return;
        }
        playerData.progressFor(player.getUUID()).clear();
        playerData.setMarked(player.getUUID(), null);
        playerData.setDirty();
        sync(player);
    }

    /**
     * Advances any of the player's ACTIVE quests whose objective matches the given type and target
     * by one. Called from gameplay hooks (e.g. talking to an NPC). Only iterates this one player's
     * active quests, so it stays off tick/render hot paths; NPC interaction is a low-frequency event.
     *
     * @param objectiveType the objective kind, e.g. {@code icecore:talk_to_npc}
     * @param matchTarget   the target the objective must match (e.g. NPC id), or empty for any
     */
    public static void notifyObjectiveEvent(ServerPlayer player, ResourceLocation objectiveType, String matchTarget) {
        if (!isReady()) {
            return;
        }
        advanceMatching(player, playerData.progressFor(player.getUUID()), objectiveType, matchTarget);
        advanceMatching(player, globalData.progress(), objectiveType, matchTarget);
    }

    private static void advanceMatching(ServerPlayer player, Map<ResourceLocation, QuestProgress> map,
                                        ResourceLocation objectiveType, String matchTarget) {
        // Copy the ids first: setObjectiveProgress may complete a quest and mutate the map.
        for (ResourceLocation questId : List.copyOf(map.keySet())) {
            QuestProgress progress = map.get(questId);
            if (progress == null || progress.state() != QuestState.ACTIVE) {
                continue;
            }
            QuestDefinition def = QuestDefinitionManager.INSTANCE.get(questId);
            if (def == null) {
                continue;
            }
            List<QuestObjective> objectives = def.objectives();
            for (int i = 0; i < objectives.size(); i++) {
                QuestObjective objective = objectives.get(i);
                if (!objective.type().equals(objectiveType) || progress.count(i) >= objective.targetCount()) {
                    continue;
                }
                if (matches(objective, matchTarget)) {
                    addObjectiveProgress(player, questId, i, 1);
                }
            }
        }
    }

    private static boolean matches(QuestObjective objective, String matchTarget) {
        if (matchTarget == null || matchTarget.isEmpty()) {
            return true;
        }
        if (objective instanceof com.yinfires.icecore.quest.objective.CountObjectiveType.CountObjective count) {
            String target = count.matchTarget();
            return target.isEmpty() || target.equals(matchTarget);
        }
        return true;
    }

    private static void sync(ServerPlayer player) {
        syncHook.syncPlayer(player);
    }
}
