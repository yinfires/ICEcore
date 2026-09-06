package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.cozycafe.seating.CozyCafeSeatingService;
import io.github.chakyl.cozycafe.entities.CustomerEntity;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "io.github.chakyl.cozycafe.entities.CustomerEntity$NavigateToMenuGoal", remap = false)
public abstract class CozyCafeNavigateToMenuMixin {
    @Shadow @Final private CustomerEntity customer;
    @Shadow @Final private double speed;

    @Inject(method = "m_8036_()Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void icecore$canUseSeat(CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(CozyCafeSeatingService.navigationTarget(customer) != null);
    }

    @Inject(method = "m_8045_()Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void icecore$canContinueSeat(CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(CozyCafeSeatingService.navigationTarget(customer) != null
                && !customer.isPassenger());
    }

    @Inject(method = "m_8056_()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void icecore$startSeat(CallbackInfo callback) {
        CozyCafeSeatingService.startNavigation(customer, speed);
        callback.cancel();
    }

    @Inject(method = "m_8037_()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void icecore$navigateToSeat(CallbackInfo callback) {
        callback.cancel();
        if (CozyCafeSeatingService.navigationTarget(customer) == null) {
            customer.setTargetMenuPos(null);
            return;
        }
        if (CozyCafeSeatingService.trySeat(customer)) return;
        CozyCafeSeatingService.tickNavigation(customer, speed);
    }
}
