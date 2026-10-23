package com.yinfires.icecore.compat.cozycafe.customer;

import net.minecraft.nbt.CompoundTag;

/** Persisted visit/order state belongs to the native menu, not a transient Java entity lookup. */
public interface CafeCustomerVisit {
    CompoundTag icecore$visit();
    void icecore$visit(CompoundTag tag);
}
