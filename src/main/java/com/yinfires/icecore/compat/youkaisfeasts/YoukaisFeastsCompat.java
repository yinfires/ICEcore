package com.yinfires.icecore.compat.youkaisfeasts;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class YoukaisFeastsCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(YoukaisFeastsCompat.class);
    private static final String TABLE_ITEM_MANAGER = "dev.xkmc.youkaishomecoming.content.pot.table.item.TableItemManager";

    private YoukaisFeastsCompat() {
    }

    /**
     * 为寿司料理台注册新的材料模型映射
     * @param modelName 模型名称（如 "fried_perch"）
     * @param tagKey 物品标签
     */
    public static void registerSushiTopMapping(String modelName, TagKey<Item> tagKey) {
        try {
            LOGGER.info("尝试注册寿司料理台材料映射: {} -> {}", modelName, tagKey.location());

            Class<?> managerClass = Class.forName(TABLE_ITEM_MANAGER);
            LOGGER.debug("成功加载 TableItemManager 类");

            Field sushiTopField = managerClass.getField("SUSHI_TOP");
            Object sushiTop = sushiTopField.get(null);
            LOGGER.debug("成功获取 SUSHI_TOP 字段: {}", sushiTop.getClass().getName());

            Method addMappingMethod = sushiTop.getClass().getMethod("addMapping", String.class, TagKey.class);
            LOGGER.debug("成功获取 addMapping 方法");

            addMappingMethod.invoke(sushiTop, modelName, tagKey);

            LOGGER.info("✓ 成功注册寿司料理台材料映射: {} -> {}", modelName, tagKey.location());
        } catch (ClassNotFoundException e) {
            LOGGER.error("✗ 无法找到 TableItemManager 类，youkaisfeasts 可能未加载", e);
        } catch (NoSuchFieldException e) {
            LOGGER.error("✗ 无法找到 SUSHI_TOP 字段", e);
        } catch (NoSuchMethodException e) {
            LOGGER.error("✗ 无法找到 addMapping 方法", e);
        } catch (Exception e) {
            LOGGER.error("✗ 注册寿司料理台材料映射时发生异常: {} -> {}", modelName, tagKey.location(), e);
        }
    }

    /**
     * 创建 forge 命名空间的物品标签
     */
    public static TagKey<Item> createForgeTag(String path) {
        return TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(),
                ResourceLocation.fromNamespaceAndPath("forge", path));
    }
}
