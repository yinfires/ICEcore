package com.yinfires.icecore.compat.kaleidoscopecookery;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** Contract checks for verified Forge APIs; no incomplete Minecraft bootstrap. */
final class OilPotInteractionsTest {
    private String source(String name) throws Exception {
        return Files.readString(Path.of("src/main/java/com/yinfires/icecore/compat/kaleidoscopecookery/" + name + ".java"));
    }

    @Test
    void canceledInteractionsAndOtherHandsCannotMutate() throws Exception {
        String code = source("OilPotInteractions");
        assertTrue(code.contains("EventPriority.LOWEST, false"));
        assertTrue(code.contains("event.isCanceled()"));
        assertTrue(code.contains("event.getUseBlock() == Event.Result.DENY"));
        assertTrue(code.contains("event.getUseItem() == Event.Result.DENY"));
        assertTrue(code.contains("event.getHand() != InteractionHand.MAIN_HAND"));
        assertTrue(code.contains("player.isSpectator()"));
        assertTrue(code.contains("!level.mayInteract(player, pos)"));
        assertTrue(code.contains("event.setCanceled(true)"));
        assertTrue(code.contains("event.setCancellationResult(InteractionResult.sidedSuccess"));
    }

    @Test
    void pickupRetainsUpstreamMetadataAndOnlyGivesAfterRemoval() throws Exception {
        String code = source("OilPotInteractions");
        assertTrue(code.contains("held.isEmpty() && player.isShiftKeyDown()"));
        assertTrue(code.contains("pot.getCloneItemStack(state, event.getHitVec(), level, pos, player)"));
        assertTrue(code.contains("state.getValue(OilPotBlock.WATERLOGGED)"));
        assertTrue(code.contains("Blocks.WATER.defaultBlockState()"));
        assertTrue(code.indexOf("if (level.isClientSide) return;") < code.indexOf("pot.getCloneItemStack"));
        assertTrue(code.contains("if (level.setBlock(pos, replacement, 3))"));
        assertFalse(code.contains("level.destroyBlock("));
    }

    @Test
    void bucketExchangeIsServerOnlyAndConsumesEvenInCreative() throws Exception {
        String code = source("GrillingOilPotBuckets");
        assertTrue(code.contains("OIL_PER_BUCKET = 8"));
        assertTrue(code.contains("\"canola\".equals(pot.grilling$getOilType())"));
        assertTrue(code.contains("level.isClientSide || count < OIL_PER_BUCKET"));
        assertTrue(code.indexOf("consumeClick(event)") < code.indexOf("count < OIL_PER_BUCKET"));
        assertTrue(code.indexOf("count < OIL_PER_BUCKET") < code.indexOf("pot.grilling$setOilCount"));
        assertTrue(code.contains("pot.grilling$setOilCount(count - OIL_PER_BUCKET)"));
        assertTrue(code.contains("held.shrink(1)"));
        assertTrue(code.contains("player.getInventory().add(filled)"));
        assertTrue(code.contains("player.drop(filled, false)"));
        assertFalse(code.contains("instabuild"));
    }

    @Test
    void optionalDependenciesStayBehindSeparateGates() throws Exception {
        String entry = Files.readString(Path.of("src/main/java/com/yinfires/icecore/ICECore.java"));
        assertTrue(entry.contains("if (ModList.get().isLoaded(\"kaleidoscope_cookery\"))"));
        String code = source("OilPotInteractions");
        assertTrue(code.contains("ModList.get().isLoaded(\"kaleidoscope_grilling\")"));
        assertTrue(code.contains("GRILLING && held.is(Items.BUCKET)"));
        assertFalse(code.contains("import cn.breezeth"));
        assertFalse(code.contains("Class.forName"));
    }
}
