package com.yinfires.icecore.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player quest state: each player's map of quest progress plus their single marked quest id.
 * Marking is always per-player even for GLOBAL-scope quests, so it lives here rather than in the
 * global data. Progress of PLAYER-scope quests is stored here; GLOBAL-scope progress lives in
 * {@link QuestGlobalData}.
 */
public final class QuestPlayerData extends SavedData {
    public static final String ID = "icecore_quest_players";

    private final Map<UUID, Map<ResourceLocation, QuestProgress>> progress = new HashMap<>();
    private final Map<UUID, ResourceLocation> marked = new HashMap<>();

    public Map<ResourceLocation, QuestProgress> progressFor(UUID player) {
        return progress.computeIfAbsent(player, key -> new HashMap<>());
    }

    @Nullable
    public ResourceLocation marked(UUID player) {
        return marked.get(player);
    }

    /** Sets or clears (null) the player's single marked quest; returns true if it changed. */
    public boolean setMarked(UUID player, @Nullable ResourceLocation questId) {
        ResourceLocation previous = marked.get(player);
        if (java.util.Objects.equals(previous, questId)) {
            return false;
        }
        if (questId == null) {
            marked.remove(player);
        } else {
            marked.put(player, questId);
        }
        setDirty();
        return true;
    }

    public static QuestPlayerData load(CompoundTag tag) {
        QuestPlayerData data = new QuestPlayerData();
        ListTag players = tag.getList("Players", 10);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag entry = players.getCompound(i);
            UUID id = entry.getUUID("UUID");
            Map<ResourceLocation, QuestProgress> map = new HashMap<>();
            ListTag quests = entry.getList("Quests", 10);
            for (int q = 0; q < quests.size(); q++) {
                CompoundTag questTag = quests.getCompound(q);
                ResourceLocation questId = ResourceLocation.tryParse(questTag.getString("Id"));
                if (questId != null) {
                    map.put(questId, QuestProgress.load(questTag.getCompound("Progress")));
                }
            }
            data.progress.put(id, map);
            if (entry.contains("Marked")) {
                ResourceLocation markedId = ResourceLocation.tryParse(entry.getString("Marked"));
                if (markedId != null) {
                    data.marked.put(id, markedId);
                }
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag players = new ListTag();
        for (Map.Entry<UUID, Map<ResourceLocation, QuestProgress>> playerEntry : progress.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("UUID", playerEntry.getKey());
            ListTag quests = new ListTag();
            playerEntry.getValue().forEach((questId, questProgress) -> {
                CompoundTag questTag = new CompoundTag();
                questTag.putString("Id", questId.toString());
                questTag.put("Progress", questProgress.save());
                quests.add(questTag);
            });
            entry.put("Quests", quests);
            ResourceLocation markedId = marked.get(playerEntry.getKey());
            if (markedId != null) {
                entry.putString("Marked", markedId.toString());
            }
            players.add(entry);
        }
        tag.put("Players", players);
        return tag;
    }
}
