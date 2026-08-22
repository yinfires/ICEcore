package com.yinfires.icecore.mixin;

import com.yinfires.icecore.building.BuildingClientEvents;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-side last line of defence.  Returning PASS leaves the outer vanilla
 * use-on call free to send its packet, but makes the local BlockState.use call
 * impossible for a wrench target, including failed removal attempts.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class ClientBlockStateUseMixin {
    @Inject(method = "m_60664_(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"), cancellable = true)
    private void icecore$blockWrenchClientUse(net.minecraft.world.level.Level level,
                                               net.minecraft.world.entity.player.Player player,
                                               InteractionHand hand, BlockHitResult hit,
                                               CallbackInfoReturnable<InteractionResult> callback) {
        if (player instanceof LocalPlayer localPlayer
                && BuildingClientEvents.shouldBlockWrenchBlockUse(localPlayer, hand, hit)) {
            callback.setReturnValue(InteractionResult.PASS);
        }
    }
}
