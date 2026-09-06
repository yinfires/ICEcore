package com.yinfires.icecore.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** NPC-style passage for CozyCafe customers while keeping them crosshair-pickable. */
@Mixin(Entity.class)
public abstract class CozyCafeCustomerCollisionMixin {
    private static final String CUSTOMER = "io.github.chakyl.cozycafe.entities.CustomerEntity";

    @Inject(method = "m_6094_()Z", at = @At("HEAD"), cancellable = true)
    private void icecore$customerNotPushable(CallbackInfoReturnable<Boolean> callback) {
        if (icecore$isCustomer((Entity) (Object) this)) callback.setReturnValue(false);
    }

    @Inject(method = "m_5829_()Z", at = @At("HEAD"), cancellable = true)
    private void icecore$customerNotCollidable(CallbackInfoReturnable<Boolean> callback) {
        if (icecore$isCustomer((Entity) (Object) this)) callback.setReturnValue(false);
    }

    @Inject(method = "m_6087_()Z", at = @At("HEAD"), cancellable = true)
    private void icecore$customerPickable(CallbackInfoReturnable<Boolean> callback) {
        if (icecore$isCustomer((Entity) (Object) this)) callback.setReturnValue(true);
    }

    @Inject(method = "m_7334_(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void icecore$customersDoNotPush(Entity other, CallbackInfo callback) {
        if (icecore$isCustomer((Entity) (Object) this) || icecore$isCustomer(other)) callback.cancel();
    }

    private static boolean icecore$isCustomer(Entity entity) {
        return entity != null && CUSTOMER.equals(entity.getClass().getName());
    }
}
