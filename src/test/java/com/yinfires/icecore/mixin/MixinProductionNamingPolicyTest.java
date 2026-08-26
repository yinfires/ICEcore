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
                    assertTrue(selector.matches("m_\\d+_\\([^\"]*\\)[^\"]+"), file + " must use an SRG method name and full descriptor: " + selector);
                    assertFalse(selector.matches("[a-zA-Z]\\(.*"), file + " must not use a Mojang one-letter obfuscated name: " + selector);
                }
            }
        }
    }
}
