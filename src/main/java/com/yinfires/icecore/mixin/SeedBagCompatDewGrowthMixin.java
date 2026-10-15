package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.dewdrop.SeedBagDailyGrowth;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adapt seed-bag structures after Dew's daily soil and season checks.
 */
@Pseudo
@Mixin(targets = "cool.bot.dewdropfarmland.utils.CropHandlerUtils", remap = false)
public abstract class SeedBagCompatDewGrowthMixin {
    @Inject(method = "handleCropGrowth(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void icecore$growDoubleCrop(BlockState crop, ServerLevel level, BlockPos pos, BlockState soil,
                                               RandomSource random, CallbackInfo callback) {
        if (SeedBagDailyGrowth.grow(crop, level, pos, soil, random)) callback.cancel();
    }
}
