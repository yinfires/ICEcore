package com.yinfires.icecore;

import com.yinfires.icecore.network.ICECoreNetwork;
import com.yinfires.icecore.item.ModCreativeTabs;
import com.yinfires.icecore.item.ModItems;
import com.yinfires.icecore.npc.ModEntities;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

@Mod(ICECore.MOD_ID)
public final class ICECore {
    public static final String MOD_ID = "icecore";

    public ICECore() {
        ICECoreNetwork.register();
        com.yinfires.icecore.quest.objective.ObjectiveRegistry.bootstrap();
        com.yinfires.icecore.quest.QuestNetworking.install();
        com.yinfires.icecore.tutorial.TutorialNetworking.install();
        com.yinfires.icecore.journal.ChatBoxJournalEvents.register();
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        com.yinfires.icecore.compat.starcatcher.StarcatcherTideDataPackCompat.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModEntities.ENTITY_TYPES.register(modBus);
        if (ModList.get().isLoaded("cozycafe")) {
            com.yinfires.icecore.compat.cozycafe.seating.CozyCafeSeatingEvents.register();
        }
        if (ModList.get().isLoaded("cozycafe") && ModList.get().isLoaded("kaleidoscope_tavern")) {
            com.yinfires.icecore.compat.cozycafe.board.CozyCafeBoardEvents.register();
        }
    }
}
