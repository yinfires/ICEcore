package com.yinfires.icecore.mixin;

import com.mojang.authlib.GameProfile;
import com.yinfires.icecore.compat.cozycafe.seating.CozyCafeMenuSeatAccess;
import com.yinfires.icecore.compat.cozycafe.seating.CozyCafeSeatingService;
import io.github.chakyl.cozycafe.blockentities.CafeManagerBlockEntity;
import io.github.chakyl.cozycafe.blockentities.CafeMenuBlockEntity;
import io.github.chakyl.cozycafe.entities.CustomerEntity;
import io.github.chakyl.cozycafe.util.CustomerEntityUtils;
import io.github.chakyl.cozycafe.util.CustomerTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Pseudo
@Mixin(targets = "io.github.chakyl.cozycafe.blockentities.CafeMenuBlockEntity", remap = false)
public abstract class CozyCafeMenuSeatingMixin implements CozyCafeMenuSeatAccess {
    @Shadow private boolean hasCustomer;
    @Shadow private int waitTime;
    @Shadow private int orderTime;
    @Shadow private int customerTravelTime;
    @Shadow private int currentCourse;
    @Shadow private ItemStack requestedItem;
    @Shadow private ItemStack eatingItem;
    @Shadow private String customerSkin;
    @Shadow private GameProfile gameProfile;

    @Unique private UUID icecore$customerUuid;
    @Unique private BlockPos icecore$seatPos;
    @Unique private BlockPos icecore$receptionPos;
    @Unique private String icecore$seatingStage = "idle";

    @Override public UUID icecore$getCustomerUuid() { return icecore$customerUuid; }
    @Override public void icecore$setCustomerUuid(UUID uuid) { icecore$customerUuid = uuid; }
    @Override public BlockPos icecore$getSeatPos() { return icecore$seatPos; }
    @Override public void icecore$setSeatPos(BlockPos pos) { icecore$seatPos = pos == null ? null : pos.immutable(); }
    @Override public BlockPos icecore$getReceptionPos() { return icecore$receptionPos; }
    @Override public void icecore$setReceptionPos(BlockPos pos) { icecore$receptionPos = pos == null ? null : pos.immutable(); }
    @Override public String icecore$getSeatingStage() { return icecore$seatingStage; }
    @Override public void icecore$setSeatingStage(String stage) { icecore$seatingStage = stage == null ? "idle" : stage; }

    @Inject(method = "canReceiveNewCustomer()Z", at = @At("RETURN"), cancellable = true, remap = false)
    private void icecore$requireSeat(CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ()) {
            callback.setReturnValue(CozyCafeSeatingService.hasValidSeat((CafeMenuBlockEntity) (Object) this, true));
        }
    }

    @Inject(method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("HEAD"), remap = false)
    private void icecore$pauseWaitUntilRequestExists(Level level, BlockPos pos, BlockState state, CallbackInfo callback) {
        if (!level.isClientSide && !requestedItem.isEmpty()) return;
        if (!level.isClientSide && waitTime >= 0) waitTime = -1;
    }

    @Inject(method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("TAIL"), remap = false)
    private void icecore$validateSeat(Level level, BlockPos pos, BlockState state, CallbackInfo callback) {
        CafeMenuBlockEntity menu = (CafeMenuBlockEntity) (Object) this;
        if (menu.getLevel() != null && !menu.getLevel().isClientSide && menu.getLevel().getGameTime() % 20L == 0L) {
            CozyCafeSeatingService.validate(menu);
        }
    }

    @Redirect(method = "onCustomerArrived(Lnet/minecraft/world/entity/PathfinderMob;)V",
            at = @At(value = "INVOKE", target = "Lio/github/chakyl/cozycafe/entities/CustomerEntity;m_142467_(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"), remap = false)
    private void icecore$keepArrivingCustomer(CustomerEntity customer, RemovalReason reason) {
        // The real customer stays mounted instead of becoming a block-entity player model.
    }

    @Redirect(method = "closeMenu(Z)V", at = @At(value = "INVOKE",
            target = "Lio/github/chakyl/cozycafe/util/CustomerEntityUtils;spawnCustomerAndTarget(Lnet/minecraft/core/BlockPos;Lnet/minecraft/server/level/ServerLevel;Ljava/lang/String;Lnet/minecraft/core/BlockPos;Lio/github/chakyl/cozycafe/entities/CustomerEntity;Lio/github/chakyl/cozycafe/util/CustomerTarget;)V"), remap = false)
    private void icecore$reuseDepartingCustomer(BlockPos spawn, ServerLevel level, String skin, BlockPos target,
                                                 CustomerEntity replacement, CustomerTarget type) {
        CafeMenuBlockEntity menu = (CafeMenuBlockEntity) (Object) this;
        CustomerEntity customer = CozyCafeSeatingService.boundCustomer(menu);
        if (customer == null) {
            replacement.discard();
            return;
        }
        CozyCafeSeatingService.prepareDeparture(menu, customer, target);
        replacement.discard();
    }

    @Inject(method = "closeMenu(Z)V", at = @At("HEAD"), remap = false)
    private void icecore$prepareWalkingCustomerDeparture(boolean forced, CallbackInfo callback) {
        CozyCafeSeatingService.prepareClose((CafeMenuBlockEntity) (Object) this);
    }

    @Inject(method = "closeMenu(Z)V", at = @At("RETURN"), remap = false)
    private void icecore$releaseSeat(boolean forced, CallbackInfo callback) {
        CozyCafeSeatingService.release((CafeMenuBlockEntity) (Object) this);
    }

    @Inject(method = "m_183515_(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"), remap = false)
    private void icecore$saveSeating(CompoundTag tag, CallbackInfo callback) {
        if (icecore$customerUuid != null) tag.putUUID("icecoreCustomer", icecore$customerUuid);
        if (icecore$seatPos != null) tag.putLong("icecoreSeat", icecore$seatPos.asLong());
        if (icecore$receptionPos != null) tag.putLong("icecoreReception", icecore$receptionPos.asLong());
        tag.putString("icecoreSeatingStage", icecore$seatingStage);
    }

    @Inject(method = "m_142466_(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"), remap = false)
    private void icecore$loadSeating(CompoundTag tag, CallbackInfo callback) {
        icecore$customerUuid = tag.hasUUID("icecoreCustomer") ? tag.getUUID("icecoreCustomer") : null;
        icecore$seatPos = tag.contains("icecoreSeat") ? BlockPos.of(tag.getLong("icecoreSeat")) : null;
        icecore$receptionPos = tag.contains("icecoreReception") ? BlockPos.of(tag.getLong("icecoreReception")) : null;
        icecore$seatingStage = tag.contains("icecoreSeatingStage") ? tag.getString("icecoreSeatingStage") : "idle";
    }
}
