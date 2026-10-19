package com.yinfires.icecore.spoilage;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

class ShelfLifeRegressionTest {
    private static ClassNode read(ZipFile zip, String name) throws Exception {
        ClassNode node = new ClassNode();
        new ClassReader(zip.getInputStream(zip.getEntry(name + ".class"))).accept(node, 0);
        return node;
    }
    @Test void nativePlateClearsHandBeforeDirtyDeliverySoConsumptionMustBeDeferred() throws Exception {
        try (var zip = new ZipFile("libs/cozycafe-1.10-all.jar")) {
            var node = read(zip, "io/github/chakyl/cozycafe/item/ServingPlateItem");
            var use = node.methods.stream().filter(m -> m.name.equals("m_7203_")).findFirst().orElseThrow();
            int shrink = -1, lastDelivery = -1, index = 0;
            for (var instruction : use.instructions) {
                if (instruction instanceof MethodInsnNode call) {
                    if (call.name.equals("m_41774_")) shrink = index;
                    if (call.name.equals("m_36054_")) lastDelivery = index;
                }
                index++;
            }
            assertTrue(shrink >= 0 && lastDelivery > shrink);
            var fix = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/ShelfLifeCozyCafePlateMixin.java"));
            assertTrue(fix.contains("@Redirect") && fix.contains("@At(\"RETURN\")"));
            assertTrue(fix.contains("getObject().shrink(1)"));
        }
    }
    @Test void nativeFridgeMenuBypassesSingleContainerGetter() throws Exception {
        try (var zip = new ZipFile("libs/cookingforblockheads-forge-1.20.1-16.0.15.jar")) {
            var node = read(zip, "net/blay09/mods/cookingforblockheads/tile/FridgeBlockEntity");
            var combined = node.methods.stream().filter(m -> m.name.equals("getCombinedContainer")).findFirst().orElseThrow();
            boolean directField = false, getter = false;
            for (var instruction : combined.instructions) {
                if (instruction instanceof FieldInsnNode field && field.name.equals("container")) directField = true;
                if (instruction instanceof MethodInsnNode call && call.name.equals("getContainer")) getter = true;
            }
            assertTrue(directField); assertFalse(getter);
            String fix = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/ShelfLifeFridgeMixin.java"));
            assertTrue(fix.contains("getCombinedContainer()Lnet/minecraft/world/Container;"));
            assertTrue(fix.contains("neighbour.getContainer()"));
        }
    }
    @Test void creativeDisplayUsesGameModeAndExpiredDescriptionIsGray() throws Exception {
        String fix = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/ShelfLifeTooltipMixin.java"));
        assertTrue(fix.contains("gameMode.getPlayerMode()"));
        assertTrue(fix.contains("GameType.CREATIVE"));
        assertTrue(fix.contains("translatable(\"tooltip.icecore.expired_food\").withStyle(net.minecraft.ChatFormatting.GRAY)"));
        assertFalse(fix.contains("translatable(\"tooltip.icecore.shelf_life\", days).withStyle"));
    }
}
