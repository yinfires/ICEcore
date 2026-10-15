package com.yinfires.icecore.tooltip;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
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

/** Immutable snapshots; failed reloads never replace the last valid snapshot. */
public final class ItemDescriptions {
    private volatile Map<ResourceLocation, List<Component>> entries = Map.of();

    public List<Component> get(ResourceLocation item) { return entries.get(item); }

    public void reload(Path path) {
        try {
            if (Files.notExists(path)) {
                entries = Map.of();
                return;
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                entries = parse(JsonParser.parseReader(reader));
            }
        } catch (Exception exception) {
            LogUtils.getLogger().error("Cannot reload item descriptions from {}; keeping previous descriptions: {}",
                    path, exception.getMessage());
        }
    }

    static Map<ResourceLocation, List<Component>> parse(JsonElement json) {
        if (!json.isJsonObject() || !json.getAsJsonObject().has("items")
                || !json.getAsJsonObject().get("items").isJsonObject()) {
            throw new IllegalArgumentException("Expected an object containing an items object");
        }
        Map<ResourceLocation, List<Component>> result = new HashMap<>();
        JsonObject items = json.getAsJsonObject().getAsJsonObject("items");
        for (var entry : items.entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            if (id == null || !entry.getKey().contains(":")) {
                throw new IllegalArgumentException("Invalid full item ID: " + entry.getKey());
            }
            if (!entry.getValue().isJsonArray()) {
                throw new IllegalArgumentException("Expected description array for " + id);
            }
            List<Component> lines = new ArrayList<>();
            for (JsonElement line : entry.getValue().getAsJsonArray()) {
                if (!(line.isJsonObject() || line.isJsonPrimitive() && line.getAsJsonPrimitive().isString())) {
                    throw new IllegalArgumentException("Expected string or text component for " + id);
                }
                Component component = Component.Serializer.fromJson(line);
                if (component == null) throw new IllegalArgumentException("Null description for " + id);
                lines.add(component);
            }
            result.put(id, List.copyOf(lines));
        }
        return Map.copyOf(result);
    }
}
