package com.yinfires.icecore.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ICECoreClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SHOW_COMPATIBILITY_DETAILS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("interface");
        SHOW_COMPATIBILITY_DETAILS = builder
                .comment("Shows compatibility details in the ICEcore configuration screen.")
                .translation("config.icecore.interface.show_compatibility_details")
                .define("showCompatibilityDetails", false);
        builder.pop();
        SPEC = builder.build();
    }

    private ICECoreClientConfig() {
    }

    public static boolean showCompatibilityDetails() {
        return SHOW_COMPATIBILITY_DETAILS.get();
    }

    public static void setShowCompatibilityDetails(boolean value) {
        SHOW_COMPATIBILITY_DETAILS.set(value);
        SPEC.save();
    }
}
