package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.cozycafe.customer.client.CafeNativeScroller;
import io.github.chakyl.cozycafe.gui.MenuSelectorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import java.util.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adapter exposes the actual upstream scroller; its draw and mouse methods remain upstream-owned. */
@Pseudo
@Mixin(targets="io.github.chakyl.cozycafe.gui.MenuSelectorScreen",remap=false)
public abstract class CozyCafeNativeScrollerMixin extends AbstractContainerScreen<MenuSelectorMenu> implements CafeNativeScroller {
    @Shadow private List<ItemStack> cafeMenu;
    @Shadow int scrollOff;
    @Shadow private boolean isDragging;
    @Unique private boolean icecore$adapter;
    @Shadow private void renderScroller(GuiGraphics graphics){throw new AssertionError();}
    protected CozyCafeNativeScrollerMixin(MenuSelectorMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    public void icecore$configure(int x,int y,int maxPixels){
        icecore$adapter=true;
        leftPos=x-155;topPos=y-22;
        imageWidth=width-2*leftPos;imageHeight=height-2*topPos;
        cafeMenu=Collections.nCopies(Math.max(0,maxPixels)+4,ItemStack.EMPTY);
        scrollOff=Math.min(scrollOff,Math.max(0,maxPixels));
    }
    public int icecore$offset(){return scrollOff;}
    public void icecore$draw(GuiGraphics graphics){
        // Upstream integer thumb math assumes a short item list. Preserve its native drawing
        // while projecting the pixel offset onto its 33 logical thumb positions.
        var entries=cafeMenu;int offset=scrollOff,max=Math.max(0,entries.size()-4);
        if(max>33){cafeMenu=Collections.nCopies(37,ItemStack.EMPTY);scrollOff=(int)Math.round(offset*33.0/max);}
        try{renderScroller(graphics);}finally{cafeMenu=entries;scrollOff=offset;}
    }
    public void icecore$release(){isDragging=false;}
    @Inject(method="m_6375_(DDI)Z",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;m_6375_(DDI)Z"),cancellable=true)
    private void icecore$noContainerClick(double x,double y,int button,CallbackInfoReturnable<Boolean> ci){if(icecore$adapter)ci.setReturnValue(isDragging);}
    @Inject(method="m_7979_(DDIDD)Z",at=@At("HEAD"),cancellable=true)
    private void icecore$noContainerDrag(double x,double y,int button,double dx,double dy,CallbackInfoReturnable<Boolean> ci){if(icecore$adapter&&!isDragging)ci.setReturnValue(false);}
}
