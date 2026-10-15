package com.yinfires.icecore.mixin;

import com.yinfires.icecore.building.BuildingClientEvents;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
    @Inject(method = "m_233746_(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"), cancellable = true)
    private void icecore$blockWrenchVanillaUse(LocalPlayer player, InteractionHand hand,
                                                BlockHitResult hit,
                                                CallbackInfoReturnable<InteractionResult> callback) {
        if (BuildingClientEvents.claimPlacement(player, hand, hit)) {
            callback.setReturnValue(InteractionResult.SUCCESS);
        } else if (BuildingClientEvents.shouldBlockWrenchBlockUse(player, hand, hit)) {
            // Let an empty main hand advance to the actual offhand wrench.
            callback.setReturnValue(com.yinfires.icecore.building.BuildingServerActions
                    .isWrench(player.getItemInHand(hand)) ? InteractionResult.SUCCESS : InteractionResult.PASS);
        }
    }
}
