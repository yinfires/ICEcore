package com.yinfires.icecore.tutorial;

import com.yinfires.icecore.quest.QuestScope;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;

/** Persistent shared unlocks. Locked entries retain their generation and migration marker. */
public final class TutorialGlobalData extends SavedData {
    public static final String ID = "icecore_tutorial_global";
    private record Entry(long generation, boolean unlocked) {}
    private final Map<ResourceLocation, Entry> entries = new HashMap<>();

    public long generation(ResourceLocation id) {
        Entry entry = entries.get(id);
        return entry != null && entry.unlocked() ? entry.generation() : 0;
    }

    public boolean unlock(ResourceLocation id) {
        Entry previous = entries.getOrDefault(id, new Entry(0, false));
        if (previous.unlocked()) return false;
        entries.put(id, new Entry(previous.generation() + 1, true));
        setDirty();
        return true;
    }

    public boolean reset(ResourceLocation id) {
        Entry previous = entries.get(id);
        if (previous == null || !previous.unlocked()) return false;
        entries.put(id, new Entry(previous.generation(), false));
        setDirty();
        return true;
    }

    public TutorialState state(TutorialPlayerData players, java.util.UUID player, ResourceLocation id) {
        long generation = generation(id);
        if (generation == 0) return TutorialState.LOCKED;
        return players.isRead(player, id, generation) ? TutorialState.UNLOCKED_READ : TutorialState.UNLOCKED_UNREAD;
    }

    public void migrate(TutorialDefinition def, TutorialPlayerData players) {
        if (def.scope() != QuestScope.GLOBAL || entries.containsKey(def.id())) return;
        boolean unlocked = players.allStates().values().stream()
                .anyMatch(map -> map.getOrDefault(def.id(), TutorialState.LOCKED) != TutorialState.LOCKED);
        entries.put(def.id(), new Entry(unlocked ? 1 : 0, unlocked));
        players.allStates().forEach((player, map) -> {
            if (map.get(def.id()) == TutorialState.UNLOCKED_READ) players.markGlobalRead(player, def.id(), 1);
            map.remove(def.id());
        });
        players.setDirty();
        setDirty();
    }

    public static TutorialGlobalData load(CompoundTag tag) {
        TutorialGlobalData data = new TutorialGlobalData();
        ListTag entries = tag.getList("Entries", 10);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("Id"));
            if (id != null) data.entries.put(id, new Entry(entry.getLong("Generation"), entry.getBoolean("Unlocked")));
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        entries.forEach((id, entry) -> {
            CompoundTag value = new CompoundTag();
            value.putString("Id", id.toString());
            value.putLong("Generation", entry.generation());
            value.putBoolean("Unlocked", entry.unlocked());
            list.add(value);
        });
        tag.put("Entries", list);
        return tag;
    }
}
