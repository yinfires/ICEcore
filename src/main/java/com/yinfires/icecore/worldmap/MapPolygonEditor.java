package com.yinfires.icecore.worldmap;

import java.util.*;
import static com.yinfires.icecore.worldmap.MapDefinition.Point;

/** Transactional polygon editing. Shared coordinates are the topology; no extra file format. */
public final class MapPolygonEditor {
    public record Hit(String region,int polygon,int edge,double fraction,Point point){}
    private MapPolygonEditor(){}
    public static Hit snap(MapDefinition d,String excluded,double x,double z,double radius){
        return snap(d,excluded,x,z,radius,null);
    }
    public static Hit snap(MapDefinition d,String excluded,double x,double z,double radius,Hit source){return snap(d,excluded,x,z,radius,source,true);}
    public static Hit edge(MapDefinition d,String excluded,double x,double z,double radius,Hit source){return snap(d,excluded,x,z,radius,source,false);}
    private static Hit snap(MapDefinition d,String excluded,double x,double z,double radius,Hit source,boolean vertices){
        Hit best=null;double limit=radius*radius;
        for(var r:d.regions)if(!r.id.equals(excluded))for(int p=0;p<r.polygons.size();p++){
            if(source!=null&&(!source.region.equals(r.id)||source.polygon!=p))continue;
            var polygon=r.polygons.get(p);
            for(int i=0;i<polygon.size();i++){
                var a=polygon.get(i);var b=polygon.get((i+1)%polygon.size());double dx=b.x()-a.x(),dz=b.z()-a.z(),length=dx*dx+dz*dz;
                double t=length==0?0:Math.max(0,Math.min(1,((x-a.x())*dx+(z-a.z())*dz)/length));
                // Vertices win within the same six-pixel attraction radius.
                if(vertices&&Math.hypot(x-a.x(),z-a.z())<=radius)t=0;else if(vertices&&Math.hypot(x-b.x(),z-b.z())<=radius)t=1;
                double px=a.x()+t*dx,pz=a.z()+t*dz,distance=(px-x)*(px-x)+(pz-z)*(pz-z);
                if(distance<=limit){limit=distance;best=new Hit(r.id,p,i,t,t==0?a:t==1?b:new Point(px,pz));}
            }
        }
        return best;
    }
    /** One nearest hit per source contour; ambiguous overlapping edges require explicit choice. */
    public static List<Hit> edgeChoices(MapDefinition d,String excluded,double x,double z,double radius,double ambiguity,Hit source){
        var choices=new ArrayList<Hit>();double nearest=Double.POSITIVE_INFINITY;
        for(var r:d.regions)if(!r.id.equals(excluded))for(int p=0;p<r.polygons.size();p++){
            if(source!=null&&(!source.region.equals(r.id)||source.polygon!=p))continue;
            var polygon=r.polygons.get(p);Hit best=null;double distance=radius*radius;
            for(int i=0;i<polygon.size();i++){
                var a=polygon.get(i);var b=polygon.get((i+1)%polygon.size());double dx=b.x()-a.x(),dz=b.z()-a.z(),length=dx*dx+dz*dz;
                double t=length==0?0:Math.max(0,Math.min(1,((x-a.x())*dx+(z-a.z())*dz)/length)),px=a.x()+t*dx,pz=a.z()+t*dz,q=(px-x)*(px-x)+(pz-z)*(pz-z);
                if(q<=distance){distance=q;best=new Hit(r.id,p,i,t,new Point(px,pz));}
            }
            if(best!=null){choices.add(best);nearest=Math.min(nearest,Math.sqrt(distance));}
        }
        double limit=nearest+ambiguity;choices.removeIf(hit->Math.hypot(hit.point.x()-x,hit.point.z()-z)>limit);return List.copyOf(choices);
    }
    public static void insert(MapDefinition d,Hit hit){
        var r=d.region(hit.region());if(r==null)throw new IllegalArgumentException("missing source");
        var polygon=new ArrayList<>(r.polygons.get(hit.polygon()));
        if(polygon.contains(hit.point()))return;
        int edge=-1;for(int i=0;i<polygon.size();i++){var a=polygon.get(i);var b=polygon.get((i+1)%polygon.size());if(java.awt.geom.Line2D.ptSegDist(a.x(),a.z(),b.x(),b.z(),hit.point.x(),hit.point.z())<1e-7){edge=i;break;}}
        if(edge<0)throw new IllegalArgumentException("source edge changed");
        polygon.add(edge+1,hit.point());r.polygons.set(hit.polygon(),List.copyOf(polygon));
    }
    public static List<Point> boundary(MapDefinition d,Hit start,Hit end,boolean reverse){
        if(!start.region.equals(end.region)||start.polygon!=end.polygon)throw new IllegalArgumentException("same source contour required");
        var polygon=d.region(start.region).polygons.get(start.polygon);var path=new ArrayList<Point>();path.add(start.point);
        double a=start.edge+start.fraction,b=end.edge+end.fraction;int n=polygon.size();
        if(reverse){if(b>=a)b-=n;for(int i=(int)Math.ceil(a)-1;i>b;i--)path.add(polygon.get(Math.floorMod(i,n)));}
        else{if(b<=a)b+=n;for(int i=(int)Math.floor(a)+1;i<b;i++)path.add(polygon.get(i%n));}
        path.add(end.point);return List.copyOf(path);
    }
    public static void placeVertex(MapDefinition d,Point from,Point to,Hit hit,boolean linked){
        if(linked)moveShared(d,from,to);
        if(hit!=null)insert(d,hit);
    }
    public static void moveShared(MapDefinition d,Point from,Point to){
        for(var r:d.regions)for(int i=0;i<r.polygons.size();i++){
            var p=r.polygons.get(i);if(!p.contains(from))continue;
            var next=new ArrayList<>(p);for(int j=0;j<next.size();j++)if(next.get(j).equals(from))next.set(j,to);r.polygons.set(i,List.copyOf(next));
        }
    }
    public static void validate(List<Point> p){
        if(p.size()<3||p.size()>512||new HashSet<>(p).size()!=p.size())throw new IllegalArgumentException("duplicate vertices");
        double area=0;for(int i=0;i<p.size();i++){var a=p.get(i);var b=p.get((i+1)%p.size());area+=a.x()*b.z()-b.x()*a.z();}
        if(Math.abs(area)<1e-6)throw new IllegalArgumentException("empty outline");
        for(int i=0;i<p.size();i++)for(int j=i+1;j<p.size();j++){
            if(j==i+1||i==0&&j==p.size()-1)continue;
            var a=p.get(i);var b=p.get((i+1)%p.size());var c=p.get(j);var e=p.get((j+1)%p.size());
            if(java.awt.geom.Line2D.linesIntersect(a.x(),a.z(),b.x(),b.z(),c.x(),c.z(),e.x(),e.z()))throw new IllegalArgumentException("self intersection");
        }
    }
    public static void validateChanged(MapDefinition before,MapDefinition after){
        for(var r:after.regions){var old=before.region(r.id);Set<List<Point>> unchanged=old==null?Set.of():new HashSet<>(old.polygons);for(var p:r.polygons)if(!unchanged.contains(p))validate(p);}
        after.validate();
    }
}
