package com.yinfires.icecore.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

final class CookeryStockpotRulesTest {
    private static final String ROOT = "com/github/ysbbbbbb/kaleidoscopecookery/";

    @Test
    void realUpstreamLidBranchesAndLoadDescriptorsMatchBothVersions() throws Exception {
        for (String jar : new String[]{"[森罗物语：厨房] kaleidoscopecookery-1.4.1-forge+mc1.20.1.jar",
                "kaleidoscopecookery-1.5.1-forge+mc1.20.1.jar"}) {
            try (ZipFile zip = new ZipFile(Path.of("libs", jar).toFile())) {
                ClassNode block = read(zip, "block/kitchen/StockpotBlock");
                MethodNode drops = method(block, "m_49635_",
                        "(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/storage/loot/LootParams$Builder;)Ljava/util/List;");
                int stateReads = 0;
                boolean lid = false, ingredients = false;
                for (var insn : drops.instructions) {
                    if (insn instanceof FieldInsnNode field && field.name.equals("HAS_LID")) lid = true;
                    if (insn instanceof MethodInsnNode call) {
                        if (call.name.equals("m_61143_")) {
                            stateReads++;
                            assertEquals("(Lnet/minecraft/world/level/block/state/properties/Property;)Ljava/lang/Comparable;", call.desc);
                        }
                        ingredients |= call.name.equals("getInputs");
                    }
                }
                assertTrue(lid);
                assertTrue(ingredients);
                assertEquals(1, stateReads, "Redirect must affect only the lid branch");
                ClassNode entity = read(zip, "blockentity/kitchen/StockpotBlockEntity");
                method(entity, "onLitClick", "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z");
                method(entity, "m_142466_", "(Lnet/minecraft/nbt/CompoundTag;)V");
                assertTrue(entity.fields.stream().anyMatch(f -> f.name.equals("lidItem")
                        && f.desc.equals("Lnet/minecraft/world/item/ItemStack;")));
                MethodNode use = method(block, "m_6227_", "(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;");
                assertTrue(java.util.stream.StreamSupport.stream(use.instructions.spliterator(), false)
                        .anyMatch(i -> i instanceof MethodInsnNode c && c.name.equals("onLitClick")));
            }
        }
    }

    @Test
    void eventConsumesSecondaryHandButOnlyServerMainHandChangesState() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/yinfires/icecore/compat/kaleidoscopecookery/StockpotInteractions.java"));
        assertTrue(source.contains("!player.getMainHandItem().isEmpty()"));
        assertFalse(source.contains("getOffhandItem"));
        assertTrue(source.contains("!player.isShiftKeyDown()"));
        assertTrue(source.contains("event.isCanceled()"));
        assertTrue(source.contains("event.getUseBlock() == Event.Result.DENY"));
        assertTrue(source.contains("!level.mayInteract(player, pos)"));
        assertTrue(source.indexOf("event.setCanceled(true)") < source.indexOf("event.getHand() != InteractionHand.MAIN_HAND"));
        assertTrue(source.indexOf("if (level.isClientSide ||") < source.indexOf("level.setBlock("));
        assertTrue(source.contains("state.setValue(StockpotBlock.HAS_LID, closed), 3"));
        assertFalse(source.contains("getInventory()"));
        assertFalse(source.contains("startUsingItem"));
        assertTrue(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock.class
                .isAnnotationPresent(net.minecraftforge.eventbus.api.Cancelable.class));
    }

    @Test
    void injectionsClearLegacyItemWithoutChangingStateOrTransferringItems() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/CookeryStockpotEntityMixin.java"));
        assertTrue(source.contains("m_142466_(Lnet/minecraft/nbt/CompoundTag;)V"));
        assertTrue(source.contains("at = @At(\"TAIL\")"));
        assertTrue(source.contains("lidItem = ItemStack.EMPTY;"));
        assertTrue(source.contains("cir.setReturnValue(false);"));
        assertFalse(source.contains("setBlock"));
        assertFalse(source.contains("popResource"));
        String drops = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/CookeryStockpotDropsMixin.java"));
        assertTrue(drops.contains("return Boolean.FALSE;"));
        assertTrue(drops.contains("m_61143_(Lnet/minecraft/world/level/block/state/properties/Property;)Ljava/lang/Comparable;"));
        for (String name : new String[]{"CookeryStockpotEntityMixin", "CookeryStockpotDropsMixin"}) {
            assertFalse(new ICECoreMixinPlugin().shouldApplyMixin("missing.cookery.Target", "com.yinfires.icecore.mixin." + name));
            assertTrue(Files.readString(Path.of("src/main/resources/icecore.mixins.json")).split("\"client\"")[0].contains(name));
        }
    }

    private static ClassNode read(ZipFile zip, String name) throws Exception {
        ClassNode node = new ClassNode();
        try (var input = zip.getInputStream(zip.getEntry(ROOT + name + ".class"))) {
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    private static MethodNode method(ClassNode node, String name, String desc) {
        return node.methods.stream().filter(m -> m.name.equals(name) && m.desc.equals(desc)).findFirst().orElseThrow();
    }
}
