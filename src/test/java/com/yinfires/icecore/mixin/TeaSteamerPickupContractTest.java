package com.yinfires.icecore.mixin;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class TeaSteamerPickupContractTest {
    private static final Path ROOT = Path.of("src/main/java/com/yinfires/icecore/mixin");
    @Test void bothUpstreamVersionsExposeNativeUsePlacementAndFoodPaths() throws Exception {
        int checked = 0;
        try (var paths = Files.list(Path.of("libs"))) {
            for (var path : paths.filter(p -> p.getFileName().toString().contains("kaleidoscopecookery-") && p.toString().endsWith(".jar")).toList()) {
                try (var jar = new java.util.zip.ZipFile(path.toFile())) {
                    var node = new org.objectweb.asm.tree.ClassNode();
                    new org.objectweb.asm.ClassReader(jar.getInputStream(jar.getEntry(
                            "com/github/ysbbbbbb/kaleidoscopecookery/block/kitchen/SteamerBlock.class"))).accept(node, 0);
                    assertTrue(node.methods.stream().anyMatch(m -> m.name.equals("m_5573_") && m.desc.equals(
                            "(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/level/block/state/BlockState;")));
                    var use = node.methods.stream().filter(m -> m.name.equals("m_6227_")).findFirst().orElseThrow();
                    for (String name : new String[]{"placeFood", "takeFood", "m_40576_", "m_36341_"})
                        assertTrue(java.util.stream.StreamSupport.stream(use.instructions.spliterator(), false)
                                .anyMatch(i -> i instanceof org.objectweb.asm.tree.MethodInsnNode call && call.name.equals(name)), name);
                    checked++;
                }
            }
        }
        assertEquals(2, checked);
        assertTrue(Files.readString(Path.of("build/createMcpToSrg/output.tsrg")).contains(
                "getStateForPlacement (Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/level/block/state/BlockState; m_5573_"));
    }
    @Test void cupsReceiveBeforeStateReductionAndKeepNativeHeldInteractions() throws Exception {
        String cup = Files.readString(ROOT.resolve("WorkstationCupPickupMixin.java"));
        assertTrue(cup.contains("!player.getMainHandItem().isEmpty()) return"));
        assertTrue(cup.contains("WorkstationInventoryTransfer.giveOne"));
        assertTrue(cup.indexOf("WorkstationInventoryTransfer.giveOne") < cup.indexOf("level.setBlockAndUpdate"));
        assertTrue(cup.contains("boolean empty = cups > tea"));
        assertTrue(cup.contains("empty ? tea : tea - 1"));
        assertFalse(cup.contains("setItemInHand"));
        assertFalse(cup.contains("PlayerFeedback"));
        String ingredient = Files.readString(ROOT.resolve("WorkstationTeapotIngredientPickupMixin.java"));
        assertTrue(ingredient.indexOf("WorkstationInventoryTransfer.giveOne") < ingredient.indexOf("self.getInput().shrink(1)"));
    }
    @Test void steamerHeldFallbackIsBlockedAndFullInventoryPreservesContents() throws Exception {
        String block = Files.readString(ROOT.resolve("WorkstationSteamerBlockMixin.java"));
        assertTrue(block.contains("ISteamer;takeFood"));
        assertTrue(block.contains("hand != InteractionHand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()"));
        String entity = Files.readString(ROOT.resolve("WorkstationSteamerMixin.java"));
        assertTrue(entity.contains("!player.getMainHandItem().isEmpty() || level.isClientSide"));
        assertTrue(entity.contains("stored.getCount() - remainder.getCount()"));
        assertTrue(entity.contains("if (moved <= 0) continue"));
        assertFalse(entity.contains("WorkstationInventoryTransfer.giveOne"));
        assertFalse(entity.contains("level.setBlockAndUpdate"));
        assertFalse(entity.contains("ModItems.STEAMER"));
        assertTrue(block.contains("instanceof com.github.ysbbbbbb.kaleidoscopecookery.item.SteamerItem"));
        assertTrue(block.contains("level.getBlockState(pos).is(block) || level.getBlockState(pos.below()).is(block)"));
        assertTrue(block.contains("cir.setReturnValue(null)"));
        assertFalse(entity.contains("getItemToLivingEntity"));
    }
}
