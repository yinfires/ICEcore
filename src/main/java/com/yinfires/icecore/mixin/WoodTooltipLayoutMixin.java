package com.yinfires.icecore.mixin;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.*;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(GuiGraphics.class)
public abstract class WoodTooltipLayoutMixin {
    @Redirect(method="m_280497_(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V",remap=false,at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;m_262814_(IIIIII)Lorg/joml/Vector2ic;"))
    private Vector2ic icecore$position(ClientTooltipPositioner positioner,int screenWidth,int screenHeight,int mouseX,int mouseY,int width,int height) {
        Vector2ic position=positioner.positionTooltip(screenWidth,screenHeight,mouseX,mouseY,width,height);
        return new Vector2i(Math.max(6,Math.min(position.x(),screenWidth-width-7)),Math.max(6,Math.min(position.y(),screenHeight-height-7)));
    }
    @Redirect(method="m_280497_(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V",remap=false,at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;m_142440_(Lnet/minecraft/client/gui/Font;IILorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V"))
    private void icecore$title(ClientTooltipComponent component,Font font,int x,int y,org.joml.Matrix4f matrix,net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers,Font originalFont,List<ClientTooltipComponent> components,int mouseX,int mouseY,ClientTooltipPositioner positioner) {
        component.renderText(font,x,y,matrix,buffers);
        if(components.size()>1 && component==components.get(0) && component instanceof ClientTextTooltip) {
            int width=0;
            for(ClientTooltipComponent entry:components) width=Math.max(width,entry.getWidth(font));
            com.yinfires.icecore.client.WoodPanel.separator((GuiGraphics)(Object)this,x,y+component.getHeight(),x+width);
        }
    }
}
