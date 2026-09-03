package com.yinfires.icecore.tutorial.client;

import com.yinfires.icecore.tutorial.TutorialState;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side cache of the player's unlocked tutorials and their read state, mirrored from the
 * server. Static, reset on logout. Read by the tutorial journal screen (NEW badge, list filter).
 */
public final class TutorialClientState {
    private static final Map<ResourceLocation, TutorialState> STATES = new HashMap<>();
    private static boolean initialized;

    private TutorialClientState() {
    }

    public static void accept(Map<ResourceLocation, TutorialState> states) {
        STATES.clear();
        STATES.putAll(states);
        initialized = true;
    }

    /**
     * Optimistically marks an unlocked tutorial read on the client so the NEW badge disappears the
     * instant it is opened, without waiting for the server round-trip. The server sync confirms it.
     */
    public static void markReadLocal(ResourceLocation tutorial) {
        if (STATES.get(tutorial) == TutorialState.UNLOCKED_UNREAD) {
            STATES.put(tutorial, TutorialState.UNLOCKED_READ);
        }
    }

    public static TutorialState state(ResourceLocation tutorial) {
        return STATES.getOrDefault(tutorial, TutorialState.LOCKED);
    }

    public static boolean isUnlocked(ResourceLocation tutorial) {
        return state(tutorial) != TutorialState.LOCKED;
    }

    public static boolean hasUnread() {
        for (TutorialState state : STATES.values()) {
            if (state == TutorialState.UNLOCKED_UNREAD) {
                return true;
            }
        }
        return false;
    }

    public static Map<ResourceLocation, TutorialState> states() {
        return STATES;
    }

    public static boolean initialized() {
        return initialized;
    }

    public static void reset() {
        STATES.clear();
        initialized = false;
    }
}
