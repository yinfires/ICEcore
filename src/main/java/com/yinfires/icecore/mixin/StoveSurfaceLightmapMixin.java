package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.bakery.client.StoveSurfaceLighting;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Preserve sampled sky/block light, remove only the material's emission minimum. */
@Mixin(LevelRenderer.class)
public abstract class StoveSurfaceLightmapMixin {
    @Redirect(method = "m_109537_(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)I", remap = false), remap = false)
    private static int icecore$materialEmission(BlockState state, BlockGetter world, BlockPos pos) {
        return StoveSurfaceLighting.emission(state, world, pos);
    }
}
