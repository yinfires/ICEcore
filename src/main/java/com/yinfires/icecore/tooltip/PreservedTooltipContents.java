package com.yinfires.icecore.tooltip;

import net.minecraft.network.chat.*;
import java.util.Optional;

/** Explicit producer marker for native dynamic contents, never inferred from color or text. */
public record PreservedTooltipContents(Component original) implements ComponentContents {
    public static MutableComponent wrap(Component original) {
        return MutableComponent.create(new PreservedTooltipContents(original)).setStyle(original.getStyle());
    }
    @Override public <T> Optional<T> visit(FormattedText.ContentConsumer<T> visitor) { return original.visit(visitor); }
    @Override public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> visitor, Style style) {
        return original.visit(visitor, style);
    }
}
