package com.yinfires.icecore.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yinfires.icecore.shelf.client.RackDisplayCache;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.xkmc.youkaishomecoming.content.pot.storage.ingredient.IngredientRackRenderer", remap = false)
public abstract class RackDisplayRendererMixin {
    @Inject(method = "render(Ldev/xkmc/youkaishomecoming/content/pot/storage/ingredient/IngredientRackBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At("HEAD"), cancellable = true)
    private void icecore$scene(@Coerce Object entity,float tick,PoseStack pose,MultiBufferSource buffers,int light,int overlay,CallbackInfo ci) {
        if(RackDisplayCache.render(this,(BlockEntity)entity,tick,pose,buffers,light,overlay)) ci.cancel();
    }
    @Redirect(method = "render(Ldev/xkmc/youkaishomecoming/content/pot/storage/ingredient/IngredientRackBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;m_269128_(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;IILcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;I)V"))
    private void icecore$item(ItemRenderer renderer,ItemStack stack,ItemDisplayContext context,int light,int overlay,PoseStack pose,MultiBufferSource buffers,Level level,int seed) {
        RackDisplayCache.item(renderer,stack,context,light,overlay,pose,buffers,level,seed);
    }
}
