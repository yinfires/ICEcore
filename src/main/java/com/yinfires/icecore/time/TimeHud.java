package com.yinfires.icecore.time;

import com.yinfires.icecore.ICECore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

public final class TimeHud {
    private static final int CLOCK_Y = 33;
    private static final int GLYPH_HEIGHT = 10;
    private static final int ICON_GAP = 4;
    private static final double VANILLA_REAL_MILLIS_PER_MINUTE = 50_000.0D / 60.0D;
    private static final double MAX_ROLL_MILLIS = 250.0D;
    private static final double ROLL_FRACTION_OF_MINUTE = 0.45D;
    private static final String[] CLOCK_TEXT = createClockTextCache();
    private static final String[] GLYPHS = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ":"};

    private static long shownDay = Long.MIN_VALUE;
    private static long fromDay;
    private static long targetDay;
    private static long rollStarted;
    private static int shownMinute = -1;
    private static int fromMinute;
    private static int targetMinute;
    private static long minuteRollStartedNanos;

    private TimeHud() {
    }

    @Mod.EventBusSubscriber(modid = ICECore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("time", (gui, graphics, partialTick, width, height) -> render(graphics, width));
        }
    }

    private static void render(GuiGraphics graphics, int width) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !TimeClientState.initialized()
                || !TimeClientState.config().hudEnabled()) {
            return;
        }

        double visualTime = TimeClientState.displayedDayTime();
        long wholeTime = (long) Math.floor(visualTime);
        long day = animatedDay(TimeCalendar.day(wholeTime));
        String dayText = Component.translatable("icecore.time.hud.day", day).getString();
        String weekdayText = Component.translatable("icecore.time.weekday." + TimeCalendar.weekday(wholeTime)).getString();
        int right = width - 7;
        drawRight(graphics, dayText, right, 7);
        drawRight(graphics, weekdayText, right, 19);

        int minute = minuteOfDay(visualTime);
        updateClockAnimation(minute);
        int clockWidth = minecraft.font.width(clockText(targetMinute));
        String celestialSymbol = celestialSymbol(wholeTime);
        Font font = minecraft.font;
        int iconX = right - clockWidth - ICON_GAP - font.width(celestialSymbol);
        drawCelestialIcon(graphics, iconX, CLOCK_Y, celestialSymbol);
        drawClock(graphics, right, CLOCK_Y);
    }

    private static void drawRight(GuiGraphics graphics, String text, int right, int y) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, text, right - font.width(text), y, 0xFFFFFFFF, true);
    }

    private static long animatedDay(long current) {
        long now = System.currentTimeMillis();
        if (shownDay == Long.MIN_VALUE) {
            shownDay = fromDay = targetDay = current;
            rollStarted = now;
        }
        if (current != targetDay) {
            fromDay = shownDay;
            targetDay = current;
            rollStarted = now;
        }
        double progress = Math.min(1.0D, (now - rollStarted) / 600.0D);
        shownDay = fromDay + Math.round((targetDay - fromDay) * (1.0D - Math.pow(1.0D - progress, 4.0D)));
        return shownDay;
    }

    private static void updateClockAnimation(int minute) {
        if (shownMinute < 0) {
            shownMinute = fromMinute = targetMinute = minute;
            minuteRollStartedNanos = System.nanoTime();
            return;
        }
        if (minute != targetMinute) {
            fromMinute = targetMinute;
            targetMinute = minute;
            shownMinute = minute;
            minuteRollStartedNanos = System.nanoTime();
        }
    }

    private static void drawClock(GuiGraphics graphics, int right, int y) {
        double elapsedMillis = (System.nanoTime() - minuteRollStartedNanos) / 1_000_000.0D;
        double durationMillis = rollDurationMillis(TimeClientState.config().dayDurationMultiplier());
        double progress = fromMinute == targetMinute ? 1.0D : Math.min(1.0D, elapsedMillis / durationMillis);
        progress = ease(progress);
        String current = clockText(fromMinute);
        String next = clockText(targetMinute);
        Font font = Minecraft.getInstance().font;
        int x = right - font.width(next);
        int offset = (int) Math.floor(progress * GLYPH_HEIGHT);

        for (int slot = 0; slot < current.length(); slot++) {
            String currentGlyph = glyph(current.charAt(slot));
            String nextGlyph = glyph(next.charAt(slot));
            int slotWidth = font.width(currentGlyph);
            if (currentGlyph.equals(nextGlyph)) {
                graphics.drawString(font, currentGlyph, x, y, 0xFFFFFFFF, true);
            } else {
                graphics.enableScissor(x, y, x + slotWidth + 1, y + GLYPH_HEIGHT);
                graphics.drawString(font, currentGlyph, x, y - offset, 0xFFFFFFFF, true);
                graphics.drawString(font, nextGlyph, x, y + GLYPH_HEIGHT - offset, 0xFFFFFFFF, true);
                graphics.disableScissor();
            }
            x += slotWidth;
        }
    }

    private static String celestialSymbol(long dayTime) {
        int hour = TimeCalendar.hour(dayTime);
        if (hour >= 6 && hour < 18) {
            return "☀";
        }
        return "☾";
    }

    private static void drawCelestialIcon(GuiGraphics graphics, int x, int y, String symbol) {
        // Font glyphs share the clock baseline and native height; no oversized bitmap is drawn.
        graphics.drawString(Minecraft.getInstance().font, symbol, x, y, 0xFFFFFFFF, true);
    }

    static int minuteOfDay(double dayTime) {
        double minecraftTicks = dayTime - Math.floor(dayTime / 24_000.0D) * 24_000.0D;
        return Math.floorMod((int) Math.floor(minecraftTicks * 1_440.0D / 24_000.0D + 360.0D), 1_440);
    }

    static double minuteProgress(double dayTime) {
        double minecraftTicks = dayTime - Math.floor(dayTime / 24_000.0D) * 24_000.0D;
        double exactMinute = minecraftTicks * 1_440.0D / 24_000.0D + 360.0D;
        return exactMinute - Math.floor(exactMinute);
    }

    static double rollDurationMillis(double multiplier) {
        return Math.max(1.0D, Math.min(MAX_ROLL_MILLIS,
                VANILLA_REAL_MILLIS_PER_MINUTE * multiplier * ROLL_FRACTION_OF_MINUTE));
    }

    private static double ease(double value) {
        double x = Math.max(0.0D, Math.min(1.0D, value));
        return x * x * (3.0D - 2.0D * x);
    }

    static void resetAnimation() {
        shownMinute = -1;
        fromMinute = 0;
        targetMinute = 0;
    }

    static String clockText(int minute) {
        return CLOCK_TEXT[Math.floorMod(minute, 1_440)];
    }

    static boolean slotChanges(int minute, int slot) {
        String current = clockText(minute);
        String next = clockText(minute + 1);
        return slot >= 0 && slot < current.length() && current.charAt(slot) != next.charAt(slot);
    }

    private static String glyph(char value) {
        return value == ':' ? GLYPHS[10] : GLYPHS[value - '0'];
    }

    private static String[] createClockTextCache() {
        String[] values = new String[1_440];
        for (int minute = 0; minute < values.length; minute++) {
            values[minute] = String.format(Locale.ROOT, "%02d:%02d", minute / 60, minute % 60);
        }
        return values;
    }
}
