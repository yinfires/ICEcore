package com.yinfires.icecore.mixin;

import com.yinfires.icecore.client.WoodPanel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Forge adds this overload without an SRG name; verified against its production JAR.
@Mixin(targets = "net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil", remap = false)
public abstract class WoodTooltipBackgroundMixin {
    @Inject(method="renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIIIIIII)V",at=@At("HEAD"),cancellable=true, remap = false)
    private static void icecore$wood(GuiGraphics graphics,int x,int y,int width,int height,int z,int backgroundTop,int backgroundBottom,int borderTop,int borderBottom,CallbackInfo ci) {
        graphics.pose().pushPose(); graphics.pose().translate(0,0,z);
        WoodPanel.drawAround(graphics,x,y,width,height,1);
        graphics.pose().popPose(); ci.cancel();
    }
}
