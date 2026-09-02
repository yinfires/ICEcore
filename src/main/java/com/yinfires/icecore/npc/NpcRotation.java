package com.yinfires.icecore.npc;

import net.minecraft.util.Mth;

public final class NpcRotation {
    private NpcRotation() {}
    public static float snapToEightDirections(float yaw) {
        return Mth.wrapDegrees(Math.round(Mth.wrapDegrees(yaw) / 45.0F) * 45.0F);
    }
}
