package com.yinfires.icecore.mixin;

import com.yinfires.icecore.client.WoodPanel;
import net.minecraft.client.gui.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class WoodOverlayBackgroundMixin {
    @Inject(method="m_93039_(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;III)V",at=@At("HEAD"),cancellable=true)
    private void icecore$wood(GuiGraphics graphics,Font font,int y,int width,int color,CallbackInfo ci) {
        // drawBackdrop is shared with titles; scope this skin to the action bar's y=-4 call.
        if(y!=-4) return;
        WoodPanel.draw(graphics,-width/2-4,y-4,width+8,font.lineHeight+8,(color>>>24)/255f); ci.cancel();
    }
}
