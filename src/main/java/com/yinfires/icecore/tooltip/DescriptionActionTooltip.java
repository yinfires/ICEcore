package com.yinfires.icecore.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.joml.Matrix4f;
import java.util.List;

/** Native tooltip content only; the enclosing tooltip retains Forge/Modern UI rendering. */
public final class DescriptionActionTooltip implements ClientTooltipComponent {
    public record Data(DescriptionAction action, int maxWidth, boolean keycaps) implements TooltipComponent {}
    private final Data data;
    private Font measuredFont;
    private List<String> keys;
    private List<FormattedCharSequence> body;
    private int keyWidth, width, height, bodyX, bodyY;
    private int[] keyX, keyY;
    private int keyHeight;
    public DescriptionActionTooltip(Data data) { this.data = data; }
    private void measure(Font font) {
        if (measuredFont == font) return;
        measuredFont = font;
        keys = data.action.keys().stream().map(key -> FixedDescriptionKeys.display(key, data.keycaps)).toList();
        keyWidth = 0;
        keyX = new int[keys.size()];
        keyY = new int[keys.size()];
        int rowX = 0, rowY = 0;
        int lineHeight = font.lineHeight + 1;
        int rowHeight = data.keycaps ? 16 : lineHeight;
        for (int i = 0; i < keys.size(); i++) {
            int plus = i > 0 ? font.width(" + ") : 0;
            int keySize = font.width(data.keycaps ? keys.get(i) : "[" + keys.get(i) + "]") + (data.keycaps ? 11 : 0);
            if (rowX > 0 && rowX + plus + keySize > data.maxWidth) { rowX = 0; rowY += rowHeight; }
            keyX[i] = rowX + plus;
            keyY[i] = rowY;
            rowX += plus + keySize;
            keyWidth = Math.max(keyWidth, rowX);
        }
        keyHeight = rowY + rowHeight;
        // Put the body below oversized combinations instead of squeezing it to unreadable columns.
        bodyX = rowY == 0 && keyWidth + 4 < data.maxWidth / 2 ? keyWidth + 4 : 0;
        bodyY = bodyX == 0 ? keyHeight : 0;
        body = font.split(data.action.text(), Math.max(1, data.maxWidth - bodyX));
        if (body.isEmpty()) body = List.of(FormattedCharSequence.EMPTY);
        width = keyWidth;
        for (FormattedCharSequence line : body) width = Math.max(width, bodyX + font.width(line));
        height = bodyY + Math.max(bodyX == 0 ? 0 : keyHeight,
                body.size() * lineHeight + (bodyX > 0 && data.keycaps ? 3 : 0));
    }
    @Override public int getWidth(Font font) { measure(font); return width; }
    @Override public int getHeight() { return height; }
    @Override public void renderText(Font font, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource buffers) {
        measure(font);
        if (!data.keycaps) for (int i = 0; i < keys.size(); i++) {
            if (i > 0) text(font, " + ", x + keyX[i] - font.width(" + "), y + keyY[i], matrix, buffers);
            String label = "[" + keys.get(i) + "]";
            text(font, label, x + keyX[i], y + keyY[i], matrix, buffers);
        }
        int baseline = bodyY + (bodyX > 0 && data.keycaps ? 3 : 0);
        for (int i = 0; i < body.size(); i++) font.drawInBatch(body.get(i), x + bodyX,
                y + baseline + i * (font.lineHeight + 1), 0xFFFFAA, true, matrix, buffers, Font.DisplayMode.NORMAL, 0, 15728880);
    }
    private static void text(Font font, String text, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource buffers) {
        font.drawInBatch(text, x, y, 0xFFFFFF, true, matrix, buffers, Font.DisplayMode.NORMAL, 0, 15728880);
    }
    @Override public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        if (!data.keycaps) return;
        measure(font);
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) NativeKeys.text(graphics, x + keyX[i] - font.width(" + "), y + keyY[i] + 3, " + ");
            NativeKeys.key(graphics, x + keyX[i], y + keyY[i], keys.get(i));
        }
    }
    private static final class NativeKeys {
        static void key(GuiGraphics g, int x, int y, String key) {
            com.mafuyu404.smartkeyprompts.init.KeyRenderer.drawKeyBoardKey(g, x, y, key, false);
        }
        static void text(GuiGraphics g, int x, int y, String text) {
            com.mafuyu404.smartkeyprompts.init.KeyRenderer.drawText(g, x, y, text);
        }
    }
}
