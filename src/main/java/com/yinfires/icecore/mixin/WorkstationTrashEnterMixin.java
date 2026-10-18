package com.yinfires.icecore.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep normal landing damage, then stop the native seat/hiding branch. */
@org.spongepowered.asm.mixin.Pseudo
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.block.misc.TrashCanBlock", remap = false)
public abstract class WorkstationTrashEnterMixin {
    @Inject(method = "m_142072_(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/HorizontalDirectionalBlock;m_142072_(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;F)V", shift = At.Shift.AFTER), cancellable = true)
    private void icecore$disableEntry(Level level, BlockState state, BlockPos pos, Entity entity, float distance, CallbackInfo ci) {
        if (entity instanceof Player) ci.cancel();
    }
}
