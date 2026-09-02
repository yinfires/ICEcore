package com.yinfires.icecore;

import com.yinfires.icecore.network.ICECoreNetwork;
import com.yinfires.icecore.item.ModCreativeTabs;
import com.yinfires.icecore.item.ModItems;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

@Mod(ICECore.MOD_ID)
public final class ICECore {
    public static final String MOD_ID = "icecore";

    public ICECore() {
        ICECoreNetwork.register();
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        if (ModList.get().isLoaded("cozycafe") && ModList.get().isLoaded("kaleidoscope_tavern")) {
            com.yinfires.icecore.compat.cozycafe.board.CozyCafeBoardEvents.register();
        }
    }
}
