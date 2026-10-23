package com.yinfires.icecore.compat.cozycafe.customer.client;
import net.minecraft.client.gui.GuiGraphics;
public interface CafeNativeScroller {
    void icecore$configure(int x,int y,int maxPixels);
    int icecore$offset();
    void icecore$draw(GuiGraphics graphics);
    void icecore$release();
}
