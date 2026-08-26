package com.yinfires.icecore.mixin;

import com.yinfires.icecore.adventure.AdventureItemService;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class AdventureLocalPlayerDropMixin {
    @Inject(method="m_108700_(Z)Z", at=@At("HEAD"), cancellable=true)
    private void icecore$denyAdventureClientDrop(boolean dropAll, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (Minecraft.getInstance().gameMode != null
                && Minecraft.getInstance().gameMode.getPlayerMode() == net.minecraft.world.level.GameType.ADVENTURE) {
            cir.setReturnValue(false);
        }
    }
}
