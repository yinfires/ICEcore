package com.yinfires.icecore.compat.cozycafe.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.*;

/** Same fitted geometry and supplied light for every plate display. */
public final class CafePlateRendering {
    private CafePlateRendering() {}
    public static boolean block(ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay,double nativeHeight,double plateTop) {
        var model=CafePlatedFoodModels.model(stack);
        pose.pushPose();
        pose.translate(0,plateTop+.001-nativeHeight,0);
        Minecraft.getInstance().getItemRenderer().render(stack,ItemDisplayContext.NONE,false,pose,buffers,light,overlay,model);
        pose.popPose();
        return true;
    }
}