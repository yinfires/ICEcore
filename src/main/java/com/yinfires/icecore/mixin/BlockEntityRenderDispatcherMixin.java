package com.yinfires.icecore.mixin;

import com.yinfires.icecore.building.BuildingClientRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents a hidden container/block entity from being drawn over its ghost. */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    // Forge's production client uses SRG names and this project does not apply
    // MixinGradle, so keep the stable 1.20.1 method name explicit here.
    @Inject(method = "m_112267_(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity> void icecore$hidePreviewBlockEntity(
            E blockEntity, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, CallbackInfo callback) {
        BlockPos position = blockEntity.getBlockPos();
        if (BuildingClientRenderState.isHidden(position)) {
            callback.cancel();
        }
    }
}
