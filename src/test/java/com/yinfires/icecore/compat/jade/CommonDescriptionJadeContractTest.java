package com.yinfires.icecore.compat.jade;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

class CommonDescriptionJadeContractTest {
    @Test void actualJadeTagsTitleAndSupportsIndexedInsertion() throws Exception {
        try (var jar = new ZipFile("libs/Jade-1.20.1-Forge-11.13.3.jar")) {
            var title = read(jar, "snownee/jade/addon/core/ObjectNameProvider.class");
            var uid = title.methods.stream().filter(m -> m.name.equals("getUid")).findFirst().orElseThrow();
            assertTrue(java.util.stream.StreamSupport.stream(uid.instructions.spliterator(), false)
                .anyMatch(i -> i instanceof FieldInsnNode f && f.name.equals("CORE_OBJECT_NAME")));
            var tooltip = read(jar, "snownee/jade/impl/Tooltip.class");
            var add = tooltip.methods.stream().filter(m -> m.name.equals("add") &&
                m.desc.equals("(ILsnownee/jade/api/ui/IElement;)V")).findFirst().orElseThrow();
            assertTrue(java.util.stream.StreamSupport.stream(add.instructions.spliterator(), false)
                .anyMatch(i -> i instanceof MethodInsnNode m && m.name.equals("add") && m.desc.equals("(ILjava/lang/Object;)V")));
            var append = tooltip.methods.stream().filter(m -> m.name.equals("append")).findFirst().orElseThrow();
            assertTrue(java.util.stream.StreamSupport.stream(append.instructions.spliterator(), false)
                .anyMatch(i -> i instanceof MethodInsnNode m && m.name.equals("tag")));
        }
    }
    private static ClassNode read(ZipFile jar, String path) throws Exception {
        var node = new ClassNode();
        try (var stream = jar.getInputStream(jar.getEntry(path))) { new ClassReader(stream).accept(node, 0); }
        return node;
    }
}
