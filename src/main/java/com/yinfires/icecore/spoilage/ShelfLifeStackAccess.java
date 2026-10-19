package com.yinfires.icecore.spoilage;

import net.minecraft.world.item.ItemStack;
import java.util.function.BooleanSupplier;

public interface ShelfLifeStackAccess {
    void icecore$replace(ItemStack replacement);
    BooleanSupplier icecore$owner();
    void icecore$owner(BooleanSupplier owner);
    boolean icecore$needsSettlement(long day, boolean paused, int revision);
    void icecore$detach();
    boolean icecore$detached();
}
