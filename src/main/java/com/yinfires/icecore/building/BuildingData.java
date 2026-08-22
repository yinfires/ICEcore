package com.yinfires.icecore.building;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BuildingData {
    public static final int FORMAT_VERSION = 1;
    private final Map<String, RegionDefinition> regions = new LinkedHashMap<>();
    private final Map<String, BlockListDefinition> blockLists = new LinkedHashMap<>();

    public Map<String, RegionDefinition> regions() {
        return regions;
    }

    public Map<String, BlockListDefinition> blockLists() {
        return blockLists;
    }

    public static Gson gson() {
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    public JsonObject toJson() {
        JsonObject object = gson().toJsonTree(this).getAsJsonObject();
        object.addProperty("formatVersion", FORMAT_VERSION);
        return object;
    }

    public static BuildingData fromJson(JsonObject object) {
        if (!object.has("formatVersion") || object.get("formatVersion").getAsInt() != FORMAT_VERSION) {
            throw new IllegalArgumentException("unsupported formatVersion");
        }
        BuildingData data = gson().fromJson(object, BuildingData.class);
        if (data == null) {
            throw new IllegalArgumentException("empty building data");
        }
        validate(data);
        return data;
    }

    public static BuildingData empty() {
        return new BuildingData();
    }

    private static void validate(BuildingData data) {
        if (data.regions == null || data.blockLists == null) {
            throw new IllegalArgumentException("regions and blockLists are required");
        }
        for (Map.Entry<String, RegionDefinition> entry : data.regions.entrySet()) {
            if (entry.getValue() == null || entry.getValue().name() == null) {
                throw new IllegalArgumentException("invalid region definition");
            }
            if (!entry.getKey().equals(entry.getValue().name())) {
                throw new IllegalArgumentException("region key/name mismatch: " + entry.getKey());
            }
        }
        for (Map.Entry<String, BlockListDefinition> entry : data.blockLists.entrySet()) {
            if (entry.getValue() == null || entry.getValue().name() == null
                    || entry.getValue().entries() == null
                    || entry.getValue().regions() == null
                    || entry.getValue().supports() == null
                    || entry.getValue().allowedFaces() == null) {
                throw new IllegalArgumentException("invalid block list definition");
            }
            if (!entry.getKey().equals(entry.getValue().name())) {
                throw new IllegalArgumentException("block list key/name mismatch: " + entry.getKey());
            }
            for (String face : entry.getValue().allowedFaces()) {
                if ("side".equalsIgnoreCase(face)) {
                    throw new IllegalArgumentException("side is a command shortcut and cannot be persisted");
                }
            }
            FaceMask.fromNames(entry.getValue().allowedFaces());
            for (String region : entry.getValue().regions()) {
                if (!data.regions.containsKey(region)) throw new IllegalArgumentException("unknown region binding: " + region);
            }
            for (String support : entry.getValue().supports()) {
                if (!data.blockLists.containsKey(support)) throw new IllegalArgumentException("unknown support binding: " + support);
            }
        }
    }
}
