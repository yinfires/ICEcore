package com.yinfires.icecore.journal.client;

import net.minecraft.network.chat.Component;

/**
 * One quest objective prepared for rendering: its description label plus current/target counts.
 * The label carries no count; the UI draws {@code current/target} and a progress bar separately,
 * keeping description and progress visually distinct (per design).
 */
public record ObjectiveView(Component label, int current, int target) {

    public boolean complete() {
        return current >= target;
    }

    public float fraction() {
        return target <= 0 ? 1.0F : Math.min(1.0F, current / (float) target);
    }
}
