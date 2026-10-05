package com.yinfires.icecore.compat.jade;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.farming.SeedBagCrops;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BushBlock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

/** Jade is only discovered through this annotation, so nothing here loads without Jade installed. */
@WailaPlugin(ICECore.MOD_ID)
public final class ICECoreJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // Every seed-bag crop part (CropBlock, FD rice, Collector's Reap bushes) is a BushBlock.
        registration.registerBlockIcon(SeedBagIconProvider.INSTANCE, BushBlock.class);
    }

    /** Shows the matching seed bag instead of the crop's pick-block item, for every part of the plant. */
    enum SeedBagIconProvider implements IBlockComponentProvider {
        INSTANCE;

        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ICECore.MOD_ID, "seed_bag_icon");

        @Override
        public IElement getIcon(BlockAccessor accessor, IPluginConfig config, IElement currentIcon) {
            SeedBagCrops.Crop crop = SeedBagCrops.find(accessor.getBlock());
            return crop == null ? currentIcon : IElementHelper.get().item(new ItemStack(crop.seedBag()));
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        /** Run after upstream icon providers so the seed bag wins. */
        @Override
        public int getDefaultPriority() {
            return 10_000;
        }

        @Override
        public boolean isRequired() {
            return true;
        }
    }
}
