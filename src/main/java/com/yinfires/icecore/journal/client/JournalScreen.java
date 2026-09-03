package com.yinfires.icecore.journal.client;

import com.yinfires.icecore.journal.CategoryDefinition;
import com.yinfires.icecore.journal.CategoryManager;
import com.yinfires.icecore.journal.JournalType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared quest/tutorial journal: centered translucent panel, left collapsible category tree with
 * entries, right detail (large bold title, divider, wrapped content). Subclasses supply the entry
 * list and journal type; this base owns layout, the tree, selection, both scrollbars and input.
 */
public abstract class JournalScreen extends Screen {
    protected int left;
    protected int top;
    protected int panelWidth;
    protected int panelHeight;
    protected int listWidth;

    private final Scrollbar listScroll = new Scrollbar();
    private final Scrollbar detailScroll = new Scrollbar();
    private final Set<ResourceLocation> collapsed = new HashSet<>();
    private final List<Row> rows = new ArrayList<>();

    @Nullable
    private ResourceLocation selected;
    private int listContentHeight;
    private int detailContentHeight;

    protected JournalScreen(Component title) {
        super(title);
    }

    /** Journal type used to filter categories to this screen's tree. */
    protected abstract JournalType journalType();

    /** The entries currently visible to the player (already filtered by the subclass). */
    protected abstract List<EntryView> entries();

    /** Called when an entry is selected; tutorials use this to mark read. Default no-op. */
    protected void onSelect(ResourceLocation entryId) {
    }

    protected boolean noEntries() {
        return entries().isEmpty();
    }

    protected abstract Component emptyMessage();

    @Override
    protected void init() {
        panelWidth = Math.min(340, width - 40);
        panelHeight = Math.min(200, height - 40);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        listWidth = (int) (panelWidth * 0.38F);
        rebuildTree();
        // No default selection: the right panel shows the "nothing selected" message until the
        // player clicks an entry.
    }

    /** Message shown in the detail panel while no entry is selected. */
    protected abstract Component noSelectionMessage();

    private int listX() {
        return left + JournalTheme.PADDING;
    }

    private int listTop() {
        return top + JournalTheme.PADDING;
    }

    private int listHeight() {
        return panelHeight - JournalTheme.PADDING * 2;
    }

    private int detailX() {
        return left + listWidth + JournalTheme.PADDING;
    }

    private int detailTop() {
        return top + JournalTheme.PADDING;
    }

    private int detailWidth() {
        return left + panelWidth - JournalTheme.PADDING - detailX();
    }

    private int detailHeight() {
        return panelHeight - JournalTheme.PADDING * 2;
    }

    @Nullable
    private EntryView selectedEntry() {
        if (selected == null) {
            return null;
        }
        for (EntryView entry : entries()) {
            if (entry.id().equals(selected)) {
                return entry;
            }
        }
        return null;
    }


    /** Rebuilds the flattened tree from categories + visible entries, honouring collapse state. */
    protected void rebuildTree() {
        rows.clear();
        List<EntryView> visible = entries();

        // Group entries by category id (null => uncategorized bucket).
        Map<ResourceLocation, List<EntryView>> byCategory = new LinkedHashMap<>();
        List<EntryView> uncategorized = new ArrayList<>();
        for (EntryView entry : visible) {
            if (entry.category() == null) {
                uncategorized.add(entry);
            } else {
                byCategory.computeIfAbsent(entry.category(), key -> new ArrayList<>()).add(entry);
            }
        }

        // Collect categories of this journal type that have children (entries or sub-categories).
        Map<ResourceLocation, CategoryDefinition> categories = new LinkedHashMap<>();
        CategoryManager.INSTANCE.all().forEach((id, def) -> {
            if (def.type() == journalType()) {
                categories.put(id, def);
            }
        });

        // Roots: categories with no parent (or an absent parent), sorted by order.
        List<CategoryDefinition> roots = new ArrayList<>();
        for (CategoryDefinition def : categories.values()) {
            if (def.parent() == null || !categories.containsKey(def.parent())) {
                roots.add(def);
            }
        }
        roots.sort(Comparator.comparingInt(CategoryDefinition::order));
        for (CategoryDefinition root : roots) {
            appendCategory(root, categories, byCategory, 0);
        }

        // Uncategorized entries at the end, no header.
        uncategorized.sort(Comparator.comparingInt(EntryView::order));
        for (EntryView entry : uncategorized) {
            rows.add(new Row(false, null, entry, 0));
        }
    }

    private void appendCategory(CategoryDefinition category, Map<ResourceLocation, CategoryDefinition> all,
                                Map<ResourceLocation, List<EntryView>> byCategory, int depth) {
        List<EntryView> own = byCategory.getOrDefault(category.id(), List.of());
        List<CategoryDefinition> children = new ArrayList<>();
        for (CategoryDefinition def : all.values()) {
            if (category.id().equals(def.parent())) {
                children.add(def);
            }
        }
        children.sort(Comparator.comparingInt(CategoryDefinition::order));
        // Skip empty categories with no visible entries and no non-empty descendants.
        if (own.isEmpty() && children.isEmpty()) {
            return;
        }
        rows.add(new Row(true, category, null, depth));
        if (collapsed.contains(category.id())) {
            return;
        }
        List<EntryView> sortedOwn = new ArrayList<>(own);
        sortedOwn.sort(Comparator.comparingInt(EntryView::order));
        for (EntryView entry : sortedOwn) {
            rows.add(new Row(false, null, entry, depth + 1));
        }
        for (CategoryDefinition child : children) {
            appendCategory(child, all, byCategory, depth + 1);
        }
    }


    private static final int ROW_HEIGHT = 12;

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, JournalTheme.PANEL_BG);
        // Left list pane background + vertical divider between list and detail.
        int splitX = left + listWidth;
        graphics.fill(left, top, splitX, top + panelHeight, JournalTheme.PANE_BG);
        graphics.fill(splitX, top + 4, splitX + 1, top + panelHeight - 4, JournalTheme.DIVIDER);

        renderList(graphics, mouseX, mouseY);
        renderDetail(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = listX();
        int y0 = listTop();
        int h = listHeight();
        listContentHeight = rows.size() * ROW_HEIGHT;
        listScroll.setRegion(x, y0, listWidth - JournalTheme.PADDING, h, listContentHeight);
        listScroll.updateHover(mouseX, mouseY);

        graphics.enableScissor(x, y0, x + listWidth, y0 + h);
        int y = y0 - listScroll.scrollOffset();
        if (rows.isEmpty()) {
            graphics.drawString(font, emptyMessage(), x + 2, y0 + 2, JournalTheme.MUTED_COLOR, false);
        }
        for (Row row : rows) {
            if (y + ROW_HEIGHT >= y0 && y <= y0 + h) {
                renderRow(graphics, row, x, y, mouseX, mouseY);
            }
            y += ROW_HEIGHT;
        }
        graphics.disableScissor();
        listScroll.render(graphics, mouseX, mouseY);
    }

    private void renderRow(GuiGraphics graphics, Row row, int x, int y, int mouseX, int mouseY) {
        int indent = x + 2 + row.depth() * 8;
        boolean hovered = mouseX >= x && mouseX <= x + listWidth - JournalTheme.PADDING
                && mouseY >= y && mouseY < y + ROW_HEIGHT;
        if (row.header() && row.category() != null) {
            boolean isCollapsed = collapsed.contains(row.category().id());
            String arrow = isCollapsed ? "▸ " : "▾ ";
            graphics.drawString(font, Component.literal(arrow).append(row.category().name().resolve()),
                    indent, y + 2, JournalTheme.TITLE_COLOR, false);
        } else if (row.entry() != null) {
            EntryView entry = row.entry();
            boolean isSelected = entry.id().equals(selected);
            if (isSelected) {
                graphics.fill(x, y, x + listWidth - JournalTheme.PADDING, y + ROW_HEIGHT, JournalTheme.ENTRY_SELECTED);
            } else if (hovered) {
                graphics.fill(x, y, x + listWidth - JournalTheme.PADDING, y + ROW_HEIGHT, JournalTheme.ENTRY_HOVER);
            }
            int textColor = isSelected ? JournalTheme.TITLE_COLOR : JournalTheme.CONTENT_COLOR;
            String label = font.plainSubstrByWidth(entry.title().getString(), listWidth - JournalTheme.PADDING - 14 - row.depth() * 8);
            graphics.drawString(font, label, indent, y + 2, textColor, false);
            if (entry.badge()) {
                int bx = x + listWidth - JournalTheme.PADDING - 6;
                graphics.fill(bx, y + 3, bx + 4, y + 7, JournalTheme.BADGE_NEW);
            }
        }
    }

    private void renderDetail(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = detailX();
        int y0 = detailTop();
        int w = detailWidth();
        int h = detailHeight();
        EntryView entry = selectedEntry();
        if (entry == null) {
            // Nothing selected: centered muted placeholder ("未选中任务/教程").
            Component msg = noSelectionMessage();
            graphics.drawString(font, msg, x + (w - font.width(msg)) / 2, y0 + h / 2 - font.lineHeight / 2,
                    JournalTheme.MUTED_COLOR, false);
            return;
        }

        int textWidth = w - JournalTheme.SCROLLBAR_WIDTH - 4;
        graphics.enableScissor(x, y0, x + w, y0 + h);
        int y = y0 - detailScroll.scrollOffset();

        // Large bold title.
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(JournalTheme.TITLE_SCALE, JournalTheme.TITLE_SCALE, 1.0F);
        graphics.drawString(font, entry.title().copy().withStyle(net.minecraft.ChatFormatting.BOLD),
                0, 0, JournalTheme.TITLE_COLOR, false);
        graphics.pose().popPose();
        int cursor = y + (int) (font.lineHeight * JournalTheme.TITLE_SCALE) + 4;

        // Divider line under the title.
        graphics.fill(x, cursor, x + w - JournalTheme.SCROLLBAR_WIDTH - 2, cursor + 1, JournalTheme.DIVIDER);
        cursor += 6;

        // Wrapped content (description).
        for (var line : font.split(entry.content(), textWidth)) {
            graphics.drawString(font, line, x, cursor, JournalTheme.CONTENT_COLOR, false);
            cursor += font.lineHeight + 1;
        }

        // Distinct objectives block (no header label): each objective on a smaller, thinner line
        // with progress, then a progress bar.
        if (!entry.objectives().isEmpty()) {
            cursor += 6;
            for (ObjectiveView objective : entry.objectives()) {
                cursor = renderObjective(graphics, objective, x, cursor, textWidth);
            }
        }

        // Embedded images below everything else.
        if (!entry.images().isEmpty()) {
            cursor += 6;
            for (var image : entry.images()) {
                graphics.blit(image.texture(), x, cursor, 0.0F, 0.0F,
                        image.width(), image.height(), image.width(), image.height());
                cursor += image.height() + 4;
            }
        }

        detailContentHeight = (cursor + detailScroll.scrollOffset()) - y0;
        graphics.disableScissor();

        detailScroll.setRegion(x, y0, w, h, detailContentHeight);
        detailScroll.updateHover(mouseX, mouseY);
        detailScroll.render(graphics, mouseX, mouseY);
        renderDetailExtras(graphics, entry, x, y0, w, h);
    }

    private static final float OBJECTIVE_SCALE = 0.85F;

    /**
     * Draws one objective row at a smaller, thinner scale: "label ... progress" then a thin bar.
     * Progress reads a check ✔ when complete; otherwise {@code x/y}, or nothing when target is 1
     * (a single-step objective needs no count, just its label and — on completion — the check).
     */
    private int renderObjective(GuiGraphics graphics, ObjectiveView objective, int x, int cursor, int textWidth) {
        boolean done = objective.complete();
        int labelColor = done ? 0xFF7DD87D : JournalTheme.MUTED_COLOR;
        Component label = objective.label();
        if (label.getString().isEmpty()) {
            label = Component.translatable("gui.icecore.journal.objective.generic");
        }
        String progress;
        if (done) {
            progress = "✔";
        } else if (objective.target() <= 1) {
            progress = "";
        } else {
            progress = objective.current() + "/" + objective.target();
        }

        // Smaller + non-bold text via a scaled pose.
        int scaledWidth = Math.round(textWidth / OBJECTIVE_SCALE);
        int progressWidth = progress.isEmpty() ? 0 : font.width(progress);
        String labelText = font.plainSubstrByWidth(label.getString(), scaledWidth - progressWidth - 4);
        graphics.pose().pushPose();
        graphics.pose().translate(x, cursor, 0);
        graphics.pose().scale(OBJECTIVE_SCALE, OBJECTIVE_SCALE, 1.0F);
        graphics.drawString(font, labelText, 0, 0, labelColor, false);
        if (!progress.isEmpty()) {
            graphics.drawString(font, progress, scaledWidth - progressWidth, 0, labelColor, false);
        }
        graphics.pose().popPose();
        cursor += Math.round(font.lineHeight * OBJECTIVE_SCALE) + 2;

        // Thin progress bar: muted track + filled portion.
        int barTop = cursor;
        graphics.fill(x, barTop, x + textWidth, barTop + 2, 0x40FFFFFF);
        int filled = Math.round(textWidth * objective.fraction());
        graphics.fill(x, barTop, x + filled, barTop + 2, done ? 0xFF7DD87D : 0xFFB0B7BE);
        cursor += 5;
        return cursor;
    }

    /** Hook for subclass-specific detail widgets (e.g. the quest mark button). */
    protected void renderDetailExtras(GuiGraphics graphics, EntryView entry, int x, int y, int w, int h) {
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (listScroll.mouseClicked(mouseX, mouseY, button) || detailScroll.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0 && handleDetailClick(mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && clickList(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Hook for subclass detail-panel buttons (mark button). Returns true if consumed. */
    protected boolean handleDetailClick(double mouseX, double mouseY) {
        return false;
    }

    private boolean clickList(double mouseX, double mouseY) {
        int x = listX();
        int y0 = listTop();
        int h = listHeight();
        if (mouseX < x || mouseX > x + listWidth - JournalTheme.PADDING || mouseY < y0 || mouseY > y0 + h) {
            return false;
        }
        int index = (int) ((mouseY - y0 + listScroll.scrollOffset()) / ROW_HEIGHT);
        if (index < 0 || index >= rows.size()) {
            return false;
        }
        Row row = rows.get(index);
        if (row.header() && row.category() != null) {
            if (!collapsed.remove(row.category().id())) {
                collapsed.add(row.category().id());
            }
            rebuildTree();
            return true;
        }
        if (row.entry() != null) {
            selected = row.entry().id();
            onSelect(selected);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (listScroll.mouseDragged(mouseX, mouseY) || detailScroll.mouseDragged(mouseX, mouseY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        listScroll.mouseReleased();
        detailScroll.mouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (detailScroll.mouseScrolled(mouseX, mouseY, delta) || listScroll.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    protected void setSelected(@Nullable ResourceLocation entryId) {
        this.selected = entryId;
    }

    @Nullable
    protected ResourceLocation selectedId() {
        return selected;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        // Notify the server that this journal was opened and closed, driving open_journal objectives.
        com.yinfires.icecore.network.ICECoreNetwork.sendToServer(
                new com.yinfires.icecore.network.ServerBoundJournalClosedPacket(journalType()));
        super.removed();
    }


    /** A flattened tree line: either a category header or an entry. */
    private record Row(boolean header, @Nullable CategoryDefinition category, @Nullable EntryView entry,
                       int depth) {
    }
}
