package com.yinfires.icecore.time;

import com.yinfires.icecore.ICECore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=ICECore.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class TimeCutsceneClient {
    private static ClientBoundCutscenePacket packet;
    private static long lastSequence=Long.MIN_VALUE;
    private static long startedMillis;
    private static ArmorStand camera;
    private static boolean cameraActive;
    private static boolean acked;
    private TimeCutsceneClient(){}
    public static boolean active(){return packet!=null;}
    public static boolean cameraActive(){return cameraActive;}
    public static void accept(ClientBoundCutscenePacket value){
        if(value.sequence()<lastSequence)return;
        if(value.sequence()==lastSequence){if(value.abort())reset();return;}
        lastSequence=value.sequence();
        if(value.abort()){reset();return;}
        NaturalDaySummaryClient.reset();
        if(cameraActive)restorePlayerCamera();
        packet=value;startedMillis=System.currentTimeMillis();acked=false;cameraActive=false;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END||packet==null)return;Minecraft mc=Minecraft.getInstance();if(mc.player==null||mc.level==null){reset();return;}
        double ticks=elapsedTicks();TimeTimings t=packet.timings();
        if(!cameraActive&&ticks>=t.fadeToCameraTicks()){
            activateCamera(mc);
        }
        if(cameraActive)lockCameraTransform();
        int fastStart=t.fadeToCameraTicks()+t.revealCameraTicks();
        if(ticks>=fastStart&&ticks<fastStart+t.fastForwardTicks()){
            double x=Math.min(1D,(ticks-fastStart)/t.fastForwardTicks());double eased=1D-Math.pow(1D-x,3D);TimeClientState.setVisualDayTime(packet.fromTime()+((packet.targetTime()-packet.fromTime())*eased));
        }else if(ticks>=fastStart+t.fastForwardTicks())TimeClientState.setVisualDayTime(packet.targetTime());
        int summaryDuration=DaySummaryRenderer.durationTicks(packet.income(),t);
        int restore=t.fadeToCameraTicks()+t.revealCameraTicks()+t.fastForwardTicks()+summaryDuration+t.fadeToPlayerTicks();
        if(cameraActive&&ticks>=restore){mc.setCameraEntity(mc.player);cameraActive=false;camera=null;}
        int total=restore+t.revealPlayerTicks();if(!acked&&ticks>=total){acked=true;long completedSequence=packet.sequence();TimeNetworking.ack(completedSequence);reset();return;}
        lockInput(mc);
    }
    @SubscribeEvent public static void mouse(InputEvent.MouseButton.Pre e){if(active())e.setCanceled(true);}
    @SubscribeEvent public static void key(InputEvent.Key e){if(active())KeyMapping.releaseAll();}
    @SubscribeEvent public static void cameraAngles(ViewportEvent.ComputeCameraAngles e){if(cameraActive&&packet!=null){e.setYaw(packet.camera().yaw());e.setPitch(packet.camera().pitch());e.setRoll(0F);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void hideHud(RenderGuiOverlayEvent.Pre e){if(cameraActive)e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void hideHand(RenderHandEvent e){if(cameraActive)e.setCanceled(true);}
    @SubscribeEvent public static void render(RenderGuiEvent.Post e){if(packet==null)return;render(e.getGuiGraphics());}
    private static void render(GuiGraphics g){
        Minecraft mc=Minecraft.getInstance();int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();double tick=elapsedTicks();TimeTimings t=packet.timings();int fast=t.fadeToCameraTicks()+t.revealCameraTicks();int summaryStart=fast+t.fastForwardTicks();int fadeBack=summaryStart+DaySummaryRenderer.durationTicks(packet.income(),t);int restore=fadeBack+t.fadeToPlayerTicks();int end=restore+t.revealPlayerTicks();
        float black=0F;if(tick<t.fadeToCameraTicks())black=ease((float)(tick/t.fadeToCameraTicks()));else if(tick<fast)black=1F-ease((float)((tick-t.fadeToCameraTicks())/t.revealCameraTicks()));else if(tick>=fadeBack&&tick<restore)black=ease((float)((tick-fadeBack)/t.fadeToPlayerTicks()));else if(tick>=restore&&tick<end)black=1F-ease((float)((tick-restore)/t.revealPlayerTicks()));
        if(tick>=summaryStart&&tick<fadeBack)DaySummaryRenderer.render(g,packet.income(),t,tick-summaryStart);
        int a=Math.max(0,Math.min(255,Math.round(black*255F)));if(a>=4)g.fill(0,0,w,h,(a<<24));
    }
    private static float ease(float x){x=Math.max(0,Math.min(1,x));return x*x*(3-2*x);}
    private static double elapsedTicks(){return (System.currentTimeMillis()-startedMillis)/50D;}
    private static void activateCamera(Minecraft mc){
        TimeCamera target=packet.camera();camera=new ArmorStand(mc.level,target.x(),target.y(),target.z());camera.setInvisible(true);camera.setNoGravity(true);moveCameraToRecordedEye(target);camera.setOldPosAndRot();camera.yHeadRot=target.yaw();camera.yHeadRotO=target.yaw();mc.setCameraEntity(camera);cameraActive=true;
    }
    private static void lockCameraTransform(){
        if(camera==null||packet==null)return;TimeCamera target=packet.camera();moveCameraToRecordedEye(target);camera.setOldPosAndRot();camera.yHeadRot=target.yaw();camera.yHeadRotO=target.yaw();
    }
    private static void moveCameraToRecordedEye(TimeCamera target){camera.absMoveTo(target.x(),cameraBaseY(target.y(),camera.getEyeHeight()),target.z(),target.yaw(),target.pitch());}
    static double cameraBaseY(double recordedEyeY,float cameraEyeHeight){return recordedEyeY-cameraEyeHeight;}
    private static void lockInput(Minecraft mc){
        KeyMapping.releaseAll();
        if(mc.player!=null){mc.player.input.leftImpulse=0;mc.player.input.forwardImpulse=0;mc.player.input.jumping=false;mc.player.input.shiftKeyDown=false;}
    }
    private static void restorePlayerCamera(){Minecraft mc=Minecraft.getInstance();if(mc.player!=null&&cameraActive)mc.setCameraEntity(mc.player);camera=null;cameraActive=false;}
    private static void reset(){restorePlayerCamera();packet=null;acked=false;KeyMapping.releaseAll();}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){reset();lastSequence=Long.MIN_VALUE;TimeClientState.reset();TimeVoteClientState.reset();}
}
