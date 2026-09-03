package com.yinfires.icecore.journal;

/** Which journal a category belongs to. */
public enum JournalType {
    QUEST,
    TUTORIAL;

    public static JournalType fromString(String value) {
        return "tutorial".equalsIgnoreCase(value) ? TUTORIAL : QUEST;
    }
}
