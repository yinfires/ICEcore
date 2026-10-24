package com.yinfires.icecore.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ICECoreConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue DISABLE_FOOD_CONSUMPTION;
    public static final ForgeConfigSpec.BooleanValue ENABLE_MOD_COMPATIBILITY;
    public static final ForgeConfigSpec.BooleanValue DISABLE_ALL_ADVANCEMENTS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("food");
        DISABLE_FOOD_CONSUMPTION = builder
                .comment("Prevents players from consuming every item that exposes FoodProperties.")
                .translation("config.icecore.food.disable_consumption")
                .define("disableConsumption", false);
        ENABLE_MOD_COMPATIBILITY = builder
                .comment("Recognizes drink and skewer item classes and tags supplied by supported mods.")
                .translation("config.icecore.food.enable_mod_compatibility")
                .define("enableModCompatibility", true);
        builder.pop();
        builder.push("advancements");
        DISABLE_ALL_ADVANCEMENTS = builder
                .comment("Disables every advancement, including hidden recipe unlocks. Restart or reload server resources after changing.")
                .translation("config.icecore.advancements.disable_all")
                .define("disableAll", false);
        builder.pop();
        SPEC = builder.build();
    }

    private ICECoreConfig() {
    }

    public static boolean disableFoodConsumption() {
        return DISABLE_FOOD_CONSUMPTION.get();
    }

    public static void setDisableFoodConsumption(boolean disabled) {
        DISABLE_FOOD_CONSUMPTION.set(disabled);
        SPEC.save();
    }

    public static boolean enableModCompatibility() {
        return ENABLE_MOD_COMPATIBILITY.get();
    }

    public static boolean disableAllAdvancements() {
        return DISABLE_ALL_ADVANCEMENTS.get();
    }
}
