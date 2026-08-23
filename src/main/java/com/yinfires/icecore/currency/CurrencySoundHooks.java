package com.yinfires.icecore.currency;

/** Stable extension point for future positive/negative balance sounds. */
public final class CurrencySoundHooks {
    private CurrencySoundHooks() {
    }

    public static void onBalanceChanged(long delta) {
        // Intentionally silent until dedicated gain/loss audio assets are selected.
    }
}
