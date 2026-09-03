package com.yinfires.icecore.journal;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Loads {@code data/<ns>/icecore/journal_categories/*.json}. Shared by quest and tutorial trees. */
public final class CategoryManager extends SimpleJsonResourceReloadListener {
    public static final CategoryManager INSTANCE = new CategoryManager();
    private static final Logger LOGGER = LogUtils.getLogger();

    private volatile Map<ResourceLocation, CategoryDefinition> categories = Map.of();

    private CategoryManager() {
        super(new Gson(), "icecore/journal_categories");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> input, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, CategoryDefinition> parsed = new HashMap<>();
        input.forEach((id, json) -> {
            try {
                parsed.put(id, parse(id, GsonHelper.convertToJsonObject(json, id.toString())));
            } catch (RuntimeException ex) {
                LOGGER.error("Ignoring invalid journal category {}", id, ex);
            }
        });
        categories = Collections.unmodifiableMap(parsed);
        LOGGER.info("Loaded {} ICEcore journal categories", parsed.size());
    }

    private static CategoryDefinition parse(ResourceLocation id, JsonObject json) {
        JournalType type = JournalType.fromString(GsonHelper.getAsString(json, "type", "quest"));
        JournalText name = new JournalText(GsonHelper.getAsString(json, "name"));
        if (name.isBlank()) {
            throw new com.google.gson.JsonParseException("category name must not be blank");
        }
        ResourceLocation parent = optionalId(json, "parent");
        ResourceLocation icon = optionalId(json, "icon");
        int order = GsonHelper.getAsInt(json, "order", 0);
        return new CategoryDefinition(id, type, name, parent, icon, order);
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
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            throw new com.google.gson.JsonParseException(field + " is not a resource location: " + value);
        }
        return id;
    }

    @Nullable
    public CategoryDefinition get(ResourceLocation id) {
        return categories.get(id);
    }

    public Map<ResourceLocation, CategoryDefinition> all() {
        return categories;
    }
}
