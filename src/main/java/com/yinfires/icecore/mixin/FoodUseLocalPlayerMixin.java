package com.yinfires.icecore.mixin;

import com.yinfires.icecore.food.BlockedFoodUse;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class FoodUseLocalPlayerMixin {
    @Inject(method = "m_6672_(Lnet/minecraft/world/InteractionHand;)V", at = @At("RETURN"))
    private void icecore$clearRejectedUseFlag(InteractionHand hand, CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        // LocalPlayer sets startedUsingItem AFTER the superclass Start event returns.
        if (BlockedFoodUse.matches(player, hand) && player.getUseItem().isEmpty()) {
            player.stopUsingItem();
        }
    }
}
