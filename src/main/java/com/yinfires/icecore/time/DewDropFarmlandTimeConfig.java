package com.yinfires.icecore.time;

import com.google.gson.JsonObject;

public final class DewDropFarmlandTimeConfig {
    public static final int DEFAULT_WINDOW_START = 23_990;
    public static final int DEFAULT_WINDOW_END = 24_000;

    private boolean enabled = true;
    private int dailyWindowStart = DEFAULT_WINDOW_START;
    private int dailyWindowEnd = DEFAULT_WINDOW_END;

    public boolean enabled() { return enabled; }
    public int dailyWindowStart() { return dailyWindowStart; }
    public int dailyWindowEnd() { return dailyWindowEnd; }

    static DewDropFarmlandTimeConfig fromJson(JsonObject object) {
        DewDropFarmlandTimeConfig config = new DewDropFarmlandTimeConfig();
        if (object == null) return config;
        if (object.has("enabled")) config.enabled = object.get("enabled").getAsBoolean();
        if (object.has("dailyWindowStart")) config.dailyWindowStart = object.get("dailyWindowStart").getAsInt();
        if (object.has("dailyWindowEnd")) config.dailyWindowEnd = object.get("dailyWindowEnd").getAsInt();
        return config;
    }

    void validate() {
        if (dailyWindowStart < 0 || dailyWindowStart >= TimeCalendar.DAY_TICKS)
            throw new IllegalArgumentException("dewDropFarmlandGrowth.dailyWindowStart must be 0..23999");
        if (dailyWindowEnd <= dailyWindowStart || dailyWindowEnd > TimeCalendar.DAY_TICKS)
            throw new IllegalArgumentException("dewDropFarmlandGrowth.dailyWindowEnd must be greater than dailyWindowStart and at most 24000");
    }
}
