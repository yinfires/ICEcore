package com.yinfires.icecore.mixin;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SDMShopCompatProductionSelectorTest {
    private static final Path PLUGIN = Path.of(
            "src/main/java/com/yinfires/icecore/mixin/ICECoreMixinPlugin.java");
    private static final Path MIXIN = Path.of("src/main/java/com/yinfires/icecore/mixin/SDMShopEconomyMixin.java");
    private static final Path CONFIG = Path.of("src/main/resources/icecore.mixins.json");
    private static final Path FORMAT_MIXIN = Path.of(
            "src/main/java/com/yinfires/icecore/mixin/SDMShopMoneyFormatMixin.java");
    private static final Path SAVE_MIXIN = Path.of(
            "src/main/java/com/yinfires/icecore/mixin/SDMShopSaveMixin.java");
    private static final Path COMPAT = Path.of(
            "src/main/java/com/yinfires/icecore/compat/sdmshop/SDMShopCompat.java");
    private static final Path SIDEBAR_OVERRIDE = Path.of(
            "src/main/resources/assets/sdmshoprework/sidebar_buttons.json");
    private static final Path ICON = Path.of(
            "src/main/resources/assets/sdmshoprework/textures/icons/money.png");

    @Test
    void targetsVerifiedEconomyInitializer() throws IOException {
        String source = Files.readString(MIXIN, StandardCharsets.UTF_8);
        String config = Files.readString(CONFIG, StandardCharsets.UTF_8);
        assertTrue(source.contains("net.sixik.sdmshoprework.economy.EconomyManager"));
        assertTrue(source.contains("method = \"init()V\""));
        assertTrue(config.contains("\"SDMShopEconomyMixin\""));
        String formatSource = Files.readString(FORMAT_MIXIN, StandardCharsets.UTF_8);
        assertTrue(formatSource.contains("moneyString(J)Ljava/lang/String;"));
        assertTrue(formatSource.contains("moneyString(Ljava/lang/String;)Ljava/lang/String;"));
        assertTrue(config.contains("\"SDMShopMoneyFormatMixin\""));
    }

    @Test
    void saveMixinTargetsVerifiedPersistenceHook() throws IOException {
        String source = Files.readString(SAVE_MIXIN, StandardCharsets.UTF_8);
        String config = Files.readString(CONFIG, StandardCharsets.UTF_8);
        assertTrue(source.contains("net.sixik.sdmshoprework.common.shop.ShopBase"));
        assertTrue(source.contains("method = \"saveShopToFile()V\""));
        assertTrue(source.contains("captureActiveSnapshot"));
        assertTrue(config.contains("\"SDMShopSaveMixin\""));
    }

    @Test
    void openShopRegistersAgainstFixedClientShopUuid() throws IOException {
        String source = Files.readString(COMPAT, StandardCharsets.UTF_8);
        assertTrue(source.contains("net.sixik.sdmshoprework.common.shop.MultiShop"));
        assertTrue(source.contains("SHOP_MAP"));
        assertTrue(source.contains("SHOP_UUID"));
        assertTrue(source.contains("captureActiveSnapshot"));
        // Full synchronous client push so the screen's onConstruct auto-selects the first tab (SDM's setSelectedTab
        // null-guard otherwise blocks the first click when the shop is opened via command).
        assertTrue(source.contains("net.sixik.sdmshoprework.network.client.SyncShopS2C"));
    }

    @Test
    void emptySidebarOverrideRemovedInFavorOfVisibilityHook() {
        assertTrue(!Files.exists(SIDEBAR_OVERRIDE));
    }

    @Test
    void optionalMixinDetectionNeverLoadsTargetClasses() throws IOException {
        String source = Files.readString(PLUGIN, StandardCharsets.UTF_8);
        assertTrue(source.contains("getResource(resourceName)"));
        assertTrue(!source.contains("Class.forName("));
    }

    @Test
    void replacementMoneyIconKeepsUpstreamDimensionsAndTransparency() throws IOException {
        BufferedImage image = ImageIO.read(ICON.toFile());
        assertEquals(32, image.getWidth());
        assertEquals(32, image.getHeight());
        assertTrue(image.getColorModel().hasAlpha());
        assertEquals(0, image.getRGB(0, 0) >>> 24);
    }
}
