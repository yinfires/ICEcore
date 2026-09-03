package com.yinfires.icecore.quest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.yinfires.icecore.journal.JournalText;
import com.yinfires.icecore.quest.objective.ObjectiveRegistry;
import com.yinfires.icecore.quest.objective.QuestObjective;
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

/** Loads {@code data/<ns>/icecore/quests/*.json} with hot-reload revision tracking. */
public final class QuestDefinitionManager extends SimpleJsonResourceReloadListener {
    public static final QuestDefinitionManager INSTANCE = new QuestDefinitionManager();
    private static final Logger LOGGER = LogUtils.getLogger();

    private volatile Map<ResourceLocation, QuestDefinition> definitions = Map.of();
    private volatile long revision;

    private QuestDefinitionManager() {
        super(new Gson(), "icecore/quests");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> input, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, QuestDefinition> parsed = new HashMap<>();
        input.forEach((id, json) -> {
            try {
                parsed.put(id, parse(id, GsonHelper.convertToJsonObject(json, id.toString())));
            } catch (RuntimeException ex) {
                LOGGER.error("Ignoring invalid quest definition {}", id, ex);
            }
        });
        definitions = Collections.unmodifiableMap(parsed);
        revision++;
        LOGGER.info("Loaded {} ICEcore quest definitions", parsed.size());
    }

    private static QuestDefinition parse(ResourceLocation id, JsonObject json) {
        JournalText title = new JournalText(GsonHelper.getAsString(json, "title"));
        JournalText content = new JournalText(GsonHelper.getAsString(json, "content", ""));
        if (title.isBlank()) {
            throw new JsonParseException("quest title must not be blank");
        }
        ResourceLocation category = optionalId(json, "category");
        ResourceLocation icon = optionalId(json, "icon");
        int order = GsonHelper.getAsInt(json, "order", 0);
        QuestScope scope = QuestScope.fromString(GsonHelper.getAsString(json, "scope", "player"));
        boolean repeatable = GsonHelper.getAsBoolean(json, "repeatable", false);
        boolean autoUnlock = GsonHelper.getAsBoolean(json, "auto_unlock_on_prereq", false);
        boolean autoComplete = GsonHelper.getAsBoolean(json, "auto_complete", true);
        List<ResourceLocation> requires = parseIdList(json, "requires");
        List<QuestObjective> objectives = parseObjectives(json);
        List<String> onAccept = parseStringList(json, "on_accept");
        List<String> onComplete = parseStringList(json, "on_complete");
        List<com.yinfires.icecore.journal.JournalImage> images = new ArrayList<>();
        if (json.has("images")) {
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "images")) {
                images.add(com.yinfires.icecore.journal.JournalImage.parse(element));
            }
        }
        return new QuestDefinition(id, title, content, category, icon, order, scope, repeatable,
                autoUnlock, autoComplete, requires, objectives, onAccept, onComplete, images);
    }

    private static List<QuestObjective> parseObjectives(JsonObject json) {
        List<QuestObjective> objectives = new ArrayList<>();
        if (!json.has("objectives")) {
            return objectives;
        }
        JsonArray array = GsonHelper.getAsJsonArray(json, "objectives");
        for (JsonElement element : array) {
            objectives.add(ObjectiveRegistry.parse(GsonHelper.convertToJsonObject(element, "objective")));
        }
        return objectives;
    }

    private static List<ResourceLocation> parseIdList(JsonObject json, String field) {
        List<ResourceLocation> ids = new ArrayList<>();
        if (!json.has(field)) {
            return ids;
        }
        for (JsonElement element : GsonHelper.getAsJsonArray(json, field)) {
            String value = element.getAsString();
            ResourceLocation parsed = ResourceLocation.tryParse(value);
            if (parsed == null) {
                throw new JsonParseException(field + " contains an invalid resource location: " + value);
            }
            ids.add(parsed);
        }
        return ids;
    }

    private static List<String> parseStringList(JsonObject json, String field) {
        List<String> values = new ArrayList<>();
        if (!json.has(field)) {
            return values;
        }
        for (JsonElement element : GsonHelper.getAsJsonArray(json, field)) {
            values.add(element.getAsString());
        }
        return values;
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
    public QuestDefinition get(ResourceLocation id) {
        return definitions.get(id);
    }

    public Set<ResourceLocation> ids() {
        return definitions.keySet();
    }

    public Map<ResourceLocation, QuestDefinition> all() {
        return definitions;
    }

    public long revision() {
        return revision;
    }
}
