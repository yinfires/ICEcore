package com.yinfires.icecore.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

class CafeTableContractTest {
    private static Object value(AnnotationNode a,String key) {
        if(a.values!=null)for(int i=0;i<a.values.size();i+=2)if(a.values.get(i).equals(key))return a.values.get(i+1);
        return null;
    }
    private static List<AnnotationNode> annotations(List<AnnotationNode> a,List<AnnotationNode> b) {
        List<AnnotationNode> out=new ArrayList<>();if(a!=null)out.addAll(a);if(b!=null)out.addAll(b);return out;
    }
    @Test void bothCafeVersionsHaveEveryNewInjectionAndShadow() throws Exception {
        for(String version:List.of("1.9","1.10"))try(ZipFile jar=new ZipFile("libs/cozycafe-"+version+"-all.jar")) {
            for(String name:List.of("CozyCafeTableMixin","CozyCafeTableRendererMixin","CozyCafePlatingStationMixin","CozyCafePlatingRendererMixin","CozyCafePlateItemModelMixin")) {
                ClassNode mixin=new ClassNode();new ClassReader(Files.readAllBytes(Path.of("build/classes/java/main/com/yinfires/icecore/mixin/"+name+".class"))).accept(mixin,0);
                AnnotationNode mix=annotations(mixin.visibleAnnotations,mixin.invisibleAnnotations).stream().filter(a->a.desc.endsWith("/Mixin;")).findFirst().orElseThrow();
                String target=((List<String>)value(mix,"targets")).get(0);
                ClassNode upstream=new ClassNode();new ClassReader(jar.getInputStream(jar.getEntry(target.replace('.','/')+".class"))).accept(upstream,0);
                for(FieldNode field:mixin.fields)if(annotations(field.visibleAnnotations,field.invisibleAnnotations).stream().anyMatch(a->a.desc.endsWith("/Shadow;")))
                    assertTrue(upstream.fields.stream().anyMatch(f->f.name.equals(field.name)&&f.desc.equals(field.desc)),version+" field "+field.name);
                for(MethodNode method:mixin.methods)for(AnnotationNode a:annotations(method.visibleAnnotations,method.invisibleAnnotations)) {
                    if(!a.desc.contains("/injection/"))continue;
                    for(String selector:(List<String>)value(a,"method")) {
                        int split=selector.indexOf('(');
                        MethodNode selected=upstream.methods.stream().filter(m->m.name.equals(selector.substring(0,split))&&m.desc.equals(selector.substring(split))).findFirst().orElseThrow();
                        Object at=value(a,"at");if(at instanceof List<?> points)for(Object p:points){
                            String member=(String)value((AnnotationNode)p,"target");if(member==null)continue;
                            assertTrue(java.util.stream.StreamSupport.stream(selected.instructions.spliterator(),false).anyMatch(i->i instanceof MethodInsnNode call&&member.equals("L"+call.owner+";"+call.name+call.desc)),version+" "+member);
                        }
                    }
                }
            }
        }
    }
    @Test void noDropAndPlatingBoundariesRemainExplicit() throws Exception {
        String table=Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/CozyCafeTableMixin.java"));
        assertFalse(table.contains("new ItemEntity"));assertFalse(table.contains("player.drop("));
        assertTrue(table.contains("WorkstationInventoryTransfer.giveOne"));
        assertTrue(table.contains("icecore$incomingPlate=hand.is"));
        assertTrue(table.contains("waitTime=icecore$savedWait"));
        String build=Files.readString(Path.of("src/main/java/com/yinfires/icecore/building/BuildingServerActions.java"));
        assertTrue(build.indexOf("instanceof com.yinfires.icecore.compat.cozycafe.CafeTableAccess")<build.indexOf("RemovalConfirmation confirmation"));
        String model=Files.readString(Path.of("src/main/resources/assets/kaleidoscope_cookery/models/item/cold_cut_ham_slices.json"));
        assertTrue(model.contains("minecraft:item/generated"));assertFalse(model.contains("_block"));
    }
    @Test void requestIconsKeepNativeGroundContextInBothJars() throws Exception {
        for(String version:List.of("1.9","1.10"))try(ZipFile jar=new ZipFile("libs/cozycafe-"+version+"-all.jar")) {
            ClassNode renderer=new ClassNode();
            new ClassReader(jar.getInputStream(jar.getEntry("io/github/chakyl/cozycafe/blockentities/renderer/CafeMenuBlockEntityRenderer.class"))).accept(renderer,0);
            assertTrue(renderer.methods.stream().flatMap(m->java.util.stream.StreamSupport.stream(m.instructions.spliterator(),false))
                .anyMatch(i->i instanceof FieldInsnNode f && f.owner.endsWith("/ItemDisplayContext") && f.name.equals("GROUND")));
        }
        ClassNode mixin=new ClassNode();
        new ClassReader(Files.readAllBytes(Path.of("build/classes/java/main/com/yinfires/icecore/mixin/CozyCafeTableRendererMixin.class"))).accept(mixin,0);
        assertFalse(mixin.methods.stream().anyMatch(m->m.name.equals("icecore$requestIcon")));
        for(String food:List.of("cold_roasted_meat","cold_cut_ham_slices","oil_splashed_fish","braised_pork_ribs")) {
            Path override=Path.of("src/main/resources/assets/kaleidoscope_cookery/models/item/"+food+".json");
            String model=Files.exists(override)?Files.readString(override):cookeryModel(food);
            assertTrue(model.contains("item/generated"),food);
            assertFalse(model.contains("\"display\""),food+" must inherit native generated GROUND size");
        }
    }
    @Test void plateGeometryAndLightContract() throws Exception {
        try(ZipFile jar=new ZipFile("libs/FarmersDelight-1.20.1-1.3.4.jar")) {
            for(String pie:List.of("pumpkin_pie","apple_pie","chocolate_pie")) {
                assertNotNull(jar.getEntry("assets/farmersdelight/blockstates/"+pie+".json"),pie);
                String model=new String(jar.getInputStream(jar.getEntry("assets/farmersdelight/models/block/"+pie+".json")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
                var json=com.google.gson.JsonParser.parseString(model).getAsJsonObject();
                assertTrue(json.has("elements") || json.has("parent"),pie);
            }
        }
        for(String version:List.of("1.9","1.10"))try(ZipFile jar=new ZipFile("libs/cozycafe-"+version+"-all.jar")) {
            var model=com.google.gson.JsonParser.parseString(new String(jar.getInputStream(jar.getEntry("assets/cozycafe/models/item/serving_plate.json")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var element=model.getAsJsonArray("elements").get(0).getAsJsonObject();
            assertEquals("[1,0,1]",element.getAsJsonArray("from").toString());
            assertEquals("[15,2,15]",element.getAsJsonArray("to").toString());
        }
        ClassNode helper=new ClassNode();new ClassReader(Files.readAllBytes(Path.of("build/classes/java/main/com/yinfires/icecore/compat/cozycafe/client/CafePlateRendering.class"))).accept(helper,0);
        var calls=helper.methods.stream().flatMap(m->java.util.stream.StreamSupport.stream(m.instructions.spliterator(),false)).filter(i->i instanceof MethodInsnNode).map(i->(MethodInsnNode)i).toList();
        assertTrue(calls.stream().anyMatch(c->c.name.equals("render")&&c.owner.endsWith("/ItemRenderer")));
        assertFalse(calls.stream().anyMatch(c->c.name.equals("tesselateBlock")));
    }    private static String cookeryModel(String food) throws Exception {
        try(ZipFile jar=new ZipFile("libs/kaleidoscopecookery-1.5.1-forge+mc1.20.1.jar")) {
            return new String(jar.getInputStream(jar.getEntry("assets/kaleidoscope_cookery/models/item/"+food+".json")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        }
    }}
