package com.yinfires.icecore.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yinfires.icecore.client.CuttingBoardItems;
import com.yinfires.icecore.workstation.CuttingBoardAccess;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "vectorwing.farmersdelight.client.renderer.CuttingBoardRenderer", remap = false)
public abstract class WorkstationCuttingBoardRenderMixin {
    @Inject(method = "render(Lvectorwing/farmersdelight/common/block/entity/CuttingBoardBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At("HEAD"), cancellable = true)
    private void icecore$items(@Coerce Object target, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay, CallbackInfo ci) {
        if (((CuttingBoardAccess)target).icecore$carved()) return;
        CuttingBoardItems.render((BlockEntity)target, pose, buffer, light, overlay); ci.cancel();
    }
}
