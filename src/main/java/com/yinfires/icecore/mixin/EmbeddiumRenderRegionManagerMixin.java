package com.yinfires.icecore.mixin;

import com.yinfires.icecore.building.BuildingClientRenderState;
import com.yinfires.icecore.building.render.EmbeddiumBuildOutputExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

/** Observes only Embeddium outputs that survived filtering and reached GPU upload. */
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager", remap = false)
public abstract class EmbeddiumRenderRegionManagerMixin {
    @Inject(method = "uploadMeshes(Lme/jellysquid/mods/sodium/client/gl/device/CommandList;Ljava/util/Collection;)V",
            at = @At("TAIL"), remap = false)
    private void icecore$observePreviewUploads(@Coerce Object commandList, Collection<?> outputs, CallbackInfo callback) {
        for (Object output : outputs) {
            if (!(output instanceof EmbeddiumBuildOutputExtension buildOutput)
                    || buildOutput.icecore$isIndexOnlyUpload()) continue;
            BuildingClientRenderState.onSectionUploaded(
                    buildOutput.icecore$getPreviewGeneration(),
                    buildOutput.icecore$getSectionX(), buildOutput.icecore$getSectionY(),
                    buildOutput.icecore$getSectionZ());
        }
    }
}
