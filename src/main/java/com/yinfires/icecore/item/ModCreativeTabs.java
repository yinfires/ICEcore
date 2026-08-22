package com.yinfires.icecore.item;

import com.yinfires.icecore.ICECore;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(
            net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, ICECore.MOD_ID);
    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.icecore.main"))
            .icon(() -> new ItemStack(ModItems.WRENCH.get()))
            .displayItems((parameters, output) -> output.accept(ModItems.WRENCH.get()))
            .build());

    private ModCreativeTabs() {
    }
}
