package com.yinfires.icecore.worldmap;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;

class MapCaptureTest {
    @Test void reportedScreenshotCornerIsOutsideTheActualRegionAndItsSoftBorder(){
        var d=new MapDefinition();d.minX=-196;d.minZ=-184;d.maxX=512;d.maxZ=512;
        var r=new MapDefinition.Region();r.id="1";
        r.polygons.add(List.of(new MapDefinition.Point(65.321774,-13.405156),new MapDefinition.Point(39.09089,-48.756428),
            new MapDefinition.Point(-7.865632,-55.2429),new MapDefinition.Point(-44.783173,-33.837542),
            new MapDefinition.Point(-67.451839,-3.999772),new MapDefinition.Point(-62.697896,35.46371),
            new MapDefinition.Point(-46.626743,66.669984),new MapDefinition.Point(-15.322396,87.480748),
            new MapDefinition.Point(27.04642,79.102388),new MapDefinition.Point(107.736075,27.210613),new MapDefinition.Point(68.220681,-10.750147)));
        d.regions.add(r);var plan=MapCapturePlan.build(d);
        assertFalse(plan.required(-13,-12));assertFalse(plan.required(31,31));assertTrue(plan.required(0,0));
    }
    @Test void unusedCornersAndOutsideHandDrawnContourAreExcludedButFutureRegionsRequired(){
        var d=new MapDefinition();d.minX=-256;d.minZ=-256;d.maxX=1024;d.maxZ=1024;
        var main=MapRulesTest.square("main",-64,-48,96);main.expansion=64;main.feather=64;d.regions.add(main);
        var tiny=MapRulesTest.square("future",700,700,1);tiny.expansion=0;tiny.feather=1;d.regions.add(tiny);
        var plan=MapCapturePlan.build(d);
        assertFalse(plan.required(-13,-12));assertFalse(plan.required(63,0));
        assertTrue(plan.required(0,0));assertFalse(plan.required(7,0));assertTrue(plan.required(43,43));
        assertFalse(plan.intersects(512,0,1024,512));assertTrue(plan.intersects(512,512,1024,1024));
        assertTrue(plan.count()>0);
    }
    @Test void coverageContainsEveryPixelThatAnyFutureUnlockCouldShowIncludingAcrossChunkBoundaries(){
        var d=new MapDefinition();d.minX=-144;d.minZ=-144;d.maxX=144;d.maxZ=144;
        var island=MapRulesTest.square("tiny",-17,-1,1);island.expansion=5;island.feather=9;d.regions.add(island);
        var field=MapFog.build(d,Set.of("tiny"));var plan=MapCapturePlan.build(d);
        for(int z=d.minZ;z<d.maxZ;z++)for(int x=d.minX;x<d.maxX;x++)if(field.alpha(x+.5,z+.5)<255)assertTrue(plan.required(Math.floorDiv(x,16),Math.floorDiv(z,16)),x+","+z);
    }
    @Test void rangeOnlyExportBeforeDrawingRequiresTheExplicitFullRectangle(){var plan=MapCapturePlan.build(new MapDefinition());assertEquals(32*32,plan.count());assertTrue(plan.required(0,0));assertTrue(plan.required(31,31));}
    @Test void nativeDiskLoaderMarksLoadedWithoutMarkingWrittenAndSnapshotUsesLoaded() throws Exception {
        Set<String> nativeCalls=new HashSet<>(),snapshotCalls=new HashSet<>();
        try(var jar=new JarFile("libs/xaeroworldmap-forge-1.20.1-1.47.0.jar");var in=jar.getInputStream(jar.getEntry("xaero/map/file/MapSaveLoad.class"))){collectTileCalls(new ClassReader(in),nativeCalls);}
        assertTrue(nativeCalls.contains("setLoaded(Z)V"));assertFalse(nativeCalls.contains("setWrittenOnce(Z)V"));
        try(var in=Files.newInputStream(Path.of("build/classes/java/main/com/yinfires/icecore/worldmap/client/XaeroSnapshot$Job$1.class"))){collectTileCalls(new ClassReader(in),snapshotCalls);}
        assertTrue(snapshotCalls.contains("isLoaded()Z"));assertFalse(snapshotCalls.contains("wasWrittenOnce()Z"));
    }
    private static void collectTileCalls(ClassReader reader,Set<String> result){
        reader.accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int access,String name,String desc,String signature,String[] exceptions){return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int opcode,String owner,String name,String desc,boolean itf){if(owner.equals("xaero/map/region/MapTile"))result.add(name+desc);}};}},0);
    }
    @Test void icecoreDoesNotSubscribeToOrReplaceXaeroScreenOpening() throws Exception {
        try(var in=Files.newInputStream(Path.of("build/classes/java/main/com/yinfires/icecore/worldmap/client/MapClient$Events.class"))){
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int access,String name,String desc,String signature,String[] exceptions){assertFalse(desc.contains("ScreenEvent$Opening"));return new MethodVisitor(Opcodes.ASM9){@Override public void visitMethodInsn(int opcode,String owner,String method,String descriptor,boolean itf){assertFalse(owner.contains("ScreenEvent$Opening")&&method.equals("setNewScreen"));}};}},0);
        }
    }
}
