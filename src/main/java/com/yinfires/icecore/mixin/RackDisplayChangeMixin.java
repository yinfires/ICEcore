package com.yinfires.icecore.mixin;

import com.yinfires.icecore.shelf.client.RackDisplayCache;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.xkmc.youkaishomecoming.content.pot.storage.ingredient.IngredientRackBlockEntity", remap = false)
public abstract class RackDisplayChangeMixin {
    @Inject(method = "notifyTile()V", at = @At("HEAD"))
    private void icecore$changed(CallbackInfo ci) { RackDisplayCache.invalidate((BlockEntity)(Object)this); }
}
