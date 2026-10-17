package com.yinfires.icecore.compat.cozycafe;

import net.minecraft.world.level.block.entity.BlockEntity;

public interface CozyCafePendingAdmission {
    void icecore$captureAdmission(BlockEntity manager, long generation, long time);
    long icecore$capturedTime();
    BlockEntity icecore$admissionManager();
    long icecore$capturedGeneration();
}
