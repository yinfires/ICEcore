package com.yinfires.icecore.workstation;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.misc.TrashCanBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record ServerBoundTrashPutPacket(BlockPos pos, Direction face) {
    private static final java.util.Map<net.minecraft.server.level.ServerPlayer, Long> LAST_PUT = new java.util.WeakHashMap<>();
    public ServerBoundTrashPutPacket(FriendlyByteBuf buffer) { this(buffer.readBlockPos(), buffer.readEnum(Direction.class)); }
    public void encode(FriendlyByteBuf buffer) { buffer.writeBlockPos(pos); buffer.writeEnum(face); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null || !player.isAlive() || player.isSpectator()) return;
            long now = System.nanoTime();
            Long previous = LAST_PUT.get(player);
            if (previous != null && now - previous < 250_000_000L) return;
            var level = player.serverLevel();
            if (!level.isLoaded(pos) || !level.mayInteract(player, pos)
                    || player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > 36
                    || !TrashCanLeftClickEvents.isTrash(level, pos)
                    || TrashCanLeftClickEvents.hasPriorityTool(player) || player.getMainHandItem().isEmpty()
                    || com.yinfires.icecore.interaction.InteractionEvents.blocked(level, player, pos)) return;
            // Consult existing protection subscribers without entering the mining pipeline.
            var permission = new TrashCanLeftClickEvents.PutPermission(player, pos, face);
            if (MinecraftForge.EVENT_BUS.post(permission) || permission.getUseBlock() == Event.Result.DENY
                    || permission.getUseItem() == Event.Result.DENY) return;
            if (!TrashCanLeftClickEvents.isTrash(level, pos) || player.getMainHandItem().isEmpty()) return;
            LAST_PUT.put(player, now);
            var entity = level.getBlockEntity(pos);
            if (entity instanceof TrashCanBlockEntity trash && entity instanceof WorkstationAccess)
                TrashCanInteraction.put(trash, player);
        });
        context.setPacketHandled(true);
    }
}
