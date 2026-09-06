package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.cozycafe.seating.CozyCafeSeatingService;
import io.github.chakyl.cozycafe.blockentities.CafeManagerBlockEntity;
import io.github.chakyl.cozycafe.blockentities.CafeMenuBlockEntity;
import io.github.chakyl.cozycafe.entities.CustomerEntity;
import io.github.chakyl.cozycafe.util.CustomerEntityUtils;
import io.github.chakyl.cozycafe.util.CustomerTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.yinfires.icecore.compat.cozycafe.spawn.CozyCafeSpawnRegionService;

@Pseudo
@Mixin(targets = "io.github.chakyl.cozycafe.blockentities.CafeManagerBlockEntity", remap = false)
public abstract class CozyCafeManagerSeatingMixin extends BlockEntity {
    @Shadow private int attemptedCustomers;
    @Shadow private BlockPos getFirstPos(BlockState state, BlockPos pos) { throw new AssertionError(); }
    @Shadow private BlockPos getSecondPos(BlockState state, BlockPos pos) { throw new AssertionError(); }

    protected CozyCafeManagerSeatingMixin(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "canBeOpened(Lnet/minecraft/server/level/ServerPlayer;)Z", at = @At("RETURN"), cancellable = true, remap = false)
    private void icecore$requireValidMenuAndSeat(ServerPlayer player, CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValueZ() || !(level instanceof ServerLevel serverLevel)) return;
        CafeManagerBlockEntity manager = (CafeManagerBlockEntity) (Object) this;
        java.util.List<BlockPos> entrances = CozyCafeSpawnRegionService.entranceCandidates(serverLevel, worldPosition);
        if (entrances.isEmpty()) entrances = CozyCafeSeatingService.defaultEntrance(manager);
        if (!CozyCafeSeatingService.buildRouteSnapshot(manager,
                getFirstPos(getBlockState(), worldPosition), getSecondPos(getBlockState(), worldPosition), entrances)) {
            io.github.chakyl.cozycafe.network.EvilPacketsIHateThem.sendToPlayer(
                    new io.github.chakyl.cozycafe.network.ClientBoundCafeCannotOpenPacket((byte) 5), player);
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "setOpen(ZZ)V", at = @At("RETURN"), remap = false)
    private void icecore$clearRoutesWhenClosed(boolean value, boolean forceClose, CallbackInfo callback) {
        if (!value && level instanceof ServerLevel serverLevel) {
            CozyCafeSeatingService.clearRouteSnapshot(serverLevel, worldPosition);
        }
    }

    @Redirect(method = "assignCustomersInArea(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At(value = "INVOKE", target = "Lio/github/chakyl/cozycafe/util/CustomerEntityUtils;spawnCustomerAndTarget(Lnet/minecraft/core/BlockPos;Lnet/minecraft/server/level/ServerLevel;Ljava/lang/String;Lnet/minecraft/core/BlockPos;Lio/github/chakyl/cozycafe/entities/CustomerEntity;Lio/github/chakyl/cozycafe/util/CustomerTarget;)V"), remap = false)
    private void icecore$reserveBeforeSpawn(BlockPos spawn, ServerLevel serverLevel, String skin, BlockPos menuPos,
                                            CustomerEntity customer, CustomerTarget target) {
        BlockEntity blockEntity = serverLevel.getBlockEntity(menuPos);
        if (blockEntity instanceof CafeMenuBlockEntity menu) {
            BlockPos routeSpawn = CozyCafeSeatingService.routeEntrance(menu, serverLevel.random);
            if (routeSpawn != null && CozyCafeSeatingService.reserveIfReachable(menu, customer, routeSpawn)) {
                CustomerEntityUtils.spawnCustomerAndTarget(routeSpawn, serverLevel, skin, menuPos, customer, target);
                return;
            }
            menu.setCustomerTravelTime(-1);
            if (io.github.chakyl.cozycafe.CozyCafe.CONFIG.dailyLimit.get()) attemptedCustomers--;
        }
    }

    @Inject(method = "assignCustomersInArea(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void icecore$restoreRoutesAfterLoad(Level currentLevel, BlockPos pos, BlockState state, CallbackInfo callback) {
        if (!(currentLevel instanceof ServerLevel serverLevel)
                || CozyCafeSeatingService.hasRouteSnapshot(serverLevel, worldPosition)) return;
        CafeManagerBlockEntity manager = (CafeManagerBlockEntity) (Object) this;
        java.util.List<BlockPos> entrances = CozyCafeSpawnRegionService.entranceCandidates(serverLevel, worldPosition);
        if (entrances.isEmpty()) entrances = CozyCafeSeatingService.defaultEntrance(manager);
        if (!CozyCafeSeatingService.buildRouteSnapshot(manager,
                getFirstPos(state, pos), getSecondPos(state, pos), entrances)) {
            callback.cancel();
        }
    }
}
