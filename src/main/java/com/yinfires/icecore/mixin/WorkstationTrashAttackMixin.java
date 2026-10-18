package com.yinfires.icecore.mixin;

import com.yinfires.icecore.workstation.TrashCanClientEvents;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Own both attack paths before creative instant breaking or survival mining can begin. */
@Mixin(Minecraft.class)
public abstract class WorkstationTrashAttackMixin {
    @Inject(method = "m_202354_()Z", at = @At("HEAD"), cancellable = true)
    private void icecore$putOnAttack(CallbackInfoReturnable<Boolean> cir) {
        if (TrashCanClientEvents.attack(true, true)) cir.setReturnValue(false);
    }

    @Inject(method = "m_91386_(Z)V", at = @At("HEAD"), cancellable = true)
    private void icecore$stopContinuedMining(boolean down, CallbackInfo ci) {
        if (TrashCanClientEvents.attack(down, false)) {
            Minecraft minecraft = (Minecraft) (Object) this;
            if (minecraft.gameMode != null) minecraft.gameMode.stopDestroyBlock();
            ci.cancel();
        }
    }
}
