package com.yinfires.icecore.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

/** The shared real tank projection already supplies Jade's standard fluid bar. */
@Mixin(targets="com.bmt.kaleidoscope_world_liquor.compat.jade.FreezerFluidProviders$TooltipProvider",remap=false)
public abstract class WorkstationFreezerMilkJadeMixin {
    @Inject(method="appendTooltip(Lsnownee/jade/api/ITooltip;Lsnownee/jade/api/BlockAccessor;Lsnownee/jade/api/config/IPluginConfig;)V",at=@At("HEAD"),cancellable=true)
    private void icecore$standardTank(ITooltip tooltip,BlockAccessor accessor,IPluginConfig config,CallbackInfo ci) {
        ci.cancel();
    }
}
