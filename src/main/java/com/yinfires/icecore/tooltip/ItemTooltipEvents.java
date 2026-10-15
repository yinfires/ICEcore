package com.yinfires.icecore.tooltip;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.config.ICECoreClientConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@Mod.EventBusSubscriber(modid = ICECore.MOD_ID, value = Dist.CLIENT)
public final class ItemTooltipEvents {
    private static final ItemDescriptions DESCRIPTIONS = new ItemDescriptions();
    private ItemTooltipEvents() {}

    private static List<Component> replacement(ItemStack stack) {
        return DESCRIPTIONS.get(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    public static void filterOriginal(ItemStack stack, Player player, List<Component> lines) {
        if (stack.isEmpty()) return;
        List<Component> replacement = replacement(stack);
        TooltipRules.applyOriginal(lines, replacement, ICECoreClientConfig.hideOriginalDetails(),
                ICECoreClientConfig.hideFoodEffects());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void addedEffects(ItemTooltipEvent event) {
        if (!event.getItemStack().isEmpty() && ICECoreClientConfig.hideFoodEffects()) {
            TooltipRules.removeFoodEffects(event.getToolTip());
        }
    }

    @Mod.EventBusSubscriber(modid = ICECore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {}
        @SubscribeEvent
        public static void register(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) Registration::reload);
        }
        private static void reload(ResourceManager resources) {
            DESCRIPTIONS.reload(FMLPaths.CONFIGDIR.get().resolve("icecore/item_descriptions.json"));
        }
    }
}
