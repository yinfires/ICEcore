package com.yinfires.icecore.island;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.client.cutscene.CameraPose;
import com.yinfires.icecore.client.cutscene.CutsceneCamera;
import com.yinfires.icecore.client.cutscene.CutsceneFade;
import com.yinfires.icecore.mixin.PostChainAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client half of the island entrance cutscene. Delegates the view/input/overlay takeover
 * to {@link CutsceneCamera} and the fades to {@link CutsceneFade}, then adds the island
 * look: the real screen shakes harder and harder (a building teleport surge), warps slightly
 * inward at the peak, then whites out; the island — which the server places ONLY once the
 * client is already fully white (see the placement delay in IslandCommands) — fades in as
 * the white recedes over 2 s. Carries no block data; blocks are placed server-side.
 *
 * <p>The warp is a bonus layer: if the custom post shader cannot load (a rendering mod
 * owns the pipeline, a compile failure, ...), the camera, white hold and reveal all still
 * run — the entrance just loses its shake/warp distortion.
 *
 * <p>Timeline (t = ticks from receipt; {@code buildup} = receipt→fully-white from the packet):
 * <pre>
 *   0                     fade to black (IN_FADE)
 *   IN_FADE               take camera + load warp on the black frame
 *   IN_FADE..+IN_REVEAL   reveal the camera view (steady)
 *   shakeStart()..peak    screen shake ramps up (accelerating, Strength 0→1) into a judder
 *   peak..buildup         (peak = buildup − WHITE_TICKS) slight inward warp + white-out fills
 *   whiteFull()=buildup   screen fully white; server starts placing the island BEHIND the
 *                         white (client is white before the server places — nothing seen).
 *                         warp shut down here; the GUI white hold takes over.
 *   whiteFull()..fade     hold white until placement is done (or the safety timeout)
 *   fadeStart..+WHITE_FALL white fades out over 2 s, revealing the finished island
 *   +HOLD                 brief hold on the finished island
 *   +OUT_FADE             fade to black, drop camera on the black frame
 *   +OUT_REVEAL           reveal the player view; done
 * </pre>
 * If the done signal never arrives the fade is forced after {@link #PLACE_TIMEOUT_TICKS}.
 */
@Mod.EventBusSubscriber(modid = ICECore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IslandEntranceClient {
    private static final ResourceLocation EFFECT =
            ResourceLocation.fromNamespaceAndPath(ICECore.MOD_ID, "shaders/post/island_warp.json");
    /** Intro: fade the player view to black, then reveal the entrance camera. */
    private static final int IN_FADE = 6;
    private static final int IN_REVEAL = 6;
    /** Ticks over which the peak white-out fills the screen after the shake builds. */
    private static final int WHITE_TICKS = 6;
    /** Minimum ticks the screen holds fully white before the fade may begin. */
    private static final int MIN_WHITE_HOLD = 4;
    /** Safety: force the fade if the server done signal is lost (from whiteFull). */
    private static final int PLACE_TIMEOUT_TICKS = 600;
    /** White fades out over 2 s (40 ticks), revealing the finished island. */
    private static final int WHITE_FALL = 40;
    private static final int HOLD = 10;
    private static final int OUT_FADE = 6;
    private static final int OUT_REVEAL = 6;

    private static ClientBoundIslandEntrancePacket packet;
    private static double elapsedClientTicks;
    private static boolean effectLoaded;
    private static boolean cameraTaken;
    private static boolean warpShutdown;
    private static boolean cameraDropped;
    /** Server signalled placement done (blocks are in the world). */
    private static boolean placementDone;
    /** Tick (from receipt) the white fade-out actually started, or -1 until then. */
    private static double fadeStartTick = -1D;

    private IslandEntranceClient() {
    }

    public static boolean active() {
        return packet != null;
    }

    /** Client entry point invoked from the entrance packet (client dist only). */
    public static void begin(ClientBoundIslandEntrancePacket value) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        if (!mc.level.dimension().location().equals(value.dimension())) {
            return;
        }
        reset();
        packet = value;
        elapsedClientTicks = 0D;
        // Suppress input/hand for the whole cutscene now; the camera is taken later,
        // on the fully-black frame, so the cut to the entrance view is never seen.
        CutsceneCamera.engage();
    }

    /**
     * Server signalled placement done (client dist only). Only marks the flag; the white
     * fade-out is gated in {@link #tick} on this AND a minimum white hold, so the island is
     * always placed behind a fully-white screen and revealed only as the white recedes.
     */
    public static void finish(ClientBoundIslandEntranceDonePacket value) {
        if (packet == null || placementDone) {
            return;
        }
        if (!value.dimension().equals(packet.dimension())) {
            return;
        }
        placementDone = true;
    }

    /** Tick from receipt at which the white-out has fully covered the screen. */
    private static double whiteFull() {
        return packet.buildup();
    }

    /** Tick from receipt at which the shake begins (after the intro reveal completes). */
    private static double shakeStart() {
        return IN_FADE + IN_REVEAL;
    }

    private static void loadEffect(Minecraft mc) {
        try {
            mc.gameRenderer.loadEffect(EFFECT);
            effectLoaded = mc.gameRenderer.currentEffect() != null;
        } catch (Exception exception) {
            effectLoaded = false;
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || packet == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            reset();
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        elapsedClientTicks++;
        double t = elapsedTicks();
        // Take the camera on the intro black frame so the cut is never seen.
        if (!cameraTaken && t >= IN_FADE) {
            cameraTaken = true;
            CutsceneCamera.takeCamera(new CameraPose(packet.eyeX(), packet.eyeY(), packet.eyeZ(),
                    packet.yaw(), packet.pitch()));
            loadEffect(mc);
        }
        // Once the screen is fully white the shader has done its job and the GUI white hold
        // covers everything: shut the warp down (one tick after whiteFull so the shader and
        // the GUI white overlap for a frame — no seam). The island is placed behind this.
        if (!warpShutdown && t >= whiteFull() + 1D) {
            warpShutdown = true;
            if (effectLoaded) {
                mc.gameRenderer.shutdownEffect();
                effectLoaded = false;
            }
        }
        // Begin the white fade-out only once the screen has been fully white for a minimum
        // hold AND placement is done — so the island is always revealed out of the white,
        // never during the distortion. A lost done signal is forced through after timeout.
        if (fadeStartTick < 0D) {
            boolean held = t >= whiteFull() + MIN_WHITE_HOLD;
            boolean timedOut = t >= whiteFull() + PLACE_TIMEOUT_TICKS;
            if ((placementDone && held) || timedOut) {
                fadeStartTick = t;
            }
            return;
        }
        double r = t - fadeStartTick;
        // Drop the camera on the outro black frame.
        double dropAt = WHITE_FALL + HOLD + OUT_FADE;
        if (cameraTaken && !cameraDropped && r >= dropAt) {
            cameraDropped = true;
            CutsceneCamera.dropCamera();
        }
        if (r >= dropAt + OUT_REVEAL) {
            reset();
        }
    }

    /** Push the per-frame warp strength into the chain before GameRenderer processes it. */
    @SubscribeEvent
    public static void warp(RenderLevelStageEvent e) {
        if (packet == null || !effectLoaded || e.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        PostChain chain = Minecraft.getInstance().gameRenderer.currentEffect();
        if (!(chain instanceof PostChainAccessor accessor)) {
            return;
        }
        float s = shakeStrength();
        float w = whiteOut();
        // Our own monotonic clock (seconds from receipt); never wraps, unlike vanilla Time,
        // so the shake path can only advance and never snaps back on screen.
        float age = (float) (elapsedTicks() / 20D);
        for (PostPass pass : accessor.icecore$passes()) {
            pass.getEffect().safeGetUniform("Strength").set(s);
            pass.getEffect().safeGetUniform("White").set(w);
            pass.getEffect().safeGetUniform("Age").set(age);
        }
    }

    @SubscribeEvent
    public static void overlay(RenderGuiEvent.Post e) {
        if (packet == null) {
            return;
        }
        GuiGraphics g = e.getGuiGraphics();
        // White hold + 2 s fade-out (covers the island placement, then reveals it); black
        // transitions hide the camera cuts. The shader draws the growing iris up to full
        // white, then this GUI white takes over the opaque hold and the fade.
        CutsceneFade.fill(g, whiteHold(), CutsceneFade.WHITE);
        CutsceneFade.fill(g, blackout(), CutsceneFade.BLACK);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
        reset();
    }

    /**
     * Shake/warp build in [0,1] driven into the shader: an <em>accelerating</em> ramp over
     * {@code [shakeStart, whiteFull - WHITE_TICKS]} using an ease-in ({@code k²}) so the
     * tremor starts faint and grows into a violent judder that only ever intensifies, held
     * at 1 through the final white-out. 0 before the shake starts (intact view during reveal).
     */
    private static float shakeStrength() {
        double t = elapsedTicks();
        double start = shakeStart();
        double end = Math.max(start + 1D, whiteFull() - WHITE_TICKS);
        double k = Math.min(1D, Math.max(0D, (t - start) / (end - start)));
        return (float) (k * k);
    }

    /**
     * Shader white-out in [0,1]: 0 until the shake peaks at {@code whiteFull - WHITE_TICKS},
     * then eased 0→1 over {@link #WHITE_TICKS} as the slightly-warped screen fills white.
     * Held at 1 after (the shader shuts down a tick later; the GUI white hold takes over).
     */
    private static float whiteOut() {
        double t = elapsedTicks();
        double start = whiteFull() - WHITE_TICKS;
        if (t <= start) {
            return 0F;
        }
        return CutsceneFade.ease((float) Math.min(1D, (t - start) / WHITE_TICKS));
    }

    /**
     * GUI white cover in [0,1]: opaque from {@link #whiteFull()} (taking over from the
     * shader iris) through the placement hold, then eased 1→0 over {@link #WHITE_FALL}
     * (2 s) once the fade fires, revealing the finished island. 0 before the screen whites.
     */
    private static float whiteHold() {
        double t = elapsedTicks();
        if (t < whiteFull()) {
            return 0F;
        }
        if (fadeStartTick < 0D) {
            return 1F;
        }
        double k = (t - fadeStartTick) / WHITE_FALL;
        return k >= 1D ? 0F : 1F - CutsceneFade.ease((float) k);
    }

    /** Black cover: intro fade-in→reveal; and the outro fade-in→reveal after the hold. */
    private static float blackout() {
        double t = elapsedTicks();
        float intro = CutsceneFade.envelope(t, 0, IN_FADE, IN_FADE, IN_REVEAL);
        if (fadeStartTick < 0D) {
            return intro;
        }
        double outStart = fadeStartTick + WHITE_FALL + HOLD;
        float outro = CutsceneFade.envelope(t, outStart, OUT_FADE, outStart + OUT_FADE, OUT_REVEAL);
        return Math.max(intro, outro);
    }

    private static double elapsedTicks() {
        return elapsedClientTicks + Minecraft.getInstance().getFrameTime();
    }

    private static void reset() {
        if (packet == null && !effectLoaded && !cameraTaken) {
            return;
        }
        CutsceneCamera.end();
        if (effectLoaded) {
            Minecraft.getInstance().gameRenderer.shutdownEffect();
        }
        packet = null;
        effectLoaded = false;
        cameraTaken = false;
        warpShutdown = false;
        cameraDropped = false;
        placementDone = false;
        fadeStartTick = -1D;
        elapsedClientTicks = 0D;
    }
}
