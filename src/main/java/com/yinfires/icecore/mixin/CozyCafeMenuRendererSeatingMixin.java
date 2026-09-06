package com.yinfires.icecore.mixin;

import io.github.chakyl.cozycafe.blockentities.CafeMenuBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "io.github.chakyl.cozycafe.blockentities.renderer.CafeMenuBlockEntityRenderer", remap = false)
public abstract class CozyCafeMenuRendererSeatingMixin {
    @Redirect(method = "render(Lio/github/chakyl/cozycafe/blockentities/CafeMenuBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "INVOKE", target = "Lio/github/chakyl/cozycafe/blockentities/CafeMenuBlockEntity;getHasCustomer()Z"), remap = false)
    private boolean icecore$hideFakeCustomer(CafeMenuBlockEntity menu) { return false; }
}
