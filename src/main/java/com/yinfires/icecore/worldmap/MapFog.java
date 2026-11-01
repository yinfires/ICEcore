package com.yinfires.icecore.worldmap;

import java.awt.BasicStroke;
import java.awt.geom.*;
import java.util.*;

/** Immutable union boundary, indexed once off-thread. Only background image jobs sample it. */
public final class MapFog {
    private final Area visible;
    private final Boundary boundary;
    private final ProfileNode profiles;
    private final double maximumFeather;
    private record Profile(Boundary shape,Boundary region,double feather){}
    private MapFog(Area visible,Boundary boundary,List<Profile> profiles){
        this.visible=visible;this.boundary=boundary;this.profiles=profiles.isEmpty()?null:new ProfileNode(profiles.toArray(Profile[]::new),0,profiles.size());
        maximumFeather=profiles.stream().mapToDouble(Profile::feather).max().orElse(1);
    }
    public static MapFog build(MapDefinition d,Set<String> unlocked){
        Path2D.Double combined=new Path2D.Double(Path2D.WIND_NON_ZERO),actualOpen=new Path2D.Double(Path2D.WIND_NON_ZERO),actualLocked=new Path2D.Double(Path2D.WIND_NON_ZERO);
        for(var r:d.regions)(unlocked.contains(r.id)?actualOpen:actualLocked).append(shape(r,false),false);
        List<Profile> profiles=new ArrayList<>();
        for(var r:d.regions)if(unlocked.contains(r.id)){
            cancelled();Area region=shape(r,true);combined.append(region,false);
            // Normalized outer components allow tiny detached islands to retain an interior;
            // hole rings are omitted here, but retained by the global union membership test.
            var regionIndex=new Boundary(region);var iterator=region.getPathIterator(null,.25);double[] c=new double[6];Path2D.Double ring=null;double sx=0,sz=0,px=0,pz=0,signed=0;
            while(!iterator.isDone()){
                cancelled();int type=iterator.currentSegment(c);
                if(type==PathIterator.SEG_MOVETO){ring=new Path2D.Double();ring.moveTo(c[0],c[1]);sx=px=c[0];sz=pz=c[1];signed=0;}
                else if(type==PathIterator.SEG_LINETO){ring.lineTo(c[0],c[1]);signed+=px*c[1]-c[0]*pz;px=c[0];pz=c[1];}
                else if(type==PathIterator.SEG_CLOSE){ring.closePath();signed+=px*sz-sx*pz;if(signed<0){var bounds=ring.getBounds2D();double feather=Math.min(r.feather,Math.min(bounds.getWidth(),bounds.getHeight())/2);profiles.add(new Profile(new Boundary(ring),regionIndex,Math.max(.000001,feather)));}}
                iterator.next();
            }
        }
        cancelled();Area union=new Area(combined),protection=new Area(actualLocked);protection.subtract(new Area(actualOpen));union.subtract(protection);union.intersect(new Area(new Rectangle2D.Double(d.minX,d.minZ,d.maxX-d.minX,d.maxZ-d.minZ)));return new MapFog(union,new Boundary(union),profiles);
    }
    private static Area shape(MapDefinition.Region r,boolean padding){
        Path2D.Double source=new Path2D.Double(Path2D.WIND_NON_ZERO);
        for(var p:r.polygons){
            double area=0;for(int i=0;i<p.size();i++){var a=p.get(i);var b=p.get((i+1)%p.size());area+=a.x()*b.z()-b.x()*a.z();}
            for(int i=0;i<p.size();i++){var v=p.get(area<0?i:p.size()-1-i);if(i==0)source.moveTo(v.x(),v.z());else source.lineTo(v.x(),v.z());}source.closePath();
        }
        if(r.runs.length>0){
            Path2D.Double runs=new Path2D.Double(Path2D.WIND_NON_ZERO);
            for(int i=0;i<r.runs.length;i+=3){if((i&1023)==0)cancelled();runs.append(new Rectangle2D.Double(r.runs[i],r.runs[i+1],r.runs[i+2],1),false);}
            Area footprint=new Area(runs);
            if(padding&&r.expansion>0)footprint.add(new Area(new BasicStroke(2*r.expansion,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND).createStrokedShape(footprint)));
            source.append(footprint,false);
        }
        return new Area(source);
    }
    /** Profile lookup is spatially pruned, including maps containing thousands of fragments. */
    private static final class ProfileNode {
        final double x,z,X,Z,minimum;
        final ProfileNode left,right;
        final Profile[] values;final int first,last;
        ProfileNode(Profile[] values,int first,int last){
            cancelled();this.values=values;this.first=first;this.last=last;
            double x=Double.POSITIVE_INFINITY,z=x,X=-x,Z=-x,min=x;
            for(int i=first;i<last;i++){var n=values[i].shape.root;x=Math.min(x,n.minX);z=Math.min(z,n.minZ);X=Math.max(X,n.maxX);Z=Math.max(Z,n.maxZ);min=Math.min(min,values[i].feather);}
            this.x=x;this.z=z;this.X=X;this.Z=Z;minimum=min;
            if(last-first>8){boolean horizontal=X-x>Z-z;Arrays.sort(values,first,last,Comparator.comparingDouble(p->horizontal?p.shape.root.minX+p.shape.root.maxX:p.shape.root.minZ+p.shape.root.maxZ));int mid=(first+last)>>>1;left=new ProfileNode(values,first,mid);right=new ProfileNode(values,mid,last);}else left=right=null;
        }
        double feather(double px,double pz,double best){
            if(px<x||px>=X||pz<z||pz>=Z||minimum>=best)return best;
            if(left!=null)return right.feather(px,pz,left.feather(px,pz,best));
            for(int i=first;i<last;i++)if(values[i].feather<best&&values[i].shape.contains(px,pz)&&values[i].region.contains(px,pz))best=values[i].feather;return best;
        }
    }
    public int alpha(double x,double z){
        if(!boundary.contains(x,z))return 255;
        double depth=Math.sqrt(boundary.distanceSquared(x,z,maximumFeather*maximumFeather));
        double feather=profiles==null?maximumFeather:profiles.feather(x,z,maximumFeather);
        double visibility=smooth(depth/feather);
        return 255-(int)Math.round(255*visibility);
    }
    public boolean needsTerrain(int x0,int z0,int x1,int z1){return visible.intersects(x0,z0,x1-x0,z1-z0);}
    public static double smooth(double t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    private static void cancelled(){if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();}

    /** AABB hierarchy: exact parity membership and nearest union edge without per-sample allocation. */
    private static final class Boundary {
        private record Edge(double x,double z,double X,double Z){
            double minX(){return Math.min(x,X);}double minZ(){return Math.min(z,Z);}
            double maxX(){return Math.max(x,X);}double maxZ(){return Math.max(z,Z);}
        }
        private final Node root;
        Boundary(java.awt.Shape shape){
            var edges=new ArrayList<Edge>();var it=shape.getPathIterator(null,.25);double[] c=new double[6];double x=0,z=0,sx=0,sz=0;
            while(!it.isDone()){cancelled();int type=it.currentSegment(c);
                if(type==PathIterator.SEG_MOVETO){x=sx=c[0];z=sz=c[1];}
                else if(type==PathIterator.SEG_LINETO){if(x!=c[0]||z!=c[1])edges.add(new Edge(x,z,c[0],c[1]));x=c[0];z=c[1];}
                else if(type==PathIterator.SEG_CLOSE){if(x!=sx||z!=sz)edges.add(new Edge(x,z,sx,sz));x=sx;z=sz;}
                it.next();
            }
            root=edges.isEmpty()?null:new Node(edges.toArray(Edge[]::new),0,edges.size());
        }
        boolean contains(double x,double z){return root!=null&&root.crossings(x,z)%2!=0;}
        double distanceSquared(double x,double z,double limit){return root==null?limit:root.distance(x,z,limit);}
        private static final class Node {
            final double minX,minZ,maxX,maxZ;
            final Node left,right;
            final Edge[] edges;
            final int first,last;
            Node(Edge[] edges,int first,int last){
                cancelled();this.edges=edges;this.first=first;this.last=last;
                double x=Double.POSITIVE_INFINITY,z=x,X=-x,Z=-x;
                for(int i=first;i<last;i++){var e=edges[i];x=Math.min(x,e.minX());z=Math.min(z,e.minZ());X=Math.max(X,e.maxX());Z=Math.max(Z,e.maxZ());}
                minX=x;minZ=z;maxX=X;maxZ=Z;
                if(last-first>8){boolean horizontal=X-x>=Z-z;Arrays.sort(edges,first,last,Comparator.comparingDouble(e->horizontal?e.x+e.X:e.z+e.Z));int mid=(first+last)>>>1;left=new Node(edges,first,mid);right=new Node(edges,mid,last);}else{left=right=null;}
            }
            int crossings(double x,double z){
                if(z<minZ||z>=maxZ||x>=maxX)return 0;
                if(left!=null)return left.crossings(x,z)+right.crossings(x,z);
                int result=0;for(int i=first;i<last;i++){var e=edges[i];if((e.z>z)!=(e.Z>z)&&x<(e.X-e.x)*(z-e.z)/(e.Z-e.z)+e.x)result++;}return result;
            }
            double bound(double x,double z){double dx=Math.max(0,Math.max(minX-x,x-maxX)),dz=Math.max(0,Math.max(minZ-z,z-maxZ));return dx*dx+dz*dz;}
            double distance(double x,double z,double best){
                if(bound(x,z)>=best)return best;
                if(left!=null){Node a=left,b=right;if(a.bound(x,z)>b.bound(x,z)){a=right;b=left;}return b.distance(x,z,a.distance(x,z,best));}
                for(int i=first;i<last;i++){var e=edges[i];best=Math.min(best,Line2D.ptSegDistSq(e.x,e.z,e.X,e.Z,x,z));}return best;
            }
        }
    }
}
