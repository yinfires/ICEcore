package com.yinfires.icecore.journal.client;

import com.yinfires.icecore.journal.JournalImage;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A single displayable journal entry (quest or tutorial) prepared by a screen for rendering.
 * {@code objectives} drives the distinct "objectives" block with progress bars (quests; empty for
 * tutorials). {@code images} are embedded in the detail panel below the content. {@code badge} is
 * the NEW marker (tutorials) or the tracked marker (quests) depending on the screen.
 */
public record EntryView(ResourceLocation id, @Nullable ResourceLocation category, int order,
                        Component title, Component content, boolean badge,
                        List<ObjectiveView> objectives, List<JournalImage> images) {

    public EntryView {
        objectives = List.copyOf(objectives);
        images = List.copyOf(images);
    }
}
