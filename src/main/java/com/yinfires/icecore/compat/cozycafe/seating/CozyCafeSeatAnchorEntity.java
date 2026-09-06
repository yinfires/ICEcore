package com.yinfires.icecore.compat.cozycafe.seating;

import com.yinfires.icecore.npc.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** Invisible vehicle used instead of invoking optional furniture mods' player-only interaction code. */
public final class CozyCafeSeatAnchorEntity extends Entity {
    private UUID customerUuid;
    private int emptyTicks;

    public CozyCafeSeatAnchorEntity(EntityType<? extends CozyCafeSeatAnchorEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public CozyCafeSeatAnchorEntity(Level level, UUID customerUuid) {
        this(ModEntities.COZY_CAFE_SEAT.get(), level);
        this.customerUuid = customerUuid;
    }

    @Override protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        boolean hasBoundPassenger = customerUuid != null
                && getPassengers().stream().anyMatch(entity -> customerUuid.equals(entity.getUUID()));
        if (hasBoundPassenger) {
            emptyTicks = 0;
        } else if (++emptyTicks > 100) {
            discard();
        }
    }

    @Override protected void positionRider(Entity passenger, MoveFunction move) {
        if (hasPassenger(passenger)) {
            // The anchor is positioned at the adapter's final rider-foot coordinate.  Calling
            // vanilla's generic passenger placement here would apply a second LivingEntity
            // offset and makes several furniture seats visibly too high.
            move.accept(passenger, getX(), getY(), getZ());
            passenger.setYRot(getYRot());
            passenger.setYBodyRot(getYRot());
            passenger.setYHeadRot(getYRot());
        }
    }
    @Override public double getPassengersRidingOffset() { return 0.0D; }
    @Override protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && customerUuid != null && customerUuid.equals(passenger.getUUID());
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Customer")) customerUuid = tag.getUUID("Customer");
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if (customerUuid != null) tag.putUUID("Customer", customerUuid);
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return new ClientboundAddEntityPacket(this); }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
}
