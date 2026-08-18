package com.yinfires.icecore.compat.cozycafe.jei;

import com.yinfires.icecore.compat.cozycafe.CozyCafeCompat;
import com.yinfires.icecore.network.ICECoreNetwork;
import com.yinfires.icecore.network.ServerBoundAddMenuItemPacket;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class CozyCafeGhostIngredientHandler implements IGhostIngredientHandler<Screen> {
    @Override
    @SuppressWarnings("unchecked")
    public <I> List<Target<I>> getTargetsTyped(Screen screen,
                                                ITypedIngredient<I> ingredient,
                                                boolean doStart) {
        if (!CozyCafeCompat.isMenuSelectorScreen(screen)
                || ingredient.getType() != VanillaTypes.ITEM_STACK
                || !(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return List.of();
        }

        AbstractContainerMenu menu = containerScreen.getMenu();
        if (!CozyCafeCompat.isMenuSelectorMenu(menu) || menu.slots.isEmpty()) {
            return List.of();
        }

        Slot inputSlot = menu.getSlot(0);
        Rect2i area = new Rect2i(
                containerScreen.getGuiLeft() + inputSlot.x,
                containerScreen.getGuiTop() + inputSlot.y,
                16,
                16
        );
        return List.of((Target<I>) new ItemStackTarget(area));
    }

    @Override
    public void onComplete() {
    }

    @Override
    public boolean shouldHighlightTargets() {
        return true;
    }

    private static final class ItemStackTarget implements Target<ItemStack> {
        private final Rect2i area;

        private ItemStackTarget(Rect2i area) {
            this.area = area;
        }

        @Override
        public Rect2i getArea() {
            return area;
        }

        @Override
        public void accept(ItemStack ingredient) {
            if (ingredient.isEmpty()) {
                return;
            }

            ItemStack stack = ingredient.copy();
            stack.setCount(1);
            ICECoreNetwork.sendToServer(new ServerBoundAddMenuItemPacket(stack));
        }
    }
}
