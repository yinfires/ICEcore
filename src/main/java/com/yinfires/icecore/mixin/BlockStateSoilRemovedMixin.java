package com.yinfires.icecore.mixin;

import com.yinfires.icecore.farming.SeedBagCrops;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Whenever any FarmBlock becomes a non-farmland block, by whatever mod or code path, the seed-bag
 * plant on top vanishes without drops. LevelChunk only calls onRemove on the server, and the
 * common path costs a single instanceof per state change.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateSoilRemovedMixin {
    @Inject(method = "m_60753_(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V",
            at = @At("HEAD"))
    private void icecore$removeCropWithoutFarmland(Level level, BlockPos pos, BlockState newState, boolean moving,
                                                   CallbackInfo callback) {
        if (((BlockBehaviour.BlockStateBase) (Object) this).getBlock() instanceof FarmBlock
                && !(newState.getBlock() instanceof FarmBlock) && !level.isClientSide) {
            SeedBagCrops.onSoilLost(level, pos);
        }
    }
}
