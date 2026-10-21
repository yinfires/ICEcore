package com.yinfires.icecore.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.joml.Matrix4f;
import net.minecraft.client.renderer.MultiBufferSource;

/** Text uses the current native font and buffers; background remains in the native tooltip. */
public record FoodCategoryTooltip(Data data) implements ClientTooltipComponent {
    public record Data(Component text, int width, boolean separator) implements TooltipComponent {}
    public int separatorOffset() { return 11; }
    @Override public int getHeight() { return data.separator() ? 15 : 10; }
    @Override public int getWidth(Font font) { return Math.max(data.width(), font.width(data.text())); }
    @Override public void renderText(Font font, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource buffers) {
        font.drawInBatch(data.text(), x + (getWidth(font)-font.width(data.text()))/2f, y, 0xAAAAAA, true,
            matrix, buffers, Font.DisplayMode.NORMAL, 0, 15728880);
    }
}
