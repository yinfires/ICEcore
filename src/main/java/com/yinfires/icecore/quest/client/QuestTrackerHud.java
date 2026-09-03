package com.yinfires.icecore.quest.client;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.journal.client.JournalTheme;
import com.yinfires.icecore.journal.client.TopRightHudStack;
import com.yinfires.icecore.quest.QuestDefinition;
import com.yinfires.icecore.quest.QuestDefinitionManager;
import com.yinfires.icecore.quest.QuestSnapshot;
import com.yinfires.icecore.quest.objective.QuestObjective;
import com.yinfires.icecore.tutorial.client.TutorialClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Top-right HUD for the single marked quest, plus an unread hint. A tick-driven state machine
 * animates a pop-in when the player (re)enters the normal view and a retract when the marked quest
 * clears; on completion the objective bars fill and a check shows before it retracts (never an
 * instant disappearance). Objectives are drawn as a distinct block with per-objective progress bars,
 * matching the quest screen. When there are unread quests/tutorials, a smaller hint line ("NEW 有新
 * 任务/教程，按 J/I 查看") is shown below the card. Stacks below the time HUD via {@link TopRightHudStack}.
 *
 * <p>Because HUD overlays render beneath any open Screen, the retract triggered by *leaving* the
 * normal view into an opaque screen is not visible (the screen covers it); the pop-in on return is.
 */
public final class QuestTrackerHud {
    private static final long ENTER_MILLIS = 220L;
    private static final long FILL_MILLIS = 450L;
    private static final long HOLD_MILLIS = 400L;
    private static final long EXIT_MILLIS = 220L;
    private static final long BAR_MILLIS = 350L; // per-objective bar ease when its count changes
    private static final int RIGHT_MARGIN = 7;
    private static final int PADDING = 4;
    private static final int MAX_WIDTH = 150;
    private static final float HINT_SCALE = 0.75F;

    private enum Phase { HIDDEN, ENTER, SHOWN, COMPLETING, EXIT }

    private static Phase phase = Phase.HIDDEN;
    private static long phaseStartNanos;
    @Nullable
    private static ResourceLocation shownId;
    @Nullable
    private static QuestSnapshot cachedSnapshot;
    private static boolean wasNormalView;

    // Per-objective progress-bar animation for the currently shown quest: each bar eases from its
    // previous fraction to the target fraction over BAR_MILLIS whenever that target changes, so
    // intermediate progress (e.g. 1/3 -> 2/3) slides smoothly rather than jumping.
    @Nullable
    private static ResourceLocation barQuestId;
    private static float[] barFrom = new float[0];
    private static float[] barTarget = new float[0];
    private static long[] barStartNanos = new long[0];

    private QuestTrackerHud() {
    }

    @Mod.EventBusSubscriber(modid = ICECore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("quest_tracker", (gui, graphics, partialTick, width, height) -> render(graphics, width));
        }
    }

    @Mod.EventBusSubscriber(modid = ICECore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Ticker {
        private Ticker() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                tick();
            }
        }
    }

    private static boolean isNormalView() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && !mc.options.hideGui && mc.screen == null && QuestClientState.initialized();
    }

    private static void setPhase(Phase next) {
        phase = next;
        phaseStartNanos = System.nanoTime();
    }

    private static float phaseProgress(long durationMillis) {
        long elapsed = (System.nanoTime() - phaseStartNanos) / 1_000_000L;
        return Mth.clamp(elapsed / (float) durationMillis, 0.0F, 1.0F);
    }

    /**
     * Drives the state machine every client tick (runs even while a screen is open, so view
     * transitions are detected reliably). Rendering only reads {@link #phase}/{@link #shownId}.
     */
    private static void tick() {
        boolean normal = isNormalView();
        ResourceLocation marked = QuestClientState.markedId();

        if (!normal) {
            // Left the normal view: freeze WITHOUT clearing shownId/cachedSnapshot, so a completion
            // that happens while a screen is open (e.g. the open_journal quest completes on closing
            // the tutorial) still animates when we return. Only remember that we left.
            wasNormalView = false;
            return;
        }

        boolean justEntered = !wasNormalView;
        wasNormalView = true;

        // Keep a snapshot of the shown quest for the completion fill animation.
        if (shownId != null && QuestClientState.active().containsKey(shownId)) {
            cachedSnapshot = QuestClientState.active().get(shownId);
        }

        // Completion / disappearance of the currently shown quest: fill bars then retract. Checked
        // before the phase switch so it also fires on the first tick after returning to normal view.
        if (shownId != null && !QuestClientState.active().containsKey(shownId)
                && phase != Phase.COMPLETING && phase != Phase.EXIT) {
            QuestDefinition def = QuestDefinitionManager.INSTANCE.get(shownId);
            if (def != null && def.hasObjectives() && cachedSnapshot != null) {
                setPhase(Phase.COMPLETING);
            } else {
                setPhase(Phase.EXIT);
            }
            return;
        }

        // Re-pop when returning to normal view with the same quest still marked and active.
        if (justEntered && shownId != null && marked != null && marked.equals(shownId)
                && QuestClientState.active().containsKey(shownId)
                && phase != Phase.COMPLETING && phase != Phase.EXIT) {
            setPhase(Phase.ENTER);
            return;
        }

        switch (phase) {
            case HIDDEN -> {
                if (marked != null) {
                    shownId = marked;
                    cachedSnapshot = QuestClientState.active().get(marked);
                    setPhase(Phase.ENTER);
                }
            }
            case ENTER -> {
                if (!java.util.Objects.equals(marked, shownId)) {
                    // Mark changed mid-animation: restart toward the new target (or retract).
                    retargetMark(marked);
                } else if (phaseProgress(ENTER_MILLIS) >= 1.0F) {
                    setPhase(Phase.SHOWN);
                }
            }
            case SHOWN -> {
                if (!java.util.Objects.equals(marked, shownId)) {
                    retargetMark(marked);
                }
            }
            case COMPLETING -> {
                if (phaseProgress(FILL_MILLIS + HOLD_MILLIS) >= 1.0F) {
                    setPhase(Phase.EXIT);
                }
            }
            case EXIT -> {
                if (phaseProgress(EXIT_MILLIS) >= 1.0F) {
                    shownId = null;
                    cachedSnapshot = null;
                    phase = Phase.HIDDEN;
                    // A new mark may already be waiting.
                    if (marked != null) {
                        shownId = marked;
                        cachedSnapshot = QuestClientState.active().get(marked);
                        setPhase(Phase.ENTER);
                    }
                }
            }
        }
    }

    private static void retargetMark(@Nullable ResourceLocation marked) {
        if (marked == null) {
            setPhase(Phase.EXIT);
        } else {
            shownId = marked;
            cachedSnapshot = QuestClientState.active().get(marked);
            setPhase(Phase.ENTER);
        }
    }


    private static void render(GuiGraphics graphics, int screenWidth) {
        if (!isNormalView()) {
            return;
        }
        int startY = TopRightHudStack.trackerStartY();
        int cardBottom = startY;

        // Draw the quest card if a quest is being shown/animated.
        if (shownId != null && phase != Phase.HIDDEN) {
            QuestDefinition def = QuestDefinitionManager.INSTANCE.get(shownId);
            QuestSnapshot snapshot = QuestClientState.active().get(shownId);
            if (snapshot == null) {
                snapshot = cachedSnapshot;
            }
            if (def != null && snapshot != null) {
                cardBottom = drawCard(graphics, screenWidth, def, snapshot, startY);
            }
        }

        // Unread hint line(s) below the card (or at the tracker start if no card).
        drawUnreadHint(graphics, screenWidth, cardBottom);
    }

    /** Reveal factor 0..1 for the current phase (height/slide/alpha driver). */
    private static float reveal() {
        return switch (phase) {
            case ENTER -> ease(phaseProgress(ENTER_MILLIS));
            case EXIT -> 1.0F - ease(phaseProgress(EXIT_MILLIS));
            case HIDDEN -> 0.0F;
            default -> 1.0F; // SHOWN, COMPLETING
        };
    }

    /** Ensures the per-objective bar animation arrays are sized/reset for the shown quest. */
    private static void ensureBarState(ResourceLocation questId, int count) {
        if (!questId.equals(barQuestId) || barTarget.length != count) {
            barQuestId = questId;
            barFrom = new float[count];
            barTarget = new float[count];
            barStartNanos = new long[count];
            long now = System.nanoTime();
            for (int i = 0; i < count; i++) {
                barStartNanos[i] = now - BAR_MILLIS; // start settled (no intro sweep from 0)
            }
        }
    }

    /**
     * Animated fraction for objective {@code i}: eases toward {@code desired}. When {@code desired}
     * changes (progress advanced, or completion sweeping to 1.0), it restarts the ease from the
     * current displayed value. On the first sizing for a quest, bars settle instantly at the live
     * value so opening the tracker does not replay a fill from empty.
     */
    private static float animatedBar(int i, float desired) {
        if (i < 0 || i >= barTarget.length) {
            return desired;
        }
        long now = System.nanoTime();
        if (Math.abs(desired - barTarget[i]) > 0.0005F) {
            barFrom[i] = currentBar(i, now);
            barTarget[i] = desired;
            barStartNanos[i] = now;
        }
        return currentBar(i, now);
    }

    private static float currentBar(int i, long now) {
        float t = Mth.clamp((now - barStartNanos[i]) / (float) (BAR_MILLIS * 1_000_000L), 0.0F, 1.0F);
        return Mth.lerp(ease(t), barFrom[i], barTarget[i]);
    }

    /** Fill fraction for completion animation: objective bars sweep to full during COMPLETING. */
    private static float completionFill() {
        if (phase != Phase.COMPLETING) {
            return -1.0F; // not completing: use live counts
        }
        return ease(Math.min(1.0F, phaseProgress(FILL_MILLIS)));
    }

    private static int drawCard(GuiGraphics graphics, int screenWidth, QuestDefinition def,
                                QuestSnapshot snapshot, int startY) {
        Font font = Minecraft.getInstance().font;
        Component title = def.title().resolve();
        List<QuestObjective> objectives = def.objectives();
        float fill = completionFill();
        ensureBarState(shownId, objectives.size());

        // Measure content width.
        int contentWidth = font.width(title);
        List<Component> labels = new ArrayList<>();
        for (QuestObjective objective : objectives) {
            Component label = objective.label();
            if (label.getString().isEmpty()) {
                label = Component.translatable("gui.icecore.journal.objective.generic");
            }
            labels.add(label);
            contentWidth = Math.max(contentWidth, font.width(label) + 22);
        }
        contentWidth = Math.min(contentWidth, MAX_WIDTH);
        int cardWidth = contentWidth + PADDING * 2;

        int rowH = font.lineHeight + 1;
        int objectivesBlock = objectives.isEmpty() ? 0 : (objectives.size() * (rowH + 4) + 2);
        int fullHeight = PADDING * 2 + font.lineHeight + 2 + objectivesBlock;
        float reveal = reveal();
        int cardHeight = Math.max(1, Math.round(fullHeight * reveal));

        int slide = Math.round((1.0F - reveal) * 12.0F);
        int x = screenWidth - RIGHT_MARGIN - cardWidth + slide;
        int y = startY;
        int alpha = Mth.clamp((int) (reveal * 255.0F), 0, 255);
        if (alpha < 4) {
            return startY;
        }
        int bgAlpha = Mth.clamp((int) (reveal * (JournalTheme.PANEL_BG >>> 24)), 4, 255);
        graphics.fill(x, y, x + cardWidth, y + cardHeight, (bgAlpha << 24) | (JournalTheme.PANEL_BG & 0xFFFFFF));
        drawBorder(graphics, x, y, cardWidth, cardHeight, (alpha << 24) | (JournalTheme.BORDER & 0xFFFFFF));

        graphics.enableScissor(x, y, x + cardWidth, y + cardHeight);
        int titleColor = (alpha << 24) | 0xFFFFFF;
        graphics.drawString(font, Component.empty().append(title).withStyle(net.minecraft.ChatFormatting.BOLD),
                x + PADDING, y + PADDING, titleColor, true);
        int cursor = y + PADDING + font.lineHeight + 1;
        graphics.fill(x + PADDING, cursor, x + cardWidth - PADDING, cursor + 1,
                (alpha << 24) | (JournalTheme.DIVIDER & 0xFFFFFF));
        cursor += 3;

        for (int i = 0; i < objectives.size(); i++) {
            QuestObjective objective = objectives.get(i);
            int target = objective.targetCount();
            int liveCount = i < snapshot.objectiveCounts().length ? snapshot.objectiveCounts()[i] : 0;
            // Desired fraction: the live progress ratio, or 1.0 while completing. The bar eases
            // toward it (BAR_MILLIS) so both intermediate steps and completion slide smoothly.
            float liveFraction = target <= 0 ? 1.0F : Math.min(1.0F, liveCount / (float) target);
            float desired = fill < 0 ? liveFraction : 1.0F;
            float barFraction = animatedBar(i, desired);
            boolean barFull = barFraction >= 0.999F;
            // The check appears the instant the bar reaches full (no extra hold delay).
            boolean done = fill < 0 ? liveCount >= target : barFull;
            int labelColor = (alpha << 24) | ((done ? 0x7DD87D : (JournalTheme.CONTENT_COLOR & 0xFFFFFF)));
            // Matches the detail panel: check replaces the number when complete; no number when
            // target<=1 (single-step); otherwise the integer x/y (rounded from the animated fill).
            String progress;
            if (done) {
                progress = "✔";
            } else if (target <= 1) {
                progress = "";
            } else {
                int shownCount = Math.round(barFraction * target);
                progress = shownCount + "/" + target;
            }
            int pw = progress.isEmpty() ? 0 : font.width(progress);
            String labelText = font.plainSubstrByWidth(labels.get(i).getString(), contentWidth - pw - 4);
            graphics.drawString(font, labelText, x + PADDING, cursor, labelColor, true);
            if (!progress.isEmpty()) {
                graphics.drawString(font, progress, x + cardWidth - PADDING - pw, cursor, labelColor, true);
            }
            cursor += rowH;
            int barW = contentWidth;
            graphics.fill(x + PADDING, cursor, x + PADDING + barW, cursor + 2, (alpha << 24) | 0x40FFFFFF);
            int filled = Math.round(barW * barFraction);
            graphics.fill(x + PADDING, cursor, x + PADDING + filled, cursor + 2,
                    (alpha << 24) | (barFull ? 0x7DD87D : 0xB0B7BE));
            cursor += 4;
        }
        graphics.disableScissor();
        return y + cardHeight;
    }

    private static void drawUnreadHint(GuiGraphics graphics, int screenWidth, int belowY) {
        boolean newQuest = QuestClientState.hasUnseen();
        boolean newTutorial = TutorialClientState.initialized() && TutorialClientState.hasUnread();
        if (!newQuest && !newTutorial) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        List<Component> lines = new ArrayList<>();
        if (newQuest) {
            lines.add(Component.translatable("gui.icecore.journal.hint.new_quest"));
        }
        if (newTutorial) {
            lines.add(Component.translatable("gui.icecore.journal.hint.new_tutorial"));
        }
        int y = belowY + 3;
        for (Component line : lines) {
            int textW = (int) (font.width(line) * HINT_SCALE);
            int badge = (int) (5 * HINT_SCALE) + 3;
            int totalW = badge + textW;
            int x = screenWidth - RIGHT_MARGIN - totalW;
            // Small NEW badge (red square), then scaled hint text.
            graphics.fill(x, y + 1, x + (int) (5 * HINT_SCALE), y + 1 + (int) (5 * HINT_SCALE), JournalTheme.BADGE_NEW);
            graphics.pose().pushPose();
            graphics.pose().translate(x + badge, y, 0);
            graphics.pose().scale(HINT_SCALE, HINT_SCALE, 1.0F);
            graphics.drawString(font, line, 0, 0, 0xFFD0D0D0, true);
            graphics.pose().popPose();
            y += (int) (font.lineHeight * HINT_SCALE) + 2;
        }
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static float ease(float x) {
        float c = Mth.clamp(x, 0.0F, 1.0F);
        return c * c * (3.0F - 2.0F * c);
    }


    /** Reset on logout so a stale card cannot linger into a new session. */
    public static void reset() {
        phase = Phase.HIDDEN;
        shownId = null;
        cachedSnapshot = null;
        wasNormalView = false;
        barQuestId = null;
        barFrom = new float[0];
        barTarget = new float[0];
        barStartNanos = new long[0];
    }
}
