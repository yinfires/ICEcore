package com.yinfires.icecore.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

final class CuttingBoardContractTest {
    private static final Path ROOT = Path.of("src/main/java/com/yinfires/icecore");
    @Test void upstreamProcessingHasOneRollAndRetainsAllNativeSideEffects() throws Exception {
        try (ZipFile jar = new ZipFile("libs/FarmersDelight-1.20.1-1.3.4.jar")) {
            ClassNode node = new ClassNode();
            new ClassReader(jar.getInputStream(jar.getEntry("vectorwing/farmersdelight/common/block/entity/CuttingBoardBlockEntity.class"))).accept(node, 0);
            MethodNode processing = node.methods.stream().filter(m -> m.name.equals("lambda$processStoredItemUsingTool$2")).findFirst().orElseThrow();
            java.util.List<String> calls = new java.util.ArrayList<>();
            for (var instruction : processing.instructions) if (instruction instanceof MethodInsnNode call) calls.add(call.name);
            assertEquals(1, calls.stream().filter("rollResults"::equals).count());
            for (String name : new String[]{"spawnItemEntity", "m_41622_", "m_220157_", "spawnCuttingParticles", "playProcessingSound", "extractItem", "trigger"}) assertTrue(calls.contains(name), name);
        }
        String mixin = Files.readString(ROOT.resolve("mixin/WorkstationCuttingBoardEntityMixin.java"));
        assertTrue(mixin.contains("random, fortune, wrapper"));
        assertTrue(mixin.contains("return List.of()"));
        assertTrue(mixin.contains("icecore$results.add(result.copy())"));
        assertTrue(mixin.contains("entity.getLevel().isClientSide"));
        assertTrue(mixin.contains("icecore$inputMetadata.save(input)"));
        assertTrue(mixin.contains("icecore$inputMetadata.load"));
    }
    @Test void partialInventoryAcceptanceAndContainerRulesHaveNoDropFallback() throws Exception {
        String transfer = Files.readString(ROOT.resolve("workstation/CuttingBoardInteraction.java"));
        assertTrue(transfer.contains("WorkstationInventoryTransfer.insert(player, stored, false)"));
        assertTrue(transfer.contains("stored.getCount() - remaining.getCount()"));
        assertTrue(transfer.contains("stored.shrink(moved)"));
        assertTrue(transfer.contains("STORED_AS_ITEM"));
        assertTrue(transfer.contains("emptyMatches"));
        assertTrue(transfer.contains("WorkstationInteractionService.need(player, required)"));
        assertFalse(transfer.contains("drop("));
        assertFalse(transfer.contains("PlayerFeedback"));
        assertFalse(transfer.contains("sendSystemMessage"));
        String handler = Files.readString(ROOT.resolve("workstation/CuttingBoardInputHandler.java"));
        assertTrue(handler.contains("getSlotLimit(int slot) { return 1; }"));
        assertTrue(handler.contains("!board.icecore$products().isEmpty()"));
        assertTrue(handler.contains("if (!loading)"));
        String entity = Files.readString(ROOT.resolve("mixin/WorkstationCuttingBoardEntityMixin.java"));
        assertTrue(entity.contains("private void icecore$silent(Player player, Component text, boolean actionBar) {}"));
        assertFalse(entity.contains("new ItemStackHandler(1) {"));
    }
    @Test void visualGeometryStorageAndOptionalModelBoundary() throws Exception {
        String render = Files.readString(ROOT.resolve("client/CuttingBoardItems.java"));
        assertTrue(render.contains("pose.translate(.5, .1, .5)"));
        assertTrue(render.contains("rotationDegrees(90)"));
        assertTrue(render.contains("Axis.XN.rotationDegrees(90)"));
        assertTrue(render.contains("Axis.YP.rotationDegrees"));
        assertFalse(render.contains("Axis.XP.rotationDegrees"));
        assertFalse(render.contains("Axis.YN.rotationDegrees"));
        assertTrue(render.contains("pose.scale(.5F, .5F, .5F)"));
        assertTrue(render.contains("90 + ROTATIONS.nextInt(90)"));
        assertTrue(render.contains("i * .025"));
        assertTrue(render.contains("isLoaded(\"kaleidoscope_cookery\")"));
        String bridge = Files.readString(ROOT.resolve("client/CuttingBoardCookeryModel.java"));
        assertTrue(bridge.contains("ItemRenderReplacer.getModel"));
        assertTrue(bridge.contains("INSTANCE.pot()"));
        String jade = Files.readString(ROOT.resolve("compat/jade/WorkstationJadeProvider.java"));
        assertTrue(jade.contains("else add(inputs, board.icecore$inventory().getStackInSlot(0))"));
        try (ZipFile jar = new ZipFile("libs/FarmersDelight-1.20.1-1.3.4.jar")) {
            String model = new String(jar.getInputStream(jar.getEntry("assets/farmersdelight/models/block/cutting_board.json")).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(model.contains("[12, 1, 15]")); assertTrue(.1 > 1.0 / 16);
        }
    }
    @Test void wokRotationAxesMatchRealJarAndFaceUpInEveryDirection() throws Exception {
        try (ZipFile jar = new ZipFile("libs/FarmersDelight-1.20.1-1.3.4.jar")) {
            ClassNode node = new ClassNode();
            new ClassReader(jar.getInputStream(jar.getEntry("vectorwing/farmersdelight/client/renderer/CuttingBoardRenderer.class"))).accept(node, 0);
            MethodNode laying = node.methods.stream().filter(m -> m.name.equals("renderItemLayingDown")).findFirst().orElseThrow();
            boolean positiveYaw = false;
            for (var instruction : laying.instructions) if (instruction instanceof FieldInsnNode field
                    && field.owner.equals("com/mojang/math/Axis") && field.name.equals("f_252436_")) positiveYaw = true;
            assertTrue(positiveYaw, "Target cutting board uses YP for horizontal texture direction");
        }
        try (ZipFile jar = new ZipFile("libs/[森罗物语：厨房] kaleidoscopecookery-1.4.1-forge+mc1.20.1.jar")) {
            ClassNode node = new ClassNode();
            new ClassReader(jar.getInputStream(jar.getEntry("com/github/ysbbbbbb/kaleidoscopecookery/client/render/block/PotBlockEntityRender.class"))).accept(node, 0);
            MethodNode render = node.methods.stream().filter(m -> m.name.equals("render") && m.desc.contains("PotBlockEntity;")).findFirst().orElseThrow();
            java.util.List<String> axes = new java.util.ArrayList<>();
            for (var instruction : render.instructions) if (instruction instanceof FieldInsnNode field && field.owner.equals("com/mojang/math/Axis")) axes.add(field.name);
            assertTrue(axes.contains("f_252392_"), "Actual wok uses YN");
            assertTrue(axes.contains("f_252495_"), "Actual wok uses XN");
        }
        for (int direction = 0; direction < 4; direction++) {
            var rotation = new org.joml.Quaternionf().rotateY((float)Math.toRadians(direction * 90)).rotateX((float)-Math.PI / 2);
            var normal = rotation.transform(new org.joml.Vector3f(0, 0, 1));
            assertEquals(1F, normal.y, .0001F);
            assertEquals(0F, normal.x, .0001F);
            assertEquals(0F, normal.z, .0001F);
            var right = rotation.transform(new org.joml.Vector3f(1, 0, 0));
            assertEquals((float)Math.cos(Math.toRadians(direction * 90)), right.x, .0001F);
            assertEquals(-(float)Math.sin(Math.toRadians(direction * 90)), right.z, .0001F);
        }
    }
}
