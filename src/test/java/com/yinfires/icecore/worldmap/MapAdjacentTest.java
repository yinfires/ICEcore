package com.yinfires.icecore.worldmap;

import org.junit.jupiter.api.Test;
import java.util.*;
import static com.yinfires.icecore.worldmap.MapDefinition.Point;
import static org.junit.jupiter.api.Assertions.*;

class MapAdjacentTest {
    private MapDefinition map(){var d=new MapDefinition();d.minX=-256;d.minZ=-256;d.maxX=512;d.maxZ=512;return d;}
    private MapDefinition.Region rect(String id,int x,int z,int w,int h){var r=new MapDefinition.Region();r.id=id;r.polygons.add(List.of(new Point(x,z),new Point(x+w,z),new Point(x+w,z+h),new Point(x,z+h)));return r;}
    @Test void unlockedCoreWinsOverlapAndNeighborProtectionNeverErodesIt(){
        var d=map();d.regions.add(rect("a",0,0,100,100));d.regions.add(rect("b",80,0,100,100));var f=MapFog.build(d,Set.of("a"));
        for(double x:new double[]{1,79.9,80,99.9})assertEquals(255-(int)Math.round(255*MapFog.smooth(Math.min(x,100-x)/50)),f.alpha(x,50),"inward overlap x="+x);
        for(double x:new double[]{100.01,110,179.9})assertEquals(255,f.alpha(x,50),"locked x="+x);
    }
    @Test void commonEdgeHasNoSeamAfterBothUnlockEvenWithDifferentFeather(){
        var d=map();d.regions.add(rect("a",0,0,100,100));var b=rect("b",100,0,100,100);b.expansion=0;b.feather=12;d.regions.add(b);var f=MapFog.build(d,Set.of("a","b"));
        for(double x=99.5;x<100.5;x+=.05)assertEquals(0,f.alpha(x,50),"common edge "+x);
        var single=MapFog.build(d,Set.of("a"));assertEquals(255,single.alpha(100,50));assertTrue(single.alpha(99,50)>single.alpha(90,50));assertEquals(255,single.alpha(100.01,50));
    }
    @Test void tinyAndBoundFootprintsHonorOpenPriority(){
        var d=map();var a=rect("a",0,0,1,1);var b=rect("b",0,0,20,20);d.regions.add(a);d.regions.add(b);var f=MapFog.build(d,Set.of("a"));
        assertEquals(0,f.alpha(.5,.5));assertEquals(255,f.alpha(1.5,.5));
        a.polygons.clear();a.expansion=0;a.runs=new int[]{0,0,1};f=MapFog.build(d,Set.of("a"));assertEquals(0,f.alpha(.5,.5));assertEquals(255,f.alpha(1.5,.5));
    }
    @Test void tracePreservesDirectionAndInsertsTwoInteriorEndpointsOnSameEdge(){
        var d=map();d.regions.add(rect("a",0,0,100,100));
        var a=MapPolygonEditor.snap(d,"other",20,0,.1);var b=MapPolygonEditor.snap(d,"other",80,0,.1);
        assertNotNull(a);assertNotNull(b);assertEquals(List.of(new Point(20,0),new Point(80,0)),MapPolygonEditor.boundary(d,a,b,false));
        assertEquals(6,MapPolygonEditor.boundary(d,a,b,true).size());
        var next=d.copy();MapPolygonEditor.insert(next,a);MapPolygonEditor.insert(next,b);
        assertEquals(List.of(new Point(0,0),new Point(20,0),new Point(80,0),new Point(100,0),new Point(100,100),new Point(0,100)),next.region("a").polygons.get(0));
        assertEquals(4,d.region("a").polygons.get(0).size());MapPolygonEditor.validateChanged(d,next);
    }
    @Test void sharedVertexMoveIsTransactionalAndDoesNotTouchAnIndependentVertex(){
        var d=map();d.regions.add(rect("a",0,0,100,100));d.regions.add(rect("b",100,0,100,100));var next=d.copy();
        MapPolygonEditor.moveShared(next,new Point(100,0),new Point(110,-10));MapPolygonEditor.validateChanged(d,next);
        assertTrue(next.region("a").polygons.get(0).contains(new Point(110,-10)));assertTrue(next.region("b").polygons.get(0).contains(new Point(110,-10)));
        assertTrue(d.region("a").polygons.get(0).contains(new Point(100,0)));
        assertFalse(next.region("b").polygons.get(0).contains(new Point(0,0)));
    }
    @Test void invalidContoursAreRejectedBeforeCommit(){
        assertThrows(IllegalArgumentException.class,()->MapPolygonEditor.validate(List.of(new Point(0,0),new Point(10,10),new Point(0,10),new Point(10,0))));
        assertThrows(IllegalArgumentException.class,()->MapPolygonEditor.validate(List.of(new Point(0,0),new Point(10,0),new Point(10,10),new Point(0,0))));
        assertThrows(IllegalArgumentException.class,()->MapPolygonEditor.validate(List.of(new Point(0,0),new Point(1,1),new Point(2,2))));
    }
    @Test void preservedViewportOnlyClampsWhenUnlockBoundsChange(){
        var v=new MapViewport();v.resize(500,300,new double[]{0,0,1000,1000},true);v.scale=2;v.x=500;v.z=600;
        v.resize(500,300,new double[]{0,0,2000,2000},false);assertEquals(2,v.scale);assertEquals(500,v.x);assertEquals(600,v.z);
        v.resize(500,300,new double[]{0,0,100,100},false);assertEquals(50,v.x);assertEquals(50,v.z);
    }

    @Test void transitionWaitsForTilesUses300msSmoothstepAndCancelsOnReadinessLoss(){
        var t=new MapRevealTransition();assertEquals(0,t.opacity(false,10));assertEquals(0,t.opacity(true,100));
        assertEquals(.5,t.opacity(true,150000100),1e-9);assertEquals(1,t.opacity(true,300000100));
        t.reset();assertEquals(0,t.opacity(true,400000100));assertEquals(0,t.opacity(false,500000100));
        assertEquals(0,t.opacity(true,600000100));assertEquals(1,t.opacity(true,900000100));
    }
    @Test void footprintMergesRowsWithoutInternalSeamsAndKeepsHolesAndNegativeTiles(){
        var f=new MapFootprint(new int[]{-2,-2,4,-2,-1,4,-2,0,1,1,0,1,-2,1,4});
        assertTrue(f.contains(-1.5,-1.5));assertFalse(f.contains(0.5,.5));assertTrue(f.contains(-1.5,.5));
        var lines=new ArrayList<MapFootprint.Segment>();for(int z=-1;z<=0;z++)for(int x=-1;x<=0;x++)lines.addAll(f.tile(x,z));
        assertFalse(lines.stream().anyMatch(l->l.z()==-1&&l.endZ()==-1),"No horizontal seam between matching rows");
        assertEquals(2,lines.stream().filter(l->l.z()==0&&l.endZ()==0&&l.x()>=-1&&l.endX()<=1).mapToInt(l->l.endX()-l.x()).sum(),"Hole remains outlined across tile split");
        var rng=new Random(42);int[] runs=new int[300];for(int i=0;i<runs.length;i+=3){runs[i]=rng.nextInt(20)-10;runs[i+1]=rng.nextInt(20)-10;runs[i+2]=rng.nextInt(5)+1;}
        var indexed=new MapFootprint(runs);for(int z=-12;z<16;z++)for(int x=-12;x<16;x++){
            boolean expected=false;for(int i=0;i<runs.length;i+=3)expected|=z==runs[i+1]&&x>=runs[i]&&x<runs[i]+runs[i+2];
            assertEquals(expected,indexed.contains(x+.5,z+.5));
        }
    }
    @Test void coarseFieldKeepsSubCellNarrowAreasAndOppositeWindingOverlaps(){
        var d=map();d.minX=-8192;d.minZ=-8192;d.maxX=8192;d.maxZ=8192;
        var a=rect("a",0,0,1,100);var b=rect("b",0,0,20,100);var reversed=new ArrayList<>(b.polygons.get(0));Collections.reverse(reversed);b.polygons.set(0,reversed);
        d.regions.add(a);d.regions.add(b);var f=MapFog.build(d,Set.of("a"));
        for(double z=.1;z<100;z+=.9){assertTrue(f.alpha(.5,z)<255);assertEquals(255,f.alpha(1.1,z));}
    }
    @Test void changedSecondaryContourFailureLeavesOriginalTransactionUntouched(){
        var d=map();d.regions.add(rect("a",0,0,100,100));d.regions.add(rect("b",100,0,100,100));var next=d.copy();
        MapPolygonEditor.moveShared(next,new Point(100,0),new Point(200,100));
        assertThrows(IllegalArgumentException.class,()->MapPolygonEditor.validateChanged(d,next));
        assertEquals(new Point(100,0),d.region("a").polygons.get(0).get(1));
        var start=MapPolygonEditor.snap(d,"other",50,0,6);var end=MapPolygonEditor.snap(d,"other",100,50,6,start);
        assertEquals(List.of(new Point(50,0),new Point(100,0),new Point(100,50)),MapPolygonEditor.boundary(d,start,end,false));
        assertNull(MapPolygonEditor.snap(d,"other",50,-6.01,6));assertNotNull(MapPolygonEditor.snap(d,"other",50,-6,6));
    }
    @Test void previewCannotSendNetworkWritesOrMutateServerState() throws Exception {
        var node=new org.objectweb.asm.tree.ClassNode();
        try(var in=java.nio.file.Files.newInputStream(java.nio.file.Path.of("build/classes/java/main/com/yinfires/icecore/worldmap/client/MapUnlockPreviewScreen.class"))){new org.objectweb.asm.ClassReader(in).accept(node,0);}
        for(var m:node.methods)for(var instruction:m.instructions){
            if(instruction instanceof org.objectweb.asm.tree.MethodInsnNode call)assertFalse(call.owner.endsWith("/MapNetwork")||call.owner.endsWith("/MapService")||call.owner.endsWith("/MapSavedData"),"Preview must remain local");
            if(instruction instanceof org.objectweb.asm.tree.FieldInsnNode field&&field.getOpcode()==org.objectweb.asm.Opcodes.PUTSTATIC)assertFalse(field.owner.endsWith("/MapClient"));
        }
    }
    @Test void renderAdjacentFogPreviewFromFinalAlgorithm() throws Exception {
        var d=map();d.regions.add(rect("a",0,0,100,140));d.regions.add(rect("b",100,0,100,140));d.regions.add(rect("islet",220,30,4,6));
        var image=new java.awt.image.BufferedImage(900,300,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();int panel=0;
        for(var ids:List.of(Set.of("a"),Set.of("b"),Set.of("a","b","islet"))){var f=MapFog.build(d,ids);
            for(int z=0;z<300;z++)for(int x=0;x<300;x++){double wx=x-40,wz=z-70;int alpha=f.alpha(wx,wz);int terrain=((x/12+z/12)%2==0)?0x487d46:0x538a50;
                int r=((terrain>>16&255)*(255-alpha)+128*alpha)/255,b=((terrain&255)*(255-alpha)+128*alpha)/255,gr=((terrain>>8&255)*(255-alpha)+128*alpha)/255;
                image.setRGB(panel*300+x,z,r<<16|gr<<8|b);}
            g.setColor(java.awt.Color.WHITE);g.drawString(panel==0?"A unlocked":panel==1?"B unlocked":"A + B + islet unlocked",panel*300+10,20);panel++;
        }
        g.dispose();var path=java.nio.file.Path.of("tmp/map-inspect/adjacent-fog-preview.png");java.nio.file.Files.createDirectories(path.getParent());javax.imageio.ImageIO.write(image,"PNG",path.toFile());
    }
    @Test void snappingMovedSharedVertexOntoItsNeighborEdgeNeverInsertsDuplicate(){
        var d=map();d.regions.add(rect("a",0,0,100,100));d.regions.add(rect("b",100,0,100,100));var next=d.copy();
        var hit=MapPolygonEditor.snap(next,"a",110,2,6);assertNotNull(hit);
        MapPolygonEditor.placeVertex(next,new Point(100,0),hit.point(),hit,true);MapPolygonEditor.validateChanged(d,next);
        assertEquals(4,next.region("b").polygons.get(0).size());assertEquals(1,next.region("b").polygons.get(0).stream().filter(hit.point()::equals).count());
    }
}
