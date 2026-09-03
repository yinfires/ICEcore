package com.yinfires.icecore.journal.client;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.quest.client.QuestClientState;
import com.yinfires.icecore.quest.client.QuestScreen;
import com.yinfires.icecore.tutorial.client.TutorialClientState;
import com.yinfires.icecore.tutorial.client.TutorialScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opens the quest/tutorial screens on key press and resets client caches on logout. */
@Mod.EventBusSubscriber(modid = ICECore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class JournalClientEvents {
    private JournalClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        if (JournalKeybinds.OPEN_QUESTS.consumeClick()) {
            minecraft.setScreen(new QuestScreen());
        }
        if (JournalKeybinds.OPEN_TUTORIALS.consumeClick()) {
            minecraft.setScreen(new TutorialScreen());
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        QuestClientState.reset();
        TutorialClientState.reset();
        com.yinfires.icecore.quest.client.QuestTrackerHud.reset();
    }
}
