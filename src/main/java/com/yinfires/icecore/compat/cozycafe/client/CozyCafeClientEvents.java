package com.yinfires.icecore.compat.cozycafe.client;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.compat.cozycafe.CozyCafeCompat;
import com.yinfires.icecore.network.ICECoreNetwork;
import com.yinfires.icecore.network.ServerBoundAddMenuItemPacket;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ICECore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CozyCafeClientEvents {
    private static Screen interceptedMouseReleaseScreen;
    private static int interceptedMouseReleaseButton = -1;

    private CozyCafeClientEvents() {
    }

    @SubscribeEvent
    public static void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        Screen screen = event.getScreen();
        if (interceptedMouseReleaseScreen != null) {
            clearInterceptedMouseRelease();
        }

        if (!CozyCafeCompat.isMenuSelectorScreen(screen)
                || !(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return;
        }

        AbstractContainerMenu menu = containerScreen.getMenu();
        if (!CozyCafeCompat.isMenuSelectorMenu(menu) || menu.slots.isEmpty()) {
            return;
        }

        double relativeMouseX = event.getMouseX() - containerScreen.getGuiLeft();
        double relativeMouseY = event.getMouseY() - containerScreen.getGuiTop();
        Slot hoveredSlot = findSlotAt(menu, relativeMouseX, relativeMouseY);
        if (hoveredSlot == null) {
            return;
        }

        int menuSlotIndex = menu.slots.indexOf(hoveredSlot);
        if (menuSlotIndex < 0) {
            return;
        }

        if (menuSlotIndex > 0) {
            if (Screen.hasShiftDown()
                    && (event.getButton() == 0 || event.getButton() == 1)
                    && hoveredSlot.hasItem()) {
                sendMenuAdditionRequest(event, hoveredSlot.getItem(), false);
            }
            return;
        }

        ItemStack carriedStack = menu.getCarried();
        if (!carriedStack.isEmpty()) {
            sendMenuAdditionRequest(event, carriedStack, true);
        }
    }

    @SubscribeEvent
    public static void onMouseButtonReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (interceptedMouseReleaseScreen == null) {
            return;
        }
        if (interceptedMouseReleaseScreen != event.getScreen()) {
            clearInterceptedMouseRelease();
            return;
        }
        if (interceptedMouseReleaseButton != event.getButton()) {
            return;
        }

        clearInterceptedMouseRelease();
        event.setCanceled(true);
    }

    private static Slot findSlotAt(AbstractContainerMenu menu, double relativeMouseX, double relativeMouseY) {
        for (Slot slot : menu.slots) {
            if (slot.isActive()
                    && relativeMouseX >= slot.x && relativeMouseX < slot.x + 16
                    && relativeMouseY >= slot.y && relativeMouseY < slot.y + 16) {
                return slot;
            }
        }
        return null;
    }

    private static void sendMenuAdditionRequest(ScreenEvent.MouseButtonPressed.Pre event,
                                                ItemStack stack,
                                                boolean interceptRelease) {
        ItemStack requestedStack = stack.copy();
        requestedStack.setCount(1);
        if (interceptRelease) {
            interceptedMouseReleaseScreen = event.getScreen();
            interceptedMouseReleaseButton = event.getButton();
        }
        event.setCanceled(true);
        ICECoreNetwork.sendToServer(new ServerBoundAddMenuItemPacket(requestedStack));
    }

    private static void clearInterceptedMouseRelease() {
        interceptedMouseReleaseScreen = null;
        interceptedMouseReleaseButton = -1;
    }
}
