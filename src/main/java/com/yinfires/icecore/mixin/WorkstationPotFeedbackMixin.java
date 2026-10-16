package com.yinfires.icecore.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.PotBlock", remap = false)
public abstract class WorkstationPotFeedbackMixin {
    @Inject(method = "sendBarMessage(Lnet/minecraft/world/entity/player/Player;Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void icecore$jadeOnly(Player player, String key, CallbackInfo ci) { ci.cancel(); }
}
