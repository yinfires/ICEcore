package com.yinfires.icecore.mixin;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import snownee.jade.api.Identifiers;
import snownee.jade.api.ui.IElement;
import snownee.jade.impl.Tooltip;
import com.yinfires.icecore.compat.jade.WoodTitleSeparator;
import com.yinfires.icecore.client.WoodPanelLayout;

/** Change native layout before Jade computes anchoring, scaling and the background rectangle. */
@Pseudo
@Mixin(targets="snownee.jade.overlay.TooltipRenderer",remap=false)
public abstract class WoodJadeLayoutMixin {
    @Shadow @Final private int[] padding;
    @Shadow @Final private Tooltip tooltip;
    @Unique private static final ResourceLocation ICECORE_LINE = ResourceLocation.fromNamespaceAndPath("icecore","title_separator");
    @Inject(method="recalculateSize",at=@At("HEAD"),remap=false)
    private void icecore$layout(CallbackInfo ci) {
        // The immutable frame occupies exactly six pixels on every side.
        java.util.Arrays.fill(padding,WoodPanelLayout.BORDER);
        tooltip.remove(ICECORE_LINE);
        for(int row=0;row<tooltip.size();row++) {
            boolean title=false;
            for(IElement.Align align:IElement.Align.values())
                for(IElement element:tooltip.get(row,align))
                    if(Identifiers.CORE_OBJECT_NAME.equals(element.getTag())) title=true;
            boolean body=false;
            if(title) for(int next=row+1;next<tooltip.size();next++)
                for(IElement.Align align:IElement.Align.values())
                    for(IElement element:tooltip.get(next,align))
                        if(!Identifiers.CORE_MOD_NAME.equals(element.getTag())
                                && !Identifiers.CORE_OBJECT_NAME.equals(element.getTag())
                                && !(element instanceof snownee.jade.impl.ui.SpacerElement)
                                && element.getCachedSize().x>0 && element.getCachedSize().y>0) body=true;
            if(title && body) {
                WoodTitleSeparator line=new WoodTitleSeparator();
                line.tag(ICECORE_LINE);
                tooltip.add(row+1,line);
                break;
            }
        }
    }
}
