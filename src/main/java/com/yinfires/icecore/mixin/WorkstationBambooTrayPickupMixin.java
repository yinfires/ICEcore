package com.yinfires.icecore.mixin;

import com.yinfires.icecore.workstation.WorkstationInventoryTransfer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.BambooTrayBlockEntity", remap=false)
public abstract class WorkstationBambooTrayPickupMixin {
    @Shadow public abstract net.minecraft.core.NonNullList<ItemStack> getItems();
    @Shadow private void resetProcessing(int slot) { throw new AssertionError(); }
    @Inject(method="onTakeOut(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;IZ)Z", at=@At("HEAD"), cancellable=true)
    private void icecore$inventory(Level level, LivingEntity user, int slot, boolean all, CallbackInfoReturnable<Boolean> cir) {
        if (!(user instanceof Player player)) return;
        cir.setReturnValue(false);
        if (slot < 0 || slot >= getItems().size()) return;
        ItemStack stored = getItems().get(slot);
        if (stored.isEmpty()) return;
        if (level.isClientSide) { cir.setReturnValue(true); return; }
        int requested = all ? stored.getCount() : 1;
        ItemStack offered = stored.copyWithCount(requested);
        int capacity = requested - WorkstationInventoryTransfer.insert(player, offered, true).getCount();
        if (capacity == 0) return;
        int moved = capacity - WorkstationInventoryTransfer.insert(player, offered.copyWithCount(capacity), false).getCount();
        if (moved == 0) return;
        stored.shrink(moved);
        if (stored.isEmpty()) { getItems().set(slot, ItemStack.EMPTY); resetProcessing(slot); }
        ((com.github.ysbbbbbb.kaleidoscopecookery.blockentity.BaseBlockEntity)(Object)this).refresh();
        user.playSound(net.minecraft.sounds.SoundEvents.ITEM_PICKUP, 1, 1);
        cir.setReturnValue(true);
    }
}
