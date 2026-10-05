package com.yinfires.icecore.mixin;

import com.yinfires.icecore.farming.ModFarming;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Vanilla farmland decay (dry-out, trampling, lost support) leaves untilled dirt instead of dirt. */
@Mixin(FarmBlock.class)
public abstract class FarmBlockUntilledDirtMixin {
    @ModifyArg(method = "m_269406_(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FarmBlock;m_49897_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"),
            index = 1)
    private static BlockState icecore$untilledDirt(BlockState target) {
        return target.is(Blocks.DIRT) ? ModFarming.UNTILLED_DIRT.get().defaultBlockState() : target;
    }
}
