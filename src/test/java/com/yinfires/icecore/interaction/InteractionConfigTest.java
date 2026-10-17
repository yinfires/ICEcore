package com.yinfires.icecore.interaction;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class InteractionConfigTest {
    @Test void removalSuggestionsOnlyExposeSelectedListAndExactRuleType() {
        var config = parse("\"blacklist\":[\"missing:block\",\"#missing:tag\",{\"mod\":\"furniture\"},{\"mod\":\"mcwdoors\",\"tag\":\"minecraft:doors\"},{\"mod\":\"mcwdoors\",\"tag\":\"missing:doors\"},{\"mod\":\"other\",\"tag\":\"other:doors\"}],\"whitelist\":[\"minecraft:chest\"]");
        assertEquals(List.of("missing:block"), config.removalSuggestions(false, "block", null).toList());
        assertEquals(List.of("minecraft:chest"), config.removalSuggestions(true, "block", null).toList());
        assertEquals(List.of("missing:tag"), config.removalSuggestions(false, "tag", null).toList());
        assertEquals(List.of("furniture"), config.removalSuggestions(false, "mod", null).toList());
        assertEquals(List.of("mcwdoors", "other"), config.removalSuggestions(false, "modtag", null).toList());
        assertEquals(List.of("minecraft:doors", "missing:doors"), config.removalSuggestions(false, "modtag", "mcwdoors").toList());
        assertTrue(config.removalSuggestions(true, "modtag", null).toList().isEmpty());
        assertTrue(config.removalSuggestions(false, "modtag", "absent").toList().isEmpty());
        var updated = config.edit(false, false, new InteractionConfig.Rule("missing:block", null, null));
        assertTrue(updated.removalSuggestions(false, "block", null).toList().isEmpty());
    }
    private InteractionConfig parse(String lists) {
        return InteractionConfig.parse(JsonParser.parseString("{\"formatVersion\":1," + lists + "}"));
    }
    @Test void selectorsAndIntersectionUseActualMembership() {
        var config = parse("\"blacklist\":[\"minecraft:chest\",\"#test:chairs\",{\"mod\":\"furniture\"},{\"mod\":\"mcwdoors\",\"tag\":\"minecraft:doors\"}],\"whitelist\":[]");
        assertTrue(config.blacklist().get(0).matches("minecraft:chest", ignored -> false));
        assertFalse(config.blacklist().get(0).matches("minecraft:barrel", ignored -> true));
        assertTrue(config.blacklist().get(1).matches("other:block", Set.of("test:chairs")::contains));
        assertFalse(config.blacklist().get(1).matches("test:chair", ignored -> false));
        assertTrue(config.blacklist().get(2).matches("furniture:table", ignored -> false));
        assertFalse(config.blacklist().get(2).matches("other:furniture", ignored -> true));
        assertTrue(config.blacklist().get(3).matches("mcwdoors:glass_door", Set.of("minecraft:doors")::contains));
        assertFalse(config.blacklist().get(3).matches("minecraft:oak_door", ignored -> true));
        assertFalse(config.blacklist().get(3).matches("mcwdoors:glass_door", ignored -> false));
        assertEquals(config, InteractionConfig.parse(config.json()));
    }
    @Test void editsDeduplicateAndKeepPreviousSnapshotImmutable() {
        var config = parse("\"blacklist\":[\"minecraft:chest\",\"minecraft:chest\"],\"whitelist\":[]");
        assertEquals(1, config.blacklist().size());
        var rule = new InteractionConfig.Rule("minecraft:chest", null, null);
        assertSame(config, config.edit(false, true, rule));
        var allow = config.edit(true, true, rule);
        assertTrue(config.whitelist().isEmpty()); assertEquals(List.of(rule), allow.whitelist());
        assertTrue(allow.edit(false, false, rule).blacklist().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> config.blacklist().clear());
    }
    @Test void rejectMalformedFieldsAndKeepUnknownSelectors() {
        assertThrows(IllegalArgumentException.class, () -> parse("\"blacklist\":[true],\"whitelist\":[]"));
        assertThrows(IllegalArgumentException.class, () -> parse("\"blacklist\":[\"Chest\"],\"whitelist\":[]"));
        assertThrows(IllegalArgumentException.class, () -> parse("\"blacklist\":[{\"mod\":\"test\",\"typo\":true}],\"whitelist\":[]"));
        assertThrows(IllegalArgumentException.class, () -> parse("\"blacklist\":[],\"whitelist\":[],\"typo\":true"));
        assertThrows(IllegalArgumentException.class, () -> InteractionConfig.parse(JsonParser.parseString("{\"formatVersion\":\"1\",\"blacklist\":[],\"whitelist\":[]}")));
        var config = parse("\"blacklist\":[\"missing:block\",\"#missing:tag\",{\"mod\":\"missing\"}],\"whitelist\":[]");
        assertEquals(3, config.blacklist().size()); assertEquals(config, InteractionConfig.parse(config.json()));
        assertEquals(InteractionConfig.empty(), parse("\"blacklist\":[],\"whitelist\":[]"));
    }
    @Test void exceptionsSeparateDimensionsAndBindBlockId() {
        var position = new InteractionExceptions.Location("minecraft:overworld", 1, 64, -3);
        var nether = new InteractionExceptions.Location("minecraft:the_nether", 1, 64, -3);
        var empty = InteractionExceptions.empty(); var saved = empty.edit(position, "minecraft:oak_door");
        assertTrue(empty.entries().isEmpty()); assertNull(saved.entries().get(nether));
        assertEquals("minecraft:oak_door", saved.entries().get(position));
        assertNotEquals("minecraft:chest", saved.entries().get(position));
        assertEquals(saved, InteractionExceptions.parse(saved.json()));
        assertEquals(empty, saved.edit(position, null));
        assertThrows(ArithmeticException.class, () -> InteractionExceptions.parse(JsonParser.parseString("{\"formatVersion\":1,\"exceptions\":[{\"dimension\":\"minecraft:overworld\",\"pos\":[1.5,64,0],\"block\":\"minecraft:chest\"}]}")));
    }
}
