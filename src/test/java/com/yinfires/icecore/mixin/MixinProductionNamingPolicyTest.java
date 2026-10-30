package com.yinfires.icecore.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MixinProductionNamingPolicyTest {
    private static final Pattern MINECRAFT_MIXIN = Pattern.compile("@Mixin\\(\\s*(?:net\\.minecraft\\.|[A-Z][A-Za-z0-9_]*\\.class)");
    private static final Pattern SELECTOR = Pattern.compile("method\\s*=\\s*\"([^\"]+)\"");

    @Test
    void minecraftMixinSelectorsUseSrgNamesAndDescriptors() throws IOException {
        try (Stream<Path> files = Files.list(Path.of("src/main/java/com/yinfires/icecore/mixin"))) {
            for (Path file : files.filter(path -> path.toString().endsWith("Mixin.java")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                // External-mod mixins intentionally use development names and remap=false.
                // Only selectors targeting Minecraft classes are subject to the SRG policy.
                if (source.contains("remap = false") && source.matches("(?s).*@Mixin\\s*\\(\\s*targets\\s*=.*")) continue;
                if (!MINECRAFT_MIXIN.matcher(source).find()) continue;
                Matcher matcher = SELECTOR.matcher(source);
                while (matcher.find()) {
                    String selector = matcher.group(1);
                    if (selector.equals("<init>") || selector.equals("<clinit>")) continue;
                    // Forge adds this overload after Minecraft's SRG map is generated.
                    // Its production name is literally tesselateBlock; pretending it has
                    // an SRG name would make the client injection fail at startup.
                    if (file.getFileName().toString().equals("StoveSurfaceAoMixin.java")
                            && selector.equals("tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V")) {
                        String descriptor = selector.substring(selector.indexOf('('));
                        String mappings = Files.readString(Path.of("build/createMcpToSrg/output.tsrg"), StandardCharsets.UTF_8);
                        assertFalse(mappings.contains("tesselateBlock " + descriptor + " "), "Forge-only overload must not invent an SRG mapping");
                        try (var input = getClass().getClassLoader().getResourceAsStream("net/minecraft/client/renderer/block/ModelBlockRenderer.class")) {
                            var node = new org.objectweb.asm.tree.ClassNode();
                            new org.objectweb.asm.ClassReader(input).accept(node, 0);
                            assertTrue(node.methods.stream().anyMatch(m -> m.name.equals("tesselateBlock") && m.desc.equals(descriptor)),
                                    "Exact Forge overload must exist in the baseline renderer");
                        }
                        continue;
                    }
                    assertTrue(selector.matches("m_\\d+_\\([^\"]*\\)[^\"]+"), file + " must use an SRG method name and full descriptor: " + selector);
                    assertFalse(selector.matches("[a-zA-Z]\\(.*"), file + " must not use a Mojang one-letter obfuscated name: " + selector);
                }
            }
        }
    }
}
