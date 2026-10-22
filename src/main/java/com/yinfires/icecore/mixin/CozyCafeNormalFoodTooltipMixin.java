package com.yinfires.icecore.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import java.util.*;

@Mixin(targets="io.github.chakyl.cozycafe.gui.MenuSelectorScreen",remap=false)
public abstract class CozyCafeNormalFoodTooltipMixin {
    @Redirect(method={"m_7286_(Lnet/minecraft/client/gui/GuiGraphics;FII)V","m_280072_(Lnet/minecraft/client/gui/GuiGraphics;II)V"},at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;m_280677_(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;II)V"),require=2,allow=2)
    private void icecore$normal(GuiGraphics graphics, Font font, List<Component> lines, Optional<TooltipComponent> image, int x,int y){
        if(lines.stream().anyMatch(c->c.getContents() instanceof com.yinfires.icecore.tooltip.FoodCategory)){
            lines=new ArrayList<>(lines);lines.removeIf(com.yinfires.icecore.tooltip.RecipeManagerTooltips::cafeLine);
        }
        graphics.renderTooltip(font,lines,image,x,y);
    }
}
