package com.yinfires.icecore.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Map;

/** Data-pack aliases reuse only model variants already registered by the native table. */
@Mixin(targets="dev.xkmc.youkaishomecoming.content.pot.table.model.VariantModelPart",remap=false)
public abstract class WorkstationCuisineModelAliasMixin {
    @Shadow @Final public Map<String, ?> ingredients;
    @Unique private final Map<String, TagKey<net.minecraft.world.item.Item>> icecore$aliases=new java.util.HashMap<>();

    @Inject(method="find(Lnet/minecraft/world/item/ItemStack;)Ljava/lang/String;",at=@At("RETURN"),cancellable=true)
    private void icecore$modelAlias(ItemStack stack,CallbackInfoReturnable<String> cir) {
        if(cir.getReturnValue()!=null)return;
        for(String variant:ingredients.keySet()) {
            var tag=icecore$aliases.computeIfAbsent(variant,key->TagKey.create(Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("icecore","cuisine_model_alias/"+key)));
            if(stack.is(tag)) {cir.setReturnValue(variant);return;}
        }
    }
}
