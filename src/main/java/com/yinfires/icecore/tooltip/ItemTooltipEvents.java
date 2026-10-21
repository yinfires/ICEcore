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
    private record LayoutKey(DescriptionAction action, int width, boolean keycaps) {}
    private static final java.util.Map<LayoutKey, DescriptionActionTooltip> ACTION_LAYOUTS = new java.util.LinkedHashMap<>();
    public static List<Component> common(net.minecraft.resources.ResourceLocation block, net.minecraft.resources.ResourceLocation item) {
        return DESCRIPTIONS.common(block, item);
    }
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
        RecipeManagerTooltips.append(event.getItemStack(), event.getToolTip());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void gather(net.minecraftforge.client.event.RenderTooltipEvent.GatherComponents event) {
        var elements = event.getTooltipElements();
        boolean keycaps = net.minecraftforge.fml.ModList.get().isLoaded("smartkeyprompts");
        int maxWidth = Math.max(1, Math.min(event.getScreenWidth() - 24,
                event.getMaxWidth() > 0 ? event.getMaxWidth() : 320));
        int width = 0;
        for (var element : elements) if (element.left().isPresent()) width=Math.max(width,net.minecraft.client.Minecraft.getInstance().font.width(element.left().get()));
        for (int i = 0; i < elements.size(); i++) {
            var text = elements.get(i).left().orElse(null);
            if (text instanceof Component component && component.getContents() instanceof DescriptionAction action) {
                elements.set(i, com.mojang.datafixers.util.Either.right(new DescriptionActionTooltip.Data(action, maxWidth, keycaps)));
            }
            if (text instanceof Component component && component.getContents() instanceof FoodCategory category)
                elements.set(i,com.mojang.datafixers.util.Either.right(new FoodCategoryTooltip.Data(category.text(),Math.min(width,maxWidth),i+1<elements.size())));
        }
    }

    private static DescriptionActionTooltip layout(DescriptionActionTooltip.Data data) {
        var key = new LayoutKey(data.action(), data.maxWidth(), data.keycaps());
        var cached = ACTION_LAYOUTS.get(key);
        if (cached != null) return cached;
        if (ACTION_LAYOUTS.size() >= 256) ACTION_LAYOUTS.clear();
        var created = new DescriptionActionTooltip(data);
        ACTION_LAYOUTS.put(key, created);
        return created;
    }

    @Mod.EventBusSubscriber(modid = ICECore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {}
        @SubscribeEvent
        public static void register(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) Registration::reload);
        }
        @SubscribeEvent
        public static void factories(net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent event) {
            event.register(DescriptionActionTooltip.Data.class, ItemTooltipEvents::layout);
            event.register(FoodCategoryTooltip.Data.class, FoodCategoryTooltip::new);
        }
        private static void reload(ResourceManager resources) {
            ACTION_LAYOUTS.clear();
            DESCRIPTIONS.reload(FMLPaths.CONFIGDIR.get().resolve("icecore/item_descriptions.json"));
            RecipeManagerTooltips.reload(FMLPaths.CONFIGDIR.get().resolve("icecore/recipe_manager_items.json"));
        }
    }
}
