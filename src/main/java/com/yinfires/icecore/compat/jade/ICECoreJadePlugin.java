package com.yinfires.icecore.compat.jade;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.farming.SeedBagCrops;
import com.yinfires.icecore.farming.RiceGrowth;
import com.yinfires.icecore.item.SeedBagDefinition.Shape;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BushBlock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.Identifiers;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

/** Jade is only discovered through this annotation, so nothing here loads without Jade installed. */
@WailaPlugin(ICECore.MOD_ID)
public final class ICECoreJadePlugin implements IWailaPlugin {
    @Override public void register(snownee.jade.api.IWailaCommonRegistration registration) {
        registration.registerItemStorage(WorkstationItemStorage.INSTANCE, net.minecraft.world.level.block.entity.BlockEntity.class);
        registration.registerFluidStorage(WorkstationFluidStorage.INSTANCE, net.minecraft.world.level.block.entity.BlockEntity.class);
        registration.registerBlockDataProvider(WorkstationJadeProvider.INSTANCE, net.minecraft.world.level.block.entity.BlockEntity.class);
    }
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(CommonDescriptionProvider.INSTANCE, net.minecraft.world.level.block.Block.class);
        registration.registerItemStorageClient(WorkstationItemStorage.INSTANCE);
        registration.registerFluidStorageClient(WorkstationFluidStorage.INSTANCE);
        registration.registerBlockComponent(WorkstationJadeProvider.INSTANCE, net.minecraft.world.level.block.Block.class);
        // Every seed-bag crop part (CropBlock, FD rice, Collector's Reap bushes) is a BushBlock.
        registration.registerBlockIcon(SeedBagIconProvider.INSTANCE, BushBlock.class);
        registration.registerBlockComponent(RiceProgressProvider.INSTANCE, BushBlock.class);
    }

    enum RiceProgressProvider implements IBlockComponentProvider {
        INSTANCE;
        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ICECore.MOD_ID, "rice_progress");

        @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            SeedBagCrops.Crop crop = SeedBagCrops.find(accessor.getBlock());
            if (crop == null || crop.definition().shape() != Shape.SPLIT) return;
            var level = accessor.getLevel();
            var root = SeedBagCrops.rootOf(crop, level, accessor.getPosition(), accessor.getBlockState());
            if (root == null) return;
            BlockState base = level.getBlockState(root);
            BlockState upper = level.getBlockState(root.above());
            int stage = RiceGrowth.stage(base.getValue(crop.age()), upper.is(crop.upper()) ? upper.getValue(crop.upperAge()) : -1);
            tooltip.remove(Identifiers.MC_CROP_PROGRESS);
            // Match Jade 11.13.3 CropProgressProvider: same translation, rounding and theme.
            var progress = stage < RiceGrowth.MATURE
                    ? IThemeHelper.get().info(String.format("%.0f%%", stage * 100.0F / RiceGrowth.MATURE))
                    : IThemeHelper.get().success(Component.translatable("tooltip.jade.crop_mature"));
            tooltip.add(Component.translatable("tooltip.jade.crop_growth", progress));
        }
        @Override public ResourceLocation getUid() { return UID; }
        @Override public int getDefaultPriority() { return 10_000; }
        @Override public boolean isRequired() { return true; }
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
