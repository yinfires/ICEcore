package com.yinfires.icecore.time;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.client.cutscene.CameraPose;
import com.yinfires.icecore.client.cutscene.CutsceneCamera;
import com.yinfires.icecore.client.cutscene.CutsceneFade;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Time-skip cutscene: drives the timeline, day-time fast-forward, day-summary
 * overlay and the black-screen fades, and delegates all camera/input/overlay
 * suppression to {@link CutsceneCamera}. The look, pacing and content are unchanged
 * from before the camera framework was extracted.
 */
@Mod.EventBusSubscriber(modid=ICECore.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class TimeCutsceneClient {
    private static ClientBoundCutscenePacket packet;
    private static long lastSequence=Long.MIN_VALUE;
    private static long elapsedClientTicks;
    private static boolean acked;
    private TimeCutsceneClient(){}
    public static boolean active(){return packet!=null;}
    public static void accept(ClientBoundCutscenePacket value){
        if(value.sequence()<lastSequence)return;
        if(value.sequence()==lastSequence){if(value.abort())reset();return;}
        lastSequence=value.sequence();
        if(value.abort()){reset();return;}
        NaturalDaySummaryClient.reset();
        TimeVoteClientState.suppressLabelsUntilUpdate();
        if(CutsceneCamera.cameraActive())CutsceneCamera.dropCamera();
        packet=value;elapsedClientTicks=0L;acked=false;
        CutsceneCamera.engage();
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END||packet==null)return;Minecraft mc=Minecraft.getInstance();if(mc.player==null||mc.level==null){reset();return;}
        if(mc.isPaused())return;
        elapsedClientTicks++;
        double ticks=elapsedTicks();TimeTimings t=packet.timings();
        if(!CutsceneCamera.cameraActive()&&ticks>=t.fadeToCameraTicks()){
            CutsceneCamera.takeCamera(cameraPose());
        }
        int fastStart=t.fadeToCameraTicks()+t.revealCameraTicks();
        if(ticks>=fastStart&&ticks<fastStart+t.fastForwardTicks()){
            double x=Math.min(1D,(ticks-fastStart)/t.fastForwardTicks());double eased=1D-Math.pow(1D-x,3D);TimeClientState.setVisualDayTime(packet.fromTime()+((packet.targetTime()-packet.fromTime())*eased));
        }else if(ticks>=fastStart+t.fastForwardTicks())TimeClientState.setVisualDayTime(packet.targetTime());
        int summaryDuration=DaySummaryRenderer.durationTicks(packet.income(),t);
        int restore=t.fadeToCameraTicks()+t.revealCameraTicks()+t.fastForwardTicks()+summaryDuration+t.fadeToPlayerTicks();
        if(CutsceneCamera.cameraActive()&&ticks>=restore)CutsceneCamera.dropCamera();
        int total=restore+t.revealPlayerTicks();if(!acked&&ticks>=total){acked=true;long completedSequence=packet.sequence();TimeNetworking.ack(completedSequence);reset();return;}
    }
    @SubscribeEvent public static void render(RenderGuiEvent.Post e){if(packet==null)return;render(e.getGuiGraphics());}
    private static void render(GuiGraphics g){
        double tick=elapsedTicks();TimeTimings t=packet.timings();int fast=t.fadeToCameraTicks()+t.revealCameraTicks();int summaryStart=fast+t.fastForwardTicks();int fadeBack=summaryStart+DaySummaryRenderer.durationTicks(packet.income(),t);int restore=fadeBack+t.fadeToPlayerTicks();
        // Two shared black-screen fades: one hides the cut to the camera, one the cut
        // back to the player. Both use the reusable CutsceneFade envelope.
        float black=Math.max(
                CutsceneFade.envelope(tick,0,t.fadeToCameraTicks(),t.fadeToCameraTicks(),t.revealCameraTicks()),
                CutsceneFade.envelope(tick,fadeBack,t.fadeToPlayerTicks(),restore,t.revealPlayerTicks()));
        if(tick>=summaryStart&&tick<fadeBack)DaySummaryRenderer.render(g,packet.income(),t,tick-summaryStart);
        CutsceneFade.fill(g,black,CutsceneFade.BLACK);
    }
    private static double elapsedTicks(){return elapsedClientTicks+Minecraft.getInstance().getFrameTime();}
    private static CameraPose cameraPose(){TimeCamera c=packet.camera();return new CameraPose(c.x(),c.y(),c.z(),c.yaw(),c.pitch());}
    private static void reset(){CutsceneCamera.end();packet=null;acked=false;}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){reset();lastSequence=Long.MIN_VALUE;TimeClientState.reset();TimeVoteClientState.reset();}
}
