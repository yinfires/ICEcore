package com.yinfires.icecore.mixin;

import com.yinfires.icecore.building.BuildingClientRenderState;
import com.yinfires.icecore.building.render.EmbeddiumBuildOutputExtension;
import com.yinfires.icecore.building.render.EmbeddiumRenderSectionExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildOutput", remap = false)
public abstract class EmbeddiumChunkBuildOutputMixin implements EmbeddiumBuildOutputExtension {
    @org.spongepowered.asm.mixin.Shadow(remap = false)
    public abstract boolean isIndexOnlyUpload();

    @Unique
    public long icecore$previewGeneration = BuildingClientRenderState.workerGeneration();
    @Unique private int icecore$sectionX;
    @Unique private int icecore$sectionY;
    @Unique private int icecore$sectionZ;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void icecore$captureSection(@Coerce Object render, @Coerce Object info,
                                        java.util.Map<?, ?> meshes, int buildTime,
                                        CallbackInfo callback) {
        if (render instanceof EmbeddiumRenderSectionExtension section) {
            icecore$sectionX = section.icecore$getSectionX();
            icecore$sectionY = section.icecore$getSectionY();
            icecore$sectionZ = section.icecore$getSectionZ();
        }
    }

    @Override
    public long icecore$getPreviewGeneration() {
        return icecore$previewGeneration;
    }

    @Override
    public boolean icecore$isIndexOnlyUpload() {
        return isIndexOnlyUpload();
    }

    @Override public int icecore$getSectionX() { return icecore$sectionX; }
    @Override public int icecore$getSectionY() { return icecore$sectionY; }
    @Override public int icecore$getSectionZ() { return icecore$sectionZ; }
}
