package com.yinfires.icecore.workstation;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import java.util.List;

public interface CuttingBoardAccess {
    List<ItemStack> icecore$products();
    WorkstationSlotState icecore$containers();
    ItemStackHandler icecore$inventory();
    boolean icecore$carved();
    void icecore$uncarve();
}
