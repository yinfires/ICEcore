package com.yinfires.icecore.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.misc.TrashCanBlockEntity;
import com.yinfires.icecore.workstation.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@org.spongepowered.asm.mixin.Pseudo
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.blockentity.misc.TrashCanBlockEntity", remap = false)
public abstract class WorkstationTrashCanMixin {
    @Unique private ItemStack icecore$remainder;
    @Unique private ItemStack[] icecore$containers;
    @Unique private boolean icecore$shifted;
    @Unique private ItemStack icecore$soundItem = ItemStack.EMPTY;

    @Inject(method = "putItem(Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    private void icecore$beforePut(ItemStack input, CallbackInfo ci) {
        TrashCanBlockEntity self = (TrashCanBlockEntity) (Object) this;
        var storage = self.getStorage();
        var state = ((WorkstationAccess) self).icecore$slots();
        state.reconcile(storage.getSlots(), storage::getStackInSlot);
        icecore$containers = new ItemStack[storage.getSlots()];
        for (int i = 0; i < storage.getSlots(); i++) icecore$containers[i] = state.container(i).copy();
        icecore$shifted = false;
        icecore$soundItem = input.copyWithCount(1);
    }

    @Redirect(method = "putItem(Lnet/minecraft/world/item/ItemStack;)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/items/ItemStackHandler;setStackInSlot(ILnet/minecraft/world/item/ItemStack;)V"))
    private void icecore$move(ItemStackHandler storage, int slot, ItemStack stack) {
        icecore$shifted = true;
        storage.setStackInSlot(slot, stack);
    }

    @Inject(method = "putItem(Lnet/minecraft/world/item/ItemStack;)V", at = @At(value = "INVOKE",
            target = "Lcom/github/ysbbbbbb/kaleidoscopecookery/blockentity/misc/TrashCanBlockEntity;refresh()V"))
    private void icecore$afterPut(ItemStack input, CallbackInfo ci) {
        TrashCanBlockEntity self = (TrashCanBlockEntity) (Object) this;
        var storage = self.getStorage();
        var state = ((WorkstationAccess) self).icecore$slots();
        state.reconcile(storage.getSlots(), storage::getStackInSlot);
        if (icecore$shifted) for (int i = 0; i < storage.getSlots() - 1; i++)
            state.inserted(i, storage.getStackInSlot(i), icecore$containers[i + 1]);
        if (icecore$shifted) {
            int last = storage.getSlots() - 1;
            state.inserted(last, storage.getStackInSlot(last), WorkstationContainerInteraction.requiredEmptyContainer(
                    storage.getStackInSlot(last), WorkstationContainerInteraction.FluidContainerMode.STORED_AS_ITEM));
        }
        icecore$containers = null;
    }

    @Inject(method = "withdrawItem(Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"), cancellable = true)
    private void icecore$receive(LivingEntity user, CallbackInfo ci) {
        icecore$remainder = null;
        if (!(user instanceof ServerPlayer player)) { ci.cancel(); return; }
        TrashCanBlockEntity self = (TrashCanBlockEntity) (Object) this;
        int last = TrashCanInteraction.last(self);
        icecore$soundItem = last < 0 ? ItemStack.EMPTY : self.getStorage().getStackInSlot(last).copyWithCount(1);
        icecore$remainder = TrashCanInteraction.receive((TrashCanBlockEntity) (Object) this, player);
        if (icecore$remainder == null) ci.cancel();
    }

    @Redirect(method = "withdrawItem(Lnet/minecraft/world/entity/LivingEntity;)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;m_21008_(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V"))
    private void icecore$alreadyReceived(LivingEntity user, InteractionHand hand, ItemStack stack) { }

    @Redirect(method = "withdrawItem(Lnet/minecraft/world/entity/LivingEntity;)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/items/ItemStackHandler;setStackInSlot(ILnet/minecraft/world/item/ItemStack;)V"))
    private void icecore$keepRemainder(ItemStackHandler storage, int slot, ItemStack ignored) {
        storage.setStackInSlot(slot, icecore$remainder);
        ((WorkstationAccess) this).icecore$slots().reconcile(storage.getSlots(), storage::getStackInSlot);
        icecore$remainder = null;
    }

    @Redirect(method = {"putItem(Lnet/minecraft/world/item/ItemStack;)V", "withdrawItem(Lnet/minecraft/world/entity/LivingEntity;)V"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;m_5594_(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
    private void icecore$sound(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.player.Player player,
            net.minecraft.core.BlockPos pos, net.minecraft.sounds.SoundEvent original, net.minecraft.sounds.SoundSource source,
            float volume, float pitch) {
        boolean taking = original == net.minecraft.sounds.SoundEvents.BARREL_CLOSE;
        var entry = WorkstationContainerCompat.describe(icecore$soundItem);
        boolean fluid = entry.isPresent() && entry.get().behavior() == WorkstationContainerCompat.Behavior.FLUID;
        var sound = fluid ? (taking ? WorkstationContainerCompat.fillSound(entry.get().category())
                : WorkstationContainerCompat.emptySound(entry.get().category()))
                : taking ? net.minecraft.sounds.SoundEvents.ITEM_FRAME_REMOVE_ITEM : net.minecraft.sounds.SoundEvents.ITEM_FRAME_ADD_ITEM;
        level.playSound(player, pos, sound, source, fluid ? 1F : .8F, fluid ? 1F : 1.1F);
        icecore$soundItem = ItemStack.EMPTY;
    }
}
