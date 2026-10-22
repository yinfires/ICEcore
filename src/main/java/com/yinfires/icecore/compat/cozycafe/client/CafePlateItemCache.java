package com.yinfires.icecore.compat.cozycafe.client;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import java.util.*;

/** Bounded shared cache: native item overrides create a fresh composite model on every resolution. */
public final class CafePlateItemCache {
    public record Key(BakedModel plate,BakedModel food,RenderType type,Direction side) {}
    private static final Map<Key,List<BakedQuad>> CACHE=new HashMap<>();
    private CafePlateItemCache() {}
    public static List<BakedQuad> get(Key key) { return CACHE.get(key); }
    public static void put(Key key,List<BakedQuad> quads) {
        if(CACHE.size()>=2048) CACHE.clear();
        CACHE.put(key,quads);
    }
    public static void clear() { CACHE.clear(); }
}
