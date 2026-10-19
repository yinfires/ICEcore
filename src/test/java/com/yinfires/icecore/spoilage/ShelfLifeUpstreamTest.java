package com.yinfires.icecore.spoilage;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

final class ShelfLifeUpstreamTest {
    private static ClassNode read(Path path) throws Exception {
        ClassNode node = new ClassNode(); new ClassReader(Files.readAllBytes(path)).accept(node, 0); return node;
    }
    private static ClassNode read(ZipFile zip, String name) throws Exception {
        ClassNode node = new ClassNode();
        new ClassReader(zip.getInputStream(zip.getEntry(name + ".class"))).accept(node, 0); return node;
    }
    @Test void allInjectSelectorsExistInActualProductionTargets() throws Exception {
        Path dir = Path.of("build/classes/java/main/com/yinfires/icecore/mixin");
        Map<String, Path> jars = Map.of(
                "com/mrbysco/spoiled/", Path.of("libs/Spoiled-forge-1.20.1-2.2.0.jar"),
                "net/blay09/mods/balm/", Path.of("libs/balm-forge-1.20.1-7.3.44.jar"),
                "net/blay09/mods/cookingforblockheads/", Path.of("libs/cookingforblockheads-forge-1.20.1-16.0.15.jar"),
                "io/github/chakyl/", Path.of("libs/cozycafe-1.10-all.jar"));
        int checked = 0;
        try (var files = Files.list(dir)) {
            for (Path file : files.filter(p -> p.getFileName().toString().matches("(ShelfLife|Spoiled).*Mixin.class")).toList()) {
                ClassNode mixin = read(file);
                AnnotationNode annotation = mixin.invisibleAnnotations.stream().filter(a -> a.desc.endsWith("/Mixin;")).findFirst().orElseThrow();
                String target = null;
                for (int i = 0; i < annotation.values.size(); i += 2) {
                    if (annotation.values.get(i).equals("value")) target = ((Type)((List<?>)annotation.values.get(i+1)).get(0)).getInternalName();
                    if (annotation.values.get(i).equals("targets")) target = ((List<?>)annotation.values.get(i+1)).get(0).toString().replace('.', '/');
                }
                assertNotNull(target);
                ClassNode production = null;
                for (var entry : jars.entrySet()) if (target.startsWith(entry.getKey())) {
                    try (ZipFile zip = new ZipFile(entry.getValue().toFile())) { production = read(zip, target); }
                }
                if (production == null) {
                    // Derive the runtime srg jar from the configured Gradle user home.
                    Path home = Path.of(System.getenv().getOrDefault("GRADLE_USER_HOME", System.getProperty("user.home") + "/.gradle"));
                    try (ZipFile zip = new ZipFile(home.resolve("caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.20.1-47.4.10/forge-1.20.1-47.4.10-srg.jar").toFile())) { production = read(zip, target); }
                }
                for (MethodNode method : mixin.methods) {
                    List<AnnotationNode> annotations = new ArrayList<>();
                    if (method.visibleAnnotations != null) annotations.addAll(method.visibleAnnotations);
                    if (method.invisibleAnnotations != null) annotations.addAll(method.invisibleAnnotations);
                    for (AnnotationNode inject : annotations) if (inject.desc.endsWith("/Inject;") || inject.desc.endsWith("/Redirect;")) {
                        for (int i = 0; i < inject.values.size(); i += 2) if (inject.values.get(i).equals("method")) {
                            for (Object value : (List<?>)inject.values.get(i+1)) {
                                String selector = value.toString(); int split = selector.indexOf('(');
                                String name = split < 0 ? selector : selector.substring(0, split);
                                String desc = split < 0 ? null : selector.substring(split);
                                assertTrue(production.methods.stream().anyMatch(m -> m.name.equals(name) && (desc == null || desc.equals(m.desc))), file + " -> " + target + "." + selector);
                                checked++;
                            }
                        }
                    }
                }
                for (FieldNode field : mixin.fields) {
                    List<AnnotationNode> annotations = new ArrayList<>();
                    if (field.visibleAnnotations != null) annotations.addAll(field.visibleAnnotations);
                    if (field.invisibleAnnotations != null) annotations.addAll(field.invisibleAnnotations);
                    if (annotations.stream().anyMatch(a -> a.desc.endsWith("/Shadow;")))
                        assertTrue(production.fields.stream().anyMatch(f -> f.name.equals(field.name) && f.desc.equals(field.desc)), "Missing shadow " + target + "." + field.name);
                }
            }
        }
        assertTrue(checked >= 25, "Unexpectedly few production injection checks: " + checked);
    }
}
