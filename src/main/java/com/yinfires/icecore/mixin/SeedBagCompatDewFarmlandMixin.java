package com.yinfires.icecore.mixin;

import com.yinfires.icecore.farming.ModFarming;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Dew Drop 9.0 daily decay sets dried farmland straight to dirt; leave untilled dirt instead. */
@Pseudo
@Mixin(targets = "cool.bot.dewdropfarmland.block.CustomFarmland", remap = false)
public abstract class SeedBagCompatDewFarmlandMixin {
    @ModifyArg(method = "m_213897_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;m_7731_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
            index = 1, remap = false)
    private BlockState icecore$untilledDirt(BlockState target) {
        return target.is(Blocks.DIRT) ? ModFarming.UNTILLED_DIRT.get().defaultBlockState() : target;
    }
}
