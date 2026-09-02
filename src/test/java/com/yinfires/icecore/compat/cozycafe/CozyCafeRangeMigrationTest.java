package com.yinfires.icecore.compat.cozycafe;

import com.google.gson.JsonParser;
import com.yinfires.icecore.compat.cozycafe.range.CozyCafeRangeData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class CozyCafeRangeMigrationTest {
    @Test void versionOneLoadsWithoutSpawnRegionAndWritesVersionTwo() {
        var json = JsonParser.parseString("""
                {"formatVersion":1,"ranges":{"minecraft:overworld|1,2,3":{
                "dimension":"minecraft:overworld","computer":[1,2,3],"pos1":[0,0,0],"pos2":[2,2,2]}}}
                """).getAsJsonObject();
        CozyCafeRangeData data = CozyCafeRangeData.fromJson(json);
        assertNull(data.ranges().values().iterator().next().spawnRegion());
        assertEquals(2, data.toJson().get("formatVersion").getAsInt());
    }
}
