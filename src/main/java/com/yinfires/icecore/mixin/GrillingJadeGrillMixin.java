package com.yinfires.icecore.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import snownee.jade.api.ITooltip;

/** Removes only the upstream icons appended to the block-name line. */
@Mixin(targets = "cn.breezeth.kaleidoscope_grilling.compat.jade.GrillProvider", remap = false)
public abstract class GrillingJadeGrillMixin {
    @Redirect(method = "appendTooltip(Lsnownee/jade/api/ITooltip;Lsnownee/jade/api/BlockAccessor;Lsnownee/jade/api/config/IPluginConfig;)V",
            at = @At(value = "INVOKE", target = "Lcn/breezeth/kaleidoscope_grilling/compat/jade/JadeElements;appendItems(Lsnownee/jade/api/ITooltip;Ljava/lang/Iterable;)V",
                    remap = false), remap = false)
    private void icecore$omitTitleSkewers(ITooltip tooltip, Iterable<ItemStack> stacks) {
        // Jade's inventory provider already lists the contents below the grill status.
    }
}
