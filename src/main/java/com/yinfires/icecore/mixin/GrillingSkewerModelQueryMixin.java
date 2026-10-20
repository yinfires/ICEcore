package com.yinfires.icecore.mixin;

import cn.breezeth.kaleidoscope_grilling.data.GrillingDataManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="cn.breezeth.kaleidoscope_grilling.skewer.SkewerRecipes",remap=false)
public abstract class GrillingSkewerModelQueryMixin {
    @Unique private static final com.yinfires.icecore.compat.render.GenerationResults<Item,Boolean> icecore$results=new com.yinfires.icecore.compat.render.GenerationResults<>();
    @Unique private static final ThreadLocal<Object> icecore$pending=new ThreadLocal<>();
    @Inject(method="usesGeneratedModel(Lnet/minecraft/world/item/ItemStack;)Z",at=@At("HEAD"),cancellable=true,remap=false)
    private static void icecore$lookup(ItemStack stack,CallbackInfoReturnable<Boolean> cir) {
        icecore$pending.remove();
        if(!com.yinfires.icecore.compat.render.RenderDiagnostics.queryCache) return;
        Object source=GrillingDataManager.skewersForDisplay();
        Boolean value=icecore$results.lookup(source,stack.getItem());
        if(value!=null) { cir.setReturnValue(value);return; }
        icecore$pending.set(source);
    }
    @Inject(method="usesGeneratedModel(Lnet/minecraft/world/item/ItemStack;)Z",at=@At("RETURN"),remap=false)
    private static void icecore$remember(ItemStack stack,CallbackInfoReturnable<Boolean> cir) {
        Object source=icecore$pending.get();icecore$pending.remove();
        if(source==null || source!=GrillingDataManager.skewersForDisplay()) return;
        icecore$results.remember(source,GrillingDataManager.skewersForDisplay(),stack.getItem(),cir.getReturnValue());
    }
}
