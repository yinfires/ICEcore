package com.yinfires.icecore.tooltip;

import com.google.gson.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class RecipeManagerTooltips {
    private record Entry(String category, List<Component> tags, Integer price) {}
    private static volatile Map<ResourceLocation, Entry> items = Map.of();
    private RecipeManagerTooltips() {}
    public static boolean contains(ItemStack stack) { return items.containsKey(ForgeRegistries.ITEMS.getKey(stack.getItem())); }
    public static void reload(Path path) {
        if (!Files.exists(path)) { items=Map.of(); return; }
        try(var reader=Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Map<ResourceLocation, Entry> next=new HashMap<>();
            JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("items").entrySet().forEach(e->{
                var value=e.getValue().getAsJsonObject(); List<Component> tags=new ArrayList<>();
                value.getAsJsonArray("tags").forEach(t->tags.add(Component.Serializer.fromJson(t)));
                next.put(ResourceLocation.parse(e.getKey()),new Entry(value.get("category").getAsString(),List.copyOf(tags),value.has("price")?value.get("price").getAsInt():null));
            }); items=Map.copyOf(next);
        } catch(Exception ex) { com.mojang.logging.LogUtils.getLogger().error("Cannot reload recipe manager item metadata; keeping previous snapshot",ex); }
    }
    public static void append(ItemStack stack,List<Component> lines) {
        Entry entry=items.get(ForgeRegistries.ITEMS.getKey(stack.getItem()));if(entry==null||lines.isEmpty())return;
        if(lines.stream().anyMatch(c->c.getContents() instanceof FoodCategory))return;
        // Remove only the known CozyCafe producers, never arbitrary price-like text.
        lines.removeIf(RecipeManagerTooltips::cafeLine);
        lines.removeIf(c->c.getContents() instanceof FoodCategory);
        int index=1;
        lines.add(index++,new FoodCategory(Component.translatable("tooltip.icecore.food_category."+entry.category()).withStyle(ChatFormatting.GRAY)).component());
        if(!entry.tags().isEmpty()) {
            var joined=Component.empty().withStyle(ChatFormatting.GRAY);
            for(int i=0;i<entry.tags().size();i++){if(i>0)joined.append(" ");joined.append(entry.tags().get(i));}
            lines.add(index++,joined);
        }
        if(entry.price()!=null) {
            lines.add(index++,Component.translatable("tooltip.icecore.food_price",entry.price()).withStyle(ChatFormatting.GRAY));
            if(com.yinfires.icecore.compat.cozycafe.CafeServingRules.requiresPlate(stack, entry.category().equals("drink")))
                lines.add(index,Component.translatable("tooltip.icecore.food_plating").withStyle(ChatFormatting.GRAY));
        }
    }
    public static boolean cafeLine(Component component) {
        if(!(component.getContents() instanceof TranslatableContents t))return false;
        return Set.of("gui.cozycafe.menu_selector.price","gui.cozycafe.menu_selector.item_category",
            "gui.cozycafe.menu_selector.bowl_food","gui.cozycafe.menu_selector.bottle_drink").contains(t.getKey());
    }
}
