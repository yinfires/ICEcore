package com.yinfires.icecore.compat.chatbox;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChatBoxDependencyMetadataTest {
    @Test
    void acceptsCompatibleChatBoxUpdatesAndCompilesAgainstCurrentReference() throws IOException {
        String modsToml = Files.readString(
                Path.of("src/main/resources/META-INF/mods.toml"), StandardCharsets.UTF_8);
        String buildScript = Files.readString(Path.of("build.gradle"), StandardCharsets.UTF_8);

        assertTrue(modsToml.contains("modId=\"chatbox\"\nmandatory=true\nversionRange=\"[1.1.4,)\""),
                "ChatBox runtime metadata must accept compatible releases from 1.1.4 onward");
        assertTrue(buildScript.contains("local:chatbox:1.1.5+1.20.1-forge"),
                "The compile classpath must verify the currently deployed ChatBox 1.1.5 API");
    }
}
