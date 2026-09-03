package com.yinfires.icecore.quest;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * One ACTIVE quest's state as synced to the client for the journal list and tracker HUD.
 * Only ACTIVE quests are sent (HIDDEN/COMPLETE are filtered server-side); objective counts drive
 * the {@code x/y} progress labels resolved against the client-side definition.
 */
public record QuestSnapshot(ResourceLocation id, int[] objectiveCounts) {

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(id);
        buffer.writeVarInt(objectiveCounts.length);
        for (int count : objectiveCounts) {
            buffer.writeVarInt(count);
        }
    }

    public static QuestSnapshot decode(FriendlyByteBuf buffer) {
        ResourceLocation id = buffer.readResourceLocation();
        int length = buffer.readVarInt();
        int[] counts = new int[length];
        for (int i = 0; i < length; i++) {
            counts[i] = buffer.readVarInt();
        }
        return new QuestSnapshot(id, counts);
    }
}
