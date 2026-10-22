package com.yinfires.icecore.mixin;

import com.yinfires.icecore.workstation.ExternalWorkstation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.*;

/** Include data-pack entries in the native login/reload packet used by JEI and item models. */
@Mixin(targets="cn.breezeth.kaleidoscope_grilling.data.GrillingDataManager",remap=false)
public abstract class WorkstationSkewerSyncMixin {
    @Inject(method="scriptSkewerRecipes()Ljava/util/List;",at=@At("RETURN"),cancellable=true)
    private static void icecore$dataPackRecipes(CallbackInfoReturnable<List<Object>> cir) {
        String owner="cn.breezeth.kaleidoscope_grilling.data.GrillingDataManager";
        @SuppressWarnings("unchecked")
        Map<String,Object> loaded=(Map<String,Object>)ExternalWorkstation.invokeStatic(owner,"skewers",new Class<?>[0]);
        Map<String,Object> packet=new TreeMap<>();
        for(Object entry:cir.getReturnValue())packet.put((String)ExternalWorkstation.call(entry,"id"),entry);
        for(var entry:loaded.entrySet())if(!packet.containsKey(entry.getKey())) {
            Object value=entry.getValue();
            packet.put(entry.getKey(),icecore$packet(entry.getKey(),value));
        }
        cir.setReturnValue(List.copyOf(packet.values()));
    }
    private static Object icecore$packet(String id,Object value) {
        try {
            var type=Class.forName("cn.breezeth.kaleidoscope_grilling.data.GrillingDataManager$ScriptSkewerRecipe");
            return type.getConstructor(String.class,List.class,String.class,String.class,String.class,int.class,String.class,String.class,String.class)
                .newInstance(id,ExternalWorkstation.call(value,"ingredients"),ExternalWorkstation.call(value,"cookedResult"),
                    ExternalWorkstation.call(value,"threadingResult"),ExternalWorkstation.call(value,"effect"),
                    ExternalWorkstation.call(value,"effectSeconds"),ExternalWorkstation.call(value,"rawModel"),
                    ExternalWorkstation.call(value,"cookedModel"),ExternalWorkstation.call(value,"eatingAnimation"));
        } catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
}
