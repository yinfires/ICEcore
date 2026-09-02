package com.yinfires.icecore.npc;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.yinfires.icecore.ICECore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class NpcDefinitionManager extends SimpleJsonResourceReloadListener {
    public static final NpcDefinitionManager INSTANCE = new NpcDefinitionManager();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation MISSING_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "missingno");
    private volatile Map<ResourceLocation, NpcDefinition> definitions = Map.of();
    private volatile boolean loaded;
    private volatile long revision;
    private final Set<ResourceLocation> warnedMissing = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private NpcDefinitionManager() { super(new Gson(), "icecore/npcs"); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> input, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, NpcDefinition> parsed = new HashMap<>();
        input.forEach((id, json) -> {
            try { parsed.put(id, parse(id, GsonHelper.convertToJsonObject(json, id.toString()))); }
            catch (RuntimeException ex) { LOGGER.error("Ignoring invalid NPC definition {}", id, ex); }
        });
        definitions = Collections.unmodifiableMap(parsed);
        loaded = true;
        revision++;
        warnedMissing.clear();
        LOGGER.info("Loaded {} ICEcore NPC definitions", parsed.size());
    }

    static NpcDefinition parse(ResourceLocation id, JsonObject json) {
        String name = GsonHelper.getAsString(json, "name");
        ResourceLocation texture = requireId(GsonHelper.getAsString(json, "texture"), "texture");
        String model = GsonHelper.getAsString(json, "model");
        if (!model.equals("wide") && !model.equals("slim")) throw new JsonParseException("model must be wide or slim");
        JsonObject dialogue = GsonHelper.getAsJsonObject(json, "dialogue");
        ResourceLocation resource = requireId(GsonHelper.getAsString(dialogue, "resource"), "dialogue.resource");
        String group = GsonHelper.getAsString(dialogue, "group");
        if (name.isBlank() || group.isBlank()) throw new JsonParseException("name and dialogue.group must not be blank");
        return new NpcDefinition(id, name, texture, model.equals("slim"), resource, group);
    }

    private static ResourceLocation requireId(String value, String field) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) throw new JsonParseException(field + " is not a resource location");
        return id;
    }

    public NpcDefinition get(ResourceLocation id) { return definitions.get(id); }
    public Set<ResourceLocation> ids() { return definitions.keySet(); }
    public boolean isLoaded() { return loaded; }
    public long revision() { return revision; }
    public void warnMissing(ResourceLocation id) {
        if (warnedMissing.size() < 128 && warnedMissing.add(id)) LOGGER.warn("NPC definition {} is missing; preserving entity with fallback rendering", id);
    }
    public static ResourceLocation missingTexture() { return MISSING_TEXTURE; }
}
