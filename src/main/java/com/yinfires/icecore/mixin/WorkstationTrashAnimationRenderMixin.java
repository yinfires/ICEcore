package com.yinfires.icecore.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.misc.TrashCanBlockEntity;
import com.yinfires.icecore.workstation.TrashCanAnimationAccess;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Expire pending states even while the container tick is paused; rendering remains native. */
@org.spongepowered.asm.mixin.Pseudo
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.client.render.block.TrashCanBlockEntityRender", remap = false)
public abstract class WorkstationTrashAnimationRenderMixin {
    @Inject(method = "render(Lcom/github/ysbbbbbb/kaleidoscopecookery/blockentity/misc/TrashCanBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At("HEAD"))
    private void icecore$expire(TrashCanBlockEntity trash, float partial, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay, CallbackInfo ci) {
        if (trash instanceof TrashCanAnimationAccess access) access.icecore$expireTrashAnimation();
    }
}
