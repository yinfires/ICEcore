package com.yinfires.icecore.workstation;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class TrashCanAnimationTest {
    @Test void nativeDurationDelayDuplicateOldAndExpiredSnapshots() {
        assertEquals(12, TrashCanAnimationTiming.remainingTicks(0, 1, 100, 100, false));
        assertEquals(10, TrashCanAnimationTiming.remainingTicks(0, 1, 100, 100, true));
        assertEquals(7, TrashCanAnimationTiming.remainingTicks(1, 2, 100, 105, false));
        assertEquals(5, TrashCanAnimationTiming.remainingTicks(1, 2, 100, 105, true));
        assertEquals(0, TrashCanAnimationTiming.remainingTicks(2, 2, 100, 105, false));
        assertEquals(0, TrashCanAnimationTiming.remainingTicks(3, 2, 100, 105, false));
        assertEquals(0, TrashCanAnimationTiming.remainingTicks(0, 0, 100, 100, false));
        assertEquals(0, TrashCanAnimationTiming.remainingTicks(1, 2, 100, 99, false));
        assertEquals(0, TrashCanAnimationTiming.remainingTicks(1, 2, 100, 112, false));
        assertEquals(0, TrashCanAnimationTiming.remainingTicks(1, 2, 100, 110, true));
        assertEquals(0, TrashCanAnimationTiming.remainingTicks(1, 2, 100, 10000, false));
    }
    @Test void successfulNativeRefreshSyncsStartsAndLoadNeverMutatesInventoryOrSendsPackets() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/WorkstationTrashAnimationMixin.java"));
        assertTrue(source.contains("putItem(Lnet/minecraft/world/item/ItemStack;)V"));
        assertTrue(source.contains("withdrawItem(Lnet/minecraft/world/entity/LivingEntity;)V"));
        assertEquals(2, source.split("TrashCanBlockEntity;refresh\\(\\)V", -1).length - 1);
        assertTrue(source.contains("m_183515_(Lnet/minecraft/nbt/CompoundTag;)V"));
        assertTrue(source.contains("m_142466_(Lnet/minecraft/nbt/CompoundTag;)V"));
        assertTrue(source.contains("sequence > icecore$animationSequence"));
        assertTrue(source.contains("(taking ? self.withdrawState : self.putState).start((int) tick)"));
        assertTrue(source.contains("self.putState.stop(); self.withdrawState.stop()"));
        assertTrue(source.contains("System.nanoTime() >= icecore$animationDeadline"));
        assertFalse(source.contains("setStackInSlot"));
        assertFalse(source.contains("sendBlockUpdated"));
        assertFalse(source.contains("sendToPlayer"));
        assertTrue(Files.readString(Path.of("src/main/resources/icecore.mixins.json")).contains("WorkstationTrashAnimationMixin"));
        String renderer = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/WorkstationTrashAnimationRenderMixin.java"));
        assertTrue(renderer.contains("at = @At(\"HEAD\")"));
        assertTrue(renderer.contains("access.icecore$expireTrashAnimation()"));
        assertFalse(renderer.contains("ci.cancel"));
        assertFalse(renderer.contains("animate("));
    }
}
