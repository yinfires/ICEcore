package com.yinfires.icecore.time;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TimeCutsceneSafetyTest {
    private static String source(String name) throws IOException {
        return Files.readString(Path.of("src/main/java/com/yinfires/icecore/time", name), StandardCharsets.UTF_8);
    }

    @Test
    void nonCancelableKeyEventIsNeverCanceled() throws IOException {
        String source = source("TimeCutsceneClient.java");
        assertFalse(source.contains("void key(InputEvent.Key"));
        assertFalse(source.contains("InputEvent.Key e){if(active())e.setCanceled(true)"));
    }

    @Test
    void configuredCameraIsStableAndHidesNormalHud() throws IOException {
        String source = source("TimeCutsceneClient.java");
        assertTrue(source.contains("ViewportEvent.ComputeCameraAngles"));
        assertTrue(source.contains("e.setYaw(packet.camera().yaw())"));
        assertTrue(source.contains("e.setPitch(packet.camera().pitch())"));
        assertTrue(source.contains("RenderGuiOverlayEvent.Pre"));
        assertTrue(source.contains("void hideHud(RenderGuiOverlayEvent.Pre e){if(cameraActive)e.setCanceled(true);}"));
        assertTrue(source.contains("void hideHand(RenderHandEvent e){if(active())e.setCanceled(true);}"));
        assertTrue(source.contains("camera.setOldPosAndRot()"));
        assertTrue(source.contains("mc.player.setOldPosAndRot()"));
    }

    @Test
    void singleplayerPauseFreezesCutsceneWithoutBlockingScreens() throws IOException {
        String source = source("TimeCutsceneClient.java");
        assertTrue(source.contains("if(mc.isPaused())return"));
        assertTrue(source.contains("elapsedClientTicks++"));
        assertTrue(source.contains("elapsedClientTicks+Minecraft.getInstance().getFrameTime()"));
        assertFalse(source.contains("System.currentTimeMillis()"));
        assertFalse(source.contains("ScreenEvent.Opening"));
        assertFalse(source.contains("PauseScreen"));
        assertTrue(source.contains("Minecraft.getInstance().screen==null"));
        assertTrue(source.contains("if(mc.screen==null)KeyMapping.releaseAll()"));
    }

    @Test
    void bedLabelsWaitForPostCutsceneVoteState() throws IOException {
        String cutscene = source("TimeCutsceneClient.java");
        String voteEvents = source("TimeVoteClientEvents.java");
        String voteState = source("TimeVoteClientState.java");
        assertTrue(cutscene.contains("TimeVoteClientState.suppressLabelsUntilUpdate()"));
        assertTrue(voteEvents.contains("TimeVoteClientState.labelsSuppressed()"));
        assertTrue(voteState.contains("labelsSuppressed=false"));
    }

    @Test
    void bedVoteLabelIsHiddenWhenThereIsOnlyOneParticipant() throws IOException {
        String source = source("TimeVoteClientEvents.java");
        assertTrue(source.contains("if(TimeVoteClientState.total() <= 1)return;"));
    }

    @Test
    void recordedEyePositionIsNotRaisedByCameraEntityEyeHeight() {
        assertEquals(72.0D, TimeCutsceneClient.cameraBaseY(73.7775D, 1.7775F), 0.0001D);
    }

    @Test
    void jadeDirectPostRendererIsExplicitlyBlockedDuringCamera() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/JadeOverlayRendererMixin.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("snownee.jade.overlay.OverlayRenderer"));
        assertTrue(source.contains("method = \"renderOverlay478757\""));
        assertTrue(source.contains("TimeCutsceneClient.cameraActive()"));
        assertTrue(source.contains("callback.cancel()"));
    }

    @Test
    void finalAckClearsClientStateAndOldPacketsCannotRestartIt() throws IOException {
        String source = source("TimeCutsceneClient.java");
        assertTrue(source.contains("if(value.sequence()<lastSequence)return"));
        assertTrue(source.contains("if(value.sequence()==lastSequence){if(value.abort())reset();return;}"));
        assertTrue(source.contains("lastSequence=value.sequence()"));
        assertTrue(source.contains("TimeNetworking.ack(completedSequence);reset();return"));
        assertTrue(source.contains("packet=null"));
    }

    @Test
    void serverDoesNotTeleportParticipantsEveryTick() throws IOException {
        String source = source("TimeVoteManager.java");
        assertFalse(source.contains("p.setPos(o.x,o.y,o.z)"));
        assertTrue(source.contains("player.connection.teleport(origin.x,origin.y,origin.z,origin.yaw,origin.pitch)"));
        assertTrue(source.contains("player.level().dimension()==Level.OVERWORLD"));
    }
}
