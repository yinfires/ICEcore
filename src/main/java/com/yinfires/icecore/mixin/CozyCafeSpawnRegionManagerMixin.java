package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.cozycafe.board.CozyCafeBoardService;
import com.yinfires.icecore.compat.cozycafe.spawn.CozyCafeSpawnRegionService;
import com.yinfires.icecore.compat.cozycafe.seating.CozyCafeSeatingService;
import io.github.chakyl.cozycafe.CozyCafe;
import io.github.chakyl.cozycafe.blockentities.CafeManagerBlockEntity;
import io.github.chakyl.cozycafe.blockentities.CafeSignBlockEntity;
import io.github.chakyl.cozycafe.network.ClientBoundCafeCannotOpenPacket;
import io.github.chakyl.cozycafe.network.EvilPacketsIHateThem;
import io.github.chakyl.cozycafe.util.GeneralUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Substitutes an ICEcore entrance for CozyCafe's sign without changing its menu lifecycle. */
@Pseudo
@Mixin(targets = "io.github.chakyl.cozycafe.blockentities.CafeManagerBlockEntity", remap = false)
public abstract class CozyCafeSpawnRegionManagerMixin extends BlockEntity {
    @Shadow private BlockPos linkedSign;
    @Shadow private boolean open;
    @Shadow private int attemptedCustomers;
    @Shadow private int dayLastOpened;
    @Shadow private List<ItemStack> menu;
    @Shadow public abstract int getStarsFromReputation();
    @Shadow protected abstract boolean hasNearbyOpenManagers();
    @Shadow public abstract void sendCloseCommandToMenus(Level level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state);
    @Shadow private BlockPos getFirstPos(net.minecraft.world.level.block.state.BlockState state, BlockPos pos) { throw new AssertionError(); }
    @Shadow private BlockPos getSecondPos(net.minecraft.world.level.block.state.BlockState state, BlockPos pos) { throw new AssertionError(); }

    @Unique private BlockPos icecore$originalSign;
    @Unique private boolean icecore$usingRegion;

    protected CozyCafeSpawnRegionManagerMixin(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos,
                                               net.minecraft.world.level.block.state.BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "assignCustomersInArea(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("HEAD"), remap = false)
    private void icecore$substituteEntrance(Level level, BlockPos pos,
                                            net.minecraft.world.level.block.state.BlockState state, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel) {
            BlockPos entrance = com.yinfires.icecore.compat.cozycafe.seating.CozyCafeSeatingService
                    .snapshotEntrance(serverLevel, pos, serverLevel.random);
            if (entrance == null) entrance = CozyCafeSpawnRegionService.randomPosition(serverLevel, pos, serverLevel.random);
            if (entrance != null) {
                icecore$originalSign = linkedSign;
                linkedSign = entrance.above();
                icecore$usingRegion = true;
            }
        }
    }

    @Redirect(method = "assignCustomersInArea(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;m_60713_(Lnet/minecraft/world/level/block/Block;)Z", ordinal = 1), remap = false)
    private boolean icecore$acceptRegionAsSign(net.minecraft.world.level.block.state.BlockState state,
                                               net.minecraft.world.level.block.Block expected) {
        return icecore$usingRegion || state.is(expected);
    }

    @Inject(method = "assignCustomersInArea(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("RETURN"), remap = false)
    private void icecore$restoreSign(Level level, BlockPos pos,
                                     net.minecraft.world.level.block.state.BlockState state, CallbackInfo ci) {
        if (icecore$usingRegion) {
            linkedSign = icecore$originalSign;
            icecore$originalSign = null;
            icecore$usingRegion = false;
        }
    }

    @Inject(method = "canBeOpened(Lnet/minecraft/server/level/ServerPlayer;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void icecore$allowRegionOpening(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (!(level instanceof ServerLevel serverLevel)
                || !CozyCafeSpawnRegionService.hasValidRegion(serverLevel, worldPosition)) return;
        int stars = getStarsFromReputation();
        int required = CozyCafe.CONFIG.menuSizePerStar.get();
        if (menu == null || (stars == 0 && menu.size() < required)) {
            EvilPacketsIHateThem.sendToPlayer(new ClientBoundCafeCannotOpenPacket((byte) 1), player); cir.setReturnValue(false);
        } else if (menu.size() < required * stars) {
            EvilPacketsIHateThem.sendToPlayer(new ClientBoundCafeCannotOpenPacket((byte) 2), player); cir.setReturnValue(false);
        } else if (CozyCafe.CONFIG.dailyLimit.get() && dayLastOpened == GeneralUtils.getDay(level)) {
            EvilPacketsIHateThem.sendToPlayer(new ClientBoundCafeCannotOpenPacket((byte) 3), player); cir.setReturnValue(false);
        } else if (hasNearbyOpenManagers()) {
            EvilPacketsIHateThem.sendToPlayer(new ClientBoundCafeCannotOpenPacket((byte) 4), player); cir.setReturnValue(false);
        } else if (!CozyCafeSeatingService.hasAnyValidMenu(serverLevel,
                getFirstPos(getBlockState(), worldPosition), getSecondPos(getBlockState(), worldPosition))) {
            // This mixin exits canBeOpened early for custom entrances, so the common RETURN hook
            // is not guaranteed to run.  Apply the same structural menu/seat check here; route
            // warmup is deliberately not part of opening validation.
            EvilPacketsIHateThem.sendToPlayer(new ClientBoundCafeCannotOpenPacket((byte) 5), player);
            cir.setReturnValue(false);
        } else {
            // The common RETURN hook performs the single structural menu-seat validation and starts
            // bounded route warmup. A custom entrance replaces only the missing-sign condition.
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "setOpen(ZZ)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void icecore$setOpenWithoutSign(boolean value, boolean forceClose, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)
                || !CozyCafeSpawnRegionService.hasValidRegion(serverLevel, worldPosition)) return;
        open = value;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        if (!value) {
            if (forceClose) sendCloseCommandToMenus(level, worldPosition, getBlockState());
            attemptedCustomers = 0;
        } else dayLastOpened = GeneralUtils.getDay(level);
        CozyCafeBoardService.sync((CafeManagerBlockEntity) (Object) this);
        ci.cancel();
    }

    @Inject(method = "setCafeName(Ljava/lang/String;)V", at = @At("RETURN"), remap = false)
    private void icecore$syncName(String name, CallbackInfo ci) {
        CozyCafeBoardService.sync((CafeManagerBlockEntity) (Object) this);
    }

    @Inject(method = "setOpen(ZZ)V", at = @At("RETURN"), remap = false)
    private void icecore$syncOpen(boolean value, boolean forceClose, CallbackInfo ci) {
        CozyCafeBoardService.sync((CafeManagerBlockEntity) (Object) this);
    }
}
