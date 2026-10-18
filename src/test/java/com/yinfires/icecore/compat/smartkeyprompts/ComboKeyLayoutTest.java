package com.yinfires.icecore.compat.smartkeyprompts;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ComboKeyLayoutTest {
    @Test void separateCapsAndPlusHaveNonOverlappingBounds() {
        var layout = ComboKeyLayout.measure("Shift+Right Button", s -> s.length() * 6);
        assertArrayEquals(new String[]{"Shift", "Right Button"}, layout.keys());
        assertEquals(42, layout.plusOffsets()[0] - 2);
        assertEquals(52, layout.offsets()[1]);
        assertEquals(136, layout.width());
        int nativeWidth = "Shift+Right Button".length() * 6;
        int delta = layout.width() - 12 - nativeWidth;
        assertEquals(layout.width(), nativeWidth + delta + 12); // left description start
        assertEquals(12, layout.width() - (nativeWidth + delta)); // right keycap end
        assertEquals(0, (nativeWidth + delta + 12) - layout.width()); // centered row extent
    }
    @Test void handlesLongBindingsMultipleKeysAndFontChanges() {
        var small = ComboKeyLayout.measure("Left Control+Mouse Button 5+G", String::length);
        var large = ComboKeyLayout.measure("Left Control+Mouse Button 5+G", s -> s.length() * 9);
        assertEquals(3, small.keys().length);
        assertTrue(large.width() > small.width());
        for (int i = 0; i < large.plusOffsets().length; i++)
            assertTrue(large.offsets()[i + 1] > large.plusOffsets()[i] + 9);
    }
    @Test void preservesSingleKeysAndMalformedOrLiteralPlusBindings() {
        assertNull(ComboKeyLayout.measure("Right Button", String::length));
        assertNull(ComboKeyLayout.measure("+", String::length));
        assertNull(ComboKeyLayout.measure("Shift+", String::length));
        assertNull(ComboKeyLayout.measure("Shift++G", String::length));
    }
}
