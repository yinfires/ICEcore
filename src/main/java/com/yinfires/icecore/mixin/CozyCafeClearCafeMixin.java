package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.cozycafe.CozyCafeManagerStateAccess;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Rejects CozyCafe's destructive clear action while the cafe is open. */
@Pseudo
@Mixin(targets = "io.github.chakyl.cozycafe.blockentities.CafeManagerBlockEntity", remap = false)
public abstract class CozyCafeClearCafeMixin implements CozyCafeManagerStateAccess {
    @Shadow
    private BlockPos linkedSign;

    @Shadow
    public abstract boolean isOpen();

    @Override
    public BlockPos icecore$getLinkedSign() {
        return linkedSign;
    }

    @Inject(method = "clearCafeData()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void icecore$rejectClearWhileOpen(CallbackInfo callback) {
        if (isOpen()) {
            callback.cancel();
        }
    }
}
