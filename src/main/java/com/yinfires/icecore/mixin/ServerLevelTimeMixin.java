package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.dewdrop.DewDropFarmlandTimeCompat;
import com.yinfires.icecore.time.TimeService;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelTimeMixin {
    @Redirect(
            method = "m_8809_()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;m_8615_(J)V"),
            require = 1
    )
    private void icecore$scaleDayTime(ServerLevel level,long vanillaValue){TimeService.advanceNatural(level);}

    @Inject(method = "m_8615_(J)V", at = @At("HEAD"))
    private void icecore$observeExternalSet(long value, CallbackInfo ci){
        ServerLevel level=(ServerLevel)(Object)this;
        DewDropFarmlandTimeCompat.observeDayTimeChange(level,level.getDayTime(),value);
        TimeService.beforeDayTimeSet();
    }
}
