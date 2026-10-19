package com.yinfires.icecore.shelf.client;

import com.yinfires.icecore.shelf.ShelfMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class ShelfScreen extends AbstractContainerScreen<ShelfMenu> {
    private static final ResourceLocation CHEST = new ResourceLocation("minecraft","textures/gui/container/generic_54.png");
    public ShelfScreen(ShelfMenu menu, Inventory inventory, Component title) {
        super(menu,inventory,title); imageHeight=258; inventoryLabelY=164;
    }
    @Override protected void renderBg(GuiGraphics graphics,float partialTick,int mouseX,int mouseY) {
        graphics.blit(CHEST,leftPos,topPos,0,0,imageWidth,17);
        for(int row=0;row<8;row++) graphics.blit(CHEST,leftPos,topPos+17+row*18,0,17,imageWidth,18);
        graphics.blit(CHEST,leftPos,topPos+161,0,125,imageWidth,97);
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        renderBackground(graphics); super.render(graphics,mouseX,mouseY,partialTick); renderTooltip(graphics,mouseX,mouseY);
    }
}
