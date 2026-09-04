package com.yinfires.icecore.client.cutscene;

import com.yinfires.icecore.ICECore;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Reusable cutscene view controller shared by every scripted camera animation
 * (time skip, island entrance, ...). It owns only the view-side concerns that are
 * identical across cutscenes: taking over the camera with a fixed pose, pinning
 * the look angle, and suppressing player input and content overlays (the HUD, Jade,
 * the held item). Timing, fades, on-screen content and networking stay with each
 * consumer — the black screen and pacing differ per cutscene.
 *
 * <p>Two independent phases:
 * <ul>
 *   <li><b>engaged</b> — the whole cutscene: movement keys, mouse clicks and the
 *       held-item render are suppressed. Start with {@link #engage()}.
 *   <li><b>camera active</b> — a sub-phase where the view is actually driven by an
 *       invisible camera entity: the HUD/overlays are hidden and the look angle is
 *       pinned. Enter with {@link #takeCamera}, leave with {@link #dropCamera()}.
 * </ul>
 * A consumer wanting an immediate cinematic (island entrance) calls both at once;
 * one that fades the player view out first (time skip) engages, then takes the
 * camera a few ticks later behind the black screen.
 *
 * <p>Client main/render thread only; plain static fields suffice (one cutscene at
 * a time).
 */
@Mod.EventBusSubscriber(modid = ICECore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CutsceneCamera {
    private static boolean engaged;
    private static ArmorStand camera;
    private static CameraPose pose;

    private CutsceneCamera() {
    }

    /** True for the whole cutscene: input and the held item are suppressed. */
    public static boolean engaged() {
        return engaged;
    }

    /** True while the view is driven by the cutscene camera: HUD hidden, angle pinned. */
    public static boolean cameraActive() {
        return camera != null;
    }

    /** Begins whole-cutscene input suppression without moving the camera yet. */
    public static void engage() {
        engaged = true;
    }

    /**
     * Takes over the view with a fixed pose, or moves an already-active camera to a
     * new one (supports future keyframed shots). Implies {@link #engage()}.
     */
    public static void takeCamera(CameraPose target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        engaged = true;
        pose = target;
        if (camera == null) {
            camera = new ArmorStand(mc.level, target.x(), target.y(), target.z());
            camera.setInvisible(true);
            camera.setNoGravity(true);
        }
        applyPose();
        camera.setOldPosAndRot();
        mc.setCameraEntity(camera);
    }

    /** Returns the view to the player without ending input suppression. */
    public static void dropCamera() {
        Minecraft mc = Minecraft.getInstance();
        if (camera != null && mc.player != null) {
            // Camera interpolation reads the target's previous transform. Collapse the
            // player's to its current one before switching so the first restored world
            // and hand frame cannot interpolate from a stale position or rotation.
            mc.player.setOldPosAndRot();
            mc.player.yHeadRotO = mc.player.yHeadRot;
            mc.player.yBodyRotO = mc.player.yBodyRot;
            mc.setCameraEntity(mc.player);
        }
        camera = null;
        pose = null;
    }

    /** Fully ends the cutscene: restores the camera and releases all suppression. */
    public static void end() {
        dropCamera();
        engaged = false;
        KeyMapping.releaseAll();
    }

    private static void applyPose() {
        CameraPose t = pose;
        // The recorded pose stores the player's eye Y; an ArmorStand renders from its
        // own eye height, so sink the stand by that height to line the eyes up.
        camera.absMoveTo(t.x(), cameraBaseY(t.y(), camera.getEyeHeight()), t.z(), t.yaw(), t.pitch());
        camera.yHeadRot = t.yaw();
        camera.yHeadRotO = t.yaw();
    }

    /** Feet Y for the camera stand so that its eyes land on the recorded eye Y. */
    public static double cameraBaseY(double recordedEyeY, float cameraEyeHeight) {
        return recordedEyeY - cameraEyeHeight;
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !engaged) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            end();
            return;
        }
        // Hold the recorded transform every tick so nothing drifts, and swallow the
        // movement the player keeps trying to feed in.
        if (camera != null) {
            applyPose();
            camera.setOldPosAndRot();
        }
        if (mc.screen == null) {
            KeyMapping.releaseAll();
        }
        mc.player.input.leftImpulse = 0;
        mc.player.input.forwardImpulse = 0;
        mc.player.input.jumping = false;
        mc.player.input.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles e) {
        if (camera != null && pose != null) {
            e.setYaw(pose.yaw());
            e.setPitch(pose.pitch());
            e.setRoll(0F);
        }
    }

    @SubscribeEvent
    public static void mouse(InputEvent.MouseButton.Pre e) {
        if (engaged && Minecraft.getInstance().screen == null) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hideHud(RenderGuiOverlayEvent.Pre e) {
        if (cameraActive()) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hideHand(RenderHandEvent e) {
        if (engaged) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
        if (engaged || camera != null) {
            end();
        }
    }
}
