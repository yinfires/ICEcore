package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.bakery.client.StoveSurfaceLighting;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Keep lit stove geometry in the same AO pipeline as ordinary kitchen masonry. */
@Mixin(ModelBlockRenderer.class)
public abstract class StoveSurfaceAoMixin {
    @Redirect(method = "tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)I", remap = false), remap = false)
    private int icecore$materialEmission(BlockState state, BlockGetter world, BlockPos pos) {
        return StoveSurfaceLighting.emission(state, world, pos);
    }
}
