package com.yinfires.icecore.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraft.client.gui.GuiGraphics;
@Pseudo
@Mixin(targets = {"com.github.ysbbbbbb.kaleidoscopecookery.client.gui.overlay.TeapotOverlay", "com.github.ysbbbbbb.kaleidoscopecookery.client.gui.overlay.PotOverlay"}, remap = false)
public abstract class WorkstationCookeryOverlayMixin {
    @Inject(method = "render(Lnet/minecraftforge/client/gui/overlay/ForgeGui;Lnet/minecraft/client/gui/GuiGraphics;FII)V", at = @At("HEAD"), cancellable = true)
    private void icecore$jadeOnly(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height, CallbackInfo ci) { ci.cancel(); }
}
