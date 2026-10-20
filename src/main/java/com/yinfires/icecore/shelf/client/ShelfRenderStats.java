package com.yinfires.icecore.shelf.client;

import com.mojang.logging.LogUtils;
import java.util.EnumMap;

/** Opt in with -Dicecore.shelf.renderStats=true. No clock reads in the disabled path. */
final class ShelfRenderStats {
    static final boolean ENABLED=Boolean.getBoolean("icecore.shelf.renderStats");
    enum Reason { SHADERS, WRAPPER, FOG, BUILD_BUDGET, ITEM_PENDING, DYNAMIC, UNKNOWN_LAYER, MEMORY, BULK_WRAPPER }
    private static final EnumMap<Reason,Long> REASONS=new EnumMap<>(Reason.class);
    private static final java.util.Map<String,Long> LAYERS=new java.util.HashMap<>();
    private static long window,shelves,hits,models,vertices,switches,rebuilds,buildNs,submitNs,gpuDraws,bulk;
    static long start() { return ENABLED?System.nanoTime():0; }
    static void shelf() { if(ENABLED) shelves++; }
    static void hit() { if(ENABLED) hits++; }
    static void model() { if(ENABLED) models++; }
    static void layer(net.minecraft.client.renderer.RenderType layer,int vertices) {
        if(ENABLED) LAYERS.merge(layer.toString(),(long)vertices,Long::sum);
    }
    static void reason(Reason reason) { if(ENABLED) REASONS.merge(reason,1L,Long::sum); }
    static void build(long start) { if(ENABLED) { rebuilds++;buildNs+=System.nanoTime()-start; } }
    static void submit(long start) { if(ENABLED) submitNs+=System.nanoTime()-start; }
    static void draw(int count,boolean gpu) { if(ENABLED) { vertices+=count;switches++;if(gpu) gpuDraws++;else bulk++; } }
    static void frame() {
        if(!ENABLED) return;
        long now=System.nanoTime();
        if(window==0) { window=now;return; }
        if(now-window<5_000_000_000L) return;
        LogUtils.getLogger().info("Shelf render 5s: shelves={}, sceneHits={}, models={}, vertices={}, bufferRequests={}, rebuilds={}, buildMs={}, submitMs={}, gpuDraws={}, bulk={}, fallback={}",
                shelves,hits,models,vertices,switches,rebuilds,buildNs/1e6,submitNs/1e6,gpuDraws,bulk,REASONS);
        if(!LAYERS.isEmpty()) LogUtils.getLogger().info("Shelf captured native layers (vertex counts): {}",LAYERS);
        shelves=hits=models=vertices=switches=rebuilds=buildNs=submitNs=gpuDraws=bulk=0;REASONS.clear();LAYERS.clear();window=now;
    }
    private ShelfRenderStats() {}
}
