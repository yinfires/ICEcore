package com.yinfires.icecore.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BuildingDependencyTest {
    private final BlockPos origin = new BlockPos(0, 64, 0);

    @Test void allSixFacesUseTheSupportOutwardDirection() {
        for (Direction face : Direction.values()) {
            BlockListDefinition rule = new BlockListDefinition("dependent");
            rule.allowedFaces().add(face.getName());
            assertTrue(BuildingRules.hasDependents(List.of(origin), (part, direction) ->
                    part.relative(direction).equals(origin.relative(face)) && rule.allowsFace(direction)));
            rule.allowedFaces().clear();
            rule.allowedFaces().add(face.getOpposite().getName());
            assertFalse(BuildingRules.hasDependents(List.of(origin), (part, direction) ->
                    part.relative(direction).equals(origin.relative(face)) && rule.allowsFace(direction)));
        }
    }

    @Test void secondaryCellSupportsExternalBlocksButNotItsOwnStructure() {
        BlockPos upper = origin.above();
        List<BlockPos> structure = List.of(origin, upper);
        assertFalse(BuildingRules.hasDependents(structure, (part, face) -> structure.contains(part.relative(face))));
        assertTrue(BuildingRules.hasDependents(structure, (part, face) -> part.relative(face).equals(upper.east())));
    }

    @Test void removedDependencyAllowsDismantlingAndScansAreBounded() {
        AtomicInteger checks = new AtomicInteger();
        assertTrue(BuildingRules.hasDependents(List.of(origin), (part, face) -> {
            checks.incrementAndGet();
            return true;
        }));
        assertEquals(1, checks.get());
        checks.set(0);
        assertFalse(BuildingRules.hasDependents(List.of(origin, origin.above()), (part, face) -> {
            checks.incrementAndGet();
            return false;
        }));
        assertEquals(10, checks.get()); // Six faces per part, excluding the two internal faces.
    }

    @Test void productionMatcherUsesAnyCurrentSupportListWithoutRegionOrAlternativeFallback() throws Exception {
        String source = read("BuildingRules");
        String query = source.substring(source.indexOf("public static boolean hasDependents"),
                source.indexOf("static boolean hasDependents(List"));
        for (String required : new String[]{"BuildingStructure.primaryPosition(level, position)",
                "BuildingStructure.positions(level.getBlockState(primary), primary)", "BuildingRuntimeCache.data()",
                "!list.supports().isEmpty()", "list.hasBaseRestriction(data)", "list.allowsFace(face)",
                "list.matches(dependent, registry)", "list.matchesSupport(data, support, registry)", "return true;"}) {
            assertTrue(query.contains(required), required);
        }
        assertFalse(query.contains("matchesRegion"));
        assertFalse(query.contains("canPlace"));
        assertFalse(query.contains("canBreak"));
        assertFalse(query.contains("BuildingRuntimeCache.listsFor"));
        String list = read("BlockListDefinition");
        assertTrue(list.contains("entry.startsWith(\"#\")"));
        assertTrue(list.contains("holder.is(tag)"));
        assertTrue(list.contains("id.equals(registered)"));
        assertTrue(list.contains("anyMatch(list -> list.matches(clicked, registry))"));
    }

    private static String read(String name) throws Exception {
        return Files.readString(Path.of("src/main/java/com/yinfires/icecore/building/" + name + ".java"),
                StandardCharsets.UTF_8);
    }
}
