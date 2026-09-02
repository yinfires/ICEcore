package com.yinfires.icecore.npc;

import com.yinfires.icecore.ICECore;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class NpcEvents {
    private NpcEvents() {}

    @Mod.EventBusSubscriber(modid = ICECore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeEvents {
        @SubscribeEvent public static void addReloadListener(AddReloadListenerEvent event) { event.addListener(NpcDefinitionManager.INSTANCE); }
        @SubscribeEvent public static void registerCommands(RegisterCommandsEvent event) { NpcCommands.register(event.getDispatcher()); }
    }

    @Mod.EventBusSubscriber(modid = ICECore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event) { event.put(ModEntities.NPC.get(), NpcEntity.createAttributes().build()); }
    }
}
