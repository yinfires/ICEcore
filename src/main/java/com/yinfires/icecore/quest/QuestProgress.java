package com.yinfires.icecore.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.Arrays;

/**
 * Mutable per-owner progress of one quest: its lifecycle state and per-objective counts.
 * Marking is not stored here; it is a per-player display preference tracked separately.
 */
public final class QuestProgress {
    private QuestState state;
    private int[] objectiveCounts;

    public QuestProgress(QuestState state, int objectiveCount) {
        this.state = state;
        this.objectiveCounts = new int[Math.max(0, objectiveCount)];
    }

    private QuestProgress(QuestState state, int[] counts) {
        this.state = state;
        this.objectiveCounts = counts;
    }

    public QuestState state() {
        return state;
    }

    public void setState(QuestState value) {
        this.state = value;
    }

    public int count(int index) {
        return index >= 0 && index < objectiveCounts.length ? objectiveCounts[index] : 0;
    }

    public int[] counts() {
        return objectiveCounts.clone();
    }

    /** Sets an objective count clamped to [0, target]; returns true if the value changed. */
    public boolean setCount(int index, int value, int target) {
        if (index < 0 || index >= objectiveCounts.length) {
            return false;
        }
        int clamped = Math.max(0, Math.min(value, target));
        if (objectiveCounts[index] == clamped) {
            return false;
        }
        objectiveCounts[index] = clamped;
        return true;
    }

    /** Resizes the count array to match the current definition, preserving overlapping values. */
    public void resize(int objectiveCount) {
        if (objectiveCounts.length != objectiveCount) {
            objectiveCounts = Arrays.copyOf(objectiveCounts, Math.max(0, objectiveCount));
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("State", state.name());
        ListTag counts = new ListTag();
        for (int value : objectiveCounts) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("C", value);
            counts.add(entry);
        }
        tag.put("Counts", counts);
        return tag;
    }

    public static QuestProgress load(CompoundTag tag) {
        QuestState state = QuestState.fromName(tag.getString("State"));
        ListTag counts = tag.getList("Counts", 10);
        int[] values = new int[counts.size()];
        for (int i = 0; i < counts.size(); i++) {
            values[i] = counts.getCompound(i).getInt("C");
        }
        return new QuestProgress(state, values);
    }
}
