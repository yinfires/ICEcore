package com.yinfires.icecore.tooltip;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Immutable snapshots; failed reloads never replace the last valid snapshot. */
public final class ItemDescriptions {
    record Entry(List<Component> lines, List<Component> common, List<ResourceLocation> blocks) {}
    record Snapshot(Map<ResourceLocation, Entry> items, Map<ResourceLocation, Entry> blocks) {}
    private volatile Snapshot snapshot = new Snapshot(Map.of(), Map.of());

    public List<Component> get(ResourceLocation item) {
        Entry entry = snapshot.items.get(item);
        return entry == null ? null : entry.lines;
    }

    public List<Component> common(ResourceLocation block, ResourceLocation item) {
        Snapshot current = snapshot;
        Entry entry = current.blocks.get(block);
        if (entry == null) entry = current.items.get(item);
        return entry == null ? List.of() : entry.common;
    }

    public void reload(Path path) {
        try {
            if (Files.notExists(path)) {
                snapshot = new Snapshot(Map.of(), Map.of());
                return;
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                snapshot = parseSnapshot(JsonParser.parseReader(reader));
            }
        } catch (Exception exception) {
            LogUtils.getLogger().error("Cannot reload item descriptions from {}; keeping previous descriptions: {}",
                    path, exception.getMessage());
        }
    }

    static Map<ResourceLocation, List<Component>> parse(JsonElement json) {
        Map<ResourceLocation, List<Component>> result = new HashMap<>();
        parseSnapshot(json).items.forEach((id, entry) -> result.put(id, entry.lines));
        return Map.copyOf(result);
    }

    static Snapshot parseSnapshot(JsonElement json) {
        if (!json.isJsonObject() || !json.getAsJsonObject().has("items")
                || !json.getAsJsonObject().get("items").isJsonObject()) {
            throw new IllegalArgumentException("Expected an object containing an items object");
        }
        Map<ResourceLocation, Entry> result = new HashMap<>();
        Map<ResourceLocation, Entry> blocks = new HashMap<>();
        JsonObject items = json.getAsJsonObject().getAsJsonObject("items");
        for (var entry : items.entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            if (id == null || !entry.getKey().contains(":")) {
                throw new IllegalArgumentException("Invalid full item ID: " + entry.getKey());
            }
            Entry parsed = parseEntry(entry.getValue());
            result.put(id, parsed);
            for (ResourceLocation block : parsed.blocks) {
                if (blocks.putIfAbsent(block, parsed) != null)
                    throw new IllegalArgumentException("Duplicate explicit block association: " + block);
            }
        }
        return new Snapshot(Map.copyOf(result), Map.copyOf(blocks));
    }

    private static Entry parseEntry(JsonElement value) {
        if (value.isJsonArray()) return new Entry(textLines(value, null), List.of(), List.of());
        if (!value.isJsonObject()) throw new IllegalArgumentException("Expected description array or section object");
        JsonObject object = value.getAsJsonObject();
        for (String field : object.keySet()) {
            if (!Set.of("description", "common", "placement", "actions", "blocks").contains(field))
                throw new IllegalArgumentException("Unknown description field: " + field);
        }
        List<Component> lines = new ArrayList<>(textLines(object.get("description"), ChatFormatting.GRAY));
        List<Component> common = textLines(object.get("common"), ChatFormatting.GRAY);
        lines.addAll(common);
        List<Component> places = textLines(object.get("placement"), null);
        if (!places.isEmpty()) {
            var joined = Component.empty();
            for (int i = 0; i < places.size(); i++) {
                if (i > 0) joined.append(Component.translatable("tooltip.icecore.description.place_separator"));
                joined.append(places.get(i));
            }
            lines.add(Component.translatable("tooltip.icecore.description.placement", joined).withStyle(ChatFormatting.GREEN));
        }
        if (object.has("actions")) {
            if (!object.get("actions").isJsonArray()) throw new IllegalArgumentException("Expected actions array");
            for (JsonElement action : object.getAsJsonArray("actions")) {
                if (!action.isJsonObject()) throw new IllegalArgumentException("Expected action object");
                JsonObject row = action.getAsJsonObject();
                if (!row.keySet().equals(Set.of("keys", "text"))) throw new IllegalArgumentException("An action requires keys and text");
                List<String> keys = strings(row.get("keys"));
                if (keys.isEmpty()) throw new IllegalArgumentException("An action requires at least one key");
                Component body = text(row.get("text"), null);
                if (body.getStyle().getColor() == null) body = body.copy().withStyle(style -> style.withColor(0xFFFFAA));
                lines.add(new DescriptionAction(keys, body).component());
            }
        }
        List<ResourceLocation> associations = new ArrayList<>();
        if (object.has("blocks")) for (String block : strings(object.get("blocks"))) {
            ResourceLocation id = ResourceLocation.tryParse(block);
            if (id == null || !block.contains(":")) throw new IllegalArgumentException("Invalid full block ID: " + block);
            associations.add(id);
        }
        return new Entry(List.copyOf(lines), common, List.copyOf(associations));
    }

    private static List<String> strings(JsonElement value) {
        if (value == null || !value.isJsonArray()) throw new IllegalArgumentException("Expected string array");
        List<String> result = new ArrayList<>();
        for (JsonElement element : value.getAsJsonArray()) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString() || element.getAsString().isBlank())
                throw new IllegalArgumentException("Expected nonblank string");
            result.add(element.getAsString().trim());
        }
        return List.copyOf(result);
    }

    private static List<Component> textLines(JsonElement value, ChatFormatting color) {
        if (value == null) return List.of();
        if (!value.isJsonArray()) throw new IllegalArgumentException("Expected description lines array");
        List<Component> result = new ArrayList<>();
        for (JsonElement line : value.getAsJsonArray()) result.add(text(line, color));
        return List.copyOf(result);
    }

    private static Component text(JsonElement value, ChatFormatting color) {
        if (value == null || !(value.isJsonObject() || value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()))
            throw new IllegalArgumentException("Expected string or text component");
        var component = Component.Serializer.fromJson(value);
        if (component == null) throw new IllegalArgumentException("Null description");
        if (color != null && component.getStyle().getColor() == null) component.withStyle(color);
        return component;
    }
}
