package com.yinfires.icecore.mixin;

import io.github.chakyl.cozycafe.blockentities.CafeManagerBlockEntity;
import io.github.chakyl.cozycafe.network.EvilPacketsIHateThem;
import io.github.chakyl.cozycafe.network.ServerBoundRenameCafePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Makes CozyCafe's Enter/Escape rename path authoritative instead of client-only. */
@Pseudo
@Mixin(targets = "io.github.chakyl.cozycafe.gui.CafeManagerMenu", remap = false)
public abstract class CozyCafeManagerNameMixin {
    @Shadow public CafeManagerBlockEntity blockEntity;

    @Inject(method = "setName(Ljava/lang/String;)V", at = @At("HEAD"), remap = false)
    private void icecore$submitClientName(String name, CallbackInfo callback) {
        if (blockEntity.getLevel() != null && blockEntity.getLevel().isClientSide) {
            EvilPacketsIHateThem.sendToServer(new ServerBoundRenameCafePacket(blockEntity.getBlockPos(), name));
        }
    }
}
