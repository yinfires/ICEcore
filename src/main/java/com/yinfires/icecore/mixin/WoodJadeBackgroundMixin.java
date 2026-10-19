package com.yinfires.icecore.mixin;

import com.yinfires.icecore.client.WoodPanel;
import net.minecraft.client.gui.GuiGraphics;
import snownee.jade.overlay.TooltipRenderer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="snownee.jade.overlay.OverlayRenderer",remap=false)
public abstract class WoodJadeBackgroundMixin {
    @Inject(method="drawTooltipBox",at=@At("HEAD"),cancellable=true,remap=false)
    private static void icecore$wood(GuiGraphics graphics,int x,int y,int width,int height,float alpha,boolean square,TooltipRenderer tooltip,CallbackInfo ci) {
        WoodPanel.draw(graphics,x,y,width,height,alpha); ci.cancel();
    }
}
