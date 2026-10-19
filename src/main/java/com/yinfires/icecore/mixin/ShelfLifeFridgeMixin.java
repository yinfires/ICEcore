package com.yinfires.icecore.mixin;

import net.blay09.mods.cookingforblockheads.tile.FridgeBlockEntity;
import com.yinfires.icecore.spoilage.PreservedContainer;
import com.yinfires.icecore.fridge.FridgePairing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = FridgeBlockEntity.class, remap = false)
public abstract class ShelfLifeFridgeMixin {
    @Inject(method = "getCombinedContainer()Lnet/minecraft/world/Container;", at = @At("HEAD"))
    private void icecore$combined(CallbackInfoReturnable<net.minecraft.world.Container> cir) {
        FridgeBlockEntity self = (FridgeBlockEntity)(Object)this;
        self.getContainer();
        FridgeBlockEntity base = self.getBaseFridge();
        base.getContainer();
        FridgeBlockEntity neighbour = base.findNeighbourFridge();
        if (neighbour != null) neighbour.getContainer();
    }
    @Inject(method = "getContainer()Lnet/minecraft/world/Container;", at = @At("RETURN"))
    private void icecore$container(CallbackInfoReturnable<net.minecraft.world.Container> cir) {
        ((PreservedContainer)cir.getReturnValue()).icecore$preserve(FridgePairing.integrated(((FridgeBlockEntity)(Object)this).getBlockState()));
    }
    @Inject(method = "serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("HEAD"))
    private void icecore$tick(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                              net.minecraft.world.level.block.state.BlockState state, CallbackInfo ci) {
        // Bind on load and date changes; slot reads/writes handle changes within the day.
        long day = com.yinfires.icecore.spoilage.ShelfLife.day();
        if (!icecore$bound || day != icecore$day) {
            net.minecraft.world.Container container = ((FridgeBlockEntity)(Object)this).getContainer();
            if (FridgePairing.integrated(state)) {
                for (int i = 0; i < container.getContainerSize(); i++)
                    com.yinfires.icecore.spoilage.ShelfLife.freeze(container.getItem(i));
            }
            icecore$bound = true;
            icecore$day = day;
        }
    }
    @org.spongepowered.asm.mixin.Unique private boolean icecore$bound;
    @org.spongepowered.asm.mixin.Unique private long icecore$day;
}
