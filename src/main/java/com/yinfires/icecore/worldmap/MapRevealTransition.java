package com.yinfires.icecore.worldmap;

/** A new reveal starts only when its visible tiles are ready; monotonic time bounds its lifetime. */
public final class MapRevealTransition {
    private long start=-1;
    public void reset(){start=-1;}
    public double opacity(boolean ready,long now){
        if(!ready){reset();return 0;}
        if(start<0)start=now;
        return MapFog.smooth((now-start)/300_000_000.0);
    }
}
