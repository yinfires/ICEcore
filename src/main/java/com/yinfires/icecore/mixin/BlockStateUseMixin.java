package com.yinfires.icecore.mixin;

import com.yinfires.icecore.building.BuildingServerActions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Server-side last line of defence against vanilla block interaction.  The
 * wrench action itself is still handled by the Forge interaction event and
 * WrenchItem; this only prevents a managed target from reaching BlockState.use
 * when removal validation fails.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateUseMixin {
    @Inject(method = "m_60664_(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"), cancellable = true)
    private void icecore$blockManagedWrenchUse(Level level, Player player,
                                                InteractionHand hand, BlockHitResult hit,
                                                CallbackInfoReturnable<InteractionResult> callback) {
        if (BuildingServerActions.shouldBlockVanillaBlockUse(level, player, hand, hit)) {
            callback.setReturnValue(InteractionResult.PASS);
        }
    }
}
