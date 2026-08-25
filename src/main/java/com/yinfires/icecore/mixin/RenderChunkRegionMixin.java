package com.yinfires.icecore.mixin;

import com.yinfires.icecore.building.BuildingClientRenderState;
import com.yinfires.icecore.building.render.PreviewMeshSnapshotHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hides preview coordinates while asynchronous chunk meshes are rebuilt. */
@Mixin(RenderChunkRegion.class)
public abstract class RenderChunkRegionMixin implements PreviewMeshSnapshotHolder {
    @Unique
    private BuildingClientRenderState.MeshSnapshot icecore$previewMeshSnapshot;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void icecore$capturePreviewSnapshot(CallbackInfo callback) {
        icecore$previewMeshSnapshot = BuildingClientRenderState.captureMeshSnapshot();
    }

    @Override
    public BuildingClientRenderState.MeshSnapshot icecore$getPreviewMeshSnapshot() {
        return icecore$previewMeshSnapshot;
    }

    @Inject(method = "m_8055_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("HEAD"), cancellable = true)
    private void icecore$hidePreviewBlock(BlockPos position, CallbackInfoReturnable<BlockState> callback) {
        if (icecore$previewMeshSnapshot.contains(position)) {
            callback.setReturnValue(BuildingClientRenderState.hiddenState());
        }
    }

    @Inject(method = "m_6425_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;", at = @At("HEAD"), cancellable = true)
    private void icecore$hidePreviewFluid(BlockPos position, CallbackInfoReturnable<FluidState> callback) {
        if (icecore$previewMeshSnapshot.contains(position)) {
            callback.setReturnValue(Fluids.EMPTY.defaultFluidState());
        }
    }

    @Inject(method = "m_7702_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;", at = @At("HEAD"), cancellable = true)
    private void icecore$hidePreviewBlockEntity(BlockPos position, CallbackInfoReturnable<BlockEntity> callback) {
        if (icecore$previewMeshSnapshot.contains(position)) {
            callback.setReturnValue(null);
        }
    }
}
