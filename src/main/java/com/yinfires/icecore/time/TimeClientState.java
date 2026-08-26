package com.yinfires.icecore.time;

public final class TimeClientState {
    private static TimeConfigData config = TimeConfigData.empty();
    private static long dayTime;
    private static long receivedMillis;
    private static boolean daylightCycle;
    private static boolean initialized;
    private TimeClientState() {}
    public static void accept(String json, long serverDayTime, boolean cycle) {
        config = TimeConfigData.fromJson(com.google.gson.JsonParser.parseString(json).getAsJsonObject());
        dayTime = serverDayTime; daylightCycle = cycle; receivedMillis = System.currentTimeMillis(); initialized = true;
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.dimension() == net.minecraft.world.level.Level.OVERWORLD && !TimeCutsceneClient.active()) minecraft.level.setDayTime(serverDayTime);
    }
    public static double displayedDayTime() {
        if (!initialized || !daylightCycle || TimeCutsceneClient.active()) return dayTime;
        double ticks = (System.currentTimeMillis() - receivedMillis) / 50.0D;
        return dayTime + ticks / config.dayDurationMultiplier();
    }
    public static void setVisualDayTime(double value) { dayTime = (long) value; receivedMillis = System.currentTimeMillis(); net.minecraft.client.Minecraft minecraft=net.minecraft.client.Minecraft.getInstance();if(minecraft.level!=null&&minecraft.level.dimension()==net.minecraft.world.level.Level.OVERWORLD)minecraft.level.setDayTime((long)value); }
    public static TimeConfigData config() { return config; }
    public static boolean initialized() { return initialized; }
    public static boolean daylightCycle() { return daylightCycle; }
    public static void reset() { config = TimeConfigData.empty(); initialized = false; TimeHud.resetAnimation(); }
}
