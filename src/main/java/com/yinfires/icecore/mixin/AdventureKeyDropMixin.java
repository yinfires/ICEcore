package com.yinfires.icecore.mixin;

import com.yinfires.icecore.adventure.AdventureItemEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Consumes the vanilla drop action before it can play the hand swing. */
@Mixin(Minecraft.class)
public abstract class AdventureKeyDropMixin {
    @Redirect(method = "m_91279_()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;m_108700_(Z)Z"))
    private boolean icecore$handleAdventureDrop(LocalPlayer player, boolean dropAll) {
        Minecraft minecraft = (Minecraft) (Object) this;
        if (minecraft.gameMode != null
                && minecraft.gameMode.getPlayerMode() == net.minecraft.world.level.GameType.ADVENTURE) {
            AdventureItemEvents.handleDropKey(minecraft, dropAll);
            return false;
        }
        return player.drop(dropAll);
    }
}
