package com.yinfires.icecore.mixin;

import com.yinfires.icecore.feedback.PlayerFeedback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets = {
    "com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.TeapotBlockEntity",
    "com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.StockpotBlockEntity"
}, remap = false)
@org.spongepowered.asm.mixin.Pseudo
public abstract class WorkstationCookeryFeedbackMixin {
    @Inject(method = "sendActionBarMessage(Lnet/minecraft/world/entity/LivingEntity;Ljava/lang/String;[Ljava/lang/Object;)V", at = @At("HEAD"), cancellable = true)
    private void icecore$feedback(LivingEntity user, String key, Object[] arguments, CallbackInfo ci) {
        if (key.endsWith("take_tea_fluid.blocked")) { ci.cancel(); return; }
        if (key.contains("container") || key.contains("carrier") || key.contains("state_incorrect") || key.contains("not_enough")) {
            if (user instanceof ServerPlayer player) PlayerFeedback.show(player, Component.translatable(key, arguments));
        }
        ci.cancel();
    }
}
