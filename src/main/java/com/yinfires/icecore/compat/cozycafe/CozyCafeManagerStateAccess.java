package com.yinfires.icecore.compat.cozycafe;

import net.minecraft.core.BlockPos;

/** Stable bridge for optional CozyCafe manager state used by compatibility mixins. */
public interface CozyCafeManagerStateAccess {
    BlockPos icecore$getLinkedSign();
}
