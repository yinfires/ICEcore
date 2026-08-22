package com.yinfires.icecore.mixin;

import com.yinfires.icecore.building.BuildingClientRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hides preview coordinates while asynchronous chunk meshes are rebuilt. */
@Mixin(RenderChunkRegion.class)
public abstract class RenderChunkRegionMixin {
    @Inject(method = "m_8055_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("HEAD"), cancellable = true)
    private void icecore$hidePreviewBlock(BlockPos position, CallbackInfoReturnable<BlockState> callback) {
        if (BuildingClientRenderState.isHidden(position)) {
            callback.setReturnValue(BuildingClientRenderState.hiddenState());
        }
    }

    @Inject(method = "m_6425_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;", at = @At("HEAD"), cancellable = true)
    private void icecore$hidePreviewFluid(BlockPos position, CallbackInfoReturnable<FluidState> callback) {
        if (BuildingClientRenderState.isHidden(position)) {
            callback.setReturnValue(Fluids.EMPTY.defaultFluidState());
        }
    }

    @Inject(method = "m_7702_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;", at = @At("HEAD"), cancellable = true)
    private void icecore$hidePreviewBlockEntity(BlockPos position, CallbackInfoReturnable<BlockEntity> callback) {
        if (BuildingClientRenderState.isHidden(position)) {
            callback.setReturnValue(null);
        }
    }
}
