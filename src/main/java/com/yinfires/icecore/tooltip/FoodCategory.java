package com.yinfires.icecore.tooltip;

import net.minecraft.network.chat.*;
import java.util.Optional;

public record FoodCategory(Component text) implements ComponentContents {
    public MutableComponent component() { return MutableComponent.create(this); }
    @Override public <T> Optional<T> visit(FormattedText.ContentConsumer<T> visitor) { return text.visit(visitor); }
    @Override public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> visitor, Style style) { return text.visit(visitor, style); }
}
