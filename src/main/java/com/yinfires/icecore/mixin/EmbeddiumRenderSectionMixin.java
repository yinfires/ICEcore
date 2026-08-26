package com.yinfires.icecore.mixin;

import com.yinfires.icecore.building.render.EmbeddiumRenderSectionExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.RenderSection", remap = false)
public abstract class EmbeddiumRenderSectionMixin implements EmbeddiumRenderSectionExtension {
    @Shadow(remap = false) public abstract int getChunkX();
    @Shadow(remap = false) public abstract int getChunkY();
    @Shadow(remap = false) public abstract int getChunkZ();

    @Override public int icecore$getSectionX() { return getChunkX(); }
    @Override public int icecore$getSectionY() { return getChunkY(); }
    @Override public int icecore$getSectionZ() { return getChunkZ(); }
}
