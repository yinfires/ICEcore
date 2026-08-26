package com.yinfires.icecore.adventure;
import com.yinfires.icecore.ICECore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;
import com.yinfires.icecore.network.ICECoreNetwork;
import com.yinfires.icecore.network.ServerBoundGiveItemPacket;

@Mod.EventBusSubscriber(modid=ICECore.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE, value=Dist.CLIENT)
public final class AdventureItemEvents {
    private static int holdTicks;
    private AdventureItemEvents() {}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.START) return; Minecraft m=Minecraft.getInstance(); if(m.player==null) return;
        if(!m.player.isCreative() && m.player.getAbilities().invulnerable) return;
        if(m.gameMode==null || m.gameMode.getPlayerMode()!=net.minecraft.world.level.GameType.ADVENTURE) return;
        if(m.options.keyDrop.isDown()) {
            if (++holdTicks >= 4) { holdTicks = 0; if (m.screen == null) send(m, false); }
        } else holdTicks = 0;
    }
    public static void handleDropKey(Minecraft m, boolean all) {
        if (m.player == null || m.screen != null) return;
        send(m, all || net.minecraft.client.gui.screens.Screen.hasControlDown());
    }
    private static void send(Minecraft m, boolean all){ if(!(m.hitResult instanceof EntityHitResult hit) || !(hit.getEntity() instanceof Player p) || p==m.player) return; ICECoreNetwork.sendToServer(new ServerBoundGiveItemPacket(p.getUUID(), all)); }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){ Minecraft m=Minecraft.getInstance(); if(m.player==null || m.gameMode==null || m.gameMode.getPlayerMode()!=net.minecraft.world.level.GameType.ADVENTURE || m.player.getMainHandItem().isEmpty() || !(m.hitResult instanceof EntityHitResult hit) || !(hit.getEntity() instanceof Player p) || p==m.player) return; GuiGraphics g=e.getGuiGraphics(); Component c=Component.translatable("icecore.adventure_give.hud"); int x=(m.getWindow().getGuiScaledWidth()-m.font.width(c))/2; g.drawString(m.font,c,x,m.getWindow().getGuiScaledHeight()/2+12,0xFFFFFFFF); }
}
