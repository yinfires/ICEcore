package com.yinfires.icecore.compat.cozycafe.range;

import com.google.gson.JsonParser;

/** Client cache used only for the active adjustment wand's range border. */
public final class CozyCafeRangeClientState {
    private static volatile CozyCafeRangeData data = CozyCafeRangeData.empty();
    private static volatile long revision;

    private CozyCafeRangeClientState() {
    }

    public static void accept(String json, long newRevision) {
        try {
            CozyCafeRangeData parsed = CozyCafeRangeData.fromJson(
                    JsonParser.parseString(json).getAsJsonObject());
            if (newRevision >= revision) {
                data = parsed;
                revision = newRevision;
            }
        } catch (RuntimeException ignored) {
            // Keep the last known-good snapshot.
        }
    }

    public static CozyCafeRangeData data() {
        return data;
    }

    public static void reset() {
        data = CozyCafeRangeData.empty();
        revision = 0L;
    }
}
