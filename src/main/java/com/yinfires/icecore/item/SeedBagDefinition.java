package com.yinfires.icecore.item;

import com.yinfires.icecore.ICECore;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Immutable rules shared by registration, planting, harvesting and loot integration.
 * Crop and produce ids may belong to optional mods; they are resolved after registries freeze.
 *
 * @param crop       root crop block placed on farmland
 * @param upperCrop  separate upper block (only for {@link Shape#SPLIT}), otherwise {@code null}
 * @param produce    harvest items; one is chosen uniformly at random per harvest
 * @param matureAge  harvestable age of the root block, or of the upper block for {@link Shape#SPLIT}
 * @param farmlandOverride whether the upstream soil rule must be widened to accept any farmland
 */
public record SeedBagDefinition(
        String id,
        ResourceLocation crop,
        ResourceLocation upperCrop,
        List<ResourceLocation> produce,
        Shape shape,
        int matureAge,
        boolean farmlandOverride
) {
    public enum Shape {
        /** One block with an {@code age} property. */
        SINGLE,
        /** Root block with {@code half=lower}; an upper half of the same block may sit above it. */
        DOUBLE,
        /** Root block plus a different upper block (Farmer's Delight rice and panicles). */
        SPLIT,
        /** Rooted vine segment; non-rooted segments of the same block may climb above it. */
        CLIMBING
    }

    public ResourceLocation seedBagId() {
        return ResourceLocation.fromNamespaceAndPath(ICECore.MOD_ID, id + "_seed_bag");
    }
}
