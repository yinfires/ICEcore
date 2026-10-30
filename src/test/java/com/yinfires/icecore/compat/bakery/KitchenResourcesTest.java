package com.yinfires.icecore.compat.bakery;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;

class KitchenResourcesTest {
    private static final Path ROOT = Path.of("src/main/resources/resourcepacks/kitchen_bricks/assets");
    private static JsonObject json(Path file) throws Exception { return JsonParser.parseString(Files.readString(file)).getAsJsonObject(); }

    @Test void allCounterVariantsHaveFullBaseFourPixelTopAndUnscaledStoveSideUv() throws Exception {
        for (var shape : KitchenConnections.Shape.values()) {
            var model = json(ROOT.resolve("icecore/models/block/kitchen_counter_" + shape.name().toLowerCase(Locale.ROOT) + ".json"));
            var elements = model.getAsJsonArray("elements");
            assertEquals(2, elements.size());
            var base = elements.get(0).getAsJsonObject(); var slab = elements.get(1).getAsJsonObject();
            assertEquals(JsonParser.parseString("[0,0,0]"), base.get("from"));
            assertEquals(JsonParser.parseString("[16,12,16]"), base.get("to"));
            assertEquals(JsonParser.parseString("[0,12,0]"), slab.get("from"));
            assertEquals(JsonParser.parseString("[16,16,16]"), slab.get("to"));
            assertEquals("farmersdelight:block/stove_side", model.getAsJsonObject("textures").get("brick").getAsString());
            for (var side : List.of("north", "east", "south", "west")) {
                var face = base.getAsJsonObject("faces").getAsJsonObject(side);
                assertEquals(JsonParser.parseString("[0,4,16,16]"), face.get("uv"));
                assertEquals(side, face.get("cullface").getAsString());
            }
        }
        // Omitting the old type property intentionally matches every existing saved type.
        var states = json(ROOT.resolve("bakery/blockstates/brick_counter.json")).getAsJsonObject("variants");
        assertEquals(4, states.size());
        for (var facing : List.of("north", "east", "south", "west")) assertTrue(states.has("facing=" + facing));
    }

    @Test void sinkEmptyAndFilledModelsKeepTheirBasinAndOnlyExtendTheBrickBase() throws Exception {
        try (var jar = new JarFile("libs/[馥郁烘焙] letsdo-bakery-forge-2.0.6.jar")) {
            for (var name : List.of("brick_sink_bottom", "brick_sink_bottom_water")) {
                var entry = "assets/bakery/models/block/" + name + ".json";
                JsonObject nativeModel;
                try (var in = jar.getInputStream(jar.getJarEntry(entry))) {
                    nativeModel = JsonParser.parseString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                }
                var model = json(ROOT.resolve("bakery/models/block/" + name + ".json"));
                var original = nativeModel.getAsJsonArray("elements"); var changed = model.getAsJsonArray("elements");
                assertEquals(original.size(), changed.size());
                assertEquals(JsonParser.parseString("[0,0,0]"), changed.get(0).getAsJsonObject().get("from"));
                for (int i = 1; i < original.size(); i++) {
                    var expected = original.get(i).deepCopy().getAsJsonObject();
                    if (i == 3) expected.getAsJsonObject("faces").remove("down");
                    assertEquals(expected, changed.get(i), "Basin/water/rim must be preserved: " + i);
                }
                assertEquals(original.get(0).getAsJsonObject().getAsJsonObject("faces").get("up"),
                        changed.get(0).getAsJsonObject().getAsJsonObject("faces").get("up"));
            }
        }
    }

    @Test void sinkProductionInjectionExistsInActualUpstreamAndIsOptional() throws Exception {
        var target = new ClassNode();
        try (var jar = new JarFile("libs/[沉浸农艺] letsdo-farm_and_charm-forge-1.0.14.jar");
             var in = jar.getInputStream(jar.getJarEntry("net/satisfy/farm_and_charm/core/block/SinkBlock.class"))) {
            new ClassReader(in).accept(target, 0);
        }
        String descriptor = "(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;";
        assertTrue(target.methods.stream().anyMatch(m -> m.name.equals("m_5940_") && m.desc.equals(descriptor)));
        var config = json(Path.of("src/main/resources/icecore.mixins.json"));
        assertTrue(config.getAsJsonArray("mixins").asList().stream().anyMatch(e -> e.getAsString().equals("WorkstationBakerySinkShapeMixin")));
        assertTrue(new com.yinfires.icecore.mixin.ICECoreMixinPlugin().shouldApplyMixin("java.lang.Object", "WorkstationBakerySinkShapeMixin"));
        assertFalse(new com.yinfires.icecore.mixin.ICECoreMixinPlugin().shouldApplyMixin("absent.mod.SinkBlock", "WorkstationBakerySinkShapeMixin"));
    }
}
