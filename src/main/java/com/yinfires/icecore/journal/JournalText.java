package com.yinfires.icecore.journal;

import net.minecraft.network.chat.Component;

/**
 * A displayable string from a datapack. Literal by default so authors just write the text (e.g.
 * {@code "打开教程"}). Prefixes override that:
 * <ul>
 *   <li>{@code translate:key} — resolve {@code key} as a translation key,</li>
 *   <li>{@code literal:text} — force literal (prefix stripped); kept for backward compatibility.</li>
 * </ul>
 * Any unprefixed value is shown verbatim. Kept as a lightweight value holder so definitions stay
 * immutable.
 */
public record JournalText(String raw) {
    private static final String LITERAL_PREFIX = "literal:";
    private static final String TRANSLATE_PREFIX = "translate:";

    public boolean isBlank() {
        return raw == null || raw.isBlank();
    }

    public Component resolve() {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        if (raw.startsWith(TRANSLATE_PREFIX)) {
            return Component.translatable(raw.substring(TRANSLATE_PREFIX.length()));
        }
        if (raw.startsWith(LITERAL_PREFIX)) {
            return Component.literal(raw.substring(LITERAL_PREFIX.length()));
        }
        return Component.literal(raw);
    }

    /** Resolve with substitution arguments; only meaningful for {@code translate:} keys. */
    public Component resolve(Object... args) {
        if (raw != null && raw.startsWith(TRANSLATE_PREFIX)) {
            return Component.translatable(raw.substring(TRANSLATE_PREFIX.length()), args);
        }
        if (raw != null && raw.startsWith(LITERAL_PREFIX)) {
            return Component.literal(raw.substring(LITERAL_PREFIX.length()));
        }
        return Component.literal(raw == null ? "" : raw);
    }
}
