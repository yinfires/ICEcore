package com.yinfires.icecore.compat.kaleidoscopecookery;

import com.github.ysbbbbbb.kaleidoscopecookery.crafting.soupbase.FluidSoupBase;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.soupbase.SoupBaseManager;
import com.github.ysbbbbbb.kaleidoscopecookery.api.item.IHasContainer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.common.SoundAction;
import net.minecraftforge.common.SoundActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public final class KaleidoscopeCookeryCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(KaleidoscopeCookeryCompat.class);
    private static final ResourceLocation CANOLA_OIL_BUCKET = ResourceLocation.fromNamespaceAndPath("kaleidoscope_grilling", "canola_oil_bucket");
    private static final ResourceLocation SOUP_BASE_ID = ResourceLocation.fromNamespaceAndPath("icecore", "canola_oil");
    private static final int CANOLA_OIL_TINT = 0xFFC08A24;

    private KaleidoscopeCookeryCompat() {
    }

    public static Optional<ItemStack> resolveContainerItem(ItemStack filled) {
        if (filled.isEmpty() || !(filled.getItem() instanceof IHasContainer container)) return Optional.empty();
        Item item = container.getContainerItem();
        return item == null || item == Items.AIR ? Optional.empty() : Optional.of(item.getDefaultInstance());
    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        Item bucket = ForgeRegistries.ITEMS.getValue(CANOLA_OIL_BUCKET);
        if (!(bucket instanceof BucketItem)) {
            LOGGER.warn("未找到可用的菜籽油桶 {}，跳过森罗物语厨房汤底兼容", CANOLA_OIL_BUCKET);
            return;
        }
        SoupBaseManager.registerSoupBase(new CanolaOilSoupBase(SOUP_BASE_ID, bucket, CANOLA_OIL_TINT));
        LOGGER.info("已注册森罗物语厨房菜籽油汤底兼容：{} -> {}", CANOLA_OIL_BUCKET, SOUP_BASE_ID);
    }

    private static final class CanolaOilSoupBase extends FluidSoupBase {
        private CanolaOilSoupBase(ResourceLocation name, Item bucketItem, int bubbleColor) {
            super(name, bucketItem, bubbleColor);
        }

        @Override
        public ItemStack getReturnContainer(Level level, LivingEntity entity, ItemStack soupBase) {
            playBucketSound(level, entity, SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY);
            return new ItemStack(Items.BUCKET);
        }

        @Override
        public ItemStack getReturnSoupBase(Level level, LivingEntity entity, ItemStack container) {
            playBucketSound(level, entity, SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL);
            return bucketItem.getDefaultInstance();
        }

        private void playBucketSound(Level level, LivingEntity entity, SoundAction action, SoundEvent fallback) {
            SoundEvent sound = fluid.getFluidType().getSound(entity, action);
            if (sound == null) {
                sound = fallback;
            }
            level.playSound(null, entity.getX(), entity.getY() + 0.5D, entity.getZ(),
                    sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }
}
