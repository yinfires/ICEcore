package com.yinfires.icecore.tutorial;

import com.yinfires.icecore.journal.JournalImage;
import com.yinfires.icecore.journal.JournalText;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Immutable, datapack-driven tutorial (guide) entry. Same title/content model as quests, plus an
 * optional ordered list of images embedded in the detail panel. Tutorials are unlocked externally
 * (command/dialogue); there is no completion concept.
 */
public record TutorialDefinition(ResourceLocation id, JournalText title, JournalText content,
                                 @Nullable ResourceLocation category, @Nullable ResourceLocation icon,
                                 int order, List<JournalImage> images) {

    public TutorialDefinition {
        images = List.copyOf(images);
    }
}
