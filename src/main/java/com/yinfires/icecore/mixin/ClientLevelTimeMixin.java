package com.yinfires.icecore.mixin;

import com.yinfires.icecore.time.TimeClientState;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientLevel.class)
public abstract class ClientLevelTimeMixin {
    private double icecore$fraction;

    @Redirect(
            method = "m_104826_()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;m_104746_(J)V"),
            require = 1
    )
    private void icecore$scaleVisualTime(ClientLevel level,long vanillaValue){
        if(level.dimension()!=net.minecraft.world.level.Level.OVERWORLD||!TimeClientState.initialized()||!TimeClientState.daylightCycle()){level.setDayTime(vanillaValue);return;}
        icecore$fraction+=1.0D/TimeClientState.config().dayDurationMultiplier();long whole=(long)Math.floor(icecore$fraction);icecore$fraction-=whole;level.setDayTime(level.getDayTime()+whole);
    }
}
