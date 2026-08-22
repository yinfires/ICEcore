package com.yinfires.icecore.building;

/** Client rule cache entry point. The class contains no client-only types so the packet is safe on a server. */
public final class BuildingClientState {
    private static volatile BuildingData data = BuildingData.empty();
    private static volatile long revision;

    private BuildingClientState() {
    }

    public static void accept(String json, long newRevision) {
        try {
            BuildingData parsed = BuildingData.fromJson(com.google.gson.JsonParser.parseString(json).getAsJsonObject());
            if (newRevision >= revision) {
                data = parsed;
                revision = newRevision;
            }
        } catch (RuntimeException ignored) {
            // Keep the last known-good snapshot when a malformed packet arrives.
        }
    }

    public static BuildingData data() {
        return data;
    }

    public static long revision() {
        return revision;
    }

    public static void reset() {
        data = BuildingData.empty();
        revision = 0L;
    }
}
