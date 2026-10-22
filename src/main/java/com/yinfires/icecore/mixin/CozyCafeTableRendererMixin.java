package com.yinfires.icecore.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yinfires.icecore.compat.cozycafe.client.CafeTableRendering;
import io.github.chakyl.cozycafe.blockentities.CafeMenuBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="io.github.chakyl.cozycafe.blockentities.renderer.CafeMenuBlockEntityRenderer",remap=false)
public abstract class CozyCafeTableRendererMixin {
    @Redirect(method="render(Lio/github/chakyl/cozycafe/blockentities/CafeMenuBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",at=@At(value="INVOKE",target="Lio/github/chakyl/cozycafe/blockentities/CafeMenuBlockEntity;getCurrentCourse()I"))
    private int icecore$servingPosition(CafeMenuBlockEntity menu) {
        return CafeTableRendering.displayCourse(menu);
    }
    @Redirect(method="render(Lio/github/chakyl/cozycafe/blockentities/CafeMenuBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",at=@At(value="INVOKE",target="Lio/github/chakyl/cozycafe/util/GeneralUtils;renderFood(Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/block/entity/BlockEntity;II)V"))
    private void icecore$platedIcon(ItemStack stack,PoseStack pose,MultiBufferSource buffers,net.minecraft.world.level.block.entity.BlockEntity menu,int light,int overlay) {
        if (menu instanceof com.yinfires.icecore.compat.cozycafe.CafeTableAccess table && table.icecore$tablePlated()
            && com.yinfires.icecore.compat.cozycafe.client.CafePlateRendering.block(stack,pose,buffers,light,overlay,.1,2.0/16)) return;
        if (!CafeTableRendering.icon(stack)) { io.github.chakyl.cozycafe.util.GeneralUtils.renderFood(stack,pose,buffers,menu,light,overlay); return; }
        pose.pushPose();
        var renderer=net.minecraft.client.Minecraft.getInstance().getItemRenderer();
        var model=renderer.getModel(stack,menu.getLevel(),null,0);
        pose.translate(0,-.5F*com.yinfires.icecore.compat.cozycafe.client.CafeModelBounds.minimumY(model,ItemDisplayContext.FIXED,false),0);
        pose.scale(.5F,.5F,.5F);
        net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(stack,ItemDisplayContext.FIXED,light,overlay,pose,buffers,menu.getLevel(),0);
        pose.popPose();
    }
    @Redirect(method="render(Lio/github/chakyl/cozycafe/blockentities/CafeMenuBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",at=@At(value="INVOKE",target="Lio/github/chakyl/cozycafe/blockentities/CafeMenuBlockEntity;getEatingItem()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack icecore$nativeFood(CafeMenuBlockEntity menu) { return CafeTableRendering.nativeFood(menu); }
    @Inject(method="render(Lio/github/chakyl/cozycafe/blockentities/CafeMenuBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",at=@At("TAIL"))
    private void icecore$table(CafeMenuBlockEntity menu,float tick,PoseStack pose,MultiBufferSource buffers,int light,int overlay,CallbackInfo ci) {
        CafeTableRendering.render(menu,pose,buffers,light,overlay);
    }
}
