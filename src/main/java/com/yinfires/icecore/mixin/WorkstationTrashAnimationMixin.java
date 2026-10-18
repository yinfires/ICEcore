package com.yinfires.icecore.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.misc.TrashCanBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Synchronize successful native animation starts through the existing refresh/update-tag path. */
@org.spongepowered.asm.mixin.Pseudo
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.blockentity.misc.TrashCanBlockEntity", remap = false)
public abstract class WorkstationTrashAnimationMixin implements com.yinfires.icecore.workstation.TrashCanAnimationAccess {
    @Unique private long icecore$animationSequence;
    @Unique private long icecore$animationTick;
    @Unique private boolean icecore$taking;
    @Unique private long icecore$animationDeadline;

    @Inject(method = "putItem(Lnet/minecraft/world/item/ItemStack;)V", at = @At(value = "INVOKE",
            target = "Lcom/github/ysbbbbbb/kaleidoscopecookery/blockentity/misc/TrashCanBlockEntity;refresh()V"))
    private void icecore$putAnimation(ItemStack stack, CallbackInfo ci) { icecore$record(false); }

    @Inject(method = "withdrawItem(Lnet/minecraft/world/entity/LivingEntity;)V", at = @At(value = "INVOKE",
            target = "Lcom/github/ysbbbbbb/kaleidoscopecookery/blockentity/misc/TrashCanBlockEntity;refresh()V"))
    private void icecore$takeAnimation(LivingEntity user, CallbackInfo ci) { icecore$record(true); }

    @Unique private void icecore$record(boolean taking) {
        var self = (TrashCanBlockEntity) (Object) this;
        if (self.getLevel() == null || self.getLevel().isClientSide) return;
        icecore$animationSequence++;
        icecore$animationTick = self.getLevel().getGameTime();
        icecore$taking = taking;
    }

    @Inject(method = "m_183515_(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void icecore$saveAnimation(CompoundTag tag, CallbackInfo ci) {
        tag.putLong("ICEcoreTrashAnimationSequence", icecore$animationSequence);
        tag.putLong("ICEcoreTrashAnimationTick", icecore$animationTick);
        tag.putBoolean("ICEcoreTrashAnimationTaking", icecore$taking);
    }

    @Inject(method = "m_142466_(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void icecore$loadAnimation(CompoundTag tag, CallbackInfo ci) {
        long sequence = tag.getLong("ICEcoreTrashAnimationSequence");
        long tick = tag.getLong("ICEcoreTrashAnimationTick");
        boolean taking = tag.getBoolean("ICEcoreTrashAnimationTaking");
        var self = (TrashCanBlockEntity) (Object) this;
        var level = self.getLevel();
        if (level != null && level.isClientSide && sequence > icecore$animationSequence) {
            self.putState.stop(); self.withdrawState.stop();
            int remaining = com.yinfires.icecore.workstation.TrashCanAnimationTiming.remainingTicks(
                    icecore$animationSequence, sequence, tick, level.getGameTime(), taking);
            icecore$animationDeadline = 0;
            if (remaining > 0) {
                (taking ? self.withdrawState : self.putState).start((int) tick);
                icecore$animationDeadline = System.nanoTime() + remaining * 50_000_000L;
            }
        }
        if (level == null || !level.isClientSide || sequence >= icecore$animationSequence) {
            icecore$animationSequence = sequence;
            icecore$animationTick = tick;
            icecore$taking = taking;
        }
    }

    @Inject(method = "clientTick(Lnet/minecraft/world/level/Level;)V", at = @At("HEAD"))
    private void icecore$expireAnimation(Level level, CallbackInfo ci) {
        icecore$expireTrashAnimation();
    }

    public void icecore$expireTrashAnimation() {
        var self = (TrashCanBlockEntity) (Object) this;
        var level = self.getLevel();
        if (icecore$animationDeadline != 0 && (System.nanoTime() >= icecore$animationDeadline
                || level == null || level.getGameTime() - icecore$animationTick >= (icecore$taking ? 10 : 12))) {
            self.putState.stop(); self.withdrawState.stop();
            icecore$animationDeadline = 0;
        }
    }
}
