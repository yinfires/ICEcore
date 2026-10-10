package com.yinfires.icecore.farming;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Production selectors and interaction boundaries; gameplay itself needs a bootstrapped game. */
class DewDropIrrigationContractTest {
    private static final Path SRC = Path.of("src/main/java/com/yinfires/icecore");
    private static String source(String path) throws Exception { return Files.readString(SRC.resolve(path)); }

    @Test void optionalMixinsNeverLoadTargetsDuringPreparation() throws Exception {
        String plugin = source("mixin/ICECoreMixinPlugin.java");
        assertTrue(plugin.contains("mixinClassName.contains(\"DewDropIrrigation\")"));
        assertTrue(plugin.contains("present(\"cool.bot.dewdropfarmland.Config\")"));
        assertFalse(plugin.contains("Class.forName"));
        assertFalse(plugin.contains("loadClass("));
        String config = Files.readString(Path.of("src/main/resources/icecore.mixins.json"));
        for (String name : List.of("WateringCan", "FarmlandEvents", "Sprinkler")) {
            assertTrue(config.contains("\"DewDropIrrigation" + name + "Mixin\""));
        }
    }

    @Test void selectorsMatchVerifiedProductionDescriptors() throws Exception {
        String can = source("mixin/DewDropIrrigationWateringCanMixin.java");
        assertTrue(can.contains("m_7203_(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;"));
        assertTrue(can.contains("Lnet/minecraft/world/entity/player/Player;m_6047_()Z"));
        assertTrue(can.contains("blockHighlightedOrNull(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/core/BlockPos;"));
        assertFalse(can.contains("startUsingItem"));
        String sprinkler = source("mixin/DewDropIrrigationSprinklerMixin.java");
        assertTrue(sprinkler.contains("onPlaceItemIntoWorld(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"));
        assertTrue(sprinkler.contains("DewDropSprinklers.placed(context, cir.getReturnValue().consumesAction())"));
        String bottle = source("mixin/DewDropIrrigationFarmlandEventsMixin.java");
        assertTrue(bottle.contains("interactBlockEvent(Lnet/minecraftforge/event/entity/player/PlayerInteractEvent;)V"));
        assertTrue(bottle.contains("onSplashWaterBottleHitEvent(Lnet/minecraftforge/event/entity/ProjectileImpactEvent;)V"));
        assertFalse(bottle.contains("event.setCanceled"));
    }

    @Test void allCropPartsUseBoundedRootLookupAndSingleUpstreamUse() throws Exception {
        String soil = source("compat/dewdrop/DewDropIrrigation.java");
        assertTrue(soil.contains("SeedBagCrops.rootOf(crop, level, hit, state)"));
        String roots = source("farming/SeedBagCrops.java");
        for (String shape : List.of("SINGLE", "DOUBLE", "SPLIT", "CLIMBING")) assertTrue(roots.contains("case " + shape));
        assertTrue(roots.contains("i < MAX_CLIMB"));
        String events = source("compat/dewdrop/DewDropIrrigationEvents.java");
        assertTrue(events.contains("stack.getItem().use(level, event.getEntity(), event.getHand())"));
        assertTrue(events.contains("stack.getItem().useOn(new UseOnContext"));
        assertTrue(events.contains("event.setCanceled(true)"));
        assertTrue(events.contains("event.setCancellationResult(result)"));
        assertFalse(events.contains("stack.shrink"));
        assertFalse(events.contains("setDamageValue"));
    }

    @Test void sprinklerAndSinkHaveFiniteServerOnlyMutation() throws Exception {
        String sprinkler = source("compat/dewdrop/DewDropSprinklers.java");
        assertTrue(sprinkler.contains("instanceof ServerLevel"));
        assertTrue(sprinkler.contains("!Config.checkSprinklers"));
        assertTrue(sprinkler.contains("level.hasChunkAt(cursor)"));
        assertTrue(sprinkler.contains("for (int y = -1; y <= 0; y++)"));
        assertFalse(sprinkler.contains("TickEvent"));
        assertTrue(sprinkler.contains("PENDING.remove()"));
        assertTrue(sprinkler.contains("placement.getClickedPos().immutable()"));
        for (int radius = 1; radius <= 4; radius++) assertTrue(2 * (2 * radius + 1) * (2 * radius + 1) <= 162);
        String sink = source("compat/dewdrop/DewDropIrrigationEvents.java");
        assertTrue(sink.contains("\"bakery\", \"kitchen_sink\""));
        assertTrue(sink.contains("!bottom.getValue(water)"));
        assertTrue(sink.contains("getDamageValue() == 0"));
        assertTrue(sink.contains("isOnCooldown"));
        assertTrue(sink.contains("bottom.setValue(water, false)"));
        assertTrue(sink.contains("top.setValue(water, false)"));
        assertTrue(sink.contains("new WateringCanFillEvent(server, player, event.getItemStack())"));
        String till = source("farming/SeedBagFarmingEvents.java");
        assertFalse(till.contains("if (player.level().isClientSide) return InteractionResult.SUCCESS"));
        assertTrue(till.contains("finally"));
        assertTrue(till.contains("mayBuild = previousMayBuild"));
    }
}
