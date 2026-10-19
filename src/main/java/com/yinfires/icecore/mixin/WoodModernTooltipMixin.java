package com.yinfires.icecore.mixin;

import com.yinfires.icecore.client.WoodPanel;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="icyllis.modernui.mc.TooltipRenderer",remap=false)
public abstract class WoodModernTooltipMixin {
    @ModifyVariable(method="drawTooltip",at=@At("HEAD"),argsOnly=true,ordinal=0,remap=false)
    private float icecore$pixelX(float value) { return 0; }
    @ModifyVariable(method="drawTooltip",at=@At("HEAD"),argsOnly=true,ordinal=1,remap=false)
    private float icecore$pixelY(float value) { return 0; }
    @Inject(method={"drawRoundedBackground","drawVanillaBackground"},at=@At("HEAD"),cancellable=true,remap=false)
    private void icecore$wood(GuiGraphics graphics,Matrix4f matrix,float x,float y,int width,int height,boolean titleGap,int titleHeight,CallbackInfo ci) {
        int screenWidth=net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight=net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();
        // This is inside Modern UI's native pose scope, so content follows the same correction.
        graphics.pose().translate(Math.max(6,Math.min((int)x,screenWidth-width-7))-(int)x,
                Math.max(6,Math.min((int)y,screenHeight-height-7))-(int)y,0);
        graphics.pose().pushPose(); graphics.pose().translate(Math.floor(x)-6,Math.floor(y)-6,0);
        WoodPanel.draw(graphics,0,0,width+13,height+13,1);
        graphics.pose().popPose();
        if(titleGap) WoodPanel.separator(graphics,(int)x,(int)y+titleHeight,(int)x+width);
        ci.cancel();
    }
}
