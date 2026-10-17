package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.cozycafe.CozyCafeAdmissionAccess;
import com.yinfires.icecore.compat.cozycafe.CozyCafePendingAdmission;
import com.yinfires.icecore.compat.cozycafe.CozyCafeHours;
import com.yinfires.icecore.compat.cozycafe.seating.CozyCafeMenuSeatAccess;
import com.yinfires.icecore.compat.cozycafe.seating.CozyCafeSeatingService;
import io.github.chakyl.cozycafe.blockentities.CafeManagerBlockEntity;
import io.github.chakyl.cozycafe.blockentities.CafeMenuBlockEntity;
import io.github.chakyl.cozycafe.entities.CustomerEntity;
import io.github.chakyl.cozycafe.util.CustomerTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "io.github.chakyl.cozycafe.util.CustomerEntityUtils", remap = false)
public abstract class CozyCafePendingSpawnMixin {
    @Inject(method = "spawnCustomerAndTarget(Lnet/minecraft/core/BlockPos;Lnet/minecraft/server/level/ServerLevel;Ljava/lang/String;Lnet/minecraft/core/BlockPos;Lio/github/chakyl/cozycafe/entities/CustomerEntity;Lio/github/chakyl/cozycafe/util/CustomerTarget;)V",
            at = @At("HEAD"), remap = false)
    private static void icecore$captureGeneration(BlockPos spawn, ServerLevel level, String skin, BlockPos target,
                                                 CustomerEntity customer, CustomerTarget type, CallbackInfo ci) {
        if (type != CustomerTarget.MENU || !level.hasChunkAt(target)) return;
        if (level.getBlockEntity(target) instanceof CafeMenuBlockEntity menu && menu.getCafeManager() != null
                && level.hasChunkAt(menu.getCafeManager())) {
            BlockEntity manager = level.getBlockEntity(menu.getCafeManager());
            if (manager instanceof CozyCafeAdmissionAccess access) {
                ((CozyCafePendingAdmission) customer).icecore$captureAdmission(manager, access.icecore$admissionGeneration(), level.getServer().overworld().getDayTime());
            }
        }
    }

    @Inject(method = "finalizeSpawn(Lnet/minecraft/core/BlockPos;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lio/github/chakyl/cozycafe/entities/CustomerEntity;Lio/github/chakyl/cozycafe/util/CustomerTarget;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void icecore$rejectLateArrival(BlockPos spawn, ServerLevel level, BlockPos target,
                                                 CustomerEntity customer, CustomerTarget type, CallbackInfo ci) {
        if (type != CustomerTarget.MENU) return;
        CozyCafePendingAdmission pending = (CozyCafePendingAdmission) customer;
        BlockEntity captured = pending.icecore$admissionManager();
        boolean ownsMenu = level.hasChunkAt(target) && level.getBlockEntity(target) instanceof CafeMenuBlockEntity menu
                && customer.getUUID().equals(((CozyCafeMenuSeatAccess) menu).icecore$getCustomerUuid());
        boolean valid = ownsMenu && captured instanceof CafeManagerBlockEntity manager && !captured.isRemoved()
                && level.hasChunkAt(captured.getBlockPos()) && level.getBlockEntity(captured.getBlockPos()) == captured
                && manager.isOpen() && !CozyCafeHours.isClosed(level.getServer().overworld().getDayTime())
                && !CozyCafeHours.crossedClosing(pending.icecore$capturedTime(), level.getServer().overworld().getDayTime())
                && ((CozyCafeAdmissionAccess) manager).icecore$admissionGeneration() == pending.icecore$capturedGeneration();
        if (!valid) {
            if (level.hasChunkAt(target) && level.getBlockEntity(target) instanceof CafeMenuBlockEntity menu
                    && customer.getUUID().equals(((CozyCafeMenuSeatAccess) menu).icecore$getCustomerUuid())) {
                CozyCafeSeatingService.release(menu);
                menu.setCustomerTravelTime(-1);
            }
            pending.icecore$captureAdmission(null, 0L, 0L);
            customer.discard();
            ci.cancel();
        } else {
            pending.icecore$captureAdmission(null, 0L, 0L);
        }
    }
}
