package com.yinfires.icecore.mixin;

import com.yinfires.icecore.building.BuildingClientRenderState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hides preview coordinates from Embeddium's asynchronous section mesh builder. */
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.world.WorldSlice", remap = false)
public abstract class EmbeddiumWorldSliceMixin {
    @Unique
    private BuildingClientRenderState.MeshSnapshot icecore$previewMeshSnapshot;

    @Inject(method = "copyData", at = @At("HEAD"), remap = false)
    private void icecore$capturePreviewSnapshot(CallbackInfo callback) {
        icecore$previewMeshSnapshot = BuildingClientRenderState.captureMeshSnapshot();
        BuildingClientRenderState.installWorkerSnapshot(icecore$previewMeshSnapshot);
    }

    @Inject(
            method = "getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void icecore$hidePreviewBlock(int x, int y, int z,
                                           CallbackInfoReturnable<BlockState> callback) {
        BuildingClientRenderState.MeshSnapshot snapshot = icecore$previewMeshSnapshot;
        if (snapshot != null && snapshot.contains(x, y, z)) {
            callback.setReturnValue(Blocks.AIR.defaultBlockState());
        }
    }
}
