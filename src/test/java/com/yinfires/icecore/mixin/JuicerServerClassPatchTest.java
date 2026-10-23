package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.fragrantorchard.JuicerServerClassPatch;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

final class JuicerServerClassPatchTest {
    private static ClassNode upstream() throws Exception {
        try (ZipFile jar = new ZipFile("libs/kaleidoscope_fragrantorchard-1.0.0-ALPHA-1.20.1.jar")) {
            ClassNode node = new ClassNode();
            new ClassReader(jar.getInputStream(jar.getEntry(
                    "com/bmt/kaleidoscope_fragrantorchard/block/entity/JuicerBlockEntity.class"))).accept(node, 0);
            return node;
        }
    }

    private static boolean clientOnly(List<AnnotationNode> annotations) {
        return annotations != null && annotations.stream().anyMatch(annotation ->
                annotation.desc.equals("Lnet/minecraftforge/api/distmarker/OnlyIn;"));
    }

    @Test void actualUpstreamAndDistCleanedClassBothHaveNoRemainingClientLinks() throws Exception {
        for (boolean cleaned : new boolean[]{false, true}) {
            ClassNode target = upstream();
            assertTrue(target.fields.stream().anyMatch(field -> field.name.equals("workingSoundInstance")));
            if (cleaned) {
                target.fields.removeIf(field -> clientOnly(field.visibleAnnotations));
                target.methods.removeIf(method -> clientOnly(method.visibleAnnotations));
            }
            JuicerServerClassPatch.apply(target);
            JuicerServerClassPatch.apply(target); // Repeated transformer hooks are harmless.
            assertFalse(target.fields.stream().anyMatch(field -> field.name.equals("soundTickCounter")
                    || field.name.equals("workingSoundInstance")));
            for (MethodNode method : target.methods) {
                if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0) {
                    new Analyzer<>(new BasicVerifier()).analyze(target.name, method);
                }
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof FieldInsnNode field) {
                        assertFalse(field.owner.startsWith("net/minecraft/client/"));
                        assertFalse(field.desc.contains("net/minecraft/client/"));
                        assertFalse(field.name.equals("soundTickCounter") || field.name.equals("workingSoundInstance"));
                    } else if (instruction instanceof MethodInsnNode call) {
                        assertFalse(call.owner.startsWith("net/minecraft/client/"));
                        assertFalse(call.desc.contains("net/minecraft/client/"));
                        assertNotEquals("tickClientSound", call.name);
                    } else if (instruction instanceof TypeInsnNode type) {
                        assertFalse(type.desc.startsWith("net/minecraft/client/"));
                    }
                }
            }
            MethodNode removed = target.methods.stream().filter(method -> method.name.equals("m_7651_")).findFirst().orElseThrow();
            assertEquals(3, removed.instructions.size());
            MethodInsnNode parent = (MethodInsnNode) removed.instructions.get(1);
            assertEquals(Opcodes.INVOKESPECIAL, parent.getOpcode());
            assertEquals(target.superName, parent.owner);
            assertEquals("m_7651_", parent.name);
            ClassWriter writer = new ClassWriter(0);
            target.accept(writer);
            assertFalse(new String(writer.toByteArray(), StandardCharsets.ISO_8859_1).contains("net/minecraft/client/"));
        }
    }

    @Test void patchIsRegisteredAndAbsentModOrUnrelatedHookLeavesClassUntouched() throws Exception {
        String configuration = Files.readString(Path.of("src/main/resources/icecore.mixins.json"));
        assertTrue(configuration.contains("\"WorkstationJuicerServerSoundMixin\""));
        var plugin = new ICECoreMixinPlugin();
        try (var loader = mockStatic(net.minecraftforge.fml.loading.FMLLoader.class)) {
            loader.when(net.minecraftforge.fml.loading.FMLLoader::getDist)
                    .thenReturn(net.minecraftforge.api.distmarker.Dist.CLIENT);
            assertFalse(plugin.shouldApplyMixin("com.yinfires.icecore.ICECore", "WorkstationJuicerServerSoundMixin"));
            loader.when(net.minecraftforge.fml.loading.FMLLoader::getDist)
                    .thenReturn(net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER);
            assertTrue(plugin.shouldApplyMixin("com.yinfires.icecore.ICECore", "WorkstationJuicerServerSoundMixin"));
            assertFalse(plugin.shouldApplyMixin("missing.mod.JuicerBlockEntity", "WorkstationJuicerServerSoundMixin"));
        }
        ClassNode client = upstream();
        plugin.preApply(client.name.replace('/', '.'), client, "WorkstationJuicerReloadMixin", null);
        assertTrue(client.fields.stream().anyMatch(field -> field.name.equals("workingSoundInstance")));
        assertThrows(IllegalArgumentException.class, () -> JuicerServerClassPatch.apply(new ClassNode()));
    }
}
