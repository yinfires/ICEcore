package com.yinfires.icecore.mixin;

import com.yinfires.icecore.client.WoodPanel;
import com.yinfires.icecore.client.WoodPanelLayout;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="icyllis.modernui.mc.TooltipRenderer",remap=false)
public abstract class WoodModernTooltipMixin {
    @Unique private int icecore$categorySeparator = -1;
    @Inject(method="drawTooltip",at=@At("HEAD"),remap=false)
    private void icecore$category(net.minecraft.world.item.ItemStack stack,GuiGraphics graphics,
        java.util.List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> list,
        int mouseX,int mouseY,net.minecraft.client.gui.Font font,int screenWidth,int screenHeight,
        float partialX,float partialY,net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner positioner,CallbackInfo ci) {
        icecore$categorySeparator=-1;
        int height=0;
        for(int i=0;i<list.size();i++){
            var component=list.get(i);
            if(component instanceof com.yinfires.icecore.tooltip.FoodCategoryTooltip category){
                icecore$categorySeparator=i+1<list.size()?height+category.separatorOffset():-2;break;
            }
            height+=component.getHeight();
        }
    }
    @ModifyVariable(method="drawTooltip",at=@At("HEAD"),argsOnly=true,ordinal=0,remap=false)
    private float icecore$pixelX(float value) { return 0; }
    @ModifyVariable(method="drawTooltip",at=@At("HEAD"),argsOnly=true,ordinal=1,remap=false)
    private float icecore$pixelY(float value) { return 0; }
    @Inject(method={"drawRoundedBackground","drawVanillaBackground"},at=@At("HEAD"),cancellable=true,remap=false)
    private void icecore$wood(GuiGraphics graphics,Matrix4f matrix,float x,float y,int width,int height,boolean titleGap,int titleHeight,CallbackInfo ci) {
        int screenWidth=net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight=net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();
        // This is inside Modern UI's native pose scope, so content follows the same correction.
        // Match native drawX/drawY truncation. The existing -mScroll pose remains untouched.
        int contentX=(int)x,contentY=(int)y;
        graphics.pose().translate(WoodPanelLayout.constrain(contentX,width,screenWidth)-contentX,
                WoodPanelLayout.constrain(contentY,height,screenHeight)-contentY,0);
        WoodPanel.drawAround(graphics,contentX,contentY,width,height,1);
        if(icecore$categorySeparator>=0) WoodPanel.separator(graphics,(int)x,(int)y+icecore$categorySeparator+(titleGap?2:0),(int)x+width);
        else if(icecore$categorySeparator==-1 && titleGap) WoodPanel.separator(graphics,(int)x,(int)y+titleHeight,(int)x+width);
        ci.cancel();
    }
}
