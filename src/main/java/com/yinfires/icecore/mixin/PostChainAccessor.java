package com.yinfires.icecore.mixin;

import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Exposes a loaded post-processing chain's passes so a cutscene can push its own
 * {@code Strength} uniform into the effect each frame. {@link PostChain} keeps the
 * pass list private and this project applies no MixinGradle refmap, so the field is
 * referenced by its stable 1.20.1 SRG name ({@code passes}).
 */
@Mixin(PostChain.class)
public interface PostChainAccessor {
    @Accessor("f_110009_")
    List<PostPass> icecore$passes();
}
