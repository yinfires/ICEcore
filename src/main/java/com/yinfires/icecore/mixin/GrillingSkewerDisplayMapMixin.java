package com.yinfires.icecore.mixin;

import java.util.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The upstream fields are immutable maps replaced on reload, script commit and sync. */
@Mixin(targets="cn.breezeth.kaleidoscope_grilling.data.GrillingDataManager",remap=false)
public abstract class GrillingSkewerDisplayMapMixin {
    @Shadow @Final private static Map<String,?> BUILT_IN_SKEWERS;
    @Shadow private static volatile Map<String,?> SKEWERS;
    @Shadow private static volatile Map<String,?> SCRIPT_SKEWERS;
    @Unique private static volatile com.yinfires.icecore.compat.render.DisplayMapSnapshot icecore$snapshot;
    @Inject(method="skewersForDisplay()Ljava/util/Map;",at=@At("HEAD"),cancellable=true,remap=false)
    private static void icecore$displaySnapshot(CallbackInfoReturnable<Map<String,?>> cir) {
        if(!com.yinfires.icecore.compat.render.RenderDiagnostics.queryCache) return;
        var data=SKEWERS;var scripts=SCRIPT_SKEWERS;var current=icecore$snapshot;
        if(current==null || current.data()!=data || current.scripts()!=scripts) {
            Map<String,Object> merged=new HashMap<>(BUILT_IN_SKEWERS);
            merged.putAll(data);merged.putAll(scripts);
            current=new com.yinfires.icecore.compat.render.DisplayMapSnapshot(data,scripts,Map.copyOf(merged));icecore$snapshot=current;
        }
        cir.setReturnValue(current.merged());
    }
}
