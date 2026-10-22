package com.yinfires.icecore.compat.cozycafe;

import com.yinfires.icecore.mixing.ContainerContentAccess;
import net.minecraft.world.item.ItemStack;

public interface CafeTableAccess extends ContainerContentAccess {
    ItemStack icecore$tableRemainder();
    boolean icecore$tableDrink();
    boolean icecore$tablePlated();
    boolean icecore$hideMenu();
}
