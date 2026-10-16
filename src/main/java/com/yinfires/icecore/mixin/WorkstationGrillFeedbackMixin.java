package com.yinfires.icecore.mixin;

import com.yinfires.icecore.feedback.PlayerFeedback;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(targets = "cn.breezeth.kaleidoscope_grilling.grill.GrillBlock", remap = false)
public abstract class WorkstationGrillFeedbackMixin {
    @Redirect(method = "m_6227_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_5661_(Lnet/minecraft/network/chat/Component;Z)V"))
    private void icecore$jadeState(Player player, Component message, boolean overlay) {
        if (message.getContents() instanceof TranslatableContents content && content.getKey().contains("grill_")) return;
        if (player instanceof ServerPlayer server) PlayerFeedback.show(server, message);
    }
}
