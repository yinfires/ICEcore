package com.yinfires.icecore.quest.objective;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * A single completion condition of a quest. Implementations are parsed from JSON by an
 * {@link ObjectiveType} and carry the target count only; live progress counts are stored
 * per owner in the quest progress data, keyed by objective index.
 *
 * <p>Objective instances are immutable definitions shared across owners. Any event-driven
 * progress tracking is registered by the {@link ObjectiveType} and must only inspect owners
 * that actually have an ACTIVE quest containing this objective type, to stay off hot paths.
 */
public interface QuestObjective {
    /** Registry id of the objective type, e.g. {@code icecore:talk_to_npc}. */
    ResourceLocation type();

    /** Target count that satisfies this objective (>= 1). */
    int targetCount();

    /**
     * Description label of this objective, WITHOUT any count. Progress ({@code current/target}) is
     * rendered separately by the UI, so this must never embed the count. May be empty when the
     * author gave no description (the UI then shows only the progress).
     */
    Component label();
}
