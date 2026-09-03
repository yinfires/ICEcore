package com.yinfires.icecore.quest;

import com.yinfires.icecore.journal.JournalImage;
import com.yinfires.icecore.journal.JournalText;
import com.yinfires.icecore.quest.objective.QuestObjective;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Immutable, datapack-driven quest definition. Title/content use {@link JournalText} so authors
 * may use translation keys or literal text. Rewards and side effects are expressed as command
 * lists ({@code onAccept}/{@code onComplete}) run server-side with permission level 2; no reward
 * type is hard-coded, keeping content editable purely from the datapack.
 */
public record QuestDefinition(ResourceLocation id, JournalText title, JournalText content,
                              @Nullable ResourceLocation category, @Nullable ResourceLocation icon,
                              int order, QuestScope scope, boolean repeatable,
                              boolean autoUnlockOnPrereq, boolean autoComplete,
                              List<ResourceLocation> requires, List<QuestObjective> objectives,
                              List<String> onAccept, List<String> onComplete, List<JournalImage> images) {

    public QuestDefinition {
        requires = List.copyOf(requires);
        objectives = List.copyOf(objectives);
        onAccept = List.copyOf(onAccept);
        onComplete = List.copyOf(onComplete);
        images = List.copyOf(images);
    }

    public boolean hasObjectives() {
        return !objectives.isEmpty();
    }
}
