package com.yinfires.icecore.mixin;

import com.yinfires.icecore.farming.SeedBagCrops;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets seed-bag crops whose upstream soil rule excludes farmland (red bean, tea) survive on any
 * FarmBlock. Subclass light checks still run before this super call.
 */
@Mixin(BushBlock.class)
public abstract class BushBlockSeedBagFarmlandMixin {
    @Inject(method = "m_7898_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z",
            at = @At("HEAD"), cancellable = true)
    private void icecore$seedBagFarmland(BlockState state, LevelReader level, BlockPos pos,
                                         CallbackInfoReturnable<Boolean> callback) {
        if (SeedBagCrops.survivesOnFarmland((Block) (Object) this, state, level, pos)) callback.setReturnValue(true);
    }
}
