package com.yinfires.icecore.mixin;

import com.yinfires.icecore.fridge.FridgePairing;
import net.blay09.mods.cookingforblockheads.block.FridgeBlock;
import net.blay09.mods.cookingforblockheads.tile.FridgeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FridgeBlockEntity.class, remap = false)
public abstract class FridgePairingMixin implements com.yinfires.icecore.mixing.ContainerContentAccess {
    public boolean icecore$hasContainerContent() {
        FridgeBlockEntity self = (FridgeBlockEntity) (Object) this;
        if (!FridgePairing.integrated(self.getBlockState())) return false;
        if (self.hasIceUpgrade || self.hasPreservationUpgrade) return true;
        return !self.getContainer().isEmpty();
    }

    public int icecore$contentFingerprint() {
        FridgeBlockEntity self = (FridgeBlockEntity) (Object) this;
        int hash = 31 * Boolean.hashCode(self.hasIceUpgrade) + Boolean.hashCode(self.hasPreservationUpgrade);
        net.minecraft.world.Container contents = self.getContainer();
        for (int slot = 0; slot < contents.getContainerSize(); slot++) {
            net.minecraft.world.item.ItemStack stack = contents.getItem(slot);
            hash = 31 * hash + (stack.isEmpty() ? 0 : stack.save(new net.minecraft.nbt.CompoundTag()).hashCode());
        }
        return hash;
    }
    @Inject(method = "findNeighbourFridge()Lnet/blay09/mods/cookingforblockheads/tile/FridgeBlockEntity;", at = @At("HEAD"), cancellable = true)
    private void icecore$neighbour(CallbackInfoReturnable<FridgeBlockEntity> cir) {
        FridgeBlockEntity self = (FridgeBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null) return;
        BlockState state = self.getBlockState();
        if (FridgePairing.integrated(state)) {
            BlockPos otherPos = FridgePairing.partnerPos(state, self.getBlockPos());
            cir.setReturnValue(FridgePairing.matches(state, level.getBlockState(otherPos))
                    && level.getBlockEntity(otherPos) instanceof FridgeBlockEntity other ? other : null);
        } else if (FridgePairing.integrated(level.getBlockState(self.getBlockPos().above()))
                || FridgePairing.integrated(level.getBlockState(self.getBlockPos().below()))) {
            // Preserve native above-first lookup, excluding only integrated fridges.
            for (BlockPos pos : new BlockPos[]{self.getBlockPos().above(), self.getBlockPos().below()}) {
                BlockState candidate = level.getBlockState(pos);
                if (candidate.getBlock() instanceof FridgeBlock && !FridgePairing.integrated(candidate)
                        && level.getBlockEntity(pos) instanceof FridgeBlockEntity other) {
                    cir.setReturnValue(other);
                    return;
                }
            }
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "getBaseFridge()Lnet/blay09/mods/cookingforblockheads/tile/FridgeBlockEntity;", at = @At("HEAD"), cancellable = true)
    private void icecore$base(CallbackInfoReturnable<FridgeBlockEntity> cir) {
        FridgeBlockEntity self = (FridgeBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null) return;
        BlockState state = self.getBlockState();
        if (FridgePairing.integrated(state)) {
            BlockPos below = self.getBlockPos().below();
            cir.setReturnValue(FridgePairing.upper(state) && FridgePairing.matches(state, level.getBlockState(below))
                    && level.getBlockEntity(below) instanceof FridgeBlockEntity base ? base : self);
        } else if (FridgePairing.integrated(level.getBlockState(self.getBlockPos().below()))) {
            cir.setReturnValue(self);
        }
    }

    @Inject(method = "getDefaultName()Lnet/minecraft/network/chat/Component;", at = @At("HEAD"), cancellable = true)
    private void icecore$name(CallbackInfoReturnable<Component> cir) {
        if (FridgePairing.integrated(((FridgeBlockEntity) (Object) this).getBlockState())) {
            cir.setReturnValue(Component.translatable("block.icecore.fridge"));
        }
    }
}
