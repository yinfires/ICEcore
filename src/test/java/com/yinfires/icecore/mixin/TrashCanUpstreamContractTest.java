package com.yinfires.icecore.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

final class TrashCanUpstreamContractTest {
    @Test void forgeAttackPathsHaveExactDescriptorsAndCanceledInputStillDrawsHitEffects() throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream("net/minecraft/client/Minecraft.class")) {
            assertNotNull(stream);
            var node = new ClassNode(); new ClassReader(stream).accept(node, 0);
            MethodNode start = method(node, "startAttack", "()Z");
            MethodNode continued = method(node, "continueAttack", "(Z)V");
            assertEquals(1, calls(start, "startDestroyBlock"));
            assertEquals(1, calls(continued, "continueDestroyBlock"));
            assertEquals(2, calls(continued, "addBlockHitEffects"));
            String mapping = Files.readString(Path.of("build/createMcpToSrg/output.tsrg"));
            assertTrue(mapping.contains("startAttack ()Z m_202354_"));
            assertTrue(mapping.contains("continueAttack (Z)V m_91386_"));
        }
    }
    @Test void bothCookeryVersionsRetainNativeSlotsEffectsAndTransferSites() throws Exception {
        int checked = 0;
        try (var paths = Files.list(Path.of("libs"))) {
            for (var path : paths.filter(p -> p.getFileName().toString().contains("kaleidoscopecookery-") && p.toString().endsWith(".jar")).toList()) {
                try (var jar = new ZipFile(path.toFile())) {
                    var block = new ClassNode();
                    new ClassReader(jar.getInputStream(jar.getEntry("com/github/ysbbbbbb/kaleidoscopecookery/block/misc/TrashCanBlock.class"))).accept(block, 0);
                    MethodNode landing = method(block, "m_142072_", "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;F)V");
                    assertEquals(1, calls(landing, "m_142072_"));
                    assertEquals(1, calls(landing, "m_7998_"));
                    assertTrue(java.util.stream.StreamSupport.stream(landing.instructions.spliterator(), false)
                            .anyMatch(i -> i instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKESPECIAL
                                    && call.owner.equals("net/minecraft/world/level/block/HorizontalDirectionalBlock") && call.name.equals("m_142072_")));
                    var node = new ClassNode();
                    new ClassReader(jar.getInputStream(jar.getEntry("com/github/ysbbbbbb/kaleidoscopecookery/blockentity/misc/TrashCanBlockEntity.class"))).accept(node, 0);
                    MethodNode put = method(node, "putItem", "(Lnet/minecraft/world/item/ItemStack;)V");
                    MethodNode take = method(node, "withdrawItem", "(Lnet/minecraft/world/entity/LivingEntity;)V");
                    assertEquals(3, calls(put, "setStackInSlot"));
                    assertEquals(1, calls(put, "insertItemStacked"));
                    assertEquals(2, calls(put, "m_41774_"));
                    assertEquals(1, calls(take, "m_21008_"));
                    assertEquals(1, calls(take, "setStackInSlot"));
                    for (var action : new MethodNode[]{put, take}) {
                        assertEquals(1, calls(action, "refresh"));
                        assertEquals(1, calls(action, "m_8767_"));
                        assertEquals(1, calls(action, "m_5594_"));
                    }
                    method(node, "m_142466_", "(Lnet/minecraft/nbt/CompoundTag;)V");
                    method(node, "m_183515_", "(Lnet/minecraft/nbt/CompoundTag;)V");
                    var renderer = new ClassNode();
                    new ClassReader(jar.getInputStream(jar.getEntry("com/github/ysbbbbbb/kaleidoscopecookery/client/render/block/TrashCanBlockEntityRender.class"))).accept(renderer, 0);
                    assertTrue(renderer.methods.stream().anyMatch(m -> java.util.stream.StreamSupport.stream(m.instructions.spliterator(), false)
                            .anyMatch(i -> i instanceof FieldInsnNode field && field.name.equals("putState"))));
                    assertTrue(renderer.methods.stream().anyMatch(m -> java.util.stream.StreamSupport.stream(m.instructions.spliterator(), false)
                            .anyMatch(i -> i instanceof FieldInsnNode field && field.name.equals("withdrawState"))));
                    var animation = new ClassNode();
                    new ClassReader(jar.getInputStream(jar.getEntry("com/github/ysbbbbbb/kaleidoscopecookery/client/animation/TrashCanAnimation.class"))).accept(animation, 0);
                    var initialize = method(animation, "<clinit>", "()V");
                    assertTrue(java.util.stream.StreamSupport.stream(initialize.instructions.spliterator(), false)
                            .anyMatch(i -> i instanceof LdcInsnNode value && Float.valueOf(.5833F).equals(value.cst)));
                    assertTrue(java.util.stream.StreamSupport.stream(initialize.instructions.spliterator(), false)
                            .anyMatch(i -> i instanceof LdcInsnNode value && Float.valueOf(.5F).equals(value.cst)));
                    checked++;
                }
            }
        }
        assertEquals(2, checked);
    }
    private static MethodNode method(ClassNode node, String name, String desc) {
        return node.methods.stream().filter(m -> m.name.equals(name) && m.desc.equals(desc)).findFirst().orElseThrow();
    }
    private static long calls(MethodNode node, String name) {
        return java.util.stream.StreamSupport.stream(node.instructions.spliterator(), false)
                .filter(i -> i instanceof MethodInsnNode call && call.name.equals(name)).count();
    }
}
