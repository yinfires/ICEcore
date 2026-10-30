package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.bakery.client.StoveSurfaceLighting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", remap = false)
public abstract class StoveSurfaceEmbeddiumModeMixin {
    @Redirect(method = "getLightingMode(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/client/renderer/RenderType;)Lme/jellysquid/mods/sodium/client/model/light/LightMode;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)I"))
    private int icecore$materialEmission(BlockState state, BlockGetter world, BlockPos pos) {
        return StoveSurfaceLighting.emission(state, world, pos);
    }
}
