package com.yinfires.icecore.tutorial;

/**
 * Lifecycle of a tutorial entry for a player. LOCKED entries are not shown; unlocked
 * entries show with a NEW badge until the player opens their detail once.
 */
public enum TutorialState {
    LOCKED,
    UNLOCKED_UNREAD,
    UNLOCKED_READ;

    public static TutorialState fromName(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException ex) {
            return LOCKED;
        }
    }
}
