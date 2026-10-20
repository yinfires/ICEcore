package com.yinfires.icecore.compat.render;

/** Bounded probes when dynamic batches never reuse exact sort keys. */
public final class SortCacheAdmission {
    private int misses,cooldown;
    public void frame() { if(cooldown>0) cooldown--; }
    public boolean active() { return cooldown==0; }
    public void hit() { misses=0; }
    public boolean miss() {
        if(++misses<32) return true;
        misses=0;cooldown=120;return false;
    }
    public void clear() { misses=cooldown=0; }
}
