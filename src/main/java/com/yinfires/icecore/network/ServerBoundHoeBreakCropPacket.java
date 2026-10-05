package com.yinfires.icecore.network;

import com.yinfires.icecore.farming.SeedBagCrops;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server: the player left-clicked a seed-bag crop with a hoe. Sent instead of the vanilla
 * break packet because adventure players are stopped client-side before any break packet exists.
 */
public record ServerBoundHoeBreakCropPacket(BlockPos pos) {
    /** Same reach the vanilla server allows for block interaction. */
    private static final double MAX_REACH_SQR = 36.0;

    public ServerBoundHoeBreakCropPacket(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.isSpectator() || !player.isAlive()) return;
            ServerLevel level = player.serverLevel();
            if (!level.isLoaded(pos)
                    || player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > MAX_REACH_SQR
                    || !SeedBagCrops.isHoe(player.getMainHandItem())) return;
            BlockState state = level.getBlockState(pos);
            if (SeedBagCrops.find(state.getBlock()) == null) return;
            if (MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player))) return;
            SeedBagCrops.breakWithHoe(level, player, pos, state);
        });
        context.setPacketHandled(true);
    }
}
