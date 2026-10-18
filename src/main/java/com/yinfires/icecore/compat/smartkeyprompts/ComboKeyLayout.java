package com.yinfires.icecore.compat.smartkeyprompts;

import java.util.function.ToIntFunction;

/** Pixel layout of native keycaps separated by a plain plus sign. */
public record ComboKeyLayout(String[] keys, int[] offsets, int[] plusOffsets, int width) {
    public static ComboKeyLayout measure(String display, ToIntFunction<String> measure) {
        String[] keys = display.split("\\+", -1);
        if (keys.length < 2) return null;
        for (int i = 0; i < keys.length; i++) {
            keys[i] = keys[i].trim();
            if (keys[i].isEmpty()) return null;
        }
        int[] offsets = new int[keys.length];
        int[] plusOffsets = new int[keys.length - 1];
        int width = 0;
        for (int i = 0; i < keys.length; i++) {
            offsets[i] = width;
            width += measure.applyAsInt(keys[i]) + 12;
            if (i + 1 < keys.length) {
                plusOffsets[i] = width + 2;
                width += measure.applyAsInt("+") + 4;
            }
        }
        return new ComboKeyLayout(keys, offsets, plusOffsets, width);
    }
}
