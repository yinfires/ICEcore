package com.yinfires.icecore.compat.cozycafe.range;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** Serialized world data for per-computer CozyCafe recognition ranges. */
public final class CozyCafeRangeData {
    public static final int FORMAT_VERSION = 1;
    public static final int MAX_RANGES = 1_024;
    private final Map<String, CozyCafeRangeDefinition> ranges = new LinkedHashMap<>();

    public Map<String, CozyCafeRangeDefinition> ranges() {
        return ranges;
    }

    public static Gson gson() {
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    public JsonObject toJson() {
        JsonObject object = gson().toJsonTree(this).getAsJsonObject();
        object.addProperty("formatVersion", FORMAT_VERSION);
        return object;
    }

    public static CozyCafeRangeData fromJson(JsonObject object) {
        if (!object.has("formatVersion") || object.get("formatVersion").getAsInt() != FORMAT_VERSION) {
            throw new IllegalArgumentException("unsupported formatVersion");
        }
        CozyCafeRangeData data = gson().fromJson(object, CozyCafeRangeData.class);
        if (data == null || data.ranges == null) {
            throw new IllegalArgumentException("ranges are required");
        }
        if (data.ranges.size() > MAX_RANGES) {
            throw new IllegalArgumentException("too many CozyCafe ranges");
        }
        for (Map.Entry<String, CozyCafeRangeDefinition> entry : data.ranges.entrySet()) {
            if (entry.getValue() == null) {
                throw new IllegalArgumentException("invalid CozyCafe range definition");
            }
            entry.getValue().validate();
            if (!entry.getKey().equals(entry.getValue().key())) {
                throw new IllegalArgumentException("CozyCafe range key/location mismatch: " + entry.getKey());
            }
        }
        return data;
    }

    public static CozyCafeRangeData empty() {
        return new CozyCafeRangeData();
    }
}
