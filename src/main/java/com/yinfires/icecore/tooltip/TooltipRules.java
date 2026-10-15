package com.yinfires.icecore.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.List;

public final class TooltipRules {
    private TooltipRules() {}

    public static void applyOriginal(List<Component> lines, List<Component> replacement,
                                     boolean hideOriginal, boolean hideEffects) {
        if (replacement != null || hideOriginal) replaceBody(lines, replacement);
        else if (hideEffects) removeFoodEffects(lines);
    }

    public static void replaceBody(List<Component> lines, List<Component> replacement) {
        if (lines.isEmpty()) return;
        lines.subList(1, lines.size()).clear();
        if (replacement != null) replacement.forEach(line -> lines.add(line.copy()));
    }

    /** Inspect translation structure, never rendered text, language or styling. */
    public static boolean isEffect(Component component) {
        if (component.getContents() instanceof TranslatableContents translated) {
            String key = translated.getKey();
            if (key.startsWith("effect.") || key.equals("potion.withDuration")
                    || key.equals("potion.withAmplifier") || key.equals("potion.whenDrank")) return true;
            for (Object argument : translated.getArgs()) {
                if (argument instanceof Component child && isEffect(child)) return true;
            }
        }
        for (Component sibling : component.getSiblings()) if (isEffect(sibling)) return true;
        return false;
    }

    public static void removeFoodEffects(List<Component> lines) {
        boolean attributes = false;
        for (int i = 1; i < lines.size();) {
            Component line = lines.get(i);
            if (line.getContents() instanceof TranslatableContents translated) {
                String key = translated.getKey();
                if (key.equals("potion.whenDrank")) attributes = true;
                else if (attributes && key.startsWith("attribute.modifier.")) {
                    lines.remove(i);
                    continue;
                } else if (attributes) attributes = false;
            } else if (attributes && !line.equals(Component.empty())) {
                attributes = false;
            }
            if (isEffect(line)) lines.remove(i);
            else i++;
        }
    }
}
