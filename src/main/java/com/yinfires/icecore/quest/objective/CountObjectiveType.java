package com.yinfires.icecore.quest.objective;

import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/**
 * Generic count-based objective: advances from 0 to a target count. Reused by several kinds
 * (talk_to_npc, command, ...) that differ only in what event increments them; that wiring lives
 * in the linkage phase, not here. An optional {@code target} string (e.g. an NPC id) is retained
 * for event matching and an optional {@code desc} translation key drives the progress label.
 */
public final class CountObjectiveType implements ObjectiveType {
    private final ResourceLocation id;
    private final String targetField;

    public CountObjectiveType(ResourceLocation id, String targetField) {
        this.id = id;
        this.targetField = targetField;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public QuestObjective parse(JsonObject json) {
        int count = GsonHelper.getAsInt(json, "count", 1);
        if (count < 1) {
            throw new com.google.gson.JsonParseException("objective count must be >= 1");
        }
        String target = targetField == null ? "" : GsonHelper.getAsString(json, targetField, "");
        // desc is a JournalText source: a translation key or a "literal:" prefixed literal string.
        com.yinfires.icecore.journal.JournalText desc =
                new com.yinfires.icecore.journal.JournalText(GsonHelper.getAsString(json, "desc", ""));
        return new CountObjective(id, count, target, desc);
    }

    /** Immutable count objective instance. */
    public static final class CountObjective implements QuestObjective {
        private final ResourceLocation type;
        private final int target;
        private final String matchTarget;
        private final com.yinfires.icecore.journal.JournalText desc;

        CountObjective(ResourceLocation type, int target, String matchTarget,
                       com.yinfires.icecore.journal.JournalText desc) {
            this.type = type;
            this.target = target;
            this.matchTarget = matchTarget;
            this.desc = desc;
        }

        @Override
        public ResourceLocation type() {
            return type;
        }

        @Override
        public int targetCount() {
            return target;
        }

        /** Optional match target (e.g. NPC definition id) used by event-driven progress. */
        public String matchTarget() {
            return matchTarget;
        }

        @Override
        public Component label() {
            // Resolves translation key or strips the "literal:" prefix; never embeds the count.
            return desc.isBlank() ? Component.empty() : desc.resolve();
        }
    }
}
