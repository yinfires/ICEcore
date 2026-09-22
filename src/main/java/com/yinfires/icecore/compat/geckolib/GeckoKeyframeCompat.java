package com.yinfires.icecore.compat.geckolib;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import org.apache.commons.lang3.math.NumberUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Preserves vector objects, including easing, for GeckoLib's keyframe baker. */
public final class GeckoKeyframeCompat {
    private GeckoKeyframeCompat() {}

    public static JsonArray wrappedVector(JsonObject keyframe, String side) {
        JsonElement value = keyframe.get(side);
        if (value instanceof JsonObject wrapper
                && wrapper.get("vector") instanceof JsonArray vector && vector.size() == 3) return vector;
        return null;
    }

    public static List<Pair<String, JsonElement>> convertChannel(JsonElement channel) {
        if (!(channel instanceof JsonObject timeline)) return null;
        boolean needsCompat = false;
        for (JsonElement frame : timeline.asMap().values()) {
            if (frame instanceof JsonObject object && !object.has("vector")
                    && (wrappedVector(object, "pre") != null || wrappedVector(object, "post") != null)) {
                needsCompat = true;
                break;
            }
        }
        if (!needsCompat) return null;
        List<Pair<String, JsonElement>> result = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : timeline.entrySet()) {
            if (entry.getValue() instanceof JsonObject frame && !frame.has("vector")) {
                String time = entry.getKey();
                double seconds = NumberUtils.isCreatable(time) ? Double.parseDouble(time) : 0;
                if (frame.has("pre")) result.add(Pair.of(time, readSide(frame, "pre")));
                if (frame.has("post")) result.add(Pair.of(String.valueOf(seconds + 1e-7), readSide(frame, "post")));
            } else {
                result.add(Pair.of(entry.getKey(), entry.getValue()));
            }
        }
        return result;
    }

    private static JsonElement readSide(JsonObject frame, String side) {
        // buildKeyframeStack already accepts vector, easing and easingArgs together.
        if (wrappedVector(frame, side) != null) return frame.get(side);
        return frame.getAsJsonArray(side);
    }
}
