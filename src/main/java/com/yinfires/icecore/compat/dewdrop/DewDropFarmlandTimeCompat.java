package com.yinfires.icecore.compat.dewdrop;

import com.mojang.logging.LogUtils;
import com.yinfires.icecore.time.DailyTimeWindow;
import com.yinfires.icecore.time.DewDropFarmlandTimeConfig;
import com.yinfires.icecore.time.TimeConfigManager;
import com.yinfires.icecore.time.TimeService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/** Adapts Dew Drop 9.0's sampled day-time window to ICEcore's variable day-time rate. */
public final class DewDropFarmlandTimeCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "dew_drop_farmland_growth";
    private static final String SUPPORTED_VERSION = "9.0";
    private static final int UPSTREAM_SCHEDULE_TICKS = 10;
    private static final int DISABLED_WINDOW = 24_000;

    private static VarHandle dailyTimeMin;
    private static boolean available;
    private static boolean overrideActive;
    private static int upstreamWindow;
    private static int pulseTicks;
    private static boolean pulseActiveThisTick;
    private static boolean pendingPulse;
    private static long pendingDay = Long.MIN_VALUE;
    private static boolean configuredEnabled;
    private static int configuredStart;
    private static int configuredEnd;

    private DewDropFarmlandTimeCompat() {}

    public static void start(MinecraftServer server) {
        stop();
        var container = ModList.get().getModContainerById(MOD_ID);
        if (container.isEmpty()) return;
        String version = container.get().getModInfo().getVersion().toString();
        if (!SUPPORTED_VERSION.equals(version)) {
            LOGGER.info("Dew Drop Farmland Growth {} is not the verified 9.0 implementation; ICEcore time-window compatibility is inactive", version);
            return;
        }
        try {
            Class<?> config = Class.forName("cool.bot.dewdropfarmland.Config", false,
                    DewDropFarmlandTimeCompat.class.getClassLoader());
            dailyTimeMin = MethodHandles.publicLookup().findStaticVarHandle(config, "dailyTimeMin", int.class);
            available = true;
            refreshConfiguration();
            LOGGER.info("Enabled ICEcore day-time compatibility for Dew Drop Farmland Growth 9.0");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            dailyTimeMin = null;
            available = false;
            LOGGER.warn("Could not enable Dew Drop Farmland Growth time-window compatibility", exception);
        }
    }

    public static void stop() {
        restoreUpstreamWindow();
        dailyTimeMin = null;
        available = false;
        pulseTicks = 0;
        pulseActiveThisTick = false;
        pendingPulse = false;
        pendingDay = Long.MIN_VALUE;
        configuredEnabled = false;
        configuredStart = 0;
        configuredEnd = 0;
    }

    public static void observeDayTimeChange(ServerLevel level, long previous, long current) {
        if (!available || level.dimension() != Level.OVERWORLD) return;
        refreshConfiguration();
        if (!configuredEnabled) return;
        long previousDay = Math.floorDiv(previous, 24_000L);
        long currentDay = Math.floorDiv(current, 24_000L);
        if (current < previous && currentDay < previousDay) {
            TimeService.clearDewDropProcessedDay();
            pendingPulse = false;
            pendingDay = Long.MIN_VALUE;
            pulseTicks = 0;
            pulseActiveThisTick = false;
        }
        if (current <= previous || !level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) return;
        long boundary = DailyTimeWindow.latestEnteredBoundary(previous, current, configuredStart, configuredEnd);
        if (boundary == DailyTimeWindow.NONE) return;
        long day = Math.floorDiv(boundary, 24_000L);
        if (TimeService.dewDropProcessedDay() == day) return;
        pendingDay = day;
        pendingPulse = true;
    }

    public static void beginLevelTick(ServerLevel level) {
        if (!available || level.dimension() != Level.OVERWORLD) return;
        refreshConfiguration();
        if (!configuredEnabled) return;
        boolean daylightCycle = level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        if (pendingPulse && daylightCycle) {
            pendingPulse = false;
            TimeService.markDewDropProcessedDay(pendingDay);
            pendingDay = Long.MIN_VALUE;
            pulseTicks = UPSTREAM_SCHEDULE_TICKS;
        } else if (pendingPulse) {
            pendingPulse = false;
            pendingDay = Long.MIN_VALUE;
        }
        upstreamWindow = readWindow();
        overrideActive = true;
        pulseActiveThisTick = daylightCycle && pulseTicks > 0;
        writeWindow(pulseActiveThisTick ? Math.floorMod(level.getDayTime(), 24_000L) : DISABLED_WINDOW);
    }

    public static void endLevelTick(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) return;
        restoreUpstreamWindow();
        if (available && configuredEnabled && pulseActiveThisTick) pulseTicks--;
        pulseActiveThisTick = false;
    }

    private static void refreshConfiguration() {
        DewDropFarmlandTimeConfig config = TimeConfigManager.get().data().dewDropFarmlandGrowth();
        if (config.enabled() == configuredEnabled
                && config.dailyWindowStart() == configuredStart
                && config.dailyWindowEnd() == configuredEnd) return;
        restoreUpstreamWindow();
        configuredEnabled = config.enabled();
        configuredStart = config.dailyWindowStart();
        configuredEnd = config.dailyWindowEnd();
        pulseTicks = 0;
        pulseActiveThisTick = false;
        pendingPulse = false;
        pendingDay = Long.MIN_VALUE;
    }

    private static int readWindow() {
        try {
            return (int) dailyTimeMin.get();
        } catch (RuntimeException exception) {
            disable(exception);
            return DISABLED_WINDOW;
        }
    }

    private static void writeWindow(long value) {
        try {
            dailyTimeMin.set((int) value);
        } catch (RuntimeException exception) {
            disable(exception);
        }
    }

    private static void restoreUpstreamWindow() {
        if (!overrideActive || dailyTimeMin == null) return;
        overrideActive = false;
        try {
            dailyTimeMin.set(upstreamWindow);
        } catch (RuntimeException exception) {
            disable(exception);
        }
    }

    private static void disable(RuntimeException exception) {
        available = false;
        overrideActive = false;
        pulseTicks = 0;
        pulseActiveThisTick = false;
        pendingPulse = false;
        pendingDay = Long.MIN_VALUE;
        LOGGER.warn("Disabled Dew Drop Farmland Growth time-window compatibility after a field access failure", exception);
    }
}
