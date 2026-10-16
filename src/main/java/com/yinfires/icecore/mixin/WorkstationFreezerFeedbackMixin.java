package com.yinfires.icecore.mixin;

import com.yinfires.icecore.feedback.PlayerFeedback;
import net.minecraft.network.chat.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(targets = "com.bmt.kaleidoscope_world_liquor.block.FreezerBlock", remap = false)
public abstract class WorkstationFreezerFeedbackMixin {
    @Redirect(method = "m_6227_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_5661_(Lnet/minecraft/network/chat/Component;Z)V"))
    private void icecore$noStartMessage(Player player, Component text, boolean overlay) {
        if (text.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents content
                && (content.getKey().endsWith(".start_crafting") || content.getKey().endsWith(".remaining_time"))) return;
        if (player instanceof ServerPlayer server) PlayerFeedback.show(server, text);
    }
}
