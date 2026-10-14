package com.yinfires.icecore.mixin;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

final class GrillingOilPressMixinTest {
    @Test
    void productionSelectorSuppressesResidueBeforeTransfer() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/GrillingOilPressMixin.java"));
        assertTrue(source.contains("method = \"tryFinish()Z\""));
        assertTrue(source.contains("(Lnet/minecraft/world/level/ItemLike;I)Lnet/minecraft/world/item/ItemStack;"));
        assertTrue(source.contains("return ItemStack.EMPTY;"));
        assertTrue(Files.readString(Path.of("src/main/resources/icecore.mixins.json")).contains("\"GrillingOilPressMixin\""));
    }

    @Test
    void absentModIsSkippedWithoutDefiningTargetClass() throws Exception {
        assertFalse(new ICECoreMixinPlugin().shouldApplyMixin(
                "missing.grilling.OilPressBlockEntity", "com.yinfires.icecore.mixin.GrillingOilPressMixin"));
        String plugin = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/ICECoreMixinPlugin.java"));
        assertFalse(plugin.contains("Class.forName"));
        assertFalse(plugin.contains("loadClass("));
        assertTrue(plugin.contains("getResource(resourceName)"));
    }
}
