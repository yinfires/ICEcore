package com.yinfires.icecore.time;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.text.NumberFormat;
import java.util.Locale;

final class DaySummaryRenderer {
    private DaySummaryRenderer() {}

    static String text(long income) {
        return Component.translatable("icecore.time.summary.title").getString() + "\n"
                + Component.translatable("icecore.time.summary.income", NumberFormat.getIntegerInstance(Locale.US).format(income)).getString() + "\n"
                + Component.translatable("icecore.time.summary.encourage").getString();
    }

    static int typingTicks(long income, TimeTimings timings) {
        return text(income).length() * timings.typewriterTicksPerCharacter();
    }

    static int durationTicks(long income, TimeTimings timings) {
        return typingTicks(income, timings) + timings.summaryHoldTicks() + timings.summaryFadeTicks();
    }

    static void render(GuiGraphics graphics, long income, TimeTimings timings, double elapsedTicks) {
        String all = text(income);
        int characters = Math.min(all.length(), (int) (elapsedTicks / timings.typewriterTicksPerCharacter()));
        int fadeStart = typingTicks(income, timings) + timings.summaryHoldTicks();
        double fadeProgress = Math.max(0.0D, Math.min(1.0D,
                (elapsedTicks - fadeStart) / timings.summaryFadeTicks()));
        int alpha = Math.max(0, Math.min(255, (int) Math.round(255.0D * (1.0D - ease(fadeProgress)))));
        draw(graphics, all.substring(0, characters), alpha);
    }

    private static void draw(GuiGraphics graphics, String text, int alpha) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        String[] lines = text.split("\n", -1);
        int y = height / 2 - 24;
        for (int index = 0; index < lines.length; index++) {
            float scale = index == 0 ? 1.8F : 1F;
            graphics.pose().pushPose();
            graphics.pose().translate(width / 2F, y, 0);
            graphics.pose().scale(scale, scale, 1);
            if (alpha >= 4) graphics.drawString(minecraft.font, lines[index], -minecraft.font.width(lines[index]) / 2, 0, (alpha << 24) | 0xFFFFFF, true);
            graphics.pose().popPose();
            y += index == 0 ? 24 : 14;
        }
    }

    private static double ease(double value) {
        double x = Math.max(0.0D, Math.min(1.0D, value));
        return x * x * (3.0D - 2.0D * x);
    }
}
