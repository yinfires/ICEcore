package com.yinfires.icecore.mixin;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.yinfires.icecore.compat.geckolib.GeckoKeyframeCompat;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.loading.json.typeadapter.BakedAnimationsAdapter", remap = false)
public abstract class GeckoLibKeyframeMixin {
    @Inject(method = "getTripletObj(Lcom/google/gson/JsonElement;)Ljava/util/List;",
            at = @At("HEAD"), cancellable = true, require = 0)
    private static void icecore$preserveKeyframeObjects(JsonElement channel,
            CallbackInfoReturnable<List<Pair<String, JsonElement>>> callback) {
        List<Pair<String, JsonElement>> result = GeckoKeyframeCompat.convertChannel(channel);
        if (result != null) callback.setReturnValue(result);
    }
}
