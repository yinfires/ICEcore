package com.yinfires.icecore.tutorial;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.yinfires.icecore.journal.JournalText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Loads {@code data/<ns>/icecore/tutorials/*.json} with hot-reload revision tracking. */
public final class TutorialDefinitionManager extends SimpleJsonResourceReloadListener {
    public static final TutorialDefinitionManager INSTANCE = new TutorialDefinitionManager();
    private static final Logger LOGGER = LogUtils.getLogger();

    private volatile Map<ResourceLocation, TutorialDefinition> definitions = Map.of();
    private volatile long revision;

    private TutorialDefinitionManager() {
        super(new Gson(), "icecore/tutorials");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> input, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, TutorialDefinition> parsed = new HashMap<>();
        input.forEach((id, json) -> {
            try {
                parsed.put(id, parse(id, GsonHelper.convertToJsonObject(json, id.toString())));
            } catch (RuntimeException ex) {
                LOGGER.error("Ignoring invalid tutorial definition {}", id, ex);
            }
        });
        definitions = Collections.unmodifiableMap(parsed);
        revision++;
        LOGGER.info("Loaded {} ICEcore tutorial definitions", parsed.size());
    }

    private static TutorialDefinition parse(ResourceLocation id, JsonObject json) {
        JournalText title = new JournalText(GsonHelper.getAsString(json, "title"));
        JournalText content = new JournalText(GsonHelper.getAsString(json, "content", ""));
        if (title.isBlank()) {
            throw new JsonParseException("tutorial title must not be blank");
        }
        ResourceLocation category = optionalId(json, "category");
        ResourceLocation icon = optionalId(json, "icon");
        int order = GsonHelper.getAsInt(json, "order", 0);
        List<com.yinfires.icecore.journal.JournalImage> images = new ArrayList<>();
        if (json.has("images")) {
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "images")) {
                images.add(com.yinfires.icecore.journal.JournalImage.parse(element));
            }
        }
        return new TutorialDefinition(id, title, content, category, icon, order, images);
    }

    @Nullable
    private static ResourceLocation optionalId(JsonObject json, String field) {
        if (!json.has(field)) {
            return null;
        }
        String value = GsonHelper.getAsString(json, field, "");
        if (value.isBlank()) {
            return null;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        if (parsed == null) {
            throw new JsonParseException(field + " is not a resource location: " + value);
        }
        return parsed;
    }

    @Nullable
    public TutorialDefinition get(ResourceLocation id) {
        return definitions.get(id);
    }

    public Set<ResourceLocation> ids() {
        return definitions.keySet();
    }

    public Map<ResourceLocation, TutorialDefinition> all() {
        return definitions;
    }

    public long revision() {
        return revision;
    }
}
