package com.yinfires.icecore.quest;

/**
 * Lifecycle of a quest for a given owner (player or global).
 * HIDDEN quests are not shown; COMPLETE quests are hidden again but their state is
 * retained for prerequisite chains and non-repeatable acquisition guards.
 */
public enum QuestState {
    HIDDEN,
    ACTIVE,
    COMPLETE;

    public static QuestState fromName(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException ex) {
            return HIDDEN;
        }
    }
}
