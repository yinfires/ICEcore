package com.yinfires.icecore.quest.client;

import com.yinfires.icecore.journal.JournalType;
import com.yinfires.icecore.journal.client.EntryView;
import com.yinfires.icecore.journal.client.JournalScreen;
import com.yinfires.icecore.journal.client.JournalTheme;
import com.yinfires.icecore.network.ICECoreNetwork;
import com.yinfires.icecore.network.ServerBoundSetMarkPacket;
import com.yinfires.icecore.quest.QuestDefinition;
import com.yinfires.icecore.quest.QuestDefinitionManager;
import com.yinfires.icecore.quest.QuestSnapshot;
import com.yinfires.icecore.quest.objective.QuestObjective;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Quest journal (J). Lists only ACTIVE quests from {@link QuestClientState}; completed/hidden are
 * absent. Detail shows the large title, divider, content and per-objective progress. A single
 * mark button (bottom-right) toggles the tracked quest; its icon differs by marked state.
 */
public final class QuestScreen extends JournalScreen {
    private int markButtonX;
    private int markButtonY;
    private int markButtonW;
    private int markButtonH;

    public QuestScreen() {
        super(Component.translatable("gui.icecore.journal.quests"));
    }

    @Override
    protected JournalType journalType() {
        return JournalType.QUEST;
    }

    @Override
    protected void onSelect(ResourceLocation entryId) {
        // Opening a quest entry clears its NEW dot (mirrors tutorials); the hint reflects remaining dots.
        if (QuestClientState.isUnseen(entryId)) {
            QuestClientState.markSeen(entryId);
            rebuildTree();
        }
    }

    @Override
    protected Component emptyMessage() {
        return Component.translatable("gui.icecore.journal.quests.empty");
    }

    @Override
    protected Component noSelectionMessage() {
        return Component.translatable("gui.icecore.journal.quests.none_selected");
    }

    @Override
    protected List<EntryView> entries() {
        List<EntryView> views = new ArrayList<>();
        for (QuestSnapshot snapshot : QuestClientState.active().values()) {
            QuestDefinition def = QuestDefinitionManager.INSTANCE.get(snapshot.id());
            if (def == null) {
                continue;
            }
            List<com.yinfires.icecore.journal.client.ObjectiveView> objectiveViews = new ArrayList<>();
            List<QuestObjective> objectives = def.objectives();
            for (int i = 0; i < objectives.size(); i++) {
                int current = i < snapshot.objectiveCounts().length ? snapshot.objectiveCounts()[i] : 0;
                QuestObjective objective = objectives.get(i);
                objectiveViews.add(new com.yinfires.icecore.journal.client.ObjectiveView(
                        objective.label(), current, objective.targetCount()));
            }
            // List NEW dot marks an unopened quest (parallels tutorials); tracked state shows via
            // the detail star button and the tracker HUD, not the list badge.
            boolean unseen = QuestClientState.isUnseen(snapshot.id());
            views.add(new EntryView(def.id(), def.category(), def.order(),
                    def.title().resolve(), def.content().resolve(), unseen, objectiveViews, def.images()));
        }
        return views;
    }

    @Override
    protected void renderDetailExtras(GuiGraphics graphics, EntryView entry, int x, int y, int w, int h) {
        // Bottom-right mark button. Two visibly distinct states: marked (filled) vs unmarked (outline).
        boolean marked = QuestClientState.isMarked(entry.id());
        markButtonW = 56;
        markButtonH = 14;
        markButtonX = x + w - JournalTheme.SCROLLBAR_WIDTH - markButtonW - 2;
        markButtonY = y + h - markButtonH - 2;
        int bg = marked ? 0x66FFFFFF : 0x22FFFFFF;
        graphics.fill(markButtonX, markButtonY, markButtonX + markButtonW, markButtonY + markButtonH, bg);
        // Outline for the unmarked state; filled state omits it, so the two read differently.
        if (!marked) {
            int b = JournalTheme.BORDER;
            graphics.fill(markButtonX, markButtonY, markButtonX + markButtonW, markButtonY + 1, b);
            graphics.fill(markButtonX, markButtonY + markButtonH - 1, markButtonX + markButtonW, markButtonY + markButtonH, b);
            graphics.fill(markButtonX, markButtonY, markButtonX + 1, markButtonY + markButtonH, b);
            graphics.fill(markButtonX + markButtonW - 1, markButtonY, markButtonX + markButtonW, markButtonY + markButtonH, b);
        }
        // Filled marker glyph differs from the hollow one.
        String glyph = marked ? "★ " : "☆ ";
        Component label = Component.literal(glyph).append(Component.translatable(
                marked ? "gui.icecore.journal.tracked" : "gui.icecore.journal.track"));
        int labelWidth = font.width(label);
        int textColor = marked ? JournalTheme.TITLE_COLOR : JournalTheme.CONTENT_COLOR;
        graphics.drawString(font, label, markButtonX + (markButtonW - labelWidth) / 2,
                markButtonY + (markButtonH - font.lineHeight) / 2 + 1, textColor, false);
    }

    @Override
    protected boolean handleDetailClick(double mouseX, double mouseY) {
        ResourceLocation id = selectedId();
        if (id == null) {
            return false;
        }
        if (mouseX >= markButtonX && mouseX <= markButtonX + markButtonW
                && mouseY >= markButtonY && mouseY <= markButtonY + markButtonH) {
            // Server treats re-marking the current quest as a clear (single-select toggle).
            ICECoreNetwork.sendToServer(new ServerBoundSetMarkPacket(id));
            return true;
        }
        return false;
    }
}
