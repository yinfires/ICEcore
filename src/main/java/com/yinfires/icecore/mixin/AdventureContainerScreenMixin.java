package com.yinfires.icecore.mixin;

import com.yinfires.icecore.adventure.AdventureItemService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AdventureContainerScreenMixin {
    @Inject(method = "m_7933_(III)Z", at = @At("HEAD"), cancellable = true)
    private void icecore$consumeDropKey(int key, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.gameMode != null
                && minecraft.gameMode.getPlayerMode() == net.minecraft.world.level.GameType.ADVENTURE
                && minecraft.options.keyDrop.matches(key, scanCode)) cir.setReturnValue(true);
    }

    @Inject(method = "m_6597_(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ClickType;)V", at = @At("HEAD"), cancellable = true)
    private void icecore$denyContainerDrop(Slot slot, int button, int slotId, ClickType clickType, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.gameMode != null
                && minecraft.gameMode.getPlayerMode() == net.minecraft.world.level.GameType.ADVENTURE
                && (clickType == ClickType.THROW || (slot == null && clickType == ClickType.PICKUP))) ci.cancel();
    }
}
