package com.yinfires.icecore.mixin;

import com.yinfires.icecore.workstation.WorkstationAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(BlockEntity.class)
public abstract class WorkstationMetadataMixin {
    @Inject(method = {"m_187482_()Lnet/minecraft/nbt/CompoundTag;", "m_187480_()Lnet/minecraft/nbt/CompoundTag;"}, at = @At("RETURN"))
    private void icecore$save(CallbackInfoReturnable<CompoundTag> cir) {
        if ((Object)this instanceof WorkstationAccess access) access.icecore$slots().save(cir.getReturnValue());
    }
    @Inject(method = "m_142466_(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("HEAD"))
    private void icecore$load(CompoundTag tag, CallbackInfo ci) {
        if ((Object)this instanceof WorkstationAccess access) access.icecore$slots().load(tag);
    }
}
