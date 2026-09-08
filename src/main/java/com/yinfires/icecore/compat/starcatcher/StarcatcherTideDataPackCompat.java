package com.yinfires.icecore.compat.starcatcher;

import com.yinfires.icecore.ICECore;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;

import java.nio.file.Path;

/** Corrects the malformed bait restriction in Starcatcher's bundled Tide data pack. */
public final class StarcatcherTideDataPackCompat {
    private static final String STARCATCHER_MOD_ID = "starcatcher";
    private static final String TIDE_MOD_ID = "tide";
    private static final String SUPPORTED_STARCATCHER_VERSION = "3.1.4.1-FORGE-1.20.1";
    // Reuse Starcatcher's built-in pack id so the repository map replaces the
    // malformed source instead of merely adding a second pack with a duplicate
    // resource path.
    private static final String PACK_ID = "mod/starcatcher:built_in_datapacks/tide_compat";
    private static final String PACK_PATH = "resourcepacks/starcatcher_tide_compat";

    private StarcatcherTideDataPackCompat() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(StarcatcherTideDataPackCompat::addPackFinders);
    }

    private static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA || !shouldEnable()) return;

        Path packRoot = ModList.get().getModFileById(ICECore.MOD_ID).getFile().findResource(PACK_PATH);
        event.addRepositorySource(consumer -> {
            Pack pack = Pack.readMetaAndCreate(
                    PACK_ID,
                    Component.translatable("pack.icecore.starcatcher_tide_compat"),
                    true,
                    id -> new PathPackResources(id, packRoot, false),
                    PackType.SERVER_DATA,
                    // Built-in packs from Starcatcher are discovered after this repository
                    // source.  The selected data-pack list applies later entries last, so
                    // placing this pack at the bottom makes its corrected JSON the winning
                    // resource for the same tide:starcatcher/fish/shooting_starfish.json path.
                    Pack.Position.TOP,
                    PackSource.BUILT_IN
            );
            if (pack != null) consumer.accept(pack);
        });
    }

    private static boolean shouldEnable() {
        if (!ModList.get().isLoaded(TIDE_MOD_ID)) return false;
        return ModList.get().getModContainerById(STARCATCHER_MOD_ID)
                .map(container -> SUPPORTED_STARCATCHER_VERSION.equals(
                        container.getModInfo().getVersion().toString()))
                .orElse(false);
    }
}
