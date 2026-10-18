package com.yinfires.icecore.workstation;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class TrashCanInteractionTest {
    @Test void integratedServerPermissionPassesBothPhysicalClientAndServerMiningGuards() throws Exception {
        // No world/player bootstrap: exemption must happen before either handler touches them.
        var field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        var unsafe = (sun.misc.Unsafe) field.get(null);
        var permission = (TrashCanLeftClickEvents.PutPermission) unsafe.allocateInstance(TrashCanLeftClickEvents.PutPermission.class);
        assertDoesNotThrow(() -> TrashCanClientEvents.preventMining(permission));
        assertDoesNotThrow(() -> TrashCanLeftClickEvents.preventMining(permission));
        assertFalse(permission.isCanceled());
    }
    @Test void batchCapacityIncludesPartialFullAndFreedHandBoundary() {
        assertEquals(64, WorkstationInventoryTransfer.maximumExchange(64, count -> true));
        assertEquals(17, WorkstationInventoryTransfer.maximumExchange(64, count -> count <= 17));
        assertEquals(0, WorkstationInventoryTransfer.maximumExchange(64, count -> false));
        assertEquals(64, WorkstationInventoryTransfer.maximumExchange(64, count -> count == 64));
        assertEquals(0, WorkstationInventoryTransfer.maximumExchange(0, count -> { fail(); return true; }));
    }
    @Test void onlyHeldLeftClickClaimsMiningAndRightClickNeverPuts() throws Exception {
        String events = read("workstation/WorkstationEvents.java");
        String branch = events.substring(events.indexOf("if (type.equals(\"TrashCanBlockEntity\"))"), events.indexOf("if (ferment &&"));
        assertTrue(branch.contains("withdrawItem(player)"));
        assertFalse(branch.contains("TrashCanInteraction.put"));
        String left = read("workstation/TrashCanLeftClickEvents.java");
        assertTrue(left.contains("!player.getMainHandItem().isEmpty() && !player.isSpectator() && !hasPriorityTool(player)"));
        assertTrue(left.contains("!(event instanceof PutPermission)"));
        String client = read("workstation/TrashCanClientEvents.java");
        assertTrue(client.contains("if (claimedPress && minecraft.player.getMainHandItem().isEmpty()) return true"));
        assertTrue(client.contains("if (!down) return false"));
        assertTrue(client.contains("ForgeHooksClient.onClickInput"));
        assertTrue(client.contains("ClientPlayerNetworkEvent.LoggingOut"));
        assertTrue(client.contains("claimedLevel = null"));
        assertFalse(client.contains("void attack(InputEvent"));
        assertFalse(left.contains("entity instanceof WorkstationAccess"));
        assertTrue(left.contains("BuiltInRegistries.BLOCK.getKey"));
        assertTrue(client.contains("shouldIntercept(event.getEntity())"));
        assertTrue(client.contains("!(event instanceof TrashCanLeftClickEvents.PutPermission) && event.getLevel().isClientSide"));
        String packet = read("workstation/ServerBoundTrashPutPacket.java");
        for (String guard : new String[]{"level.isLoaded(pos)", "level.mayInteract(player, pos)", "distanceToSqr", "hasPriorityTool", "InteractionEvents.blocked", "EVENT_BUS.post(permission)"}) assertTrue(packet.contains(guard), guard);
        assertTrue(packet.indexOf("EVENT_BUS.post(permission)") < packet.indexOf("TrashCanInteraction.put"));
    }
    @Test void bothAttackEntryPointsCancelBeforeMiningAndReleaseClearsClaim() throws Exception {
        String mixin = read("mixin/WorkstationTrashAttackMixin.java");
        assertTrue(mixin.contains("m_202354_()Z"));
        assertTrue(mixin.contains("m_91386_(Z)V"));
        assertEquals(2, mixin.split("at = @At\\(\"HEAD\"\\)", -1).length - 1);
        assertTrue(mixin.contains("TrashCanClientEvents.attack(true, true)"));
        assertTrue(mixin.contains("TrashCanClientEvents.attack(down, false)"));
        assertTrue(mixin.contains("gameMode.stopDestroyBlock()"));
        assertTrue(Files.readString(Path.of("src/main/resources/icecore.mixins.json")).substring(
                Files.readString(Path.of("src/main/resources/icecore.mixins.json")).indexOf("\"client\"")).contains("WorkstationTrashAttackMixin"));
    }
    @Test void receiveBeforeReductionAndNativeEffectsRemain() throws Exception {
        String bridge = read("workstation/TrashCanInteraction.java");
        assertTrue(bridge.contains("Math.min(stored.getCount(), held.getCount())"));
        assertTrue(bridge.contains("state.container(slot)"));
        assertTrue(bridge.contains("WorkstationContainerInteraction.canTake(required, held)"));
        assertTrue(bridge.contains("WorkstationInventoryTransfer.insert(player, stored, true)"));
        assertTrue(bridge.contains("stored.getCount() - moved"));
        assertFalse(bridge.contains("setItemInHand"));
        String mixin = read("mixin/WorkstationTrashCanMixin.java");
        assertTrue(mixin.contains("if (icecore$remainder == null) ci.cancel()"));
        assertTrue(mixin.contains("icecore$containers[i + 1]"));
        assertFalse(mixin.contains("consumeEvictedInput"));
        assertTrue(mixin.contains("storage.setStackInSlot(slot, icecore$remainder)"));
        assertFalse(mixin.contains("sendParticles("));
        assertTrue(mixin.contains("WorkstationContainerCompat.fillSound"));
        assertTrue(mixin.contains("original == net.minecraft.sounds.SoundEvents.BARREL_CLOSE"));
    }
    private static String read(String path) throws Exception { return Files.readString(Path.of("src/main/java/com/yinfires/icecore", path)); }
}
