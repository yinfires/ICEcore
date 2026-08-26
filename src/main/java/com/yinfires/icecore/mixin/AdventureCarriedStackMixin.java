package com.yinfires.icecore.mixin;

import com.yinfires.icecore.adventure.AdventureItemService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class AdventureCarriedStackMixin {
    @Shadow public abstract net.minecraft.world.item.ItemStack m_142621_();
    @Shadow public abstract void m_142503_(net.minecraft.world.item.ItemStack stack);

    @Inject(method="m_6877_(Lnet/minecraft/world/entity/player/Player;)V", at=@At("HEAD"), cancellable=true)
    private void icecore$preserveAdventureCarried(Player player, CallbackInfo ci) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && AdventureItemService.isAdventure(serverPlayer) && !m_142621_().isEmpty()) {
            net.minecraft.world.item.ItemStack remainder = m_142621_().copy();
            serverPlayer.getInventory().add(remainder);
            if (!remainder.isEmpty()) {
                AdventureItemService.captureCarried(serverPlayer, remainder);
                AdventureItemService.restoreAfterMenuClose(serverPlayer);
            }
            m_142503_(net.minecraft.world.item.ItemStack.EMPTY);
            ci.cancel();
        }
    }
}
