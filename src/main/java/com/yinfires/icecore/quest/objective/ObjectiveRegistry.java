package com.yinfires.icecore.quest.objective;

import com.google.gson.JsonObject;
import com.yinfires.icecore.ICECore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of objective kinds. New quest types are added by registering one {@link ObjectiveType}
 * here; the core parsing/completion pipeline does not change. Built-in types register on class load.
 */
public final class ObjectiveRegistry {
    private static final Map<ResourceLocation, ObjectiveType> TYPES = new ConcurrentHashMap<>();

    private ObjectiveRegistry() {
    }

    public static void register(ObjectiveType type) {
        TYPES.put(type.id(), type);
    }

    public static boolean isRegistered(ResourceLocation id) {
        return TYPES.containsKey(id);
    }

    /** Parse one objective from its JSON object; the {@code type} field selects the kind. */
    public static QuestObjective parse(JsonObject json) {
        String rawType = GsonHelper.getAsString(json, "type");
        ResourceLocation id = ResourceLocation.tryParse(rawType);
        if (id == null) {
            throw new com.google.gson.JsonParseException("objective type is not a resource location: " + rawType);
        }
        ObjectiveType type = TYPES.get(id);
        if (type == null) {
            throw new com.google.gson.JsonParseException("unknown objective type: " + id);
        }
        return type.parse(json);
    }

    /** Registers the built-in objective kinds. Called once during mod construction. */
    public static void bootstrap() {
        register(new CountObjectiveType(ResourceLocation.fromNamespaceAndPath(ICECore.MOD_ID, "talk_to_npc"), "npc"));
        register(new CountObjectiveType(ResourceLocation.fromNamespaceAndPath(ICECore.MOD_ID, "command"), null));
        // Advances when the player closes the named journal screen ("quest" or "tutorial").
        register(new CountObjectiveType(ResourceLocation.fromNamespaceAndPath(ICECore.MOD_ID, "open_journal"), "journal"));
    }
}
