package com.yinfires.icecore.quest.client;

import com.yinfires.icecore.quest.QuestSnapshot;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client-side cache of the player's ACTIVE quests and single marked quest id, mirrored from the
 * server. Static, reset on logout. Read by the quest journal screen and tracker HUD.
 */
public final class QuestClientState {
    private static final Map<ResourceLocation, QuestSnapshot> ACTIVE = new LinkedHashMap<>();
    // Active quests the player has already viewed in the quest screen; drives the "new quest" hint.
    private static final java.util.Set<ResourceLocation> SEEN = new java.util.HashSet<>();
    @Nullable
    private static ResourceLocation marked;
    private static boolean initialized;

    private QuestClientState() {
    }

    public static void accept(java.util.List<QuestSnapshot> snapshots, @Nullable ResourceLocation markedId) {
        ACTIVE.clear();
        for (QuestSnapshot snapshot : snapshots) {
            ACTIVE.put(snapshot.id(), snapshot);
        }
        // Drop seen ids that are no longer active so the set stays bounded.
        SEEN.retainAll(ACTIVE.keySet());
        marked = markedId;
        initialized = true;
    }

    /** Marks one active quest as seen (called when that quest's entry is opened), clearing its NEW dot. */
    public static void markSeen(ResourceLocation questId) {
        SEEN.add(questId);
    }

    /** True if this active quest still carries a NEW dot (unlocked but not yet opened). */
    public static boolean isUnseen(ResourceLocation questId) {
        return ACTIVE.containsKey(questId) && !SEEN.contains(questId);
    }

    /** True if some active quest still carries a NEW dot. Drives the "new quest" hint. */
    public static boolean hasUnseen() {
        for (ResourceLocation id : ACTIVE.keySet()) {
            if (!SEEN.contains(id)) {
                return true;
            }
        }
        return false;
    }

    public static Map<ResourceLocation, QuestSnapshot> active() {
        return ACTIVE;
    }

    @Nullable
    public static QuestSnapshot marked() {
        return marked == null ? null : ACTIVE.get(marked);
    }

    @Nullable
    public static ResourceLocation markedId() {
        return marked;
    }

    public static boolean isMarked(ResourceLocation questId) {
        return questId.equals(marked);
    }

    public static boolean initialized() {
        return initialized;
    }

    public static void reset() {
        ACTIVE.clear();
        SEEN.clear();
        marked = null;
        initialized = false;
    }
}
