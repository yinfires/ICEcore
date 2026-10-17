package com.yinfires.icecore.interaction;

import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.network.NetworkEvent;
import java.util.*;
import java.util.function.Supplier;

/** Sends resolved block IDs rather than requiring client-side tag loading order. */
public final class ClientBoundInteractionPacket {
    private final InteractionSnapshot snapshot;
    private final long revision;
    public ClientBoundInteractionPacket(InteractionSnapshot snapshot, long revision) { this.snapshot = snapshot; this.revision = revision; }
    public ClientBoundInteractionPacket(FriendlyByteBuf buffer) {
        revision = buffer.readLong();
        Set<Block> denied = readBlocks(buffer), allowed = readBlocks(buffer);
        snapshot = new InteractionSnapshot(denied, allowed, InteractionExceptions.parse(JsonParser.parseString(buffer.readUtf(1_000_000))));
    }
    private static Set<Block> readBlocks(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > BuiltInRegistries.BLOCK.size()) throw new IllegalArgumentException("Invalid block count");
        Set<Block> blocks = new HashSet<>();
        for (int i = 0; i < count; i++) {
            var id = buffer.readResourceLocation();
            if (!BuiltInRegistries.BLOCK.containsKey(id)) throw new IllegalArgumentException("Unknown block: " + id);
            blocks.add(BuiltInRegistries.BLOCK.get(id));
        }
        return blocks;
    }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeLong(revision); writeBlocks(buffer, snapshot.denied()); writeBlocks(buffer, snapshot.allowed());
        buffer.writeUtf(snapshot.exceptions().json().toString(), 1_000_000);
    }
    private static void writeBlocks(FriendlyByteBuf buffer, Set<Block> blocks) {
        buffer.writeVarInt(blocks.size()); blocks.forEach(block -> buffer.writeResourceLocation(BuiltInRegistries.BLOCK.getKey(block)));
    }
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> InteractionClientState.accept(snapshot, revision)); context.setPacketHandled(true);
    }
}
