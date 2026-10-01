package com.yinfires.icecore.mixin;

import com.yinfires.icecore.food.BlockedFoodUse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class FoodUseFeedbackMixin {
    @Inject(method = "m_91277_()V", at = @At("HEAD"))
    private void icecore$beginFoodFeedback(CallbackInfo ci) {
        BlockedFoodUse.begin();
    }

    @Redirect(method = "m_91277_()V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;m_109320_(Lnet/minecraft/world/InteractionHand;)V"))
    private void icecore$skipBlockedFoodLowering(ItemInHandRenderer renderer, InteractionHand hand) {
        Minecraft minecraft = (Minecraft) (Object) this;
        if (!BlockedFoodUse.matches(minecraft.player, hand)) {
            renderer.itemUsed(hand);
        }
    }

    @Inject(method = "m_91277_()V", at = @At("RETURN"))
    private void icecore$endFoodFeedback(CallbackInfo ci) {
        BlockedFoodUse.clear();
    }
}
