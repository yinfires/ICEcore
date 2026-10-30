package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.bakery.client.StoveSurfaceLighting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(targets = "net.minecraft.client.renderer.block.ModelBlockRenderer$Cache", remap = false)
public abstract class StoveSurfaceShadeMixin {
    @Redirect(method = "m_111226_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)F",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;m_60792_(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"))
    private float icecore$materialShade(BlockState state, BlockGetter world, BlockPos pos) {
        return StoveSurfaceLighting.shade(state, world, pos);
    }
}
