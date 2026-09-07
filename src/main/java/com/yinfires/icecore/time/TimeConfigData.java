package com.yinfires.icecore.time;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.yinfires.icecore.building.BuildingData;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class TimeConfigData {
    public static final int FORMAT_VERSION = 1;
    private double dayDurationMultiplier = 1.0D;
    private boolean hudEnabled = true;
    private boolean naturalDaySummaryEnabled = true;
    private String triggerBlockList = "beds";
    private final Set<String> regions = new LinkedHashSet<>();
    private final Map<String, TimeCamera> cameras = new LinkedHashMap<>();
    private TimeTimings timings = new TimeTimings();
    private TimeCompatibilityConfig compatibility = new TimeCompatibilityConfig();

    public double dayDurationMultiplier() { return dayDurationMultiplier; }
    public boolean hudEnabled() { return hudEnabled; }
    public boolean naturalDaySummaryEnabled() { return naturalDaySummaryEnabled; }
    public String triggerBlockList() { return triggerBlockList; }
    public Set<String> regions() { return regions; }
    public Map<String, TimeCamera> cameras() { return cameras; }
    public TimeTimings timings() { return timings; }
    public TimeCompatibilityConfig compatibility() { return compatibility; }
    public DewDropFarmlandTimeConfig dewDropFarmlandGrowth() { return compatibility.dewDropFarmlandGrowth(); }
    public void setDayDurationMultiplier(double value) { dayDurationMultiplier = value; }
    public void setHudEnabled(boolean value) { hudEnabled = value; }
    public void setNaturalDaySummaryEnabled(boolean value) { naturalDaySummaryEnabled = value; }
    public void setTriggerBlockList(String value) { triggerBlockList = value; }

    public static Gson gson() { return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create(); }
    public JsonObject toJson() {
        JsonObject object = gson().toJsonTree(this).getAsJsonObject();
        object.addProperty("formatVersion", FORMAT_VERSION);
        return object;
    }
    public static TimeConfigData fromJson(JsonObject object) {
        if (!object.has("formatVersion") || object.get("formatVersion").getAsInt() != FORMAT_VERSION)
            throw new IllegalArgumentException("unsupported formatVersion");
        TimeConfigData data = gson().fromJson(object, TimeConfigData.class);
        if (!object.has("naturalDaySummaryEnabled")) data.naturalDaySummaryEnabled = true;
        JsonObject compatibilityObject = object.has("compatibility") && object.get("compatibility").isJsonObject()
                ? object.getAsJsonObject("compatibility") : null;
        data.compatibility = TimeCompatibilityConfig.fromJson(compatibilityObject);
        JsonObject timingObject = object.has("timings") && object.get("timings").isJsonObject()
                ? object.getAsJsonObject("timings") : null;
        if (timingObject != null && !timingObject.has("summaryFadeTicks")) {
            if (data.timings.fastForwardTicks() == 60) data.timings.set("fastForward", 20);
            if (data.timings.typewriterTicksPerCharacter() == 1) data.timings.set("typewriterPerCharacter", 2);
            data.timings.set("summaryFade", 10);
        }
        data.validateBasic();
        return data;
    }
    public static TimeConfigData empty() { return new TimeConfigData(); }
    public void validateBasic() {
        if (!Double.isFinite(dayDurationMultiplier) || dayDurationMultiplier < 0.01D || dayDurationMultiplier > 100.0D)
            throw new IllegalArgumentException("dayDurationMultiplier must be 0.01..100");
        if (triggerBlockList == null || triggerBlockList.isBlank() || regions == null || cameras == null || timings == null || compatibility == null)
            throw new IllegalArgumentException("required time fields are missing");
        timings.validateConfiguration();
        compatibility.validate();
        for (Map.Entry<String, TimeCamera> entry : cameras.entrySet()) {
            TimeCamera camera = entry.getValue();
            if (entry.getKey() == null || entry.getKey().isBlank() || camera == null
                    || !Double.isFinite(camera.x()) || !Double.isFinite(camera.y()) || !Double.isFinite(camera.z())
                    || !Float.isFinite(camera.yaw()) || !Float.isFinite(camera.pitch()))
                throw new IllegalArgumentException("invalid camera");
        }
    }
    public void validateReferences(BuildingData building) {
        if (!building.blockLists().containsKey(triggerBlockList))
            throw new IllegalArgumentException("unknown trigger block list: " + triggerBlockList);
        for (String region : regions) if (!building.regions().containsKey(region))
            throw new IllegalArgumentException("unknown region: " + region);
        for (String region : cameras.keySet()) if (!building.regions().containsKey(region))
            throw new IllegalArgumentException("unknown camera region: " + region);
    }
}
