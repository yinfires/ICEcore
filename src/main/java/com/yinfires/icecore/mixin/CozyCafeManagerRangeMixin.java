package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.cozycafe.range.CozyCafeRangeDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Overrides CozyCafe's reputation-derived scan corners for configured computers. */
@Pseudo
@Mixin(targets = "io.github.chakyl.cozycafe.blockentities.CafeManagerBlockEntity", remap = false)
public abstract class CozyCafeManagerRangeMixin {
    @Inject(method = "getFirstPos(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void icecore$customFirstPosition(BlockState state, BlockPos computer,
                                              CallbackInfoReturnable<BlockPos> callback) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        BlockPos custom = CozyCafeRangeDataManager.get().first(level, computer);
        if (custom != null) {
            callback.setReturnValue(custom);
        }
    }

    @Inject(method = "getSecondPos(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void icecore$customSecondPosition(BlockState state, BlockPos computer,
                                               CallbackInfoReturnable<BlockPos> callback) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        BlockPos custom = CozyCafeRangeDataManager.get().second(level, computer);
        if (custom != null) {
            callback.setReturnValue(custom);
        }
    }
}
