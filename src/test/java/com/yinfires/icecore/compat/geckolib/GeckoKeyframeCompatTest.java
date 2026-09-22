package com.yinfires.icecore.compat.geckolib;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class GeckoKeyframeCompatTest {
    @Test
    void allObservedPackKeyframesKeepVectorAndEasingObjects() throws Exception {
        var samples = JsonParser.parseString(Files.readString(
                Path.of("src/test/resources/geckolib/wrapped-keyframes.json"))).getAsJsonArray();
        assertEquals(205, samples.size());
        int sides = 0;
        int easing = 0;
        for (var sample : samples) {
            JsonObject frame = sample.getAsJsonObject().getAsJsonObject("frame");
            String original = frame.toString();
            JsonObject timeline = new JsonObject();
            timeline.add("2.5", frame);
            var converted = GeckoKeyframeCompat.convertChannel(timeline);
            assertNotNull(converted, sample.getAsJsonObject().get("source").getAsString());
            int index = 0;
            for (String side : new String[]{"pre", "post"}) {
                if (!frame.has(side)) continue;
                var pair = converted.get(index++);
                assertSame(frame.get(side), pair.getSecond());
                assertEquals(side.equals("pre") ? 2.5 : 2.5000001,
                        Double.parseDouble(pair.getFirst()), 1e-12);
                if (pair.getSecond() instanceof JsonObject object) {
                    assertEquals(3, object.getAsJsonArray("vector").size());
                    sides++;
                    if (object.has("easing")) {
                        assertEquals("easeInBack", object.get("easing").getAsString());
                        easing++;
                    }
                }
            }
            assertEquals(original, frame.toString());
        }
        assertEquals(214, sides);
        assertEquals(1, easing);
    }

    @Test
    void nativeChannelsPassThroughAndMixedChannelPreservesOrder() {
        assertNull(GeckoKeyframeCompat.convertChannel(JsonParser.parseString("[0,0,0]")));
        assertNull(GeckoKeyframeCompat.convertChannel(JsonParser.parseString("{\"0\":[0,0,0]}")));
        var timeline = JsonParser.parseString("""
                {"0":[0,0,0],"1":{"pre":[1,2,3],"post":{"vector":[4,5,6],
                "easing":"easeInBack","easingArgs":[2]}},"2":{"vector":[7,8,9]}}
                """).getAsJsonObject();
        var converted = GeckoKeyframeCompat.convertChannel(timeline);
        assertEquals(4, converted.size());
        assertSame(timeline.get("0"), converted.get(0).getSecond());
        assertSame(timeline.getAsJsonObject("1").get("post"), converted.get(2).getSecond());
        assertSame(timeline.get("2"), converted.get(3).getSecond());
    }

    @Test
    void unwrapsBothSidesWithoutChangingExpressionsOrInput() {
        JsonObject frame = JsonParser.parseString("""
                {"pre":{"vector":[-5.35254,-11.31255,10.53051]},
                 "post":{"vector":["math.sin(query.anim_time * 120) * 6",0,4]}}
                """).getAsJsonObject();
        String original = frame.toString();
        assertEquals(-5.35254, GeckoKeyframeCompat.wrappedVector(frame, "pre").get(0).getAsDouble());
        assertEquals("math.sin(query.anim_time * 120) * 6",
                GeckoKeyframeCompat.wrappedVector(frame, "post").get(0).getAsString());
        assertEquals(original, frame.toString());
    }

    @Test
    void leavesNativeArraysAndUnsupportedDataToOriginalParser() {
        for (String value : new String[]{"[0,0,0]", "null", "7", "{}",
                "{\"vector\":[0,0]}"}) {
            JsonObject frame = JsonParser.parseString("{\"post\":" + value + "}").getAsJsonObject();
            assertNull(GeckoKeyframeCompat.wrappedVector(frame, "post"));
        }
        assertNull(GeckoKeyframeCompat.wrappedVector(new JsonObject(), "post"));
    }

    @Test
    void productionSelectorsAndOptionalLoadingRemainSafe() throws Exception {
        String mixin = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/GeckoLibKeyframeMixin.java"));
        assertTrue(mixin.contains("getTripletObj(Lcom/google/gson/JsonElement;)Ljava/util/List;"));
        assertTrue(mixin.contains("remap = false"));
        assertTrue(mixin.contains("cancellable = true, require = 0"));
        String plugin = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/ICECoreMixinPlugin.java"));
        assertTrue(plugin.contains("if (mixinClassName.contains(\"GeckoLib\")) return present(targetClassName);"));
        assertFalse(plugin.contains("Class.forName("));
        assertFalse(plugin.contains("loadClass("));
        JsonObject config = JsonParser.parseString(Files.readString(Path.of("src/main/resources/icecore.mixins.json"))).getAsJsonObject();
        assertTrue(config.getAsJsonArray("client").toString().contains("GeckoLibKeyframeMixin"));
        assertFalse(config.getAsJsonArray("mixins").toString().contains("GeckoLibKeyframeMixin"));
    }
}
