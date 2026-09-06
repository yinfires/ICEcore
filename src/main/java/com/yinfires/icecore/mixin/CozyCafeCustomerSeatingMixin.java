package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.cozycafe.seating.CozyCafeSeatAnchorEntity;
import com.yinfires.icecore.compat.cozycafe.seating.CozyCafeSeatingService;
import io.github.chakyl.cozycafe.entities.CustomerEntity;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "io.github.chakyl.cozycafe.entities.CustomerEntity", remap = false)
public abstract class CozyCafeCustomerSeatingMixin {
    @Shadow private int travelTime;

    @Inject(method = "m_8119_()V", at = @At("HEAD"), remap = false)
    private void icecore$disableInboundTravelTimeout(CallbackInfo callback) {
        CustomerEntity customer = (CustomerEntity) (Object) this;
        if (customer.getTargetMenuPos() != null || customer.getVehicle() instanceof CozyCafeSeatAnchorEntity) {
            travelTime = 0;
        }
    }

}
