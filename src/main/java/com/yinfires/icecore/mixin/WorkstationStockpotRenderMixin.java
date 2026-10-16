package com.yinfires.icecore.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.StockpotBlockEntity;
import com.yinfires.icecore.workstation.StockpotProducts;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.client.render.block.StockpotBlockEntityRender", remap = false)
public abstract class WorkstationStockpotRenderMixin {
    @Inject(method = "render(Lcom/github/ysbbbbbb/kaleidoscopecookery/blockentity/kitchen/StockpotBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At("HEAD"), cancellable = true)
    private void icecore$solidProduct(StockpotBlockEntity pot, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay, CallbackInfo ci) {
        if (pot.getStatus() != 3 || pot.hasLid() || pot.getResult().isEmpty()) return;
        Boolean needsCarrier = ((com.yinfires.icecore.workstation.StockpotProductView)pot).icecore$needsCarrier();
        if (needsCarrier == null ? !StockpotProducts.carrier(pot).isEmpty() : needsCarrier) return;
        pose.pushPose();
        pose.translate(.5, .1, .5);
        pose.mulPose(Axis.YP.rotationDegrees(pot.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).get2DDataValue() * 90));
        pose.mulPose(Axis.XP.rotationDegrees(90)); pose.scale(.5F, .5F, .5F);
        var item = pot.getResult().copyWithCount(1);
        var model = com.github.ysbbbbbb.kaleidoscopecookery.client.resources.ItemRenderReplacer.getModel(pot.getLevel(), item,
                com.github.ysbbbbbb.kaleidoscopecookery.client.resources.ItemRenderReplacerReloadListener.INSTANCE.pot());
        Minecraft.getInstance().getItemRenderer().render(item, ItemDisplayContext.FIXED, false, pose, buffer, light, overlay, model);
        pose.popPose(); ci.cancel();
    }
}
