package com.yinfires.icecore.shelf.client;

import com.mojang.logging.LogUtils;
import java.util.EnumMap;

/** Opt in with -Dicecore.shelf.renderStats=true. No clock reads in the disabled path. */
final class ShelfRenderStats {
    static boolean ENABLED=Boolean.getBoolean("icecore.shelf.renderStats");
    enum Reason { ACCELERATOR, SHADERS, WRAPPER, FOG, BUILD_BUDGET, ITEM_PENDING, DYNAMIC, UNKNOWN_LAYER, MEMORY, BULK_WRAPPER }
    private static final EnumMap<Reason,Long> REASONS=new EnumMap<>(Reason.class);
    private static final java.util.Map<String,Long> LAYERS=new java.util.HashMap<>();
    private static final java.util.Set<String> TYPES=new java.util.HashSet<>();
    static void type(String kind,Object value) { if(ENABLED && TYPES.size()<64 && TYPES.add(kind+":"+value.getClass().getName())) LogUtils.getLogger().info("Shelf {}: {}",kind,value.getClass().getName()); }
    private static final long[] FRAMES=new long[16384];
    private static int frameCount;private static long previousFrame;
    static void reset() { staticSlots=nativeSlots=0;frameCount=0;previousFrame=0;window=0;TYPES.clear();REASONS.clear();LAYERS.clear();shelves=hits=models=vertices=switches=rebuilds=buildNs=submitNs=gpuDraws=bulk=0; }
    private static long staticSlots,nativeSlots;
    private static long window,shelves,hits,models,vertices,switches,rebuilds,buildNs,submitNs,gpuDraws,bulk;
    static long start() { return ENABLED?System.nanoTime():0; }
    static void shelf() { if(ENABLED) shelves++; }
    static void slots(int cached,int fallback) { if(ENABLED) { staticSlots+=cached;nativeSlots+=fallback; } }
    private static final java.util.Set<String> ELIGIBILITY=new java.util.HashSet<>();
    static void clearEligibility() { ELIGIBILITY.clear(); }
    static void eligibility(net.minecraft.world.item.ItemStack stack,net.minecraft.client.resources.model.BakedModel model,String reason) {
        if(!ENABLED || ELIGIBILITY.size()>=256) return;
        boolean handler=((com.yinfires.icecore.mixin.DisplayItemColorsAccessor)net.minecraft.client.Minecraft.getInstance().getItemColors()).icecore$colors()
                .containsKey(net.minecraftforge.registries.ForgeRegistries.ITEMS.getDelegateOrThrow(stack.getItem()));
        String key=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())+":"+model.getClass().getName()+":"+reason;
        if(ELIGIBILITY.add(key)) LogUtils.getLogger().info("Shelf eligibility: item={}, model={}, overrides={}, colorHandler={}, result={}",
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()),model.getClass().getName(),model.getOverrides().getClass().getName(),handler,reason==null?"static":reason);
    }
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
        if(previousFrame!=0 && frameCount<FRAMES.length) FRAMES[frameCount++]=now-previousFrame;
        previousFrame=now;
        if(window==0) { window=now;return; }
        if(now-window<5_000_000_000L) return;
        LogUtils.getLogger().info("Shelf render 5s: shelves={}, sceneHits={}, models={}, vertices={}, bufferRequests={}, rebuilds={}, buildMs={}, submitMs={}, gpuDraws={}, bulk={}, fallback={}",
                shelves,hits,models,vertices,switches,rebuilds,buildNs/1e6,submitNs/1e6,gpuDraws,bulk,REASONS);
        LogUtils.getLogger().info("Shelf geometry slots: static={}, native={}",staticSlots,nativeSlots);staticSlots=nativeSlots=0;
        if(!LAYERS.isEmpty()) LogUtils.getLogger().info("Shelf captured native layers (vertex counts): {}",LAYERS);
        if(frameCount>0) {
            java.util.Arrays.sort(FRAMES,0,frameCount);
            LogUtils.getLogger().info("Shelf frame intervals: mode={}, queryCache={}, n={}, medianMs={}, p95Ms={}",com.yinfires.icecore.compat.render.RenderDiagnostics.mode,com.yinfires.icecore.compat.render.RenderDiagnostics.queryCache,frameCount,FRAMES[frameCount/2]/1e6,FRAMES[Math.min(frameCount-1,(int)Math.ceil(frameCount*.95)-1)]/1e6);
        }
        frameCount=0;
        shelves=hits=models=vertices=switches=rebuilds=buildNs=submitNs=gpuDraws=bulk=0;REASONS.clear();LAYERS.clear();window=now;
    }
    private ShelfRenderStats() {}
}
