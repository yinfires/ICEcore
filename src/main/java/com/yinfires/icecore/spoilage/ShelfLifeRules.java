package com.yinfires.icecore.spoilage;

import com.google.gson.*;
import java.util.*;

/** External configuration is authoritative; no implicit edible-item fallback. */
public record ShelfLifeRules(Map<String, Integer> items, Set<String> excluded) {
    public static final ShelfLifeRules EMPTY = new ShelfLifeRules(Map.of(), Set.of());
    public static ShelfLifeRules parse(JsonElement json) {
        JsonObject root = json.getAsJsonObject();
        Set<String> excluded = new HashSet<>();
        excluded.add("minecraft:water_bucket");
        if (root.has("excluded")) for (JsonElement value : root.getAsJsonArray("excluded")) excluded.add(id(value));
        Map<String, Integer> items = new HashMap<>();
        for (var group : root.getAsJsonObject("groups").entrySet()) {
            int days = Integer.parseInt(group.getKey());
            if (days < 1) throw new IllegalArgumentException("Shelf life must be positive");
            for (JsonElement value : group.getValue().getAsJsonArray()) {
                String id = id(value);
                if (items.putIfAbsent(id, days) != null) throw new IllegalArgumentException("Duplicate item: " + id);
            }
        }
        excluded.forEach(items::remove);
        return new ShelfLifeRules(Map.copyOf(items), Set.copyOf(excluded));
    }
    private static String id(JsonElement value) {
        String id = value.getAsString();
        if (!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IllegalArgumentException("Invalid item ID: " + id);
        return id;
    }
}
