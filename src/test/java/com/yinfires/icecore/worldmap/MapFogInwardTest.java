package com.yinfires.icecore.worldmap;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;
import static com.yinfires.icecore.worldmap.MapDefinition.Point;

class MapFogInwardTest {
    private MapDefinition map(){var d=new MapDefinition();d.minX=-1024;d.minZ=-1024;d.maxX=1024;d.maxZ=1024;return d;}
    private MapDefinition.Region rect(String id,int x,int z,int w,int h,int feather){var r=new MapDefinition.Region();r.id=id;r.feather=feather;r.expansion=0;r.polygons.add(List.of(new Point(x,z),new Point(x+w,z),new Point(x+w,z+h),new Point(x,z+h)));return r;}
    @Test void fractionalContourIsOutermostFogBoundaryRegardlessOfLegacyExpansion(){
        var d=map();var r=rect("a",0,0,256,256,64);r.expansion=512;r.polygons.set(0,List.of(new Point(.25,.75),new Point(256.25,.75),new Point(256.25,256.75),new Point(.25,256.75)));d.regions.add(r);
        var f=MapFog.build(d,Set.of("a"));for(double x:new double[]{-64,0,.24,.25})assertEquals(255,f.alpha(x,128));
        assertEquals(127,f.alpha(32.25,128));assertEquals(0,f.alpha(64.25,128));assertEquals(255,f.alpha(257,128));
        assertArrayEquals(new double[]{.25,.75,256.25,256.75},d.bounds(Set.of("a"),false));
    }
    @Test void unlockedUnionRemovesSeamAndLockedSideStaysOpaqueWithSoftOpenSide(){
        var d=map();d.regions.add(rect("a",0,0,256,256,32));d.regions.add(rect("b",256,0,256,256,64));
        var single=MapFog.build(d,Set.of("a"));assertEquals(0,single.alpha(224,128));assertEquals(127,single.alpha(240,128));assertEquals(255,single.alpha(256,128));assertEquals(255,single.alpha(270,128));
        var both=MapFog.build(d,Set.of("a","b"));for(double x=250;x<262;x+=.1)assertEquals(0,both.alpha(x,128));
        assertEquals(127,both.alpha(480,128));assertEquals(0,both.alpha(256,64));assertEquals(255,both.alpha(256,0));
    }
    @Test void projectionPaddingCannotRevealLockedCoreButOverlappingOpenCoreWins(){
        var d=map();var bound=rect("bound",0,0,1,1,16);bound.polygons.clear();bound.runs=new int[]{0,0,100};bound.expansion=128;d.regions.add(bound);
        d.regions.add(rect("locked",80,0,100,100,16));var f=MapFog.build(d,Set.of("bound"));
        assertTrue(f.alpha(90,.5)<255);assertEquals(255,f.alpha(110,20));assertTrue(f.alpha(110,-8)>0);assertEquals(255,f.alpha(400,0));
    }
    @Test void holesAndSinglePixelFragmentsSurviveWithoutRasterLeakOrInternalRowSeam(){
        var d=map();var r=rect("ring",0,0,1,1,4);r.polygons.clear();var runs=new ArrayList<Integer>();
        for(int z=0;z<32;z++){if(z<8||z>=24){runs.add(0);runs.add(z);runs.add(32);}else{runs.add(0);runs.add(z);runs.add(8);runs.add(24);runs.add(z);runs.add(8);}}
        r.runs=runs.stream().mapToInt(Integer::intValue).toArray();d.regions.add(r);var tiny=rect("tiny",100,0,1,1,64);d.regions.add(tiny);var f=MapFog.build(d,Set.of("ring","tiny"));
        assertEquals(255,f.alpha(16,16));assertEquals(127,f.alpha(6,16));assertEquals(0,f.alpha(4,16));
        assertEquals(0,f.alpha(100.5,.5));assertEquals(255,f.alpha(101.01,.5));
        assertEquals(f.alpha(4,15.9999),f.alpha(4,16.0001));
    }
    @Test void commonEdgePickingProjectsToLineRatherThanSnappingToNearbyYellowVertex(){
        var d=map();d.regions.add(rect("a",0,0,100,100,32));
        var edge=MapPolygonEditor.edge(d,"other",3,1,6,null);var vertex=MapPolygonEditor.snap(d,"other",3,1,6);
        assertEquals(new Point(3,0),edge.point());assertEquals(new Point(0,0),vertex.point());
        var end=MapPolygonEditor.edge(d,"other",100,50,6,edge);assertNotNull(end);
        var points=new ArrayList<Point>();var next=d.copy();var path=MapPolygonEditor.boundary(next,edge,end,false);
        assertTrue(points.isEmpty(),"Picking two edge endpoints must not add draw vertices");
        MapPolygonEditor.insert(next,edge);MapPolygonEditor.insert(next,end);points.addAll(path);
        assertEquals(MapEditMode.SHARED_EDGE,MapEditMode.SHARED_EDGE.afterAcceptingEdge());assertFalse(MapEditMode.SHARED_EDGE.showsVertices());assertTrue(MapEditMode.POINTS.showsVertices());
        assertEquals(List.of(new Point(3,0),new Point(100,0),new Point(100,50)),points);assertEquals(4,d.region("a").polygons.get(0).size());
    }
    @Test void triangleDistanceUsesEuclideanDiagonalAndDoesNotCreateStairSteps(){
        var d=map();var r=rect("a",0,0,256,256,16);r.polygons.set(0,List.of(new Point(0,0),new Point(256,0),new Point(0,256)));d.regions.add(r);var f=MapFog.build(d,Set.of("a"));
        assertEquals(255,f.alpha(150,150));for(double x=100;x<120;x+=.13){double z=256-x-8*Math.sqrt(2);assertEquals(127,f.alpha(x,z),1);}
    }
    @Test void manyFootprintRowsAndDetachedFragmentsStayBoundedOffThread() throws Exception {
        var d=map();var r=rect("r",0,0,1,1,16);r.polygons.clear();r.runs=new int[60000];for(int i=0;i<20000;i++){r.runs[i*3]=(i%200)*8-800;r.runs[i*3+1]=(i/200)*8-400;r.runs[i*3+2]=4;}d.regions.add(r);
        long start=System.nanoTime();var f=MapFog.build(d,Set.of("r"));long built=System.nanoTime();long checksum=0;
        for(int z=-512;z<512;z+=2)for(int x=-512;x<512;x+=2)checksum+=f.alpha(x+.5,z+.5);
        assertTrue(checksum>0);long sampled=System.nanoTime();Files.writeString(Path.of("tmp/map-inspect/inward-performance.txt"),"20000 fragments build ms="+(built-start)/1000000+"; 262144 samples ms="+(sampled-built)/1000000);
    }
    @Test void finalSamplerProducesInwardAndMergedBoundaryPreview() throws Exception {
        var d=map();d.regions.add(rect("a",0,0,160,240,32));d.regions.add(rect("b",160,0,160,240,32));d.regions.add(rect("small",340,30,8,12,32));
        BufferedImage image=new BufferedImage(1200,340,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();int panel=0;
        for(var ids:List.of(Set.of("a"),Set.of("b"),Set.of("a","b","small"))){var f=MapFog.build(d,ids);
            for(int z=0;z<340;z++)for(int x=0;x<400;x++){int alpha=f.alpha(x-24,z-45);int terrain=((x/12+z/12)%2==0)?0x47794d:0x548b55;int r=((terrain>>16&255)*(255-alpha)+128*alpha)/255,b=((terrain&255)*(255-alpha)+128*alpha)/255,gr=((terrain>>8&255)*(255-alpha)+128*alpha)/255;image.setRGB(panel*400+x,z,r<<16|gr<<8|b);}
            g.setColor(java.awt.Color.WHITE);g.drawString(panel==0?"A only: inward fade":panel==1?"B only: inward fade":"A + B: shared seam removed",panel*400+12,20);panel++;
        }
        g.dispose();javax.imageio.ImageIO.write(image,"PNG",Path.of("tmp/map-inspect/inward-fog-preview.png").toFile());
    }
    @Test void overlappingEdgeSourcesAreExplicitAndEndpointStaysOnChosenSource(){
        var d=map();d.regions.add(rect("a",0,0,100,100,32));d.regions.add(rect("b",0,0,100,100,32));
        var choices=MapPolygonEditor.edgeChoices(d,"other",30,1,6,1,null);assertEquals(Set.of("a","b"),choices.stream().map(MapPolygonEditor.Hit::region).collect(java.util.stream.Collectors.toSet()));
        var chosen=choices.stream().filter(h->h.region().equals("a")).findFirst().orElseThrow();
        var ends=MapPolygonEditor.edgeChoices(d,"other",90,1,6,1,chosen);assertEquals(1,ends.size());assertEquals("a",ends.get(0).region());
        assertEquals(List.of(new Point(30,0),new Point(90,0)),MapPolygonEditor.boundary(d,chosen,ends.get(0),false));
        assertTrue(MapPolygonEditor.edgeChoices(d,"other",30,-6.01,6,1,null).isEmpty());
    }
    @Test void unlockedIslandInAnotherRegionsHoleUsesItsOwnFeather(){
        var d=map();var r=rect("ring",0,0,1,1,4);r.polygons.clear();var runs=new ArrayList<Integer>();
        for(int z=0;z<100;z++){if(z<20||z>=80){runs.add(0);runs.add(z);runs.add(100);}else{runs.add(0);runs.add(z);runs.add(20);runs.add(80);runs.add(z);runs.add(20);}}
        r.runs=runs.stream().mapToInt(Integer::intValue).toArray();d.regions.add(r);d.regions.add(rect("inner",40,40,20,20,64));
        var f=MapFog.build(d,Set.of("ring","inner"));assertEquals(127,f.alpha(45,50));assertEquals(255,f.alpha(30,50));assertEquals(0,f.alpha(50,50));
    }
}
