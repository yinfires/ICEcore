package com.yinfires.icecore.mixin;

import com.yinfires.icecore.shelf.client.RackDisplayCache;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.xkmc.l2library.base.tile.BaseBlockEntity", remap = false)
public abstract class RackDisplayLoadMixin {
    @Inject(method = "m_142466_(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void icecore$load(CompoundTag tag,CallbackInfo ci) { RackDisplayCache.invalidate((BlockEntity)(Object)this); }
}
