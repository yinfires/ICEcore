package com.yinfires.icecore.tutorial;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player tutorial unlock/read state. LOCKED entries are omitted (the default). */
public final class TutorialPlayerData extends SavedData {
    public static final String ID = "icecore_tutorial_players";

    private final Map<UUID, Map<ResourceLocation, TutorialState>> states = new HashMap<>();

    public Map<ResourceLocation, TutorialState> statesFor(UUID player) {
        return states.computeIfAbsent(player, key -> new HashMap<>());
    }

    public TutorialState state(UUID player, ResourceLocation tutorial) {
        Map<ResourceLocation, TutorialState> map = states.get(player);
        if (map == null) {
            return TutorialState.LOCKED;
        }
        return map.getOrDefault(tutorial, TutorialState.LOCKED);
    }

    /** Sets a tutorial state; LOCKED removes the entry. Returns true if it changed. */
    public boolean setState(UUID player, ResourceLocation tutorial, TutorialState state) {
        Map<ResourceLocation, TutorialState> map = statesFor(player);
        TutorialState previous = map.getOrDefault(tutorial, TutorialState.LOCKED);
        if (previous == state) {
            return false;
        }
        if (state == TutorialState.LOCKED) {
            map.remove(tutorial);
        } else {
            map.put(tutorial, state);
        }
        setDirty();
        return true;
    }

    public static TutorialPlayerData load(CompoundTag tag) {
        TutorialPlayerData data = new TutorialPlayerData();
        ListTag players = tag.getList("Players", 10);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag entry = players.getCompound(i);
            UUID id = entry.getUUID("UUID");
            Map<ResourceLocation, TutorialState> map = new HashMap<>();
            ListTag tutorials = entry.getList("Tutorials", 10);
            for (int t = 0; t < tutorials.size(); t++) {
                CompoundTag tutorialTag = tutorials.getCompound(t);
                ResourceLocation tutorialId = ResourceLocation.tryParse(tutorialTag.getString("Id"));
                if (tutorialId != null) {
                    map.put(tutorialId, TutorialState.fromName(tutorialTag.getString("State")));
                }
            }
            data.states.put(id, map);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag players = new ListTag();
        states.forEach((playerId, map) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("UUID", playerId);
            ListTag tutorials = new ListTag();
            map.forEach((tutorialId, state) -> {
                CompoundTag tutorialTag = new CompoundTag();
                tutorialTag.putString("Id", tutorialId.toString());
                tutorialTag.putString("State", state.name());
                tutorials.add(tutorialTag);
            });
            entry.put("Tutorials", tutorials);
            players.add(entry);
        });
        tag.put("Players", players);
        return tag;
    }
}
