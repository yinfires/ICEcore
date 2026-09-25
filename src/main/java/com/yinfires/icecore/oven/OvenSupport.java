package com.yinfires.icecore.oven;

import net.minecraft.util.StringRepresentable;

public enum OvenSupport implements StringRepresentable {
    NONE("none"), TRAY("tray"), HANDLE("handle");
    private final String name;
    OvenSupport(String name) { this.name = name; }
    @Override public String getSerializedName() { return name; }
}
