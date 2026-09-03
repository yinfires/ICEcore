package com.yinfires.icecore.quest;

/** Where a quest's progress is stored: per-player or shared by the whole server. */
public enum QuestScope {
    PLAYER,
    GLOBAL;

    public static QuestScope fromString(String value) {
        return "global".equalsIgnoreCase(value) ? GLOBAL : PLAYER;
    }

    public String serialize() {
        return this == GLOBAL ? "global" : "player";
    }
}
