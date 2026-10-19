package com.yinfires.icecore.mixin;

import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import snownee.jade.api.Identifiers;
import snownee.jade.api.ui.IElement;
import java.util.List;

/** Reposition the native title row; its elements still render through Jade. */
@Pseudo
@Mixin(targets="snownee.jade.impl.Tooltip$Line",remap=false)
public abstract class WoodJadeTitleMixin {
    @Shadow public abstract List<IElement> getAlignedElements(IElement.Align align);
    @ModifyVariable(method="render",at=@At("HEAD"),argsOnly=true,ordinal=0,remap=false)
    private float icecore$center(float value,GuiGraphics graphics,float x,float y,float right,float height) {
        List<IElement> elements=getAlignedElements(IElement.Align.LEFT);
        if(!getAlignedElements(IElement.Align.RIGHT).isEmpty()) return value;
        boolean title=false;
        float width=0;
        for(IElement element:elements) {
            title |= Identifiers.CORE_OBJECT_NAME.equals(element.getTag());
            width+=element.getCachedSize().x;
        }
        return title ? x+(float)Math.floor((right-x-width)/2) : value;
    }
}
