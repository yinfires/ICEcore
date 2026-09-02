package com.yinfires.icecore.mixin;

import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.deco.TextBlockEntity;
import com.yinfires.icecore.compat.cozycafe.board.CozyCafeBoardService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopetavern.network.message.TextUpdateC2SMessage", remap = false)
public abstract class TavernTextUpdateMixin {
    @Inject(method = "onHandle(Lnet/minecraftforge/network/NetworkEvent$Context;Lcom/github/ysbbbbbb/kaleidoscopetavern/network/message/TextUpdateC2SMessage;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void icecore$blockAdventureUpdate(NetworkEvent.Context context,
                                                     com.github.ysbbbbbb.kaleidoscopetavern.network.message.TextUpdateC2SMessage message,
                                                     CallbackInfo ci) {
        ServerPlayer player = context.getSender();
        if (player != null && !player.getAbilities().mayBuild) ci.cancel();
    }

    @Inject(method = "onHandle(Lnet/minecraftforge/network/NetworkEvent$Context;Lcom/github/ysbbbbbb/kaleidoscopetavern/network/message/TextUpdateC2SMessage;)V",
            at = @At(value = "INVOKE", target = "Lcom/github/ysbbbbbb/kaleidoscopetavern/blockentity/deco/TextBlockEntity;setText(Ljava/lang/String;)V"), remap = false)
    private static void icecore$unbindOnManualText(NetworkEvent.Context context,
                                                   com.github.ysbbbbbb.kaleidoscopetavern.network.message.TextUpdateC2SMessage message,
                                                   CallbackInfo ci) {
        ServerPlayer player = context.getSender();
        if (player != null && player.level().getBlockEntity(message.pos()) instanceof TextBlockEntity board) {
            CozyCafeBoardService.unbind(board);
        }
    }
}
