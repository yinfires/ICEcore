package com.yinfires.icecore.mixin;

import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** These upstream methods contain only the extra bottle/bucket hydration, not vanilla uses. */
@Pseudo
@Mixin(targets = "cool.bot.dewdropfarmland.events.FarmlandEventHandler", remap = false)
public abstract class DewDropIrrigationFarmlandEventsMixin {
    @Inject(method = "interactBlockEvent(Lnet/minecraftforge/event/entity/player/PlayerInteractEvent;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void icecore$noBottleBucket(PlayerInteractEvent event, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "onSplashWaterBottleHitEvent(Lnet/minecraftforge/event/entity/ProjectileImpactEvent;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void icecore$noSplashWater(ProjectileImpactEvent event, CallbackInfo ci) {
        ci.cancel();
    }
}
