package com.yinfires.icecore.compat.cozycafe.seating;

import net.minecraft.core.BlockPos;

import java.util.UUID;

/** Persistent ICEcore state attached to a CozyCafe menu block entity. */
public interface CozyCafeMenuSeatAccess {
    UUID icecore$getCustomerUuid();
    void icecore$setCustomerUuid(UUID uuid);
    BlockPos icecore$getSeatPos();
    void icecore$setSeatPos(BlockPos pos);
    BlockPos icecore$getReceptionPos();
    void icecore$setReceptionPos(BlockPos pos);
    String icecore$getSeatingStage();
    void icecore$setSeatingStage(String stage);
}
