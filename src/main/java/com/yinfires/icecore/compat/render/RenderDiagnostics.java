package com.yinfires.icecore.compat.render;

/** Session-only switches; no world, save or network state. */
public final class RenderDiagnostics {
    public static volatile boolean queryCache=true;
    public static volatile Mode mode=Mode.OPTIMIZED;
    public enum Mode { NATIVE, OPTIMIZED, HIDDEN }
    private RenderDiagnostics() {}
}
