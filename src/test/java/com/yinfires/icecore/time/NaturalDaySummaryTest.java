package com.yinfires.icecore.time;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NaturalDaySummaryTest {
    @Test
    void oldConfigurationDefaultsNaturalSummaryToEnabled() {
        JsonObject json = TimeConfigData.empty().toJson();
        json.remove("naturalDaySummaryEnabled");
        assertTrue(TimeConfigData.fromJson(json).naturalDaySummaryEnabled());
    }

    @Test
    void disabledConfigurationRoundTrips() {
        TimeConfigData data = TimeConfigData.empty();
        data.setNaturalDaySummaryEnabled(false);
        assertFalse(TimeConfigData.fromJson(data.toJson()).naturalDaySummaryEnabled());
    }

    @Test
    void legacyDefaultsMigrateToSlowerSharedSummary() {
        JsonObject json = TimeConfigData.empty().toJson();
        JsonObject timings = json.getAsJsonObject("timings");
        timings.addProperty("fastForwardTicks", 60);
        timings.addProperty("typewriterTicksPerCharacter", 1);
        timings.remove("summaryFadeTicks");
        TimeTimings migrated = TimeConfigData.fromJson(json).timings();
        assertTrue(migrated.fastForwardTicks() == 20);
        assertTrue(migrated.typewriterTicksPerCharacter() == 2);
        assertTrue(migrated.summaryHoldTicks() == 40);
        assertTrue(migrated.summaryFadeTicks() == 10);
    }

    @Test
    void onlyNaturalForwardBoundaryPlaysSummary() {
        assertTrue(TimeService.shouldPlayNaturalSummary(23_999L, 24_000L, true, true, true, false));
        assertTrue(TimeService.shouldPlayNaturalSummary(23_999L, 48_000L, true, true, true, false));
        assertFalse(TimeService.shouldPlayNaturalSummary(24_000L, 24_001L, false, true, true, false));
        assertFalse(TimeService.shouldPlayNaturalSummary(24_000L, 23_999L, true, true, true, false));
        assertFalse(TimeService.shouldPlayNaturalSummary(23_999L, 24_000L, true, false, true, false));
        assertFalse(TimeService.shouldPlayNaturalSummary(23_999L, 24_000L, true, true, false, false));
        assertFalse(TimeService.shouldPlayNaturalSummary(23_999L, 24_000L, true, true, true, true));
    }

    @Test
    void bothAnimationsUseSharedRendererAndNaturalPathHasNoCameraOrInputLock() throws IOException {
        String cutscene = source("TimeCutsceneClient.java");
        String natural = source("NaturalDaySummaryClient.java");
        assertTrue(cutscene.contains("DaySummaryRenderer.render"));
        assertTrue(natural.contains("DaySummaryRenderer.render"));
        assertFalse(natural.contains("setCameraEntity"));
        assertFalse(natural.contains("KeyMapping.releaseAll"));
        assertFalse(natural.contains("RenderGuiOverlayEvent"));
    }

    private static String source(String name) throws IOException {
        return Files.readString(Path.of("src/main/java/com/yinfires/icecore/time", name), StandardCharsets.UTF_8);
    }
}
