package com.yinfires.icecore.worldmap;

import java.util.*;

/** Row index and exterior segments of an RLE footprint, built once off the render thread. */
public final class MapFootprint {
    public record Segment(int x,int z,int endX,int endZ){}
    private static final int[] EMPTY=new int[0];
    private final NavigableMap<Integer,int[]> rows=new TreeMap<>();
    private final Map<Long,List<Segment>> tiles=new HashMap<>();
    public MapFootprint(int[] runs){
        var raw=new TreeMap<Integer,List<int[]>>();for(int i=0;i<runs.length;i+=3){if((i&1023)==0)checkCancelled();raw.computeIfAbsent(runs[i+1],k->new ArrayList<>()).add(new int[]{runs[i],runs[i]+runs[i+2]});}
        for(var entry:raw.entrySet()){checkCancelled();
            entry.getValue().sort(Comparator.comparingInt(a->a[0]));var merged=new ArrayList<Integer>();
            for(var interval:entry.getValue())if(!merged.isEmpty()&&interval[0]<=merged.get(merged.size()-1))merged.set(merged.size()-1,Math.max(interval[1],merged.get(merged.size()-1)));else{merged.add(interval[0]);merged.add(interval[1]);}
            rows.put(entry.getKey(),merged.stream().mapToInt(Integer::intValue).toArray());
        }
        var vertical=new HashMap<Integer,List<int[]>>();var boundaries=new TreeSet<Integer>();
        for(var row:rows.entrySet()){
            int z=row.getKey();boundaries.add(z);boundaries.add(z+1);int[] a=row.getValue();
            for(int i=0;i<a.length;i+=2){vertical.computeIfAbsent(a[i],k->new ArrayList<>()).add(new int[]{z,z+1});vertical.computeIfAbsent(a[i+1],k->new ArrayList<>()).add(new int[]{z,z+1});}
        }
        for(var entry:vertical.entrySet()){
            int start=Integer.MIN_VALUE,end=start;
            for(var interval:entry.getValue()){if(interval[0]==end)end=interval[1];else{if(start!=Integer.MIN_VALUE)add(new Segment(entry.getKey(),start,entry.getKey(),end));start=interval[0];end=interval[1];}}
            if(start!=Integer.MIN_VALUE)add(new Segment(entry.getKey(),start,entry.getKey(),end));
        }
        for(int z:boundaries){checkCancelled();
            int[] a=rows.getOrDefault(z-1,EMPTY),b=rows.getOrDefault(z,EMPTY);int i=0,j=0,start=0;boolean open=false;
            while(i<a.length||j<b.length){int value;if(j==b.length||i<a.length&&a[i]<b[j])value=a[i++];else if(i==a.length||b[j]<a[i])value=b[j++];else{i++;j++;continue;}
                if(!open)start=value;else if(value>start)add(new Segment(start,z,value,z));open=!open;
            }
        }
    }
    private static void checkCancelled(){if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();}
    private void add(Segment segment){
        // Split long edges at 512-block boundaries so each viewport tile has bounded work.
        if(segment.x==segment.endX){int z=segment.z;while(z<segment.endZ){int end=Math.min(segment.endZ,(Math.floorDiv(z,512)+1)*512);store(new Segment(segment.x,z,segment.endX,end),Math.floorDiv(segment.x,512),Math.floorDiv(z,512));z=end;}}
        else{int x=segment.x;while(x<segment.endX){int end=Math.min(segment.endX,(Math.floorDiv(x,512)+1)*512);store(new Segment(x,segment.z,end,segment.endZ),Math.floorDiv(x,512),Math.floorDiv(segment.z,512));x=end;}}
    }
    private void store(Segment segment,int x,int z){tiles.computeIfAbsent(key(x,z),k->new ArrayList<>()).add(segment);}
    private static long key(int x,int z){return (long)x<<32|(z&0xffffffffL);}
    public List<Segment> tile(int x,int z){return tiles.getOrDefault(key(x,z),List.of());}
    public boolean contains(double x,double z){
        int[] a=rows.get((int)Math.floor(z));if(a==null)return false;
        int low=0,high=a.length/2-1;while(low<=high){int mid=(low+high)>>>1;if(x<a[mid*2])high=mid-1;else if(x>=a[mid*2+1])low=mid+1;else return true;}return false;
    }
}
