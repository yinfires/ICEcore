package com.yinfires.icecore.compat.bakery;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;

/** Check every new selector and exact invocation against the real renderer bytecode. */
class StoveLightingUpstreamTest {
    private static final List<String> NAMES = List.of("StoveSurfaceAoMixin", "StoveSurfaceLightmapMixin",
            "StoveSurfaceShadeMixin", "StoveSurfaceEmbeddiumModeMixin", "StoveSurfaceEmbeddiumDataMixin");
    private Object value(AnnotationNode a, String key) {
        for (int i=0; i<a.values.size(); i+=2) if (a.values.get(i).equals(key)) return a.values.get(i+1);
        return null;
    }
    private ClassNode read(java.io.InputStream stream) throws Exception {
        assertNotNull(stream); var node = new ClassNode();
        try (stream) { new ClassReader(stream).accept(node, 0); }
        return node;
    }
    private String mapped(String name) throws Exception {
        if (!name.startsWith("m_")) return name;
        for (String line : Files.readAllLines(Path.of("build/createMcpToSrg/output.tsrg"))) {
            String[] fields = line.trim().split("\\s+");
            if (fields.length == 3 && fields[2].equals(name)) return fields[0];
        }
        fail("Missing SRG mapping: " + name); return name;
    }

    @Test void exactSelectorsAndInvocationCountsAndNoRegisteredRedirectConflicts() throws Exception {
        var config = JsonParser.parseString(Files.readString(Path.of("src/main/resources/icecore.mixins.json"))).getAsJsonObject();
        var claims = new HashSet<String>();
        for (String name : NAMES) {
            assertTrue(config.getAsJsonArray("client").asList().stream().anyMatch(e -> e.getAsString().equals(name)));
            assertFalse(config.getAsJsonArray("mixins").asList().stream().anyMatch(e -> e.getAsString().equals(name)));
            var local = read(getClass().getClassLoader().getResourceAsStream("com/yinfires/icecore/mixin/"+name+".class"));
            var mixin = local.invisibleAnnotations.stream().filter(a -> a.desc.endsWith("/Mixin;")).findFirst().orElseThrow();
            String owner;
            var targets = value(mixin, "targets");
            if (targets instanceof List<?> list) owner = ((String)list.get(0)).replace('.', '/');
            else owner = ((org.objectweb.asm.Type)((List<?>)value(mixin,"value")).get(0)).getInternalName();
            boolean embeddium = owner.startsWith("me/jellysquid/");
            ClassNode upstream;
            if (embeddium) {
                try (var jar = new JarFile("libs/embeddium-0.3.31+mc1.20.1.jar")) {
                    upstream = read(jar.getInputStream(jar.getJarEntry(owner+".class")));
                }
            } else upstream = read(getClass().getClassLoader().getResourceAsStream(owner+".class"));
            for (var method : local.methods) {
                var annotations = new ArrayList<AnnotationNode>();
                if (method.visibleAnnotations != null) annotations.addAll(method.visibleAnnotations);
                if (method.invisibleAnnotations != null) annotations.addAll(method.invisibleAnnotations);
                for (var redirect : annotations) if (redirect.desc.endsWith("/Redirect;")) {
                    String selector = (String)((List<?>)value(redirect,"method")).get(0);
                    int start = selector.indexOf('(');
                    String methodName = selector.substring(0,start), desc = selector.substring(start);
                    if (!embeddium) methodName = mapped(methodName);
                    final String expectedName = methodName;
                    var targetMethod = upstream.methods.stream().filter(m -> m.name.equals(expectedName) && m.desc.equals(desc)).findFirst().orElseThrow();
                    Object atObject = value(redirect,"at");
                    var at = (AnnotationNode)(atObject instanceof List<?> list ? list.get(0) : atObject);
                    String invoke = (String)value(at,"target");
                    int semicolon = invoke.indexOf(';'), paren = invoke.indexOf('(');
                    String invokeOwner = invoke.substring(1,semicolon), invokeName = invoke.substring(semicolon+1,paren), invokeDesc = invoke.substring(paren);
                    if (!embeddium) invokeName = mapped(invokeName);
                    int matches=0;
                    for (var insn : targetMethod.instructions) if (insn instanceof MethodInsnNode call
                            && call.owner.equals(invokeOwner) && call.name.equals(invokeName) && call.desc.equals(invokeDesc)) matches++;
                    assertEquals(1,matches,name+" must intercept exactly one real invocation");
                    assertTrue(claims.add(owner+selector+invoke),"New redirects overlap");
                    // Include all pre-existing registered mixins in the conflict check.
                    for (String section : List.of("mixins","client")) for (var entry : config.getAsJsonArray(section)) {
                        String other=entry.getAsString(); if(NAMES.contains(other)) continue;
                        var bytes=getClass().getClassLoader().getResourceAsStream("com/yinfires/icecore/mixin/"+other+".class");
                        if(bytes==null) continue;
                        var otherClass=read(bytes);
                        for(var otherMethod:otherClass.methods) {
                            var all=new ArrayList<AnnotationNode>();
                            if(otherMethod.visibleAnnotations!=null)all.addAll(otherMethod.visibleAnnotations);
                            if(otherMethod.invisibleAnnotations!=null)all.addAll(otherMethod.invisibleAnnotations);
                            for(var a:all)if(a.desc.endsWith("/Redirect;") && ((List<?>)value(a,"method")).contains(selector)) {
                                Object point=value(a,"at"); var p=(AnnotationNode)(point instanceof List<?> l?l.get(0):point);
                                assertNotEquals(invoke,value(p,"target"),"Registered redirect collision: "+other);
                            }
                        }
                    }
                }
            }
        }
    }
    @Test void optionalEmbeddiumTargetsAreSkippedWhenAbsent() {
        assertFalse(new com.yinfires.icecore.mixin.ICECoreMixinPlugin().shouldApplyMixin("absent.Embeddium", "StoveSurfaceEmbeddiumModeMixin"));
    }
}
