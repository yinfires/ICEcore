package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.bakery.client.StoveSurfaceLighting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Embeddium otherwise also clamps flat lightmaps to the source's packed luminance. */
@Mixin(targets = "me.jellysquid.mods.sodium.client.model.light.data.LightDataAccess", remap = false)
public abstract class StoveSurfaceEmbeddiumDataMixin {
    @Redirect(method = "compute(III)I", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)I"))
    private int icecore$materialEmission(BlockState state, BlockGetter world, BlockPos pos) {
        return StoveSurfaceLighting.emission(state, world, pos);
    }

    @Redirect(method = "compute(III)I", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;m_60792_(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"))
    private float icecore$materialShade(BlockState state, BlockGetter world, BlockPos pos) {
        return StoveSurfaceLighting.shade(state, world, pos);
    }
}
