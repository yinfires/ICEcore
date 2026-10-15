package com.yinfires.icecore.mixin;

import com.yinfires.icecore.farming.SeedBagCrops;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Covers vanilla sampling and ICEcore time compensation through the same dispatch entry. */
@Mixin(value = BlockBehaviour.BlockStateBase.class, remap = false)
public abstract class SeedBagCompatDewRandomTickMixin {
    @Inject(method = "m_222972_(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void icecore$dailyOnly(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo callback) {
        if (SeedBagCrops.find(((BlockBehaviour.BlockStateBase) (Object) this).getBlock()) != null) callback.cancel();
    }
}
