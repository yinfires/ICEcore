package com.yinfires.icecore.journal;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.quest.QuestDefinitionManager;
import com.yinfires.icecore.quest.QuestService;
import com.yinfires.icecore.tutorial.TutorialDefinitionManager;
import com.yinfires.icecore.tutorial.TutorialService;
import com.yinfires.icecore.quest.QuestNetworking;
import com.yinfires.icecore.tutorial.TutorialNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Wires the journal datapack loaders and quest/tutorial services into the server lifecycle. */
@Mod.EventBusSubscriber(modid = ICECore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class JournalEvents {
    private JournalEvents() {
    }

    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(CategoryManager.INSTANCE);
        event.addListener(QuestDefinitionManager.INSTANCE);
        event.addListener(TutorialDefinitionManager.INSTANCE);
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        QuestService.start(event.getServer());
        TutorialService.start(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        QuestService.stop();
        TutorialService.stop();
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            QuestNetworking.syncPlayer(player);
            TutorialNetworking.syncPlayer(player);
        }
    }
}
