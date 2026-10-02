package com.yinfires.icecore.release;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChangelogVersionTest {
    private static final Pattern MOD_VERSION = Pattern.compile("(?m)^mod_version=(\\S+)$");
    private static final Pattern FIRST_CHANGELOG_VERSION = Pattern.compile("(?m)^## (\\S+) - ");

    @Test
    void currentBuildVersionMatchesTopChangelogEntry() throws IOException {
        String properties = Files.readString(Path.of("gradle.properties"), StandardCharsets.UTF_8);
        String changelog = Files.readString(Path.of("CHANGELOG.md"), StandardCharsets.UTF_8);

        Matcher modVersion = MOD_VERSION.matcher(properties);
        Matcher changelogVersion = FIRST_CHANGELOG_VERSION.matcher(changelog);
        assertTrue(modVersion.find(), "gradle.properties must define mod_version");
        assertTrue(changelogVersion.find(), "CHANGELOG.md must contain a version entry");
        assertEquals(modVersion.group(1), changelogVersion.group(1),
                "CHANGELOG.md top version must match the current build version");
    }
}
