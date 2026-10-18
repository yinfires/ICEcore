package com.yinfires.icecore.tooltip;

import net.minecraft.network.chat.*;
import java.util.List;
import java.util.Optional;

/** Marker survives the ordinary Component.copy() tooltip pipeline. */
public record DescriptionAction(List<String> keys, Component text) implements ComponentContents {
    public DescriptionAction { keys = List.copyOf(keys); text = text.copy(); }
    public MutableComponent component() { return MutableComponent.create(this); }
    @Override public <T> Optional<T> visit(FormattedText.ContentConsumer<T> visitor) { return text.visit(visitor); }
    @Override public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> visitor, Style style) {
        return text.visit(visitor, style);
    }
}