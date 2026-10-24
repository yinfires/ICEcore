package com.yinfires.icecore.mixin;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.yinfires.icecore.config.ICECoreConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ServerAdvancementsDisableTest {
    private final ServerAdvancementsDisableMixin hook = new ServerAdvancementsDisableMixin() {};

    @BeforeEach
    void configure() {
        var config = CommentedConfig.inMemory();
        ICECoreConfig.SPEC.correct(config);
        ICECoreConfig.SPEC.setConfig(config);
    }

    @AfterEach
    void reset() {
        ICECoreConfig.SPEC.setConfig(null);
    }

    private void applyHook(Map<ResourceLocation, JsonElement> definitions) throws Exception {
        var method = ServerAdvancementsDisableMixin.class.getDeclaredMethod("icecore$disableAdvancements",
                Map.class, ResourceManager.class, ProfilerFiller.class, CallbackInfo.class);
        method.setAccessible(true);
        method.invoke(hook, definitions, null, null, new CallbackInfo("apply", false));
    }

    private static Map<ResourceLocation, JsonElement> definitions() {
        var definitions = new HashMap<ResourceLocation, JsonElement>();
        var json = JsonParser.parseString("{\"criteria\":{\"never\":{\"trigger\":\"minecraft:impossible\"}}}");
        definitions.put(new ResourceLocation("minecraft:story/root"), json);
        definitions.put(new ResourceLocation("minecraft:recipes/test"), json);
        definitions.put(new ResourceLocation("tide:root"), json);
        definitions.put(new ResourceLocation("custom_pack:hidden"), json);
        return definitions;
    }

    @Test
    void defaultPreservesEveryDefinitionAndItsIdentity() throws Exception {
        assertFalse(ICECoreConfig.disableAllAdvancements());
        var definitions = definitions();
        var original = new HashMap<>(definitions);
        applyHook(definitions);
        assertEquals(original, definitions);
        original.forEach((id, json) -> assertSame(json, definitions.get(id)));
    }

    @Test
    void disablesAllNamespacesAndHiddenRecipesOnEveryReload() throws Exception {
        ICECoreConfig.DISABLE_ALL_ADVANCEMENTS.set(true);
        for (int reload = 0; reload < 3; reload++) {
            var definitions = definitions();
            applyHook(definitions);
            assertTrue(definitions.isEmpty());
        }
        ICECoreConfig.DISABLE_ALL_ADVANCEMENTS.set(false);
        var restored = definitions();
        applyHook(restored);
        assertEquals(4, restored.size());
    }

    @Test
    void vanillaReloadBuildsFreshEmptyTreesWithoutRegistryBootstrap() throws Exception {
        var manager = new TestManager();
        var tree = ServerAdvancementManager.class.getDeclaredField("advancements");
        tree.setAccessible(true);
        var previous = tree.get(manager);
        ICECoreConfig.DISABLE_ALL_ADVANCEMENTS.set(true);
        for (int reload = 0; reload < 2; reload++) {
            var definitions = definitions();
            applyHook(definitions);
            manager.load(definitions);
            assertTrue(manager.getAllAdvancements().isEmpty());
            assertNotSame(previous, tree.get(manager));
            previous = tree.get(manager);
        }
    }

    @Test
    void productionSelectorMatchesMappedTargetAndIsRegisteredOnBothSides() throws Exception {
        var mixin = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/ServerAdvancementsDisableMixin.java"));
        String descriptor = "(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V";
        assertTrue(mixin.contains("m_5787_" + descriptor));
        assertTrue(mixin.contains("@At(\"HEAD\")"));
        var mappings = Files.readString(Path.of("build/createMcpToSrg/output.tsrg"), StandardCharsets.UTF_8);
        var targetMappings = mappings.substring(mappings.indexOf("net/minecraft/server/ServerAdvancementManager net/"));
        assertTrue(targetMappings.substring(0, targetMappings.indexOf("\nnet/")).contains("apply " + descriptor + " m_5787_"));
        try (var stream = ServerAdvancementManager.class.getResourceAsStream("ServerAdvancementManager.class")) {
            var node = new ClassNode();
            new ClassReader(stream).accept(node, 0);
            assertTrue(node.methods.stream().anyMatch(m -> m.name.equals("apply") && m.desc.equals(descriptor)));
        }
        var config = JsonParser.parseString(Files.readString(Path.of("src/main/resources/icecore.mixins.json"))).getAsJsonObject();
        assertTrue(config.getAsJsonArray("mixins").asList().stream()
                .anyMatch(value -> value.getAsString().equals("ServerAdvancementsDisableMixin")));
    }

    private static class TestManager extends ServerAdvancementManager {
        TestManager() {
            super(null);
        }

        void load(Map<ResourceLocation, JsonElement> definitions) {
            apply(definitions, null, null);
        }
    }
}
