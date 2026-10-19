package com.yinfires.icecore.mixin;

import com.yinfires.icecore.spoilage.*;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ItemRenderer.class)
public abstract class ShelfLifeItemRendererMixin {
    @Unique private final java.util.Map<BakedModel, BakedModel> icecore$expiredModels = new java.util.IdentityHashMap<>();
    @Inject(method = "m_174264_(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)Lnet/minecraft/client/resources/model/BakedModel;", at = @At("HEAD"), cancellable = true)
    private void icecore$model(ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.entity.LivingEntity entity,
                               int seed, CallbackInfoReturnable<BakedModel> cir) {
        if (!(stack.getItem() instanceof ExpiredFoodItem)) return;
        ItemStack original = ExpiredFoodItem.original(stack);
        if (original.isEmpty()) return;
        BakedModel model = ((ItemRenderer)(Object)this).getModel(original, level, entity, seed);
        if (icecore$expiredModels.size() >= 512) icecore$expiredModels.clear();
        cir.setReturnValue(icecore$expiredModels.computeIfAbsent(model, ExpiredFoodModel::new));
    }
    @Inject(method = "m_6213_(Lnet/minecraft/server/packs/resources/ResourceManager;)V", at = @At("HEAD"))
    private void icecore$reload(net.minecraft.server.packs.resources.ResourceManager resources, CallbackInfo ci) { icecore$expiredModels.clear(); }
}
