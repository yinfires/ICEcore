package com.yinfires.icecore.mixin;

import io.github.chakyl.cozycafe.entities.CustomerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives entering and leaving customers the same bounded wooden-door interaction. */
@Pseudo
@Mixin(targets = "io.github.chakyl.cozycafe.entities.CustomerEntity", remap = false)
public abstract class CozyCafeCustomerDoorMixin {
    @Inject(method = "m_8119_()V", at = @At("TAIL"), remap = false)
    private void icecore$openRouteDoor(CallbackInfo ci) {
        CustomerEntity customer = (CustomerEntity) (Object) this;
        if (customer.level().isClientSide || customer.tickCount % 5 != 0) return;
        BlockPos target = customer.getTargetMenuPos() != null ? customer.getTargetMenuPos() : customer.getTargetSignPos();
        if (target == null) return;
        BlockPos center = customer.blockPosition();
        double currentDistance = center.distSqr(target);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 1, 1))) {
            if (pos.distSqr(target) > currentDistance + 2.0D) continue;
            BlockState state = customer.level().getBlockState(pos);
            if (!state.hasProperty(BlockStateProperties.OPEN) || state.getValue(BlockStateProperties.OPEN)) continue;
            if (state.getBlock() instanceof DoorBlock && state.is(BlockTags.WOODEN_DOORS)) {
                customer.level().setBlock(pos, state.setValue(BlockStateProperties.OPEN, true), 10);
                com.yinfires.icecore.compat.cozycafe.seating.CozyCafeSeatingService.requestPathRecovery(customer);
                return;
            }
            if (state.getBlock() instanceof FenceGateBlock) {
                customer.level().setBlock(pos, state.setValue(BlockStateProperties.OPEN, true), 10);
                com.yinfires.icecore.compat.cozycafe.seating.CozyCafeSeatingService.requestPathRecovery(customer);
                return;
            }
        }
    }
}
