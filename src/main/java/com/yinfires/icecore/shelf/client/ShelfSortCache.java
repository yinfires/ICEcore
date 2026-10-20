package com.yinfires.icecore.shelf.client;

import com.yinfires.icecore.compat.render.ExactSortCache;
import com.mojang.blaze3d.systems.RenderSystem;

public final class ShelfSortCache {
    private static final ExactSortCache CACHE=new ExactSortCache();
    private static final com.yinfires.icecore.compat.render.SortCacheAdmission ADMISSION=new com.yinfires.icecore.compat.render.SortCacheAdmission();
    private static boolean enabled;
    private static float[] pending;
    public static long hits,misses,bypassed;
    static void frame() {
        CACHE.frame();ADMISSION.frame();
        boolean next=!ShelfShaderCompatibility.nativeRequired() && !ShelfShaderCompatibility.acceleratedRenderingPresent()
            && com.yinfires.icecore.compat.render.RenderDiagnostics.mode==com.yinfires.icecore.compat.render.RenderDiagnostics.Mode.OPTIMIZED;
        if(enabled && !next) clear();enabled=next;
    }
    public static int[] lookup(float[] keys) {
        if(!RenderSystem.isOnRenderThread()) return null;
        pending=null;
        if(!enabled || keys.length<32) return null;
        if(!ADMISSION.active()) { if(ShelfRenderStats.ENABLED) bypassed++;return null; }
        int[] order=CACHE.lookup(keys);
        if(order==null) { if(ADMISSION.miss()) pending=keys;else CACHE.clear();if(ShelfRenderStats.ENABLED) misses++; }
        else { ADMISSION.hit();if(ShelfRenderStats.ENABLED) hits++; }
        return order;
    }
    public static void remember(float[] keys,int[] order) {
        if(!RenderSystem.isOnRenderThread() || pending!=keys) return;
        pending=null;CACHE.remember(keys,order);
    }
    static void clear() { CACHE.clear();ADMISSION.clear();pending=null;enabled=false;hits=misses=bypassed=0; }
    private ShelfSortCache() {}
}
