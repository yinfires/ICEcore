package com.yinfires.icecore.tooltip;

import net.minecraft.network.chat.Component;
import java.util.Locale;
import java.util.Map;

public final class FixedDescriptionKeys {
    private FixedDescriptionKeys() {}
    private static final Map<String, String> ALIASES = Map.ofEntries(
        Map.entry("shift", "left.shift"), Map.entry("lshift", "left.shift"), Map.entry("rshift", "right.shift"),
        Map.entry("ctrl", "left.control"), Map.entry("control", "left.control"), Map.entry("lctrl", "left.control"),
        Map.entry("rctrl", "right.control"), Map.entry("alt", "left.alt"), Map.entry("lalt", "left.alt"),
        Map.entry("ralt", "right.alt"), Map.entry("space", "space"), Map.entry("enter", "enter"),
        Map.entry("esc", "escape"), Map.entry("tab", "tab"));
    public static String canonical(String name) {
        String lower = name.trim().toLowerCase(Locale.ROOT);
        if (lower.startsWith("key.keyboard.") || lower.startsWith("key.mouse.")) return lower;
        if (lower.equals("右键") || lower.equals("right click") || lower.equals("right_click")) return "key.mouse.right";
        if (lower.equals("左键") || lower.equals("left click") || lower.equals("left_click")) return "key.mouse.left";
        if (lower.equals("中键") || lower.equals("middle click") || lower.equals("middle_click")) return "key.mouse.middle";
        String alias = ALIASES.get(lower);
        if (alias != null) return "key.keyboard." + alias;
        if (lower.matches("[a-z0-9]|f([1-9]|1[0-9]|2[0-5])")) return "key.keyboard." + lower;
        return name;
    }
    public static String display(String name, boolean nativeKeys) {
        String key = canonical(name);
        if (!key.startsWith("key.keyboard.") && !key.startsWith("key.mouse.")) return name;
        return nativeKeys ? NativeKeys.translate(key) : Component.translatable(key).getString();
    }
    // Loaded only when the optional mod is present.
    private static final class NativeKeys {
        static String translate(String key) { return com.mafuyu404.smartkeyprompts.util.KeyUtils.translateKey(key); }
    }
}