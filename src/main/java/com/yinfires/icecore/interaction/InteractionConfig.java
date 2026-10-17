package com.yinfires.icecore.interaction;

import com.google.gson.*;
import java.util.*;
import java.util.function.Predicate;

/** Pure configuration model; tags are resolved by the runtime, never inferred from names. */
public record InteractionConfig(List<Rule> blacklist, List<Rule> whitelist) {
    public InteractionConfig {
        blacklist = List.copyOf(new LinkedHashSet<>(blacklist));
        whitelist = List.copyOf(new LinkedHashSet<>(whitelist));
    }
    public static InteractionConfig empty() { return new InteractionConfig(List.of(), List.of()); }

    public record Rule(String block, String mod, String tag) {
        public Rule {
            if (block != null) id(block);
            if (tag != null) id(tag);
            if (mod != null && !mod.matches("[a-z0-9_.-]+")) throw new IllegalArgumentException("Invalid namespace: " + mod);
            if ((block != null && (mod != null || tag != null)) || (block == null && mod == null && tag == null))
                throw new IllegalArgumentException("Expected block, tag, mod or mod+tag rule");
        }
        public boolean matches(String blockId, Predicate<String> hasTag) {
            return (block == null || block.equals(blockId))
                    && (mod == null || blockId.startsWith(mod + ":"))
                    && (tag == null || hasTag.test(tag));
        }
        public JsonElement json() {
            if (block != null) return new JsonPrimitive(block);
            if (mod == null) return new JsonPrimitive("#" + tag);
            JsonObject object = new JsonObject(); object.addProperty("mod", mod);
            if (tag != null) object.addProperty("tag", tag);
            return object;
        }
        @Override public String toString() { return json().toString(); }
    }

    public static String id(String value) {
        if (!value.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")) throw new IllegalArgumentException("Invalid resource ID: " + value);
        return value;
    }
    static String string(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("Expected string");
        return value.getAsString();
    }
    static void fields(JsonObject value, String... allowed) {
        Set<String> names = Set.of(allowed);
        if (!names.containsAll(value.keySet())) throw new IllegalArgumentException("Unknown JSON field");
    }
    static void version(JsonObject value) {
        if (!value.has("formatVersion") || !value.get("formatVersion").isJsonPrimitive()
                || !value.get("formatVersion").getAsJsonPrimitive().isNumber()
                || !value.get("formatVersion").getAsString().equals("1"))
            throw new IllegalArgumentException("formatVersion must be 1");
    }
    public static InteractionConfig parse(JsonElement element) {
        if (!element.isJsonObject()) throw new IllegalArgumentException("Expected JSON object");
        JsonObject object = element.getAsJsonObject(); fields(object, "formatVersion", "blacklist", "whitelist"); version(object);
        return new InteractionConfig(rules(object.get("blacklist")), rules(object.get("whitelist")));
    }
    private static List<Rule> rules(JsonElement element) {
        if (element == null || !element.isJsonArray()) throw new IllegalArgumentException("Expected rule array");
        List<Rule> result = new ArrayList<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            if (entry.isJsonPrimitive()) {
                String text = string(entry);
                result.add(text.startsWith("#") ? new Rule(null, null, text.substring(1)) : new Rule(text, null, null));
            } else if (entry.isJsonObject()) {
                JsonObject object = entry.getAsJsonObject(); fields(object, "mod", "tag");
                result.add(new Rule(null, string(object.get("mod")), object.has("tag") ? string(object.get("tag")) : null));
            } else throw new IllegalArgumentException("Invalid rule");
        }
        return result;
    }
    public JsonObject json() {
        JsonObject object = new JsonObject(); object.addProperty("formatVersion", 1);
        JsonArray deny = new JsonArray(), allow = new JsonArray();
        blacklist.forEach(rule -> deny.add(rule.json())); whitelist.forEach(rule -> allow.add(rule.json()));
        object.add("blacklist", deny); object.add("whitelist", allow); return object;
    }
    public InteractionConfig edit(boolean allow, boolean add, Rule rule) {
        List<Rule> edited = new ArrayList<>(allow ? whitelist : blacklist);
        boolean changed = add ? !edited.contains(rule) && edited.add(rule) : edited.remove(rule);
        if (!changed) return this;
        return allow ? new InteractionConfig(blacklist, edited) : new InteractionConfig(edited, whitelist);
    }
    /** Removal candidates come from stored rules, including currently unresolved IDs/tags. */
    public java.util.stream.Stream<String> removalSuggestions(boolean allow, String kind, String selectedMod) {
        return (allow ? whitelist : blacklist).stream().filter(rule -> switch (kind) {
            case "block" -> rule.block() != null;
            case "tag" -> rule.mod() == null && rule.tag() != null;
            case "mod" -> rule.mod() != null && rule.tag() == null;
            case "modtag" -> rule.mod() != null && rule.tag() != null
                    && (selectedMod == null || selectedMod.equals(rule.mod()));
            default -> false;
        }).map(rule -> switch (kind) {
            case "block" -> rule.block();
            case "tag" -> rule.tag();
            case "modtag" -> selectedMod == null ? rule.mod() : rule.tag();
            default -> rule.mod();
        }).distinct();
    }
}
