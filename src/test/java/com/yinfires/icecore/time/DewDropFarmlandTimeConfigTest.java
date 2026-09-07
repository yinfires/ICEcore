package com.yinfires.icecore.time;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DewDropFarmlandTimeConfigTest {
    @Test void oldTimeConfigGetsEndOfDayDefaults() {
        JsonObject json = TimeConfigData.empty().toJson();
        json.remove("compatibility");
        DewDropFarmlandTimeConfig config = TimeConfigData.fromJson(json).dewDropFarmlandGrowth();
        assertTrue(config.enabled());
        assertEquals(23_990, config.dailyWindowStart());
        assertEquals(24_000, config.dailyWindowEnd());
    }

    @Test void missingDewDropSectionGetsDefaults() {
        JsonObject json = TimeConfigData.empty().toJson();
        json.add("compatibility", new JsonObject());
        DewDropFarmlandTimeConfig config = TimeConfigData.fromJson(json).dewDropFarmlandGrowth();
        assertTrue(config.enabled());
        assertEquals(23_990, config.dailyWindowStart());
        assertEquals(24_000, config.dailyWindowEnd());
    }

    @Test void rejectsInvalidWindow() {
        JsonObject json = TimeConfigData.empty().toJson();
        JsonObject compat = json.getAsJsonObject("compatibility").getAsJsonObject("dewDropFarmlandGrowth");
        compat.addProperty("dailyWindowStart", 24_000);
        assertThrows(IllegalArgumentException.class, () -> TimeConfigData.fromJson(json));
    }
}
