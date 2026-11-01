package com.yinfires.icecore.worldmap;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;

class MapEditorTest {
    private MapDefinition draft(){var d=new MapDefinition();var r=new MapDefinition.Region();r.id="island";d.regions.add(r);return d;}
    private boolean enabled(String action,MapDefinition d,boolean drawing,int points,boolean exporting){return MapEditorRules.enabled(action,d,d.regions.isEmpty()?null:d.regions.get(0),"new_island",drawing,points,exporting,false,false,false);}
    @Test void firstOpenHasUsefulActionsWithoutAnyPublishedAtlas(){
        var d=new MapDefinition();assertEquals("unpublished",d.atlas);
        assertTrue(enabled("new",d,false,0,false));assertTrue(enabled("bounds_apply",d,false,0,false));
        for(String action:List.of("select","draw","edit_contour","remove_contour","bind","source","cancel_snapshot","finish","undo"))assertFalse(enabled(action,d,false,0,false),action);
    }
    @Test void drawingAndExportLockConflictingActionsButPermitCompletionAndCancellation(){
        var d=draft();
        for(String action:List.of("new","select","save","snapshot","draw","edit_contour","delete","bounds_apply"))assertFalse(enabled(action,d,true,3,false),action);
        assertTrue(enabled("finish",d,true,3,false));assertFalse(enabled("finish",d,true,2,false));
        assertTrue(enabled("undo",d,true,1,false));assertFalse(enabled("undo",d,true,0,false));assertTrue(enabled("cancel_draw",d,true,0,false));
        for(String action:List.of("new","select","save","snapshot","draw","delete"))assertFalse(enabled(action,d,false,0,true),action);
        assertTrue(enabled("cancel_snapshot",d,false,0,true));assertTrue(enabled("close",d,false,0,true));
    }
    @Test void finishExtendsAtlasExactlyToHandDrawnFogBoundaryAndKeepsSmallIslands(){
        var d=draft();var r=d.regions.get(0);
        MapEditorRules.finish(d,r,List.of(new MapDefinition.Point(-900,-700),new MapDefinition.Point(-800,-700),new MapDefinition.Point(-800,-600)),-1);
        assertEquals(-900,d.minX);assertEquals(-700,d.minZ);
        MapEditorRules.finish(d,r,List.of(new MapDefinition.Point(100,100),new MapDefinition.Point(101,100),new MapDefinition.Point(101,101)),-1);
        assertEquals(2,r.polygons.size());assertTrue(r.contains(100.75,100.25));
    }
    @Test void editingReplacesOnlyChosenContourAndRejectedDraftLeavesOriginalUntouched(){
        var d=draft();var points=List.of(new MapDefinition.Point(0,0),new MapDefinition.Point(10,0),new MapDefinition.Point(10,10));
        MapEditorRules.finish(d,d.regions.get(0),points,-1);var next=d.copy();
        MapEditorRules.finish(next,next.regions.get(0),List.of(new MapDefinition.Point(20,20),new MapDefinition.Point(30,20),new MapDefinition.Point(30,30)),0);
        assertEquals(1,next.regions.get(0).polygons.size());assertEquals(points,d.regions.get(0).polygons.get(0));
        var bad=d.copy();assertThrows(IllegalArgumentException.class,()->MapEditorRules.finish(bad,bad.regions.get(0),List.of(new MapDefinition.Point(20000,0),new MapDefinition.Point(20010,0),new MapDefinition.Point(20010,10)),0));
        assertEquals(points,d.regions.get(0).polygons.get(0));
        assertThrows(IllegalArgumentException.class,()->MapEditorRules.finish(d.copy(),new MapDefinition.Region(),List.of(new MapDefinition.Point(0,0),new MapDefinition.Point(1,1),new MapDefinition.Point(2,2)),-1));
    }
    @Test void everyToolAndFieldHasAnExplicitTranslatedDescription() throws Exception {
        for(String lang:List.of("zh_cn","en_us")){
            var json=JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/icecore/lang/"+lang+".json"),StandardCharsets.UTF_8)).getAsJsonObject();
            var keys=new ArrayList<>(MapEditorRules.ACTIONS);keys.addAll(List.of("region_id","expansion","feather","bounds","search","choice","back"));
            for(String key:keys){String full="icecore.map.help."+key;assertTrue(json.has(full),lang+":"+full);assertTrue(json.get(full).getAsString().length()>12,full);}
        }
    }
    @Test void pinnedNativeMapProvidesExactCameraFieldsAndMinecraftVirtualRenderEntry() throws Exception {
        try(var jar=new JarFile("libs/xaeroworldmap-forge-1.20.1-1.47.0.jar");var in=jar.getInputStream(jar.getJarEntry("xaero/map/gui/GuiMap.class"))){
            Map<String,String> fields=new HashMap<>();Map<String,Integer> flags=new HashMap<>();Set<String> methods=new HashSet<>();
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9){
                @Override public FieldVisitor visitField(int access,String name,String desc,String signature,Object value){fields.put(name,desc);flags.put(name,access);return null;}
                @Override public MethodVisitor visitMethod(int access,String name,String desc,String signature,String[] exceptions){methods.add(name+desc);return null;}
            },ClassReader.SKIP_CODE);
            for(String field:List.of("cameraX","cameraZ","scale"))assertEquals("D",fields.get(field));
            for(String field:List.of("hiddenUI","attachedCamera")){assertEquals("Z",fields.get(field));assertNotEquals(0,flags.get(field)&Opcodes.ACC_STATIC);}
            assertTrue(methods.contains("m_88315_(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"));assertTrue(methods.contains("m_6050_(DDD)Z"));
        }
    }
    @Test void nativeCameraProjectionRoundTripsWithGuiScalePanAndZoom(){
        for(double guiScale:new double[]{1,2,3,4})for(double physicalScale:new double[]{0.125,1,8}){
            var view=new MapViewport();view.width=960;view.height=540;view.x=-1450.25;view.z=328.75;view.scale=physicalScale/guiScale;
            assertEquals(-1450.25,view.worldX(480),1e-9);assertEquals(328.75,view.worldZ(270),1e-9);
            assertEquals(123.75,view.screenX(view.worldX(123.75)),1e-8);assertEquals(98.25,view.screenZ(view.worldZ(98.25)),1e-8);
        }
        var odd=new MapViewport();odd.width=481;odd.height=271;odd.nativeCamera(1921,1081,4,0.125,-1000,500);
        assertEquals(-1000,odd.worldX(240),1e-9);assertEquals(500,odd.worldZ(135),1e-9);
        assertEquals(240,odd.screenX(-1000),1e-9);assertEquals(135,odd.screenZ(500),1e-9);
    }
}
