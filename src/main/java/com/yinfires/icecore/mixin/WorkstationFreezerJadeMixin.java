package com.yinfires.icecore.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

@Mixin(targets = "com.bmt.kaleidoscope_world_liquor.compat.jade.block.FreezerComponentProvider", remap = false)
public abstract class WorkstationFreezerJadeMixin {
    @Inject(method = "appendTooltip(Lsnownee/jade/api/ITooltip;Lsnownee/jade/api/BlockAccessor;Lsnownee/jade/api/config/IPluginConfig;)V", at = @At("HEAD"), cancellable = true)
    private void icecore$oneTimer(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config, CallbackInfo ci) { ci.cancel(); }
}
