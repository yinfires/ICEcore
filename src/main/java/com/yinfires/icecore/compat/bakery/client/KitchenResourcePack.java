package com.yinfires.icecore.compat.bakery.client;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/** Explicitly selected above ordinary mod assets, independent of mod discovery order. */
@Mod.EventBusSubscriber(modid = "icecore", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class KitchenResourcePack {
    private KitchenResourcePack() {}

    @SubscribeEvent public static void packs(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES
                || !com.yinfires.icecore.compat.bakery.KitchenConnections.available()) return;
        var root = ModList.get().getModFileById("icecore").getFile().findResource("resourcepacks/kitchen_bricks");
        event.addRepositorySource(consumer -> {
            var pack = Pack.readMetaAndCreate("icecore:kitchen_bricks", Component.translatable("block.bakery.brick_counter"),
                    true, id -> new PathPackResources(id, root, false), PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.BUILT_IN);
            if (pack != null) consumer.accept(pack);
        });
    }
}
