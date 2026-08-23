package com.yinfires.icecore.currency;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public final class CurrencySavedData extends SavedData {
    public static final String FILE_ID = "icecore_currency";

    private long balance;
    private boolean hudEnabled = true;

    public static CurrencySavedData load(CompoundTag tag) {
        CurrencySavedData data = new CurrencySavedData();
        data.balance = Math.max(0L, tag.getLong("Balance"));
        data.hudEnabled = !tag.contains("HudEnabled") || tag.getBoolean("HudEnabled");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("Balance", balance);
        tag.putBoolean("HudEnabled", hudEnabled);
        return tag;
    }

    public long balance() {
        return balance;
    }

    public boolean hudEnabled() {
        return hudEnabled;
    }

    void setBalance(long balance) {
        this.balance = balance;
        setDirty();
    }

    void setHudEnabled(boolean hudEnabled) {
        this.hudEnabled = hudEnabled;
        setDirty();
    }
}
