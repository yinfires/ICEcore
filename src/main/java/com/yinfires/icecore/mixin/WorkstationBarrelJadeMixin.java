package com.yinfires.icecore.mixin;
import net.minecraft.network.chat.Component;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopetavern.compat.jade.block.BarrelComponentProvider", remap = false)
public abstract class WorkstationBarrelJadeMixin {
    @Redirect(method = "appendTooltip(Lsnownee/jade/api/ITooltip;Lsnownee/jade/api/BlockAccessor;Lsnownee/jade/api/config/IPluginConfig;)V", at = @At(value = "INVOKE", target = "Lsnownee/jade/api/ITooltip;add(Lnet/minecraft/network/chat/Component;)V"))
    private void icecore$onlyState(ITooltip tooltip, Component text) {
        if (text.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translated
                && translated.getKey().equals("jade.plugin_kaleidoscope_tavern.item_and_count")) return;
        tooltip.add(text);
    }
    public int getDefaultPriority() { return 1200; }
}
