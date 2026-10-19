package com.yinfires.icecore.compat.jade;

import com.yinfires.icecore.client.WoodPanel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.ui.Element;

/** A Jade layout element using the exact same rule as the item tooltip. */
public final class WoodTitleSeparator extends Element {
    @Override public Vec2 getSize() { return new Vec2(0,2); }
    @Override public void render(GuiGraphics graphics,float x,float y,float right,float bottom) {
        WoodPanel.separator(graphics,(int)x,(int)y,(int)right);
    }
}
