package com.yinfires.icecore.mixin;

import com.yinfires.icecore.adventure.AdventureItemService;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Restores carried items after ServerPlayer has installed inventoryMenu. */
@Mixin(ServerPlayer.class)
public abstract class AdventureMenuCloseMixin {
    @Inject(method = "m_9230_()V", at = @At("RETURN"))
    private void icecore$restoreAfterClose(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (AdventureItemService.isAdventure(player)) AdventureItemService.restore(player);
    }
}
