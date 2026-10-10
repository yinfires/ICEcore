package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.dewdrop.DewDropIrrigation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "cool.bot.dewdropwateringcans.item.wateringCan.WateringCanItem", remap = false)
public abstract class DewDropIrrigationWateringCanMixin {
    private static final String USE = "m_7203_(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;";

    @Redirect(method = USE, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_6047_()Z"), remap = false)
    private boolean icecore$singleCell(Player player) {
        return false;
    }

    @Redirect(method = USE, at = @At(value = "INVOKE", target = "Lcool/bot/botslib/util/Util;blockHighlightedOrNull(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/core/BlockPos;"), remap = false)
    private BlockPos icecore$cropSoil(Player player) {
        BlockPos hit = cool.bot.botslib.util.Util.blockHighlightedOrNull(player);
        BlockPos soil = DewDropIrrigation.soil(player.level(), hit);
        return soil == null ? hit : soil;
    }
}
