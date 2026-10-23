package com.yinfires.icecore.mixing;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

final class MixingClientIsolationTest {
    private static ClassNode read(String name) throws IOException {
        try (var stream = MixingClientIsolationTest.class.getResourceAsStream("/" + name + ".class")) {
            assertNotNull(stream);
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, 0);
            return node;
        }
    }

    @Test void commonBootstrapDoesNotLinkClientSetupOrRendering() throws IOException {
        for (String name : new String[]{"com/yinfires/icecore/ICECore", "com/yinfires/icecore/mixing/ModMixing"}) {
            for (var method : read(name).methods) {
                for (var instruction : method.instructions) {
                    if (instruction instanceof MethodInsnNode call) {
                        assertFalse(call.owner.startsWith("net/minecraft/client/"));
                        assertFalse(call.desc.contains("FMLClientSetupEvent"));
                    }
                    if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                        for (Object argument : dynamic.bsmArgs) {
                            if (argument instanceof Handle handle) {
                                assertFalse(handle.getDesc().contains("FMLClientSetupEvent"));
                                assertFalse(handle.getOwner().endsWith("Renderer"));
                            }
                        }
                    }
                }
            }
        }
    }

    @Test void rendererSubscriberIsRestrictedToPhysicalClient() throws IOException {
        ClassNode node = read("com/yinfires/icecore/mixing/client/MixingClient");
        var subscriber = node.visibleAnnotations.stream()
                .filter(annotation -> annotation.desc.endsWith("Mod$EventBusSubscriber;")).findFirst().orElseThrow();
        int index = subscriber.values.indexOf("value");
        assertTrue(index >= 0);
        var sides = (java.util.List<?>) subscriber.values.get(index + 1);
        assertEquals(1, sides.size());
        assertEquals("CLIENT", ((String[]) sides.get(0))[1]);
        var callback = node.methods.stream().filter(method -> method.name.equals("registerRenderers")).findFirst().orElseThrow();
        assertTrue(callback.visibleAnnotations.stream().anyMatch(annotation -> annotation.desc.endsWith("SubscribeEvent;")));
        assertEquals(2, java.util.stream.StreamSupport.stream(callback.instructions.spliterator(), false)
                .filter(instruction -> instruction instanceof MethodInsnNode call && call.name.equals("registerBlockEntityRenderer")).count());
    }
}
