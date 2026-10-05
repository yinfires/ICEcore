package com.yinfires.icecore.adventure;
import com.yinfires.icecore.ICECore;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import com.yinfires.icecore.compat.smartkeyprompts.AdventureGivePrompt;
import com.yinfires.icecore.network.ICECoreNetwork;
import com.yinfires.icecore.network.ServerBoundGiveItemPacket;

@Mod.EventBusSubscriber(modid=ICECore.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE, value=Dist.CLIENT)
public final class AdventureItemEvents {
    private static int holdTicks;
    private static final boolean SMART_KEY_PROMPTS = ModList.get().isLoaded("smartkeyprompts");
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
    @SubscribeEvent
    public static void prompt(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !SMART_KEY_PROMPTS) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gameMode == null || minecraft.screen != null
                || minecraft.gameMode.getPlayerMode() != net.minecraft.world.level.GameType.ADVENTURE
                || minecraft.player.getMainHandItem().isEmpty()
                || !(minecraft.hitResult instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof Player target) || target == minecraft.player) return;
        AdventureGivePrompt.submit(minecraft.options.keyDrop.getKey().getName());
    }
}
