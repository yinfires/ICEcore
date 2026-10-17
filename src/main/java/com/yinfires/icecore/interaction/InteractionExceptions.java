package com.yinfires.icecore.interaction;

import com.google.gson.*;
import java.util.*;

public record InteractionExceptions(Map<Location, String> entries) {
    public InteractionExceptions { entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries)); }
    public record Location(String dimension, int x, int y, int z) {
        public Location { InteractionConfig.id(dimension); }
        @Override public String toString() { return dimension + " " + x + " " + y + " " + z; }
    }
    public static InteractionExceptions empty() { return new InteractionExceptions(Map.of()); }
    public static InteractionExceptions parse(JsonElement element) {
        JsonObject object = element.getAsJsonObject(); InteractionConfig.fields(object, "formatVersion", "exceptions"); InteractionConfig.version(object);
        if (!object.has("exceptions") || !object.get("exceptions").isJsonArray()) throw new IllegalArgumentException("Expected exceptions array");
        Map<Location, String> entries = new LinkedHashMap<>();
        for (JsonElement entry : object.getAsJsonArray("exceptions")) {
            JsonObject value = entry.getAsJsonObject(); InteractionConfig.fields(value, "dimension", "pos", "block");
            JsonArray pos = value.getAsJsonArray("pos");
            if (pos == null || pos.size() != 3) throw new IllegalArgumentException("Expected three coordinates");
            int[] xyz = new int[3];
            for (int i = 0; i < 3; i++) {
                if (!pos.get(i).isJsonPrimitive() || !pos.get(i).getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Expected integer coordinate");
                xyz[i] = new java.math.BigDecimal(pos.get(i).getAsString()).intValueExact();
            }
            Location location = new Location(InteractionConfig.string(value.get("dimension")), xyz[0], xyz[1], xyz[2]);
            String block = InteractionConfig.id(InteractionConfig.string(value.get("block")));
            if (entries.containsKey(location) && !entries.get(location).equals(block)) throw new IllegalArgumentException("Conflicting exception: " + location);
            entries.put(location, block);
        }
        return new InteractionExceptions(entries);
    }
    public JsonObject json() {
        JsonObject object = new JsonObject(); object.addProperty("formatVersion", 1); JsonArray array = new JsonArray();
        entries.forEach((location, block) -> {
            JsonObject entry = new JsonObject(); entry.addProperty("dimension", location.dimension());
            JsonArray pos = new JsonArray(); pos.add(location.x()); pos.add(location.y()); pos.add(location.z());
            entry.add("pos", pos); entry.addProperty("block", block); array.add(entry);
        }); object.add("exceptions", array); return object;
    }
    public InteractionExceptions edit(Location location, String block) {
        Map<Location, String> edited = new LinkedHashMap<>(entries);
        if (block == null) edited.remove(location); else edited.put(location, InteractionConfig.id(block));
        return new InteractionExceptions(edited);
    }
}
