package com.yinfires.icecore.mixin;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(targets="dev.xkmc.youkaishomecoming.content.pot.table.item.FoodTableItem",remap=false)
public abstract class WorkstationCuisineRoeModelMixin {
    @Shadow @Final private ResourceLocation baseId;
    @Inject(method="getModels()Ljava/util/List;",at=@At("RETURN"),cancellable=true)
    private void icecore$roeCover(CallbackInfoReturnable<List<ResourceLocation>> cir) {
        if(!baseId.equals(ResourceLocation.parse("youkaisfeasts:roe_california_roll")))return;
        var models=new java.util.ArrayList<>(cir.getReturnValue());
        models.add(ResourceLocation.parse("youkaisfeasts:cuisine/california/cover/crab_roe/0"));
        cir.setReturnValue(models);
    }
}
