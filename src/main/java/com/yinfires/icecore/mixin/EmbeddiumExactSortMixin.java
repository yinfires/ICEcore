package com.yinfires.icecore.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.yinfires.icecore.shelf.client.ShelfSortCache;

@Mixin(targets="me.jellysquid.mods.sodium.client.util.sorting.MergeSort",remap=false)
public abstract class EmbeddiumExactSortMixin {
    @Inject(method="mergeSort([F)[I",at=@At("HEAD"),cancellable=true,remap=false)
    private static void icecore$lookup(float[] keys,CallbackInfoReturnable<int[]> cir) {
        int[] order=ShelfSortCache.lookup(keys);if(order!=null) cir.setReturnValue(order);
    }
    @Inject(method="mergeSort([F)[I",at=@At("RETURN"),remap=false)
    private static void icecore$remember(float[] keys,CallbackInfoReturnable<int[]> cir) {
        ShelfSortCache.remember(keys,cir.getReturnValue());
    }
}
