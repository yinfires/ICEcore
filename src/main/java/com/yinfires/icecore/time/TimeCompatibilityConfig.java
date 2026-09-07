package com.yinfires.icecore.time;

import com.google.gson.JsonObject;

public final class TimeCompatibilityConfig {
    private DewDropFarmlandTimeConfig dewDropFarmlandGrowth = new DewDropFarmlandTimeConfig();

    public DewDropFarmlandTimeConfig dewDropFarmlandGrowth() { return dewDropFarmlandGrowth; }

    static TimeCompatibilityConfig fromJson(JsonObject object) {
        TimeCompatibilityConfig config = new TimeCompatibilityConfig();
        if (object != null) {
            JsonObject dewDrop = object.has("dewDropFarmlandGrowth")
                    && object.get("dewDropFarmlandGrowth").isJsonObject()
                    ? object.getAsJsonObject("dewDropFarmlandGrowth") : null;
            config.dewDropFarmlandGrowth = DewDropFarmlandTimeConfig.fromJson(dewDrop);
        }
        return config;
    }

    void validate() { dewDropFarmlandGrowth.validate(); }
}
