package com.yinfires.icecore.mixin;

import com.yinfires.icecore.building.BuildingClientEvents;
import com.yinfires.icecore.food.FoodConsumptionClientState;
import com.yinfires.icecore.food.FoodConsumptionRules;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hard-stop the local block-use branch for a wrench target.  The surrounding
 * vanilla useItemOn call still sends its use-on packet, so the server remains
 * responsible for the actual dismantle decision.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    /**
     * This is the final item-use fallback in Minecraft.startUseItem. Block and
     * entity interactions have already had a chance to consume the click before
     * this method is called, so returning PASS here suppresses consumption
     * without breaking those interactions or invoking the item's custom use
     * method (and therefore without starting its animation or sound).
     */
    @Inject(method = "m_233721_(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"), cancellable = true)
    private void icecore$blockFoodUseFallback(Player player, InteractionHand hand,
                                               CallbackInfoReturnable<InteractionResult> callback) {
        if (FoodConsumptionClientState.disabled()
                && FoodConsumptionRules.isFood(player.getItemInHand(hand), player)) {
            player.stopUsingItem();
            callback.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "m_233746_(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"), cancellable = true)
    private void icecore$blockWrenchVanillaUse(LocalPlayer player, InteractionHand hand,
                                                BlockHitResult hit,
                                                CallbackInfoReturnable<InteractionResult> callback) {
        if (BuildingClientEvents.shouldBlockWrenchBlockUse(player, hand, hit)) {
            callback.setReturnValue(InteractionResult.PASS);
        }
    }
}
