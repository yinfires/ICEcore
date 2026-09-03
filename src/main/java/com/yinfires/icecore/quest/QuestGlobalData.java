package com.yinfires.icecore.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Server-shared progress for GLOBAL-scope quests. Marking stays per-player in {@link QuestPlayerData}. */
public final class QuestGlobalData extends SavedData {
    public static final String ID = "icecore_quest_global";

    private final Map<ResourceLocation, QuestProgress> progress = new HashMap<>();

    public Map<ResourceLocation, QuestProgress> progress() {
        return progress;
    }

    public static QuestGlobalData load(CompoundTag tag) {
        QuestGlobalData data = new QuestGlobalData();
        ListTag quests = tag.getList("Quests", 10);
        for (int i = 0; i < quests.size(); i++) {
            CompoundTag questTag = quests.getCompound(i);
            ResourceLocation questId = ResourceLocation.tryParse(questTag.getString("Id"));
            if (questId != null) {
                data.progress.put(questId, QuestProgress.load(questTag.getCompound("Progress")));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag quests = new ListTag();
        progress.forEach((questId, questProgress) -> {
            CompoundTag questTag = new CompoundTag();
            questTag.putString("Id", questId.toString());
            questTag.put("Progress", questProgress.save());
            quests.add(questTag);
        });
        tag.put("Quests", quests);
        return tag;
    }
}
