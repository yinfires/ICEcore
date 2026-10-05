package com.yinfires.icecore.mixin;

import com.yinfires.icecore.farming.SeedBagCrops;
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
 * Dew Drop's daily CropBlock growth writes getStateForAge directly, which never creates the upper
 * half of two-block seed-bag crops (tea). Route those through the crop's own bonemeal growth.
 */
@Pseudo
@Mixin(targets = "cool.bot.dewdropfarmland.utils.CropHandlerUtils", remap = false)
public abstract class SeedBagCompatDewGrowthMixin {
    @Inject(method = "handleCropGrowth(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void icecore$growDoubleCrop(BlockState crop, ServerLevel level, BlockPos pos, BlockState soil,
                                               RandomSource random, CallbackInfo callback) {
        if (SeedBagCrops.growDoubleCrop(level, pos, crop, random)) callback.cancel();
    }
}
