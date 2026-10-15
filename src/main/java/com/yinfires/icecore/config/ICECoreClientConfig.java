package com.yinfires.icecore.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ICECoreClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SHOW_COMPATIBILITY_DETAILS;
    public static final ForgeConfigSpec.BooleanValue HIDE_ORIGINAL_DETAILS;
    public static final ForgeConfigSpec.BooleanValue HIDE_FOOD_EFFECTS;

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
