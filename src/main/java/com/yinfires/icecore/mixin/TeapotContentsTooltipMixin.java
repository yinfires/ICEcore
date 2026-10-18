package com.yinfires.icecore.mixin;

import com.yinfires.icecore.tooltip.PreservedTooltipContents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.List;

/** Both native append sites are dynamic contents (fluid or finished tea and count). */
@Pseudo
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.item.TeapotItem", remap = false)
public abstract class TeapotContentsTooltipMixin {
    @Redirect(method = "m_7373_(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"), require = 2, allow = 2, remap = false)
    private boolean icecore$preserveContents(List<Component> lines, Object line) {
        return lines.add(PreservedTooltipContents.wrap((Component) line));
    }
}
