package com.yinfires.icecore.mixin;

import com.yinfires.icecore.adventure.AdventureItemService;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class AdventureDropPacketMixin {
    @Shadow public net.minecraft.server.level.ServerPlayer f_9743_;

    @Inject(method="m_7502_(Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket;)V", at=@At("HEAD"), cancellable=true)
    private void icecore$denyAdventureDrop(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (AdventureItemService.isAdventure(f_9743_)) {
            var action = packet.getAction();
            if (action == ServerboundPlayerActionPacket.Action.DROP_ITEM
                    || action == ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS) ci.cancel();
        }
    }

    @Inject(method="m_5914_(Lnet/minecraft/network/protocol/game/ServerboundContainerClickPacket;)V", at=@At("HEAD"), cancellable=true)
    private void icecore$denyAdventureContainerDrop(net.minecraft.network.protocol.game.ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (!AdventureItemService.isAdventure(f_9743_)) return;
        if (packet.getClickType() == net.minecraft.world.inventory.ClickType.THROW
                || (packet.getSlotNum() == -999
                && packet.getClickType() == net.minecraft.world.inventory.ClickType.PICKUP)) {
            f_9743_.containerMenu.broadcastFullState();
            ci.cancel();
        }
    }
}
