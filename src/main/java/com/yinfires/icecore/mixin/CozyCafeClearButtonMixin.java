package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.cozycafe.CozyCafeMenuStateAccess;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Silently consumes CozyCafe's clear-button click while the cafe is open. */
@Pseudo
@Mixin(targets = "io.github.chakyl.cozycafe.gui.CafeManagerScreen", remap = false)
public abstract class CozyCafeClearButtonMixin extends AbstractContainerScreen<AbstractContainerMenu> {
    @Unique
    private AbstractWidget icecore$clearButton;
    @Unique
    private Boolean icecore$lastOpenState;

    protected CozyCafeClearButtonMixin(AbstractContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "m_7856_()V", at = @At("TAIL"), remap = false)
    private void icecore$captureClearButton(CallbackInfo callback) {
        if (!children().isEmpty() && children().get(children().size() - 1) instanceof AbstractWidget widget) {
            icecore$clearButton = widget;
        }
        icecore$updateClearButton();
    }

    @Inject(method = "m_88315_(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At("HEAD"), remap = false)
    private void icecore$disableClearWhileOpen(GuiGraphics graphics, int mouseX, int mouseY,
                                                float partialTick, CallbackInfo callback) {
        icecore$updateClearButton();
    }

    @Unique
    private void icecore$updateClearButton() {
        if (icecore$clearButton != null && menu instanceof CozyCafeMenuStateAccess access) {
            boolean open = access.icecore$isCafeOpen();
            icecore$clearButton.active = !open;
            icecore$clearButton.visible = !open;
            if (icecore$lastOpenState == null || icecore$lastOpenState != open) {
                icecore$clearButton.setTooltip(open ? null : Tooltip.create(
                        Component.translatable("gui.cozycafe.cafe_manager.clear_data")));
                icecore$lastOpenState = open;
            }
        }
    }
}
