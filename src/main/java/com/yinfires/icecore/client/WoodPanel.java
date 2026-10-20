package com.yinfires.icecore.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Shared nine-slice skin. Content, positioning and timing remain owned by the callers. */
public final class WoodPanel {
    public static final ResourceLocation TEXTURE = new ResourceLocation("icecore","textures/gui/wood_panel.png");
    public static void drawAround(GuiGraphics graphics,int x,int y,int width,int height,float alpha) {
        draw(graphics,WoodPanelLayout.outerStart(x),WoodPanelLayout.outerStart(y),
                WoodPanelLayout.outerSize(width),WoodPanelLayout.outerSize(height),alpha);
    }
    public static void draw(GuiGraphics graphics,int x,int y,int width,int height,float alpha) {
        if (width<=0 || height<=0 || alpha<=0) return;
        graphics.flush();
        float[] color=RenderSystem.getShaderColor();
        float r=color[0],g=color[1],b=color[2],a=color[3];
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1,1,1,alpha);
        // The source crop is 27x27, with a six-pixel immutable border.
        // GuiGraphics' nine-slice helper assumes 256px textures; draw the nine pieces explicitly.
        int edgeX=Math.min(WoodPanelLayout.BORDER,width/2),edgeY=Math.min(WoodPanelLayout.BORDER,height/2);
        for(int row=0;row<3;row++) for(int col=0;col<3;col++) {
            int sx=col==0?0:col==1?6:21, sy=row==0?0:row==1?6:21;
            int sw=col==1?15:6,sh=row==1?15:6;
            int dx=x+(col==0?0:col==1?edgeX:width-edgeX);
            int dy=y+(row==0?0:row==1?edgeY:height-edgeY);
            int dw=col==1?width-2*edgeX:edgeX,dh=row==1?height-2*edgeY:edgeY;
            if(dw>0&&dh>0) graphics.blit(TEXTURE,dx,dy,dw,dh,sx,sy,sw,sh,27,27);
        }
        graphics.flush(); RenderSystem.setShaderColor(r,g,b,a);
    }
    /** Shared one-logical-pixel white rule, without inset, shadow or gradient. */
    public static void separator(GuiGraphics graphics,int left,int top,int right) {
        graphics.fill(left,top,right,top+1,0xFFFFFFFF);
    }
    private WoodPanel() {}
}
