package com.yinfires.icecore.mixin;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.StockpotBlockEntity;
import com.yinfires.icecore.workstation.*;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.StockpotBlockEntity", remap = false)
public abstract class WorkstationStockpotProductSyncMixin implements StockpotProductView {
    @Unique private Boolean icecore$carrier;
    public Boolean icecore$needsCarrier() { return icecore$carrier; }
    @Inject(method = "m_183515_(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void icecore$saveCarrier(CompoundTag tag, CallbackInfo ci) {
        var self = (StockpotBlockEntity)(Object)this;
        if (self.getLevel() != null && !self.getLevel().isClientSide && self.getStatus() == 3)
            tag.putBoolean("ICEcoreProductCarrier", !StockpotProducts.carrier(self).isEmpty());
    }
    @Inject(method = "m_142466_(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void icecore$loadCarrier(CompoundTag tag, CallbackInfo ci) {
        icecore$carrier = tag.contains("ICEcoreProductCarrier") ? tag.getBoolean("ICEcoreProductCarrier") : null;
    }
}
