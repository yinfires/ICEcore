package com.yinfires.icecore.mixin;

import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.deco.TextBlockEntity;
import com.yinfires.icecore.compat.cozycafe.board.CozyCafeBoardService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopetavern.blockentity.deco.TextBlockEntity", remap = false)
public abstract class TavernTextBlockEntityMixin {
    @Inject(method = "onItemUse(Lnet/minecraft/world/level/Level;Lcom/github/ysbbbbbb/kaleidoscopetavern/blockentity/deco/TextBlockEntity;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void icecore$adventureLock(Level level, TextBlockEntity board, Player player, InteractionHand hand,
                                              CallbackInfoReturnable<InteractionResult> cir) {
        if (!player.getAbilities().mayBuild) cir.setReturnValue(InteractionResult.FAIL);
    }

}
