package com.yinfires.icecore.mixin;

import com.yinfires.icecore.client.cutscene.CutsceneCamera;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Jade renders directly from RenderGuiEvent.Post instead of a cancellable Forge GUI overlay. */
@Pseudo
@Mixin(targets = "snownee.jade.overlay.OverlayRenderer", remap = false)
public abstract class JadeOverlayRendererMixin {
    @Inject(method = "renderOverlay478757", at = @At("HEAD"), cancellable = true, remap = false)
    private static void icecore$hideDuringTimeCamera(GuiGraphics graphics, CallbackInfo callback) {
        if (CutsceneCamera.cameraActive()) callback.cancel();
    }
}
