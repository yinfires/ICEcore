package com.yinfires.icecore.fridge;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

final class FridgeContractTest {
    private static String read(String path) throws Exception { return Files.readString(Path.of(path), StandardCharsets.UTF_8); }

    @Test void productionSelectorsAndBridgeDoNotDependOnRefmap() throws Exception {
        String type = read("src/main/java/com/yinfires/icecore/mixin/FridgeBlockEntityTypeMixin.java");
        assertTrue(type.contains("private Set<Block> f_58915_"));
        assertTrue(type.contains("@Final @Mutable"));
        assertTrue(type.contains("implements FridgeBlockTypeAccess"));
        assertFalse(Files.exists(Path.of("src/main/java/com/yinfires/icecore/mixin/FridgeBlockTypeAccess.java")));
        String pairing = read("src/main/java/com/yinfires/icecore/mixin/FridgePairingMixin.java");
        for (String name : new String[]{"getBaseFridge", "findNeighbourFridge"}) {
            assertTrue(pairing.contains(name + "()Lnet/blay09/mods/cookingforblockheads/tile/FridgeBlockEntity;"));
        }
        assertTrue(pairing.contains("getDefaultName()Lnet/minecraft/network/chat/Component;"));
        assertFalse(pairing.contains("Class.forName"));
        assertFalse(pairing.contains("loadClass("));
    }

    @Test void placementChecksBothCellsAndSupportsForgeSnapshotRollback() throws Exception {
        String item = read("src/main/java/com/yinfires/icecore/fridge/IntegratedFridgeItem.java");
        for (String contract : new String[]{"isOutsideBuildHeight(upperPos)", "isWithinBounds(upperPos)",
                "canBeReplaced(upperContext)", "super.canPlace(upperContext, upper)", "mayInteract(player, upperPos)",
                "mayUseItemAt(upperPos", "original.restore(true, false)", "finally"}) assertTrue(item.contains(contract), contract);
        String block = read("src/main/java/com/yinfires/icecore/fridge/IntegratedFridgeBlock.java");
        assertTrue(block.contains("level.restoringBlockSnapshots"));
        assertTrue(block.contains("FridgePairing.matches(state, other)"));
        assertTrue(block.contains("fridge.dropItems(level, pos)"));
        assertFalse(block.contains("super.onRemove("));
        assertFalse(block.contains("fridge.getCombinedContainer("));
        assertTrue(block.contains("fridge.hasIceUpgrade = false"));
        assertTrue(block.contains("fridge.hasPreservationUpgrade = false"));
        assertTrue(block.contains("PushReaction.BLOCK"));
    }

    @Test void resourcesRepresentBothHalvesAndNoRecipe() throws Exception {
        var states = JsonParser.parseString(read("src/main/resources/assets/icecore/blockstates/fridge.json"))
                .getAsJsonObject().getAsJsonObject("variants");
        assertEquals(8, states.size());
        for (String half : new String[]{"lower", "upper"}) for (String facing : new String[]{"north", "south", "east", "west"}) {
            assertEquals("cookingforblockheads:block/fridge_large_" + half,
                    states.getAsJsonObject("model=large_" + half + ",facing=" + facing).get("model").getAsString());
        }
        var model = JsonParser.parseString(read("src/main/resources/assets/icecore/models/item/fridge.json")).getAsJsonObject();
        double minY = 100, maxY = -100;
        for (var element : model.getAsJsonArray("elements")) {
            var box = element.getAsJsonObject();
            minY = Math.min(minY, box.getAsJsonArray("from").get(1).getAsDouble());
            maxY = Math.max(maxY, box.getAsJsonArray("to").get(1).getAsDouble());
        }
        assertEquals(0, minY); assertEquals(29, maxY); // Upstream double fridge is 29/16 blocks tall.
        assertFalse(Files.exists(Path.of("src/main/resources/data/icecore/recipes/fridge.json")));
        for (String lang : new String[]{"zh_cn", "en_us"}) assertTrue(JsonParser.parseString(read(
                "src/main/resources/assets/icecore/lang/" + lang + ".json")).getAsJsonObject().has("block.icecore.fridge"));
    }

    @Test void wrenchChecksBothInventoriesAndClearsBothEntitiesBeforeRemovingBlocks() throws Exception {
        String structure = read("src/main/java/com/yinfires/icecore/building/BuildingStructure.java");
        assertTrue(structure.contains("\"large_lower\", \"large_upper\", PairKind.VERTICAL"));
        String actions = read("src/main/java/com/yinfires/icecore/building/BuildingServerActions.java");
        assertTrue(actions.indexOf("level.removeBlockEntity(part)") < actions.indexOf("level.removeBlock(part, false)"));
        assertTrue(actions.contains("BuildingStructure.positions(state, primary)"));
        String mixin = read("src/main/java/com/yinfires/icecore/mixin/FridgePairingMixin.java");
        assertTrue(mixin.contains("implements com.yinfires.icecore.mixing.ContainerContentAccess"));
        assertTrue(mixin.contains("self.getContainer().isEmpty()"));
        assertTrue(mixin.contains("self.hasIceUpgrade || self.hasPreservationUpgrade"));
        assertTrue(mixin.contains("stack.save(new net.minecraft.nbt.CompoundTag()).hashCode()"));
        for (String file : new String[]{"BuildingRules", "BuildingClientRules"}) {
            String rules = read("src/main/java/com/yinfires/icecore/building/" + file + ".java");
            assertTrue(rules.contains("target.above()"));
        }
    }
}
