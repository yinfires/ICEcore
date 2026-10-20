package com.yinfires.icecore.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ICECoreClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SHOW_COMPATIBILITY_DETAILS;
    public static final ForgeConfigSpec.BooleanValue HIDE_ORIGINAL_DETAILS;
    public static final ForgeConfigSpec.BooleanValue HIDE_FOOD_EFFECTS;

    public static final ForgeConfigSpec.BooleanValue SHELF_DISPLAY_ENABLED;
    public static final ForgeConfigSpec.IntValue SHELF_DISPLAY_DISTANCE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("interface");
        SHOW_COMPATIBILITY_DETAILS = builder
                .comment("Shows compatibility details in the ICEcore configuration screen.")
                .translation("config.icecore.interface.show_compatibility_details")
                .define("showCompatibilityDetails", false);
        builder.pop();
        builder.push("tooltip");
        HIDE_ORIGINAL_DETAILS = builder.comment("Hide original item details, JEI mod names and Ponder prompts; keep added recipe/menu information.")
                .translation("config.icecore.tooltip.hide_original_details").define("hideOriginalDetails", true);
        HIDE_FOOD_EFFECTS = builder.comment("Hide food effect tooltips, including duration, chance and effect attributes.")
                .translation("config.icecore.tooltip.hide_food_effects").define("hideFoodEffects", true);
        builder.pop();
        builder.translation("config.icecore.shelf_display").push("shelfDisplay");
        SHELF_DISPLAY_ENABLED=builder.comment("Show displayed items on ICEcore shelves only.")
                .translation("config.icecore.shelf_display.enabled").define("enabled",true);
        SHELF_DISPLAY_DISTANCE=builder.comment("Maximum item display distance from the camera to the lower block center, in blocks.")
                .translation("config.icecore.shelf_display.distance").defineInRange("distance",64,1,64);
        builder.pop();
        SPEC = builder.build();
    }

    private ICECoreClientConfig() {
    }

    public static boolean showCompatibilityDetails() {
        return SHOW_COMPATIBILITY_DETAILS.get();
    }

    public static boolean hideOriginalDetails() { return HIDE_ORIGINAL_DETAILS.get(); }
    public static boolean hideFoodEffects() { return HIDE_FOOD_EFFECTS.get(); }

    public static void setShowCompatibilityDetails(boolean value) {
        SHOW_COMPATIBILITY_DETAILS.set(value);
        SPEC.save();
    }
}
