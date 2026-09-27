package com.yinfires.icecore.food;

import com.yinfires.icecore.config.ICECoreConfig;

public final class FoodConsumptionClientState {
    private static boolean synchronizedFromServer;
    private static boolean disabled;
    private static boolean modCompatibility = true;

    private FoodConsumptionClientState() {
    }

    public static boolean disabled() {
        return synchronizedFromServer ? disabled : ICECoreConfig.disableFoodConsumption();
    }

    public static boolean synchronizedFromServer() {
        return synchronizedFromServer;
    }

    public static void accept(boolean value) {
        disabled = value;
        synchronizedFromServer = true;
    }

    public static boolean modCompatibility() {
        return synchronizedFromServer ? modCompatibility : com.yinfires.icecore.config.ICECoreConfig.enableModCompatibility();
    }

    public static void accept(boolean value, boolean compatibility) {
        disabled = value;
        modCompatibility = compatibility;
        synchronizedFromServer = true;
    }

    public static void reset() {
        synchronizedFromServer = false;
        disabled = false;
        modCompatibility = true;
    }
}
