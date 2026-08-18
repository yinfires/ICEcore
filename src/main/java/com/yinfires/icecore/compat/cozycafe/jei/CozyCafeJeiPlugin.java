package com.yinfires.icecore.compat.cozycafe.jei;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.compat.cozycafe.CozyCafeCompat;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

@JeiPlugin
public final class CozyCafeJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ICECore.MOD_ID, "cozycafe");
    private static final CozyCafeJeiTooltipHandler TOOLTIP_HANDLER = new CozyCafeJeiTooltipHandler();
    private static boolean tooltipHandlerRegistered;

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        try {
            Class<?> screenClass = Class.forName(CozyCafeCompat.MENU_SELECTOR_SCREEN, false,
                    CozyCafeJeiPlugin.class.getClassLoader());
            if (Screen.class.isAssignableFrom(screenClass)) {
                registration.addGhostIngredientHandler((Class) screenClass, new CozyCafeGhostIngredientHandler());
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        TOOLTIP_HANDLER.setRuntime(runtime);
        if (!tooltipHandlerRegistered) {
            MinecraftForge.EVENT_BUS.register(TOOLTIP_HANDLER);
            tooltipHandlerRegistered = true;
        }
    }

    @Override
    public void onRuntimeUnavailable() {
        TOOLTIP_HANDLER.setRuntime(null);
        if (tooltipHandlerRegistered) {
            MinecraftForge.EVENT_BUS.unregister(TOOLTIP_HANDLER);
            tooltipHandlerRegistered = false;
        }
    }
}
