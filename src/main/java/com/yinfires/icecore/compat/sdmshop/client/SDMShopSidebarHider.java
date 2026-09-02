package com.yinfires.icecore.compat.sdmshop.client;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.compat.sdmshop.SDMShopCompat;
import dev.ftb.mods.ftblibrary.sidebar.SidebarButton;
import dev.ftb.mods.ftblibrary.sidebar.SidebarButtonCreatedEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Hides SDM Shop Rework's inventory sidebar button.
 * <p>FTB Library merges every pack's {@code sidebar_buttons.json} via {@code getResourceStack}, so an empty
 * resource override cannot remove the upstream button. Instead we attach an always-false visibility condition
 * to the {@code sdmshoprework:shop} button as it is created.
 */
@Mod.EventBusSubscriber(modid = ICECore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SDMShopSidebarHider {
    private static final String SHOP_BUTTON_ID = "sdmshoprework:shop";

    private SDMShopSidebarHider() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!SDMShopCompat.isLoaded()) {
            return;
        }
        SidebarButtonCreatedEvent.EVENT.register(SDMShopSidebarHider::onButtonCreated);
    }

    private static void onButtonCreated(SidebarButtonCreatedEvent created) {
        SidebarButton button = created.getButton();
        if (button != null && SHOP_BUTTON_ID.equals(button.getId().toString())) {
            button.addVisibilityCondition(() -> false);
        }
    }
}
