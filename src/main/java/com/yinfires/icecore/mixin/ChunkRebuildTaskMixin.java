package com.yinfires.icecore.mixin;

import com.yinfires.icecore.building.BuildingClientRenderState;
import com.yinfires.icecore.building.render.PreviewMeshSnapshotHolder;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.List;

@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk$RebuildTask")
public abstract class ChunkRebuildTaskMixin {
    @Shadow(aliases = "region")
    protected RenderChunkRegion f_112858_;

    @Shadow(aliases = "this$1")
    @Final
    private ChunkRenderDispatcher.RenderChunk f_112859_;

    @Unique
    private BuildingClientRenderState.MeshSnapshot icecore$previewMeshSnapshot;

    @Inject(method = "m_5869_(Lnet/minecraft/client/renderer/ChunkBufferBuilderPack;)Ljava/util/concurrent/CompletableFuture;", at = @At("HEAD"))
    private void icecore$installPreviewSnapshot(ChunkBufferBuilderPack buffers,
                                                CallbackInfoReturnable<CompletableFuture<?>> callback) {
        if (f_112858_ instanceof PreviewMeshSnapshotHolder holder) {
            icecore$previewMeshSnapshot = holder.icecore$getPreviewMeshSnapshot();
            BuildingClientRenderState.installWorkerSnapshot(icecore$previewMeshSnapshot);
        }
    }

    @Inject(
            method = "m_234472_(Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$CompiledChunk;Ljava/util/List;Ljava/lang/Throwable;)Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$ChunkTaskResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;m_194352_(Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk;)V",
                    shift = At.Shift.AFTER))
    private void icecore$observePreviewUpload(ChunkRenderDispatcher.CompiledChunk compiledChunk,
                                              List<?> uploads, Throwable failure,
                                              CallbackInfoReturnable<?> callback) {
        if (icecore$previewMeshSnapshot == null) return;
        BlockPos origin = f_112859_.getOrigin();
        BuildingClientRenderState.onSectionUploaded(
                icecore$previewMeshSnapshot.generation(),
                SectionPos.blockToSectionCoord(origin.getX()),
                SectionPos.blockToSectionCoord(origin.getY()),
                SectionPos.blockToSectionCoord(origin.getZ()));
    }
}
