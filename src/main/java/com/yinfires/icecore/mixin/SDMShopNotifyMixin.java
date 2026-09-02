package com.yinfires.icecore.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses SDM Shop Rework's "you bought/sold X" chat message. {@code ShopItemEntryType}
 * overrides the otherwise-empty {@code AbstractShopEntryType.sendNotifiedMessage} to push a
 * component into the player's chat box on every buy/sell; cancelling at HEAD stops that popup.
 */
@Pseudo
@Mixin(targets = "net.sixik.sdmshoprework.common.shop.type.ShopItemEntryType", remap = false)
public abstract class SDMShopNotifyMixin {
    @Inject(method = "sendNotifiedMessage(Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void icecore$suppressPurchaseMessage(Player player, CallbackInfo callback) {
        callback.cancel();
    }
}
