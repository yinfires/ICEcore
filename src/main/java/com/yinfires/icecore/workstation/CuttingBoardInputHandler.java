package com.yinfires.icecore.workstation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;

/** A package-external capability implementation, safe to instantiate from a transformed mixin. */
public final class CuttingBoardInputHandler extends ItemStackHandler {
    private final BlockEntity entity;
    private boolean loading;
    public CuttingBoardInputHandler(BlockEntity entity) { super(1); this.entity = entity; }
    @Override public int getSlotLimit(int slot) { return 1; }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        CuttingBoardAccess board = (CuttingBoardAccess)entity;
        if (board.icecore$carved() || !board.icecore$products().isEmpty() || !getStackInSlot(slot).isEmpty()) return stack;
        return super.insertItem(slot, stack, simulate);
    }
    @Override public void deserializeNBT(CompoundTag tag) {
        loading = true;
        try { super.deserializeNBT(tag); } finally { loading = false; }
    }
    @Override protected void onContentsChanged(int slot) { if (!loading) ExternalWorkstation.changed(entity); }
}
