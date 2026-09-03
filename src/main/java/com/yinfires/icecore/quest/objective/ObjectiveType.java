package com.yinfires.icecore.quest.objective;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

/** Factory that parses one objective kind from JSON. Registered in {@link ObjectiveRegistry}. */
public interface ObjectiveType {
    ResourceLocation id();

    /** Parse an objective instance from its JSON object. Throw to reject an invalid definition. */
    QuestObjective parse(JsonObject json);
}
