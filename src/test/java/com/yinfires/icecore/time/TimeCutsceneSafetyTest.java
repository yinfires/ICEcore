package com.yinfires.icecore.time;

import com.yinfires.icecore.client.cutscene.CutsceneCamera;
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

    /** The reusable camera/input/overlay suppression lives here after the extraction. */
    private static String cutsceneCamera() throws IOException {
        return Files.readString(Path.of("src/main/java/com/yinfires/icecore/client/cutscene/CutsceneCamera.java"),
                StandardCharsets.UTF_8);
    }

    @Test
    void nonCancelableKeyEventIsNeverCanceled() throws IOException {
        // Cancelling InputEvent.Key crashes the game; neither the cutscene nor the
        // shared camera controller may touch it (they only clear impulses/releaseAll).
        assertFalse(source("TimeCutsceneClient.java").contains("InputEvent.Key"));
        assertFalse(cutsceneCamera().contains("InputEvent.Key"));
    }

    @Test
    void configuredCameraIsStableAndHidesNormalHud() throws IOException {
        String camera = cutsceneCamera();
        assertTrue(camera.contains("ViewportEvent.ComputeCameraAngles"));
        assertTrue(camera.contains("e.setYaw(pose.yaw())"));
        assertTrue(camera.contains("e.setPitch(pose.pitch())"));
        assertTrue(camera.contains("RenderGuiOverlayEvent.Pre"));
        // HUD/overlays hidden only while the camera actually drives the view.
        assertTrue(camera.contains("hideHud(RenderGuiOverlayEvent.Pre"));
        assertTrue(camera.contains("cameraActive()"));
        // The held item is hidden for the whole engaged cutscene, camera or not.
        assertTrue(camera.contains("hideHand(RenderHandEvent"));
        // Anti-interpolation: collapse both the camera's and the player's previous
        // transform so no restored frame lerps from a stale pose.
        assertTrue(camera.contains("camera.setOldPosAndRot()"));
        assertTrue(camera.contains("mc.player.setOldPosAndRot()"));
    }

    @Test
    void singleplayerPauseFreezesCutsceneWithoutBlockingScreens() throws IOException {
        String cutscene = source("TimeCutsceneClient.java");
        assertTrue(cutscene.contains("if(mc.isPaused())return"));
        assertTrue(cutscene.contains("elapsedClientTicks++"));
        assertTrue(cutscene.contains("elapsedClientTicks+Minecraft.getInstance().getFrameTime()"));
        assertFalse(cutscene.contains("System.currentTimeMillis()"));
        assertFalse(cutscene.contains("ScreenEvent.Opening"));
        assertFalse(cutscene.contains("PauseScreen"));
        // Input suppression must yield whenever a screen is open so menus stay usable.
        String camera = cutsceneCamera();
        assertTrue(camera.contains("Minecraft.getInstance().screen == null"));
        assertTrue(camera.contains("mc.screen == null"));
        assertTrue(camera.contains("KeyMapping.releaseAll()"));
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
        assertEquals(72.0D, CutsceneCamera.cameraBaseY(73.7775D, 1.7775F), 0.0001D);
    }

    @Test
    void jadeDirectPostRendererIsExplicitlyBlockedDuringCamera() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/JadeOverlayRendererMixin.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("snownee.jade.overlay.OverlayRenderer"));
        assertTrue(source.contains("method = \"renderOverlay478757\""));
        assertTrue(source.contains("CutsceneCamera.cameraActive()"));
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
