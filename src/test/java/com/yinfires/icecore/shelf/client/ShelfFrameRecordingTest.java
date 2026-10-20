package com.yinfires.icecore.shelf.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class ShelfFrameRecordingTest {
    @Test void excludesBothSidesOfFocusAndMenuTransitions() {
        var r=new ShelfFrameRecording("OPTIMIZED");
        r.add(100,0);r.add(110,0);r.add(120,2);r.add(130,0);r.add(140,0);
        String[] lines=r.csv().split("\n");
        assertTrue(lines[1].endsWith("false"));assertTrue(lines[2].endsWith("true"));
        assertTrue(lines[3].endsWith("false"));assertTrue(lines[4].endsWith("false"));assertTrue(lines[5].endsWith("true"));
        assertTrue(lines[5].contains(",40,10,0,"));
    }
    @Test void capacityIsBoundedWithoutOverwritingEarlierSamples() {
        var r=new ShelfFrameRecording("HIDDEN");
        for(int i=0;i<ShelfFrameRecording.CAPACITY;i++) assertTrue(r.add(i,0));
        assertFalse(r.add(999999,0));
    }
}
