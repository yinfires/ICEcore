package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.cozycafe.spawn.CozyCafeSpawnRegionService;
import io.github.chakyl.cozycafe.blockentities.CafeManagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Chooses a fresh region exit when a table creates its departing customer. */
@Pseudo
@Mixin(targets = "io.github.chakyl.cozycafe.blockentities.CafeMenuBlockEntity", remap = false)
public abstract class CozyCafeSpawnRegionMenuMixin extends BlockEntity {
    protected CozyCafeSpawnRegionMenuMixin(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos,
                                            net.minecraft.world.level.block.state.BlockState state) { super(type, pos, state); }

    @Redirect(method = "closeMenu(Z)V", at = @At(value = "INVOKE",
            target = "Lio/github/chakyl/cozycafe/blockentities/CafeManagerBlockEntity;getLinkedSign()Lnet/minecraft/core/BlockPos;"), remap = false)
    private BlockPos icecore$regionExit(CafeManagerBlockEntity manager) {
        BlockPos fallback = manager.getLinkedSign();
        return level instanceof ServerLevel serverLevel
                ? CozyCafeSpawnRegionService.exitSignPosition(serverLevel, manager.getBlockPos(), fallback)
                : fallback;
    }
}
