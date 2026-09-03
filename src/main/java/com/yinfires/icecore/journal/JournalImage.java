package com.yinfires.icecore.journal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/**
 * An image that a quest or tutorial can embed in its detail panel. {@code width}/{@code height} are
 * the on-screen draw size in GUI pixels; the source PNG is drawn scaled to that box (its full
 * texture treated as one sprite). Authors put the PNG in a resource pack under the referenced path.
 *
 * <p>JSON forms accepted per entry in an {@code "images"} array:
 * <ul>
 *   <li>a string {@code "icecore:textures/gui/journal/pic.png"} (drawn at a default 100x56 box), or</li>
 *   <li>an object {@code {"texture":"...","width":120,"height":68}}.</li>
 * </ul>
 */
public record JournalImage(ResourceLocation texture, int width, int height) {
    private static final int DEFAULT_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 56;

    public static JournalImage parse(JsonElement element) {
        if (element.isJsonPrimitive()) {
            return new JournalImage(requireTexture(element.getAsString()), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        }
        JsonObject json = element.getAsJsonObject();
        ResourceLocation texture = requireTexture(GsonHelper.getAsString(json, "texture"));
        int width = GsonHelper.getAsInt(json, "width", DEFAULT_WIDTH);
        int height = GsonHelper.getAsInt(json, "height", DEFAULT_HEIGHT);
        if (width < 1 || height < 1) {
            throw new JsonParseException("image width/height must be >= 1");
        }
        return new JournalImage(texture, width, height);
    }

    private static ResourceLocation requireTexture(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            throw new JsonParseException("image texture is not a resource location: " + value);
        }
        return id;
    }
}
