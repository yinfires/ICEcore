package com.yinfires.icecore.journal;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * A left-panel category (chapter) shared by the quest and tutorial journals. Categories form a
 * tree via {@link #parent}; {@link #type} keeps quest and tutorial trees separate. {@link #icon}
 * is optional and may be swapped for artwork later without code changes.
 */
public record CategoryDefinition(ResourceLocation id, JournalType type, JournalText name,
                                 @Nullable ResourceLocation parent, @Nullable ResourceLocation icon,
                                 int order) {
}
