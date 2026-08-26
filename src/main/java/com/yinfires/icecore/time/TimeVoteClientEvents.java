package com.yinfires.icecore.time;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.building.BuildingClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=ICECore.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class TimeVoteClientEvents {
    private TimeVoteClientEvents(){}
    @SubscribeEvent(priority=EventPriority.HIGHEST,receiveCanceled=true)
    public static void right(PlayerInteractEvent.RightClickBlock e){
        Minecraft mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||e.getEntity()!=mc.player)return;
        if(TimeRules.match(mc.level,e.getPos(),TimeClientState.config(),BuildingClientState.data())!=null){
            // Deny the local block call but keep the outer predicted use packet. Explicitly
            // allowing the item branch also survives Minecraft's other-hand retry.
            e.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);e.setUseItem(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
        }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||TimeCutsceneClient.active())return;Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||!(mc.hitResult instanceof BlockHitResult hit))return;
        var match=TimeRules.match(mc.level,hit.getBlockPos(),TimeClientState.config(),BuildingClientState.data());if(match==null)return;
        PoseStack pose=e.getPoseStack();var cam=e.getCamera().getPosition();pose.pushPose();pose.translate(match.labelX()-cam.x,match.labelY()-cam.y,match.labelZ()-cam.z);pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());pose.scale(-0.025F,-0.025F,0.025F);
        MultiBufferSource.BufferSource buffers=mc.renderBuffers().bufferSource();Font font=mc.font;
        String line1=Component.translatable("icecore.time.vote.required").getString();String line2=TimeVoteClientState.confirmed().size()+"/"+TimeVoteClientState.total();
        draw(font,line1,-font.width(line1)/2F,-20,pose,buffers);draw(font,line2,-font.width(line2)/2F,-9,pose,buffers);
        buffers.endBatch();int perRow=7,count=TimeVoteClientState.confirmed().size();for(int i=0;i<count;i++){var entry=TimeVoteClientState.confirmed().get(i);int row=i/perRow,cols=Math.min(perRow,count-row*perRow),col=i%perRow;float x=(col-(cols-1)/2F)*11F;drawHead(pose,entry.uuid(),x-4,3+row*11);}
        RenderSystem.enableDepthTest();pose.popPose();
    }
    private static void draw(Font font,String text,float x,float y,PoseStack pose,MultiBufferSource buffers){font.drawInBatch(text,x,y,0xFFFFFFFF,false,pose.last().pose(),buffers,Font.DisplayMode.SEE_THROUGH,0x50000000,0xF000F0);}
    private static void drawHead(PoseStack pose,java.util.UUID id,float x,float y){
        Minecraft mc=Minecraft.getInstance();var info=mc.getConnection()==null?null:mc.getConnection().getPlayerInfo(id);if(info==null)return;ResourceLocation skin=info.getSkinLocation();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);RenderSystem.setShaderTexture(0,skin);RenderSystem.enableBlend();
        com.mojang.blaze3d.vertex.Tesselator tess=com.mojang.blaze3d.vertex.Tesselator.getInstance();var b=tess.getBuilder();var m=pose.last().pose();b.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX);b.vertex(m,x,y+8,0).uv(8/64F,16/64F).endVertex();b.vertex(m,x+8,y+8,0).uv(16/64F,16/64F).endVertex();b.vertex(m,x+8,y,0).uv(16/64F,8/64F).endVertex();b.vertex(m,x,y,0).uv(8/64F,8/64F).endVertex();tess.end();RenderSystem.disableBlend();
    }
}
