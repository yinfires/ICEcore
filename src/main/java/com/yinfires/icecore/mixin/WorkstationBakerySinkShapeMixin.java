package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.bakery.KitchenConnections;
import com.yinfires.icecore.compat.bakery.KitchenSinkShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.satisfy.farm_and_charm.core.block.SinkBlock", remap = false)
public abstract class WorkstationBakerySinkShapeMixin {
    @Inject(method = "m_5940_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("RETURN"), cancellable = true)
    private void icecore$fillBase(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context,
            CallbackInfoReturnable<VoxelShape> cir) {
        if (!KitchenConnections.available() || state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != DoubleBlockHalf.LOWER
                || !KitchenConnections.SINK.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) return;
        cir.setReturnValue(KitchenSinkShapes.extend(cir.getReturnValue(), state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }
}
