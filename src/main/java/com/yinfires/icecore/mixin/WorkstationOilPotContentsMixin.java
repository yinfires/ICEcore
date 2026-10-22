package com.yinfires.icecore.mixin;

import com.yinfires.icecore.tooltip.PreservedTooltipContents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import java.util.List;

@Mixin(targets="com.github.ysbbbbbb.kaleidoscopecookery.item.OilPotItem",remap=false)
public abstract class WorkstationOilPotContentsMixin {
    @Redirect(method="m_7373_(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V",at=@At(value="INVOKE",target="Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private boolean icecore$contents(List<Component> lines,Object component){return lines.add(PreservedTooltipContents.wrap((Component)component));}
}
