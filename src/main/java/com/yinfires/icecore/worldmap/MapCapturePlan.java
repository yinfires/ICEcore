package com.yinfires.icecore.worldmap;

import java.util.BitSet;
import java.util.stream.Collectors;

/** Immutable chunk coverage for every defined region, including future unlocks and feather. */
public final class MapCapturePlan {
    private final int minX,minZ,columns,rows;
    private final BitSet required;
    private MapCapturePlan(MapDefinition d,MapFog field){
        minX=Math.floorDiv(d.minX,16);minZ=Math.floorDiv(d.minZ,16);
        columns=Math.floorDiv(d.maxX-1,16)-minX+1;rows=Math.floorDiv(d.maxZ-1,16)-minZ+1;
        required=new BitSet(columns*rows);
        for(int z=0;z<rows;z++){
            if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();
            for(int x=0;x<columns;x++){
                int cx=(minX+x)*16,cz=(minZ+z)*16;
                if(field==null||field.needsTerrain(Math.max(cx,d.minX),Math.max(cz,d.minZ),Math.min(cx+16,d.maxX),Math.min(cz+16,d.maxZ)))required.set(z*columns+x);
            }
        }
    }
    public static MapCapturePlan build(MapDefinition d){
        boolean hasOutlines=d.regions.stream().anyMatch(r->!r.polygons.isEmpty()||r.runs.length>0);
        // Preserve range-only exports before any contours have been authored.
        return new MapCapturePlan(d,hasOutlines?MapFog.build(d,d.regions.stream().map(r->r.id).collect(Collectors.toUnmodifiableSet())):null);
    }
    public boolean required(int chunkX,int chunkZ){
        int x=chunkX-minX,z=chunkZ-minZ;
        return x>=0&&z>=0&&x<columns&&z<rows&&required.get(z*columns+x);
    }
    public int count(){return required.cardinality();}
    public boolean intersects(int x0,int z0,int x1,int z1){
        for(int z=Math.floorDiv(z0,16);z<=Math.floorDiv(z1-1,16);z++)for(int x=Math.floorDiv(x0,16);x<=Math.floorDiv(x1-1,16);x++)if(required(x,z))return true;
        return false;
    }
}
