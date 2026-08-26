package com.yinfires.icecore.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TimeMixinProductionSelectorTest {
    private static String source(String name) throws IOException {
        return Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin", name), StandardCharsets.UTF_8);
    }

    @Test
    void serverLevelSelectorsUseProductionNamesAndDescriptors() throws IOException {
        String source = source("ServerLevelTimeMixin.java");
        assertTrue(source.contains("m_8809_()V"));
        assertTrue(source.contains("Lnet/minecraft/server/level/ServerLevel;m_8615_(J)V"));
        assertTrue(source.contains("m_8615_(J)V"));
        assertFalse(source.contains("method=\"tickTime\"") || source.contains("method = \"tickTime\""));
        assertFalse(source.contains(";setDayTime(J)V"));
    }

    @Test
    void clientLevelSelectorsUseProductionNamesAndDescriptors() throws IOException {
        String source = source("ClientLevelTimeMixin.java");
        assertTrue(source.contains("m_104826_()V"));
        assertTrue(source.contains("Lnet/minecraft/client/multiplayer/ClientLevel;m_104746_(J)V"));
        assertFalse(source.contains("method=\"tickTime\"") || source.contains("method = \"tickTime\""));
        assertFalse(source.contains(";setDayTime(J)V"));
    }

    @Test
    void timeCommandSelectorsUseProductionNamesAndDescriptors() throws IOException {
        String source = source("TimeCommandMixin.java");
        assertTrue(source.contains("m_139077_(Lnet/minecraft/commands/CommandSourceStack;I)I"));
        assertTrue(source.contains("m_139082_(Lnet/minecraft/commands/CommandSourceStack;I)I"));
        assertFalse(source.contains("\"setTime\"") || source.contains("\"addTime\""));
    }
}
