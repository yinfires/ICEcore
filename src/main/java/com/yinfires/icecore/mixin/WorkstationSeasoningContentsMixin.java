package com.yinfires.icecore.mixin;

import com.yinfires.icecore.tooltip.PreservedTooltipContents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.List;

/** Preserve native remaining charges when static descriptions are hidden. */
@Mixin(targets="cn.breezeth.kaleidoscope_grilling.seasoning.SeasoningTooltip",remap=false)
public abstract class WorkstationSeasoningContentsMixin {
    @Redirect(method="appendFinished(Lnet/minecraft/world/item/ItemStack;Ljava/util/List;)V",
        at=@At(value="INVOKE",target="Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private static boolean icecore$charges(List<Component> lines,Object value) {
        Component component=(Component)value;
        if(component.getContents() instanceof TranslatableContents text
                && text.getKey().equals("tooltip.kaleidoscope_grilling.seasoning.uses_full"))
            component=PreservedTooltipContents.wrap(component);
        return lines.add(component);
    }
}
