package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.cozycafe.CozyCafeAdmissionAccess;
import com.yinfires.icecore.compat.cozycafe.CozyCafeHours;
import io.github.chakyl.cozycafe.blockentities.CafeManagerBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "io.github.chakyl.cozycafe.blockentities.CafeManagerBlockEntity", remap = false)
public abstract class CozyCafeHoursMixin extends BlockEntity implements CozyCafeAdmissionAccess {
    @Unique private long icecore$previousTime;
    @Unique private boolean icecore$observedTime;
    @Unique private long icecore$generation;

    protected CozyCafeHoursMixin(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override public long icecore$admissionGeneration() { return icecore$generation; }

    @Inject(method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("HEAD"), remap = false)
    private void icecore$closeAtEvening(CallbackInfo ci) {
        if (level == null || level.isClientSide || level.getServer() == null) return;
        long time = level.getServer().overworld().getDayTime();
        CafeManagerBlockEntity manager = (CafeManagerBlockEntity) (Object) this;
        boolean crossed = icecore$observedTime && CozyCafeHours.crossedClosing(icecore$previousTime, time);
        icecore$previousTime = time;
        icecore$observedTime = true;
        if (manager.isOpen() && (CozyCafeHours.isClosed(time) || crossed)) manager.setOpen(false, false);
    }

    @Inject(method = "assignCustomersInArea(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void icecore$guardAdmissions(CallbackInfo ci) {
        if (level != null && !level.isClientSide && level.getServer() != null
                && (!((CafeManagerBlockEntity) (Object) this).isOpen()
                || CozyCafeHours.isClosed(level.getServer().overworld().getDayTime()))) ci.cancel();
    }

    @Inject(method = "canBeOpened(Lnet/minecraft/server/level/ServerPlayer;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void icecore$rejectNightOpening(ServerPlayer player, CallbackInfoReturnable<Boolean> ci) {
        if (CozyCafeHours.isClosed(player.server.overworld().getDayTime())) {
            io.github.chakyl.cozycafe.network.EvilPacketsIHateThem.sendToPlayer(
                    new io.github.chakyl.cozycafe.network.ClientBoundCafeCannotOpenPacket((byte) 6), player);
            ci.setReturnValue(false);
        }
    }

    @Inject(method = "setOpen(ZZ)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void icecore$guardState(boolean open, boolean forceClose, CallbackInfo ci) {
        if (level == null || level.isClientSide || level.getServer() == null) return;
        if (open && CozyCafeHours.isClosed(level.getServer().overworld().getDayTime())) {
            ci.cancel();
        } else if (!open && ((CafeManagerBlockEntity) (Object) this).isOpen()) {
            icecore$generation++;
        } else if (open) {
            icecore$previousTime = level.getServer().overworld().getDayTime();
            icecore$observedTime = true;
        }
    }
}
