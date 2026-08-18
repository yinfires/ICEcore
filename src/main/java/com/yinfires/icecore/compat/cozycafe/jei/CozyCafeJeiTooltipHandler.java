package com.yinfires.icecore.compat.cozycafe.jei;

import com.mojang.datafixers.util.Either;
import com.yinfires.icecore.compat.cozycafe.CozyCafeCompat;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public final class CozyCafeJeiTooltipHandler {
    private volatile IJeiRuntime runtime;

    public void setRuntime(IJeiRuntime runtime) {
        this.runtime = runtime;
    }

    @SubscribeEvent
    public void onGatherComponents(RenderTooltipEvent.GatherComponents event) {
        if (!CozyCafeCompat.isMenuSelectorScreen(Minecraft.getInstance().screen)) {
            return;
        }

        ItemStack itemStack = event.getItemStack();
        if (itemStack.isEmpty() || !isJeiIngredientUnderMouse(itemStack)) {
            return;
        }

        CozyCafeCompat.getMenuItemInfo(itemStack.getItem()).ifPresent(info -> appendMenuInfo(event.getTooltipElements(), info));
    }

    private boolean isJeiIngredientUnderMouse(ItemStack itemStack) {
        IJeiRuntime currentRuntime = runtime;
        if (currentRuntime == null) {
            return false;
        }

        ItemStack ingredient = currentRuntime.getIngredientListOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
        return ingredient != null
                && !ingredient.isEmpty()
                && ingredient.getItem() == itemStack.getItem();
    }

    private static void appendMenuInfo(List<Either<FormattedText, TooltipComponent>> tooltipElements,
                                       CozyCafeCompat.MenuItemInfo info) {
        Component price = Component.translatable("gui.cozycafe.menu_selector.price", info.price());
        if (containsLine(tooltipElements, price)) {
            return;
        }

        tooltipElements.add(Either.left(price));
        String categoryName = Component.translatable(
                "category.cozycafe." + info.category()
        ).getString();
        tooltipElements.add(Either.left(Component.translatable(
                "gui.cozycafe.menu_selector.item_category",
                categoryName
        ).withStyle(ChatFormatting.GRAY)));
        if (info.bowlFood()) {
            tooltipElements.add(Either.left(Component.translatable(
                    "gui.cozycafe.menu_selector.bowl_food"
            ).withStyle(ChatFormatting.GRAY)));
        }
        if (info.bottleDrink()) {
            tooltipElements.add(Either.left(Component.translatable(
                    "gui.cozycafe.menu_selector.bottle_drink"
            ).withStyle(ChatFormatting.RED)));
        }
    }

    private static boolean containsLine(List<Either<FormattedText, TooltipComponent>> tooltipElements,
                                        Component expected) {
        String expectedText = expected.getString();
        for (Either<FormattedText, TooltipComponent> element : tooltipElements) {
            if (element.left().map(FormattedText::getString).filter(expectedText::equals).isPresent()) {
                return true;
            }
        }
        return false;
    }
}
