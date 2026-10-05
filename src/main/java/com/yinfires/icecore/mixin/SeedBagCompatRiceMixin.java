package com.yinfires.icecore.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Farmer's Delight rice is a seed-bag crop grown on farmland: no water source, no built-in water. */
@Pseudo
@Mixin(targets = "vectorwing.farmersdelight.common.block.RiceBlock", remap = false)
public abstract class SeedBagCompatRiceMixin {
    @Inject(method = "m_7898_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void icecore$surviveOnFarmland(BlockState state, LevelReader level, BlockPos pos,
                                           CallbackInfoReturnable<Boolean> callback) {
        if (level.getBlockState(pos.below()).getBlock() instanceof FarmBlock) callback.setReturnValue(true);
    }

    @Inject(method = "m_5888_(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/material/FluidState;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void icecore$noWater(BlockState state, CallbackInfoReturnable<FluidState> callback) {
        callback.setReturnValue(Fluids.EMPTY.defaultFluidState());
    }
}
