package com.yinfires.icecore.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yinfires.icecore.compat.cozycafe.client.CafePlateRendering;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(targets="io.github.chakyl.cozycafe.blockentities.renderer.PlatingStationBlockEntityRenderer",remap=false)
public abstract class CozyCafePlatingRendererMixin {
    @Redirect(method="render(Lio/github/chakyl/cozycafe/blockentities/PlatingStationBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",at=@At(value="INVOKE",target="Lio/github/chakyl/cozycafe/util/GeneralUtils;renderFood(Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/block/entity/BlockEntity;II)V"))
    private void icecore$plateFood(ItemStack stack,PoseStack pose,MultiBufferSource buffers,BlockEntity station,int light,int overlay) {
        if (!CafePlateRendering.block(stack,pose,buffers,light,overlay,1.11,1.15))
            io.github.chakyl.cozycafe.util.GeneralUtils.renderFood(stack,pose,buffers,station,light,overlay);
    }
}
