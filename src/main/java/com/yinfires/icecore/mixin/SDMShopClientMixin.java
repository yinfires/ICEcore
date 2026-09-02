package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.sdmshop.SDMShopCompat;
import dev.architectury.event.EventResult;
import dev.ftb.mods.ftblibrary.ui.CustomClickEvent;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Disables SDM's legacy key and sidebar click entry points. */
@Pseudo
@Mixin(targets = "net.sixik.sdmshoprework.SDMShopClient", remap = false)
public abstract class SDMShopClientMixin {
    @Inject(method = "keyInput(Lnet/minecraft/client/Minecraft;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void icecore$disableKey(Minecraft minecraft, CallbackInfo callback) {
        if (SDMShopCompat.isLoaded()) callback.cancel();
    }

    @Inject(method = "customClick(Ldev/ftb/mods/ftblibrary/ui/CustomClickEvent;)Ldev/architectury/event/EventResult;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void icecore$disableSidebar(CustomClickEvent event, CallbackInfoReturnable<EventResult> callback) {
        if ("sdmshoprework:open_gui".equals(event.id().toString())) callback.setReturnValue(EventResult.interruptTrue());
    }

}
