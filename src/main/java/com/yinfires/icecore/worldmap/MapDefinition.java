package com.yinfires.icecore.worldmap;

import com.google.gson.Gson;
import java.util.*;

/** World-specific content. Coordinates are block coordinates, north is -Z. */
public final class MapDefinition {
    public static final Gson JSON = new Gson();
    public String dimension = "minecraft:overworld";
    public int minX, minZ, maxX = 512, maxZ = 512;
    public String atlas = "unpublished";
    public List<Region> regions = new ArrayList<>();
    public record Point(double x, double z) {}
    public static final class Region {
        public String id = "";
        public String binding = "";
        public int expansion = 64, feather = 64;
        public List<List<Point>> polygons = new ArrayList<>();
        /** Exact horizontal footprint: triples (x, z, inclusive run length). */
        public int[] runs = new int[0];
        public boolean contains(double x, double z) {
            for (var polygon : polygons) if (inside(polygon, x, z)) return true;
            for (int i = 0; i < runs.length; i += 3)
                if (z >= runs[i+1] && z < runs[i+1]+1 && x >= runs[i] && x < runs[i]+runs[i+2]) return true;
            return false;
        }
    }
    public Region region(String id) { return regions.stream().filter(r -> r.id.equals(id)).findFirst().orElse(null); }
    public MapDefinition copy() { return parse(JSON.toJson(this)); }
    public static MapDefinition parse(String json) {
        MapDefinition d = JSON.fromJson(json, MapDefinition.class);
        if (d == null) throw new IllegalArgumentException("empty map");
        d.validate(); return d;
    }
    public void validate() {
        if (dimension == null || !dimension.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || atlas == null || !atlas.matches("[a-zA-Z0-9_-]{1,64}")) throw new IllegalArgumentException("map identity");
        if (minX < -30000000 || minZ < -30000000 || maxX > 30000000 || maxZ > 30000000 || maxX <= minX || maxZ <= minZ || (long)maxX-minX > 16384 || (long)maxZ-minZ > 16384) throw new IllegalArgumentException("map bounds: maximum 16384 blocks per axis");
        if (regions == null || regions.size() > 128) throw new IllegalArgumentException("region limit");
        Set<String> ids = new HashSet<>(); int total = 0;
        for (Region r : regions) {
            if (r == null || r.id == null || !r.id.matches("[a-z0-9_-]{1,48}") || !ids.add(r.id) || r.binding == null || r.binding.length()>128 || r.expansion<0 || r.expansion>512 || r.feather<1 || r.feather>512 || r.polygons==null || r.runs==null || r.runs.length%3!=0) throw new IllegalArgumentException("invalid region");
            for (var p : r.polygons) {
                if (p == null || p.size()<3 || p.size()>512) throw new IllegalArgumentException("polygon");
                total+=p.size();
                for (Point v : p) if (v==null || !Double.isFinite(v.x) || !Double.isFinite(v.z) || v.x<minX || v.x>maxX || v.z<minZ || v.z>maxZ) throw new IllegalArgumentException("point outside atlas");
            }
            total+=r.runs.length;
            for (int i=0;i<r.runs.length;i+=3) if(r.runs[i]<minX || (long)r.runs[i]+r.runs[i+2]>maxX || r.runs[i+1]<minZ || r.runs[i+1]>=maxZ || r.runs[i+2]<=0) throw new IllegalArgumentException("footprint outside atlas");
        }
        if(total>180000) throw new IllegalArgumentException("map complexity limit");
        if(JSON.toJson(this).getBytes(java.nio.charset.StandardCharsets.UTF_8).length>750000)throw new IllegalArgumentException("map definition exceeds network byte budget");
    }
    public static boolean inside(List<Point> polygon, double x, double z) {
        boolean hit=false;
        for(int i=0,j=polygon.size()-1;i<polygon.size();j=i++) {
            Point a=polygon.get(i),b=polygon.get(j);
            if((a.z>z)!=(b.z>z) && x<(b.x-a.x)*(z-a.z)/(b.z-a.z)+a.x) hit=!hit;
        }
        return hit;
    }
    public double[] bounds(Set<String> unlocked, boolean editor) {
        if(editor)return new double[]{minX,minZ,maxX,maxZ};
        double[] b={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        for(Region r:regions) if(unlocked.contains(r.id)) {
            int pad=r.expansion;
            for(var p:r.polygons)for(Point v:p)extend(b,v.x,v.z,v.x,v.z);
            for(int i=0;i<r.runs.length;i+=3)extend(b,r.runs[i]-pad,r.runs[i+1]-pad,r.runs[i]+r.runs[i+2]+pad,r.runs[i+1]+1+pad);
        }
        if(!Double.isFinite(b[0]))return null;
        b[0]=Math.max(minX,b[0]);b[1]=Math.max(minZ,b[1]);b[2]=Math.min(maxX,b[2]);b[3]=Math.min(maxZ,b[3]);return b;
    }
    private static void extend(double[] b,double x,double z,double X,double Z){b[0]=Math.min(b[0],x);b[1]=Math.min(b[1],z);b[2]=Math.max(b[2],X);b[3]=Math.max(b[3],Z);}
}
