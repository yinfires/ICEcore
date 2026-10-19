package com.yinfires.icecore.mixin;

import com.yinfires.icecore.spoilage.ShelfLife;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = ItemStackHandler.class, remap = false)
public abstract class ShelfLifeItemHandlerMixin implements com.yinfires.icecore.spoilage.DiscardingStorage {
    @Unique private boolean icecore$discarding;
    @Override public void icecore$discardExpired() { icecore$discarding = true; }
    @Inject(method = "setStackInSlot", at = @At("HEAD"))
    private void icecore$set(int slot, ItemStack stack, CallbackInfo ci) { ShelfLife.release(stack); }
    @Inject(method = "insertItem", at = @At("HEAD"))
    private void icecore$insert(int slot, ItemStack stack, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        if (!simulate) ShelfLife.release(stack);
    }
    @Inject(method = "getStackInSlot", at = @At("RETURN"), cancellable = true)
    private void icecore$get(int slot, CallbackInfoReturnable<ItemStack> cir) {
        ShelfLife.release(cir.getReturnValue());
        if (icecore$discarding && ShelfLife.expired(cir.getReturnValue())) {
            ((ItemStackHandler)(Object)this).setStackInSlot(slot, ItemStack.EMPTY);
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
    @Inject(method = "extractItem", at = @At("HEAD"), cancellable = true)
    private void icecore$extract(int slot, int amount, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        if (icecore$discarding && ((ItemStackHandler)(Object)this).getStackInSlot(slot).isEmpty()) cir.setReturnValue(ItemStack.EMPTY);
    }
}
