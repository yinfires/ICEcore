package com.yinfires.icecore.fridge;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import com.google.gson.*;
import static org.junit.jupiter.api.Assertions.*;

final class FridgeUpstreamContractTest {
    private static final Path JAR = Path.of("libs/cookingforblockheads-forge-1.20.1-16.0.15.jar");
    private static ClassNode clazz(ZipFile jar, String path) throws Exception {
        ClassNode node = new ClassNode();
        new ClassReader(jar.getInputStream(jar.getEntry(path + ".class"))).accept(node, 0);
        return node;
    }
    @Test void injectionsMatchActualProductionJarAndNativeStorageIsRetained() throws Exception {
        try (ZipFile jar = new ZipFile(JAR.toFile())) {
            ClassNode target = clazz(jar, "net/blay09/mods/cookingforblockheads/tile/FridgeBlockEntity");
            ClassNode mixin = new ClassNode();
            new ClassReader(Files.readAllBytes(Path.of("build/classes/java/main/com/yinfires/icecore/mixin/FridgePairingMixin.class"))).accept(mixin, 0);
            int checked = 0;
            for (MethodNode method : mixin.methods) {
                List<AnnotationNode> annotations = new ArrayList<>();
                if (method.visibleAnnotations != null) annotations.addAll(method.visibleAnnotations);
                if (method.invisibleAnnotations != null) annotations.addAll(method.invisibleAnnotations);
                for (AnnotationNode annotation : annotations) {
                    if (!annotation.desc.endsWith("/Inject;")) continue;
                    for (int i=0; i<annotation.values.size(); i+=2) if (annotation.values.get(i).equals("method")) {
                        for (Object raw : (List<?>) annotation.values.get(i+1)) {
                            String selector = raw.toString(); int split = selector.indexOf('(');
                            assertTrue(target.methods.stream().anyMatch(m -> m.name.equals(selector.substring(0,split))
                                    && m.desc.equals(selector.substring(split))), selector);
                            checked++;
                        }
                    }
                }
            }
            assertEquals(3, checked);
            assertTrue(target.fields.stream().anyMatch(f -> f.name.equals("container")
                    && f.desc.equals("Lnet/blay09/mods/balm/api/container/DefaultContainer;")));
            ClassNode renderer = clazz(jar,"net/blay09/mods/cookingforblockheads/client/render/FridgeRenderer");
            assertTrue(renderer.methods.stream().flatMap(m -> Arrays.stream(m.instructions.toArray()))
                    .anyMatch(i -> i instanceof MethodInsnNode call && call.name.equals("getCombinedContainer")));
        }
        String mapping = Files.readString(Path.of("build/createMcpToSrg/output.tsrg"));
        assertTrue(mapping.contains("validBlocks f_58915_"));
    }

    @Test void itemUsesEveryOriginalBoxWithUnchangedUVAndTextureBindings() throws Exception {
        JsonObject item = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/assets/icecore/models/item/fridge.json"))).getAsJsonObject();
        Map<String,JsonObject> boxes = new HashMap<>();
        for (JsonElement element : item.getAsJsonArray("elements")) {
            JsonObject box = element.getAsJsonObject(); boxes.put(box.get("name").getAsString(), box);
        }
        int checked=0;
        try (ZipFile jar = new ZipFile(JAR.toFile())) {
            for (String name : new String[]{"fridge_large_lower","fridge_large_upper","fridge_large_door_lower","fridge_large_door_upper"}) {
                var entry=jar.getEntry("assets/cookingforblockheads/models/block/"+name+".json");
                JsonObject source=JsonParser.parseString(new String(jar.getInputStream(entry).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                int offset=name.endsWith("upper")?16:0;
                for (JsonElement element : source.getAsJsonArray("elements")) {
                    JsonObject expected=element.getAsJsonObject().deepCopy();
                    String id=name+"/"+expected.get("name").getAsString();
                    expected.addProperty("name",id);
                    for (String coordinate : new String[]{"from","to"}) {
                        var point=expected.getAsJsonArray(coordinate);
                        point.set(1,new JsonPrimitive(point.get(1).getAsDouble()+offset));
                    }
                    for (var face : expected.getAsJsonObject("faces").entrySet()) {
                        face.getValue().getAsJsonObject().remove("tintindex");
                        face.getValue().getAsJsonObject().remove("cullface");
                    }
                    assertEquals(expected, boxes.get(id), id); checked++;
                }
                for(var texture:source.getAsJsonObject("textures").entrySet())
                    assertEquals(texture.getValue(),item.getAsJsonObject("textures").get(texture.getKey()));
            }
        }
        assertEquals(25,checked); assertEquals(checked,boxes.size());
    }
}
