package com.yinfires.icecore.compat.youkaisfeasts;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class YoukaisFeastsEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(YoukaisFeastsEvents.class);

    private YoukaisFeastsEvents() {
    }

    public static void register() {
        LOGGER.info("注册 youkaisfeasts 兼容事件");
    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        // 不使用 enqueueWork，直接在事件中执行以确保在游戏主线程早期运行
        LOGGER.info("初始化 youkaisfeasts 料理台材料映射");

        // 注册炸鱼专用标签和模型映射
        TagKey<Item> friedPerchTag = YoukaisFeastsCompat.createForgeTag("raw_fishes/fried_perch");
        YoukaisFeastsCompat.registerSushiTopMapping("fried_perch", friedPerchTag);
    }
}
