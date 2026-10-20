package com.yinfires.icecore.compat.render;
import java.util.IdentityHashMap;
/** A result computed against an old immutable source cannot contaminate a new source. */
public final class GenerationResults<K,V> {
    private Object source;
    private final IdentityHashMap<K,V> values=new IdentityHashMap<>();
    public synchronized V lookup(Object current,K key) {
        if(source!=current) { source=current;values.clear(); }
        return values.get(key);
    }
    public synchronized void remember(Object computed,Object current,K key,V value) {
        if(computed==current && source==computed && values.size()<4096) values.put(key,value);
    }
}
