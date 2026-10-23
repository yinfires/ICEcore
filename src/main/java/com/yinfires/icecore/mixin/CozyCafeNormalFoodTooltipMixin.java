package com.yinfires.icecore.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.*;
import java.util.*;

@Mixin(targets="io.github.chakyl.cozycafe.gui.MenuSelectorScreen",remap=false)
public abstract class CozyCafeNormalFoodTooltipMixin {
    @Unique private ItemStack icecore$tooltipStack=ItemStack.EMPTY;
    @Redirect(method={"m_7286_(Lnet/minecraft/client/gui/GuiGraphics;FII)V","m_280072_(Lnet/minecraft/client/gui/GuiGraphics;II)V"},at=@At(value="INVOKE",target="Lio/github/chakyl/cozycafe/gui/MenuSelectorScreen;m_280152_(Lnet/minecraft/client/Minecraft;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;"),require=2,allow=2)
    private List<Component> icecore$captureStack(Minecraft minecraft,ItemStack stack){
        icecore$tooltipStack=stack;
        return Screen.getTooltipFromItem(minecraft,stack);
    }
    @Redirect(method={"m_7286_(Lnet/minecraft/client/gui/GuiGraphics;FII)V","m_280072_(Lnet/minecraft/client/gui/GuiGraphics;II)V"},at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;m_280677_(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;II)V"),require=2,allow=2)
    private void icecore$normal(GuiGraphics graphics, Font font, List<Component> lines, Optional<TooltipComponent> image, int x,int y){
        try { graphics.renderTooltip(font,icecore$tooltipStack,x,y); }
        finally { icecore$tooltipStack=ItemStack.EMPTY; }
    }
}
