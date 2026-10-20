package com.yinfires.icecore.compat.render;

/** Exact memoization of the verified pure Embeddium float-key sorter. Render-thread owned. */
public final class ExactSortCache {
    public static final int MAX_BYTES=1024*1024;
    private static final int SLOTS=128;
    private final Entry[] entries=new Entry[SLOTS];
    private int bytes;
    private int admissions=2;
    public void frame() { admissions=2; }
    private record Entry(int hash,float[] keys,int[] order) {}
    private static int hash(float[] keys) {
        int hash=1;for(float key:keys) hash=31*hash+Float.floatToRawIntBits(key);return hash^(hash>>>16);
    }
    private static boolean equal(float[] a,float[] b) {
        if(a.length!=b.length) return false;
        for(int i=0;i<a.length;i++) if(Float.floatToRawIntBits(a[i])!=Float.floatToRawIntBits(b[i])) return false;
        return true;
    }
    public int[] lookup(float[] keys) {
        int hash=hash(keys);Entry entry=entries[hash&(SLOTS-1)];
        return entry!=null && entry.hash==hash && equal(keys,entry.keys)?entry.order.clone():null;
    }
    public void remember(float[] keys,int[] order) {
        if(admissions==0 || keys.length<32 || keys.length!=order.length || keys.length>(MAX_BYTES-64)/8) return;
        int hash=hash(keys),slot=hash&(SLOTS-1);Entry previous=entries[slot];
        if(previous!=null) { bytes-=64+previous.keys.length*8;entries[slot]=null; }
        int required=64+keys.length*8;
        if(bytes+required>MAX_BYTES-2048) return;
        entries[slot]=new Entry(hash,keys.clone(),order.clone());bytes+=required;admissions--;
    }
    public int bytes() { return bytes; }
    public void clear() { java.util.Arrays.fill(entries,null);bytes=0; }
}
