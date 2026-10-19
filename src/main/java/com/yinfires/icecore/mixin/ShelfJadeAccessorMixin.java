package com.yinfires.icecore.mixin;

import com.yinfires.icecore.shelf.ShelfBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Feed the same native container collector from either half. No alternate item UI. */
@Pseudo
@Mixin(targets="snownee.jade.impl.BlockAccessorImpl",remap=false)
public abstract class ShelfJadeAccessorMixin {
    @Inject(method="getBlockEntity",at=@At("HEAD"),cancellable=true,remap=false)
    private void icecore$base(CallbackInfoReturnable<BlockEntity> ci) {
        BlockAccessor accessor=(BlockAccessor)(Object)this;
        if(accessor.getBlockState().getBlock() instanceof ShelfBlock)
            ci.setReturnValue(accessor.getLevel().getBlockEntity(ShelfBlock.base(accessor.getBlockState(),accessor.getPosition())));
    }
}
