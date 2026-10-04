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
    private final java.util.Set<ResourceLocation> migrated = new java.util.HashSet<>();

    public void migrate(QuestDefinition def, QuestPlayerData players) {
        if (def.scope() != QuestScope.GLOBAL || !migrated.add(def.id())) return;
        if (!progress.containsKey(def.id())) {
            QuestProgress merged = null;
            for (var map : players.allProgress().values()) {
                QuestProgress old = map.get(def.id());
                if (old == null) continue;
                if (merged == null) merged = new QuestProgress(QuestState.HIDDEN, def.objectives().size());
                if (old.state().ordinal() > merged.state().ordinal()) merged.setState(old.state());
                for (int i = 0; i < def.objectives().size(); i++) {
                    merged.setCount(i, Math.max(merged.count(i), old.count(i)), def.objectives().get(i).targetCount());
                }
            }
            if (merged != null) progress.put(def.id(), merged);
        }
        // Remove migrated copies so gameplay events cannot advance the same quest twice.
        players.allProgress().values().forEach(map -> map.remove(def.id()));
        players.setDirty();
        setDirty();
    }

    public Map<ResourceLocation, QuestProgress> progress() {
        return progress;
    }

    public static QuestGlobalData load(CompoundTag tag) {
        QuestGlobalData data = new QuestGlobalData();
        ListTag migration = tag.getList("Migrated", 8);
        for (int i = 0; i < migration.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(migration.getString(i));
            if (id != null) data.migrated.add(id);
        }
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
        ListTag migration = new ListTag();
        migrated.forEach(id -> migration.add(net.minecraft.nbt.StringTag.valueOf(id.toString())));
        tag.put("Migrated", migration);
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
